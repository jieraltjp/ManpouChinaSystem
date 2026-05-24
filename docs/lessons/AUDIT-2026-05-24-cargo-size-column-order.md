# cargo-size 列顺序审计

> **日期**: 2026-05-24
> **来源**: 用户对照参考表格审查 `/base/cargo-size` 列顺序
> **状态**: ✅ 已修复

---

## 参考列顺序（期望）

```
ID → 商品代码 → 高(cm) → 宽(cm) → 深(cm) → 尺寸总计 → 重量(kg)
  → 每包数量 → 包装高(cm) → 包装宽(cm) → 包装深(cm) → 包装尺寸总计 → 包装重量計(kg)
  → 备注 → 名称 → 仓库 → 更新者 → 更新時間
```

**尺寸总计** = 长 + 宽 + 高（线性加和，非体积）

---

## 当前实现（CargoSizePage.vue Tab A，行 104–140）

| # | 当前列 | 字段 | 行号 |
|---|--------|------|------|
| 1 | 主货号 | `masterCode` | 104 |
| 2 | 货号 | `code` | 107 |
| 3 | 支番 | `subCode` | 112 |
| 4 | 长(cm) | `lengthCm` | 115 |
| 5 | 宽(cm) | `widthCm` | 116 |
| 6 | 高(cm) | `heightCm` | 117 |
| 7 | 净重(kg) | `netWeightKg` | 118 |
| 8 | 体积(m³) | 计算值 (L×W×H/10⁶) | 119 |
| 9 | 状态 | `status` | 122 |
| 10 | 录入时间 | `updateTime` | 127 |
| 11 | 操作 | — | 130 |

SPEC-B15 §7.3 定义的列顺序与当前实现一致。

---

## 差异分析（Gap Analysis）

### G1 — 缺少「尺寸总计」列

- **期望**: 长+宽+高 的线性加和，放在「高」后面
- **当前**: 只有 `体积(m³)`（三者乘积/10⁶），无线性加和列
- **影响**: 用户无法快速判断货物外形尺寸是否超标

### G2 — 主表格缺少包装列

VO 已包含以下字段，但仅在详情抽屉（行 219–224）和编辑弹窗中展示，主表格无：

| 字段 | VO 属性 | i18n key |
|------|---------|----------|
| 每包数量 | `unitsPerPackage` | `cargoSize.unitsPerPackage` |
| 包装高(cm) | `packHeightCm` | `cargoSize.packHeightCm` |
| 包装宽(cm) | `packWidthCm` | `cargoSize.packWidthCm` |
| 包装深(cm) | `packDepthCm` | `cargoSize.packDepthCm` |
| 包装尺寸总计 | 需计算 | 需新增 |
| 包装重量(kg) | `packageWeightKg` | `cargoSize.packageWeightKg` |

### G3 — 主表格缺少「备注」列

`remarks` 字段仅在详情抽屉展示（行 227），主表格无此列。

### G4 — 列顺序偏差

- 当前: 长→宽→高→净重→体积
- 期望: 高→宽→深(=长)→尺寸总计→重量
- SPEC 与当前一致（长→宽→高），但用户期望高排前面

### G5 — 术语不统一

- 代码/SPEC 用「长(cm)」，参考 HTML 用「深(cm)」
- 日文 i18n 中 `lengthCm` 已译为「長(cm)」，中文应保持一致用「长(cm)」

### G6 — 参考 HTML 中的列在当前模型中不存在

| 参考列 | 说明 |
|--------|------|
| 名称 | CargoSize 无此字段；升格后变为 Product.nameZh |
| 仓库 | CargoSize 无此字段 |
| 更新者 | VO 有 `inputUser` 但未在表格中展示 |
| 更新時間 | VO 有 `updateTime`/`updateTime2`，当前已展示「录入时间」 |

---

## 修复建议

### 必须修复

| 优先级 | 改动 | 涉及文件 |
|--------|------|----------|
| **P0** | Tab A 表格新增「尺寸总计」列（长+宽+高），放在「高(cm)」之后 | `CargoSizePage.vue` |
| **P0** | 新增 i18n key `cargoSize.column.dimensionTotal` / `cargoSize.dimensionTotal` | `zh.json` / `ja.json` |
| **P1** | Tab A 表格新增包装列：每包数量、包装高、包装宽、包装深、包装尺寸总计（计算列）、包装重量 | `CargoSizePage.vue` |
| **P1** | 新增 i18n key `cargoSize.column.packageDimTotal` / `cargoSize.packageDimTotal` | `zh.json` / `ja.json` |
| **P2** | Tab A 表格新增「备注」列 | `CargoSizePage.vue` |
| **P2** | 新增 i18n key `cargoSize.column.remarks`（已存在 `cargoSize.dialog.remarks`，可复用或新增） | `zh.json` / `ja.json` |

### 可选

| 优先级 | 改动 | 说明 |
|--------|------|------|
| P3 | 调整列顺序为 高→宽→长（与参考一致） | 与 SPEC-B15 冲突，需确认 |
| P3 | 展示 `inputUser` 为「更新者」列 | 参考 HTML 有，当前 VO 有字段但未展示 |

### 建议最终列顺序

```
主货号 → 货号 → 支番 →
高(cm) → 宽(cm) → 长(cm) → 尺寸总计 → 净重(kg) → 体积(m³) →
每包数量 → 包装高(cm) → 包装宽(cm) → 包装深(cm) → 包装尺寸总计 → 包装重量(kg) →
备注 → 状态 → 录入时间 → 操作
```

---

## 影响范围

| 文件 | 改动类型 |
|------|----------|
| `apps/web/src/pages/product/CargoSizePage.vue` | 新增列、新增计算函数、调整列顺序 |
| `apps/web/src/locales/zh.json` | 新增 i18n key |
| `apps/web/src/locales/ja.json` | 新增 i18n key |
| `docs/business/SPEC-B15-货物尺寸管理.md` | §7.3 表格列定义需同步更新 |