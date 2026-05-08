# Lesson 75: allinone JWT 过滤器遗漏 permissions 导致 @PreAuthorize 形同虚设

> **发现日期**: 2026-05-08
> **项目**: ManpouChinaSystem
> **影响**: allinone 所有业务 Controller 增删改查无权限拦截

---

## 问题

Phase 3 审计发现：SPEC-B11 文档标注 Phase 3 已完成，但实际权限控制完全失效。

```
表现：任何登录用户都能调用 DELETE /api/v1/procurements/1
预期：只有持有 procurement:delete 权限的用户才能调用
```

---

## 根因（两条）

### 根因 1：JwtAuthenticationFilter 只提取 roles

```java
// allinone JwtAuthenticationFilter.java 第61-63行（修复前）
List<SimpleGrantedAuthority> authorities = claims.roles().stream()
    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
    .toList();
// claims.permissions() 被完全忽略！
```

**后果**：JWT payload 中有 `permissions: ["procurement:create", ...]`，但 SecurityContext 为空，`@PreAuthorize` 永远失败。

user-service 的 `JwtAuthenticationFilter` 第87-97行正确提取了 permissions，但 allinone 漏了。

### 根因 2：17个业务 Controller 零注解

已检查全部 Controller：
- `ProcurementController` / `ReplenishmentDemandController` / `ShipmentBatchController`
- `CustomsController` / `JapanCustomsController` / `TaxRefundController` / `FinanceController`
- `LogisticsController` / `ConsolidationPoolController` / `ContainerController`
- `ProductController` / `FactoryController` / `WarehouseController`
- `NotificationController` / `SalesRecordController`
- `OrderOverviewController` / `QcRecordController` / `QcImageController`

全部 **零个** `@PreAuthorize` 注解。

---

## 修复

### 修复 1：JwtAuthenticationFilter 添加 permissions 提取逻辑

```java
// 与 user-service JwtAuthenticationFilter 同步
private static final Set<String> ALL_PERMISSIONS = Set.of(
    "demand:create", "demand:read", "demand:update", "demand:delete",
    "procurement:create", "procurement:read", "procurement:update", "procurement:delete",
    // ... 66 条
    "warehouse:create", "warehouse:read", "warehouse:update", "warehouse:delete",
    "notification:create", "notification:read", "notification:update", "notification:delete",
    "user:create", "user:read", "user:update", "user:delete", "user:reset_password",
    "role:create", "role:read", "role:update", "role:delete", "role:assign",
    "audit:read"
);

// 构建 authorities
List<SimpleGrantedAuthority> authorities = new ArrayList<>();

// 角色 → ROLE_<role>
for (String role : claims.roles()) {
    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
}

// 权限 → 直接作为 authority
if (claims.permissions() != null) {
    boolean isAdmin = claims.roles().contains("ADMIN");
    for (String perm : claims.permissions()) {
        if (isAdmin && "*:*".equals(perm)) {
            authorities.add(new SimpleGrantedAuthority(perm));
            for (String specific : ALL_PERMISSIONS) {
                authorities.add(new SimpleGrantedAuthority(specific));
            }
        } else {
            authorities.add(new SimpleGrantedAuthority(perm));
        }
    }
}
```

### 修复 2：17个 Controller 加 @PreAuthorize

| HTTP 方法 | 注解 |
|-----------|------|
| GET | `@PreAuthorize("hasAuthority('module:read')")` |
| POST | `@PreAuthorize("hasAuthority('module:create')")` |
| PUT / PATCH | `@PreAuthorize("hasAuthority('module:update')")` |
| DELETE | `@PreAuthorize("hasAuthority('module:delete')")` |
| 状态变更（submit/start/complete/link） | `@PreAuthorize("hasAuthority('module:update')")` |

---

## 预防

| 检查点 | 做法 |
|--------|------|
| JWT 过滤器新增 claim | 两端同步（user-service + allinone） |
| 新增 Controller | 必须加 `@PreAuthorize` |
| 新增 API 模块 | 在 `ALL_PERMISSIONS` 同步添加 |
| 代码审计 | 每轮审计检查 `JwtAuthenticationFilter` 和 Controller 注解 |
