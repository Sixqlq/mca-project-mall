package com.msb.mall.product.dao;

import com.msb.mall.product.entity.AttrGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.product.vo.SpuItemGroupAttrVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 18:08:55
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

    List<SpuItemGroupAttrVO> getAttrGroupWithSpuId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}
