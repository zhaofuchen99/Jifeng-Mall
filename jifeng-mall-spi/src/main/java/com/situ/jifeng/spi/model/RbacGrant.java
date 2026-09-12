package com.situ.jifeng.spi.model;

import lombok.Builder;
import lombok.Data;

/**
 * 权限判定结果（返回给网关）。
 */
@Data
@Builder
public class RbacGrant {
    /* 是否放行 */
    private boolean allowed;
    /* 命中资源（可选，便于排查） */
    private String resource;
}
