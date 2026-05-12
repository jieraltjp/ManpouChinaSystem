# Lesson 80-83: 操作日志切面审计日志体系（2026-05-12）

> **日期**: 2026-05-12
> **触发**: 审计日志中 username 全为 null，所有日志记录都像"登录检查"
> **相关 Commit**: `958c669`

---

## Lesson 80: `@EnableAsync` 缺失导致 `@Async` 方法同步执行

### 问题

user-service 的 `AuditLogService.saveAsync()` 声明了 `@Async`，但方法仍同步执行：

```
// saveAsync 声明
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveAsync(AuditLog auditLog) { ... }
```

HTTP POST 到 `/api/v1/audit-logs` 返回后，审计日志仍未写入 DB。

### 根因

user-service 的 `@SpringBootApplication` 类缺少 `@EnableAsync`：

```java
// UserServiceApplication.java — 修复前
@SpringBootApplication(exclude = { ... })
public class UserServiceApplication { }

// UserServiceApplication.java — 修复后
@SpringBootApplication(exclude = { ... })
@EnableAsync  // ← 添加此注解
public class UserServiceApplication { }
```

**无 `@EnableAsync` 时，`@Async` 注解被 Spring 完全忽略**，方法在调用线程同步执行。

### 症状辨别

- 异步方法执行了（主线程等待）但行为与同步无异
- 如果方法内开了新事务（`REQUIRES_NEW`），会污染外层事务
- 日志中看不到"异步线程池"调度迹象（正常异步应有 `SimpleAsyncTaskExecutor-N` 线程名）

### 预防

新增 `@Async` 方法时，同步在启动类或配置类加 `@EnableAsync`。
用 `@SpringBootApplication` 的项目：确认已有或显式添加。

---

## Lesson 81: 审计日志写入 `user_service` DB，不是 `manpou` DB

### 问题

一直查询 `manpou.audit_log` 找不到审计日志，以为 `saveAsync` 失败。
实际上 `user-service` 的 datasource 指向 `user_service` 数据库，审计日志全在那里。

### 根因

user-service datasource 配置：
```yaml
datasource:
  url: jdbc:mysql://.../user_service?...
```

allinone datasource 配置：
```yaml
datasource:
  url: jdbc:mysql://.../manpou?...
```

两服务各自独立数据库。审计日志由 user-service 写入，自然在 `user_service` DB。

### 诊断方法

审计日志查不到时，先确认是哪个 service 负责写入，然后查对应数据库：
```sql
-- user-service → user_service DB
SELECT id, module, action, username FROM user_service.audit_log ORDER BY id DESC LIMIT 5;

-- allinone → manpou DB（当前无业务审计日志）
SELECT id, module, action FROM manpou.audit_log ORDER BY id DESC LIMIT 5;
```

---

## Lesson 82: `UserContext` 接口缺失 `getUsername()` — JWT claims 有值但无法传递

### 问题

审计日志中 `username = null`。虽然 JWT payload 里有 `"username": "admin"`。

### 根因（调用链断裂）

```
JwtAuthenticationFilter:
  JwtClaims claims = jwtService.extractClaims(token);
  // claims.username = "admin"  ✓
  JwtContextHolder.set(claims);  // claims 实现 UserContext

AuditLogAspect:
  payload.put("username", JwtContextHolder.getUsername());  // null ✗
```

`JwtClaims`（实现 `UserContext`）有 `username` 字段，但 `UserContext` 接口本身没有声明 `getUsername()` 方法：

```java
// UserContext.java — 修复前
public interface UserContext {
    String getUserId();
    String getTenantId();
    // 缺少 getUsername() ✗
}

// JwtClaims.java — 有值但接口未暴露
public record JwtClaims(String userId, String username, ...) implements UserContext {
    // username 字段存在，但接口没有对应方法
}
```

### 修复（三处同步）

```java
// 1. UserContext 接口新增
String getUsername();

// 2. JwtClaims 实现
@Override public String getUsername() { return username; }

// 3. JwtContextHolder 暴露
public static String getUsername() {
    UserContext ctx = get();
    return ctx != null ? ctx.getUsername() : null;
}
```

### 预防

接口增加方法时，必须检查所有实现类是否已实现。仅记录修改点不够——要追踪整个调用链。

---

## Lesson 83: `@PreAuthorize` 在内部方法调用时绕过 Spring AOP 代理

### 问题

发现 `@PreAuthorize("hasAuthority('procurement:create')")` 在 `ProcurementUseCase.create()` 上（Service 层），但 jiangjie(VIEWER) 调用 `POST /api/v1/procurements` 时仍能通过校验（HTTP 200）。

### 根因

Spring AOP 代理机制的限制：**同一类内部方法调用不走代理**。

```java
// ProcurementController.java
@PostMapping
@PreAuthorize("hasAuthority('procurement:create')")  // ← 有效（入口）
public Result<Long> create(...) {
    procurementUseCase.create(cmd);  // ← 内部调用，不走 Spring 代理
}

// ProcurementUseCase.java
@Transactional
@PreAuthorize("hasAuthority('procurement:create')")  // ← 无效！
public Long create(...) { ... }
```

Controller → UseCase 是同一 JVM 内的 Java 方法调用，绕过了 Spring 的 AOP 代理链。只有来自 Servlet 容器（HTTP 请求）的入口才经过代理。

### 正确做法

**`@PreAuthorize` 必须加在 Controller 层**，Service 层注解仅用于深度防御。

```java
// ✓ 正确：Controller 层做权限判断
@RestController
public class XxxController {
    @PostMapping
    @PreAuthorize("hasAuthority('xxx:create')")  // ← 主要防线
    public Result<Void> create(...) { ... }
}

// ✓ 防御性：Service 层也可加（但要意识到可能绕过）
@Service
public class XxxUseCase {
    @Transactional
    @PreAuthorize("hasAuthority('xxx:create')")  // ← 深度防御，不可靠
    public Long create(...) { ... }
}
```

### 验证方法

用 curl 直接测 Controller 端点（走 HTTP → 经过代理）：
```bash
# 走代理：有效
curl -X POST -H "Authorization: Bearer $TOKEN" ...

# 内部调用：不走代理（无法直接测，只有代码审查）
# 检查 Controller → UseCase 是否在同类内调用
```

### 预防

新增 `@PreAuthorize` 时，确认加在 Controller 层。Service 层注解作为补充说明，不作主要依赖。
