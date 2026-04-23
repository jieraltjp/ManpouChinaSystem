/**
 * 商品目录 API 客户端。
 * 与 docs/business/SPEC-B10-商品目录-产品管理.md §2.1 API 设计完全对齐。
 */
import client from './client'

export type ProductCategory = 'OEM' | 'ORDINARY' | 'FACTORY_DIRECT'

export interface ProductPageVO {
  id: number
  masterCode: string
  subCode?: string
  nameJa?: string
  nameEn?: string
  nameZh?: string
  imageUrl?: string
  colorName?: string
  material?: string
  category?: ProductCategory
  origin?: string
  unit?: string
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  volumeCbm?: number
  netWeightKg?: number
  grossWeightKg?: number
  unitPriceRmb?: number
  taxPoint?: number
  taxRate?: number
  hsCode?: string
  declarationElements?: string
  unitsPerPackage?: number
  packageLengthCm?: number
  packageWidthCm?: number
  packageHeightCm?: number
  packageVolumeCbm?: number
  packageWeightKg?: number
  warehouse?: string
  requiresQc?: boolean
  remarks?: string
  lastUsedDate?: string
  createBy?: string
  createTime?: string
  updateTime?: string
}

export interface ProductPageResponse {
  content: ProductPageVO[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CreateProductRequest {
  masterCode: string
  subCode?: string
  nameJa?: string
  nameEn?: string
  nameZh?: string
  imageUrl?: string
  colorName?: string
  material?: string
  category?: ProductCategory
  origin?: string
  unit?: string
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  netWeightKg?: number
  grossWeightKg?: number
  unitPriceRmb?: number
  taxPoint?: number
  taxRate?: number
  hsCode?: string
  declarationElements?: string
  unitsPerPackage?: number
  packageLengthCm?: number
  packageWidthCm?: number
  packageHeightCm?: number
  warehouse?: string
  requiresQc?: boolean
  remarks?: string
}

export interface UpdateProductRequest {
  masterCode?: string
  subCode?: string
  nameJa?: string
  nameEn?: string
  nameZh?: string
  imageUrl?: string
  colorName?: string
  material?: string
  category?: ProductCategory
  origin?: string
  unit?: string
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  netWeightKg?: number
  grossWeightKg?: number
  unitPriceRmb?: number
  taxPoint?: number
  taxRate?: number
  hsCode?: string
  declarationElements?: string
  unitsPerPackage?: number
  packageLengthCm?: number
  packageWidthCm?: number
  packageHeightCm?: number
  warehouse?: string
  requiresQc?: boolean
  remarks?: string
}

export interface MasterCodeSuggestVO {
  masterCode: string
  nameZh?: string
  colorCount: number
}

export interface SubCodeSuggestVO {
  subCode: string
  colorName?: string
}

export interface ProductFactoryVO {
  productId: number
  factoryId: number
  supplierSku?: string
  moq?: number
  leadTimeDays?: number
  unitPriceRmb?: number
  isPreferred?: boolean
  factoryCode?: string
  factoryName?: string
  province?: string
  city?: string
  contactName?: string
  contactPhone?: string
  cooperationStatus?: string
}

export const productApi = {
  list(params: { page?: number; pageSize?: number; masterCode?: string; keyword?: string; hsCode?: string }) {
    return client.get<{ code: string; data: ProductPageResponse }>('/products', { params })
  },
  get(id: number) {
    return client.get<{ code: string; data: ProductPageVO }>(`/products/${id}`)
  },
  getByCode(masterCode: string) {
    return client.get<{ code: string; data: ProductPageVO }>(`/products/code/${masterCode}`)
  },
  create(data: CreateProductRequest) {
    return client.post<{ code: string; data: number }>('/products', data)
  },
  update(id: number, data: UpdateProductRequest) {
    return client.patch<{ code: string }>(`/products/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/products/${id}`)
  },
  suggestMasterCodes(keyword: string) {
    return client.get<{ code: string; data: MasterCodeSuggestVO[] }>('/products/suggest/master-codes', {
      params: { keyword },
    })
  },
  suggestSubCodes(masterCode: string) {
    return client.get<{ code: string; data: SubCodeSuggestVO[] }>('/products/suggest/sub-codes', {
      params: { masterCode },
    })
  },
  getProductFactories(id: number) {
    return client.get<{ code: string; data: ProductFactoryVO[] }>(`/products/${id}/factories`)
  },
}
