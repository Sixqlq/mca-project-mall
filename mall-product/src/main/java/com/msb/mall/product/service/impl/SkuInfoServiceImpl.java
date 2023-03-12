package com.msb.mall.product.service.impl;

import com.msb.mall.product.entity.SkuImagesEntity;
import com.msb.mall.product.entity.SpuInfoDescEntity;
import com.msb.mall.product.service.*;
import com.msb.mall.product.vo.SpuItemVO;
import com.msb.mall.product.vo.SkuItemSaleAttrVO;
import com.msb.mall.product.vo.SpuItemGroupAttrVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;

import com.msb.mall.product.dao.SkuInfoDao;
import com.msb.mall.product.entity.SkuInfoEntity;
import org.springframework.util.StringUtils;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * SKU 信息检索的方法
     * 类别
     * 品牌
     * 价格区间
     * 检索的关键字
     * 分页查询
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        // 检索关键字
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w->{
                w.eq("sku_id",key).or().like("sku_name",key);
            });
        }

        // 分类
        String catalogId = (String)params.get("catalogId");
        if(!StringUtils.isEmpty(catalogId) && !"0".equalsIgnoreCase(catalogId)){
            wrapper.eq("catalog_id",catalogId);
        }
        // 品牌
        String brandId = (String)params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }
        // 价格区间
        String min = (String) params.get("min");
        if(!StringUtils.isEmpty(min)){
            wrapper.ge("price",min);
        }
        String max = (String) params.get("max");
        if(!StringUtils.isEmpty(max)){
            try {
                // 如果max=0那么我们也不需要加这个条件
                BigDecimal bigDecimal = new BigDecimal(max);
                if(bigDecimal.compareTo(new BigDecimal(0)) == 1){
                    // 说明 max > 0
                    wrapper.le("price",max);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据spuId查询所有的sku信息
     * @param spuId
     * @return
     */
    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;
    }

    /**
     * 根据skuId查询出返回商品详情页的数据
     * CompletableFuture异步编排处理
     * @param skuId
     * @return
     */
    @Override
    public SpuItemVO item(Long skuId) throws ExecutionException, InterruptedException {
        SpuItemVO itemVO = new SpuItemVO();
        CompletableFuture<SkuInfoEntity> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            // 1. sku的基本信息 pms_sku_info
            SkuInfoEntity skuInfo = getById(skuId);
            itemVO.setInfo(skuInfo);

            return skuInfo;
        }, threadPoolExecutor);

        CompletableFuture<Void> saleFuture = skuInfoFuture.thenAcceptAsync((res) -> {
            // 3. 获取spu中的销售属性的组合
            List<SkuItemSaleAttrVO> saleAttrs = skuSaleAttrValueService.getSkuSaleAttrValueBySpuId(res.getSpuId());
            itemVO.setSaleAttrs(saleAttrs);
        }, threadPoolExecutor);

        CompletableFuture<Void> spuFuture = skuInfoFuture.thenAcceptAsync((res) -> {
            // 4. 获取SPU的介绍
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            itemVO.setDesc(spuInfoDesc);
        }, threadPoolExecutor);

        CompletableFuture<Void> groupFuture = skuInfoFuture.thenAcceptAsync((res) -> {
            // 5. 获取SPU的规格参数信息
            List<SpuItemGroupAttrVO> groupAttrVo = attrGroupService.getAttrGroupWithSpuId(res.getSpuId(), res.getCatalogId());
            itemVO.setBaseAttrs(groupAttrVo);
        }, threadPoolExecutor);

        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            // 2. sku的图片信息 pms_spu_images
            List<SkuImagesEntity> skuImages = skuImagesService.getImagesBySkuId(skuId);
            itemVO.setImages(skuImages);
        }, threadPoolExecutor);

        CompletableFuture.allOf(saleFuture, spuFuture, groupFuture, imageFuture).get();

        return itemVO;
    }

    /**
     * 根据skuId查询销售信息
     * @param skuId
     * @return
     */
    @Override
    public List<String> getSkuSaleAttrs(Long skuId) {
        return baseMapper.getSkuSaleAttrs(skuId);
    }

}