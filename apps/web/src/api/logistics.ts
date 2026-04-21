/**
 * 调配计划 API 客户端。
 * 与 SPEC-调配计划流程.md 完全对齐。
 */
import client from './client'

export type LogisticsStatus = 'PLANNED' | 'BOOKED' | 'IN_TRANSIT' | 'DELIVERED'
export type PlanType = 'SEA' | 'AIR' | 'CONSOLIDATION'

export interface LogisticsPlanVO {
  id: number
  planCode: string
  procurementId?: number
  factoryId?: number
  productCode: string
  subProductCode?: string
  planType: PlanType
  status: LogisticsStatus
  cargoLengthCm?: number
  cargoWidthCm?: number
  cargoHeightCm?: number
  cargoWeightKg?: number
  cargoVolumeCbm?: number
  quantity?: number
  requiresQc?: boolean
  containerId?: number
  poolId?: number
  estimatedShipDate?: string
  actualShipDate?: string
  remarks?: string
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface LogisticsPlanPageResponse {
  content: LogisticsPlanVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateLogisticsPlanRequest {
  procurementId?: number
  factoryId?: number
  productCode: string
  subProductCode?: string
  planType: PlanType
  cargoLengthCm?: number
  cargoWidthCm?: number
  cargoHeightCm?: number
  cargoWeightKg?: number
  quantity?: number
  requiresQc?: boolean
  estimatedShipDate?: string
  actualShipDate?: string
  remarks?: string
}

export interface UpdateLogisticsPlanRequest {
  planType?: PlanType
  status?: LogisticsStatus
  cargoLengthCm?: number
  cargoWidthCm?: number
  cargoHeightCm?: number
  cargoWeightKg?: number
  quantity?: number
  requiresQc?: boolean
  containerId?: number
  poolId?: number
  estimatedShipDate?: string
  actualShipDate?: string
  remarks?: string
}

export const logisticsApi = {
  list(params: { page?: number; pageSize?: number; productCode?: string; planType?: PlanType; status?: LogisticsStatus; procurementId?: number; factoryId?: number }) {
    return client.get<{ code: string; data: LogisticsPlanPageResponse }>('/logistics-plans', { params })
  },
  get(id: number) {
    return client.get<{ code: string; data: LogisticsPlanVO }>(`/logistics-plans/${id}`)
  },
  create(data: CreateLogisticsPlanRequest) {
    return client.post<{ code: string; data: number }>('/logistics-plans', data)
  },
  update(id: number, data: UpdateLogisticsPlanRequest) {
    return client.patch<{ code: string }>(`/logistics-plans/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/logistics-plans/${id}`)
  },
}
