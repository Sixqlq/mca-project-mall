package com.msb.mall.product.dao;

import com.msb.mall.product.entity.AttrAttrgroupRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性&属性分组关联
 * 
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 18:08:55
 */
@Mapper
public interface AttrAttrgroupRelationDao extends BaseMapper<AttrAttrgroupRelationEntity> {


    /**
     * 删除属性组与属性的关联信息
     * @param entityList
     */
    void removeBatchRelation(@Param("entityList")List<AttrAttrgroupRelationEntity> entityList);
}
