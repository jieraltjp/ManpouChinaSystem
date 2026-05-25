# 操作日志规格

> **版本**: 1.2.1
> **创建**: 2026-05-12
> **更新**: 2026-05-25（v1.2.1：补充 3 个 Controller 缺失 @AuditLog；新增覆盖审计报告）
> **Phase**: ✅ Phase 3 完成 | ⚠️ audit:export 未实现

---

## 1. 表结构

来源：`user_service.audit_log`（allinone 通过 AuditLogClient HTTP POST 写入）

```sql
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id`              bigint NOT NULL AUTO_INCREMENT,
  `trace_id`       varchar(64)  COMMENT '链路追踪ID',
  `user_id`        varchar(64)  NOT NULL COMMENT '操作人ID',
  `username`       varchar(64)  NOT NULL COMMENT '操作人账号',
  `user_name`      varchar(64)  COMMENT '操作人姓名（operatorName）',
  `company_id`     bigint COMMENT '公司ID（预留）',
  `department_id`  bigint COMMENT '部门ID（预留）',
  `module`         varchar(32)  NOT NULL COMMENT '模块标识',
  `action`         varchar(32)  NOT NULL COMMENT '动作类型',
  `http_method`    varchar(8)   COMMENT 'HTTP方法',
  `http_url`       varchar(256) COMMENT '请求路径',
  `resource_type`  varchar(64)  COMMENT '资源类型',
  `resource_id`    varchar(64)  COMMENT '资源ID',
  `resource_code`  varchar(64)  COMMENT '资源编码',
  `detail`         json         COMMENT '变更详情JSON（脱敏后）',
  `ip_address`     varchar(45)  COMMENT '客户端IP（X-Forwarded-For解析）',
  `user_agent`     varchar(512) COMMENT '浏览器UA',
  `request_id`     varchar(64)  COMMENT 'X-Request-Id',
  `create_time`    datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_time`   (`user_id`, `create_time`),
  KEY `idx_module_time`  (`module`, `create_time`),
  KEY `idx_resource`     (`resource_type`, `resource_id`),
  KEY `idx_time`         (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

> 注意：无 `success` / `error_message` / `is_deleted` 列（纯追加表，不继承 BaseEntity）

---

## 2. 动作类型（action）

| action | 触发时机 | 实现 |
|--------|---------|------|
| `LOGIN` | 登录成功 | `AuthController.auditLogin()` |
| `LOGOUT` | 登出成功 | `AuthController.auditLogout()`（2026-05-25 新增） |
| `CREATE` | POST 成功 | `@AuditLog` |
| `UPDATE` | PUT/PATCH 成功 | `@AuditLog` |
| `DELETE` | DELETE 成功（软删） | `@AuditLog` |
| `ASSIGN_PERMISSIONS` | 角色分配权限 | `@AuditLog` RoleController.assignPermissions() |
| `ASSIGN_ROLES` | 用户分配角色 | `@AuditLog` UserController.assignRoles() |
| `UPDATE_STATUS` | 用户启用/禁用 | `@AuditLog` UserController.updateStatus() |
| `RESET_PASSWORD` | 管理员重置密码 | `@AuditLog` UserController.resetPassword() |
| `PATCH` | 角色属性更新 | `@AuditLog` RoleController.patch() |
| `STATUS_CHANGE` | 业务状态变更 | `@AuditLog`（发注/验货/物流等业务链） |
| `EXPORT` | 数据导出 | **未实现**（audit:export 缺口） |
| `REGISTRATION_APPROVED` | 管理员审核通过 | 预留 |
| `REGISTRATION_REJECTED` | 管理员审核拒绝 | 预留 |

---

## 3. 模块标识（module）

与 permission.module 保持一致：

| module | 说明 | 审计实现 |
|--------|------|----------|
| `auth` | 认证（登录/登出） | ✅ AuthController 手动调用 |
| `user` | 用户管理 | ✅ UserController @AuditLog |
| `role` | 角色管理 | ✅ RoleController @AuditLog |
| `permission` | 权限管理 | 只读展示，不审计 |
| `audit` | 操作日志 | 只读展示，不审计 |
| `procurement` | 发注单 | ✅ @AuditLog |
| `demand` | 补货需求 | ✅ @AuditLog |
| `shipment` | 出货批次 | ✅ @AuditLog |
| `qc` | 验货记录 | ✅ @AuditLog |
| `logistics` | 物流调配 | ✅ @AuditLog |
| `consolidation` | 拼柜池 | ✅ @AuditLog |
| `container` | 货柜 | ✅ @AuditLog |
| `ship` | 船只 | ✅ @AuditLog |
| `customs` | 国内报关 | ✅ @AuditLog |
| `japan_customs` | 日本清关 | ✅ @AuditLog |
| `tax_refund` | 退税记录 | ✅ @AuditLog |
| `sales` | 销售记录 | ✅ @AuditLog |
| `factory` | 工厂 | ✅ @AuditLog |
| `product` | 商品 | ✅ @AuditLog |
| `legacy_procurement` | 直接采购 | ✅ @AuditLog |
| `notification` | 通知 | ✅ @AuditLog |

---

## 4. 触发规则

### allinone 业务模块（@AuditLog AOP）

在 Controller 方法上加 `@AuditLog` 注解，自动 AOP 记录：

```java
@AuditLog(module = "role", action = "CREATE", resourceType = "role")
@AuditLog(module = "user", action = "UPDATE", resourceType = "user", resourceId = "#id")
@AuditLog(module = "role", action = "ASSIGN_PERMISSIONS", resourceType = "role_permission", resourceId = "#id")
```

**注解参数**：
| 参数 | 必填 | 说明 |
|------|------|------|
| `module` | ✅ | 模块标识（与 permission.module 一致） |
| `action` | ✅ | 动作类型 |
| `resourceType` | 选填 | 资源类型（默认 `module` 大写） |
| `resourceId` | 选填 | SpEL 表达式，如 `#id`、`#_return` |
| `resourceCode` | 选填 | SpEL 表达式，取返回值中的编码字段 |

**SpEL 支持**：
- `#paramName` — 方法参数名
- `#_return` — 方法返回值（自动解包 `Result<T>` 取 payload）

**脱敏规则**：password/token/secret/credential 等字段自动替换为 `***`

### user-service（UserServiceAuditLogAspect）

与 allinone 共用同一 `@AuditLog` 注解，但 Service 层在同 JVM 调用，同步写入。

### AuthController 认证日志（手动）

登录/登出不在 AOP 切面，手动调用 `auditLogin()` / `auditLogout()`：
- LOGIN：登录成功后立即调用，写入 `user_service.audit_log`
- LOGOUT：POST `/api/v1/auth/logout` 成功后调用

**排除路径**：GET 请求默认不记录（除非显式标注 `@AuditLog`）

---

## 5. API 设计

### 后端

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/audit-logs` | audit:read | 分页查询 |
| GET | `/api/v1/audit-logs/{id}` | audit:read | 详情 |
| POST | `/api/v1/audit-logs` | permitAll（secret 校验） | allinone 内部写入 |
| GET | `/api/v1/audit-logs/export` | audit:export | **未实现** CSV 导出 |

**查询参数**（GET `/api/v1/audit-logs`）：

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | string | 操作人 |
| module | string | 模块 |
| action | string | 动作 |
| resourceType | string | 资源类型 |
| resourceId | string | 资源ID |
| mainProductCode | string | 按 detail JSON 中货号筛选 |
| subProductCode | string | 按 detail JSON 中子货号筛选 |
| startTime | datetime | 开始时间（ISO 8601） |
| endTime | datetime | 结束时间 |
| page | int | 页码（默认 0） |
| size | int | 每页（默认 20） |

### 前端

| 文件 | 说明 |
|------|------|
| `api/auditLog.ts` | 查询 API |
| `pages/system/AuditLogPage.vue`（UI-19） | 列表 + 详情抽屉 |
| `/system/audit-log` | 路由（ADMIN/MANAGER） |

---

## 6. Phase 状态（v1.2.1）

| 组件 | 状态 | 文件 |
|------|------|------|
| `audit_log` 表 | ✅ | user_service 数据库 |
| `@AuditLog` 注解 | ✅ | `libs/manpou-common/annotation/AuditLog.java` |
| AuditLogAspect AOP（allinone） | ✅ | `infrastructure/aspect/AuditLogAspect.java` |
| UserServiceAuditLogAspect（user-service） | ✅ | `infrastructure/aspect/UserServiceAuditLogAspect.java` |
| AuditLogClient HTTP | ✅ | `infrastructure/client/AuditLogClient.java` → user-service |
| AuthController 登录/登出审计 | ✅ | `auditLogin()` + `POST /api/v1/auth/logout` |
| RoleController @AuditLog | ✅ | 5 个方法（2026-05-25） |
| UserController @AuditLog | ✅ | 6 个方法（2026-05-25） |
| OrderOverviewController @AuditLog | ✅ | `deleteChain()`（2026-05-25 新增） |
| LegacyImportList8Controller @AuditLog | ✅ | CREATE/UPDATE/DELETE（2026-05-25 新增） |
| ItemSizeImportController @AuditLog | ✅ | IMPORT（2026-05-25 新增） |
| allinone 其余 23 个写操作 Controller | ✅ | 100% 覆盖 |
| AuditLogController（user-service） | ✅ | audit:read @PreAuthorize |
| 前端 AuditLogPage.vue | ✅ | pages/system/AuditLogPage.vue |
| 前端路由 | ✅ | /system/audit-log（ADMIN/MANAGER） |
| 前端 API + i18n | ✅ | api/auditLog.ts + zh.json/ja.json |
| 审计覆盖报告 | ✅ | `docs/permission/AUDIT-LOG-COVERAGE.md` |
| audit:export（CSV 导出） | ⚠️ 未实现 | user-service 需新增端点 |

---

## 7. 已知缺口

| 缺口 | 风险 | 修复 |
|------|------|------|
| audit:export 未实现 | 无法导出审计日志 | user-service AuditLogController 新增 `GET /export` |
| AuditLogPage.vue 无 `hasPermission('audit:read')` | 自定义角色绕过路由守卫可见页面 | 加 `v-if="hasPermission('audit:read')"` |
| CosTestController 无 roles 限制 | 登录用户均可访问开发调试接口 | 生产环境移除路由或加 ADMIN 限制 |
| ItemSizeImportController `/internal/` 无 IP 白名单 | 内部导入接口可被内网调用 | 添加 ADMIN 角色限制 |
