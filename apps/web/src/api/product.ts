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
  janCode?: string
  nameZh?: string
  nameEn?: string
  nameJa?: string
  material?: string
  materialJa?: string
  origin?: string
  colorName?: string
  imageUrl?: string
  category?: ProductCategory
  status?: string
  unit?: string
  quantities?: number
  cartonQty?: number
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  volumeCbm?: number
  netWeightKg?: number
  grossWeightKg?: number
  unitPriceRmb?: number
  taxPoint?: number
  taxRate?: number
  amountRmb?: number
  hsCode?: string
  hsCodeJp?: string
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
  factoryCount?: number
  factoryNames?: string
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
  janCode?: string
  nameZh?: string
  nameEn?: string
  nameJa?: string
  material?: string
  materialJa?: string
  origin?: string
  colorName?: string
  imageUrl?: string
  category?: ProductCategory
  status?: string
  unit?: string
  quantities?: number
  cartonQty?: number
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  netWeightKg?: number
  grossWeightKg?: number
  unitPriceRmb?: number
  taxPoint?: number
  taxRate?: number
  amountRmb?: number
  hsCode?: string
  hsCodeJp?: string
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
  janCode?: string
  nameZh?: string
  nameEn?: string
  nameJa?: string
  material?: string
  materialJa?: string
  origin?: string
  colorName?: string
  imageUrl?: string
  category?: ProductCategory
  status?: string
  unit?: string
  quantities?: number
  cartonQty?: number
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  netWeightKg?: number
  grossWeightKg?: number
  unitPriceRmb?: number
  taxPoint?: number
  taxRate?: number
  amountRmb?: number
  hsCode?: string
  hsCodeJp?: string
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
  list(params: { page?: number; pageSize?: number; masterCode?: string; keyword?: string; hsCode?: string; hsCodeJp?: string; factoryName?: string }) {
    return client.get<ProductPageResponse>('/products', { params })
  },
  get(id: number) {
    return client.get<ProductPageVO>(`/products/${id}`)
  },
  getByCode(masterCode: string) {
    return client.get<ProductPageVO>(`/products/code/${masterCode}`)
  },
  create(data: CreateProductRequest) {
    return client.post<number>('/products', data)
  },
  update(id: number, data: UpdateProductRequest) {
    return client.patch<{ code: string }>(`/products/${id}`, data)
  },
  delete(id: number) {
    return client.delete<{ code: string }>(`/products/${id}`)
  },
  suggestMasterCodes(keyword: string) {
    return client.get<MasterCodeSuggestVO[]>('/products/suggest/master-codes', {
      params: { keyword },
    })
  },
  suggestSubCodes(masterCode: string) {
    return client.get<SubCodeSuggestVO[]>('/products/suggest/sub-codes', {
      params: { masterCode },
    })
  },
  getProductFactories(id: number) {
    return client.get<ProductFactoryVO[]>(`/products/${id}/factories`)
  },
}
