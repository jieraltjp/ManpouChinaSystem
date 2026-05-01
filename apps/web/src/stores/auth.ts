/**
 * 认证状态管理（Pinia）。
 * 详见 docs/pro/00-root-project.md §3
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, fetchPublicKey } from '@/api/adapters/auth'
import type { LoginCmd } from '@/types/api'
import type { UserClaims } from '@/types/user'
import { jwtDecode } from 'jwt-decode'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('accessToken'))
  const publicKey = ref<string | null>(localStorage.getItem('publicKey'))
  const claims = ref<UserClaims | null>(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => claims.value?.roles.includes('ADMIN') ?? false)

  /** 解码 token 获取用户信息 */
  function decodeToken(t: string) {
    try {
      claims.value = jwtDecode<UserClaims>(t)
    } catch (err) {
      console.error('[AuthStore] token decode failed, clearing claims', err)
      claims.value = null
    }
  }

  if (token.value) {
    decodeToken(token.value)
  }

  /** 从服务端加载 RSA 公钥 */
  async function loadPublicKey(): Promise<string> {
    if (publicKey.value) return publicKey.value
    const vo = await fetchPublicKey()
    publicKey.value = vo.publicKey
    localStorage.setItem('publicKey', vo.publicKey)
    return vo.publicKey
  }

  /** 登录 */
  async function login(cmd: LoginCmd): Promise<void> {
    const vo = await apiLogin(cmd)
    token.value = vo.accessToken
    localStorage.setItem('accessToken', vo.accessToken)
    decodeToken(vo.accessToken)
  }

  /** 登出 */
  function logout() {
    token.value = null
    claims.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('publicKey')
  }

  return {
    token,
    claims,
    isAuthenticated,
    isAdmin,
    loadPublicKey,
    login,
    logout,
  }
})
