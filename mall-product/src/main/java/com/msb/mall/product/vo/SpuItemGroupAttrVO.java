package com.msb.mall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品规格参数里属性组的名称和对应的基本属性的集合
 */
@Data
public class SpuItemGroupAttrVO{
    private String groupName; // 属性组名称
    private List<Attr> baseAttrs; // 各个属性组下基本属性id、名称和对应的值
}
