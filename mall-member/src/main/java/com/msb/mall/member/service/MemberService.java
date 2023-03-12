package com.msb.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.utils.PageUtils;
import com.msb.mall.member.entity.MemberEntity;
import com.msb.mall.member.exception.PhoneExistException;
import com.msb.mall.member.exception.UserNameExistException;
import com.msb.mall.member.vo.MemberLoginVO;
import com.msb.mall.member.vo.MemberRegisterVO;
import com.msb.mall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 20:25:33
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegisterVO vo) throws PhoneExistException, UserNameExistException;

    MemberEntity login(MemberLoginVO vo);

    MemberEntity socialLogin(SocialUser vo);
}

