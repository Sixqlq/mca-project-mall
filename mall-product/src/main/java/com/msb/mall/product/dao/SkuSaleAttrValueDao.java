package com.msb.mall.product.dao;

import com.msb.mall.product.entity.SkuSaleAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.product.vo.ItemVO;
import com.msb.mall.product.vo.SkuItemSaleAttrVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku销售属性&值
 * 
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 18:08:55
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    List<SkuItemSaleAttrVO> getSkuSaleAttrValueBySpuId(@Param("spuId") Long spuId);
}
