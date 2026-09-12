package com.situ.jifeng.spi.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀下单入参（秒杀服务 → order 服务，内部调用）。
 */
@Data
public class SeckillOrderDTO {
    /* 商品 */
    private Long goodId;
    /* 数量（通常为 1） */
    private Integer qty;
    /* 秒杀价 */
    private BigDecimal seckillPrice;
    /* 会员账号 */
    private String memberAccount;
    /* 秒杀流水号 */
    private String seckillNo;
}
