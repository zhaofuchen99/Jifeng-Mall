package com.situ.jifeng.spi.model;


import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class RbacGroupEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 组名称 */
    private String name;
    /* 备注 */
    private String description;
    /* 是否启用 */
    private Boolean enabled;
}