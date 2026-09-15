package com.situ.jifeng.order.config;

import com.situ.jifeng.order.mapper.OrderMapper;
import com.situ.jifeng.order.service.impl.OrderServiceImpl;
import com.situ.jifeng.spi.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 超时关单的<b>兜底</b>扫描（设计文档 6.2「定时扫描兜底」）。
 *
 * <h2>为什么需要它</h2>
 * 待付款订单超时关闭的主路径是 RabbitMQ 延时消息（{@code OrderDelayMessageSender} →
 * 延迟队列 TTL → 死信队列 → {@link OrderCloseConsumer}）。这条路径是单点的：
 * <b>消息一旦丢失，订单就会永远停在「待付款」、库存永远不回补</b>。
 * 丢失的现实触发条件包括：投递时 order-api 不可用、消费端重试耗尽被丢弃
 * （{@code listener.simple.retry.max-attempts=3}）、MQ 重启丢持久化消息、
 * 队列被误删等。本任务就是这条路径的保险。
 *
 * <h2>为什么要留宽限期</h2>
 * 扫描的截止时刻是 {@code 超时时长 + 宽限期}，<b>比 MQ 的延时更长</b>
 * （默认 30 分钟 + 5 分钟 = 35 分钟）。这样正常运行时两件事同时成立：
 * <ul>
 *   <li>关单几乎总是由 MQ 在 30 分钟准点完成，扫描空转；</li>
 *   <li>扫描只在「MQ 该到而没到」时才动手，是真正的兜底。</li>
 * </ul>
 * 若两者用同样的 30 分钟，扫描会经常抢在消息之前把单关掉，
 * 于是 {@link OrderCloseConsumer} 每次消费都发现订单已不是待付款而打印「跳过」——
 * 主路径被架空、日志噪音大，出问题时也分不清是主路径坏了还是兜底在干活。
 *
 * <h2>并发安全</h2>
 * 本任务与 MQ 消费者可能同时处理同一单（消息延迟到点 vs 扫描先命中）。
 * {@link OrderService#cancel(Long)} 开头就判 {@code status != 待付款} 直接返回 false，
 * 这个判断在事务内、且有 {@code update ... where id} 的条件更新兜着，
 * 所以两条路径撞车时只会有一边真正生效，<b>不会重复回补库存</b>。
 *
 * <p>调用 {@code cancel} 走的是注入的 <b>Spring 代理</b>（注入接口而非 {@code this}），
 * 保证每单一个独立事务：某一单失败只回滚它自己，不会把整批带下水。
 * （order-api 没有开启 {@code exposeProxy}，无法用 {@code AopContext} 自调用。）</p>
 */
@Component
public class OrderTimeoutCloseTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutCloseTask.class);

    private OrderMapper orderMapper;
    private OrderService orderService;

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 宽限期（毫秒）。扫描截止时刻 = 订单超时时长 + 本值。
     * 调大它 → 兜底更晚介入、更不容易与 MQ 主路径重叠；调小到 0 → 退化成与主路径抢单。
     */
    @Value("${jifeng-mall.order.timeout-close.grace-ms:300000}")
    private long graceMillis;

    /** 单次扫描最多处理多少单，防止故障后一次性堆积把服务压垮；超出部分留给下一轮 */
    @Value("${jifeng-mall.order.timeout-close.batch-size:200}")
    private int batchSize;

    @Scheduled(initialDelayString = "${jifeng-mall.order.timeout-close.initial-delay-ms:60000}",
            fixedDelayString = "${jifeng-mall.order.timeout-close.scan-interval-ms:60000}")
    public void closeTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now()
                .minus(OrderDelayMessageSender.DEFAULT_TIMEOUT, ChronoUnit.MILLIS)
                .minus(graceMillis, ChronoUnit.MILLIS);

        List<Long> ids;
        try {
            ids = orderMapper.findTimeoutOrderIds(
                    OrderServiceImpl.STATUS_PENDING_PAY, deadline, batchSize);
        } catch (Exception e) {
            // 查询失败必须留痕：本任务本身就是"防静默丢单"的，
            // 自己静默失败就失去意义了（对照 SeckillStockPreheatTask 的教训）
            log.error("超时关单兜底：查询超时订单失败，deadline={}", deadline, e);
            return;
        }

        if (ids == null || ids.isEmpty()) {
            // 正常情况：MQ 主路径把单都关了，这里无事可做。不打印，避免每 60 秒刷一行噪音
            return;
        }

        int closed = 0;
        for (Long id : ids) {
            try {
                if (orderService.cancel(id)) {
                    closed++;
                }
            } catch (Exception e) {
                // 单笔失败不影响其余：可能是回补库存时下游服务不可用，
                // 该单下一轮扫描还会被捞到（status 仍是待付款），自愈
                log.error("超时关单兜底：关闭订单失败，orderId={}（下一轮会重试）", id, e);
            }
        }

        log.warn("超时关单兜底生效：命中 {} 单（MQ 延时消息未送达），本次关闭 {} 单",
                ids.size(), closed);

        // 取满了说明积压超过单批容量，明确告警而不是悄悄留到下一轮
        if (ids.size() >= batchSize) {
            log.warn("超时关单兜底：单批已达上限 {}，可能仍有积压未处理，请检查 MQ 消费端是否正常",
                    batchSize);
        }
    }
}
