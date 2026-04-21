/**
 * 权限判断 composable。
 * 详见 docs/pro/00-root-project.md §4
 */
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function usePermission() {
  const auth = useAuthStore()

  /** 判断是否拥有指定权限 */
  function hasPermission(permission: string): boolean {
    return auth.claims?.permissions?.includes(permission) ?? false
  }

  /** 判断是否拥有指定角色 */
  function hasRole(role: string): boolean {
    return auth.claims?.roles?.includes(role) ?? false
  }

  /** 判断是否为管理员 */
  const isAdmin = computed(() => hasRole('ADMIN'))

  return { hasPermission, hasRole, isAdmin }
}
