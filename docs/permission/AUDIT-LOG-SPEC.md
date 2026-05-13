# 操作日志规格

> **版本**: 1.1.0
> **创建**: 2026-05-12
> **更新**: 2026-05-12（v1.1.0：Phase 4 已完成，链路验证通过；audit:export 未实现）
> **Phase**: ✅ Phase 4 完成

---

## 1. 表结构

来源：`allinone/src/main/resources/db/migration/V15__baseline_schema.sql`

```sql
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id`              bigint NOT NULL AUTO_INCREMENT,
  `trace_id`       varchar(64)  COMMENT '链路追踪ID',
  `user_id`        varchar(64)  NOT NULL COMMENT '操作人ID',
  `username`       varchar(64)  NOT NULL COMMENT '操作人账号',
  `user_name`      varchar(64)  COMMENT '操作人姓名',
  `company_id`     bigint COMMENT '公司ID',
  `department_id`  bigint COMMENT '部门ID',
  `module`         varchar(32)  NOT NULL COMMENT '模块标识',
  `action`         varchar(32)  NOT NULL COMMENT '动作类型',
  `http_method`    varchar(8),
  `http_url`       varchar(256),
  `resource_type`  varchar(64)  COMMENT '资源类型',
  `resource_id`    varchar(64),
  `resource_code`  varchar(64),
  `detail`         json COMMENT '变更详情JSON',
  `ip_address`     varchar(45),
  `user_agent`     varchar(512),
  `request_id`     varchar(64),
  `create_time`    datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_time`    (`user_id`, `create_time`),
  KEY `idx_module_time`  (`module`, `create_time`),
  KEY `idx_resource`     (`resource_type`, `resource_id`),
  KEY `idx_time`        (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

---

## 2. 动作类型（action）

| action | 触发时机 | detail 示例 |
|--------|---------|------------|
| `CREATE` | POST 成功 | `{procurementId, factoryCode, ...}` |
| `UPDATE` | PUT/PATCH 成功 | `{before: {...}, after: {...}}` |
| `DELETE` | DELETE 成功（软删） | `{procurementId, ...}` |
| `STATUS_CHANGE` | 状态字段变更 | `{before: 'DRAFT', after: 'CONFIRMED'}` |
| `LOGIN` | 登录成功 | `{username, ip, userAgent}` |
| `LOGOUT` | 登出 | `{username, sessionDuration}` |
| `REGISTER` | 用户注册 | `{username, email}` |
| `REGISTRATION_APPROVED` | 管理员审核通过 | `{userId, username}` |
| `REGISTRATION_REJECTED` | 管理员审核拒绝 | `{userId, username, reason}` |
| `EXPORT` | 数据导出 | `{resourceType, count, filters}` |

---

## 3. 模块标识（module）

与 permission.module 一致：

| module | 说明 |
|--------|------|
| `user` | 用户管理 |
| `role` | 角色管理 |
| `permission` | 权限管理（只读展示） |
| `audit` | 操作日志 |
| `procurement` | 发注单 |
| `demand` | 补货需求 |
| `shipment` | 出货批次 |
| `qc` | 验货记录 |
| `logistics` | 物流调配 |
| `consolidation` | 拼柜池 |
| `container` | 货柜 |
| `customs` | 国内报关 |
| `japan_customs` | 日本清关 |
| `tax_refund` | 退税记录 |
| `sales` | 销售记录 |
| `factory` | 工厂 |
| `product` | 商品 |
| `notification` | 通知 |

---

## 4. 触发规则

### Phase 4 计划

| 规则 | 说明 |
|------|------|
| `@AuditLog` 注解 | 在 Controller 方法上加注解，自动 AOP 记录 |
| 注解参数 | module（必填）、action（必填）、resourceType（选填） |
| 排除路径 | GET 请求默认不记录（除非显式标注） |
| 脱敏字段 | password、password_hash、token 等不写入 detail |

### Phase 3 已实现（allinone JwtAuthenticationFilter）

仅记录 LOGIN/LOGOUT（通过 `AuditLogAspect` 或 Security Filter）。

---

## 5. API 设计

### 后端

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/audit-logs` | audit:read | 分页查询 |
| GET | `/api/v1/audit-logs/{id}` | audit:read | 详情 |
| GET | `/api/v1/audit-logs/export` | audit:export | 导出 CSV |

**查询参数**（GET `/api/v1/audit-logs`）：

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | long | 操作人 |
| module | string | 模块 |
| action | string | 动作 |
| resourceType | string | 资源类型 |
| resourceId | string | 资源ID |
| startTime | datetime | 开始时间（ISO 8601） |
| endTime | datetime | 结束时间 |
| page | int | 页码（默认0） |
| size | int | 每页（默认20） |

**响应**：
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "traceId": "abc123",
        "userId": "1",
        "username": "admin",
        "userName": "系统管理员",
        "module": "procurement",
        "action": "CREATE",
        "resourceType": "Procurement",
        "resourceId": "100",
        "resourceCode": "PC-2026-001",
        "detail": {"factoryCode": "F-001", "totalAmount": 50000},
        "ipAddress": "192.168.1.100",
        "userAgent": "Mozilla/5.0 ...",
        "createTime": "2026-05-12T10:00:00.000"
      }
    ],
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 前端（Phase 4 ✅ 已完成：AuditLogPage.vue + 链路验证通过）

- `api/auditLog.ts`
- `pages/system/AuditLogPage.vue`（UI-19）
- 路由：`/system/audit-log`

---

## 6. Phase 状态

| 组件 | 状态 | 说明 |
|------|------|------|
| `audit_log` 表 | ✅ | allinone V15，user-service 写入 |
| `@AuditLog` 注解 | ✅ | allinone/common/annotation/AuditLog.java |
| AuditLogAspect AOP | ✅ | allinone/infrastructure/aspect/AuditLogAspect.java（DEBUG 日志已启用）|
| AuditLogClient HTTP 客户端 | ✅ | allinone/infrastructure/client/AuditLogClient.java → user-service |
| 测试端点 | ✅ | POST /api/v1/procurements/test-audit（链路验证通过）|
| 后端 AuditLogController | ✅ | user-service（audit:read @PreAuthorize）|
| 前端 AuditLogPage.vue | ✅ | pages/system/AuditLogPage.vue |
| 前端路由 | ✅ | /system/audit-log（roles=ADMIN/MANAGER） |
| 前端 API + i18n | ✅ | api/auditLog.ts + i18n auditLog.* |
| audit:export（CSV 导出） | ⚠️ 未实现 | user-service 需新增端点 |

---

## 7. 已知缺口

| 缺口 | 风险 | 修复 |
|------|------|------|
| AuditLogPage.vue 无 `hasPermission('audit:read')` | 自定义角色绕过路由守卫可见页面 | 加 `v-if="hasPermission('audit:read')"` |
| CosTestPage.vue 无 roles 限制 | 登录用户均可访问开发调试接口 | 生产环境移除路由或加 ADMIN 限制 |
| application-production.yml USER_SERVICE_URL | 需配置正确 | `USER_SERVICE_URL=http://user-service:18081`（K8s/Docker）|
