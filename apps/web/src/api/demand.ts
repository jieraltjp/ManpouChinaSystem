/**
 * 补货需求单 API 客户端。
 * 与 docs/business/SPEC-B01-补货需求-步骤1.md v2.0.0 完全对齐。
 * 一行 = 一个子货号（商品唯一标识 = 主货号+子货号）。
 */
import client from './client'

export type DemandType = 'REPLENISHMENT' | 'NEW_PURCHASE'
export type DemandStatus = 'PENDING' | 'CONVERTED' | 'CANCELLED'

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
  /** 关联的 Procurement ID（CONVERTED 时有值） */
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

/** 转采购响应（v2.0.0：1:1） */
export interface ConvertDemandResponse {
  demandStatus: DemandStatus
  linkedProcurementId: number
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
  status?: DemandStatus
}

/** 转采购请求（v2.0.0） */
export interface ConvertDemandCmd {
  factoryId: number
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
  /** 转采购（v2.0.0）：POST /demands/{id}/convert body={factoryId} */
  convertToProcurement(id: number, cmd: ConvertDemandCmd) {
    return client.post<{ code: string; data: ConvertDemandResponse }>(`/demands/${id}/convert`, cmd)
  },
  /** 撤销转换（v2.0.0） */
  revertConversion(id: number) {
    return client.post<{ code: string }>(`/demands/${id}/revert`)
  },
  /** 查看关联的采购单（v2.0.0） */
  getLinkedProcurement(id: number) {
    return client.get<{ code: string; data: unknown }>(`/demands/${id}/procurement`)
  },
  /** 目的地建议（下拉去重列表） */
  suggestDestinations() {
    return client.get<{ code: string; data: string[] }>('/demands/suggest/destinations')
  },
  /** 日本担当建议（下拉去重列表） */
  suggestJapanLeads() {
    return client.get<{ code: string; data: string[] }>('/demands/suggest/japan-leads')
  },
}
