/**
 * 验货记录 API 客户端。
 * 与 docs/business/SPEC-B03-验货记录-步骤3.md §API设计 完全对齐。
 */
import client from './client'

export type QcResult = 'PASS' | 'FAIL'
export type QcStatus = 'PENDING' | 'COMPLETED' | 'RETURN_REQUESTED'
export type QcType = 'ONSITE' | 'REMOTE'

export interface QcRecordVO {
  id: number
  qcCode: string
  procurementId: number
  sellerName?: string
  productCode: string
  subProductCode?: string
  qcUserId?: number
  qcType?: QcType
  qcDate?: string
  result: QcResult
  status: QcStatus
  inspectionCount?: number
  passedCount?: number
  defectiveCount?: number
  boxCount?: number
  boxLengthCm?: number
  boxWidthCm?: number
  boxHeightCm?: number
  netWeightPerUnit?: number
  grossWeight?: number
  taxInclusivePrice?: number
  material?: string
  taxRefund?: boolean
  qcStandard?: string
  remarks?: string
  images?: string
  destination?: string
  quantity?: number
  orderDate?: string
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface QcRecordPageResponse {
  content: QcRecordVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateQcRecordRequest {
  procurementId: number
  sellerName?: string
  productCode: string
  subProductCode?: string
  qcUserId?: number
  qcType?: QcType
  qcDate?: string
  result?: QcResult
  inspectionCount?: number
  passedCount?: number
  boxCount?: number
  boxLengthCm?: number
  boxWidthCm?: number
  boxHeightCm?: number
  netWeightPerUnit?: number
  grossWeight?: number
  taxInclusivePrice?: number
  material?: string
  taxRefund?: boolean
  qcStandard?: string
  remarks?: string
  images?: string
  destination?: string
  quantity?: number
  orderDate?: string
}

export interface UpdateQcRecordRequest {
  sellerName?: string
  qcUserId?: number
  qcType?: QcType
  qcDate?: string
  result?: QcResult
  status?: QcStatus
  inspectionCount?: number
  passedCount?: number
  boxCount?: number
  boxLengthCm?: number
  boxWidthCm?: number
  boxHeightCm?: number
  netWeightPerUnit?: number
  grossWeight?: number
  taxInclusivePrice?: number
  material?: string
  taxRefund?: boolean
  qcStandard?: string
  remarks?: string
  images?: string
}

export const inspectionApi = {
  list(params: { page?: number; pageSize?: number; qcCode?: string; productCode?: string; result?: QcResult; status?: string; qcDateFrom?: string; qcDateTo?: string; procurementId?: number }) {
    return client.get<{ code: string; data: QcRecordPageResponse }>('/qc-records', { params })
  },
  get(id: number) {
    return client.get<{ code: string; data: QcRecordVO }>(`/qc-records/${id}`)
  },
  create(data: CreateQcRecordRequest) {
    return client.post<{ code: string; data: number }>('/qc-records', data)
  },
  update(id: number, data: UpdateQcRecordRequest) {
    return client.patch<{ code: string }>(`/qc-records/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/qc-records/${id}`)
  },
}
