package com.msb.mall.cart.service;

import com.msb.mall.cart.vo.Cart;
import com.msb.mall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 购物车Service接口
 */
public interface ICartService {
    public Cart getCartList();

    CartItem addCart(Long skuId, Integer num) throws Exception;
}
