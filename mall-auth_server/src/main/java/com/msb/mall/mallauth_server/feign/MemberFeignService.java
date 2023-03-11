package com.msb.mall.mallauth_server.feign;

import com.msb.common.utils.R;
import com.msb.mall.mallauth_server.vo.LoginVO;
import com.msb.mall.mallauth_server.vo.UserRegisterVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("mall-member")
public interface MemberFeignService {
    @PostMapping("/member/member/register")
    public R register(@RequestBody UserRegisterVO vo);

    @RequestMapping("/member/member/login")
    public R login(@RequestBody LoginVO vo);
}
