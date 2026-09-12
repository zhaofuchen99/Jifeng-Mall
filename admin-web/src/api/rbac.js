import { makeCrud } from './crud'

/**
 * RBAC 的全部资源。十张表都是同一套 CRUD 形状，只是关联表多了几个反查接口。
 *
 * ⚠️ 两个坑：
 * 1. `t_rbac_role` / `t_rbac_group` 的 `enabled` 是 NOT NULL 且 insert 语句显式带该字段，
 *    所以调 POST /api/roles、POST /api/groups 时 <b>JSON 里必须显式传 enabled</b>，不传就是 NULL 报错。
 * 2. 关联表的实体没有审计字段，查询条件只有 id 和两个外键（精确匹配）。
 */
export const userApi = makeCrud('/api/users')
export const groupApi = makeCrud('/api/groups')
export const roleApi = makeCrud('/api/roles')
export const permApi = makeCrud('/api/perms')
export const resourceApi = makeCrud('/api/resources')
export const menuApi = makeCrud('/api/menus')
