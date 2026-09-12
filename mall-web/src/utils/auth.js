/**
 * 登录态的本地存储读写。
 *
 * <p>单独抽出来是为了打断循环依赖：axios 封装（request.js）需要拿令牌、需要在 401 时清登录态，
 * 但它不该 import Pinia store（store 初始化要在 app.use(pinia) 之后）。所以两边都只依赖本文件。</p>
 */

const TOKEN_KEY = 'mall_token'
const USER_KEY = 'mall_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

/** 保存登录结果（LoginUserInfo：token/userId/account/name/audience） */
export function saveAuth(loginUserInfo) {
  localStorage.setItem(TOKEN_KEY, loginUserInfo.token)
  localStorage.setItem(USER_KEY, JSON.stringify(loginUserInfo))
}

/** 更新本地缓存的会员资料（不改令牌） */
export function saveUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
