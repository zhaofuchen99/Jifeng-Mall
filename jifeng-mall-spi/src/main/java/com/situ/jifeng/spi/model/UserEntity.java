package com.situ.jifeng.spi.model;


import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class UserEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 编号，主键 */
    private Long id;
    /* 用户名(账号)，唯一 */
    private String username;
    /* 密码，存放加密字符串 */
    private String password;
    /* 头像地址 */
    private String avatarUrl;
    /* 是否启用，默认1启用，0未启用 */
    private Boolean enabled;
    /* 状态。0：正常，1：异常 */
    private Integer status;
    /* 锁定状态，0：未锁定，1：已锁定 */
    private Boolean locked;
    /* 账号过期时间 */
    private LocalDateTime userExpireTime;
    /* 权限过期时间 */
    private LocalDateTime credentialExpireTime;
    /* 登录次数 */
    private Integer loginTimes;
    /* 上次登录时间 */
    private LocalDateTime lastLoginTime;
    /* 上次登录ip */
    private String lastLoginIp;
    /* 备注 */
    private String description;
    /* 操作权限：1-可删除，2-可修改，4-可读取 */
    private Integer opMode;
}