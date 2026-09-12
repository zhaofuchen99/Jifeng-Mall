package com.situ.jifeng.spi.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@JsonIgnoreProperties("handler")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class CategoryEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 父类别编号 */
    private Long parentId;
    /* 类别名称 */
    private String name;
    /* 类别标题 */
    private String title;
    /* 类别标签 */
    private String tag;
    /* 类别样式 */
    private String icon;
    /* 类别摘要 */
    private String summary;
    /* 排序号 */
    private Integer sort;
    /* 备注 */
    private String description;

    //父类别
    @JsonBackReference
    private CategoryEntity parent;

    //子类别
    @JsonManagedReference
    private List<CategoryEntity> children;
}
