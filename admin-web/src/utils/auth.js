/**
 * 后台登录态的本地存储读写。
 *
 * <p>与前台 mall-web 分开存（键名带 admin_ 前缀），这样同一个浏览器里
 * 前台会员登录和后台管理员登录互不覆盖，可以同时开着联调。</p>
 */

const TOKEN_KEY = 'admin_token'
const USER_KEY = 'admin_user'

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

export function saveAuth(loginUserInfo) {
  localStorage.setItem(TOKEN_KEY, loginUserInfo.token)
  localStorage.setItem(USER_KEY, JSON.stringify(loginUserInfo))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
