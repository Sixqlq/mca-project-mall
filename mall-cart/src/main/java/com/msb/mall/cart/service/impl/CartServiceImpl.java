package com.msb.mall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.msb.common.constant.CartConstant;
import com.msb.common.utils.R;
import com.msb.common.vo.MemberVO;
import com.msb.mall.cart.feign.ProductFeignService;
import com.msb.mall.cart.interceptor.AuthInterceptor;
import com.msb.mall.cart.service.ICartService;
import com.msb.mall.cart.vo.Cart;
import com.msb.mall.cart.vo.CartItem;
import com.msb.mall.cart.vo.SkuInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 购物车信息存储在redis中
 */
@Service
public class CartServiceImpl implements ICartService {
    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    /**
     * 查询出当前登录用户的购物车信息
     * @return
     */
    @Override
    public Cart getCartList() {
        BoundHashOperations<String, Object, Object> operations = getCartKeyOperation();
        Set<Object> keys = operations.keys();
        Cart cart = new Cart();
        List<CartItem> list = new ArrayList<>();
        for (Object k: keys) {
            String key = (String) k;
            Object o = operations.get(key);
            String json = (String) o;
            CartItem item = JSON.parseObject(json, CartItem.class);
            list.add(item);
        }
        cart.setItems(list);
        return cart;
    }

    /**
     * 把商品添加到购物车中
     * @param skuId
     * @param num
     * @return
     */
    @Override
    public CartItem addCart(Long skuId, Integer num) throws Exception {
        BoundHashOperations<String, Object, Object> hashOperations = getCartKeyOperation();
        // 如果redis中存储该商品信息，只需要修改该商品的数量即可
        Object o = hashOperations.get(skuId.toString());
        if(o != null){
            String json = (String) o;
            CartItem item = JSON.parseObject(json, CartItem.class);
            item.setCount(item.getCount() + num);
            hashOperations.put(skuId.toString(), JSON.toJSONString(item));
            return item;
        }
        CartItem item = new CartItem();
        CompletableFuture future1 = CompletableFuture.runAsync(()->{
            // 1. 远程调用获取商品sku信息
            R r = productFeignService.info(skuId);
            String skuInfoJson = (String) r.get("skuInfoJson");
            SkuInfoVO vo = JSON.parseObject(skuInfoJson, SkuInfoVO.class);
            item.setCheck(true);
            item.setCount(num);
            item.setPrice(vo.getPrice());
            item.setImage(vo.getSkuDefaultImg());
            item.setSkuId(skuId);
            item.setTitle(vo.getSkuTitle());
        }, threadPoolExecutor);
        CompletableFuture future2 = CompletableFuture.runAsync(()->{
            // 2. 获取商品的销售属性
            List<String> skuSaleAttrs = productFeignService.getSkuSaleAttrs(skuId);
            item.setSkuAttr(skuSaleAttrs);
        }, threadPoolExecutor);
        CompletableFuture.allOf(future1, future2).get();
        // 3. 把数据存储到redis中
        String json = JSON.toJSONString(item);
        hashOperations.put(skuId.toString(), json);
        return item;
    }

    private BoundHashOperations<String, Object, Object> getCartKeyOperation() {
        // hash key: cart:1  skuId:cartItem
        MemberVO memberVO = AuthInterceptor.threadLocal.get();
        String cartKey = CartConstant.CART_PREFIX + memberVO.getId();
        BoundHashOperations<String, Object, Object> hashOperations = redisTemplate.boundHashOps(cartKey);
        return hashOperations;
    }
}
