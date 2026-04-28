# Element Plus 表格布局规范

> 版本: 1.0.0
> 创建: 2026-04-26
> 来源: Lesson 46 — `el-card` + `el-table` 布局防 ::deep 污染

---

## 1. 正确写法

```html
<!-- ✅ 不写 table-layout（默认 auto）-->
<!-- ✅ 不写 fixed="right"（无横向滚动时禁用）-->
<!-- ✅ 列宽用 min-width，允许表格按内容扩展 -->
<el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
  <el-table-column prop="xxx" label="XXX" min-width="120" />
  <el-table-column prop="xxx" label="XXX" min-width="140" show-overflow-tooltip />
  <el-table-column :label="$t('action')" min-width="150" align="center" />
</el-table>
```

```css
/* ✅ el-card 内边距由外层控制，不覆盖 el-table 内部结构 */
.table-card :deep(.el-card__body) { padding: 16px; }

/* ❌ 禁止：覆盖 el-table 内部 width/flex 计算，破坏 header/body 对齐 */
:deep(.el-table) { width: 100% !important; }          /* ← 删除 */
:deep(.el-table__header) { width: 100% !important; } /* ← 删除 */
```

---

## 2. 规则总结

| 规则 | 说明 | 违反后果 |
|------|------|---------|
| 不写 `table-layout="fixed"` | 保持默认 auto，内容决定列宽 | min-width 不生效 |
| 列宽用 `min-width` | 可扩展；`table-layout: fixed` 下 min-width 被忽略 | 列宽锁死，无法自动扩展 |
| 操作列不写 `fixed="right"` | 无横向滚动时，fixed 列破坏 header/body 对齐 | 表头/表体错位 |
| `::deep` 不覆盖内部 width/flex | 只穿透到子组件根节点，不干预框架内部布局计算 | header/body 同步失效 |
| `el-card__body` padding 外层控制 | 通过 `:deep(.el-card__body)` 设置，不影响子组件 | 表格撑满无边距 |

---

## 3. table-layout: fixed vs auto 关键区别

| 特性 | `table-layout: fixed` | `table-layout: auto`（默认） |
|------|----------------------|-------------------------------|
| 列宽决定方式 | 仅看 `width` 属性，忽略内容 | 参考 `min-width` + 内容计算 |
| `min-width` | **不生效** | **生效** |
| 渲染性能 | 快（跳过内容分析） | 稍慢（需分析所有单元格） |
| 适用场景 | 列宽固定、有横向滚动的大表 | 普通业务列表（无横向滚动） |

> **本项目所有列表页列数 ≤ 12，宽度总和不超过常见屏幕宽度，无横向滚动需求。统一使用 `auto` + `min-width`。**

---

## 4. fixed 列的正确用法

`fixed="right"` 的存在前提：**存在横向滚动**。

```html
<!-- ✅ 场景：列数 > 15，容器宽度不足以展示全部列时 -->
<el-table ... style="overflow-x: auto">
  <el-table-column ... width="80" />
  <!-- ... 很多列 ... -->
  <el-table-column :label="$t('action')" width="160" fixed="right" />
</el-table>
```

本项目所有列表页列数 ≤ 12，**禁止使用 `fixed`**。

---

## 5. ::deep 使用禁区

`::deep` 可以穿透到子组件根节点，但**禁止覆盖框架内部计算逻辑**：

```css
/* ✅ 可以：控制外层样式 */
.table-card :deep(.el-card__body) { padding: 16px; }
.table-card :deep(.el-table) { border-radius: 8px; }

/* ❌ 禁止：覆盖内部 width/flex/布局计算 */
:deep(.el-table) { width: 100% !important; }
:deep(.el-table__header) { width: 100% !important; }
:deep(.el-table__body) { display: block; } /* 破坏 flex 布局 */
```

Element Plus 内部通过 JS 同步 header/body 列宽，`::deep` 强制覆盖会破坏这个同步机制。

---

## 6. 审查清单

新建或修改页面表格时，逐项检查：

- [ ] `el-table` 上没有 `table-layout="fixed"`（如有则删除）
- [ ] 所有 `el-table-column` 使用 `min-width` 而非 `width`
- [ ] 操作列没有 `fixed="right"`
- [ ] 样式中没有 `:deep(.el-table) { width: ... }`
- [ ] 样式中没有 `:deep(.el-table__header) { width: ... }`
- [ ] `el-card` 有 `:deep(.el-card__body)` 显式设置 padding

---

*相关文档：[docs/ui/ARCHITECTURE.md §8](../ui/ARCHITECTURE.md#8-element-plus-表格布局规范) · [docs/lessons/LESSONS-FRONTEND.md#lesson-46](../lessons/LESSONS-FRONTEND.md#lesson-46-element-plus-el-card--el-table-布局须防-deep-污染禁止覆盖内部结构宽度)*
