package com.situ.jifeng.spi.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class PermResourceEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 资源编号 */
    private Long resourceId;
    /* 权限编号 */
    private Long permId;
}