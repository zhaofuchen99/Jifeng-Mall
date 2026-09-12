package com.situ.jifeng.seckill.config;

import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.seckill.mapper.SeckillGoodMapper;
import com.situ.jifeng.seckill.service.OrderFeignService;
import com.situ.jifeng.spi.model.OrderEntity;
import com.situ.jifeng.spi.model.SeckillOrderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 秒杀异步下单消费者：消费抢购队列，创建秒杀订单并扣减秒杀库存。
 *
 * <p>一致性要点：</p>
 * <ol>
 *   <li>幂等：以 seckillNo 为准，已存在订单则直接跳过（MQ 重投不重复下单/不重复扣减）；</li>
 *   <li>DB 兜底防超卖：{@code decreaseStock} 条件更新（stock &gt;= qty）原子扣减 sold+ / stock-；</li>
 *   <li>失败补偿：下单未成功则回滚本次扣减，交由消息重试，避免重复扣减。</li>
 * </ol>
 */
@Component
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    private OrderFeignService orderFeignService;
    private SeckillGoodMapper seckillGoodMapper;

    @Autowired
    public void setOrderFeignService(OrderFeignService orderFeignService) {
        this.orderFeignService = orderFeignService;
    }

    @Autowired
    public void setSeckillGoodMapper(SeckillGoodMapper seckillGoodMapper) {
        this.seckillGoodMapper = seckillGoodMapper;
    }

    @RabbitListener(queues = SeckillMqConfig.SECKILL_QUEUE)
    public void onMessage(SeckillGrabMessage msg) {
        try {
            // 1. 幂等：同一秒杀流水号已存在订单 → 视为已处理，直接确认
            TypedJsonResp<OrderEntity> existed = orderFeignService.findBySeckillNo(msg.getSeckillNo());
            if (existed != null && existed.isSuccess() && existed.getData() != null) {
                log.info("秒杀订单已存在，跳过重复消费：seckillNo={}, orderNo={}",
                        msg.getSeckillNo(), existed.getData().getOrderNo());
                return;
            }

            // 2. DB 兜底：原子扣减秒杀库存（sold+、stock-），条件 stock>=qty 防超卖
            int rows = seckillGoodMapper.decreaseStock(msg.getSeckillGoodId(), msg.getQty());
            if (rows == 0) {
                log.error("秒杀 DB 库存不足，成交失败（防超卖兜底）：seckillGoodId={}, qty={}",
                        msg.getSeckillGoodId(), msg.getQty());
                throw new IllegalStateException("秒杀库存不足");
            }

            // 3. 创建秒杀订单（order 侧同样按 seckillNo 幂等）
            SeckillOrderDTO dto = new SeckillOrderDTO();
            dto.setGoodId(msg.getGoodId());
            dto.setQty(msg.getQty());
            dto.setSeckillPrice(msg.getSeckillPrice());
            dto.setMemberAccount(msg.getMemberAccount());
            dto.setSeckillNo(msg.getSeckillNo());

            TypedJsonResp<OrderEntity> resp;
            try {
                resp = orderFeignService.createSeckillOrder(dto);
            } catch (Exception e) {
                // 下单异常：回滚本次库存扣减，避免重投时重复扣减
                seckillGoodMapper.increaseStock(msg.getSeckillGoodId(), msg.getQty());
                log.error("秒杀下单异常，已回滚库存扣减：seckillNo={}", msg.getSeckillNo(), e);
                throw e;
            }
            if (resp == null || !resp.isSuccess()) {
                seckillGoodMapper.increaseStock(msg.getSeckillGoodId(), msg.getQty());
                log.error("秒杀下单失败，已回滚库存扣减，消息重新投递：seckillNo={}, msg={}",
                        msg.getSeckillNo(), resp == null ? "null" : resp.getMsg());
                throw new IllegalStateException("秒杀下单失败");
            }

            log.info("秒杀下单成功：seckillNo={}, orderNo={}", msg.getSeckillNo(),
                    resp.getData() == null ? "-" : resp.getData().getOrderNo());
        } catch (Exception e) {
            log.error("秒杀下单处理异常，消息重投：{}", msg, e);
            throw e; // 触发重试/死信
        }
    }
}
