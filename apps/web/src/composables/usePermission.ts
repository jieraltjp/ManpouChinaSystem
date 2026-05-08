/**
 * 权限判断 composable。
 * 支持精确权限匹配 + 通配符匹配（*:* / module:*）。
 * 详见 docs/pro/00-root-project.md §4
 */
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function usePermission() {
  const auth = useAuthStore()

  /**
   * 判断是否拥有指定权限。
   * 支持三种形式：
   * - 精确匹配：hasPermission('procurement:create') → 直接查找
   * - 模块通配：hasPermission('procurement:create') + user has 'procurement:*'
   * - 全局通配：hasPermission('procurement:create') + user has '*:*'
   */
  function hasPermission(permission: string): boolean {
    const perms = auth.claims?.permissions ?? []

    // 精确匹配
    if (perms.includes(permission)) return true

    // 全局通配 *:* → 拥有所有权限
    if (perms.includes('*:*')) return true

    // 模块通配 procurement:* → 匹配该模块下所有权限
    const module = permission.split(':')[0]
    if (perms.includes(`${module}:*`)) return true

    return false
  }

  /** 判断是否拥有指定角色 */
  function hasRole(role: string): boolean {
    return auth.claims?.roles?.includes(role) ?? false
  }

  /** 判断是否为管理员 */
  const isAdmin = computed(() => hasRole('ADMIN'))

  return { hasPermission, hasRole, isAdmin }
}
