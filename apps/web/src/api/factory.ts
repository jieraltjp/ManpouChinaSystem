/**
 * 工厂管理 API 客户端。
 * 与 docs/database/DB-10-factory.md 完全对齐。
 */
import client from './client'

export type CooperationStatus = 'ACTIVE' | 'SUSPENDED' | 'ELIMINATED' | 'POTENTIAL'

export interface FactoryStatsDTO {
  total: number
  active: number
  potential: number
  suspended: number
  eliminated: number
}

export interface FactoryPageVO {
  id: number
  factoryCode: string
  factoryName: string
  province?: string
  city?: string
  county?: string
  roughLocation?: string
  longitude?: number
  latitude?: number
  contactName?: string
  contactPhone?: string
  contactWechat?: string
  contactQq?: string
  cooperationStatus?: CooperationStatus
  notes?: string
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
  province?: string
  city?: string
  county?: string
  roughLocation?: string
  longitude?: number
  latitude?: number
  contactName?: string
  contactPhone?: string
  contactWechat?: string
  contactQq?: string
  cooperationStatus?: CooperationStatus
  notes?: string
}

export interface UpdateFactoryRequest {
  factoryName?: string
  province?: string
  city?: string
  county?: string
  roughLocation?: string
  longitude?: number
  latitude?: number
  contactName?: string
  contactPhone?: string
  contactWechat?: string
  contactQq?: string
  cooperationStatus?: CooperationStatus
  notes?: string
}

export const factoryApi = {
  list(params: { page?: number; pageSize?: number; factoryName?: string; cooperationStatus?: string; province?: string; city?: string; county?: string }) {
    return client.get<{ code: string; data: FactoryPageResponse }>('/factories', { params })
  },
  stats() {
    return client.get<{ code: string; data: FactoryStatsDTO }>('/factories/stats')
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
