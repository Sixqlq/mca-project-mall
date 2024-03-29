package com.msb.mall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRedisConfig {
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        // 单机Redisson配置连接信息
        config.useSingleServer().setAddress("redis://192.168.153.178:6379");
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
