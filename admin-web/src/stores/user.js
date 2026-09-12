import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { clearAuth, getStoredUser, getToken, saveAuth } from '@/utils/auth'
import * as authApi from '@/api/auth'

/** 当前登录的后台管理员。令牌与用户信息落 localStorage，本 store 是它们的响应式投影。 */
export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const loginUser = ref(getStoredUser())

  /** 必须是后台令牌（会员令牌 audience=member，不能在后台用） */
  const isLoggedIn = computed(() => !!token.value && loginUser.value?.audience === 'admin')
  const userId = computed(() => loginUser.value?.userId ?? null)
  const account = computed(() => loginUser.value?.account ?? '')
  const displayName = computed(() => loginUser.value?.name || loginUser.value?.account || '')

  async function doLogin(form) {
    const info = await authApi.login(form)
    // 前端拦一道：会员账号从 /api/members/login 拿到的也是合法 JWT，
    // 但它 audience=member，放进来网关的 RBAC 过滤器根本不会生效，后台等同于裸奔。
    if (info?.audience !== 'admin') {
      throw new Error('该账号不是后台账号，请使用管理员账号登录')
    }
    saveAuth(info)
    token.value = info.token
    loginUser.value = info
    return info
  }

  async function fetchProfile() {
    if (!userId.value) return null
    return authApi.getUserById(userId.value)
  }

  function logout() {
    clearAuth()
    token.value = ''
    loginUser.value = null
  }

  return { token, loginUser, isLoggedIn, userId, account, displayName, doLogin, fetchProfile, logout }
})
