package com.situ.jifeng.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单超时关单的延时消息发送器。
 */
@Component
public class OrderDelayMessageSender {

    /** 默认超时 30 分钟（毫秒） */
    public static final long DEFAULT_TIMEOUT = 30L * 60L * 1000L;

    private RabbitTemplate rabbitTemplate;

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送订单关单延时消息。
     *
     * @param orderId    订单主键
     * @param delayMillis 延时时长（毫秒）
     */
    public void sendCloseDelay(Long orderId, long delayMillis) {
        byte[] body = String.valueOf(orderId).getBytes(StandardCharsets.UTF_8);
        Message message = new Message(body);
        message.getMessageProperties().setContentType("text/plain");
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        // TTL 设定为延时时间；超过则将消息投递到死信队列
        message.getMessageProperties().setExpiration(String.valueOf(delayMillis));
        rabbitTemplate.convertAndSend(
                OrderMqConfig.ORDER_DELAY_EXCHANGE,
                OrderMqConfig.ORDER_DELAY_ROUTING_KEY,
                message);
    }
}
