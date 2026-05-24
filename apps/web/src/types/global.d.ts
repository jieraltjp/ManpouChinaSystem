/**
 * 全局变量声明。
 */
interface Window {
  __userNameMap__?: Record<string, string>
}

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    /** 用户名映射（username → displayName） */
    $userNameMap?: Record<string, string>
  }
}
