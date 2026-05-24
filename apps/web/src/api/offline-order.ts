import client from './client'

export interface OfflineOrderPageVO {
  id: number
  showFlag: number
  code: string
  subCode: string
  houkoku: string
  infoFile: string
  itemName: string
  volumeCount: number
  orderCount: number
  expectedDate: string
  orderDate: string
  arrival: string
  unitCh: number
  rate: number
  souko: string
  factory: string
  contactor: string
  contactorTel: string
  principal: string
  memo: string
  link: string
  updater: string
  updatetime: string
  inventoryNote: number
  rireki: string
}

export interface OfflineOrderPageResponse {
  content: OfflineOrderPageVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateOfflineOrderRequest {
  showFlag?: number
  code?: string
  subCode?: string
  houkoku?: string
  infoFile?: string
  itemName?: string
  volumeCount?: number
  orderCount?: number
  expectedDate?: string
  orderDate?: string
  arrival?: string
  unitCh?: number
  rate?: number
  souko?: string
  factory?: string
  contactor?: string
  contactorTel?: string
  principal?: string
  memo?: string
  link?: string
  inventoryNote?: number
  rireki?: string
}

export interface UpdateOfflineOrderRequest {
  showFlag?: number
  code?: string
  subCode?: string
  houkoku?: string
  infoFile?: string
  itemName?: string
  volumeCount?: number
  orderCount?: number
  expectedDate?: string
  orderDate?: string
  arrival?: string
  unitCh?: number
  rate?: number
  souko?: string
  factory?: string
  contactor?: string
  contactorTel?: string
  principal?: string
  memo?: string
  link?: string
  inventoryNote?: number
  rireki?: string
}

export const offlineOrderApi = {
  list(params: { page: number; pageSize: number; code?: string; itemName?: string; factory?: string; arrival?: string }) {
    return client.get<OfflineOrderPageResponse>('/offline-orders', { params })
  },
  get(id: number) {
    return client.get<OfflineOrderPageVO>(`/offline-orders/${id}`)
  },
  create(data: CreateOfflineOrderRequest) {
    return client.post<number>('/offline-orders', data)
  },
  update(id: number, data: UpdateOfflineOrderRequest) {
    return client.patch<{ code: string }>(`/offline-orders/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/offline-orders/${id}`)
  },
}