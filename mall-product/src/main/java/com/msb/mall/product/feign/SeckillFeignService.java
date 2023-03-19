package com.msb.mall.product.feign;

import com.msb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("mall-seckill")
public interface SeckillFeignService {

    @GetMapping("/seckill/seckillSessionBySkuId/{skuId}")
    public R getSeckillSessionBySkuId(@PathVariable("skuId") Long skuId);
}
