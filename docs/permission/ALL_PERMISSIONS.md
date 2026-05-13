# ALL_PERMISSIONS Set（71条）

> **版本**: 1.1.0
> **创建**: 2026-05-12
> **更新**: 2026-05-13（补 customs:approve、tax_refund:complete；计数 59→71）
> **Source of Truth**: `allinone/infrastructure/security/JwtAuthenticationFilter.java` ALL_PERMISSIONS
> **同步**: `user-service/infrastructure/security/JwtAuthenticationFilter.java` ALL_PERMISSIONS（两端必须完全一致）

---

## 完整列表（71条）

```
demand CRUD:                    demand:create, demand:read, demand:update, demand:delete,              -- 4条
procurement CRUD:              procurement:create, procurement:read, procurement:update, procurement:delete, -- 4条
shipment CRUD:                shipment:create, shipment:read, shipment:update, shipment:delete,       -- 4条
qc CRUD:                      qc:create, qc:read, qc:update, qc:delete,                            -- 4条
logistics CRUD:               logistics:create, logistics:read, logistics:update, logistics:delete,     -- 4条
consolidation CRUD:           consolidation:create, consolidation:read, consolidation:update, consolidation:delete, -- 4条
container CRUD:               container:create, container:read, container:update, container:delete,  -- 4条
customs CRUD:                customs:create, customs:read, customs:update, customs:delete,           -- 4条
customs:approve:              customs:approve,                                                     -- 1条（DB ID 45）
japan_customs CRUD:           japan_customs:create, japan_customs:read, japan_customs:update, japan_customs:delete, -- 4条
japan_customs action:        japan_customs:start, japan_customs:complete,                           -- 2条（DB ID 48,49）
tax_refund CRUD:              tax_refund:create, tax_refund:read, tax_refund:update, tax_refund:delete, -- 4条
tax_refund:complete:          tax_refund:complete,                                                 -- 1条（DB ID 64）
sales CRUD:                   sales:create, sales:read, sales:update, sales:delete,                  -- 4条
factory CRUD:                factory:create, factory:read, factory:update, factory:delete,           -- 4条
product CRUD:                product:create, product:read, product:update, product:delete,           -- 4条
order:read:                  order:read,                                                             -- 1条（DB ID 78）
user:                        user:create, user:read, user:update, user:delete, user:approve, user:reset_password, -- 6条
role:                        role:create, role:read, role:update, role:delete, role:assign,         -- 5条
permission:read:             permission:read,                                                        -- 1条（DB ID 92）
audit:                       audit:read, audit:export                                                -- 2条

小计: 12×4(CRUD) + 1(customs:approve) + 6(japan_customs) + 1(tax:complete)
    + 6(user) + 5(role) + 1(permission) + 1(order) + 2(audit) = 71条
```

**不在 Set 中（有意排除）**：
- warehouse CRUD（ID 101-104）：未实现，无对应 Controller
- notification CRUD（ID 111-114）：未实现，无对应 Controller
- japan_customs:delete（ID 50）：危险操作，ADMIN 专用
- role:delete（ID 91）：危险操作，ADMIN 专用
- permission:read（ID 92）：系统管理，仅 ADMIN

---

## 代码引用

### allinone

| 文件 | 行号 | 用途 |
|------|------|------|
| `infrastructure/security/JwtAuthenticationFilter.java` | 48-69 | ALL_PERMISSIONS Set（71条），ADMIN `*:*` 展开用 |

### user-service

| 文件 | 行号 | 用途 |
|------|------|------|
| `infrastructure/security/JwtAuthenticationFilter.java` | 41-61 | ALL_PERMISSIONS Set（66条，ship:* 未入 user-service Set） |

> **注意**：user-service Set 与 allinone Set 条数不同（66 vs 71），差异为 ship CRUD（ship:* 未在 user-service 出现），这是正确的。

### 前端

| 文件 | 用途 |
|------|------|
| `composables/usePermission.ts` | `hasPermission(perm)` 守卫 |
| `pages/system/UserPage.vue` | 按钮级 `v-if="hasPermission('user:delete')"` |
| `router/index.ts` | 路由级角色守卫 |

---

## DB 权限总览（83条）

| 范围 | 条数 | 说明 |
|------|------|------|
| ALL_PERMISSIONS Set | 71条 | ADMIN `*:*` 展开包含 |
| warehouse CRUD | 4条 | DB 有，Set 无（未实现） |
| notification CRUD | 4条 | DB 有，Set 无（未实现） |
| DB 总计 | 83条 | V15基线78 + V18 ship 4 + V19 japan_customs:update 1 |

---

## 变更记录

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-05-12 | 1.0.0 | 初始版本（59条，遗漏 customs:approve、tax_refund:complete） |
| 2026-05-13 | 1.1.0 | 补充 customs:approve、tax_refund:complete；计数更正为 71 条 |

---

## 变更流程

### 新增权限（示例：将 warehouse CRUD 从 DB 加入 Set）

warehouse CRUD 已在 V15 DB（ID 101-104），但未入 ALL_PERMISSIONS Set：

```java
// Step 1: ✅ DB 已有 INSERT（V15）
// (101, 'warehouse:read',   ...)
// (102, 'warehouse:create', ...)
// (103, 'warehouse:update', ...)
// (104, 'warehouse:delete', ...)

// Step 2: 两端 ALL_PERMISSIONS 同步追加
private static final Set<String> ALL_PERMISSIONS = Set.of(
    ...
    "warehouse:read", "warehouse:create", "warehouse:update", "warehouse:delete",
    ...
);

// Step 3: WarehouseController 已有 @PreAuthorize
@PreAuthorize("hasAuthority('warehouse:create')")
@PostMapping
public Result<Void> create(...) { ... }
```

### 禁止事项

- ❌ 禁止仅在 ALL_PERMISSIONS 添加而不 INSERT DB
- ❌ 禁止仅在 DB 添加而不更新两端正 Set
- ❌ 禁止 ADMIN is_editable 设为 1（代码硬编码禁止编辑 ADMIN）
