/**
 * 货物尺寸管理 API 客户端。
 * 对应 SPEC-B15 §6 API 设计。
 */
import client from './client'

export type CargoSizeStatus = 'PENDING' | 'PROMOTED' | 'DISCARDED'

interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  pageNumber: number
}

export interface CargoSizeVO {
  id: number
  masterCode: string
  subCode?: string
  code: string
  legacyId?: number
  updateTime?: string
  inputUser?: string
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  netWeightKg?: number
  packHeightCm?: number
  packWidthCm?: number
  packDepthCm?: number
  packageWeightKg?: number
  unitsPerPackage?: number
  volumeCbm?: number
  status: CargoSizeStatus
  productId?: number
  promotedBy?: string
  promotedAt?: string
  remarks?: string
  showFlag?: string
  createTime?: string
  updateTime2?: string
}

export interface CargoSizeQueryParams {
  keyword?: string
  status?: CargoSizeStatus
  page?: number
  size?: number
}

export interface CargoSizeCreateCmd {
  masterCode: string
  subCode?: string
  code: string
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  netWeightKg?: number
  packHeightCm?: number
  packWidthCm?: number
  packDepthCm?: number
  packageWeightKg?: number
  unitsPerPackage?: number
  remarks?: string
}

export interface CargoSizeUpdateCmd {
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  netWeightKg?: number
  packHeightCm?: number
  packWidthCm?: number
  packDepthCm?: number
  packageWeightKg?: number
  unitsPerPackage?: number
  remarks?: string
}

export interface CargoSizePromoteCmd {
  nameZh: string
  nameEn?: string
  category?: string
  unit?: string
  unitPriceRmb?: number
  origin?: string
  factoryIds?: number[]
  hsCode?: string
  remarks?: string
}

/** 货物尺寸列表 */
export function getCargoSizes(params: CargoSizeQueryParams) {
  return client.get<PageResult<CargoSizeVO>>('/cargo-sizes', { params })
}

/** 新增货物尺寸 */
export function createCargoSize(cmd: CargoSizeCreateCmd) {
  return client.post<CargoSizeVO>('/cargo-sizes', cmd)
}

/** 货物尺寸详情 */
export function getCargoSize(id: number) {
  return client.get<CargoSizeVO>(`/cargo-sizes/${id}`)
}

/** 升格为商品 */
export function promoteCargoSize(id: number, cmd: CargoSizePromoteCmd) {
  return client.post<CargoSizeVO>(`/cargo-sizes/${id}/promote`, cmd)
}

/** 废弃货物尺寸 */
export function discardCargoSize(id: number) {
  return client.post<CargoSizeVO>(`/cargo-sizes/${id}/discard`, {})
}

/** 更新货物尺寸 */
export function updateCargoSize(id: number, cmd: CargoSizeUpdateCmd) {
  return client.put<CargoSizeVO>(`/cargo-sizes/${id}`, cmd)
}

/** 删除货物尺寸 */
export function deleteCargoSize(id: number) {
  return client.delete(`/cargo-sizes/${id}`)
}

/** 触发导入 */
export function triggerImport() {
  return client.post('/internal/item-size/import')
}
