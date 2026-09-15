package com.situ.jifeng.spi.model;


import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 首页轮播 Banner（需求 5.3 / FR-102：可配置图片与跳转链接）。
 *
 * <p>设计说明书没有为轮播建模，本表是本期补充，详见
 * {@code docs/sql/jifeng-mall-init.sql} 第 23 节的说明。</p>
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class BannerEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* 标题：前台可作文字展示，后台用于辨识 */
    private String title;
    /* 轮播图片地址；为空时前台渲染渐变兜底 */
    private String imageUrl;
    /* 点击跳转地址（站内路由或外链） */
    private String linkUrl;
    /* 排序号，小的在前 */
    private Integer sortNo;
    /* 是否启用；停用后前台不展示 */
    private Boolean enabled;
    /* 备注 */
    private String description;
}
