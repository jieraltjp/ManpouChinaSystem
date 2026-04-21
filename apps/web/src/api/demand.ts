/**
 * 补货需求单 API 客户端。
 * 与 docs/business/SPEC-发注管理流程.md §3.1 完全对齐。
 */
import client from './client'

export type DemandType = 'REPLENISHMENT' | 'NEW_PURCHASE'
export type DemandStatus = 'PENDING' | 'CONVERTED' | 'CANCELLED'

export interface DemandPageVO {
  id: number
  demandCode: string
  demandType: DemandType
  productCode: string
  subProductCode?: string
  quantity: number
  destination?: string
  japanLead?: string
  status: DemandStatus
  linkedProcurementId?: number
  remarks?: string
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface DemandPageResponse {
  content: DemandPageVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateDemandRequest {
  demandType: DemandType
  productCode: string
  subProductCode?: string
  quantity: number
  destination?: string
  japanLead?: string
  remarks?: string
}

export interface UpdateDemandRequest {
  demandType?: DemandType
  productCode?: string
  subProductCode?: string
  quantity?: number
  destination?: string
  japanLead?: string
  remarks?: string
  status?: DemandStatus
}

export const demandApi = {
  list(params: { page?: number; pageSize?: number; status?: string; demandType?: string; productCode?: string }) {
    return client.get<{ code: string; data: DemandPageResponse }>('/demands', { params })
  },
  get(id: number) {
    return client.get<{ code: string; data: DemandPageVO }>(`/demands/${id}`)
  },
  create(data: CreateDemandRequest) {
    return client.post<{ code: string; data: number }>('/demands', data)
  },
  update(id: number, data: UpdateDemandRequest) {
    return client.patch<{ code: string }>(`/demands/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/demands/${id}`)
  },
  convertToProcurement(id: number, procurementId: number) {
    return client.post<{ code: string }>(`/demands/${id}/convert?procurementId=${procurementId}`)
  },
}
