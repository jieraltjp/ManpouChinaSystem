import client from './client'

export interface LegacyProcurementStatsDTO {
  total: number
  withContainer: number
  withImg: number
}

export interface LegacyProcurementPageVO {
  legacyId?: number
  code?: string
  subCode?: string
  img?: string
  itemName?: string
  orderGroup?: string
  orderCount?: number
  inspectCount?: number
  yoyakuHasoubi?: string
  arrivalDepo?: string
  departure?: string
  arrival?: string
  arrivalJikan?: number
  arrivalFlag?: number
  unitCh?: number
  totalCh?: number
  unitJp?: number
  totalJp?: number
  rate?: number
  fbaStock?: number
  houkoku?: string
  kaitsuke?: number
  hyoten?: number
  kanpu?: string
  neStock?: string
  container?: string
  boxNum?: string
  boxCount?: number
  kg?: number
  oneM3?: number
  allM3?: number
  material?: string
  materialCh?: string
  height?: number
  width?: number
  depth?: number
  infoFile1?: string
  infoFile2?: string
  note?: string
  receive?: string
  updater?: string
  updatetime?: string
}

export interface LegacyProcurementPageResponse {
  content: LegacyProcurementPageVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

/** 新建/编辑表单数据 */
export interface LegacyProcurementFormData {
  code?: string
  subCode?: string
  itemName?: string
  orderGroup?: string
  orderCount?: number
  inspectCount?: number
  yoyakuHasoubi?: string
  arrivalDepo?: string
  departure?: string
  arrival?: string
  arrivalJikan?: number
  unitCh?: number
  totalCh?: number
  unitJp?: number
  totalJp?: number
  rate?: number
  fbaStock?: number
  houkoku?: string
  kaitsuke?: number
  hyoten?: number
  kanpu?: string
  neStock?: string
  container?: string
  boxNum?: string
  boxCount?: number
  kg?: number
  oneM3?: number
  allM3?: number
  material?: string
  materialCh?: string
  height?: number
  width?: number
  depth?: number
  infoFile1?: string
  infoFile2?: string
  note?: string
  receive?: string
}

export const legacyProcurementApi = {
  list(params: { page?: number; pageSize?: number; code?: string; orderGroup?: string; itemName?: string; updater?: string }) {
    return client.get('/legacy-procurements', { params })
  },
  stats() {
    return client.get('/legacy-procurements/stats')
  },
  get(id: number) {
    return client.get(`/legacy-procurements/${id}`)
  },
  create(data: LegacyProcurementFormData) {
    return client.post<LegacyProcurementPageVO>('/legacy-procurements', data)
  },
  update(id: number, data: LegacyProcurementFormData) {
    return client.put<LegacyProcurementPageVO>(`/legacy-procurements/${id}`, data)
  },
  delete(id: number) {
    return client.delete(`/legacy-procurements/${id}`)
  },
}