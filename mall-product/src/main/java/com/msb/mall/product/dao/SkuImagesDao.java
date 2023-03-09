package com.msb.mall.product.dao;

import com.msb.mall.product.entity.SkuImagesEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku图片
 * 
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 18:08:55
 */
@Mapper
public interface SkuImagesDao extends BaseMapper<SkuImagesEntity> {

    List<SkuImagesEntity> getImagesBySkuId(@Param("skuId") Long skuId);
	
}
