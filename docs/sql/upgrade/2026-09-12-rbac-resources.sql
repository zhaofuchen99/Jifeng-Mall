-- =====================================================================
-- 增量变更：补充 RBAC 接口资源 2009~2020 及其授权
--
-- 适用：2026-09-12 之前创建的库。
--
-- 背景：网关的 RBAC 越权判定原先因三处缺陷形同虚设（详见 docs/测试报告.md §8.3），
--   修复后判定真正生效 —— 但种子数据只授权了 8 个接口前缀，
--   /api/roles、/api/users、/api/menus、四张关联表等**没有任何资源行**，
--   于是「只修网关不补数据」会让管理员访问系统管理模块**全部 403**。
--
-- 幂等：t_rbac_resource 用主键 INSERT IGNORE；
--       t_rbac_perm_resource 有 UNIQUE(resource_id, perm_id)，同样 IGNORE。
-- =====================================================================

SET NAMES utf8mb4;

-- 1) 接口资源
INSERT IGNORE INTO `t_rbac_resource` (`id`,`name`,`type`,`value`,`description`,`created_time`,`created_by`)
VALUES
    (2009, '后台用户-接口',    '接口', '/api/users/**',            '后台用户管理接口',   NOW(), 'system'),
    (2010, '角色-接口',        '接口', '/api/roles/**',            '角色管理接口',       NOW(), 'system'),
    (2011, '用户组-接口',      '接口', '/api/groups/**',           '用户组管理接口',     NOW(), 'system'),
    (2012, '权限-接口',        '接口', '/api/perms/**',            '权限管理接口',       NOW(), 'system'),
    (2013, '资源-接口',        '接口', '/api/resources/**',        '资源管理接口',       NOW(), 'system'),
    (2014, '菜单-接口',        '接口', '/api/menus/**',            '菜单管理接口',       NOW(), 'system'),
    (2015, '用户组关联-接口',  '接口', '/api/user-groups/**',      '用户-用户组关联接口', NOW(), 'system'),
    (2016, '组角色关联-接口',  '接口', '/api/group-roles/**',      '用户组-角色关联接口', NOW(), 'system'),
    (2017, '角色权限关联-接口','接口', '/api/role-perms/**',       '角色-权限关联接口',  NOW(), 'system'),
    (2018, '权限资源关联-接口','接口', '/api/perm-resources/**',   '权限-资源关联接口',  NOW(), 'system'),
    (2019, '秒杀商品-接口',    '接口', '/api/seckill-goods/**',    '秒杀商品管理接口',   NOW(), 'system'),
    (2020, '订单明细-接口',    '接口', '/api/order-items/**',      '订单明细查询接口',   NOW(), 'system');

-- 2) 授权给已有权限（系统管理类挂 305，秒杀商品挂 304，订单明细挂 302）
INSERT IGNORE INTO `t_rbac_perm_resource` (`resource_id`,`perm_id`,`created_time`,`created_by`)
VALUES
    (2009, 305, NOW(), 'system'), (2010, 305, NOW(), 'system'),
    (2011, 305, NOW(), 'system'), (2012, 305, NOW(), 'system'),
    (2013, 305, NOW(), 'system'), (2014, 305, NOW(), 'system'),
    (2015, 305, NOW(), 'system'), (2016, 305, NOW(), 'system'),
    (2017, 305, NOW(), 'system'), (2018, 305, NOW(), 'system'),
    (2019, 304, NOW(), 'system'), (2020, 302, NOW(), 'system');

-- 3) 核对
SELECT '资源 2009~2020 应共 12 条' AS check_item;
SELECT COUNT(*) AS cnt FROM `t_rbac_resource` WHERE id BETWEEN 2009 AND 2020;

SELECT '对应授权应共 12 条' AS check_item;
SELECT COUNT(*) AS cnt FROM `t_rbac_perm_resource` WHERE resource_id BETWEEN 2009 AND 2020;

SELECT '总数：资源应为 26（含本脚本前已有的 14 条）、权限资源关联应为 26' AS check_item;
SELECT (SELECT COUNT(*) FROM `t_rbac_resource`)      AS res_total,
       (SELECT COUNT(*) FROM `t_rbac_perm_resource`) AS pr_total;
