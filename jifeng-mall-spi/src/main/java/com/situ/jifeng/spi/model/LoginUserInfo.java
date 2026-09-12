package com.situ.jifeng.spi.model;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功返回给前端的模型：token + 用户基础信息。
 */
@Data
@Builder
public class LoginUserInfo {
    /* JWT 令牌 */
    private String token;
    /* 用户/会员主键 */
    private Long userId;
    /* 登录账号 */
    private String account;
    /* 显示名 */
    private String name;
    /* 令牌类型（admin / member） */
    private String audience;
}
