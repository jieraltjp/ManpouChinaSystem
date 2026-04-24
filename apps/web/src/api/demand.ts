/**
 * 补货需求单 API 客户端。
 * 与 docs/business/SPEC-B01-补货需求-步骤1.md §API设计 完全对齐。
 */
import client from './client'

export type DemandType = 'REPLENISHMENT' | 'NEW_PURCHASE'
export type DemandStatus = 'PENDING' | 'CONVERTED' | 'CANCELLED'

/** 子货号明细（v1.6.0：每个子货号独立数量+目的地） */
export interface SubProductItem {
  subCode: string
  quantity: number
  destination?: string
}

/** 关联发注表明细（v1.6.0） */
export interface LinkedDemandItem {
  linkedProcurementId: number
  subCode: string
}

export interface DemandPageVO {
  id: number
  demandCode: string
  demandType: DemandType
  productCode: string
  /** 子货号明细列表（v1.6.0） */
  subProductItems?: SubProductItem[]
  japanLead?: string
  status: DemandStatus
  /** 关联发注表明细（v1.6.0，CONVERTED 时有值） */
  linkedDemandItems?: LinkedDemandItem[]
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

/** 转采购响应（v1.6.0） */
export interface ConvertDemandResponse {
  demandStatus: DemandStatus
  linkedProcurementIds: number[]
}

export interface CreateDemandRequest {
  demandType: DemandType
  productCode: string
  /** 子货号明细列表（v1.6.0） */
  subProductItems: SubProductItem[]
  japanLead?: string
  remarks?: string
}

export interface UpdateDemandRequest {
  demandType?: DemandType
  productCode?: string
  /** 子货号明细列表（v1.6.0） */
  subProductItems?: SubProductItem[]
  japanLead?: string
  remarks?: string
  status?: DemandStatus
}

/** 转采购请求（v1.6.0） */
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
  /** 批量转采购（v1.6.0）：POST /demands/{id}/convert body={factoryId} */
  convertToProcurement(id: number, cmd: ConvertDemandCmd) {
    return client.post<{ code: string; data: ConvertDemandResponse }>(`/demands/${id}/convert`, cmd)
  },
  /** 撤销转换（v1.6.0）：批量删除关联 Procurement，回滚状态 */
  revertConversion(id: number) {
    return client.post<{ code: string }>(`/demands/${id}/revert`)
  },
  /** 查看关联的采购单列表（v1.6.0） */
  getLinkedProcurements(id: number) {
    return client.get<{ code: string; data: unknown[] }>(`/demands/${id}/procurements`)
  },
}
