package com.situ.jifeng.spi.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class RolePermEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 角色编号 */
    private Long roleId;
    /* 权限编号 */
    private Long permId;
}