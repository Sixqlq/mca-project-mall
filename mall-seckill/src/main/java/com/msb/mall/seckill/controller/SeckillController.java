package com.msb.mall.seckill.controller;

import com.alibaba.fastjson.JSON;
import com.msb.common.utils.R;
import com.msb.mall.seckill.dto.SeckillSkuRedisDto;
import com.msb.mall.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * 查询出当前时间内的秒杀活动及对应的商品Sku信息
     * @return
     */
    @GetMapping("/currentSeckillSessionSkus")
    @ResponseBody
    public R getCurrentSeckillSessionSkus(){
        List<SeckillSkuRedisDto> currentSeckillSkus = seckillService.getCurrentSeckillSkus();
        return R.ok().put("data", JSON.toJSONString(currentSeckillSkus));
    }

    /**
     * 根据skuId查询出秒杀活动对应的信息
     * @param skuId
     * @return
     */
    @GetMapping("/seckillSessionBySkuId/{skuId}")
    @ResponseBody
    public R getSeckillSessionBySkuId(@PathVariable("skuId") Long skuId){
        SeckillSkuRedisDto seckillSkuRedisDto = seckillService.getSeckillSessionBySkuId(skuId);
        return R.ok().put("data", JSON.toJSONString(seckillSkuRedisDto));
    }

    /**
     * 秒杀抢购
     * killId=1_14&code=5478874f0f5347619b72f396a1d768bc&num=1
     * @return
     */
    @GetMapping("/kill")
    public String seckill(@RequestParam("killId") String killId,
                          @RequestParam("code") String code,
                          @RequestParam("num") Integer num,
                          Model model){
        String orderSn = seckillService.kill(killId, code, num);
        model.addAttribute("orderSn", orderSn);
        return "success";
    }
}
