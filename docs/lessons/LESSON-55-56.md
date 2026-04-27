# LESSON 55-56 — 表单 el-input-number 布局宽度 + dialog 紧凑设计

> **日期**: 2026-04-27
> **触发**: InspectionPage.vue 新规验货弹窗中 `el-input-number` 加减按钮显示异常
> **根因**: `el-input-number` 所在列宽度（span 4 = 约 133px）不足以容纳「标签 + 输入框 + 两侧按钮」，按钮被截断

---

## LESSON 55 — el-input-number 最小可用列宽

### 判定

`el-input-number` 含左右两个按钮（各 30px），最小可用宽度约 **150px**（不含 label）。

### 公式

```
el-input-number 最小可用 content 宽度 ≈ 150px
el-input-number span 最小值 ≈ 4（当 dialog width=800, gutter=10, label-width=86）
  → content = (800 - 86 - 10*2) / 4 ≈ 173px ✅ 可用
  → span 3 时 content ≈ 231px / 3 ≈ 62px ❌ 太窄
```

### 结论

| el-input-number 每行数量 | 最小 span | 最小 dialog 宽度 |
|------------------------|-----------|-----------------|
| 3 列 | span 4（≈166px content）| 600px+ |
| 4 列 | span 3（≈133px content）| 600px+（紧张） |
| 6 列 | span 2（≈133px content）| 600px+（危险） |

### 手术

InspectionPage.vue 新规弹窗：
- 改为 span 6（3列一行），content ≈ 200px
- 弹窗宽度 680 → 820px
- label-width 100 → 86px

---

## LESSON 56 — 表单 diviser 视觉噪音

### 判定

`el-divider` 分隔条在紧凑表单中是视觉噪音。**同一业务域的字段应自然分组**，不需要显式分隔线。

### 手术

InspectionPage.vue 新规弹窗移除两个 `el-divider`：
- ❌ 删除 `验货信息` 分隔线
- ❌ 删除 `货物信息` 分隔线

### 替代方案

用 `el-row` 自然分行即可。同一语义区的字段紧凑排列，不同语义区之间留 `margin-bottom` 或空白行。

---

## 规范（写入 ARCHITECTURE.md）

```markdown
### el-input-number 布局规范

| 每行字段数 | span 值 | 适用场景 |
|-----------|---------|---------|
| 3 列 | span 6 | el-input-number（推荐） |
| 4 列 | span 6 | el-input-number（紧张） |
| 3 列 | span 8 | el-input-number（宽松） |
| 6 列 | span 4 | 仅 el-select / el-date-picker |

弹窗推荐宽度：800px+（含数字输入）或 900px+（复杂表单）
label-width 推荐：80~88px
```

---

## 溯源

- **EV-055**: span 4 对话框宽度不足 → Lesson 55
- **EV-056**: 无必要 divider → Lesson 56
