/**
 * 财务管理 API 客户端。
 *
 * 后端现状：FinanceExample 存根（仅 name/status 字段）
 * Phase 7 扩展：增加 taxType, totalCostRmb, actualPaidRmb, currency 等财务字段
 *
 * 文档: docs/business/SPEC-B07-退税-步骤7.md
 */
import client from './client'

/** 财务状态枚举 */
export type FinanceStatus = 'ACTIVE' | 'INACTIVE'

export interface FinanceVO {
  id: number
  name: string
  status: FinanceStatus
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface FinancePageResponse {
  content: FinanceVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateFinanceRequest {
  name: string
  status?: FinanceStatus
}

export interface UpdateFinanceRequest {
  name?: string
  status?: FinanceStatus
}

export const financeApi = {
  list(params: { page?: number; pageSize?: number; keyword?: string }) {
    return client.get<{ code: string; data: FinancePageResponse }>('/finance', { params })
  },
  get(id: number) {
    return client.get<{ code: string; data: FinanceVO }>(`/finance/${id}`)
  },
  create(data: CreateFinanceRequest) {
    return client.post<{ code: string; data: number }>('/finance', data)
  },
  update(id: number, data: UpdateFinanceRequest) {
    return client.put<{ code: string }>(`/finance/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/finance/${id}`)
  },
}
