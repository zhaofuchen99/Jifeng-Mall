package com.situ.jifeng.spi.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class CartItemEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 会员编号 */
    private Long memberId;
    /* 商品编号。如果有sku，则关联sku表 */
    private Long goodId;
    /* 数量 */
    private Integer qty;

    //商品
    private GoodEntity good;
}