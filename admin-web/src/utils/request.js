import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './auth'
import router from '@/router'

/**
 * 统一响应结构：{ code, success, msg, data }。
 *
 * <p><b>不能靠 HTTP 状态码判断成败</b>：业务失败（含 400/500/7001~7005）后端也返回 HTTP 200，
 * 只有网关这一层（401 未登录 / 403 无权限 / 429 限流）才是真实 HTTP 状态码。</p>
 *
 * <p>成功时 resolve 的是 envelope.data；失败时 reject 一个带 code 的 Error，默认弹一次提示，
 * 传 { silent: true } 可关掉。</p>
 */
const service = axios.create({
  timeout: 20000
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const envelope = response.data
    if (!envelope || typeof envelope !== 'object' || !('success' in envelope)) {
      return envelope
    }
    if (envelope.success) {
      return envelope.data
    }
    return rejectWith(envelope.code, envelope.msg, response.config)
  },
  (error) => {
    if (error.code === 'ECONNABORTED') {
      return rejectWith(0, '请求超时，请检查后端服务是否已启动', error.config)
    }
    const status = error.response?.status
    const msg = error.response?.data?.msg
    // 403 是网关 RBAC 过滤器给的，补齐上下文，否则用户只看到"无权限访问"会一头雾水
    const text =
      status === 403 ? '无权限访问该接口，请确认当前账号的菜单与资源授权' : msg || error.message
    return rejectWith(status, text || '网络异常', error.config || {})
  }
)

function rejectWith(code, msg, config) {
  const err = new Error(msg || '请求失败')
  err.code = code

  if (code === 401) {
    clearAuth()
    const current = router.currentRoute.value
    if (current.name !== 'login') {
      ElMessage.warning('登录已失效，请重新登录')
      router.push({ name: 'login', query: { redirect: current.fullPath } })
    }
  } else if (!config?.silent) {
    ElMessage.error(err.message)
  }
  return Promise.reject(err)
}

export default service
