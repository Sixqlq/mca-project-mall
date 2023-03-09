package com.msb.mall.mallsearch.vo;

import com.msb.common.dto.es.SkuESModel;
import lombok.Data;

import java.util.List;

/**
 * 封装从ES检索后的响应信息
 */
@Data
public class SearchResult {
    /**
     * 查询到的满足条件的所有商品信息
     */
    private List<SkuESModel> products;
    // 分页信息
    /**
     * 当前页
     */
    private Integer pageNum;
    /**
     * 总记录
     */
    private Long total;
    /**
     * 总页数
     */
    private Integer totalPages;

    private List<Integer> navs; // 需要显示的分页的页码

    /**
     * 当前查询到的所有的商品涉及到的品牌信息
     */
    private List<BrandVO> brands;
    /**
     * 当前查询到的所有的商品涉及到的属性信息
     */
    private List<AttrVO> attrs;
    /**
     * 当前查询到的所有的商品涉及到的类别信息
     */
    private List<CatalogVO> catalogs;

    /**
     * 类别信息
     */
    @Data
    public static class CatalogVO{
        private Long catalogId;
        private String catalogName;
    }

    /**
     * 品牌信息
     */
    @Data
    public static class BrandVO{
        private Long brandId; // 品牌编号
        private String brandName; // 品牌名称
        private String brandImg; // 品牌图片
    }

    /**
     * 属性信息
     */
    @Data
    public static class AttrVO{
        private Long attrId; // 属性的编号
        private String attrName; // 属性的名称
        private List<String> attrValue; // 属性的值
    }
}
