# SPEC-B16: 超期过滤（Overdue Filter）全局化与 URL 持久化

## 需求

1. **URL 持久化**：`/base/legacy-procurement?overdue=true` 直接访问自动启用超期过滤
2. **双向同步**：勾选/取消「只显示超期」checkbox，URL 同步更新；刷新页面状态不丢失
3. **全局复用**：`useOverdue` composable 可跨页面复用，避免逻辑散落
4. **翻页安全**：切换超期过滤时自动回第 1 页，避免超期数据在非首页时切换后无数据

---

## 变更范围

### 1. `composables/useOverdue.ts`（新建 → 重构）

**重构前**：仅含 `isOverdue` 纯函数

**重构后**（两个导出）：

```typescript
// 核心检测逻辑
export function useOverdue<T extends OverdueableVO>(_data: Ref<T[]>) {
  // isOverdue(row) → boolean
  // getRowClassName({ row }) → 'overdue-row' | ''
  // filterOverdue(rows) → rows[]
}

// URL query param 同步（可选，各页面按需调用一次）
export function useOverdueUrlSync(
  overdueOnly: Ref<boolean>,          // filterForm.overdueOnly
  onOverdueChange?: (overdue: boolean) => void,  // 重置分页等
) {
  // 初始化：从 ?overdue=true 恢复状态
  // watch：用户切换 → router.replace({ query: { overdue: 'true' } })
  return syncToQuery: (overdue: boolean) => void
}
```

**`OverdueableVO` 接口**：

```typescript
export interface OverdueableVO {
  arrivalDepo?: string | null    // 倉庫着（优先）
  yoyakuHasoubi?: string | null  // 予定発送日（备选）
}
```

**超期判断规则**：

```
参考日期 = arrivalDepo ?? yoyakuHasoubi
today > 参考日期 → 超期
（today 以当天 00:00:00 参与比较，排除时间部分干扰）
```

### 2. `LegacyProcurementPage.vue`

**新增导入**：
```typescript
import { useOverdue, useOverdueUrlSync } from '@/composables/useOverdue'
import { toRef } from 'vue'
```

**移除**：本地 `isOverdue()` 函数

**新增**：
```typescript
// URL 持久化
const syncToQuery = useOverdueUrlSync(
  toRef(filterForm, 'overdueOnly'),
  () => { pagination.page = 1 },  // 切换超期过滤 → 回第1页
)
watch(() => filterForm.overdueOnly, (v: boolean) => { syncToQuery(v) })
```

**checkbox 移除** `@change="onSearch"`（由 watch 接管）

---

## 其他页面扩展指引

如有其他页面需要超期过滤（如 `ProcurementPage`、`DemandPage`），在 `<script setup>` 中：

```typescript
import { useOverdue, useOverdueUrlSync } from '@/composables/useOverdue'
import { toRef } from 'vue'

const { getRowClassName, filterOverdue } = useOverdue(tableRows)

// 表格行样式
<el-table ... :row-class-name="getRowClassName">

// URL 持久化
const syncToQuery = useOverdueUrlSync(
  toRef(filterForm, 'overdueOnly'),
  () => { pagination.page = 1 },
)
watch(() => filterForm.overdueOnly, (v: boolean) => { syncToQuery(v) })
```

**注意**：`useOverdue` 要求 VO 实现 `OverdueableVO` 接口（`arrivalDepo` / `yoyakuHasoubi` 字段）。现有 `LegacyProcurementPageVO` 满足，其他页面需确认 VO 结构。

---

## 已完成文件

| 文件 | 状态 |
|------|------|
| `composables/useOverdue.ts` | ✅ |
| `LegacyProcurementPage.vue` | ✅ |

---

## 待扩展页面（如有需要）

| 页面 | 状态 |
|------|------|
| `ProcurementPage.vue` | ⏸ 按需 |
| `DemandPage.vue` | ⏸ 按需 |

---

## 测试验证

- [ ] 访问 `/base/legacy-procurement` → 默认不过滤超期
- [ ] 访问 `/base/legacy-procurement?overdue=true` → 自动启用超期过滤，表格仅显示超期行
- [ ] 勾选「只显示超期」→ URL 变为 `?overdue=true`，表格自动回第 1 页
- [ ] 取消勾选 → URL 移除 `overdue` 参数
- [ ] 刷新带 `?overdue=true` 的页面 → 状态保持
- [ ] Excel 视图中超期行高亮不变（`getRowClassName` 仍生效）
