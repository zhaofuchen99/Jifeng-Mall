import request from '@/utils/request'

/** 后台登录（公开）。成功后返回 LoginUserInfo，audience=admin */
export function login(data) {
  return request.post('/api/users/login', data)
}

/**
 * 当前管理员的动态菜单。
 * 后端按「用户 → 用户组 → 角色 → 权限 → 资源 → 菜单」整条链路算出来，
 * 所以不同账号拿到的菜单可能不一样（种子数据里 operator 没有任何授权，会返回空数组）。
 */
export function getMyMenus() {
  return request.get('/api/menus/mine')
}

/** 后台用户自己的资料（复用 user-api 的查询接口） */
export function getUserById(id) {
  return request.get(`/api/users/id/${id}`)
}

export function updateUser(data) {
  return request.put('/api/users', data)
}
