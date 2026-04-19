/**
 * Axios 实例（自动注入 JWT + 错误处理）。
 * 详见 docs/core/10-认证授权与权限模型.md §3
 */
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const client = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
})

/** 请求拦截：注入 Access Token */
client.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

/** 响应拦截：统一错误处理 + 401 跳转登录 */
client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (!err.response) {
      ElMessage.error('Network error, please try again')
      return Promise.reject(err)
    }

    const { status, data } = err.response
    const message = data?.message || err.message

    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
      window.location.href = '/login'
      ElMessage.error('Session expired, please login again')
    } else if (status === 403) {
      ElMessage.error('No permission to access this resource')
    } else if (status >= 500) {
      ElMessage.error(`Server error: ${message}`)
    } else {
      ElMessage.error(message)
    }

    return Promise.reject(err)
  },
)

export default client
