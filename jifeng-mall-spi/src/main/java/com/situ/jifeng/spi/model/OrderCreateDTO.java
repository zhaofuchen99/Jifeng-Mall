package com.situ.jifeng.spi.model;

import lombok.Data;

import java.util.List;

/**
 * 下单入参（依据设计文档 4.6.2）。
 *
 * <p>来源两种：a) 购物车勾选项批量下单（cartIds）；b) 立即购买单件下单（goodId + qty）。</p>
 */
@Data
public class OrderCreateDTO {
    /* 购物车勾选条目 id 集合（来自购物车下单） */
    private List<Long> cartIds;
    /* 立即购买：商品 id */
    private Long goodId;
    /* 立即购买：数量 */
    private Integer qty;
    /* 收货地址 id */
    private Long addrId;
    /* 订单备注 */
    private String comment;
    /* 下单会员账号（由网关/登录上下文注入，也可前端携带） */
    private String memberAccount;
}
