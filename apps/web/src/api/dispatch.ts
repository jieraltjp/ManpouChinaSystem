import client from './client'

export interface DispatchVO {
  id: number
  code: string
  productNameZh?: string
  manager: string
  destination: string
  tax: string
  material: string
  kensa?: string
  quantity: number
  pieces: number
  weight: number
  weight2: number
  length: number
  location: string
  dispatchDate: string
  status: string
  other: string
  unitPrice: number
  rate: number
  warehouse: string
  factoryAddr?: string
  showFlag: number
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface DispatchPageResponse {
  content: DispatchVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateDispatchRequest {
  code: string
  manager: string
  destination: string
  tax?: string
  material?: string
  kensa?: string
  quantity: number
  pieces: number
  weight: number
  weight2?: number
  length?: number
  location?: string
  dispatchDate: string
  status?: string
  other?: string
  unitPrice?: number
  rate?: number
  warehouse?: string
  factoryAddr?: string
  showFlag?: number
}

export interface UpdateDispatchRequest {
  code?: string
  manager?: string
  destination?: string
  tax?: string
  material?: string
  kensa?: string
  quantity?: number
  pieces?: number
  weight?: number
  weight2?: number
  length?: number
  location?: string
  dispatchDate?: string
  status?: string
  other?: string
  unitPrice?: number
  rate?: number
  warehouse?: string
  factoryAddr?: string
  showFlag?: number
}

export const dispatchApi = {
  list(params?: {
    page?: number
    pageSize?: number
    keyword?: string
    destManager?: string
    showFlag?: string
    status?: string
    dateFrom?: string
    dateTo?: string
  }) {
    return client.get<DispatchPageResponse>('/dispatches', { params })
  },
  get(id: number) {
    return client.get<DispatchVO>(`/dispatches/${id}`)
  },
  getLatestByCode(code: string) {
    return client.get<DispatchVO>(`/dispatches/by-code/${encodeURIComponent(code)}`)
  },
  create(data: CreateDispatchRequest) {
    return client.post<number>('/dispatches', data)
  },
  update(id: number, data: UpdateDispatchRequest) {
    return client.put<void>(`/dispatches/${id}`, data)
  },
  delete(id: number) {
    return client.delete<void>(`/dispatches/${id}`)
  },
  patchStatus(id: number, status: string) {
    return client.patch<void>(`/dispatches/${id}/status`, { status })
  },
  patchBatchStatus(ids: number[], status: string) {
    return client.patch<number>('/dispatches/batch-status', { ids, status })
  },
  exportCsv(params?: {
    keyword?: string
    destManager?: string
    status?: string
    dateFrom?: string
    dateTo?: string
  }) {
    return client.get('/dispatches/export', { params, responseType: 'blob' })
  },
}