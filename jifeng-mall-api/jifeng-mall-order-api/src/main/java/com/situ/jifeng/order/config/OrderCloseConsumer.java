package com.situ.jifeng.order.config;

import com.situ.jifeng.order.service.impl.OrderServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 超时关单消费者：消费死信队列消息，将待付款订单自动关闭并回补库存（幂等）。
 */
@Component
public class OrderCloseConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseConsumer.class);

    private OrderServiceImpl orderService;

    @Autowired
    public void setOrderService(OrderServiceImpl orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = OrderMqConfig.ORDER_DEAD_QUEUE)
    public void onMessage(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            Long orderId = Long.valueOf(body.trim());
            boolean ok = orderService.cancel(orderId);
            log.info("超时关单：orderId={} {}", orderId, ok ? "成功" : "跳过（非待付款或已处理）");
        } catch (Exception e) {
            log.error("超时关单失败：{}", body, e);
        }
    }
}
