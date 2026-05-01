/**
 * 运营销售管理 API 客户端。
 * 后端：SalesRecord 实体（步骤8 运营销售）
 * 路径：/api/v1/sales-records
 *
 * 状态流转：LISTED → LOW_STOCK → OUT_OF_STOCK | DISCONTINUED
 *          OUT_OF_STOCK → LISTED（重新上架）
 *
 * 文档: docs/business/SPEC-B08-运营销售-步骤8.md
 */
import client from './client'

export type SalesStatus = 'LISTED' | 'LOW_STOCK' | 'OUT_OF_STOCK' | 'DISCONTINUED'

export type SalesChannel = 'AMAZON' | 'MERCALI' | 'SELF_SITE' | 'OTHER'

export interface SalesRecordVO {
  id: number
  recordCode: string
  procurementId: number | null
  productCode: string | null
  subProductCode: string | null
  salesChannel: string | null
  status: SalesStatus
  listingDate: string | null
  initialStock: number | null
  currentStock: number | null
  safetyStock: number | null
  salesQuantity: number | null
  returnedQuantity: number | null
  returnRate: number | null
  sellingPriceJpy: number | null
  remarks: string | null
  createBy: string
  createTime: string
  updateTime: string
}

export interface SalesRecordPageResponse {
  content: SalesRecordVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface SalesRecordQuery {
  page?: number
  pageSize?: number
  productCode?: string
  salesChannel?: SalesChannel
  status?: SalesStatus
  procurementId?: number
}

export interface SalesRecordCreateRequest {
  procurementId?: number
  productCode?: string
  subProductCode?: string
  salesChannel?: SalesChannel
  listingDate?: string
  initialStock?: number
  safetyStock?: number
  sellingPriceJpy?: number
  remarks?: string
}

export interface SalesRecordUpdateRequest {
  listingDate?: string
  safetyStock?: number
  sellingPriceJpy?: number
  remarks?: string
}

export interface SalesRecordStockRequest {
  sold?: number
  returned?: number
}

export const salesOperationsApi = {
  list(params: SalesRecordQuery) {
    return client.get<SalesRecordPageResponse>('/sales-records', { params })
  },
  alerts(params: SalesRecordQuery) {
    return client.get<SalesRecordPageResponse>('/sales-records/alerts', { params })
  },
  get(id: number) {
    return client.get<SalesRecordVO>(`/sales-records/${id}`)
  },
  create(data: SalesRecordCreateRequest) {
    return client.post<number>('/sales-records', data)
  },
  update(id: number, data: SalesRecordUpdateRequest) {
    return client.put<{ code: string }>(`/sales-records/${id}`, data)
  },
  updateStock(id: number, data: SalesRecordStockRequest) {
    return client.patch<{ code: string }>(`/sales-records/${id}/stock`, data)
  },
  discontinue(id: number) {
    return client.patch<{ code: string }>(`/sales-records/${id}/discontinue`)
  },
  relist(id: number) {
    return client.patch<{ code: string }>(`/sales-records/${id}/relist`)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/sales-records/${id}`)
  },
}
