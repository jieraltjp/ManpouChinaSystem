/**
 * 商品图片缓存 composable。
 *
 * 所有 01-09 流程列表页面统一使用此 composable 加载产品图片，
 * 避免每个页面重复 productImageMap ref + loadImageMap() 逻辑。
 *
 * 缓存策略：同一会话内已加载的 masterCode 不重复请求。
 * 用法：
 *   const { imageMap, loadImageMap } = useProductImage()
 *   await loadImageMap(tableData.value)           // 默认用 row.productCode
 *   await loadImageMap(rows, 'masterProductCode') // 自定义 code 字段名
 *
 * 对应 Lesson: Lesson 74（统一 API 模式），Lesson 77（COS URL 干净）
 */
import { ref } from 'vue'
import { productApi } from '@/api/product'

const imageMap = ref<Record<string, string>>({})
const loadedCodes = new Set<string>()

export function useProductImage() {
  /**
   * 根据行数据批量加载 productCode 对应的图片 URL。
   * @param rows - 表格行数据数组
   * @param codeKey - 行数据中产品编码字段名，默认 'productCode'
   */
  async function loadImageMap(rows: Record<string, unknown>[], codeKey = 'productCode') {
    const codes = [...new Set(
      rows
        .map((r) => String(r[codeKey] ?? ''))
        .filter((c) => c.length > 0 && !loadedCodes.has(c)),
    )]
    if (!codes.length) return

    try {
      const res = await productApi.batchGetCategories(codes)
      const data = res.data ?? []
      const newMap: Record<string, string> = {}
      for (const item of data as Array<{ masterCode: string; imageUrl?: string }>) {
        if (item.imageUrl) {
          newMap[item.masterCode] = item.imageUrl
          loadedCodes.add(item.masterCode)
        }
      }
      imageMap.value = { ...imageMap.value, ...newMap }
    } catch {
      // 静默失败，不阻塞业务
    }
  }

  /** 获取单个 productCode 的图片 URL */
  function getImage(productCode: string): string | undefined {
    return imageMap.value[productCode]
  }

  /** 手动注册一条 imageMap（跨组件共享已加载数据） */
  function registerImage(productCode: string, imageUrl: string) {
    imageMap.value = { ...imageMap.value, [productCode]: imageUrl }
    loadedCodes.add(productCode)
  }

  return { imageMap, loadImageMap, getImage, registerImage }
}
