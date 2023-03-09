package com.msb.mall.product.vo;

import lombok.Data;


/**
 * spu中的销售属性VO, pms_sku_sale_attr_value
 * 销售属性包括颜色、版本、套装、京选服务等
 */
@Data
public class SkuItemSaleAttrVO{
    private Long attrId;
    private String attrName;
    private String attrValue;
}
