package com.situ.jifeng.spi.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class GroupRoleEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 组编号 */
    private Long groupId;
    /* 角色编号 */
    private Long roleId;
}