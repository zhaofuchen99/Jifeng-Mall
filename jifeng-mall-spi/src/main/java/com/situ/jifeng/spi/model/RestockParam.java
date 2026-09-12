package com.situ.jifeng.spi.model;

import lombok.Data;

/**
 * 秒杀库存回补入参（order 关单 → seckill 服务）。
 */
@Data
public class RestockParam {
    /* 秒杀流水号 */
    private String seckillNo;
}
