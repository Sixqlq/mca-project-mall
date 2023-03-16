package com.msb.mall.ware.service.impl;

import com.msb.common.dto.SkuHasStockDto;
import com.msb.common.exception.NoStockException;
import com.msb.common.utils.R;
import com.msb.mall.ware.feign.ProductFeignService;
import com.msb.mall.ware.vo.OrderItemVO;
import com.msb.mall.ware.vo.WareSkuLockVO;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;

import com.msb.mall.ware.dao.WareSkuDao;
import com.msb.mall.ware.entity.WareSkuEntity;
import com.msb.mall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuDao skuDao;

    private ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 入库操作
     * @param skuId 商品编号
     * @param wareId 仓库编号
     * @param skuNum 采购商品的数量
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 判断是否有改商品和仓库的入库记录
        List<WareSkuEntity> list = skuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if(list == null || list.size() == 0){
            // 如果没有就新增商品库存记录
            WareSkuEntity entity = new WareSkuEntity();
            entity.setSkuId(skuId);
            entity.setWareId(wareId);
            entity.setStock(skuNum);
            entity.setStockLocked(0);
            try {
                // 动态的设置商品的名称
                R info = productFeignService.info(skuId); // 通过Feign远程调用商品服务的接口
                Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");
                if(info.getCode() == 0){
                    entity.setSkuName((String) data.get("skuName"));
                }
            }catch (Exception e){

            }
            skuDao.insert(entity); // 插入商品库存记录
        }else{
            // 如果有就更新库存
            skuDao.addStock(skuId,wareId,skuNum);
        }

    }

    /**
     * 获取每个sku对应的库存
     * @param skuIds
     * @return
     */
    @Override
    public List<SkuHasStockDto> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockDto> list = skuIds.stream().map(skuId -> {
            // 需要处理查询不到库存的异常情况
            Long count = baseMapper.getSkuStock(skuId);
            SkuHasStockDto dto = new SkuHasStockDto();
            dto.setSkuId(skuId);
            if(count == null || count == 0l){
                dto.setHasStock(false);
            }else{
                dto.setHasStock(true);
            }
            return dto;
        }).collect(Collectors.toList());
        return list;
    }

    /**
     * 多个商品锁定库存
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVO vo) {
        List<OrderItemVO> items = vo.getItems();
        // 首先找到具有库存的仓库
        List<SkuWareHasStock> ware = items.stream().map(item -> {
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            skuWareHasStock.setSkuId(item.getSkuId());
            List<WareSkuEntity> wareSkuEntities = this.baseMapper.listHasStock(item.getSkuId());
            skuWareHasStock.setWareSkuEntities(wareSkuEntities);
            skuWareHasStock.setNum(item.getCount());
            return skuWareHasStock;
        }).collect(Collectors.toList());
        // 尝试锁定库存
        for (SkuWareHasStock skuWareHasStock : ware) {
            Long skuId = skuWareHasStock.getSkuId();
            List<WareSkuEntity> wareSkuEntities = skuWareHasStock.getWareSkuEntities();
            if(wareSkuEntities == null || wareSkuEntities.size() == 0){
                // 当前商品没有库存
                throw new NoStockException(skuId);
            }
            // 当前需要锁定的商品的数量
            Integer count = skuWareHasStock.getNum();
            Boolean skuStocked = false; // 表示当前skuId的库存没有锁定完成
            for (WareSkuEntity wareSkuEntity : wareSkuEntities) {
                // 循环获取到对应的仓库，然后需要锁定库存
                // 获取当前仓库能够锁定的库存数
                Integer canStock = wareSkuEntity.getStock() - wareSkuEntity.getStockLocked();
                if(count <= canStock){
                    // 表示当前skuId的商品数量小于等于需要锁定的数量
                    Integer i = this.baseMapper.lockSkuStock(skuId, wareSkuEntity.getWareId(), count);
                    count = 0;
                    skuStocked = true;
                }else {
                    // 需要锁定的库存大于 可以锁定的库存 就按照已有的库存来锁定
                    Integer i = this.baseMapper.lockSkuStock(skuId, wareSkuEntity.getWareId(), canStock);
                    count = count - canStock;
                }
                if(count <= 0){
                    // 表示skuId对应的商品数量全部锁定
                     break;
                }
            }
            if(count > 0){
                // 说明库存没有锁定完
                throw new NoStockException(skuId);
            }
            if(!skuStocked){
                // 表示上一个商品的库存没有锁定成功
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    @Data
    class SkuWareHasStock{
        private Long skuId;
        private Integer num;
        private List<WareSkuEntity> wareSkuEntities;
    }

}