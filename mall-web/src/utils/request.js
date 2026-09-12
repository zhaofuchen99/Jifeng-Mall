import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './auth'
import router from '@/router'

/**
 * 统一响应结构：{ code, success, msg, data }。
 *
 * <p><b>关键：不能靠 HTTP 状态码判断成败。</b>业务失败（含 400/500/7001~7005）后端也是
 * HTTP 200，只是 body 里 success=false；只有网关这一层（401 未登录 / 403 无权限 / 429 限流）
 * 才会给出真实的 HTTP 状态码。所以下面两个分支都得处理。</p>
 *
 * <p>成功时 resolve 的是 <b>envelope.data</b>（业务数据本身），分页接口拿到的就是
 * {@code {list, total, pageNum, pageSize}}。失败时 reject 一个带 code 的 Error，
 * 并默认弹一次 message——传 {@code { silent: true }} 可关掉，交给页面自己提示。</p>
 */
const service = axios.create({
  timeout: 15000
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
    // 非统一结构（如静态资源）原样返回
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
    return rejectWith(status, msg || error.message || '网络异常', error.config || {})
  }
)

function rejectWith(code, msg, config) {
  const err = new Error(msg || '请求失败')
  err.code = code

  if (code === 401) {
    // 令牌缺失/失效：清登录态，回跳登录页并记住来路（文档 5.1 的要求）
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
