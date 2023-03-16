package com.msb.mall.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.msb.common.constant.OrderConstant;
import com.msb.common.exception.NoStockException;
import com.msb.common.utils.R;
import com.msb.common.vo.MemberVO;
import com.msb.mall.order.dto.OrderCreateTO;
import com.msb.mall.order.entity.OrderItemEntity;
import com.msb.mall.order.feign.CartFeignService;
import com.msb.mall.order.feign.MemberFeignService;
import com.msb.mall.order.feign.ProductFeignService;
import com.msb.mall.order.feign.WareFeignService;
import com.msb.mall.order.interceptor.AuthInterceptor;
import com.msb.mall.order.service.OrderItemService;
import com.msb.mall.order.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;

import com.msb.mall.order.dao.OrderDao;
import com.msb.mall.order.entity.OrderEntity;
import com.msb.mall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    WareFeignService wareFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 订单支付页需要获取的信息
     * @return
     */
    @Override
    public OrderConfirmVO confirmOrder() {
        OrderConfirmVO vo = new OrderConfirmVO();
        MemberVO memberVO = (MemberVO) AuthInterceptor.threadLocal.get();
        // 获取到RequestContextHolder相关信息
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            // 同步主线程中的 RequestContextHolder
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 1. 查询当前登录用户对应的会员地址信息
            Long id = memberVO.getId();
            List<MemberAddressVO> address = memberFeignService.getAddress(id);
            vo.setAddress(address);
        }, executor);
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 2. 查询购物车中选中的商品信息
            List<OrderItemVO> userCartItems = cartFeignService.getUserCartItems();
            vo.setItems(userCartItems);
        }, executor);
        try {
            CompletableFuture.allOf(future1, future2).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        // 3. 计算订单的总金额和需要支付的总金额 vo自动计算

        // 4. 生成防重token
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        // 我们需要把token信息存储到Redis中
        // order:token:用户编号
        redisTemplate.opsForValue().set(OrderConstant.ORDER_TOKEN_PREFIX + ":" + memberVO.getId(), token);
        // 需要把token绑定到响应的数据对象中
        vo.setOrderToken(token);
        return vo;
    }

    private Lock lock = new ReentrantLock();
    /**
     * 提交订单
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public OrderResponseVO submitOrder(OrderSubmitVO vo) throws NoStockException{
        // 需要返回的响应对象
        OrderResponseVO responseVO = new OrderResponseVO();
        // 获取当前登录用户信息
        MemberVO memberVO = (MemberVO) AuthInterceptor.threadLocal.get();
        // 1. 验证是否重复提交 保证Redis中的token 的查询和删除是一个原子性操作
        String key = OrderConstant.ORDER_TOKEN_PREFIX + ":" + memberVO.getId();
        String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(key), vo.getOrderToken());
        if(result == 0){
            // 表示验证失败 重复提交
            responseVO.setCode(1);
            return responseVO;
        }
        // 2. 创建订单和订单项信息
        OrderCreateTO orderCreateTO = createOrder(vo);
        responseVO.setOrderEntity(orderCreateTO.getOrderEntity());
        // 3. 保存订单信息
        saveOrder(orderCreateTO);
        // 4. 锁定库存信息 订单号 sku_id sku_name 商品数量
        WareSkuLockVO wareSkuLockVO = new WareSkuLockVO();
        wareSkuLockVO.setOrderSn(orderCreateTO.getOrderEntity().getOrderSn());
        List<OrderItemVO> orderItemVOs = orderCreateTO.getOrderItemEntities().stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setSkuId(item.getSkuId());
            itemVO.setTitle(item.getSkuName());
            itemVO.setCount(item.getSkuQuantity());
            return itemVO;
        }).collect(Collectors.toList());
        wareSkuLockVO.setItems(orderItemVOs);
        // 远程锁库存操作
        R r = wareFeignService.orderLockStock(wareSkuLockVO);
        if(r.getCode() == 0){
            // 锁定库存成功
            responseVO.setCode(0); // 表示创建订单成功
        }else {
            // 锁定库存失败
            responseVO.setCode(2); // 表示库存不足，锁定失败
            throw new NoStockException(10000l);
        }
        return responseVO;
    }

    /**
     * 生成订单数据
     * @param orderCreateTO
     */
    private void saveOrder(OrderCreateTO orderCreateTO) {
        // 1. 订单数据
        OrderEntity orderEntity = orderCreateTO.getOrderEntity();
        this.save(orderEntity);
        // 2. 订单项数据
        List<OrderItemEntity> orderItemEntityList = orderCreateTO.getOrderItemEntities();
        orderItemService.saveBatch(orderItemEntityList);
    }

    /**
     * 创建订单
     * @param vo
     * @return
     */
    private OrderCreateTO createOrder(OrderSubmitVO vo) {
        OrderCreateTO createTO = new OrderCreateTO();
        // 创建OrderEntity
        OrderEntity orderEntity = createOrderEntity(vo);
        createTO.setOrderEntity(orderEntity);
        // 创建OrderItemEntity 订单项
        List<OrderItemEntity> orderItemEntityList = createOrderItems(orderEntity.getOrderSn());
        createTO.setOrderItemEntities(orderItemEntityList);
        return createTO;
    }

    /**
     * 创建订单信息
     * @param vo
     * @return
     */
    private OrderEntity createOrderEntity(OrderSubmitVO vo) {
        OrderEntity orderEntity = new OrderEntity();
        // 创建订单编号
        String orderSn = IdWorker.getTimeId();
        orderEntity.setOrderSn(orderSn);
        // 设置会员信息
        MemberVO memberVO = (MemberVO) AuthInterceptor.threadLocal.get();
        orderEntity.setMemberId(memberVO.getId());
        orderEntity.setMemberUsername(memberVO.getUsername());
        // 根据收货地址ID获取收货地址详细信息
        MemberAddressVO memberAddressVO = memberFeignService.getAddressById(vo.getAddrId());
        orderEntity.setReceiverCity(memberAddressVO.getCity());
        orderEntity.setReceiverDetailAddress(memberAddressVO.getDetailAddress());
        orderEntity.setReceiverName(memberAddressVO.getName());
        orderEntity.setReceiverPhone(memberAddressVO.getPhone());
        orderEntity.setReceiverPostCode(memberAddressVO.getPostCode());
        orderEntity.setReceiverProvince(memberAddressVO.getProvince());
        orderEntity.setReceiverRegion(memberAddressVO.getRegion());
        // 设置订单状态
        orderEntity.setStatus(OrderConstant.OrderStatusEnum.FOR_THE_PAYMENT.getCode());
        return orderEntity;
    }

    /**
     * 创建订单项信息
     * @param orderSn
     * @return
     */
    private List<OrderItemEntity> createOrderItems(String orderSn) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        // 获取购物车中选中的商品信息
        List<OrderItemVO> userCartItems = cartFeignService.getUserCartItems();
        if(userCartItems != null && userCartItems.size() > 0){
            // 根据spuId查询出对应Spu信息
            List<Long> spuIds = new ArrayList<>();
            for (OrderItemVO orderItemVO : userCartItems) {
                if(!spuIds.contains(orderItemVO.getSpuId())){
                    spuIds.add(orderItemVO.getSpuId());
                }
            }
            // 远程调用获取Spu信息
            Long [] spuIdsArray = new Long[spuIds.size()];
            List<OrderItemSpuInfoVO> spuInfos = productFeignService.getOrderItemSpuInfoBySpuId(spuIds.toArray(spuIdsArray));
            // 转成map，key:spuId value:OrderItemSpuInfoVO
            Map<Long, OrderItemSpuInfoVO> map = spuInfos.stream().collect(Collectors.toMap(OrderItemSpuInfoVO::getId, item -> item));
            for (OrderItemVO userCartItem : userCartItems) {
                // 获取到商品信息对应的Spu信息
                OrderItemSpuInfoVO spuInfo = map.get(userCartItem.getSpuId());
                OrderItemEntity orderItemEntity = createOrderItem(userCartItem, spuInfo);
                // 绑定对应的订单编号
                orderItemEntity.setOrderSn(orderSn);
                orderItemEntities.add(orderItemEntity);
            }
        }
        return orderItemEntities;
    }

    /**
     * 根据购物车中一个商品创建订单项
     * @param userCartItem
     * @return
     */
    private OrderItemEntity createOrderItem(OrderItemVO userCartItem, OrderItemSpuInfoVO spuInfo) {
        OrderItemEntity entity = new OrderItemEntity();
        // Sku信息
        entity.setSkuId(userCartItem.getSkuId());
        entity.setSkuName(userCartItem.getTitle());
        entity.setSkuPic(userCartItem.getImage());
        entity.setSkuQuantity(userCartItem.getCount());
        List<String> skuAttr = userCartItem.getSkuAttr();
        String skuAttrStr = StringUtils.collectionToDelimitedString(skuAttr, ";");
        entity.setSkuAttrsVals(skuAttrStr);
        // Spu信息
        entity.setSpuId(spuInfo.getId());
        entity.setSpuName(spuInfo.getSpuName());
        entity.setSpuBrand(spuInfo.getBrandName());
        entity.setSpuPic(spuInfo.getImg());
        entity.setCategoryId(spuInfo.getCatalogId());
        // 优惠信息 忽略
        // 积分信息
        entity.setGiftGrowth(userCartItem.getPrice().intValue());
        entity.setGiftIntegration(userCartItem.getPrice().intValue());
        return entity;
    }



}