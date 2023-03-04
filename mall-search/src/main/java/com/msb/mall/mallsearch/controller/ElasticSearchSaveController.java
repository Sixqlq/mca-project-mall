package com.msb.mall.mallsearch.controller;

import com.msb.common.dto.es.SkuESModel;
import com.msb.common.exception.BizCodeEnume;
import com.msb.common.utils.R;
import com.msb.mall.mallsearch.service.ElasticSearchSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 存储商品上架信息到ES服务的接口
 * 存储商城数据到ES服务
 */
@Slf4j
@RequestMapping("/search/save")
@RestController
public class ElasticSearchSaveController {
    @Autowired
    private ElasticSearchSaveService elasticSearchSaveService;
    /**
     * 存储商品上架信息到ES服务的接口
     * @return
     */
    @PostMapping("/product")
    public R productStartUp(@RequestBody List<SkuESModel> skuESModels) {
        Boolean b = false;
        try {
            b = elasticSearchSaveService.productStartUp(skuESModels);
        } catch (IOException e) {
            log.error("ElasticSearch商品上架错误: {}", e);
            return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());
        }
        if(b){
            return R.ok();
        }
        return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());
    }
}
