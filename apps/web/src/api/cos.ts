/**
 * COS 腾讯云对象存储测试接口。
 */
import client from './client'

export interface CosStatusInfo {
  enabled: boolean
  bucket: string
  region: string
  domain: string
  prefix: string
  maxFileSize: number
  secretIdSet: boolean
  secretKeySet: boolean
}

export interface CosUploadResult {
  url: string
  filename: string
  size: number
  contentType: string
}

/** 查看 COS 配置状态 */
export function getCosStatus() {
  return client.get<CosStatusInfo>('/test/cos/status')
}

/** 上传测试文件 */
export function uploadCosFile(file: File) {
  const form = new FormData()
  form.append('file', file)
  return client.post<CosUploadResult>('/test/cos/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 删除测试文件 */
export function deleteCosFile(url: string) {
  return client.delete('/test/cos/delete', { params: { url } })
}
