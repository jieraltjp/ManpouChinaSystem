/**
 * 出口退税管理 API 客户端。
 * 后端：TaxRefundRecord 实体（步骤7 出口退税）
 * 路径：/api/v1/tax-refunds
 *
 * 状态流转：APPLYING → COMPLETED | NO_REFUND
 *
 * 文档: docs/business/SPEC-B07-出口退税-步骤7.md
 */
import client from './client'

export type TaxRefundStatus = 'APPLYING' | 'COMPLETED' | 'NO_REFUND'

export interface TaxRefundVO {
  id: number
  refundCode: string
  procurementId: number | null
  japanCustomsId: number | null
  status: TaxRefundStatus
  billingType: string | null
  priceRmb: number | null
  quantity: number | null
  taxPoint: number | null
  estimatedRefundRmb: number | null
  actualRefundRmb: number | null
  exchangeRate: number | null
  refundDate: string | null
  refundBank: string | null
  remarks: string | null
  createBy: string
  createTime: string
  updateTime: string
}

export interface TaxRefundPageResponse {
  content: TaxRefundVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface TaxRefundQuery {
  page?: number
  pageSize?: number
  status?: TaxRefundStatus
  procurementId?: number
}

export interface TaxRefundCreateRequest {
  procurementId?: number
  japanCustomsId?: number
  billingType?: string
  priceRmb?: number
  quantity?: number
  taxPoint?: number
  exchangeRate?: number
  remarks?: string
}

export interface TaxRefundCompleteRequest {
  actualRefundRmb: number
  refundDate: string
  refundBank?: string
}

export const taxRefundApi = {
  list(params: TaxRefundQuery) {
    return client.get<TaxRefundPageResponse>('/tax-refunds', { params })
  },
  get(id: number) {
    return client.get<TaxRefundVO>(`/tax-refunds/${id}`)
  },
  create(data: TaxRefundCreateRequest) {
    return client.post<number>('/tax-refunds', data)
  },
  complete(id: number, data: TaxRefundCompleteRequest) {
    return client.patch<{ code: string }>(`/tax-refunds/${id}/complete`, data)
  },
  markNoRefund(id: number) {
    return client.patch<{ code: string }>(`/tax-refunds/${id}/no-refund`)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/tax-refunds/${id}`)
  },
}
