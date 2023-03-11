package com.msb.mall.third.controller;

import com.msb.common.utils.R;
import com.msb.mall.third.utils.SMSComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外提供短信服务
 */
@RestController
public class SMSController {

    @Autowired
    private SMSComponent component;

    /**
     * 调用短信服务商提供的短信API发送短信
     * @return
     */
    @GetMapping("/sms/sendCode")
    public R sendSMSCode(@RequestParam("phone") String phone, @RequestParam("code") String code){
        component.sendSMSCode(phone, code);
        return R.ok();
    }
}
