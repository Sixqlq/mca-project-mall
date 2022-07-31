package com.msb.mall.order.dao;

import com.msb.mall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 20:26:53
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
