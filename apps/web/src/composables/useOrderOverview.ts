/**
 * 订单总览数据 composable。
 * 防腐层：聚合 API 响应，转换为前端可用的状态。
 * 对应文档: docs/business/SPEC-B09-IMPLEMENTATION.md §6.3
 */
import { ref, computed, watch, type Ref } from 'vue'
import { orderOverviewApi, type OrderOverviewVO } from '@/api/orderOverview'

export function useOrderOverview(procurementId: Ref<number>) {
  const overview = ref<OrderOverviewVO | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetch() {
    loading.value = true
    error.value = null
    try {
      const res = await orderOverviewApi.getOverview(procurementId.value)
      overview.value = res.data.data
    } catch (e: unknown) {
      error.value = (e as Error).message ?? '加载订单总览失败'
    } finally {
      loading.value = false
    }
  }

  /** 当前步骤号（1-8），全部完成时返回 8 */
  const currentStep = computed(() => {
    const s = overview.value?.stepStatuses
    if (!s) return 0
    for (let i = 0; i < s.length; i++) {
      if (s[i] !== 'COMPLETED') return i + 1
    }
    return 8
  })

  watch(procurementId, fetch, { immediate: true })

  return { overview, loading, error, currentStep, fetch }
}
