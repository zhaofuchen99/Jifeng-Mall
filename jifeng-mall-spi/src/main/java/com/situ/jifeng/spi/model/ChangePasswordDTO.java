package com.situ.jifeng.spi.model;

import lombok.Data;

/**
 * 会员自助改密码的请求体。
 *
 * <p>要求带上旧密码，是为了让<b>服务端</b>校验——原先前台是「先用旧密码走一次登录接口」
 * 来间接验证的（见旧版 ChangePassword.vue），那个校验只在客户端，绕过前端直接发请求就能跳过。
 * 令牌被盗时，「不知道旧密码也能改掉密码」会把真正的会员彻底锁在门外，所以这道校验必须在服务端。</p>
 */
@Data
public class ChangePasswordDTO {
    /* 当前密码 */
    private String oldPassword;
    /* 新密码 */
    private String newPassword;
}
