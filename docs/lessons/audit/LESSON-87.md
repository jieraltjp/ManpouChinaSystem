# Lesson 87: 操作日志 `operatorName` 始终 null — JWT 未携带姓名 claim

> **日期**: 2026-05-12
> **触发**: audit_log 表数据审查，operatorName 列为空
> **根因**: `JwtContextHolder.getUsername()` 只取 JWT 中的 `username` claim，`operatorName`（用户真实姓名）需要额外 claim

---

## 当前状态

```java
// AuditLogAspect.java
payload.put("userId", JwtContextHolder.getUserId());     // ✅ 有值
payload.put("username", JwtContextHolder.getUsername());   // ✅ 有值
payload.put("operatorName", null);                        // ❌ 硬编码 null
payload.put("companyId", null);                           // ❌ 硬编码 null
payload.put("departmentId", null);                       // ❌ 硬编码 null
```

审计日志查询结果：
```json
{
  "userId": "1",
  "username": "admin",
  "operatorName": null,    // ← 始终 null
  "companyId": null,
  "departmentId": null
}
```

---

## 根因分析

JWT payload（user-service 签发）只包含：
```json
{
  "sub": "1",
  "username": "admin",
  "roles": ["ADMIN"],
  "permissions": ["*:*"],
  "tenantId": "HAI-001",
  "iat": 1778554284,
  "exp": 1778640684
}
```

**缺少**: `operatorName`（用户真实姓名）、`companyId`、`departmentId`。

---

## 修复路线图

### Phase 1: JWT claim 扩展（user-service）

修改 user-service 签发 JWT 时添加姓名：

```java
// AuthService.createToken()
return Jwts.builder()
    .claim("sub", user.getId().toString())
    .claim("username", user.getUsername())
    .claim("realName", user.getRealName())           // ← 新增
    .claim("companyId", user.getCompanyId())          // ← 新增
    .claim("departmentId", user.getDepartmentId())    // ← 新增
    .claim("roles", roleNames)
    .claim("permissions", permissionCodes)
    .build();
```

### Phase 2: allinone JWT 解析

修改 `JwtContextHolder` 读取新 claim：

```java
// JwtContextHolder.java
public class JwtContextHolder {
    private static final ThreadLocal<Claims> CLAIMS = new ThreadLocal<>();

    public static String getOperatorName() {
        return CLAIMS.get().get("realName", String.class);  // ← 新增
    }
}
```

### Phase 3: Aspect 使用

```java
// AuditLogAspect.java
payload.put("operatorName", JwtContextHolder.getOperatorName());  // 替换 null
payload.put("companyId", JwtContextHolder.getCompanyId());
payload.put("departmentId", JwtContextHolder.getDepartmentId());
```

---

## 教训

**JWT claim 扩展是跨服务协调变更**。user-service 签发 → allinone 解析，两端必须同步修改。

**当前状态**: operatorName 留空，不阻塞业务追溯（username 已有值）。
