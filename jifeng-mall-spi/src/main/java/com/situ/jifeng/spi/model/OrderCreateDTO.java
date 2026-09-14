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
    /*
     * 下单会员主键。由 OrderApi 从网关注入的 X-User-Id 填入，**不接受前端携带**。
     * 用途是校验购物车条目的归属：CartItemEntity 里存的是 memberId（Long），
     * 而订单存的是 memberAccount（String），两边对不上就没法比对。
     * 为 null 表示非会员请求（服务间调用/后台），此时跳过购物车归属校验。
     */
    private Long memberId;
}
