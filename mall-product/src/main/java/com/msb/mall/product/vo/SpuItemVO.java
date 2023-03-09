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
public class SpuItemVO {
    // 1. sku的基本信息 pms_sku_info
    private SkuInfoEntity info;

    private boolean hasStock = true; // 是否有库存

    // 2. sku的图片信息 pms_spu_images
    private List<SkuImagesEntity> images;

    // 3. 获取spu中的销售属性的组合
    private List<SkuItemSaleAttrVO> saleAttrs;

    // 4. 获取SPU的介绍
    private SpuInfoDescEntity desc;

    // 5. 获取SPU的规格参数信息
    private List<SpuItemGroupAttrVO> baseAttrs;

    // 6. 绑定的对应的秒杀服务
    private SeckillVO seckillVO;

}
