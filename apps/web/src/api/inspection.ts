/**
 * 验货记录 API 客户端。
 * 与 docs/business/SPEC-B03-验货记录-步骤3.md §API设计 完全对齐。
 */
import client from './client'

export type QcResult = 'PASS' | 'FAIL'
export type QcStatus = 'PENDING' | 'COMPLETED' | 'RETURN_REQUESTED'
export type QcType = 'ONSITE' | 'REMOTE' | 'EXEMPT'

export interface QcRecordVO {
  id: number
  qcCode: string
  procurementId?: number    // 保留用于审计追溯
  shipmentBatchId?: number  // V43新增：关联出货批次

  // 出货批次信息（enrichment）
  batchCode?: string       // 批次编号
  batchStatus?: string     // 批次状态
  shipmentQuantity?: number // 批次出货数量

  sellerName?: string
  factoryId?: number    // 关联工厂ID（来自 procurement.factoryId，v1.3.0）
  factoryName?: string  // 关联工厂名称（来自 factory 表，v1.3.0）
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
  shipmentBatchId: number   // V43新增：必填
  procurementId?: number    // 保留用于审计追溯
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
  list(params: { page?: number; pageSize?: number; qcCode?: string; productCode?: string; result?: QcResult; status?: string; qcDateFrom?: string; qcDateTo?: string; procurementId?: number; shipmentBatchId?: number }) {
    return client.get<QcRecordPageResponse>('/qc-records', { params })
  },
  get(id: number) {
    return client.get<QcRecordVO>(`/qc-records/${id}`)
  },
  create(data: CreateQcRecordRequest) {
    return client.post<number>('/qc-records', data)
  },
  update(id: number, data: UpdateQcRecordRequest) {
    return client.patch<{ code: string }>(`/qc-records/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/qc-records/${id}`)
  },

  /** 上传单张图片（SPEC-C12） */
  uploadImage(file: File, qcRecordId?: number) {
    const form = new FormData()
    form.append('file', file)
    if (qcRecordId) form.append('qcRecordId', String(qcRecordId))
    return client.post<ImageUploadResult>('/qc/images/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  /** 批量上传图片（SPEC-C12） */
  uploadImages(files: File[], qcRecordId?: number) {
    const form = new FormData()
    files.forEach(f => form.append('files', f))
    if (qcRecordId) form.append('qcRecordId', String(qcRecordId))
    return client.post<ImageUploadResult[]>('/qc/images/upload-multiple', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  /** 删除图片（SPEC-C12） */
  deleteImage(id: number) {
    return client.delete<{ code: string }>(`/qc/images?id=${id}`)
  },

  /** 查询验货记录关联的图片列表（SPEC-C12） */
  listImages(qcRecordId: number) {
    return client.get<QcImageVO[]>(`/qc/images`, { params: { qcRecordId } })
  },
}

export interface ImageUploadResult {
  url: string
  filename: string
  size: number
}

export interface QcImageVO {
  id: number
  qcRecordId: number | null
  filename: string
  originalName: string
  url: string
  size: number
  mimeType: string
  uploadedBy: number | null
  createTime: string
}
