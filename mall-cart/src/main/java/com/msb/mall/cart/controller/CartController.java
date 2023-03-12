package com.msb.mall.cart.controller;

import com.msb.mall.cart.service.ICartService;
import com.msb.mall.cart.vo.Cart;
import com.msb.mall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class CartController {
    @Autowired
    private ICartService cartService;

    /**
     * 展示当前用户购物车信息
     * @param model
     * @return
     */
    @GetMapping("/cart_list")
    public String queryCartList(Model model) {
        Cart cart = cartService.getCartList();
        model.addAttribute("list", cart);
        return "/cartList";
    }

    /**
     * 将商品添加到购物车
     *
     * @param skuId
     * @return
     */
    @GetMapping("/addCart")
    public String addCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, Model model) {
        CartItem item = null;
        try {
            item = cartService.addCart(skuId, num);
        } catch (Exception e) {
            e.printStackTrace();
        }
        model.addAttribute("item", item);
        return "/success";
    }
}

