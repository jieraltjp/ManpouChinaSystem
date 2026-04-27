/**
 * 报关管理 API 客户端。
 * 后端：DomesticCustomsRecord 实体（国内报关）
 * 路径：/api/v1/customs
 *
 * 状态流转：PENDING → SUBMITTED → CLEARED | REJECTED
 *
 * 文档: docs/business/SPEC-B05-国内报关-步骤5.md
 */
import client from './client'

export type DomesticCustomsStatus = 'PENDING' | 'SUBMITTED' | 'CLEARED' | 'REJECTED'

export interface CustomsVO {
  id: number
  customsCode: string
  containerNo?: string   // 货柜号（v1.3.0）
  procurementId: number | null
  logisticsPlanId: number | null
  factoryId: number | null
  productCode: string
  subProductCode: string | null
  quantity: number | null
  estimatedValueCny: number | null
  status: DomesticCustomsStatus
  remarks: string | null
  createBy: string
  createTime: string
  updateTime: string
}

export interface CustomsPageResponse {
  content: CustomsVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CustomsQuery {
  page?: number
  pageSize?: number
  keyword?: string
  containerNo?: string   // 货柜号筛选（v1.3.0）
  status?: DomesticCustomsStatus
  procurementId?: number
  logisticsPlanId?: number
}

export interface CustomsCreateRequest {
  containerNo?: string   // 货柜号（v1.3.0）
  procurementId?: number
  logisticsPlanId?: number
  factoryId?: number
  productCode: string
  subProductCode?: string
  quantity?: number
  estimatedValueCny?: number
  remarks?: string
}

export interface CustomsUpdateRequest {
  containerNo?: string   // 货柜号（v1.3.0）
  factoryId?: number
  productCode?: string
  subProductCode?: string
  quantity?: number
  estimatedValueCny?: number
  remarks?: string
}

export interface RejectRequest {
  reason: string
}

export const customsApi = {
  list(params: CustomsQuery) {
    return client.get<{ code: string; data: CustomsPageResponse }>('/customs', { params })
  },
  get(id: number) {
    return client.get<{ code: string; data: CustomsVO }>(`/customs/${id}`)
  },
  create(data: CustomsCreateRequest) {
    return client.post<{ code: string; data: number }>('/customs', data)
  },
  update(id: number, data: CustomsUpdateRequest) {
    return client.put<{ code: string }>(`/customs/${id}`, data)
  },
  submit(id: number) {
    return client.patch<{ code: string }>(`/customs/${id}/submit`)
  },
  clear(id: number) {
    return client.patch<{ code: string }>(`/customs/${id}/clear`)
  },
  reject(id: number, data: RejectRequest) {
    return client.patch<{ code: string }>(`/customs/${id}/reject`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/customs/${id}`)
  },
}
