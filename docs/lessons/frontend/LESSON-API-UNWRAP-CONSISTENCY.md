---
name: API Response Unwrap Consistency
description: client.ts interceptor 解包 Result<T> 后，API 泛型与页面访问必须同步更新，否则大量页面同时无数据
type: feedback
---

# Lesson 74: client.ts Result<T> 解包一致性问题

## 触发场景

为解决 UserPage.vue 无数据问题，在 `client.ts` 响应拦截器添加 Result 解包：

```typescript
// apps/web/src/api/client.ts
if (res.data && 'code' in res.data && 'data' in res.data) {
  res.data = res.data.data  // 解包：{code, data} → data
}
```

**后果：** 已有 47 处 `res.data.data` 和 48 处 API 泛型 `{ code: string; data: T }` 全部失效，导致 13 个页面同时无数据。

## 根因

两层不一致：

1. **API 泛型层**：`client.get<{ code: string; data: T }>()` 声明 `res.data` 为 Result 包装类型
2. **页面访问层**：`res.data.data.content` 假设 Result 存在
3. **拦截器**实际将 `res.data` 替换为 payload（解包后）

三者之间任何一层单独修改都会造成不一致。

## 手术

**原则：统一在拦截器处理解包，API 泛型和页面访问层均使用解包后的类型。**

### 1. 拦截器（解包逻辑，保留）

```typescript
// apps/web/src/api/client.ts
if (res.data && 'code' in res.data && 'data' in res.data) {
  res.data = res.data.data
}
```

### 2. API 泛型（统一改为直接类型）

```diff
- client.get<{ code: string; data: ProcurementPageResponse }>('/demands')
+ client.get<ProcurementPageResponse>('/demands')
```

执行（除 client.ts 外所有 API 文件）：
```bash
sed -i "s/client\.get<{ code: string; data: \([^}]*\) }>/client.get<\1>/g" *.ts
sed -i "s/client\.post<{ code: string; data: \([^}]*\) }>/client.post<\1>/g" *.ts
# 同理 patch / delete
```

### 3. 页面访问（`res.data.data` → `res.data`）

```diff
- const payload = res.data.data as { content: T[]; totalElements }
+ const payload = res.data as { content: T[]; totalElements }
- map[codes[i]] = r.value.data.data?.category
+ map[codes[i]] = r.value.data?.category
```

执行（所有 Vue 文件，排除 client.ts）：
```bash
find . \( -name "*.vue" -o -name "*.ts" \) \
  | xargs grep -l "res\.data\.data" \
  | grep -v "client.ts" \
  | xargs sed -i 's/res\.data\.data/res.data/g'
```

### 4. auth.ts 特殊处理

auth 端点的 `isSuccess()` 断言在解包后不再适用：

```diff
- const res = await client.get<ApiResponse<PublicKeyVO>>('/auth/public-key')
- if (!isSuccess(res.data)) { throw ... }
- return res.data!
+ const res = await client.get<PublicKeyVO>('/auth/public-key')
+ return res.data
```

## 验证

```bash
# 无残留
grep -rn "res\.data\.data" --include="*.vue" --include="*.ts" . | grep -v "client.ts"

# 类型检查
npx vue-tsc --noEmit

# 构建
npm run build
```

## 预防

| 时机 | 检查项 |
|------|--------|
| 修改 `client.ts` interceptor | 同步更新所有 API 泛型和页面数据访问 |
| 新增 API 文件 | 泛型直接用 payload 类型，不用 `{code, data}` 包装 |
| 新增页面文件 | 访问 `res.data`（已解包），不访问 `res.data.data` |
| 任何 API 泛型变更 | 同步 grep 检查所有页面是否需要配合修改 |

## Why

Result<T> 统一解包的目的是让业务代码无需感知后端包装格式。一旦在某处解包，所有消费端必须同步更新，否则出现**级联失效**：单个修改导致所有页面同时损坏。
