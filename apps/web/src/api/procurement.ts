/**
 * 发注单 API 客户端。
 * 与 docs/business/SPEC-B02-发注单-步骤2.md §API设计 完全对齐。
 * 与 docs/database/DB-02-procurement-order.md 完全对齐。
 */
import client from './client'

/** 报关类型枚举 — 与后端 BillingType 完全对齐 */
export type BillingType = 'ZHE_LU_KAI_PIAO' | 'CHAO_HUI_TUI_SHUI' | 'NO_REFUND' | 'OTHER'

export const BILLING_TYPE_OPTIONS: { value: BillingType; label: string }[] = [
  { value: 'ZHE_LU_KAI_PIAO', label: '浙鲁开票' },
  { value: 'CHAO_HUI_TUI_SHUI', label: '超慧退税' },
  { value: 'NO_REFUND', label: '不退税' },
  { value: 'OTHER', label: '其他' },
]

/** 发注单分页查询响应（v1.3.0 扩展字段） */
export interface ProcurementPageVO {
  id: number
  factoryId?: number            // 关联工厂ID
  factoryName?: string          // 关联工厂名称（来自 DB-10 Factory 表，Assembler 查库填充）
  qcRecordId?: number          // 关联 QC 记录 ID（Phase2：状态联动）
  productCode: string          // 主货号
  subProductCode?: string     // 子货号/枝番（颜色）
  material?: string            // 材质
  requiresQc?: boolean        // 是否需要检测
  quantity: number
  priceRmb: number
  exchangeRate: number
  taxPoint: number
  billingType?: BillingType  // 报关类型（v1.3.0）
  estimatedPriceJpy?: number
  customsRemarks?: string      // 报关备注（v1.3.0）
  instructionManual?: string   // 说明书（v1.3.0）
  orderDate?: string
  factoryShipDate?: string
  plannedShipDate?: string
  actualShipDate?: string     // 实际出货日（v1.3.0）
  leadTimeDays?: number       // 交货期天数（v1.9.0）
  cartonNotes?: string       // 纸箱备注（v1.9.0）
  afterSalesDeadline?: string  // 售后截止日（v1.10.0）
  productLead?: string
  japanLead?: string
  chinaLead?: string
  destination?: string
  customerCompany?: string
  status: string
  createBy?: string
  createTime?: string
  updateBy?: string
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
  billingType?: BillingType
  customsRemarks?: string
  instructionManual?: string
  orderDate?: string
  factoryShipDate?: string
  plannedShipDate?: string
  actualShipDate?: string
  leadTimeDays?: number
  cartonNotes?: string
  afterSalesDeadline?: string  // 售后截止日（v1.10.0）
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
  billingType?: BillingType
  customsRemarks?: string
  instructionManual?: string
  orderDate?: string
  factoryShipDate?: string
  plannedShipDate?: string
  actualShipDate?: string
  leadTimeDays?: number
  cartonNotes?: string
  afterSalesDeadline?: string  // 售后截止日（v1.10.0）
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
