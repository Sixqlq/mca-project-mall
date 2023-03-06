package com.msb.mall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 二级分类需要展示的数据VO
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Catalog2VO {
    /**
     * 二级分类对应的一级分类的编号
     */
    private String catalog1Id;
    /**
     * 二级分类对应的三级分类的数据
     */
    private List<Catalog3VO> catalog3List;
    /**
     * 二级分类的编号
     */
    private String id;
    /**
     * 二级分类对应的类别名称
     */
    private String name;

    /**
     * 三级分类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Catalog3VO{
        /**
         * 三级分类对应的二级分类的编号
         */
        private String catalog2Id;
        /**
         * 三级分类编号
         */
        private String id;
        /**
         * 三级分类对应的类别名称
         */
        private String name;
    }
}
