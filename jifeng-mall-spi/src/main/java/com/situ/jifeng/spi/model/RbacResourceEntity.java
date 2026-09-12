package com.situ.jifeng.spi.model;


import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class RbacResourceEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 资源名称 */
    private String name;
    /* 资源类型。1-路由，2-菜单，3-按钮，4-数据 */
    private String type;
    /* 资源值 */
    private String value;
    /* 备注 */
    private String description;
}