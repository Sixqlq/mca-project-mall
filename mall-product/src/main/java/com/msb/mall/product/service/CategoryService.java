package com.msb.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.utils.PageUtils;
import com.msb.mall.product.entity.CategoryEntity;
import com.msb.mall.product.vo.Catalog2VO;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 18:08:55
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询所有的类别数据，然后将数据封装为树形结构，便于前端使用
     * @param params
     * @return
     */
    List<CategoryEntity> queryPageWithTree(Map<String, Object> params);

    /**
     * 逻辑批量删除操作
     * @param ids
     */
    void removeCategoryByIds(List<Long> ids);

    /**
     * 查询三级分类路径
     * @param catelogId
     * @return
     */
    Long[] findCatelogPath(Long catelogId);

    void updateDetail(CategoryEntity entity);

    List<CategoryEntity> getLevel1Category();

    Map<String, List<Catalog2VO>> getCatalog2Json();
}

