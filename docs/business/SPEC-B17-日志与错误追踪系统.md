# SPEC-B17 日志与错误追踪系统

> **版本**: v1.2.0
> **日期**: 2026-05-25
> **状态**: Phase 1/2/3 ✅ 完成（v1.2.0 审计补充：3 个 Controller @AuditLog + 错误码表补全）
> **Owner**: 全组

---

> **审计日志详细规格**：参见 `docs/permission/AUDIT-LOG-SPEC.md`（本文件仅聚焦日志基础设施）


---

## 1. 背景与目标

### 1.1 现状（2026-05-25 审计）

| 层级 | 现状 | 状态 |
|------|------|------|
| 后端 manpou-allinone | `logback-spring.xml`（ERROR_FILE/AUDIT_FILE/JSON/彩色控制台） | ✅ 已完善 |
| 后端 user-service | `logback-spring.xml` + ERROR_FILE | ✅ 已完善 |
| 前端 apps/web | `logger.ts` + `errorHandler.ts` + client.ts 拦截 | ✅ 已实现 |
| 审计日志 | @AuditLog AOP + Auth/Role/User 审计 | ✅ Phase 3 完成 |
| 错误码 | SystemErrorCode 28 个（AUTH/PROC/DEMAND/QC/SHIP/PROD/FACTORY/CUSTOMS/FINANCE/SYS） | ✅ 已完善 |
| 日志设计文档 | SPEC-B17 + AUDIT-LOG-SPEC | ✅ 已完成 |

### 1.2 目标

1. **前端错误收集**：静默上报 JS 运行时错误、API 请求失败
2. **后端错误规范**：统一错误码 + 结构化错误日志格式
3. **分服务日志隔离**：各服务日志文件物理分离（含前端）
4. **错误链路追踪**：traceId 贯穿前后端
5. **可分析性**：日志存储格式支持 grep/JSON 解析

---

## 2. 系统架构

```
[前端 Vue3]
  └── BrowserConsole + ErrorHandler → logger.ts 缓冲
  └── API 错误 → client.ts → logger.ts（提取 X-Trace-Id）
  └── WARN/ERROR → 批量 POST /api/v1/audit-logs

[后端]
  ├── manpou-allinone (18090)
  │     ├── logs/app.log           ← 结构化日志（100MB 滚动，30天）
  │     ├── logs/error.log         ← ERROR/FATAL（100MB 滚动，30天）
  │     └── logs/audit.log          ← 业务审计（200MB 滚动，90天）
  │
  ├── user-service (18081)
  │     ├── logs/app.log           ← 结构化日志
  │     ├── logs/error.log         ← ERROR/FATAL（2026-05-25 新增）
  │     └── logs/audit.log         ← 认证/用户审计

[MySQL]
  └── user_service.audit_logs      ← 审计日志持久化
```

---

## 3. 前端日志基础设施

### 3.1 实现方案（原生，无第三方依赖）

| 文件 | 职责 | 状态 |
|------|------|------|
| `src/utils/logger.ts` | 日志核心（console 输出 + 本地缓冲 + 批量上报） | ✅ |
| `src/utils/errorHandler.ts` | 全局错误收集（Vue errorHandler + unhandledrejection + onerror） | ✅ |
| `src/api/client.ts` 拦截器 | API 请求失败时写入 logger | ✅ |
| `src/main.ts` | 注册 `setupErrorHandler(app)` | ✅ |

不引入 Sentry（需第三方账号），使用原生实现：缓冲 20 条或 5 分钟触发一次批量上报。

### 3.2 logger.ts 设计

```typescript
// src/utils/logger.ts
type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface LogEntry {
  time: string;        // ISO 8601
  level: LogLevel;
  msg: string;
  category?: string;   // 'api' | 'render' | 'router' | 'custom'
  traceId?: string;   // 从响应头 X-Trace-Id 提取
  stack?: string;      // error only
  meta?: Record<string, unknown>;
}

// 上报阈值：缓冲 20 条或距上次上报 > 5 分钟
const BUFFER_SIZE = 20;
const FLUSH_INTERVAL = 5 * 60 * 1000;
```

### 3.3 脱敏规范

前端日志**禁止**记录：
- JWT token（自动从 localStorage key `token` 过滤）
- 密码、表单敏感字段（自动过滤 `password`/`secret`/`credential` 等 key）
- 请求/响应 body 中的文件内容

---

## 4. 后端日志规范

### 4.1 错误码体系（业务异常）

| 前缀 | 范围 | 示例 | 说明 |
|------|------|------|------|
| `AUTH_` | 1001-1099 | `AUTH_1001` | 认证/授权错误 |
| `PROC_` | 2001-2099 | `PROC_2001` | 发注单业务错误 |
| `DEMAND_` | 2101-2199 | `DEMAND_2101` | 需求单错误 |
| `QC_` | 2201-2299 | `QC_2201` | 验货记录错误 |
| `SHIP_` | 3001-3099 | `SHIP_3001` | 船只/货柜错误 |
| `PROD_` | 4001-4099 | `PROD_4001` | 商品错误 |
| `FACTORY_` | 5001-5099 | `FACTORY_5001` | 工厂错误 |
| `CUSTOMS_` | 6001-6099 | `CUSTOMS_6001` | 报关错误 |
| `FINANCE_` | 7001-7099 | `FINANCE_7001` | 财务错误 |
| `SYS_` | 9001-9099 | `SYS_9001` | 系统级错误 |

错误码定义在 `libs/manpou-common/src/main/java/com/manpou/common/exception/SystemErrorCode.java`：
```java
public final class SystemErrorCode {
    public static final String AUTH_1001 = "AUTH_1001"; // Token 已过期或无效
    public static final String PROC_2001 = "PROC_2001"; // 发注单状态不允许此操作
    ...
}
```

### 4.2 日志格式（JSON 结构化）

```json
{
  "time": "2026-05-25T10:30:00.123",
  "level": "ERROR",
  "service": "manpou-allinone",
  "traceId": "a1b2c3d4",
  "thread": "http-nio-18090-exec-5",
  "logger": "com.manpou.xxx.UseCase",
  "msg": "发注单状态不允许此操作",
  "errorCode": "PROC_2001",
  "stackTrace": "...",
  "context": {
    "procurementId": 123,
    "operator": "jiangjie"
  }
}
```

### 4.3 日志分级使用规范

| 级别 | 使用场景 |
|------|----------|
| DEBUG | 开发调试：方法入参、中间变量（生产不输出） |
| INFO | 业务流程节点：创建/更新/删除操作成功 |
| WARN | 可恢复异常：参数校验失败、记录不存在（业务可用） |
| ERROR | 系统异常：NPE、数据库连接失败、业务异常（需告警） |

---

## 5. 审计日志

详见 `docs/permission/AUDIT-LOG-SPEC.md`（v1.2.0）

| 内容 | 状态 | 说明 |
|------|------|------|
| LOGIN 审计 | ✅ | `AuthController.auditLogin()` |
| LOGOUT 审计 | ✅ | `POST /api/v1/auth/logout`（2026-05-25 新增） |
| 角色管理审计 | ✅ | `@AuditLog` RoleController 5 个方法（2026-05-25） |
| 用户管理审计 | ✅ | `@AuditLog` UserController 6 个方法（2026-05-25） |
| 业务模块审计 | ✅ | allinone 各领域 @AuditLog AOP |
| audit:export | ⚠️ 未实现 | user-service 需新增端点 |

---

## 6. 分服务日志文件

### 6.1 目录结构（logs/）

```
logs/
├── app.log                ← manpou-allinone 主日志（100MB 滚动，30天）
├── error.log              ← manpou-allinone ERROR/FATAL（100MB 滚动，30天）
├── audit.log             ← manpou-allinone 业务审计（200MB 滚动，90天）
├── user-service.log      ← user-service 主日志
├── user-service-error.log
├── user-service-audit.log
└── audit.log            ← user-service 认证/用户审计（软链接或独立）
```

> 实际路径由 `logback-spring.xml` 中 `${APP_NAME:-manpou-allinone}` 属性控制

---

## 7. 前端实现文件

| 文件 | 状态 | 说明 |
|------|------|------|
| `src/utils/logger.ts` | ✅ | 日志核心（缓冲+脱敏+批量上报） |
| `src/utils/errorHandler.ts` | ✅ | Vue errorHandler + unhandledrejection + onerror |
| `src/api/client.ts` | ✅ 修改 | API 错误拦截 + traceId 提取 |
| `src/main.ts` | ✅ 修改 | 全局注册 `setupErrorHandler(app)` |
| `src/pages/system/FrontEndLogPage.vue` | ⏸ 可选 | 查看本机缓冲日志（暂不需要） |

### 7.1 上报机制

- **触发**：WARN/ERROR 级别，缓冲 20 条或距上次 > 5 分钟
- **目标**：`POST /api/v1/audit-logs`（前端错误上报格式见 AUDIT-LOG-SPEC.md）
- **脱敏**：password/token/secret/credential 等 key 的 value 自动替换为 `[REDACTED]`
- **链路**：`X-Trace-Id` 从后端响应头提取，写入 MDC 供调试

---

## 8. 实施计划

### Phase 1 — 后端日志基础设施 ✅（2026-05-25 完成）

- [x] `libs/manpou-common` 添加 `SystemErrorCode.java`（28 个错误码）
- [x] `manpou-allinone` 新建 `logback-spring.xml`（ERROR_FILE/AUDIT_FILE/JSON/异步/彩色控制台）
- [x] `user-service` logback ERROR_FILE appender（生产环境独立 error.log）

### Phase 2 — 前端日志基础设施 ✅（2026-05-25 完成）

- [x] `src/utils/logger.ts`（日志核心 + 脱敏 + 缓冲 + 批量上报）
- [x] `src/utils/errorHandler.ts`（Vue errorHandler + unhandledrejection + window.onerror）
- [x] client.ts 拦截器集成（API 错误提取 traceId → logger）
- [x] main.ts 全局注册 `setupErrorHandler(app)`

### Phase 3 — 审计扩展 ✅（2026-05-25 完成）

- [x] 登录审计（user-service `AuthController.auditLogin()`）
- [x] 登出审计（POST `/api/v1/auth/logout` + `auditLogout()`）
- [x] 角色管理审计（RoleController 5 个 @AuditLog 注解）
- [x] 用户管理审计（UserController 6 个 @AuditLog 注解）
- [x] 订单链路删除审计（`OrderOverviewController.deleteChain()` DELETE_CHAIN）
- [x] 老系统导入审计（`LegacyImportList8Controller` CREATE/UPDATE/DELETE）
- [x] 商品数据导入审计（`ItemSizeImportController` IMPORT）
- [ ] 导出操作审计（无导出 API 端点，audit:export 缺口已记录）

### Phase 4 — 可选增强

- [ ] 前端日志查看页 `src/pages/system/FrontEndLogPage.vue`
- [ ] Sentry 集成（生产环境，第三方账号）
- [ ] ELK/Loki 日志聚合
- [ ] 前端日志上报 API（POST `/api/v1/front-logs`）

---

## 9. 质量门禁

- [ ] 前端 `console` 禁止提交（ESLint rule：`no-console`）
- [ ] 错误日志 `log.error` 必须包含 `errorCode` context
- [ ] `@AuditLog` 注解覆盖所有敏感操作
- [ ] `vue-tsc --noEmit` 通过（当前 193 个 pre-existing errors，需清理）
