/**
 * 日本清关管理 API 客户端。
 * 后端：JapanCustomsRecord 实体（步骤6 日本清关）
 * 路径：/api/v1/japan-customs
 *
 * 状态流转：PENDING → IN_PROGRESS → CLEARED | FAILED
 *
 * 文档: docs/business/SPEC-B06-日本清关-步骤6.md
 */
import client from './client'

export type JapanCustomsStatus = 'PENDING' | 'IN_PROGRESS' | 'CLEARED' | 'FAILED'

export interface JapanCustomsVO {
  id: number
  customsEntryNo: string
  containerNo: string | null   // 货柜号（v1.4.0 核心字段）
  domesticCustomsId: number | null
  procurementId: number | null   // v1.4.0 可选参考
  logisticsPlanId: number | null
  productCode: string | null    // 货号（v1.4.0 新增）
  subProductCode: string | null  // 子货号/颜色（v1.6.1 全链路追踪）
  status: JapanCustomsStatus
  arrivalDate: string | null
  customsBroker: string | null
  brokerPhone: string | null
  brokerContact: string | null
  importDutyPaid: number | null
  consumptionTaxPaid: number | null
  clearanceDate: string | null
  arrivalPort: string | null
  declaredWeightKg: number | null
  declaredVolumeCbm: number | null
  remarks: string | null
  createBy: string
  createTime: string
  updateTime: string
}

export interface JapanCustomsPageResponse {
  content: JapanCustomsVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface JapanCustomsQuery {
  page?: number
  pageSize?: number
  status?: JapanCustomsStatus
  containerNo?: string   // 货柜号（v1.4.0 GET 筛选优先字段）
  procurementId?: number
  domesticCustomsId?: number
}

export interface JapanCustomsCreateRequest {
  containerNo: string    // 货柜号（v1.4.0 必填，第一位）
  domesticCustomsId?: number
  logisticsPlanId?: number
  procurementId?: number  // v1.4.0 可选参考
  factoryId?: number      // v1.4.0 新增
  productCode?: string    // 货号（v1.4.0 新增）
  subProductCode?: string  // 子货号/颜色（v1.6.1 全链路追踪）
  arrivalDate?: string
  customsBroker?: string
  brokerPhone?: string
  brokerContact?: string
  arrivalPort?: string
  declaredWeightKg?: number
  declaredVolumeCbm?: number
  remarks?: string
}

export interface JapanCustomsUpdateRequest {
  subProductCode?: string  // 子货号/颜色
  arrivalDate?: string
  customsBroker?: string
  brokerPhone?: string
  brokerContact?: string
  arrivalPort?: string
  declaredWeightKg?: number
  declaredVolumeCbm?: number
  remarks?: string
}

export interface JapanCustomsCompleteRequest {
  importDutyPaid: number
  consumptionTaxPaid: number
  clearanceDate: string
}

export interface JapanCustomsFailRequest {
  reason: string
}

export interface JapanCustomsBatchCreateRequest {
  containerNo: string
  domesticCustomsIds: number[]
}

export const japanCustomsApi = {
  list(params: JapanCustomsQuery) {
    return client.get<{ code: string; data: JapanCustomsPageResponse }>('/japan-customs', { params })
  },
  get(id: number) {
    return client.get<{ code: string; data: JapanCustomsVO }>(`/japan-customs/${id}`)
  },
  create(data: JapanCustomsCreateRequest) {
    return client.post<{ code: string; data: number }>('/japan-customs', data)
  },
  batchCreate(data: JapanCustomsBatchCreateRequest) {
    return client.post<{ code: string; data: number[] }>('/japan-customs/batch', data)
  },
  update(id: number, data: JapanCustomsUpdateRequest) {
    return client.put<{ code: string }>(`/japan-customs/${id}`, data)
  },
  start(id: number) {
    return client.patch<{ code: string }>(`/japan-customs/${id}/start`)
  },
  complete(id: number, data: JapanCustomsCompleteRequest) {
    return client.patch<{ code: string }>(`/japan-customs/${id}/complete`, data)
  },
  fail(id: number, data: JapanCustomsFailRequest) {
    return client.patch<{ code: string }>(`/japan-customs/${id}/fail`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/japan-customs/${id}`)
  },
}
