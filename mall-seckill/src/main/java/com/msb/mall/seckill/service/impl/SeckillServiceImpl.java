package com.msb.mall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.msb.common.constant.OrderConstant;
import com.msb.common.constant.SeckillConstant;
import com.msb.common.dto.SeckillOrderDto;
import com.msb.common.utils.R;
import com.msb.common.vo.MemberVO;
import com.msb.mall.seckill.dto.SeckillSkuRedisDto;
import com.msb.mall.seckill.feign.CouponFeignService;
import com.msb.mall.seckill.feign.ProductFeignService;
import com.msb.mall.seckill.interceptor.AuthInterceptor;
import com.msb.mall.seckill.service.SeckillService;
import com.msb.mall.seckill.vo.SeckillSessionEntity;
import com.msb.mall.seckill.vo.SkuInfoVo;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RocketMQTemplate rocketMQTemplate;

    @Override
    public void uploadSeckillSku3Days() {
        // 1. 通过feign 远程调用Coupon服务接口获取未来三天的秒杀活动的商品
        R r = couponFeignService.getLates3DaysSession();
        if(r.getCode() == 0){
            // 查询成功
            String json = (String) r.get("data");
            List<SeckillSessionEntity> seckillSessionEntityList = JSON.parseArray(json, SeckillSessionEntity.class);
            // 2. 上架商品 Redis数据保存
            // 2.1 缓存每日秒杀的活动信息
            saveSessionInfos(seckillSessionEntityList);
            // 2.2 缓存Sku的基本信息
            saveSessionSkuInfos(seckillSessionEntityList);
        }



    }

    /**
     * 查询出当前时间内的秒杀活动及对应的商品Sku信息
     * @return
     */
    @Override
    public List<SeckillSkuRedisDto> getCurrentSeckillSkus() {
        // 1. 确定当前时间是属于哪个秒杀活动的
        long time = new Date().getTime();
        // 从Redis中查询所有的秒杀活动
        Set<String> keys = stringRedisTemplate.keys(SeckillConstant.SESSION_CACHE_PREFIX + "*");
        for (String key : keys) {
            // seckill:sessions1679270400000_1679274000000
            String replace = key.replace(SeckillConstant.SESSION_CACHE_PREFIX, "");
            // replace: 1679270400000_1679274000000
            String[] s = replace.split("_");
            Long start = Long.parseLong(s[0]); // 活动开始时间
            Long end = Long.parseLong(s[1]); // 活动结束时间
            if(time > start && start < end){
                // 说明秒杀活动就是当前时间需要参与的活动
                // 取出来的是sessionId_skuId 1_14
                List<String> range = stringRedisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> ops = stringRedisTemplate.boundHashOps(SeckillConstant.SKU_CACHE_PREFIX);
                List<String> list = ops.multiGet(range);
                if(list != null && list.size() > 0){
                    List<SeckillSkuRedisDto> seckillSkus = list.stream().map(item -> {
                        SeckillSkuRedisDto seckillSkuRedisDto = JSON.parseObject(item, SeckillSkuRedisDto.class);
                        return seckillSkuRedisDto;
                    }).collect(Collectors.toList());
                    return seckillSkus;
                }
            }
        }
        return null;
    }

    /**
     * 根据skuId查询秒杀活动对应的信息
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisDto getSeckillSessionBySkuId(Long skuId) {
        // 1. 找到所有需要参与秒杀的商品的sku信息
        BoundHashOperations<String, String, String> ops = stringRedisTemplate.boundHashOps(SeckillConstant.SKU_CACHE_PREFIX);
        Set<String> keys = ops.keys();
        if(keys != null && keys.size() > 0){
            // 正则表达式构建hash key：1_14
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                boolean matches = Pattern.matches(regx, key);
                if(matches){
                    // 说明找到了Sku信息
                    String json = ops.get(key);
                    SeckillSkuRedisDto seckillSkuRedisDto = JSON.parseObject(json, SeckillSkuRedisDto.class);
                    return seckillSkuRedisDto;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String code, Integer num) {
        // 1. 根据killId获取当前秒杀商品的信息 Redis中
        BoundHashOperations<String, String, String> ops = stringRedisTemplate.boundHashOps(SeckillConstant.SKU_CACHE_PREFIX);
        String json = ops.get(killId);
        if(StringUtils.isNotBlank(json)){
            SeckillSkuRedisDto dto = JSON.parseObject(json, SeckillSkuRedisDto.class);
            // 校验合法性  1. 校验时效性
            Long startTime = dto.getStartTime();
            Long endTime = dto.getEndTime();
            Long now = new Date().getTime();
            if(now > startTime && now < endTime){
                // 说明是在秒杀活动时间范围内的请求
                // 2. 校验随机码
                String randCode = dto.getRandCode();
                Long skuId = dto.getSkuId();
                String redisKillId = dto.getPromotionSessionId() + "_" + skuId;
                if(randCode.equals(code) && killId.equals(redisKillId)){
                    // 随机码校验合法
                    // 3. 判断商品数量是否符合限制
                    if(num <= dto.getSeckillLimit().intValue()){
                        // 满足限购条件
                        // 4. 判断是否满足幂等性
                        // 只要抢购成功我们就在Redis中 存储一条信息 userId + sessionId + skuId
                        MemberVO memberVO = AuthInterceptor.threadLocal.get();
                        Long id = memberVO.getId();
                        String redisKey = id + "_" + redisKillId;
                        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), (endTime - now), TimeUnit.MILLISECONDS);
                        if(aBoolean){
                            // 表示数据插入成功，是第一次操作
                            RSemaphore semaphore = redissonClient.getSemaphore(SeckillConstant.SKU_STOCK_SEMAPHORE + randCode);
                            try {
                                boolean b = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
                                if(b){
                                    // 表示秒杀成功
                                    String orderSn = UUID.randomUUID().toString().replace("-", "");
                                    // 继续完成快速下订单操作 --> RocketMQ
                                    SeckillOrderDto orderDto = new SeckillOrderDto();
                                    orderDto.setOrderSn(orderSn);
                                    orderDto.setSkuId(skuId);
                                    orderDto.setSeckillPrice(dto.getSeckillPrice());
                                    orderDto.setMemberId(id);
                                    orderDto.setNum(num);
                                    orderDto.setPromotionSessionId(dto.getPromotionSessionId());
                                    // 通过RocketMQ 发送异步消息
                                    rocketMQTemplate.sendOneWay(OrderConstant.ROCKETMQ_SECKILL_ORDER_TOPIC,
                                            MessageBuilder.withPayload(JSON.toJSONString(orderDto)).build());
                                    return orderSn;
                                }
                            } catch (InterruptedException e) {
                                return null;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 保存每日秒杀的活动信息
     * @param seckillSessionEntityList
     */
    private void saveSessionInfos(List<SeckillSessionEntity> seckillSessionEntityList) {
        for (SeckillSessionEntity seckillSessionEntity : seckillSessionEntityList) {
            // 循环缓存每一个活动 key: start_end value:{skuId}
            long start = seckillSessionEntity.getStartTime().getTime();
            long end = seckillSessionEntity.getEndTime().getTime();
            // 生成key
            String key = SeckillConstant.SESSION_CACHE_PREFIX + start + "_" + end;
            Boolean flag = stringRedisTemplate.hasKey(key);
            if(!flag){
                // 表示这个秒杀活动在Redis中不存在，也就是还没有上架，那么需要保存
                // 需要存储到Redis中的这个秒杀活动涉及到所有相关商品信息的skuId
                List<String> skus = seckillSessionEntity.getRelationEntities().stream().map(item -> {
                    // 秒杀活动存储的value是 sessionId_skuId
                    return item.getPromotionSessionId() + "_" + item.getSkuId().toString();
                }).collect(Collectors.toList());
                stringRedisTemplate.opsForList().leftPushAll(key, skus);
            }
        }
    }

    /**
     * 保存每日秒杀活动对应的Sku信息
     * @param seckillSessionEntityList
     */
    private void saveSessionSkuInfos(List<SeckillSessionEntity> seckillSessionEntityList) {
        seckillSessionEntityList.stream().forEach(session->{
            // 循环取出每个Session, 然后取出对应SkuId 封装相关信息
            BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(SeckillConstant.SKU_CACHE_PREFIX);
            session.getRelationEntities().stream().forEach(item->{
                String skuKey = item.getPromotionSessionId() + "_" + item.getSkuId();
                Boolean flag = stringRedisTemplate.hasKey(skuKey);
                if(!flag){
                    SeckillSkuRedisDto dto = new SeckillSkuRedisDto();
                    // 1. 获取Sku的基本信息
                    R info = productFeignService.info(item.getSkuId());
                    if(info.getCode() == 0){
                        String skuInfoJson = (String) info.get("skuInfoJson");
                        SkuInfoVo skuInfoVo = JSON.parseObject(skuInfoJson, SkuInfoVo.class);
                        dto.setSkuInfoVo(skuInfoVo);
                    }
                    // 2. 获取Sku的秒杀信息
//                dto.setSkuId(item.getSkuId());
//                dto.setSeckillPrice(item.getSeckillPrice());
//                dto.setSeckillCount(item.getSeckillCount());
//                dto.setSeckillLimit(item.getSeckillLimit());
//                dto.setSeckillSort(item.getSeckillSort());
                    BeanUtils.copyProperties(item, dto);
                    // 3. 设置当前商品的秒杀时间
                    dto.setStartTime(session.getStartTime().getTime());
                    dto.setEndTime(session.getEndTime().getTime());

                    // 4. 随机码
                    String token = UUID.randomUUID().toString().replace("-", "");
                    dto.setRandCode(token);
                    // 绑定对应的活动编号
                    dto.setPromotionSessionId(item.getPromotionSessionId());
                    // 分布式信号量处理 达到限流目的
                    RSemaphore semaphore = redissonClient.getSemaphore(SeckillConstant.SKU_STOCK_SEMAPHORE + token);
                    // 把秒杀活动的商品数量作为分布式信号量的信号量
                    semaphore.trySetPermits(item.getSeckillCount().intValue());
                    hashOps.put(skuKey, JSON.toJSONString(dto));
                }
            });
        });
    }


}
