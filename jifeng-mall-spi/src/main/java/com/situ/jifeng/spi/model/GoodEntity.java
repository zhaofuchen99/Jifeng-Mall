package com.situ.jifeng.spi.model;


import com.situ.jifeng.common.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
public class GoodEntity extends AuditEntity {
    @EqualsAndHashCode.Include
    /* 主键 */
    private Long id;
    /* spu编号 */
    private String spuNo;
    /* 商品名称 */
    private String name;
    /* 商品别名 */
    private String alias;
    /* 商品摘要 */
    private String summary;
    /* 所属分类编号 */
    private Long categoryId;
    /* 所属品牌编号 */
    private Long brandId;
    /* 建议售价，标售价 */
    private BigDecimal markPrice;
    /* 实售价 */
    private BigDecimal price;
    /* 库存数量，如有单独的库存表，则以库存表为准 */
    private Integer qty;
    /* 主图 */
    private String pic;
    /* 次图 */
    private String pic2;
    /* 详情图，多副详情图使用逗号分隔 */
    private String detailPics;
    /* 详情，富文本编辑器内容，可覆盖spu详情 */
    private String detail;
    /* 是否下架 */
    private Boolean isTakeDown;
    /* 是否热销 */
    private Boolean isHot;
    /* 是否删除 */
    private Boolean isDel;
    /* 是否秒杀商品 */
    private Boolean isSeckill;
    /* 备注 */
    private String description;

    //品牌
    private BrandEntity brand;
    //分类
    private CategoryEntity category;
}
