# ALL_PERMISSIONS Set（63条）

> **版本**: 1.0.0
> **创建**: 2026-05-12
> **Source of Truth**: `allinone/infrastructure/security/JwtAuthenticationFilter.java` ALL_PERMISSIONS
> **同步**: `user-service/infrastructure/security/JwtAuthenticationFilter.java` ALL_PERMISSIONS（两端必须完全一致）

---

## 完整列表（63条）

```
demand:create, demand:read, demand:update, demand:delete,              -- 4条
procurement:create, procurement:read, procurement:update, procurement:delete,  -- 4条
shipment:create, shipment:read, shipment:update, shipment:delete,       -- 4条
qc:create, qc:read, qc:update, qc:delete,                            -- 4条
logistics:create, logistics:read, logistics:update, logistics:delete,     -- 4条
consolidation:create, consolidation:read, consolidation:update, consolidation:delete, -- 4条
container:create, container:read, container:update, container:delete,  -- 4条
customs:create, customs:read, customs:update, customs:delete,           -- 4条
japan_customs:create, japan_customs:read, japan_customs:update, japan_customs:delete, -- 4条
japan_customs:start, japan_customs:complete,                           -- 2条（DB ID 48,49）
tax_refund:create, tax_refund:read, tax_refund:update, tax_refund:delete, -- 4条
sales:create, sales:read, sales:update, sales:delete,                  -- 4条
factory:create, factory:read, factory:update, factory:delete,           -- 4条
product:create, product:read, product:update, product:delete,           -- 4条
order:read,                                                             -- 1条（DB ID 78）
user:create, user:read, user:update, user:delete, user:approve, user:reset_password, -- 6条
role:create, role:read, role:update, role:delete, role:assign,         -- 5条
permission:read,                                                        -- 1条（DB ID 92）
audit:read, audit:export                                                -- 2条
```

---

## 代码引用

### allinone

| 文件 | 行号 | 用途 |
|------|------|------|
| `infrastructure/security/JwtAuthenticationFilter.java` | 49-69 | ALL_PERMISSIONS Set（63条），ADMIN `*:*` 展开用 |
| `interfaces/controller/*Controller.java` | — | `@PreAuthorize("hasAuthority('xxx:read')")` 注解 |

### user-service

| 文件 | 行号 | 用途 |
|------|------|------|
| `infrastructure/security/JwtAuthenticationFilter.java` | 38-60 | ALL_PERMISSIONS Set（63条），与 allinone 同步 |
| `interfaces/controller/UserController.java` | — | `@PreAuthorize` 注解 |

### 前端

| 文件 | 用途 |
|------|------|
| `composables/usePermission.ts` | `hasPermission(perm)` 守卫 |
| `pages/system/UserPage.vue` | 按钮级 `v-if="hasPermission('user:delete')"` |
| `router/index.ts` | 路由级角色守卫 |

---

## 变更流程

### 新增权限（Phase 5+ 示例：warehouse）

```java
// Step 1: V15__baseline_schema.sql 新增 INSERT
// (115, 'warehouse:read', '查看仓储', '倉庫記録を表示', 'warehouse', 'READ', ...);

// Step 2: 两端 ALL_PERMISSIONS 同步追加
private static final Set<String> ALL_PERMISSIONS = Set.of(
    ...
    "warehouse:create", "warehouse:read", "warehouse:update", "warehouse:delete",
    ...
);

// Step 3: 对应 Controller 加注解
@PreAuthorize("hasAuthority('warehouse:create')")
@PostMapping
public Result<Void> create(...) { ... }
```

### 禁止事项

- ❌ 禁止仅在 ALL_PERMISSIONS 添加而不 INSERT DB
- ❌ 禁止仅在 DB 添加而不更新两端正 Set
- ❌ 禁止 ADMIN is_editable 设为 1（代码硬编码禁止编辑 ADMIN）
