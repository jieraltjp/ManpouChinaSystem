/**
 * 工厂管理 API 客户端。
 * 与 docs/business/SPEC-发注管理流程.md §3.2 完全对齐。
 */
import client from './client'

export type FactoryStatus = 'ACTIVE' | 'INACTIVE'

export interface FactoryPageVO {
  id: number
  factoryCode: string
  factoryName: string
  location?: string
  roughLocation?: string
  contactName?: string
  contactPhone?: string
  status: FactoryStatus
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface FactoryPageResponse {
  content: FactoryPageVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateFactoryRequest {
  factoryName: string
  location?: string
  roughLocation?: string
  contactName?: string
  contactPhone?: string
}

export interface UpdateFactoryRequest {
  factoryName?: string
  location?: string
  roughLocation?: string
  contactName?: string
  contactPhone?: string
  status?: FactoryStatus
}

export const factoryApi = {
  list(params: { page?: number; pageSize?: number; factoryName?: string; status?: string }) {
    return client.get<{ code: string; data: FactoryPageResponse }>('/factories', { params })
  },
  get(id: number) {
    return client.get<{ code: string; data: FactoryPageVO }>(`/factories/${id}`)
  },
  create(data: CreateFactoryRequest) {
    return client.post<{ code: string; data: number }>('/factories', data)
  },
  update(id: number, data: UpdateFactoryRequest) {
    return client.patch<{ code: string }>(`/factories/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/factories/${id}`)
  },
}
