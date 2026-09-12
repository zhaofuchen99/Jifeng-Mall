import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { clearAuth, getStoredUser, getToken, saveAuth, saveUser } from '@/utils/auth'
import * as memberApi from '@/api/member'

/**
 * 登录会员信息。令牌与用户信息都落在 localStorage（刷新不丢），
 * 本 store 只是它们的响应式投影。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const loginUser = ref(getStoredUser())

  /** 必须有令牌，且必须是会员令牌（后台令牌 audience=admin，不能在前台用） */
  const isLoggedIn = computed(() => !!token.value && loginUser.value?.audience === 'member')
  const memberId = computed(() => loginUser.value?.userId ?? null)
  const account = computed(() => loginUser.value?.account ?? '')
  const displayName = computed(() => loginUser.value?.name || loginUser.value?.account || '')

  async function doLogin(form) {
    const info = await memberApi.login(form)
    saveAuth(info)
    token.value = info.token
    loginUser.value = info
    return info
  }

  async function doRegister(form) {
    const info = await memberApi.register(form)
    saveAuth(info)
    token.value = info.token
    loginUser.value = info
    return info
  }

  /** 拉一次完整会员资料（头像、手机号等注册时没写进令牌的字段） */
  async function fetchProfile() {
    if (!memberId.value) return null
    return memberApi.getMemberById(memberId.value)
  }

  /** 更新本地缓存的姓名等（令牌里的字段不会变，这里只影响显示） */
  function patchLoginUser(patch) {
    loginUser.value = { ...loginUser.value, ...patch }
    saveUser(loginUser.value)
  }

  function logout() {
    clearAuth()
    token.value = ''
    loginUser.value = null
  }

  return {
    token,
    loginUser,
    isLoggedIn,
    memberId,
    account,
    displayName,
    doLogin,
    doRegister,
    fetchProfile,
    patchLoginUser,
    logout
  }
})
