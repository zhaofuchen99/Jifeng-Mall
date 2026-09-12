package com.situ.jifeng.spi.model;

import lombok.Data;

/**
 * 扣减/回补库存请求体。
 */
@Data
public class DeductStockDto {
    /* 扣减（或回补）数量 */
    private Integer qty;
}
