package com.msb.mall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * basePackages 指定Feign接口的路径
 */
@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.msb.mall.order.feign")
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.msb.mall.order.dao")
public class MallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallOrderApplication.class, args);
    }

}
