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
 * 修改会员资料。
 * 注意：PUT /api/members 的 password 字段会被后端加密后保存，但**不传则不改**，
 * 所以改资料时不要把 password 一起带上（除了「修改密码」那一次）。
 */
export function updateMember(data) {
  return request.put('/api/members', data)
}
