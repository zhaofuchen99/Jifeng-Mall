package com.situ.jifeng.seckill.config;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀抢购 → 异步下单的消息体。
 */
@Data
@Builder
public class SeckillGrabMessage implements Serializable {
    /* 秒杀流水号 */
    private String seckillNo;
    /* 秒杀活动商品 id */
    private Long seckillGoodId;
    /* 商品 id */
    private Long goodId;
    /* 秒杀价 */
    private BigDecimal seckillPrice;
    /* 购买数量 */
    private Integer qty;
    /* 会员 id */
    private Long memberId;
    /* 会员账号 */
    private String memberAccount;
}
