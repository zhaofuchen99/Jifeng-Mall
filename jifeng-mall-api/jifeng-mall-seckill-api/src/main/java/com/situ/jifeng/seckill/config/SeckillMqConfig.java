package com.situ.jifeng.seckill.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 秒杀异步下单的 RabbitMQ 配置。
 *
 * <p>秒杀抢购：Redis + Lua 扣减成功后，发送异步下单消息，由消费者创建秒杀订单（削峰）。</p>
 */
@Configuration
public class SeckillMqConfig {

    public static final String SECKILL_QUEUE = "seckill.order.queue";
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE).build();
    }

    /**
     * 消息转换器：把秒杀消息体加入 Java 反序列化白名单。
     *
     * <p>Spring AMQP 出于安全（防反序列化攻击），默认只允许反序列化 JDK 基础类，
     * <b>应用自定义类一律拒绝</b>。{@link SeckillGrabMessage} 走 Java 序列化，
     * 若不显式加白名单，消费端会抛 {@code SecurityException: Attempt to deserialize
     * unauthorized class} 并在重试耗尽后丢弃消息——而<b>发送端完全正常</b>（序列化不受限），
     * 因此该问题只在消费侧暴露，极易漏测。</p>
     *
     * <p>此处仅放开本项目包与消息体实际用到的 JDK 类型，不使用
     * {@code spring.amqp.deserialization.trust.all=true} 这类全放开开关。</p>
     */
    @Bean
    public MessageConverter simpleMessageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(List.of(
                "com.situ.jifeng.*",
                "java.lang.*",
                "java.math.*",
                "java.util.*",
                "java.time.*"));
        return converter;
    }

    @Bean
    public DirectExchange seckillExchange() {
        return ExchangeBuilder.directExchange(SECKILL_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(SECKILL_ROUTING_KEY);
    }
}
