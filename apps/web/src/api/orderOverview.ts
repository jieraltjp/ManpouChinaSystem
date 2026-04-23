/**
 * 订单总览 API 客户端。
 * 路由: /api/v1/orders/{procurementId}/overview
 * 对应文档: docs/business/SPEC-B09-IMPLEMENTATION.md
 */
import client from './client'

export type StepStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

export interface ProcurementPageVO {
  id: number
  factoryId?: number
  factoryName?: string
  productCode: string
  subProductCode?: string
  material?: string
  requiresQc?: boolean
  quantity?: number
  priceRmb?: number
  exchangeRate?: number
  taxPoint?: number
  billingType?: string
  estimatedPriceJpy?: number
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
  createTime?: string
}

export interface ProcurementPageResponse {
  content: ProcurementPageVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface ProcurementVO {
  id: number
  procurementCode: string
  factoryId?: number
  factoryName?: string
  productCode: string
  subProductCode?: string
  material?: string
  requiresQc?: boolean
  quantity?: number
  priceRmb?: number
  exchangeRate?: number
  taxPoint?: number
  billingType?: string
  estimatedPriceJpy?: number
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
  createTime?: string
}

export interface FactoryVO {
  id: number
  factoryCode: string
  factoryName: string
  category?: string
  province?: string
  city?: string
  county?: string
  roughLocation?: string
  contactName?: string
  contactPhone?: string
  cooperationStatus?: string
}

export interface DemandVO {
  id: number
  demandCode: string
  demandType?: string
  productCode: string
  subProductCode?: string
  quantity?: number
  destination?: string
  japanLead?: string
  status?: string
  createTime?: string
}

export interface QcRecordVO {
  id: number
  qcCode: string
  procurementId?: number
  sellerName?: string
  productCode: string
  subProductCode?: string
  result?: string
  inspectionCount?: number
  passedCount?: number
  defectiveCount?: number
  boxCount?: number
  boxLengthCm?: number
  boxWidthCm?: number
  boxHeightCm?: number
  netWeightPerUnit?: number
  grossWeight?: number
  qcDate?: string
  qcUserId?: number
  status?: string
}

export interface LogisticsPlanVO {
  id: number
  planCode: string
  procurementId?: number
  factoryId?: number
  productCode: string
  subProductCode?: string
  planType?: string
  status?: string
  cargoLengthCm?: number
  cargoWidthCm?: number
  cargoHeightCm?: number
  cargoVolumeCbm?: number
  cargoWeightKg?: number
  quantity?: number
  requiresQc?: boolean
  estimatedShipDate?: string
  actualShipDate?: string
}

export interface DomesticCustomsVO {
  id: number
  customsCode: string
  procurementId?: number
  productCode: string
  status?: string
  estimatedValueCny?: number
  remarks?: string
}

export interface JapanCustomsVO {
  id: number
  procurementId?: number
  domesticCustomsId?: number
  logisticsPlanId?: number
  customsEntryNo?: string
  status?: string
  arrivalDate?: string
  clearanceDate?: string
  customsBroker?: string
  brokerPhone?: string
  brokerContact?: string
  importDutyPaid?: number
  consumptionTaxPaid?: number
  arrivalPort?: string
  declaredWeightKg?: number
  declaredVolumeCbm?: number
  remarks?: string
}

export interface TaxRefundVO {
  id: number
  procurementId?: number
  japanCustomsId?: number
  refundCode?: string
  status?: string
  billingType?: string
  priceRmb?: number
  quantity?: number
  taxPoint?: number
  estimatedRefundRmb?: number
  actualRefundRmb?: number
  exchangeRate?: number
  refundDate?: string
  refundBank?: string
  remarks?: string
}

export interface SalesRecordVO {
  id: number
  recordCode?: string
  procurementId?: number
  productCode: string
  subProductCode?: string
  salesChannel?: string
  listingDate?: string
  initialStock?: number
  currentStock?: number
  safetyStock?: number
  salesQuantity?: number
  returnedQuantity?: number
  returnRate?: number
  sellingPriceJpy?: number
  status?: string
  remarks?: string
}

export interface OrderOverviewVO {
  procurementId: number
  procurement: ProcurementVO
  factory?: FactoryVO
  demand?: DemandVO
  qcRecord?: QcRecordVO
  logisticsPlan?: LogisticsPlanVO
  domesticCustoms?: DomesticCustomsVO
  japanCustoms?: JapanCustomsVO
  taxRefund?: TaxRefundVO
  salesRecord?: SalesRecordVO
  stepStatuses: StepStatus[]
}

export const orderOverviewApi = {
  getOverview(procurementId: number) {
    return client.get<{ code: string; data: OrderOverviewVO }>(`/orders/${procurementId}/overview`)
  },
  listSelector(params: { page?: number; pageSize?: number; keyword?: string }) {
    return client.get<{ code: string; data: ProcurementPageResponse }>('/orders/selector', { params })
  },
}
