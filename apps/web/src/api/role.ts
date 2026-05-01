/**
 * 角色权限 API 客户端。
 * 与 SPEC-B11-IMPLEMENT.md §3 API 契约完全对齐。
 */
import client from './client'

// ===== 响应类型 =====

export interface PermissionVO {
  id: number
  permissionCode: string
  permissionNameCn?: string
  permissionNameJp?: string
  module: string
  action?: string
  description?: string
  sortOrder?: number
  checked?: boolean
}

export interface PermissionModuleVO {
  module: string
  moduleNameCn?: string
  moduleNameJp?: string
  permissions: PermissionVO[]
}

export interface RoleVO {
  id: number
  roleCode: string
  roleNameCn?: string
  roleNameJp?: string
  roleType?: string
  description?: string
  isEditable?: number
  status?: number
  userCount?: number
  createTime?: string
  permissions?: PermissionVO[]
}

// ===== 请求类型 =====

export interface RoleCreateCmd {
  roleCode: string
  roleNameCn?: string
  roleNameJp?: string
  roleType?: string
  description?: string
  permissionIds?: number[]
}

export interface RoleUpdateCmd {
  roleNameCn?: string
  roleNameJp?: string
  description?: string
}

export interface RolePermissionsCmd {
  permissionIds: number[]
}

// ===== API 函数 =====

/**
 * 获取角色列表（不含权限）
 * GET /api/v1/roles
 */
export function listRoles(): Promise<RoleVO[]> {
  return client.get<RoleVO[]>('/roles').then(r => r.data)
}

/**
 * 获取角色详情（含权限）
 * GET /api/v1/roles/{id}
 */
export function getRole(id: number): Promise<RoleVO> {
  return client.get<RoleVO>(`/roles/${id}`).then(r => r.data)
}

/**
 * 新增角色
 * POST /api/v1/roles
 */
export function createRole(cmd: RoleCreateCmd): Promise<RoleVO> {
  return client.post<RoleVO>('/roles', cmd).then(r => r.data)
}

/**
 * 更新角色
 * PUT /api/v1/roles/{id}
 */
export function updateRole(id: number, cmd: RoleUpdateCmd): Promise<void> {
  return client.put(`/roles/${id}`, cmd).then(() => undefined)
}

/**
 * 删除角色
 * DELETE /api/v1/roles/{id}
 */
export function deleteRole(id: number): Promise<void> {
  return client.delete(`/roles/${id}`).then(() => undefined)
}

/**
 * 分配权限
 * PUT /api/v1/roles/{id}/permissions
 */
export function assignRolePermissions(id: number, cmd: RolePermissionsCmd): Promise<void> {
  return client.put(`/roles/${id}/permissions`, cmd).then(() => undefined)
}

/**
 * 获取权限树（按模块分组）
 * GET /api/v1/permissions/tree
 */
export function getPermissionTree(): Promise<PermissionModuleVO[]> {
  return client.get<PermissionModuleVO[]>('/permissions/tree').then(r => r.data)
}
