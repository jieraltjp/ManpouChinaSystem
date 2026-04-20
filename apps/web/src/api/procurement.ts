/**
 * 发注单 API 客户端。
 * 与 docs/business/API-发注管理.md §1 完全对齐。
 */
import client from './client'

/** 发注单分页查询响应 */
export interface ProcurementPageVO {
  id: number
  productCode: string
  quantity: number
  priceRmb: number
  exchangeRate: number
  taxPoint: number
  estimatedPriceJpy: number
  billingMethod: string
  orderDate: string
  factoryShipDate: string
  plannedShipDate: string
  productLead: string
  japanLead: string
  chinaLead: string
  destination: string
  customerCompany: string
  status: string
  createBy: string
  createTime: string
  updateTime: string
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
  productCode: string
  quantity: number
  priceRmb: number
  exchangeRate: number
  taxPoint: number
  billingMethod?: string
  orderDate?: string
  factoryShipDate?: string
  plannedShipDate?: string
  productLead?: string
  japanLead?: string
  chinaLead?: string
  destination?: string
  customerCompany?: string
  status?: string  // 默认未定，后端填充
}

/** 更新发注单请求 */
export interface UpdateProcurementRequest {
  productCode?: string
  quantity?: number
  priceRmb?: number
  exchangeRate?: number
  taxPoint?: number
  billingMethod?: string
  orderDate?: string
  factoryShipDate?: string
  plannedShipDate?: string
  productLead?: string
  japanLead?: string
  chinaLead?: string
  destination?: string
  customerCompany?: string
  status?: string
}

export const procurementApi = {
  /** 分页查询 */
  list(params: { page?: number; pageSize?: number; status?: string; productCode?: string; customerCompany?: string }) {
    return client.get<{ code: string; data: ProcurementPageResponse }>('/procurements', { params })
  },

  /** 详情 */
  get(id: number) {
    return client.get<{ code: string; data: ProcurementPageVO }>(`/procurements/${id}`)
  },

  /** 创建 */
  create(data: CreateProcurementRequest) {
    return client.post<{ code: string; data: number }>('/procurements', data)
  },

  /** 更新 */
  update(id: number, data: UpdateProcurementRequest) {
    return client.patch<{ code: string }>(`/procurements/${id}`, data)
  },

  /** 删除 */
  delete(id: number) {
    return client.delete<{ code: string }>(`/procurements/${id}`)
  },
}
