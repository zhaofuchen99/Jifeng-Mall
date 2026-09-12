package com.situ.jifeng.spi.model;


import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class BrandEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 品牌名称 */
    private String name;
    /* 公司名称 */
    private String company;
    /* logo */
    private String logo;
    /* 网址 */
    private String site;
    /* 简介 */
    private String description;
}
