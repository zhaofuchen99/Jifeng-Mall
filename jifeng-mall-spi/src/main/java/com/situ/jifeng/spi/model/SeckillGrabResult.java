package com.situ.jifeng.spi.model;

import lombok.Builder;
import lombok.Data;

/**
 * 秒杀抢购结果。
 */
@Data
@Builder
public class SeckillGrabResult {
    /* 状态码：200 成功；7002 未开始/已结束；7003 已参与/超限购；7004 已售罄 */
    private int code;
    /* 提示消息 */
    private String msg;
    /* 生成的秒杀订单号（成功/异步处理中时返回，供前端轮询结果） */
    private String orderNo;
    /* 是否抢购提交成功（已进入异步下单/支付流程） */
    private boolean success;
}
