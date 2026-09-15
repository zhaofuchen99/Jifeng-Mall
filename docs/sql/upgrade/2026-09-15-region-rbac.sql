-- =====================================================================
-- 增量变更：地区管理（FR-210）的 RBAC 挂载
--
-- 适用：2026-09-15 之前创建的库。
--
-- 背景：region-api 原先只有查询接口，后台「地区管理」是只读页；
--   本轮补上了 POST / PUT / DELETE。**但仅加接口是不够的** ——
--   网关对 /api/regions/** **只放行 GET**，写请求照样要过 RBAC 判定，
--   而种子里没有任何 /api/regions 资源，结果是管理员点「新增/编辑/删除」
--   必然吃 403。
--
-- 所以必须补齐：菜单资源 1007、接口资源 2021、权限 306，
-- 以及三条绑定关系（role_perm / perm_resource × 2）与一行菜单。
--
-- 幂等：INSERT IGNORE（前两张表有主键/UNIQUE 约束）；
--       菜单表无唯一约束，用 NOT EXISTS 保护。
-- =====================================================================

SET NAMES utf8mb4;

-- 1) 资源：菜单资源 1007 + 接口资源 2021
INSERT IGNORE INTO `t_rbac_resource` (`id`,`name`,`type`,`value`,`description`,`created_time`,`created_by`)
VALUES (1007, '地区管理',  '菜单', '/region',            '地区菜单',       NOW(), 'system'),
       (2021, '地区-接口', '接口', '/api/regions/**',    '行政区划维护接口', NOW(), 'system');

-- 2) 权限 306
INSERT IGNORE INTO `t_rbac_perm` (`id`,`name`,`description`,`enabled`,`created_time`,`created_by`)
VALUES (306, '地区管理权限', '行政区划维护', 1, NOW(), 'system');

-- 3) 超管角色（id=1）授予该权限
INSERT IGNORE INTO `t_rbac_role_perm` (`role_id`,`perm_id`,`created_time`,`created_by`)
VALUES (1, 306, NOW(), 'system');

-- 4) 权限 → 资源
INSERT IGNORE INTO `t_rbac_perm_resource` (`resource_id`,`perm_id`,`created_time`,`created_by`)
VALUES (1007, 306, NOW(), 'system'), (2021, 306, NOW(), 'system');

-- 5) 菜单行（url 必须与前端路由 /region 对齐，否则侧边栏点了跳不过去）
INSERT INTO `t_rbac_menu` (`parent_id`,`resource_id`,`name`,`icon`,`url`,`sort`,`description`,
                           `created_time`,`created_by`,`updated_time`,`updated_by`)
SELECT 0, 1007, '地区管理', 'el-icon-location', '/region', 7, '基础数据',
       NOW(), 'system', NOW(), 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_rbac_menu` WHERE url = '/region');

-- 6) 核对
SELECT '资源 1007 / 2021' AS check_item;
SELECT id, name, type, value FROM `t_rbac_resource` WHERE id IN (1007, 2021);

SELECT '权限 306 及其绑定' AS check_item;
SELECT p.id, p.name FROM `t_rbac_perm` p WHERE p.id = 306;
SELECT * FROM `t_rbac_role_perm`      WHERE perm_id = 306;
SELECT * FROM `t_rbac_perm_resource`  WHERE perm_id = 306;

SELECT '菜单行' AS check_item;
SELECT id, parent_id, resource_id, name, url, sort FROM `t_rbac_menu` WHERE url = '/region';

SELECT '总数应为：资源 28、权限 6、权限资源关联 28' AS check_item;
SELECT (SELECT COUNT(*) FROM `t_rbac_resource`)      AS res_total,
       (SELECT COUNT(*) FROM `t_rbac_perm`)          AS perm_total,
       (SELECT COUNT(*) FROM `t_rbac_perm_resource`) AS pr_total;
