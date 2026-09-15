import request from '@/utils/request'

/** 会员注册（公开）—— 返回 LoginUserInfo，注册即登录 */
export function register(data) {
  return request.post('/api/members/register', data)
}

/** 会员登录（公开）—— 返回 LoginUserInfo */
export function login(data) {
  return request.post('/api/members/login', data)
}

export function getMemberById(id) {
  return request.get(`/api/members/id/${id}`)
}

export function getMemberByAccount(account) {
  return request.get(`/api/members/account/${account}`)
}

/**
 * 修改自己的会员资料（个人信息页）。
 *
 * ⚠️ 走的是 `/api/members/id/{id}`，**不是**裸的 `PUT /api/members`——
 * 后者是后台的会员管理接口，网关的会员白名单里只有 `/api/members/id/**`
 * 与 `/api/members/account/**`，会员调裸路径会 403。
 *
 * 请求体里的 `password` 会被服务端丢弃，改密码请用下面的 changePassword。
 */
export function updateMember(id, data) {
  return request.put(`/api/members/id/${id}`, data)
}

/**
 * 修改自己的密码。
 *
 * 旧密码由**服务端**校验（`PUT /api/members/id/{id}/password`）。
 * 原先这里是「先拿旧密码走一次登录接口」来间接验证的，那个校验只在浏览器里，
 * 绕过前端直接发请求就能跳过，令牌被盗时会把这名会员彻底锁在门外。
 */
export function changePassword(id, oldPassword, newPassword) {
  return request.put(`/api/members/id/${id}/password`, { oldPassword, newPassword })
}
