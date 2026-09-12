package com.situ.jifeng.spi.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class OrderItemEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 订单编号 */
    private Long orderId;
    /* 商品编号 */
    private Long goodId;
    /* 成交价格 */
    private BigDecimal dealPrice;
    /* 数量 */
    private Integer count;
    /* 商品名称 */
    private String goodName;
    /* 商品图片 */
    private String goodPic;
    /* 商品摘要 */
    private String goodDesc;
    /* 订单项备注 */
    private String description;

    //商品
    private GoodEntity good;
}