package com.msb.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.msb.mall.product.entity.BrandEntity;
import com.msb.mall.product.entity.CategoryEntity;
import com.msb.mall.product.service.BrandService;
import com.msb.mall.product.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;

import com.msb.mall.product.dao.CategoryBrandRelationDao;
import com.msb.mall.product.entity.CategoryBrandRelationEntity;
import com.msb.mall.product.service.CategoryBrandRelationService;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }


    /**
     * 新增品牌与分类的关联
     * @param categoryBrandRelation
     */
    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        // 根据类别编号和品牌编号查询出对应的类别名称和品牌名称
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();

        CategoryEntity categoryEntity = categoryService.getById(catelogId);
        BrandEntity brandEntity = brandService.getById(brandId);

        categoryBrandRelation.setCatelogName(categoryEntity.getName());
        categoryBrandRelation.setBrandName(brandEntity.getName());

        this.save(categoryBrandRelation);
    }

    /**
     * 同步更新分类与品牌级联关系中品牌的名称
     * @param brandId
     * @param name
     */
    @Override
    public void updateBrandName(Long brandId, String name) {
        CategoryBrandRelationEntity entity = new CategoryBrandRelationEntity();
        entity.setBrandId(brandId);
        entity.setBrandName(name);
        this.update(entity, new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
    }

    /**
     * 同步更新分类与品牌级联关系中类别的名称
     * @param catId
     * @param name
     */
    @Override
    public void updateCatelogName(Long catId, String name) {
        CategoryBrandRelationEntity entity = new CategoryBrandRelationEntity();
        entity.setCatelogName(name);
        entity.setCatelogId(catId);
        this.update(entity, new UpdateWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));
    }

}