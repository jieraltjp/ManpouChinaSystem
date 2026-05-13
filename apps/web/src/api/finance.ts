// ⚠️ 已废弃 — 财务存根文件，勿在新代码中使用。
// 历史原因：FinanceExample 存根阶段遗留（仅 name/status 字段）。
// 当前实现：TaxRefundController 已迁至 taxRefund.ts。
// 此文件不被任何 .vue 引用，可安全删除。
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
    return client.get<FinancePageResponse>('/finance', { params })
  },
  get(id: number) {
    return client.get<FinanceVO>(`/finance/${id}`)
  },
  create(data: CreateFinanceRequest) {
    return client.post<number>('/finance', data)
  },
  update(id: number, data: UpdateFinanceRequest) {
    return client.put<{ code: string }>(`/finance/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/finance/${id}`)
  },
}
