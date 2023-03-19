package com.msb.mall.seckill.config;

import com.msb.mall.seckill.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 在秒杀活动中并不是所有的请求都是需要在登录状态下的，所有这个拦截器应该只需要拦截部分的请求
 */
@Configuration
public class MyWebInterceptorConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor()).addPathPatterns("/seckill/kill");
    }
}
