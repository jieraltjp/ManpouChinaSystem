/**
 * 发注单 API 客户端。
 * 与 docs/business/SPEC-发注管理流程.md §3.2 完全对齐。
 */
import client from './client'

/** 发注单分页查询响应（v1.3.0 扩展字段） */
export interface ProcurementPageVO {
  id: number
  factoryId?: number            // 关联工厂ID
  productCode: string          // 主货号
  subProductCode?: string     // 子货号/枝番（颜色）
  material?: string            // 材质
  requiresQc?: boolean        // 是否需要检测
  quantity: number
  priceRmb: number
  exchangeRate: number
  taxPoint: number
  billingType?: string        // 报关类型（v1.3.0）
  estimatedPriceJpy?: number
  customsRemarks?: string      // 报关备注（v1.3.0）
  instructionManual?: string   // 说明书（v1.3.0）
  orderDate?: string
  factoryShipDate?: string
  plannedShipDate?: string
  actualShipDate?: string     // 实际出货日（v1.3.0）
  productLead?: string
  japanLead?: string
  chinaLead?: string
  destination?: string
  customerCompany?: string
  status: string
  createBy?: string
  createTime?: string
  updateTime?: string
}

/** 发注单分页响应 */
export interface ProcurementPageResponse {
  content: ProcurementPageVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

/** 创建发注单请求 */
export interface CreateProcurementRequest {
  factoryId?: number
  productCode: string
  subProductCode?: string
  material?: string
  requiresQc?: boolean
  quantity: number
  priceRmb: number
  exchangeRate: number
  taxPoint: number
  billingType?: string
  customsRemarks?: string
  instructionManual?: string
  orderDate?: string
  factoryShipDate?: string
  plannedShipDate?: string
  actualShipDate?: string
  productLead?: string
  japanLead?: string
  chinaLead?: string
  destination?: string
  customerCompany?: string
  status?: string
}

/** 更新发注单请求 */
export interface UpdateProcurementRequest {
  factoryId?: number
  productCode?: string
  subProductCode?: string
  material?: string
  requiresQc?: boolean
  quantity?: number
  priceRmb?: number
  exchangeRate?: number
  taxPoint?: number
  billingType?: string
  customsRemarks?: string
  instructionManual?: string
  orderDate?: string
  factoryShipDate?: string
  plannedShipDate?: string
  actualShipDate?: string
  productLead?: string
  japanLead?: string
  chinaLead?: string
  destination?: string
  customerCompany?: string
  status?: string
}

export const procurementApi = {
  list(params: { page?: number; pageSize?: number; status?: string; productCode?: string; customerCompany?: string; factoryId?: number }) {
    return client.get<{ code: string; data: ProcurementPageResponse }>('/procurements', { params })
  },
  get(id: number) {
    return client.get<{ code: string; data: ProcurementPageVO }>(`/procurements/${id}`)
  },
  create(data: CreateProcurementRequest) {
    return client.post<{ code: string; data: number }>('/procurements', data)
  },
  update(id: number, data: UpdateProcurementRequest) {
    return client.patch<{ code: string }>(`/procurements/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/procurements/${id}`)
  },
}
