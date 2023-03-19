package com.msb.mall.order.utils;


import com.msb.common.constant.OrderConstant;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;


@Component
public class OrderMsgProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendOrderMessage(String orderSn){
        // 设置延时等级4,这个消息将在30s之后发送(现在只支持固定的几个时间,详看delayTimeLevel)
        rocketMQTemplate.syncSend(OrderConstant.ROCKETMQ_ORDER_TOPIC, MessageBuilder.withPayload(orderSn).build(), 5000, 4);
    }
}
