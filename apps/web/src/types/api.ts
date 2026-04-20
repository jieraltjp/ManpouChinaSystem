/**
 * API 类型定义（与后端 Result<T> 完全对应）。
 * 详见 docs/desigin/01-项目规划与架构设计.md 或 docs/pro/11-api-gateway.md
 */
export interface ApiResponse<T = unknown> {
  code: string
  message: string
  data: T | null   // 后端 Result<T> 字段名，与 docs/desigin/01 或 docs/pro/11 一致
  traceId: string | null
  detail: string | null
}

export interface PublicKeyVO {
  kid: string
  algorithm: string
  publicKey: string
}

export interface LoginVO {
  accessToken: string
  expiresIn: number
  tokenType: string
  kid: string
}

export interface LoginCmd {
  username: string
  password: string
}

/** 判断 API 响应是否成功 */
export function isSuccess<T>(res: ApiResponse<T>): res is ApiResponse<T> & { data: T } {
  return res.code === 'ok' && res.data !== null
}
