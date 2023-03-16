package com.msb.mall.mallsearch.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能提交的查询条件
 */
@Data
public class SearchParam {

    private String keyword; //页面传递的查询全文匹配的关键字

    private Long catalog3Id; //需要根据分类查询的编号
    /**
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort; //排序条件

    private Integer hasStock; //是否只显示有货
    /**
     * brandId=1&brandId=2
     */
    private Long brandId; //按照品牌来查询, 可以多选
    /**
     * skuPrice=200_300 200-300之间
     * skuPrice=_300 最高300
     * skuPrice=200_ 最低200
     */
    private String skuPrice; //价格区间查询
    /**
     * attrs=10_6.5英寸:8英寸&attrs=11_64GB:128GB
     */
    private List<String> attrs; //按照属性信息筛选

    private Integer pageNum = 1; //页码


}
