-- =====================================================================
-- 基于 Spring Cloud 的 B2C 微服务秒杀商城
-- 数据库初始化脚本（依据详细设计说明书 V1.0.2 第 3 章）
-- 数据库：shoplook2026   字符集：utf8mb4   引擎：InnoDB
-- 说明：22 张数据表 + 关键索引 + 演示种子数据
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `shoplook2026` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `shoplook2026`;

-- 统一审计字段：created_time / created_by / updated_time / updated_by

-- ---------------------------------------------------------------------
-- 1. brand 商品品牌
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `brand`;
CREATE TABLE `brand` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(30)  NOT NULL COMMENT '品牌名称（唯一）',
  `company`      VARCHAR(255) DEFAULT NULL COMMENT '公司名称',
  `logo`         VARCHAR(255) DEFAULT NULL COMMENT 'Logo URL',
  `site`         VARCHAR(255) DEFAULT NULL COMMENT '网址',
  `description`  VARCHAR(1024) DEFAULT NULL COMMENT '简介',
  `created_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='商品品牌';

-- ---------------------------------------------------------------------
-- 2. category 商品分类（多级树形）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类（0 为根）',
  `name`         VARCHAR(50)  NOT NULL COMMENT '名称',
  `title`        VARCHAR(100) DEFAULT NULL COMMENT '标题',
  `tag`          VARCHAR(100) DEFAULT NULL COMMENT '标签',
  `icon`         VARCHAR(255) DEFAULT NULL COMMENT '图标',
  `summary`      VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `sort`         INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `description`  VARCHAR(1024) DEFAULT NULL COMMENT '简介',
  `created_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_category_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

-- ---------------------------------------------------------------------
-- 3. good 商品（SPU）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `good`;
CREATE TABLE `good` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spu_no`       VARCHAR(30)   NOT NULL COMMENT '货号（唯一）',
  `name`         VARCHAR(100)  NOT NULL COMMENT '名称',
  `alias`        VARCHAR(100)  DEFAULT NULL COMMENT '别名',
  `summary`      VARCHAR(500)  DEFAULT NULL COMMENT '摘要',
  `category_id`  BIGINT        NOT NULL COMMENT '分类',
  `brand_id`     BIGINT        NOT NULL COMMENT '品牌',
  `mark_price`   DECIMAL(10,2) DEFAULT NULL COMMENT '标价',
  `price`        DECIMAL(10,2) NOT NULL COMMENT '实价',
  `qty`          INT           NOT NULL DEFAULT 0 COMMENT '库存',
  `pic`          VARCHAR(255)  DEFAULT NULL COMMENT '主图',
  `pic2`         VARCHAR(255)  DEFAULT NULL COMMENT '次图',
  `detail_pics`  VARCHAR(2000) DEFAULT NULL COMMENT '详情图 URL 集合（逗号分隔）',
  `detail`       TEXT          COMMENT '富文本详情',
  `is_take_down` TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否下架：0上架 1下架',
  `is_hot`       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否热销',
  `is_del`       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  `is_seckill`   TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否秒杀商品',
  `description`  VARCHAR(1024) DEFAULT NULL COMMENT '备注',
  `created_time` DATETIME      DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME      DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_good_spu_no` (`spu_no`),
  KEY `idx_good_category_id` (`category_id`),
  KEY `idx_good_brand_id` (`brand_id`),
  KEY `idx_good_name` (`name`),
  KEY `idx_good_hot_del_take` (`is_hot`,`is_del`,`is_take_down`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='商品（SPU）';

-- ---------------------------------------------------------------------
-- 4. good_detail_pics 商品详情图片
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `good_detail_pics`;
CREATE TABLE `good_detail_pics` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `good_id`      BIGINT       NOT NULL COMMENT '商品',
  `url`          VARCHAR(255) NOT NULL COMMENT '图片 URL',
  `sort`         INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `description`  VARCHAR(255) DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`id`),
  KEY `idx_gdp_good_id` (`good_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='商品详情图片';

-- ---------------------------------------------------------------------
-- 5. cart 购物车
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `member_id`    BIGINT   NOT NULL COMMENT '会员',
  `good_id`      BIGINT   NOT NULL COMMENT '商品',
  `qty`          INT      NOT NULL DEFAULT 1 COMMENT '数量',
  `created_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_member_good` (`member_id`,`good_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

-- ---------------------------------------------------------------------
-- 6. member 会员
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
  `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account`              VARCHAR(30)  NOT NULL COMMENT '登录账号（唯一）',
  `password`             VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
  `enabled`              TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  `name`                 VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  `pinyin`               VARCHAR(100) DEFAULT NULL COMMENT '姓名含音调拼音',
  `pinyin_untoned`       VARCHAR(100) DEFAULT NULL COMMENT '姓名无音调拼音连写',
  `first_name`           VARCHAR(50)  DEFAULT NULL COMMENT '名',
  `last_name`            VARCHAR(50)  DEFAULT NULL COMMENT '姓氏',
  `sex`                  VARCHAR(10)  DEFAULT NULL COMMENT '性别',
  `birthday`             DATE         DEFAULT NULL COMMENT '生日',
  `height`               INT          DEFAULT NULL COMMENT '身高 cm',
  `weight`               DECIMAL(10,2) DEFAULT NULL COMMENT '体重 kg',
  `iq`                   INT          DEFAULT NULL COMMENT '智商',
  `qq`                   VARCHAR(30)  DEFAULT NULL COMMENT 'QQ',
  `wechat`               VARCHAR(50)  DEFAULT NULL COMMENT '微信',
  `phone`                VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `email`                VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `native_place_id`      BIGINT       DEFAULT NULL COMMENT '籍贯地区编号',
  `card_id`              VARCHAR(30)  DEFAULT NULL COMMENT '证件号',
  `wedlock`              VARCHAR(10)  DEFAULT NULL COMMENT '婚姻状况',
  `political_orientation` VARCHAR(30) DEFAULT NULL COMMENT '政治面貌',
  `address_id`           BIGINT       DEFAULT NULL COMMENT '现住地区编号',
  `address_detail`       VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `race`                 VARCHAR(30)  DEFAULT NULL COMMENT '民族',
  `religion`             VARCHAR(30)  DEFAULT NULL COMMENT '宗教',
  `nationality`          VARCHAR(30)  DEFAULT NULL COMMENT '国籍',
  `portrait`             VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `description`          TEXT         COMMENT '简介',
  `extra_info`           JSON         DEFAULT NULL COMMENT '扩展信息',
  `last_login_time`      DATETIME     DEFAULT NULL COMMENT '最近登录时间',
  `last_login_ip`        VARCHAR(64)  DEFAULT NULL COMMENT '最近登录 IP',
  `version`              INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_time`         DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`           VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time`         DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`           VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_account` (`account`),
  KEY `idx_member_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='会员';

-- ---------------------------------------------------------------------
-- 7. member_address 会员收货地址
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `member_address`;
CREATE TABLE `member_address` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `member_account` VARCHAR(30)  NOT NULL COMMENT '会员账号',
  `receiver`       VARCHAR(30)  NOT NULL COMMENT '收货人',
  `phone`          VARCHAR(20)  NOT NULL COMMENT '手机号',
  `addr_id`        BIGINT       NOT NULL COMMENT '所在地区（省市区）',
  `addr_detail`    VARCHAR(255) NOT NULL COMMENT '详细地址',
  `is_default`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认地址',
  `created_time`   DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time`   DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`     VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_ma_member_account` (`member_account`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='会员收货地址';

-- ---------------------------------------------------------------------
-- 8. `order` 订单（含模拟退款状态 refund_status）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`           VARCHAR(32)   NOT NULL COMMENT '订单号（雪花 ID，唯一）',
  `seckill_no`         VARCHAR(32)   DEFAULT NULL COMMENT '秒杀流水号（秒杀订单填写）',
  `member_account`     VARCHAR(30)   NOT NULL COMMENT '下单会员账号',
  `total_pay`          DECIMAL(10,2) NOT NULL COMMENT '应付总额',
  `pay_type`           VARCHAR(20)   DEFAULT NULL COMMENT '支付方式（模拟支付）',
  `alipay_trade_no`    VARCHAR(64)   DEFAULT NULL COMMENT '支付交易号（模拟流水号）',
  `checkout_time`      DATETIME      DEFAULT NULL COMMENT '下单时间',
  `pay_time`           DATETIME      DEFAULT NULL COMMENT '支付时间',
  `ship_time`          DATETIME      DEFAULT NULL COMMENT '发货时间',
  `accept_time`        DATETIME      DEFAULT NULL COMMENT '收货时间',
  `status`             VARCHAR(20)   NOT NULL DEFAULT '待付款' COMMENT '订单状态',
  `receiver_addr_id`   BIGINT        DEFAULT NULL COMMENT '收货地址（快照来源）',
  `receiver_name`      VARCHAR(30)   DEFAULT NULL COMMENT '收货人快照',
  `receiver_phone`     VARCHAR(20)   DEFAULT NULL COMMENT '收货人手机快照',
  `receiver_addr_detail` VARCHAR(255) DEFAULT NULL COMMENT '收货地址快照',
  `order_comment`      VARCHAR(500)  DEFAULT NULL COMMENT '订单备注',
  `refund_status`      VARCHAR(20)   DEFAULT '无退款' COMMENT '退款状态（无退款/退款中/已退款）',
  `is_del`             TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `description`        VARCHAR(1024) DEFAULT NULL COMMENT '备注',
  `created_time`       DATETIME      DEFAULT NULL COMMENT '创建时间',
  `created_by`         VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `updated_time`       DATETIME      DEFAULT NULL COMMENT '更新时间',
  `updated_by`         VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_order_no` (`order_no`),
  KEY `idx_order_member_time` (`member_account`,`created_time`),
  KEY `idx_order_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- ---------------------------------------------------------------------
-- 9. order_item 订单明细
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`     BIGINT        NOT NULL COMMENT '订单',
  `good_id`      BIGINT        NOT NULL COMMENT '商品',
  `deal_price`   DECIMAL(10,2) NOT NULL COMMENT '成交价',
  `count`        INT           NOT NULL COMMENT '数量',
  `good_name`    VARCHAR(100)  DEFAULT NULL COMMENT '商品名称快照',
  `good_pic`     VARCHAR(255)  DEFAULT NULL COMMENT '商品主图快照',
  `good_desc`    VARCHAR(500)  DEFAULT NULL COMMENT '商品描述快照',
  `description`  VARCHAR(1024) DEFAULT NULL COMMENT '备注',
  `created_time` DATETIME      DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME      DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_oi_order_id` (`order_id`),
  KEY `idx_oi_good_id` (`good_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- ---------------------------------------------------------------------
-- 10. seckill 秒杀活动
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `seckill`;
CREATE TABLE `seckill` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(50)  NOT NULL COMMENT '活动名称',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  `start_time`   DATETIME     NOT NULL COMMENT '开始时间',
  `end_time`     DATETIME     NOT NULL COMMENT '结束时间',
  `description`  VARCHAR(1024) DEFAULT NULL COMMENT '说明',
  `created_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_seckill_time` (`start_time`,`end_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动';

-- ---------------------------------------------------------------------
-- 11. seckill_good 秒杀活动商品（扩展秒杀价/库存/已售/限购）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `seckill_good`;
CREATE TABLE `seckill_good` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `seckill_id`   BIGINT        NOT NULL COMMENT '秒杀活动',
  `good_id`      BIGINT        NOT NULL COMMENT '商品',
  `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
  `stock`        INT           NOT NULL DEFAULT 0 COMMENT '秒杀库存',
  `sold`         INT           NOT NULL DEFAULT 0 COMMENT '已售数量',
  `limit_per_user` INT         NOT NULL DEFAULT 1 COMMENT '每人限购数',
  `description`  VARCHAR(1024) DEFAULT NULL COMMENT '说明',
  `created_time` DATETIME      DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME      DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seckill_good` (`seckill_id`,`good_id`),
  KEY `idx_sg_good_id` (`good_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动商品';

-- ---------------------------------------------------------------------
-- 12. t_china_region 行政区划（省/市/区）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_china_region`;
CREATE TABLE `t_china_region` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(50)  NOT NULL COMMENT '名称',
  `parent_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '父级',
  `sort_order`   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `level`        TINYINT      NOT NULL DEFAULT 1 COMMENT '层级（1省/2市/3区）',
  `description`  VARCHAR(255) DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`id`),
  KEY `idx_region_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='行政区划';

-- ---------------------------------------------------------------------
-- 13. user 后台用户
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`              VARCHAR(30)  NOT NULL COMMENT '用户名（唯一）',
  `password`              VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
  `avatar_url`            VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `enabled`               TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  `status`                TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态',
  `locked`                TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否锁定',
  `user_expire_time`      DATETIME     DEFAULT NULL COMMENT '账号过期时间',
  `credential_expire_time` DATETIME    DEFAULT NULL COMMENT '凭证过期时间',
  `login_times`           INT          NOT NULL DEFAULT 0 COMMENT '登录次数',
  `last_login_time`       DATETIME     DEFAULT NULL COMMENT '最近登录时间',
  `last_login_ip`         VARCHAR(64)  DEFAULT NULL COMMENT '最近登录 IP',
  `op_mode`               INT          DEFAULT NULL COMMENT '操作模式',
  `description`           VARCHAR(1024) DEFAULT NULL COMMENT '说明',
  `created_time`          DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`            VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time`          DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`            VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='后台用户';

-- ---------------------------------------------------------------------
-- 14. t_rbac_group 用户组
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_rbac_group`;
CREATE TABLE `t_rbac_group` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(50)  NOT NULL COMMENT '名称（唯一）',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  `description`  VARCHAR(1024) DEFAULT NULL COMMENT '说明',
  `created_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_group_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户组';

-- ---------------------------------------------------------------------
-- 15. t_rbac_role 角色
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_rbac_role`;
CREATE TABLE `t_rbac_role` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(50)  NOT NULL COMMENT '名称（唯一）',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  `description`  VARCHAR(1024) DEFAULT NULL COMMENT '说明',
  `created_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_role_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- ---------------------------------------------------------------------
-- 16. t_rbac_perm 权限
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_rbac_perm`;
CREATE TABLE `t_rbac_perm` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(50)  NOT NULL COMMENT '名称（唯一）',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  `description`  VARCHAR(1024) DEFAULT NULL COMMENT '说明',
  `created_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_perm_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='权限';

-- ---------------------------------------------------------------------
-- 17. t_rbac_resource 资源（接口/按钮）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_rbac_resource`;
CREATE TABLE `t_rbac_resource` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(100) NOT NULL COMMENT '名称',
  `type`         VARCHAR(20)  NOT NULL COMMENT '类型（接口/按钮）',
  `value`        VARCHAR(255) NOT NULL COMMENT '值（路径或按钮标识）',
  `description`  VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `created_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_resource_type_value` (`type`,`value`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='资源（接口/按钮）';

-- ---------------------------------------------------------------------
-- 18. t_rbac_menu 菜单
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_rbac_menu`;
CREATE TABLE `t_rbac_menu` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单（0 为根）',
  `resource_id`  BIGINT       DEFAULT NULL COMMENT '关联资源',
  `name`         VARCHAR(50)  NOT NULL COMMENT '菜单名称',
  `icon`         VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `url`          VARCHAR(255) DEFAULT NULL COMMENT 'URL',
  `sort`         INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `description`  VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `created_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_rbac_menu_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='菜单';

-- ---------------------------------------------------------------------
-- 19. t_rbac_user_group 用户-组 关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_rbac_user_group`;
CREATE TABLE `t_rbac_user_group` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT   NOT NULL COMMENT '用户',
  `group_id`     BIGINT   NOT NULL COMMENT '用户组',
  `created_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_user_group` (`user_id`,`group_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户-组关联';

-- ---------------------------------------------------------------------
-- 20. t_rbac_group_role 组-角色 关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_rbac_group_role`;
CREATE TABLE `t_rbac_group_role` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id`     BIGINT   NOT NULL COMMENT '用户组',
  `role_id`      BIGINT   NOT NULL COMMENT '角色',
  `created_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_group_role` (`group_id`,`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='组-角色关联';

-- ---------------------------------------------------------------------
-- 21. t_rbac_role_perm 角色-权限 关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_rbac_role_perm`;
CREATE TABLE `t_rbac_role_perm` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`      BIGINT   NOT NULL COMMENT '角色',
  `perm_id`      BIGINT   NOT NULL COMMENT '权限',
  `created_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_role_perm` (`role_id`,`perm_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联';

-- ---------------------------------------------------------------------
-- 22. t_rbac_perm_resource 权限-资源 关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_rbac_perm_resource`;
CREATE TABLE `t_rbac_perm_resource` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resource_id`  BIGINT   NOT NULL COMMENT '资源',
  `perm_id`      BIGINT   NOT NULL COMMENT '权限',
  `created_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `created_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `updated_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rbac_perm_resource` (`resource_id`,`perm_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='权限-资源关联';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 种子数据（演示用）
-- =====================================================================

-- 后台超级管理员：admin / 123456（BCrypt）
INSERT INTO `user` (`username`,`password`,`enabled`,`status`,`locked`,`op_mode`,
                    `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES ('admin', '$2b$10$/9iXAHzHBJLPTaO2hlBgIeyhz9lUvsnFw0Dy4GFKw2da4qKSnD/.e',
        1, 1, 0, 1, NOW(), 'system', NOW(), 'system');
INSERT INTO `user` (`username`,`password`,`enabled`,`status`,`locked`,`op_mode`,
                    `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES ('operator', '$2b$10$/9iXAHzHBJLPTaO2hlBgIeyhz9lUvsnFw0Dy4GFKw2da4qKSnD/.e',
        1, 1, 0, 1, NOW(), 'system', NOW(), 'system');

-- 演示会员：member / 123456
INSERT INTO `member` (`account`,`password`,`enabled`,`name`,`version`,
                      `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES ('member', '$2b$10$/9iXAHzHBJLPTaO2hlBgIeyhz9lUvsnFw0Dy4GFKw2da4qKSnD/.e',
        1, '测试会员', 0, NOW(), 'system', NOW(), 'system');

-- 演示分类（一级）
INSERT INTO `category` (`parent_id`,`name`,`title`,`sort`,`description`,
                        `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES (0, '手机数码', '手机数码', 1, '手机数码类', NOW(), 'system', NOW(), 'system'),
       (0, '家用电器', '家用电器', 2, '家用电器类', NOW(), 'system', NOW(), 'system'),
       (0, '服饰鞋包', '服饰鞋包', 3, '服饰鞋包类', NOW(), 'system', NOW(), 'system');

-- 演示品牌
INSERT INTO `brand` (`name`,`company`,`site`,`description`,
                     `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES ('Apple', '苹果公司', 'https://www.apple.com', '科技品牌', NOW(), 'system', NOW(), 'system'),
       ('Huawei', '华为技术有限公司', 'https://www.huawei.com', '科技品牌', NOW(), 'system', NOW(), 'system');

-- 演示商品（含热销、秒杀标记）
INSERT INTO `good` (`spu_no`,`name`,`alias`,`category_id`,`brand_id`,`mark_price`,`price`,`qty`,
                    `pic`,`is_take_down`,`is_hot`,`is_del`,`is_seckill`,`description`,
                    `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES ('SPU1001', 'iPhone 16 Pro', '苹果16pro', 1, 1, 9999.00, 8999.00, 100,
        '/upload/good/iphone.png', 0, 1, 0, 1, '演示热销+秒杀商品', NOW(), 'system', NOW(), 'system'),
       ('SPU1002', 'Mate 70 Pro', '华为mate70', 1, 2, 7999.00, 6999.00, 200,
        '/upload/good/mate.png', 0, 1, 0, 0, '演示热销商品', NOW(), 'system', NOW(), 'system');

-- 演示秒杀活动（开始时间 = 建库时刻 -1h，结束时间 = 建库时刻 +30 天）
-- 注意：这里的 NOW() 是建库那一刻的时间，是写死的。窗口不能太短，否则过几天再启动服务时
-- seckill-api 的预热任务（SeckillStockPreheatTask 走 findActive 的 end_time > now()）会查不到活动，
-- 表现为 Redis 里没有 seckill:stock:* 键、且不会有任何日志，排查起来很隐蔽。
-- 若库已建好而活动已过期，直接执行：
--   UPDATE seckill SET start_time = NOW() - INTERVAL 1 HOUR, end_time = NOW() + INTERVAL 24 HOUR WHERE id = 1;
INSERT INTO `seckill` (`name`,`enabled`,`start_time`,`end_time`,`description`,
                       `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES ('每周三秒杀', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 30 DAY),
        '演示秒杀活动', NOW(), 'system', NOW(), 'system');

-- 演示秒杀活动商品
INSERT INTO `seckill_good` (`seckill_id`,`good_id`,`seckill_price`,`stock`,`sold`,`limit_per_user`,`description`,
                            `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES (1, 1, 5999.00, 50, 0, 1, '演示秒杀商品，限购1件', NOW(), 'system', NOW(), 'system');

-- 行政区划（示例：广东省/广州市/天河区 三级）
INSERT INTO `t_china_region` (`id`,`name`,`parent_id`,`sort_order`,`level`,`description`)
VALUES (440000, '广东省', 0, 1, 1, '省'),
       (440100, '广州市', 440000, 1, 2, '市'),
       (440106, '天河区', 440100, 1, 3, '区'),
       (110000, '北京市', 0, 2, 1, '省'),
       (110100, '北京市市辖区', 110000, 1, 2, '市');

-- RBAC 角色（超级管理员/运营）
INSERT INTO `t_rbac_role` (`name`,`enabled`,`description`,
                           `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES ('超级管理员', 1, '全部权限', NOW(), 'system', NOW(), 'system'),
       ('运营人员', 1, '运营权限', NOW(), 'system', NOW(), 'system');

-- RBAC 用户组（管理员组）
INSERT INTO `t_rbac_group` (`name`,`enabled`,`description`,
                            `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES ('管理员组', 1, '系统管理员组', NOW(), 'system', NOW(), 'system');

-- 用户-组关联：admin 加入 管理员组
INSERT INTO `t_rbac_user_group` (`user_id`,`group_id`,`created_time`,`created_by`)
VALUES (1, 1, NOW(), 'system');

-- 组-角色 关联：管理员组 绑定 超级管理员角色
INSERT INTO `t_rbac_group_role` (`group_id`,`role_id`,`created_time`,`created_by`)
VALUES (1, 1, NOW(), 'system');

-- 后台菜单（演示，超级管理员全部可见）
INSERT INTO `t_rbac_menu` (`parent_id`,`resource_id`,`name`,`icon`,`url`,`sort`,`description`,
                           `created_time`,`created_by`,`updated_time`,`updated_by`)
VALUES (0, 1001, '工作台', 'el-icon-house', '/dashboard', 1, '首页', NOW(), 'system', NOW(), 'system'),
       (0, 1002, '商品管理', 'el-icon-goods', '/goods', 2, '商品中心', NOW(), 'system', NOW(), 'system'),
       (0, 1003, '订单管理', 'el-icon-document', '/orders', 3, '交易中心', NOW(), 'system', NOW(), 'system'),
       (0, 1004, '会员管理', 'el-icon-user', '/members', 4, '会员中心', NOW(), 'system', NOW(), 'system'),
       (0, 1005, '秒杀管理', 'el-icon-timer', '/seckills', 5, '秒杀中心', NOW(), 'system', NOW(), 'system'),
       (0, 1006, '系统管理', 'el-icon-setting', '/system', 6, 'RBAC', NOW(), 'system', NOW(), 'system');

-- 接口/按钮资源（演示）。value 为接口路径（Ant 风格），type='接口'/'按钮'
INSERT INTO `t_rbac_resource` (`id`,`name`,`type`,`value`,`description`,`created_time`,`created_by`)
VALUES
    (1001, '工作台',   '菜单', '/dashboard', '工作台菜单', NOW(), 'system'),
    (1002, '商品管理', '菜单', '/goods',     '商品菜单',   NOW(), 'system'),
    (1003, '订单管理', '菜单', '/orders',    '订单菜单',   NOW(), 'system'),
    (1004, '会员管理', '菜单', '/members',   '会员菜单',   NOW(), 'system'),
    (1005, '秒杀管理', '菜单', '/seckills',  '秒杀菜单',   NOW(), 'system'),
    (1006, '系统管理', '菜单', '/system',    '系统菜单',   NOW(), 'system'),
    (2001, '品牌-接口',  '接口', '/api/brands/**', '品牌接口', NOW(), 'system'),
    (2002, '分类-接口',  '接口', '/api/categories/**', '分类接口', NOW(), 'system'),
    (2003, '商品-查询',  '接口', '/api/goods', '商品查询写接口', NOW(), 'system'),
    (2004, '商品-详情',  '接口', '/api/goods/**', '商品详情接口', NOW(), 'system'),
    (2005, '订单-接口',  '接口', '/api/orders/**', '订单接口', NOW(), 'system'),
    (2006, '会员-接口',  '接口', '/api/members/**', '会员接口', NOW(), 'system'),
    (2007, '秒杀-接口',  '接口', '/api/seckills/**', '秒杀接口', NOW(), 'system'),
    (2008, '地址-接口',  '接口', '/api/member-addresses/**', '会员地址接口', NOW(), 'system');

-- 补充的接口资源（2009~2020）。
-- 原先只授权了 8 个接口前缀，/api/roles、/api/users、/api/menus 等系统管理接口没有任何资源行。
-- 网关 RBAC 过滤器修好之后，缺这些行会让 admin 访问系统管理模块全部 403。
INSERT INTO `t_rbac_resource` (`id`,`name`,`type`,`value`,`description`,`created_time`,`created_by`)
VALUES
    (2009, '后台用户-接口', '接口', '/api/users/**',            '后台用户管理接口', NOW(), 'system'),
    (2010, '角色-接口',     '接口', '/api/roles/**',            '角色管理接口',     NOW(), 'system'),
    (2011, '用户组-接口',   '接口', '/api/groups/**',           '用户组管理接口',   NOW(), 'system'),
    (2012, '权限-接口',     '接口', '/api/perms/**',            '权限管理接口',     NOW(), 'system'),
    (2013, '资源-接口',     '接口', '/api/resources/**',        '资源管理接口',     NOW(), 'system'),
    (2014, '菜单-接口',     '接口', '/api/menus/**',            '菜单管理接口',     NOW(), 'system'),
    (2015, '用户组关联-接口','接口','/api/user-groups/**',      '用户-用户组关联接口', NOW(), 'system'),
    (2016, '组角色关联-接口','接口','/api/group-roles/**',      '用户组-角色关联接口', NOW(), 'system'),
    (2017, '角色权限关联-接口','接口','/api/role-perms/**',     '角色-权限关联接口', NOW(), 'system'),
    (2018, '权限资源关联-接口','接口','/api/perm-resources/**', '权限-资源关联接口', NOW(), 'system'),
    (2019, '秒杀商品-接口', '接口', '/api/seckill-goods/**',    '秒杀商品管理接口', NOW(), 'system'),
    (2020, '订单明细-接口', '接口', '/api/order-items/**',      '订单明细查询接口', NOW(), 'system');

-- 权限
INSERT INTO `t_rbac_perm` (`id`,`name`,`description`,`enabled`,`created_time`,`created_by`)
VALUES (301, '商品管理权限', '商品中心全部操作', 1, NOW(), 'system'),
       (302, '订单管理权限', '交易中心全部操作', 1, NOW(), 'system'),
       (303, '会员管理权限', '会员中心全部操作', 1, NOW(), 'system'),
       (304, '秒杀管理权限', '秒杀中心全部操作', 1, NOW(), 'system'),
       (305, '系统管理权限', 'RBAC 全部操作',     1, NOW(), 'system');

-- 角色-权限（超管角色1 拥有全部权限）
INSERT INTO `t_rbac_role_perm` (`role_id`,`perm_id`,`created_time`,`created_by`)
VALUES (1, 301, NOW(), 'system'), (1, 302, NOW(), 'system'), (1, 303, NOW(), 'system'),
       (1, 304, NOW(), 'system'), (1, 305, NOW(), 'system');

-- 权限-资源（权限 → 菜单/接口资源）
INSERT INTO `t_rbac_perm_resource` (`resource_id`,`perm_id`,`created_time`,`created_by`)
VALUES
    (1001, 305, NOW(), 'system'), (1002, 301, NOW(), 'system'), (1003, 302, NOW(), 'system'),
    (1004, 303, NOW(), 'system'), (1005, 304, NOW(), 'system'), (1006, 305, NOW(), 'system'),
    (2001, 301, NOW(), 'system'), (2002, 301, NOW(), 'system'), (2003, 301, NOW(), 'system'),
    (2004, 301, NOW(), 'system'), (2005, 302, NOW(), 'system'), (2006, 303, NOW(), 'system'),
    (2007, 304, NOW(), 'system'), (2008, 303, NOW(), 'system'),
    -- 补充资源对应的授权：系统管理类挂 305，秒杀商品挂 304，订单明细挂 302
    (2009, 305, NOW(), 'system'), (2010, 305, NOW(), 'system'),
    (2011, 305, NOW(), 'system'), (2012, 305, NOW(), 'system'),
    (2013, 305, NOW(), 'system'), (2014, 305, NOW(), 'system'),
    (2015, 305, NOW(), 'system'), (2016, 305, NOW(), 'system'),
    (2017, 305, NOW(), 'system'), (2018, 305, NOW(), 'system'),
    (2019, 304, NOW(), 'system'), (2020, 302, NOW(), 'system');

-- =====================================================================
-- 完成
-- =====================================================================
