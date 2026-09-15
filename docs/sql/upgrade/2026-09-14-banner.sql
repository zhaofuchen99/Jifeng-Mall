-- =====================================================================
-- 增量变更：新增 banner 表（第 23 张，设计说明书未建模）+ 3 条演示轮播
--
-- 适用：2026-09-14 之前创建的库。
--
-- 背景：需求 5.3 / FR-102 要求「轮播 Banner（可配置图片与跳转链接）」，
--   而详细设计说明书第 3 章没有这张表、2.4 节的 13 个服务清单里也没有对应服务。
--   为不擅自新增服务（服务清单是对外承诺），轮播并入**商品中心 good-api**，
--   接口挂在 `/api/goods/banners` —— 这样能直接复用网关已有的
--   「GET /api/goods/** 公开」白名单与商品管理权限资源，
--   不用改网关、不用补 RBAC 种子。
--
-- 幂等：CREATE TABLE IF NOT EXISTS；种子用 NOT EXISTS 保护（banner 无唯一约束）。
-- =====================================================================

SET NAMES utf8mb4;

-- 1) 建表
CREATE TABLE IF NOT EXISTS `banner` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`        VARCHAR(100) NOT NULL COMMENT '标题（前台可作副标题展示，后台用于辨识）',
  `image_url`    VARCHAR(255) DEFAULT NULL COMMENT '轮播图片地址；为空时前台渲染纯色/渐变兜底',
  `link_url`     VARCHAR(255) DEFAULT NULL COMMENT '点击跳转地址（站内路由如 /goods，或外链）',
  `sort_no`      INT          NOT NULL DEFAULT 0 COMMENT '排序号，小的在前',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用（停用后前台不展示）',
  `description`  VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_banner_enabled_sort` (`enabled`,`sort_no`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播 Banner';

-- 2) 演示种子（3 条）
--    图片放在 my.upload.location 下的 banner/ 目录，生成脚本见 docs/首页轮播图来源.md；
--    image_url 留空时前台渲染渐变兜底，不会开天窗。
--    仅当表里一条数据都没有时才插入，避免覆盖运维自己配的轮播。
INSERT INTO `banner` (`title`,`image_url`,`link_url`,`sort_no`,`enabled`,`description`,
                      `created_time`,`created_by`,`updated_time`,`updated_by`)
SELECT * FROM (SELECT
        '新品首发 · 极锋商城'    AS title,  '/upload/banner/banner-1.png' AS image_url,
        '/goods'                AS link_url, 1 AS sort_no, 1 AS enabled,
        '首页第一条轮播，跳商品列表' AS description,
        NOW() AS created_time, 'system' AS created_by, NOW() AS updated_time, 'system' AS updated_by
    UNION ALL SELECT
        '手机专享 · 立减不止一点', '/upload/banner/banner-2.png',
        '/goods/1', 2, 1, '跳商品详情', NOW(), 'system', NOW(), 'system'
    UNION ALL SELECT
        '限时秒杀 · 每天一场',     '/upload/banner/banner-3.png',
        '/seckill', 3, 1, '跳秒杀会场', NOW(), 'system', NOW(), 'system'
) seed
WHERE NOT EXISTS (SELECT 1 FROM `banner`);

-- 3) 核对
SELECT 'banner 表应有 3 条演示数据' AS check_item;
SELECT id, title, image_url, link_url, sort_no, enabled FROM `banner` ORDER BY sort_no;
