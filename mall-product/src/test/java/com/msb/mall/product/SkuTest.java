package com.msb.mall.product;

import com.msb.mall.product.entity.SkuImagesEntity;
import com.msb.mall.product.service.SkuImagesService;
import com.msb.mall.product.service.SkuSaleAttrValueService;
import com.msb.mall.product.vo.SkuItemSaleAttrVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * Sku测试类
 */
@SpringBootTest(classes = MallProductApplication.class)
public class SkuTest {
    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    /**
     * 测试skuImagesDao方法getImagesBySkuId
     */
    @Test
    public void testGetImagesBySkuId(){
        Long skuId = 13l;
        List<SkuImagesEntity> skuImagesEntities = skuImagesService.getImagesBySkuId(skuId);
        for (SkuImagesEntity skuImagesEntity:skuImagesEntities) {
            System.out.println(skuImagesEntity);
        }
    }

    /**
     * 测试SkuSaleAttrValueDao方法getSkuSaleAttrValueBySpuId
     */
    @Test
    public void testGetSkuSaleAttrValueBySpuId(){
        Long spuId = 21l;
        List<SkuItemSaleAttrVO> saleAttrs = skuSaleAttrValueService.getSkuSaleAttrValueBySpuId(spuId);
        for (SkuItemSaleAttrVO skuItemSaleAttrVO : saleAttrs){
            System.out.println(skuItemSaleAttrVO);
        }
    }

}
