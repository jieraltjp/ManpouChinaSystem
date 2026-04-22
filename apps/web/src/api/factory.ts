/**
 * 工厂管理 API 客户端。
 * 与 DB-10 factory.md 完全对齐（v1.4.0）。
 */
import client from './client'

export type CooperationStatus = 'ACTIVE' | 'SUSPENDED' | 'ELIMINATED' | 'POTENTIAL'
export type FactoryCategory =
  | 'TOOLS' | 'TEXTILE' | 'PLASTIC' | 'ELECTRONICS' | 'FURNITURE'
  | 'AUTO_PARTS' | 'SPORTS' | 'PET' | 'MEDICAL' | 'CRAFTS' | 'CHEMICAL' | 'OTHER'
export type PaymentTerms = 'CASH' | 'NET_30' | 'NET_60' | 'NET_90' | 'CREDIT'

export interface FactoryPageVO {
  id: number
  factoryCode: string
  factoryName: string
  category?: FactoryCategory
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
  paymentTerms?: PaymentTerms
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
  category?: FactoryCategory
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
  paymentTerms?: PaymentTerms
  notes?: string
}

export interface UpdateFactoryRequest {
  factoryName?: string
  category?: FactoryCategory
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
  paymentTerms?: PaymentTerms
  notes?: string
}

export const factoryApi = {
  list(params: { page?: number; pageSize?: number; factoryName?: string; cooperationStatus?: string }) {
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
