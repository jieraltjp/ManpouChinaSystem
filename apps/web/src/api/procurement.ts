/**
 * 发注单 API 客户端。
 * 与 docs/business/SPEC-B02-发注单-步骤2.md §API设计 完全对齐。
 * 与 docs/database/DB-02-procurement-order.md 完全对齐。
 */
import client from './client'

/** 报关类型枚举 — 与后端 BillingType 完全对齐 */
export type BillingType = 'ZHE_LU_KAI_PIAO' | 'CHAO_HUI_TUI_SHUI' | 'NO_REFUND' | 'OTHER'

/** 商品类型枚举 — 发注单维度 */
export type ProductType = 'NORMAL' | 'SAMPLE' | 'SELF_USE' | 'PARTS' | 'INDEPENDENT'

export const BILLING_TYPE_OPTIONS: { value: BillingType; label: string }[] = [
  { value: 'ZHE_LU_KAI_PIAO', label: '浙鲁开票' },
  { value: 'CHAO_HUI_TUI_SHUI', label: '超慧退税' },
  { value: 'NO_REFUND', label: '不退税' },
  { value: 'OTHER', label: '其他' },
]

/** 发注单分页查询响应（v1.3.0 扩展字段，SPEC-B11 batchCount） */
export interface ProcurementPageVO {
  id: number
  factoryId?: number            // 关联工厂ID
  factoryName?: string          // 关联工厂名称（来自 DB-10 Factory 表，Assembler 查库填充）
  batchCount?: number         // 出货批次数量（Phase2：batchCount>0 → 已出货，SPEC-B11）
  productType?: ProductType    // 商品类型（SPEC-B13）
  demandCode?: string          // 关联需求单号（SPEC-B13）
  syntheticDemand?: boolean     // 是否为系统自动生成的需求（SPEC-B13）
  productCode: string          // 主货号
  subProductCode?: string     // 子货号/枝番（颜色）
  material?: string            // 材质
  requiresQc?: boolean        // 是否需要检测
  quantity: number
  shipmentQuantity?: number  // 出货数量（所有批次之和，SPEC-B11 §7.1）
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
  returnReason?: string  // 退货原因（订货失败时）
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
  demandId?: number   // 关联需求ID（不关联补货时为 null，后端自动创建 demand）
  factoryId?: number
  productType?: ProductType
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
  productType?: ProductType
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
  returnReason?: string  // 退货原因（标记为订货失败时）
}

export const procurementApi = {
  list(params: { page?: number; pageSize?: number; status?: string; productCode?: string; customerCompany?: string; factoryId?: number; productType?: ProductType }) {
    return client.get<ProcurementPageResponse>('/procurements', { params })
  },
  get(id: number) {
    return client.get<ProcurementPageVO>(`/procurements/${id}`)
  },
  create(data: CreateProcurementRequest) {
    return client.post<number>('/procurements', data)
  },
  update(id: number, data: UpdateProcurementRequest) {
    return client.patch<{ code: string }>(`/procurements/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/procurements/${id}`)
  },
  suggestDestinations() {
    return client.get<string[]>('/procurements/suggest/destinations')
  },
  suggestCustomerCompanies() {
    return client.get<string[]>('/procurements/suggest/customer-companies')
  },
}

// ===== 出货批次 API（SPEC-B11） =====
export type ShipmentBatchStatus = '待验货' | '验货中' | '已验货' | '已取消'

/** 出货批次分页查询响应 */
export interface ShipmentBatchVO {
  id: number
  procurementId: number
  batchCode: string
  shipmentQuantity: number
  factoryShipDate?: string
  actualShipDate?: string
  status: ShipmentBatchStatus
  remarks?: string
  qcRecordCount?: number
  totalPassedCount?: number
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
  // 商品信息
  productMasterCode?: string
  productSubCode?: string
  productImageUrl?: string
}

export interface ShipmentBatchPageResponse {
  content: ShipmentBatchVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateShipmentBatchRequest {
  procurementId: number
  shipmentQuantity: number
  factoryShipDate?: string
  remarks?: string
}

export interface UpdateShipmentBatchRequest {
  batchCode?: string
  shipmentQuantity?: number
  factoryShipDate?: string
  actualShipDate?: string
  status?: ShipmentBatchStatus
  remarks?: string
}

export const shipmentBatchApi = {
  list(params: { page?: number; pageSize?: number; procurementId?: number; status?: ShipmentBatchStatus }) {
    return client.get<ShipmentBatchPageResponse>('/shipment-batches', { params })
  },
  get(id: number) {
    return client.get<ShipmentBatchVO>(`/shipment-batches/${id}`)
  },
  create(data: CreateShipmentBatchRequest) {
    return client.post<number>('/shipment-batches', data)
  },
  update(id: number, data: UpdateShipmentBatchRequest) {
    return client.patch<{ code: string }>(`/shipment-batches/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/shipment-batches/${id}`)
  },
  linkQc(batchId: number, qcRecordId: number) {
    return client.post<{ code: string }>(`/shipment-batches/${batchId}/link-qc`, { qcRecordId })
  },
}
