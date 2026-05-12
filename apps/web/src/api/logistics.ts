/**
 * 物流相关 API 客户端（调配计划 + 拼柜池 + 货柜）。
 * v1.5.0 SPEC-B00 Issue #8。
 */
import client from './client'

// ===== 调配计划 =====
export type LogisticsStatus = 'PLANNED' | 'BOOKED' | 'IN_TRANSIT' | 'DELIVERED'
export type PlanType = 'SEA' | 'AIR' | 'CONSOLIDATION'

// ===== 拼柜池 =====
export type ConsolidationPoolStatus = 'OPEN' | 'PENDING' | 'LOADED' | 'SHIPPED'

export interface ConsolidationPoolVO {
  id: number
  poolCode: string
  destinationPort: string
  totalCbm?: number
  totalWeightKg?: number
  planCount?: number
  containerThresholdCbm?: number
  status: ConsolidationPoolStatus
  createTime?: string
  updateTime?: string
}

// ===== 货柜 =====
export type ContainerStatus = 'CREATED' | 'LOADED' | 'DEPARTED' | 'ARRIVED'
export type ContainerType = 'GP20' | 'GP40' | 'HC40' | 'HC45'

export interface ContainerVO {
  id: number
  containerNo: string
  containerType: ContainerType
  totalCbm?: number
  totalWeightKg?: number
  planCount?: number
  poolId?: number
  status: ContainerStatus
  loadDate?: string
  departureDate?: string
  arrivalDate?: string
  createTime?: string
  updateTime?: string
  // ===== v2.0 扩展字段（SPEC-B12）=====
  shipId?: number
  shipName?: string
  shipNumber?: string
  timeSlot?: string
  arrivalLocation?: string
  remarks?: string
}

// ===== 调配计划 =====
export interface LogisticsPlanVO {
  id: number
  planCode: string
  containerNo?: string    // 货柜号（v1.3.0）
  qcRecordId?: number    // 关联验货记录（v1.2.0）
  qcCode?: string        // 验货编号（v1.2.0，来自 QC record）
  procurementId?: number
  factoryId?: number
  factoryName?: string   // 关联工厂名称（来自 factory 表 JOIN）
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
  qcRecordId?: number    // 关联验货记录（v1.2.0）
  containerNo?: string   // 货柜号（v1.3.0）
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
  qcRecordId?: number     // 关联验货记录（v1.2.0）
  factoryId?: number       // 关联工厂（v1.3.0）
  planType?: PlanType
  status?: LogisticsStatus
  containerNo?: string    // 货柜号（v1.3.0）
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
  list(params: { page?: number; pageSize?: number; productCode?: string; planType?: PlanType; status?: LogisticsStatus; qcRecordId?: number; procurementId?: number; factoryId?: number; containerNo?: string }) {
    return client.get<LogisticsPlanPageResponse>('/logistics-plans', { params })
  },
  get(id: number) {
    return client.get<LogisticsPlanVO>(`/logistics-plans/${id}`)
  },
  create(data: CreateLogisticsPlanRequest) {
    return client.post<number>('/logistics-plans', data)
  },
  update(id: number, data: UpdateLogisticsPlanRequest) {
    return client.patch<{ code: string }>(`/logistics-plans/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/logistics-plans/${id}`)
  },
}

// ===== 拼柜池 API =====
export interface ConsolidationPoolPageResponse {
  content: ConsolidationPoolVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateConsolidationPoolRequest {
  destinationPort: string
  containerThresholdCbm?: number
}

export interface UpdateConsolidationPoolRequest {
  destinationPort?: string
  containerThresholdCbm?: number
  status?: ConsolidationPoolStatus
}

export const consolidationPoolApi = {
  list(params: { page?: number; pageSize?: number; status?: ConsolidationPoolStatus; destinationPort?: string }) {
    return client.get<ConsolidationPoolPageResponse>('/consolidation-pools', { params })
  },
  get(id: number) {
    return client.get<ConsolidationPoolVO>(`/consolidation-pools/${id}`)
  },
  create(data: CreateConsolidationPoolRequest) {
    return client.post<number>('/consolidation-pools', data)
  },
  update(id: number, data: UpdateConsolidationPoolRequest) {
    return client.patch<{ code: string }>(`/consolidation-pools/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/consolidation-pools/${id}`)
  },
  addPlan(poolId: number, planId: number) {
    return client.post<{ code: string }>(`/consolidation-pools/${poolId}/plans/${planId}`)
  },
  removePlan(poolId: number, planId: number) {
    return client.delete<{ code: string }>(`/consolidation-pools/${poolId}/plans/${planId}`)
  },
}

// ===== 货柜 API =====
export interface ContainerPageResponse {
  content: ContainerVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateContainerRequest {
  containerNo: string
  containerType: ContainerType
  poolId?: number
  loadDate?: string
  departureDate?: string
  arrivalDate?: string
  // ===== v2.0 扩展字段（SPEC-B12）=====
  timeSlot?: string
  arrivalLocation?: string
  remarks?: string
}

export interface UpdateContainerRequest {
  containerNo?: string
  containerType?: ContainerType
  status?: ContainerStatus
  loadDate?: string
  departureDate?: string
  arrivalDate?: string
  // ===== v2.0 扩展字段（SPEC-B12）=====
  shipId?: number
  timeSlot?: string
  arrivalLocation?: string
  remarks?: string
}

export interface AssignShipRequest {
  shipId: number
  loadDate?: string
}

export const containerApi = {
  list(params: { page?: number; pageSize?: number; status?: ContainerStatus; poolId?: number; shipId?: number }) {
    return client.get<ContainerPageResponse>('/containers', { params })
  },
  get(id: number) {
    return client.get<ContainerVO>(`/containers/${id}`)
  },
  create(data: CreateContainerRequest) {
    return client.post<number>('/containers', data)
  },
  update(id: number, data: UpdateContainerRequest) {
    return client.patch<{ code: string }>(`/containers/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/containers/${id}`)
  },
  addPlan(containerId: number, planId: number) {
    return client.post<{ code: string }>(`/containers/${containerId}/plans/${planId}`)
  },
  assignShip(containerId: number, data: AssignShipRequest) {
    return client.put<void>(`/containers/${containerId}/assign-ship`, data)
  },
  unassignShip(containerId: number) {
    return client.put<void>(`/containers/${containerId}/unassign-ship`)
  },
}
