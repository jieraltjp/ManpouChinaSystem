/**
 * 补货需求单 API 客户端。
 * 与 docs/business/SPEC-B01-补货需求-步骤1.md v2.2.0 完全对齐。
 * 一行 = 一个子货号（商品唯一标识 = 主货号+子货号）。
 */
import client from './client'

export type DemandType = 'REPLENISHMENT' | 'NEW_PURCHASE'
export type DemandStatus = 'PENDING' | 'CONFIRMED'

export interface DemandPageVO {
  id: number
  demandCode: string
  demandType: DemandType
  /** 主货号 */
  productCode: string
  /** 子货号（全码，如 ad009-be） */
  subProductCode: string
  /** 需求数量 */
  quantity: number
  /** 目的地 */
  destination?: string
  japanLead?: string
  status: DemandStatus
  /** 关联的 Procurement ID（CONFIRMED 时有值，v2.2.0） */
  linkedProcurementId?: number
  remarks?: string
  /** 商品图片URL（v2.1.0） */
  imageUrl?: string
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
  subProductCode: string
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
}

export const demandApi = {
  list(params: { page?: number; pageSize?: number; demandType?: string; productCode?: string }) {
    return client.get<DemandPageResponse>('/demands', { params })
  },
  get(id: number) {
    return client.get<DemandPageVO>(`/demands/${id}`)
  },
  create(data: CreateDemandRequest) {
    return client.post<number>('/demands', data)
  },
  update(id: number, data: UpdateDemandRequest) {
    return client.patch<{ code: string }>(`/demands/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/demands/${id}`)
  },
  /** 关联到发注单（v2.2.0）：POST /demands/{id}/link?procurementId= */
  link(id: number, procurementId: number) {
    return client.post<{ code: string }>(`/demands/${id}/link?procurementId=${procurementId}`)
  },
  /** 取消关联（v2.2.0）：POST /demands/{id}/unlink */
  unlink(id: number) {
    return client.post<{ code: string }>(`/demands/${id}/unlink`)
  },
  /** 查看关联的采购单 */
  getLinkedProcurement(id: number) {
    return client.get<unknown>(`/demands/${id}/procurement`)
  },
  /** 目的地建议（下拉去重列表） */
  suggestDestinations() {
    return client.get<string[]>('/demands/suggest/destinations')
  },
  /** 日本担当建议（下拉去重列表） */
  suggestJapanLeads() {
    return client.get<string[]>('/demands/suggest/japan-leads')
  },
}
