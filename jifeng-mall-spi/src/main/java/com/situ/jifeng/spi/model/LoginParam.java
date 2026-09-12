package com.situ.jifeng.spi.model;

import lombok.Data;

/**
 * 登录请求参数（member 用 account，admin 用 username，二者均赋值到 account 字段）。
 */
@Data
public class LoginParam {
    /* 登录账号（member.account / user.username） */
    private String account;
    /* 密码 */
    private String password;
}
