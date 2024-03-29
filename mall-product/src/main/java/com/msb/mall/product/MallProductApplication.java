package com.msb.mall.product;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableRedisHttpSession
// 放开SpringCache
@EnableCaching
// 放开注册中心
@EnableDiscoveryClient
@SpringBootApplication
// 指定mapper接口对应的路径
@MapperScan("com.msb.mall.product.dao")
@ComponentScan(basePackages = "com.msb")
@EnableFeignClients(basePackages = "com.msb.mall.product.feign")
public class MallProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallProductApplication.class, args);
	}

}
