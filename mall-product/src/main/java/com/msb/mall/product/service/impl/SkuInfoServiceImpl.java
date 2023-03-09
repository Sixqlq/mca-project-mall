package com.msb.mall.product.service.impl;

import com.msb.mall.product.entity.SkuImagesEntity;
import com.msb.mall.product.entity.SkuSaleAttrValueEntity;
import com.msb.mall.product.entity.SpuInfoDescEntity;
import com.msb.mall.product.service.*;
import com.msb.mall.product.vo.ItemVO;
import com.msb.mall.product.vo.SkuItemSaleAttrVO;
import com.msb.mall.product.vo.SpuItemGroupAttrVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * @param skuId
     * @return
     */
    @Override
    public ItemVO item(Long skuId) {
        ItemVO itemVO = new ItemVO();
        // 1. sku的基本信息 pms_sku_info
        SkuInfoEntity skuInfo = this.getById(skuId);
        itemVO.setSkuInfo(skuInfo);
        Long spuId = skuInfo.getSpuId();
        Long catalogId = skuInfo.getCatalogId();
        // 2. sku的图片信息 pms_spu_images
        List<SkuImagesEntity> skuImages = skuImagesService.getImagesBySkuId(skuId);
        itemVO.setImages(skuImages);
        // 3. 获取spu中的销售属性的组合
        List<SkuItemSaleAttrVO> saleAttrs = skuSaleAttrValueService.getSkuSaleAttrValueBySpuId(spuId);
        itemVO.setSaleAttrVOS(saleAttrs);
        // 4. 获取SPU的介绍
        SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(spuId);
        itemVO.setSpuInfoDesc(spuInfoDesc);
        // 5. 获取SPU的规格参数信息
        List<SpuItemGroupAttrVO> groupAttrVo = attrGroupService.getAttrGroupWithSpuId(spuId,catalogId);
        itemVO.setBaseAttrs(groupAttrVo);
        return itemVO;
    }

}