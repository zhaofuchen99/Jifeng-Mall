package com.situ.jifeng.spi.model;


import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class RbacMenuEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 父菜单编号 */
    private Long parentId;
    /* 资源编号 */
    private Long resourceId;
    /* 菜单名称 */
    private String name;
    /* 菜单图标 */
    private String icon;
    /* 菜单链接地址 */
    private String url;
    /* 顺序号 */
    private Integer sort;
    /* 备注 */
    private String description;

    // 子菜单（动态菜单树）
    private List<RbacMenuEntity> children;
}