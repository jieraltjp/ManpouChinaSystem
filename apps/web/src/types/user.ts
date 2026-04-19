/**
 * 用户类型定义。
 */
export interface UserClaims {
  sub: string          // userId
  username: string
  roles: string[]
  permissions: string[]
  tenantId: string
  iat: number
  exp: number
}
