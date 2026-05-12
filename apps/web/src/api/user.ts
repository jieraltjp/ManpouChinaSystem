/**
 * 用户管理 API 客户端。
 * 与 SPEC-B11-IMPLEMENT.md §3 API 契约完全对齐。
 */
import client from './client'

// ===== 响应类型 =====

export interface UserVO {
  id: number
  userCode: string
  username: string
  nameCn?: string
  nameJp?: string
  email: string
  phone?: string
  avatarUrl?: string
  companyId?: number
  companyName?: string
  departmentId?: number
  departmentName?: string
  positions?: PositionVO[]
  roles?: RoleSimpleVO[]
  language?: string
  timezone?: string
  status: number
  registrationStatus?: string
  lastLoginTime?: string
  lastLoginIp?: string
  createTime?: string
}

export interface PositionVO {
  id: number
  nameCn?: string
  nameJp?: string
}

export interface RoleSimpleVO {
  id: number
  roleCode: string
  roleNameCn?: string
  roleNameJp?: string
}

export interface UserPageResponse {
  content: UserVO[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface PasswordResetVO {
  username: string
  newPassword: string
}

// ===== 请求类型 =====

export interface UserPageQuery {
  keyword?: string
  companyId?: number
  departmentId?: number
  roleId?: number
  status?: number
  page?: number
  size?: number
}

export interface UserCreateCmd {
  username: string
  password: string
  nameCn?: string
  nameJp?: string
  email: string
  phone?: string
  companyId?: number
  departmentId?: number
  roleIds?: number[]
  language?: string
  timezone?: string
}

export interface UserUpdateCmd {
  nameCn?: string
  nameJp?: string
  email: string
  phone?: string
  companyId?: number
  departmentId?: number
  language?: string
  timezone?: string
}

export interface UserStatusCmd {
  status: number
}

export interface UserRolesCmd {
  roleIds: number[]
}

// ===== API 函数 =====

/**
 * 分页查询用户列表
 * GET /api/v1/users?page=0&size=20&keyword=&status=1&companyId=&departmentId=&roleId=
 */
export function pageUsers(query: UserPageQuery = {}): Promise<UserPageResponse> {
  return client.get<UserPageResponse>('/users', { params: query }).then(r => r.data)
}

/**
 * 获取用户详情
 * GET /api/v1/users/{id}
 */
export function getUser(id: number): Promise<UserVO> {
  return client.get<UserVO>(`/users/${id}`).then(r => r.data)
}

/**
 * 新增用户
 * POST /api/v1/users
 */
export function createUser(cmd: UserCreateCmd): Promise<UserVO> {
  return client.post<UserVO>('/users', cmd).then(r => r.data)
}

/**
 * 更新用户
 * PUT /api/v1/users/{id}
 */
export function updateUser(id: number, cmd: UserUpdateCmd): Promise<void> {
  return client.put(`/users/${id}`, cmd).then(() => undefined)
}

/**
 * 删除用户
 * DELETE /api/v1/users/{id}
 */
export function deleteUser(id: number): Promise<void> {
  return client.delete(`/users/${id}`).then(() => undefined)
}

/**
 * 启用/禁用用户
 * PUT /api/v1/users/{id}/status
 */
export function updateUserStatus(id: number, cmd: UserStatusCmd): Promise<void> {
  return client.put(`/users/${id}/status`, cmd).then(() => undefined)
}

/**
 * 重置密码
 * PUT /api/v1/users/{id}/password/reset
 */
export function resetUserPassword(id: number): Promise<PasswordResetVO> {
  return client.put<PasswordResetVO>(`/users/${id}/password/reset`).then(r => r.data)
}

/**
 * 分配角色
 * PUT /api/v1/users/{id}/roles
 */
export function assignUserRoles(id: number, cmd: UserRolesCmd): Promise<void> {
  return client.put(`/users/${id}/roles`, cmd).then(() => undefined)
}

// ===== 个人中心 =====

export interface ProfileUpdateCmd {
  nameCn?: string
  nameJp?: string
  phone?: string
  language?: string
  timezone?: string
}

export interface ChangePasswordCmd {
  oldPassword: string
  newPassword: string
}

/**
 * 获取当前登录用户信息
 * GET /api/v1/users/me
 */
export function getCurrentUser(): Promise<UserVO> {
  return client.get<UserVO>('/users/me').then(r => r.data)
}

/**
 * 更新当前登录用户信息
 * PUT /api/v1/users/me
 */
export function updateCurrentUser(cmd: ProfileUpdateCmd): Promise<UserVO> {
  return client.put<UserVO>('/users/me', cmd).then(r => r.data)
}

/**
 * 修改密码
 * PUT /api/v1/auth/password
 */
export function changePassword(cmd: ChangePasswordCmd): Promise<void> {
  return client.put('/auth/password', cmd).then(() => undefined)
}
