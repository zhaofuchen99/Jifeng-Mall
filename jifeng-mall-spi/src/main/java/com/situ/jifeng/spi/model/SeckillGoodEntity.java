package com.situ.jifeng.spi.model;


import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class SeckillGoodEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 秒杀活动 */
    private Long seckillId;
    /* 商品 */
    private Long goodId;
    /* 秒杀价 */
    private BigDecimal seckillPrice;
    /* 秒杀库存 */
    private Integer stock;
    /* 已售数量 */
    private Integer sold;
    /* 每人限购数 */
    private Integer limitPerUser;
    /* 说明 */
    private String description;
}
