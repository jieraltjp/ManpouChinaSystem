# LESSON 58 — el-input-number 按钮截断：列宽计算未扣 padding

> **日期**: 2026-04-27
> **触发**: LogisticsPlanPage.vue 新规调配弹窗，货物长/宽/高三列，`el-input-number` 按钮截断
> **根因**: 列宽计算仅扣了 label 和 gutter，漏扣了 `el-form-item` 上下 padding + `el-col` 侧 padding，内容区实际宽度 < 最小可用值 150px

---

## 问题现场

```
820px 弹窗，span 6 三列布局
content = (820 - 100 - 10*2) / 3 ≈ 233px ← 理论值
实际列宽 ≈ 213px
el-input-number: 按钮(30px) + 内容 + 按钮(30px) = 153px 可用
aria-valuenow="0" 显示为 "0"，但宽度不足导致文字被遮挡
```

## 根因分析

### Element Plus el-input-number 内部结构

```
┌─────────────────────────────────────────────────┐
│ [−] │           input content (number)         │ [+] │
└─────────────────────────────────────────────────┘
  30px              剩余 = 列宽 - 60px             30px
```

### 漏扣项（多次嵌套 padding 消耗）

| 层 | padding | 说明 |
|----|---------|------|
| `el-form-item` | 上下各 0px | el-form-item 默认无额外 padding |
| `el-form-item__content` | 左右各 0px | 默认无额外 padding |
| `el-input-number` | 左右各 1px | border + padding |
| `el-input-number .el-input` | 左右各 0px | 默认无额外 padding |
| `el-input__wrapper` | 左右各 11px | 输入框内边距（输入文字不贴边） |
| `el-col` | 左右各 8px | `gutter=16` 时 padding: 0 8px |

### 正确公式

```
el-input-number 最小可用 content 宽度 = 150px（按钮不截断）
最小列宽 = content + 按钮×2 + el-col padding×2
         = 150 + 30×2 + 8×2
         = 150 + 60 + 16
         = 226px

每行 N 列时：
  span N 时 content = (弹窗宽度 - label-width - gutter×2) / N × (N/span值)
  → 实际 content = 列宽 - padding 消耗

快速判定：
  dialog = 820px, gutter = 16, label-width = 100
  span 6: content ≈ 213px → ✅ 够用（理论 233px - 实际 padding 消耗 ≈ 213px）
  span 8: content ≈ 158px → ✅ 宽松（理论 313px - 实际 padding 消耗 ≈ 158px）
  span 4: content ≈ 113px → ❌ 截断（理论 153px - 实际 padding 消耗 ≈ 113px < 150px）
```

---

## 判定速查表

| 弹窗宽度 | label-width | gutter | 列数 | span | content 估算 | 可用? |
|---------|-----------|--------|------|------|-------------|--------|
| 640px | 100 | 16 | 3 | span 6 | ≈ 153px | ✅ |
| 680px | 86 | 10 | 3 | span 6 | ≈ 193px | ✅ |
| 820px | 100 | 16 | 3 | span 6 | ≈ 213px | ✅ |
| 820px | 100 | 16 | 3 | span 8 | ≈ 158px | ✅ |
| 640px | 100 | 16 | 3 | span 4 | ≈ 113px | ❌ |

---

## 手术（本次）

LogisticsPlanPage.vue 新规调配弹窗：

| 项目 | 旧值 | 新值 |
|------|------|------|
| 弹窗宽度 | 640px | 820px |
| 货物尺寸行 | span 8（三列） | span 6（三列） |
| gutter | 16 | 16 |
| cargo content 估算 | ≈ 113px ❌ | ≈ 213px ✅ |

---

## 溯源

- **EV-058**: LogisticsPlanPage el-input-number 按钮截断 → Lesson 58

---

## 铁律

> **el-input-number 列宽计算：content = 列宽 - 60px（按钮×2）- 16px（el-col padding×2）。列宽 < 226px 时按钮截断。**
