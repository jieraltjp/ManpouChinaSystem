/**
 * 前端日志核心模块（SPEC-B17 Phase 2）。
 *
 * 设计原则：
 * - DEBUG/INFO 仅控制台输出，不上报
 * - WARN/ERROR 缓冲至 Pinia store，批量上报后端审计日志
 * - 敏感字段（password/token/secret）自动脱敏
 */

export type LogLevel = 'debug' | 'info' | 'warn' | 'error'

export interface LogEntry {
  time: string       // ISO 8601
  level: LogLevel
  msg: string
  category: LogCategory
  traceId?: string   // 从响应头 X-Trace-Id 提取
  stack?: string     // error only
  meta?: Record<string, unknown>
}

export type LogCategory = 'api' | 'render' | 'router' | 'custom'

// 脱敏关键词（key 命中则整条日志 value 被替换为 [REDACTED]）
const SENSITIVE_KEYS = /^(password|token|secret|credential|key|authorization|api_?key|access_?token)$/i

function mask(_val: unknown): unknown {
  return '[REDACTED]'
}

function sanitize(obj: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(obj)) {
    if (SENSITIVE_KEYS.test(k)) {
      result[k] = mask(v)
    } else if (v !== null && typeof v === 'object' && !Array.isArray(v)) {
      result[k] = sanitize(v as Record<string, unknown>)
    } else {
      result[k] = v
    }
  }
  return result
}

function formatMeta(meta?: Record<string, unknown>): Record<string, unknown> | undefined {
  if (!meta) return undefined
  const s = sanitize(meta as Record<string, unknown>)
  return Object.keys(s).length > 0 ? s : undefined
}

let _traceId = ''
export function setTraceId(id: string) { _traceId = id }
export function getTraceId() { return _traceId }

let _buffer: LogEntry[] = []
let _flushTimer: ReturnType<typeof setTimeout> | null = null

const BUFFER_SIZE = 20
const FLUSH_INTERVAL = 5 * 60 * 1000 // 5 分钟

function scheduleFlush() {
  if (_flushTimer) return
  _flushTimer = setTimeout(() => {
    _flushTimer = null
    flush()
  }, FLUSH_INTERVAL)
}

export function push(entry: Omit<LogEntry, 'time'>) {
  const full: LogEntry = {
    ...entry,
    time: new Date().toISOString(),
    traceId: entry.traceId || _traceId || undefined,
    meta: formatMeta(entry.meta),
  }

  // console 永远输出
  const style = full.level === 'error'
    ? 'color:red;font-weight:bold'
    : full.level === 'warn'
    ? 'color:orange;font-weight:bold'
    : 'color:#888'
  console.log(`[${full.level.toUpperCase()}] [${full.category}] ${full.msg}`, style)

  // WARN/ERROR 缓冲
  if (full.level === 'warn' || full.level === 'error') {
    _buffer.push(full)
    if (_buffer.length >= BUFFER_SIZE) flush()
    else scheduleFlush()
  }
}

export function flush() {
  if (_buffer.length === 0) return
  const entries = _buffer.splice(0)
  if (_flushTimer) {
    clearTimeout(_flushTimer)
    _flushTimer = null
  }
  // 异步上报，不阻塞
  void fetch('/api/v1/audit-logs', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      // token 从 localStorage 直接读取，避免 pinia 循环依赖
      ...(localStorage.getItem('token')
        ? { Authorization: `Bearer ${localStorage.getItem('token')}` }
        : {}),
    },
    body: JSON.stringify({
      action: 'FRONTEND_ERROR_REPORT',
      detail: { entries },
    }),
  }).catch(() => {
    // 上报失败静默，不递归
  })
}

// 公开日志 API
export const logger = {
  debug(msg: string, meta?: Record<string, unknown>) {
    push({ level: 'debug', msg, category: 'custom', meta })
  },
  info(msg: string, meta?: Record<string, unknown>) {
    push({ level: 'info', msg, category: 'custom', meta })
  },
  warn(msg: string, meta?: Record<string, unknown>) {
    push({ level: 'warn', msg, category: 'custom', meta })
  },
  error(msg: string, meta?: Record<string, unknown>) {
    push({ level: 'error', msg, category: 'custom', meta })
  },
}
