/**
 * 认证 API 适配层。
 * 详见 docs/core/10-认证授权与权限模型.md §2
 */
import client from '@/api/client'
import type { ApiResponse, PublicKeyVO, LoginVO, LoginCmd } from '@/types/api'
import { isSuccess } from '@/types/api'

/** 获取 RSA 公钥（前端用于验签 RS256 Token） */
export async function fetchPublicKey(): Promise<PublicKeyVO> {
  const res = await client.get<ApiResponse<PublicKeyVO>>('/auth/public-key')
  if (!isSuccess(res.data)) {
    throw new Error(res.data.message || 'Failed to fetch public key')
  }
  return res.data.data!
}

/** 登录 */
export async function login(cmd: LoginCmd): Promise<LoginVO> {
  const res = await client.post<ApiResponse<LoginVO>>('/auth/login', cmd)
  const body = res.data
  if (!isSuccess(body)) {
    throw new Error(body.message || 'Login failed')
  }
  return body.data!
}
