/**
 * 订单总览（订单链）API 客户端。
 * 路由: /api/v1/orders/chain
 * 后端: OrderChainController → OrderChainUseCase → v_order_chain_v2 VIEW + 实际表
 * 全链路9步（步骤3：厂家出货多批次，步骤4：验货记录多记录）
 */
import client from './client'

export type StepStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

// ====== 列表响应 ======

export interface OrderChainVO {
  demandId: number
  demandCode: string
  demandType?: string
  demandProductCode: string
  demandSubProductCode?: string
  demandQuantity?: number
  demandDestination?: string
  demandJapanLead?: string
  demandStatus?: string
  linkedProcurementId?: number
  demandImageUrl?: string
  demandCreateTime?: string
  demandUpdateTime?: string
  snapshot?: SnapshotVO
  step1Status: string
  step2Status: string
  step3Status: string
  step4Status: string
  step5Status?: string
  step6Status?: string
  step7Status?: string
  step8Status?: string
  step9Status?: string
  procurementPriceRmb?: string
  procurementTaxPoint?: string
  procurementBillingType?: string
  procurementCreateBy?: string
}

export interface SnapshotVO {
  factoryId?: number
  factoryCode?: string
  factoryName?: string
  factoryProvince?: string
  factoryCity?: string
  factoryContactName?: string
  factoryContactPhone?: string
  productNameZh?: string
  productNameJa?: string
  productCategory?: string
}

// ====== 详情响应 ======

export interface OrderChainDetailVO {
  demandId: number
  demand: DemandVO | null
  procurement: ProcurementVO | null
  factory: FactoryVO | null
  shipmentBatches: ShipmentBatchVO[] | null
  qcRecords: QcRecordVO[] | null
  logisticsPlan: LogisticsPlanVO | null
  domesticCustoms: DomesticCustomsVO | null
  japanCustoms: JapanCustomsVO | null
  taxRefund: TaxRefundVO | null
  salesRecord: SalesRecordVO | null
}

export interface ShipmentBatchVO {
  id: number
  batchCode: string
  procurementId?: number
  shipmentQuantity?: number
  factoryShipDate?: string
  actualShipDate?: string
  status?: string
  remarks?: string
  qcRecordCount?: number
  totalPassedCount?: number
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

export interface ProcurementVO {
  id: number
  procurementCode: string
  factoryId?: number
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
  returnReason?: string
  createTime?: string
}

export interface FactoryVO {
  id?: number
  factoryCode?: string
  factoryName?: string
  category?: string
  province?: string
  city?: string
  county?: string
  roughLocation?: string
  contactName?: string
  contactPhone?: string
  cooperationStatus?: string
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
  productImageUrl?: string
}

export interface LogisticsPlanVO {
  id: number
  planCode: string
  containerNo?: string
  procurementId?: number
  factoryId?: number
  factoryName?: string
  productCode: string
  subProductCode?: string
  planType?: string
  status?: string
  cargoLengthCm?: number
  cargoWidthCm?: number
  cargoHeightCm?: number
  cargoVolumeCbm?: number
  cargoWeightKg?: number
  netWeightKg?: number
  grossWeightKg?: number
  qcPassedCount?: number
  quantity?: number
  requiresQc?: boolean
  estimatedShipDate?: string
  actualShipDate?: string
}

export interface DomesticCustomsVO {
  id: number
  customsCode: string
  containerNo?: string
  procurementId?: number
  productCode: string
  subProductCode?: string
  status?: string
  estimatedValueCny?: number
  remarks?: string
  createTime?: string
  updateTime?: string
}

export interface JapanCustomsVO {
  id?: number
  customsEntryNo?: string
  containerNo?: string
  domesticCustomsId?: number
  procurementId?: number
  logisticsPlanId?: number
  productCode?: string
  subProductCode?: string
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
  createBy?: string
  createTime?: string
  updateTime?: string
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

export interface ChainPageResponse {
  content: OrderChainVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export const orderChainApi = {
  listChain(params: { page?: number; pageSize?: number; demandStatus?: string; keyword?: string }) {
    return client.get<ChainPageResponse>('/orders/chain', { params })
  },
  getChainDetail(demandId: number) {
    return client.get<OrderChainDetailVO>(`/orders/chain/${demandId}`)
  },
  deleteChain(demandId: number) {
    return client.delete(`/orders/chain/${demandId}`)
  },
}
