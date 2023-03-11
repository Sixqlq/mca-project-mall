package com.msb.mall.mallauth_server.controller;

import com.msb.common.constant.SMSConstant;
import com.msb.common.exception.BizCodeEnume;
import com.msb.common.utils.R;
import com.msb.mall.mallauth_server.feign.MemberFeignService;
import com.msb.mall.mallauth_server.feign.ThirdPartFeignService;
import com.msb.mall.mallauth_server.vo.LoginVO;
import com.msb.mall.mallauth_server.vo.UserRegisterVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {
    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;

    /**
     * 发送短信验证码
     * @param phone
     * @return
     */
    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendSMSCode(@RequestParam("phone") String phone){
        // 防止60秒内重复发送
        Object redisCode = redisTemplate.opsForValue().get(SMSConstant.SMS_CODE_PREFIX + phone);
        if(redisCode != null && StringUtils.isNotBlank(redisCode.toString())){
            Long l = Long.parseLong(redisCode.toString().split("_")[1]);
            if(System.currentTimeMillis() - l <= 60000){
                // 说明验证码的发送间隔不足一分钟
                return R.error(BizCodeEnume.VALID_SMS_EXCEPTION.getCode(), BizCodeEnume.VALID_SMS_EXCEPTION.getMsg());
            }
        }
        // 生成随机验证码 --> 把生成的验证码存储到Redis服务中 sms:code:15251891599 12345
        String code = UUID.randomUUID().toString().substring(0, 5);
        thirdPartFeignService.sendSMSCode(phone, code);
        code = code + "_" + System.currentTimeMillis();
        System.out.println("code = " + code);
        redisTemplate.opsForValue().set(SMSConstant.SMS_CODE_PREFIX + phone, code, 5, TimeUnit.MINUTES);

        return R.ok();
    }

    @PostMapping("/sms/register")
    public String register(@Valid UserRegisterVO vo, BindingResult result, Model model){
        Map<String, String> map = new HashMap<>();
        if(result.hasErrors()){
            // 表示提交的数据不合法
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                String field = fieldError.getField();
                String defaultMessage = fieldError.getDefaultMessage();
                map.put(field, defaultMessage);
            }
            model.addAttribute("error", map);
            return "/reg";
        }else {
            // 验证码是否正确
            String code  = (String) redisTemplate.opsForValue().get(SMSConstant.SMS_CODE_PREFIX + vo.getPhone());
            code = code.split("_")[0];
            if(!code.equals(vo.getCode())){
                map.put("code", "验证码错误");
                model.addAttribute("error", map);
                return "/reg";
            }else{
                // 验证码正确 删除验证码
                redisTemplate.delete(SMSConstant.SMS_CODE_PREFIX + vo.getPhone());
                // 远程调用对应的服务 完成注册功能
                R r = memberFeignService.register(vo);
                if(r.getCode() == 0){
                    // 注册成功
                    return "redirect:http://msb.auth.com/login.html";
                }else {
                    // 注册失败
                    map.put("msg", r.getCode() + ":" + r.get("msg"));
                    model.addAttribute("error", map);
                    return "/reg";
                }
            }
        }
    }

    /**
     *
     * @return
     */
    @PostMapping("/login")
    public String login(LoginVO loginVO, RedirectAttributes redirectAttributes){
        R r = memberFeignService.login(loginVO);
        if(r.getCode() == 0){
            // 表示登录成功 跳转到商城首页
            return "redirect:http://msb.mall.com/home.html";
        }

        redirectAttributes.addAttribute("errors", r.get("msg"));
        // 表示登录失败,重新跳转到登录页面
        return "redirect:http://msb.auth.com/login.html";
    }
}
