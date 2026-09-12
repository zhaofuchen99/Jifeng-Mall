package com.situ.jifeng.spi.model;

import lombok.Data;

/**
 * 权限判定入参（网关调用 rbac-api 时传入，依据设计文档 4.6.5）。
 */
@Data
public class RbacCheckParam {
    /* 后台用户主键 */
    private Long userId;
    /* 请求路径，如 /api/orders/ship */
    private String path;
    /* 请求方法，如 PUT */
    private String method;
}
