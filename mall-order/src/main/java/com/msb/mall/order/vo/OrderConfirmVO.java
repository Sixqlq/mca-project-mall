package com.msb.mall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单确认页面中的数据VO
 */

public class OrderConfirmVO {
    @Getter @Setter
    // 订单收获人及收货地址信息
    List<MemberAddressVO> address;
    @Getter @Setter
    // 购物车选中的商品信息
    List<OrderItemVO> items;

    @Getter @Setter
    private String orderToken;

    public Integer getCountNum(){
        int count = 0;
        if(items != null){
            for (OrderItemVO item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    // 支付方式
    // 发票信息
    // 优惠信息
    BigDecimal total; // 总金额

    BigDecimal payTotal; // 需要支付的总金额

    public BigDecimal getTotal(){
        BigDecimal sum = new BigDecimal(0);
        if(items != null){
            for (OrderItemVO item : items) {
                BigDecimal totalPrice = item.getPrice().multiply(new BigDecimal(item.getCount()));
                sum = sum.add(totalPrice);
            }
        }
        return sum;
    }

    /**
     * 没有优惠的前提下，payTotal=total
     * @return
     */
    public BigDecimal getPayTotal(){
        return getTotal();
    }

}
