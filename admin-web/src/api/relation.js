import request from '@/utils/request'
import { makeCrud } from './crud'

/**
 * RBAC 的四张关联表。除了标准 CRUD，各自还有按外键反查的接口，
 * 用来做「某个组有哪些角色」「某个用户属于哪些组」这类双向维护。
 */

export const userGroupApi = {
  ...makeCrud('/api/user-groups'),
  byGroup: (groupId) => request.get(`/api/user-groups/group/${groupId}`),
  byUser: (userId) => request.get(`/api/user-groups/user/${userId}`)
}

export const groupRoleApi = {
  ...makeCrud('/api/group-roles'),
  byGroup: (groupId) => request.get(`/api/group-roles/group/${groupId}`)
}

export const rolePermApi = {
  ...makeCrud('/api/role-perms'),
  byRole: (roleId) => request.get(`/api/role-perms/role/${roleId}`)
}

export const permResourceApi = {
  ...makeCrud('/api/perm-resources'),
  byPerm: (permId) => request.get(`/api/perm-resources/perm/${permId}`),
  byResource: (resourceId) => request.get(`/api/perm-resources/resource/${resourceId}`)
}
