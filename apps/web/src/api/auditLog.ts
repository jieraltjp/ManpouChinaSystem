/**
 * 操作日志 API 客户端。
 * 与 user-service AuditLogController 对齐。
 */
import client from './client'

export interface AuditLogVO {
  id: number
  traceId: string
  userId: string
  username: string
  operatorName: string
  companyId: number | null
  departmentId: number | null
  module: string
  action: string
  httpMethod: string
  httpUrl: string
  resourceType: string
  resourceId: string
  resourceCode: string
  detail: string
  ipAddress: string
  userAgent: string
  requestId: string
  createTime: string
}

export interface AuditLogPageQuery {
  userId?: string
  module?: string
  action?: string
  resourceType?: string
  resourceId?: string
  mainProductCode?: string
  subProductCode?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

export interface AuditLogPageVO {
  content: AuditLogVO[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

/** 分页查询 */
export function pageAuditLogs(query: AuditLogPageQuery): Promise<AuditLogPageVO> {
  return client.get<AuditLogPageVO>('/audit-logs', { params: query }).then(res => res.data)
}

/** 查询详情 */
export function getAuditLog(id: number): Promise<AuditLogVO> {
  return client.get<AuditLogVO>(`/audit-logs/${id}`).then(res => res.data)
}
