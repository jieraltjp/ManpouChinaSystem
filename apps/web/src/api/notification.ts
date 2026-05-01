/**
 * 通知管理 API 客户端。
 *
 * 后端现状：NotificationExample 存根（仅 name/status 字段）
 * Phase 2 扩展：增加通知类型、渠道、优先级、已读状态等
 */
import client from './client'

/** 通知状态枚举 */
export type NotificationStatus = 'ACTIVE' | 'INACTIVE'

export interface NotificationVO {
  id: number
  name: string
  status: NotificationStatus
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface NotificationPageResponse {
  content: NotificationVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateNotificationRequest {
  name: string
  status?: NotificationStatus
}

export interface UpdateNotificationRequest {
  name?: string
  status?: NotificationStatus
}

export const notificationApi = {
  list(params: { page?: number; pageSize?: number; keyword?: string }) {
    return client.get<NotificationPageResponse>('/notifications', { params })
  },
  get(id: number) {
    return client.get<NotificationVO>(`/notifications/${id}`)
  },
  create(data: CreateNotificationRequest) {
    return client.post<number>('/notifications', data)
  },
  update(id: number, data: UpdateNotificationRequest) {
    return client.put<{ code: string }>(`/notifications/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/notifications/${id}`)
  },
}
