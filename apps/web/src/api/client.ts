/**
 * Axios 实例（自动注入 JWT + 错误处理）。
 * 详见 docs/pro/00-root-project.md §3
 */
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { i18n } from '@/locales'
import { push, setTraceId } from '@/utils/logger'

const t = (key: string) => i18n.global.t(key)

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

/** 响应拦截：统一解包 + 提取 traceId */
client.interceptors.response.use(
  (res) => {
    // 提取 traceId（后端 JSON 日志中的链路追踪 ID）
    const traceId = res.headers['x-trace-id'] as string | undefined
    if (traceId) setTraceId(traceId)

    // 统一解包 Result<T> 包装：{ code, message, data } → data
    // 如果后端直接返回数组（如 GET /roles），res.data 就是数组，无需解包
    if (res.data && typeof res.data === 'object' && 'code' in res.data && 'data' in res.data) {
      res.data = res.data.data
    }
    return res
  },
  (err) => {
    // 提取 traceId
    const traceId = err.response?.headers?.['x-trace-id'] as string | undefined

    if (!err.response) {
      push({
        level: 'error',
        msg: t('common.error.network'),
        category: 'api',
        traceId,
        meta: { url: err.config?.url, method: err.config?.method },
      })
      ElMessage.error(t('common.error.network'))
      return Promise.reject(err)
    }

    const { status, data } = err.response
    const message = data?.message || err.message
    const logLevel = status >= 500 ? 'error' : 'warn'

    push({
      level: logLevel,
      msg: `[${status}] ${message}`,
      category: 'api',
      traceId,
      meta: { url: err.config?.url, method: err.config?.method, status },
    })

    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
      window.location.href = '/login'
      ElMessage.error(t('common.error.tokenExpired'))
    } else if (status === 403) {
      ElMessage.error(t('common.error.forbidden'))
    } else if (status >= 500) {
      ElMessage.error(t('common.error.server') + message)
    } else {
      ElMessage.error(message)
    }

    return Promise.reject(err)
  },
)

export default client
