/**
 * API 类型定义（与后端 Result<T> 完全对应）。
 * 详见 docs/core/02-API设计规范.md §1
 */
export interface ApiResponse<T = unknown> {
  code: string
  message: string
  data: T | null   // 后端 Result<T> 字段名，必须与 docs/core/02 一致
  traceId: string | null
  detail: string | null
}

export interface PublicKeyVO {
  algorithm: string
  publicKey: string
}

export interface LoginVO {
  accessToken: string
  expiresIn: number
  tokenType: string
}

export interface LoginCmd {
  username: string
  password: string
}

/** 判断 API 响应是否成功 */
export function isSuccess<T>(res: ApiResponse<T>): res is ApiResponse<T> & { data: T } {
  return res.code === 'ok' && res.data !== null
}
