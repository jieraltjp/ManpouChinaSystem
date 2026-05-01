/**
 * 仓库管理 API 客户端。
 *
 * 后端现状：WarehouseExample 存根（仅 name/status 字段）
 * Phase 2 扩展：增加仓库类型、地址、容量等字段
 */
import client from './client'

/** 仓库状态枚举 */
export type WarehouseStatus = 'ACTIVE' | 'INACTIVE'

export interface WarehouseVO {
  id: number
  name: string
  status: WarehouseStatus
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface WarehousePageResponse {
  content: WarehouseVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateWarehouseRequest {
  name: string
  status?: WarehouseStatus
}

export interface UpdateWarehouseRequest {
  name?: string
  status?: WarehouseStatus
}

export const warehouseApi = {
  list(params: { page?: number; pageSize?: number; keyword?: string }) {
    return client.get<WarehousePageResponse>('/warehouse', { params })
  },
  get(id: number) {
    return client.get<WarehouseVO>(`/warehouse/${id}`)
  },
  create(data: CreateWarehouseRequest) {
    return client.post<number>('/warehouse', data)
  },
  update(id: number, data: UpdateWarehouseRequest) {
    return client.put<{ code: string }>(`/warehouse/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/warehouse/${id}`)
  },
}
