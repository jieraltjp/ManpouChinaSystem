import client from './client'

export interface DispatchVO {
  id: number
  code: string
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
  rireki?: string
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
  rireki?: string
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
  rireki?: string
}

export const dispatchApi = {
  list(params?: {
    page?: number
    pageSize?: number
    code?: string
    destination?: string
    manager?: string
    showFlag?: string
  }) {
    return client.get<DispatchPageResponse>('/dispatches', { params })
  },
  get(id: number) {
    return client.get<DispatchVO>(`/dispatches/${id}`)
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
}