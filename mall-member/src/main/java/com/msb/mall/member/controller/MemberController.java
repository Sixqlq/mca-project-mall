package com.msb.mall.member.controller;

import java.util.Arrays;
import java.util.Map;


import com.alibaba.fastjson.JSON;
import com.msb.common.exception.BizCodeEnume;
import com.msb.mall.member.exception.PhoneExistException;
import com.msb.mall.member.exception.UserNameExistException;
import com.msb.mall.member.vo.MemberLoginVO;
import com.msb.mall.member.vo.MemberRegisterVO;
import com.msb.mall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.msb.mall.member.entity.MemberEntity;
import com.msb.mall.member.service.MemberService;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.R;



/**
 * 会员
 *
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 20:25:33
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    /**
     * 会员注册
     * @return
     */
    @PostMapping("/register")
    public R register(@RequestBody MemberRegisterVO vo){
        try{
            memberService.register(vo);
        }catch (UserNameExistException userNameExistException){
            return R.error(BizCodeEnume.USERNAME_EXIST_EXCEPTION.getCode(), BizCodeEnume.USERNAME_EXIST_EXCEPTION.getMsg());
        }catch (PhoneExistException phoneExistException){
            return R.error(BizCodeEnume.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnume.PHONE_EXIST_EXCEPTION.getMsg());
        }catch (Exception e){
            return R.error(BizCodeEnume.UNKNOW_EXCEPTION.getCode(), BizCodeEnume.UNKNOW_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    /**
     * 会员登录
     * @return
     */
    @RequestMapping("/login")
    public R login(@RequestBody MemberLoginVO vo){
        MemberEntity entity = memberService.login(vo);
        if(entity != null){
            return R.ok().put("entity", JSON.toJSONString(entity));
        }
        return R.error(BizCodeEnume.USERNAME_PHONE_VALID_EXCEPTION.getCode(), BizCodeEnume.USERNAME_PHONE_VALID_EXCEPTION.getMsg());
    }

    /**
     * 社交登录
     * @return
     */
    @RequestMapping("/oauth2/login")
    public R socialLogin(@RequestBody SocialUser vo){
        MemberEntity entity = memberService.socialLogin(vo);
        return R.ok().put("entity", JSON.toJSONString(entity));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
