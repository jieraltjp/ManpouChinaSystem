/**
 * 认证 API 适配层。
 * 详见 docs/pro/00-root-project.md §2
 */
import client from '@/api/client'
import type { PublicKeyVO, LoginVO, LoginCmd } from '@/types/api'

/** 获取 RSA 公钥（前端用于验签 RS256 Token） */
export async function fetchPublicKey(): Promise<PublicKeyVO> {
  const res = await client.get<PublicKeyVO>('/auth/public-key')
  return res.data
}

/** 登录 */
export async function login(cmd: LoginCmd): Promise<LoginVO> {
  const res = await client.post<LoginVO>('/auth/login', cmd)
  return res.data
}
