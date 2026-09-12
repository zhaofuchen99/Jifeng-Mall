package com.situ.jifeng.spi.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class OrderEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 订单编号，雪花算法生成 */
    private String orderNo;
    /* 秒杀编号，雪花算法生成 */
    private String seckillNo;
    /* 会员账号 */
    private String memberAccount;
    /* 订单总价 */
    private BigDecimal totalPay;
    /* 支付方式 */
    private String payType;
    /* 支付宝交易号 */
    private String alipayTradeNo;
    /* 下单时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkoutTime;
    /* 支付宝支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    /* 发货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    /* 确认收货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptTime;
    /* 订单状态 */
    private String status;
    /* 收货人地址编号，对应会员地址编号 */
    private Long receiverAddrId;
    /* 收货人姓名 */
    private String receiverName;
    /* 收货人手机号 */
    private String receiverPhone;
    /* 收货人地址（完整） */
    private String receiverAddrDetail;
    /* 订单备注，客户用 */
    private String orderComment;
    /* 退款状态（无退款/退款中/已退款，模拟退款用） */
    private String refundStatus;
    /* 是否逻辑删除 */
    private Boolean isDel;
    /* 备注，平台用 */
    private String description;
}