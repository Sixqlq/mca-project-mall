package com.msb.mall.product;

import com.msb.mall.product.entity.SkuImagesEntity;
import com.msb.mall.product.service.SkuImagesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = MallProductApplication.class)
public class SkuTest {
    @Autowired
    private SkuImagesService skuImagesService;

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
}
