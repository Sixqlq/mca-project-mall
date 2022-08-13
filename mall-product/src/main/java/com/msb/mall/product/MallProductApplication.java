package com.msb.mall.product;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

// 放开注册中心
@EnableDiscoveryClient
@SpringBootApplication
// 指定mapper接口对应的路径
@MapperScan("com.msb.mall.product.dao")
@ComponentScan(basePackages = "com.msb")
public class MallProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallProductApplication.class, args);
	}

}
