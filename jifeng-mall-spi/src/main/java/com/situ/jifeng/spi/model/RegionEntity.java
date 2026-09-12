package com.situ.jifeng.spi.model;


import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class RegionEntity {
    @EqualsAndHashCode.Include
    /* 自增ID */
    private Long id;
    /* 名称 */
    private String name;
    /* 上级ID */
    private Long parentId;
    /* 排序 */
    private Integer sortOrder;
    /* 级别 */
    private Integer level;
    /* 备注 */
    private String description;

    //父地区
    private RegionEntity parent;

    public String getFullName() {
        if (getParent() != null) {
            return getParent().getFullName() + getName();
        } else {
            return getName();
        }
    }
}