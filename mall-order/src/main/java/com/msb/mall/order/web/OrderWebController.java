package com.msb.mall.order.web;

import com.msb.common.exception.NoStockException;
import com.msb.mall.order.service.OrderService;
import com.msb.mall.order.vo.OrderConfirmVO;
import com.msb.mall.order.vo.OrderResponseVO;
import com.msb.mall.order.vo.OrderSubmitVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderWebController {
    @Autowired
    OrderService orderService;

    /**
     * 订单结算页
     * @return
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model){
        // 查询订单确认页需要的数据
        OrderConfirmVO confirmVO = orderService.confirmOrder();
        model.addAttribute("confirmVo", confirmVO);
        return "confirm";
    }

    @PostMapping("/orderSubmit")
    public String orderSubmit(OrderSubmitVO vo, Model model, RedirectAttributes redirectAttributes){
        Integer code = 0;
        OrderResponseVO responseVO = null;
        try {
            responseVO = orderService.submitOrder(vo);
            code = responseVO.getCode();
        }catch (NoStockException e){
            code = 2;
        }
        if(code == 0){
            // 下单成功
            model.addAttribute("orderResponseVO", responseVO);
            return "pay";
        }else {
            System.out.println("code = " + code);
            String msg = "下订单失败";
            if(code == 1){
                msg = msg + ": 重复提交";
            } else if (code == 2) {
                msg = msg + ": 锁库存失败";
            }
//            redirectAttributes.addAttribute("msg", msg);
            redirectAttributes.addFlashAttribute("msg", msg);
            // 下单失败，回到订单结算页
            return "redirect:http://order.msb.com/toTrade";
        }
    }
}
