package com.msb.mall.product.vo;

import com.msb.mall.product.entity.SkuImagesEntity;
import com.msb.mall.product.entity.SkuInfoEntity;
import com.msb.mall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

/**
 * 商品详情页的数据对象
 */
@Data
public class ItemVO {
    // 1. sku的基本信息 pms_sku_info
    private SkuInfoEntity skuInfo;
    // 2. sku的图片信息 pms_spu_images
    private List<SkuImagesEntity> images;
    // 3. 获取spu中的销售属性的组合
    private List<SkuItemSaleAttrVO> saleAttrVOS;
    // 4. 获取SPU的介绍
    private SpuInfoDescEntity spuInfoDesc;
    // 5. 获取SPU的规格参数信息
    private List<SpuItemGroupAttrVO> baseAttrs;


//    /**
//     * 商品每个属性组中各个基本属性及对应的值
//     * 如手机基本信息这一属性组内容包括 {CPU型号：骁龙695 机身颜色：彩云追月 机身尺寸：宽73.9mm；长161.6mm；厚7.9mm 机身重量：172g}
//     */
//    public static class SpuBaseAttrVO{
//        private String attrName;
//        private String attrValue;
//    }
}
