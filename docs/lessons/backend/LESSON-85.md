# Lesson 85: `#_return` SpEL 对 `ResponseEntity` / `Result<List>` 返回类型失效

> **日期**: 2026-05-12
> **触发**: 审计日志链路验证，批量检查 CREATE 端点 return type
> **根因**: `extractReturnId` 只处理 `Result<T>`，对 `ResponseEntity` 和 `Result<List<Long>>` 静默降级

---

## 问题 1: `ResponseEntity<Long>` → ID 变成 `"<200 OK,123,[]>"`

以下端点使用 `resourceId="#_return"` 但返回 `ResponseEntity<Long>`：

| Controller | 方法 | 返回类型 |
|---|---|---|
| `SalesRecordController` | `create()` | `ResponseEntity<Long>` |
| `TaxRefundController` | `create()` | `ResponseEntity<Long>` |

`extractReturnId` 代码：

```java
private String extractReturnId(Object returnValue) {
    if (returnValue instanceof com.manpou.allinone.common.result.Result) {
        // 处理 Result<T>
        Result<Object> r = (Result<Object>) returnValue;
        Object payload = r.getPayload();
        return payload != null ? String.valueOf(payload) : null;
    }
    // ResponseEntity 降级：String.valueOf() 返回 "<200 OK,123,[]>"
    return String.valueOf(returnValue);
}
```

**结果**: audit_log.resourceId = `"<200 OK,123,[]>"` ❌

---

## 问题 2: `Result<List<Long>>` → ID 变成 `"[1, 2, 3]"`

| Controller | 方法 | 返回类型 |
|---|---|---|
| `CustomsController` | `batchCreate()` | `Result<List<Long>>` |
| `JapanCustomsController` | `batchCreate()` | `Result<List<Long>>` |

**结果**: audit_log.resourceId = `"[1, 2, 3]"` ❌

---

## 修复方案

### 方案 A: 统一使用 `Result<T>` 返回类型（推荐）

将所有 CREATE 端点统一返回 `Result<Long>` 或 `Result<List<Long>>`。这样 `extractReturnId` 无需修改：

```java
// 批量创建：只记录主 ID 或记录 "batch:N"
@AuditLog(module = "customs", action = "CREATE", resourceType = "customs",
          resourceId = "#_return")  // 需改为手动传入 cmd.batchSize 等
```

### 方案 B: 增强 `extractReturnId` 支持多类型

```java
private String extractReturnId(Object returnValue) {
    if (returnValue == null) return null;
    try {
        // Result<T>
        if (returnValue instanceof com.manpou.allinone.common.result.Result r) {
            Object payload = r.getPayload();
            if (payload instanceof Long || payload instanceof Integer) {
                return String.valueOf(payload);
            }
            // Result<List<Long>> → 取第一个
            if (payload instanceof List<?> list && !list.isEmpty()) {
                return String.valueOf(list.get(0));
            }
            return payload != null ? String.valueOf(payload) : null;
        }
        // ResponseEntity<Long>
        if (returnValue instanceof org.springframework.http.ResponseEntity<?> re) {
            Object body = re.getBody();
            return body != null ? String.valueOf(body) : null;
        }
        return String.valueOf(returnValue);
    } catch (Exception ex) {
        return String.valueOf(returnValue);
    }
}
```

### 方案 C: 移除 `#_return`，改用显式参数

最安全但需要改动所有批量接口：

```java
@AuditLog(module = "customs", action = "CREATE", resourceType = "customs",
          resourceId = "#cmd.firstId")  // 从 cmd 中显式引用
```

---

## 教训

**#_return 隐式耦合到返回类型**。Controller 方法签名变更（`Result` → `ResponseEntity`）时，注解不会编译报错，但日志 resourceId 会静默损坏。

**预防**: 在 `@AuditLog` 注解上增加元信息，或在 CI 中增加返回类型检查。

---

## 状态

- [ ] `SalesRecordController.create()` → 改为 `Result<Long>`
- [ ] `TaxRefundController.create()` → 改为 `Result<Long>`
- [ ] `CustomsController.batchCreate()` → 改为 `Result<Long>` 或移除 `#_return`
- [ ] `JapanCustomsController.batchCreate()` → 同上
