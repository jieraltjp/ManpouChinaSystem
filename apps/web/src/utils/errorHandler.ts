/**
 * 全局错误收集器（SPEC-B17 Phase 2）。
 *
 * 注册 Vue errorHandler + unhandledrejection + window.onerror。
 * 错误自动写入 logger（缓冲 → 上报后端）。
 */
import { createApp } from 'vue'
import { push } from './logger'

type VueApp = ReturnType<typeof createApp>

export function setupErrorHandler(app: VueApp) {
  // Vue 组件渲染错误
  app.config.errorHandler = (err: unknown, instance: unknown, info: string) => {
    push({
      level: 'error',
      msg: formatError(err),
      category: 'render',
      stack: extractStack(err),
      meta: {
        componentName: (instance as { $options?: { name?: string } })?.$options?.name,
        info,
      },
    })
  }

  // 全局未捕获的 Promise 拒绝
  window.addEventListener('unhandledrejection', (event) => {
    push({
      level: 'error',
      msg: `UnhandledPromiseRejection: ${event.reason}`,
      category: 'render',
      stack: extractStack(event.reason),
    })
  })

  // 全局 JS 运行时错误
  window.addEventListener('error', (event) => {
    push({
      level: 'error',
      msg: event.message,
      category: 'render',
      stack: event.error?.stack,
      meta: {
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
      },
    })
  })
}

function formatError(err: unknown): string {
  if (err instanceof Error) return err.message
  if (typeof err === 'string') return err
  return JSON.stringify(err)
}

function extractStack(err: unknown): string | undefined {
  if (err instanceof Error) return err.stack
  return undefined
}
