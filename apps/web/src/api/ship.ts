/**
 * 船只管理 API 客户端（SPEC-B12 Phase 2）。
 */
import client from './client'

// ===== 响应类型 =====

export interface ShipVO {
  id: number
  shipName: string
  shipNumber: string
  carrier?: string
  departurePort?: string
  arrivalPort?: string
  containerCount?: number
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface ShipPageResponse {
  content: ShipVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateShipRequest {
  shipName: string
  shipNumber: string
  carrier?: string
  departurePort?: string
  arrivalPort?: string
}

export interface UpdateShipRequest {
  shipName: string
  carrier?: string
  departurePort?: string
  arrivalPort?: string
}

// ===== API =====

export const shipApi = {
  list(params?: {
    page?: number
    pageSize?: number
    shipName?: string
    shipNumber?: string
    arrivalPort?: string
  }) {
    return client.get<ShipPageResponse>('/ships', { params })
  },

  get(id: number) {
    return client.get<ShipVO>(`/ships/${id}`)
  },

  create(data: CreateShipRequest) {
    return client.post<number>('/ships', data)
  },

  update(id: number, data: UpdateShipRequest) {
    return client.put<void>(`/ships/${id}`, data)
  },

  delete(id: number) {
    return client.delete<void>(`/ships/${id}`)
  },
}
