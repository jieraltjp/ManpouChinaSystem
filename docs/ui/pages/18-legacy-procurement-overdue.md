# SPEC-LegacyProcurement 超期标记与筛选

## 需求背景

旧发注管理页面（`/base/legacy-procurement`）需要识别超期货物并高亮，同时支持按超期状态筛选。

## 字段映射

| 业务含义 | 字段名 | i18n 列名 |
|----------|--------|-----------|
| 倉庫着（参考日期） | `arrivalDepo` | 倉庫着 |
| 予定発送日（备用参考日期） | `yoyakuHasoubi` | 予定発送日 |

## 超期判断逻辑

**参考日期选取（优先级）：**
1. `arrivalDepo`（倉庫着）存在 → 以此为准
2. `arrivalDepo` 不存在 → 以 `yoyakuHasoubi`（予定発送日）为准
3. 两者均不存在 → 不触发超期

**超期条件：**
```
today > refDate  →  超期（整行红色背景）
```

注：`departure`（バン詰め）字段不参与超期判断，用于业务含义记录。

## 前端实现

### 筛选器变更

筛选栏追加复选框，置于「查询」「重置」按钮右侧：

```
[品种] [批次号] [品名] [更新人]  [查询] [重置]  ☐ 只显示超期(バン詰め)
```

- **控件**: `el-checkbox`
- **绑定**: `filterForm.overdueOnly`（boolean）
- **触发**: 勾选/取消时自动调用 `onSearch()`

### 表格数据过滤

```typescript
// computed — 前端过滤，不改 API
const displayData = computed(() => {
  if (!filterForm.overdueOnly) return tableData.value
  return tableData.value.filter(isOverdue)
})
```

### 行颜色

`el-table` 添加 `:row-class-name="getRowClassName"`：

| 状态 | 行背景色 |
|------|----------|
| 超期 | `#fde2e2`（淡红）|
| 超期 hover | `#fbd5d5` |
| 正常 | 默认白/斑马纹 |

### 重置行为

「重置」按钮除清空各字段外，同时取消 `overdueOnly` 勾选。

## 文件变更

| 文件 | 变更 |
|------|------|
| `LegacyProcurementPage.vue` | filter checkbox + `displayData` + `isOverdue` + `getRowClassName` + CSS |
| `locales/zh.json` | `legacyProcurement.filter.overdueOnly` |
| `locales/ja.json` | `legacyProcurement.filter.overdueOnly` |
| `docs/ui/pages/XX-legacy-procurement.md` | 本文档 |

## 设计状态

**完成日期**: 2026-05-24
**状态**: 已实现
