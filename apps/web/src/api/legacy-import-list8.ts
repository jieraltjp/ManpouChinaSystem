import client from './client'

export interface LegacyImportList8VO {
  id: number
  code: string
  manager: string
  destination: string
  tax: string
  material: string
  kensa: string
  num: number
  pieces: number
  weight: number
  weight2: number
  length: number
  location: string
  date1: string
  status: string
  other: string
  unitCh: number
  rate: number
  souko: string
  factoryAddr: string
  updatetime: string
  updateuser: string
  showFlag: number
  rireki: string
}

export interface LegacyImportList8PageResponse {
  content: LegacyImportList8VO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface LegacyImportList8UpdateCmd {
  code?: string
  manager?: string
  destination?: string
  tax?: string
  material?: string
  kensa?: string
  num?: number
  pieces?: number
  weight?: number
  weight2?: number
  length?: number
  location?: string
  date1?: string
  status?: string
  other?: string
  unitCh?: number
  rate?: number
  souko?: string
  factoryAddr?: string
  showFlag?: number
  rireki?: string
}

export const legacyImportList8Api = {
  list(params: {
    page?: number
    pageSize?: number
    code?: string
    location?: string
    souko?: string
    destination?: string
  }) {
    return client.get<LegacyImportList8PageResponse>('/legacy-import-list8', { params })
  },
  get(id: number) {
    return client.get<LegacyImportList8VO>(`/legacy-import-list8/${id}`)
  },
  create(data: LegacyImportList8UpdateCmd) {
    return client.post<LegacyImportList8VO>('/legacy-import-list8', data)
  },
  update(id: number, data: LegacyImportList8UpdateCmd) {
    return client.put<LegacyImportList8VO>(`/legacy-import-list8/${id}`, data)
  },
  delete(id: number) {
    return client.delete(`/legacy-import-list8/${id}`)
  },
  count() {
    return client.get<number>('/legacy-import-list8/count')
  },
}
