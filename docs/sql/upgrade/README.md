# 数据库增量变更脚本

《开发流程指南》10.1 要求「数据库脚本 = **初始化脚本 + 增量变更脚本**」。

| 类型 | 文件 | 适用场景 |
| --- | --- | --- |
| 初始化 | `../jifeng-mall-init.sql` | **全新部署**。建 23 张表 + 索引 + 种子数据 |
| 增量 | 本目录 | **已有的旧库升级**。只补差量，不动既有数据 |

## 使用原则

1. **全新部署只跑 `jifeng-mall-init.sql`**，它已包含本目录所有变更的最终状态。
   再跑增量脚本是安全的（全部幂等），但没有必要。
2. **旧库升级按文件名顺序执行本目录的脚本**，每个脚本头部说明了它解决什么问题、
   适用哪个时间点之前的库。
3. **所有脚本都幂等**，可重复执行：
   - 建表用 `CREATE TABLE IF NOT EXISTS`；
   - 种子/授权用 `INSERT IGNORE`（依赖 `t_rbac_resource` 的主键、
     `t_rbac_perm_resource` / `t_rbac_role_perm` 的 UNIQUE 约束）；
   - 菜单这类无唯一约束的表用 `NOT EXISTS` 子查询保护。
4. ⚠️ **执行前先备份**（见 `docs/运维手册.md` §5.2）。
   增量脚本本身不删数据，但涉及权限表的变更一旦出问题会影响后台可用性。

## 为什么会有增量脚本

项目在开发过程中对表结构/种子做过三次调整，每次都需要「已部署的库」能跟上：

| 脚本 | 变更 | 不做的后果 |
| --- | --- | --- |
| `2026-09-12-rbac-resources.sql` | 补 12 条接口资源（2009~2020）及其授权 | **管理员访问「系统管理」整个模块全部 403** |
| `2026-09-14-banner.sql` | 新增第 23 张表 `banner` + 3 条演示轮播 | 首页没有轮播数据（前台渲染渐变兜底，不会报错） |
| `2026-09-15-region-rbac.sql` | 补地区管理（FR-210）的菜单/接口资源与权限 306 | **后台「地区管理」新增/编辑/删除全部 403** |

## 执行方式

```bat
cd /d <项目根目录>
mysql -uroot -p --default-character-set=utf8mb4 shoplook2026 < docs\sql\upgrade\2026-09-12-rbac-resources.sql
mysql -uroot -p --default-character-set=utf8mb4 shoplook2026 < docs\sql\upgrade\2026-09-14-banner.sql
mysql -uroot -p --default-character-set=utf8mb4 shoplook2026 < docs\sql\upgrade\2026-09-15-region-rbac.sql
```

执行后核对（三个数都必须是期望值，否则后台会出现大面积 403）：

```sql
SELECT (SELECT COUNT(*) FROM t_rbac_resource)      AS res,   -- 28
       (SELECT COUNT(*) FROM t_rbac_perm)          AS perm,  -- 6
       (SELECT COUNT(*) FROM t_rbac_perm_resource) AS pr;    -- 28
```
