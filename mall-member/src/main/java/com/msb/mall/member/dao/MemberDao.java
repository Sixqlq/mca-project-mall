package com.msb.mall.member.dao;

import com.msb.mall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 20:25:33
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
