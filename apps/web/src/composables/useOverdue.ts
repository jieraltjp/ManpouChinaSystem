/**
 * 超期检测 composable。
 * 规则：arrivalDepo（倉庫着）优先，否则用 yoyakuHasoubi（予定発送日）。
 * 今天 > 参考日期 → 超期。
 *
 * URL 持久化：传入 route/router 时，overdueOnly 状态与 ?overdue=true 双向同步，
 * 页面刷新或直接访问带 overdue 参数的 URL 均自动恢复过滤状态。
 */
import { type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export interface OverdueableVO {
  arrivalDepo?: string | null
  yoyakuHasoubi?: string | null
}

/**
 * 超期检测核心逻辑（纯函数，无副作用）。
 * @param data - 页面数据源，类型约束
 */
export function useOverdue<T extends OverdueableVO>(_data: Ref<T[]>) {
  function isOverdue(row: T): boolean {
    const refDate = row.arrivalDepo || row.yoyakuHasoubi
    if (!refDate) return false
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return today > new Date(refDate + 'T00:00:00')
  }

  function getRowClassName({ row }: { row: T }): string {
    return isOverdue(row) ? 'overdue-row' : ''
  }

  function filterOverdue(rows: T[]): T[] {
    return rows.filter(isOverdue)
  }

  return { isOverdue, getRowClassName, filterOverdue }
}

/**
 * 将 overdueOnly 状态与 URL query param 同步。
 * 调用方在 setup 中调用一次即可。
 * @param overdueOnly - overdueOnly reactive/computed ref
 * @param onOverdueChange - 超期状态变更回调（用于重置分页等）
 */
export function useOverdueUrlSync(
  overdueOnly: Ref<boolean>,
  onOverdueChange?: (overdue: boolean) => void,
) {
  const route = useRoute()
  const router = useRouter()

  // 初始化：从 URL 恢复状态
  if (route.query.overdue === 'true') {
    overdueOnly.value = true
  }

  // URL → state（导航时）
  if (route.query.overdue === undefined && overdueOnly.value) {
    overdueOnly.value = false
  }

  // state → URL（用户交互时）
  return function syncToQuery(overdue: boolean) {
    const query = { ...route.query }
    if (overdue) {
      query.overdue = 'true'
    } else {
      delete query.overdue
    }
    router.replace({ query })
    onOverdueChange?.(overdue)
  }
}
