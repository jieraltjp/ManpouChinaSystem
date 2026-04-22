# 页面规格 — 步骤8：运营销售

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **路由**: `/sales/operations`
> **组件**: `SalesOperationsPage.vue`（占位）
> **对应后端**: `SalesRecord` 聚合根（待实现）
> **依赖文档**: `SPEC-B01-全链路总览.md` · `SPEC-B08-运营销售-步骤8.md`
> **前置步骤**: 步骤7（退税 TaxRefundRecord 已完成）/ 步骤6（日本清关完成后直接上架）
> **反馈**: 步骤8 → 步骤1（补货需求 / 新品立项）

---

## 1. 页面定位

运营销售管理页面。对应业务流第八步。货物在日本清关完成后上架销售，追踪库存、销售数据和退货情况。**关键：此步骤是反馈循环的起点**。

> ⚠️ **占位页面** — 字段和触发时机待运营方确认。当前仅定义页面骨架。

---

## 2. 布局结构（骨架）

```
┌────────────────────────────────────────────────────────────────────┐
│ 页面标题：运营销售管理                       [+ 新规上架]  [库存预警] │
├────────────────────────────────────────────────────────────────────┤
│ 统计卡                                                               │
│ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────┐ │
│ │ 在售商品数 │ │ 总库存    │ │ 本月销量  │ │ 本月退货  │ │预警数 │ │
│ │           │ │           │ │           │ │           │ │       │ │
│ └───────────┘ └───────────┘ └───────────┘ └───────────┘ └───────┘ │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏                                                               │
│ 货号 [________]  销售渠道 [全部▼]  商品状态 [全部▼]  日期范围       │
│                                               [搜索]  [重置]        │
├────────────────────────────────────────────────────────────────────┤
│ 表格（占位字段，待确认）                                             │
│ ┌────────┬────────┬───────┬────────┬───────┬───────┬─────┬────┐  │
│ │货号    │商品名称│在库数 │销量累计│退货数 │销售总额│状态  │操作│  │
│ └────────┴────────┴───────┴────────┴───────┴───────┴─────┴────┘  │
└────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表格列定义（占位）

| 列名 | 字段 | 来源 | 说明 |
|------|------|------|------|
| 货号 | `productCode` | 来自 Procurement | — |
| 子货号 | `subProductCode` | 来自 Procurement | — |
| 商品名称 | `nameJa` | 来自 Product | 日文名称 |
| 销售渠道 | `salesChannel` | 用户输入 | Amazon / メルカリ / 自社サイト / 其他 |
| 上架日期 | `listingDate` | 用户输入 | — |
| 初始库存 | `initialStock` | 用户输入 | 上架时入库数量 |
| 当前库存 | `currentStock` | 用户输入 / 自动扣减 | 实时更新 |
| 本月销量 | `monthlySales` | 用户输入 | 累计 or 月度？ |
| 累计销量 | `salesQuantity` | 用户输入 | — |
| 退货数量 | `returnedQuantity` | 用户输入 | — |
| 退货率 | `returnRate` | 自动计算 | `returnedQuantity / salesQuantity` |
| 实际销售价(JPY) | `sellingPriceJpy` | 用户输入 | — |
| 库存预警 | `lowStockAlert` | 自动判断 | currentStock < threshold → 触发 |
| 状态 | `status` | 系统 | LISTED / LOW_STOCK / OUT_OF_STOCK / DISCONTINUED |
| 操作 | — | — | 详情 / 编辑 / 下架 |

---

## 4. 新规上架弹窗（骨架）

### 4.1 触发

点击 `[+ 新规上架]` → 弹出表单弹窗。

### 4.2 表单字段（占位）

**关联信息（自动代入）**：

| 字段 | 来源 | 说明 |
|------|------|------|
| 采购单 | 用户选择 | 选择 JapanCustomsRecord 已清关的 Procurement |
| 货号 | 自动代入 | — |
| 子货号 | 自动代入 | — |
| 到货日期 | 自动代入 | JapanCustomsRecord.arrivalDate |
| 估算批发价 | 自动代入 | Procurement.estimatedPriceJpy（参考定价） |
| 到货数量 | 自动代入 | Procurement.quantity（作为初始库存） |

**用户填入（待确认）**：

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 销售渠道 | `el-select` | ✅ | Amazon / メルカリ / 自社サイト / 其他 |
| 上架日期 | `el-date-picker` | ✅ | 默认今天 |
| 实际销售价(JPY) | `el-input-number` | | 建议参考 estimatedPriceJpy |
| 初始库存 | `el-input-number` | | 默认带入采购数量 |
| 安全库存阈值 | `el-input-number` | | 低于此值触发预警 |
| 备注 | `el-input`（textarea） | | — |

---

## 5. 库存预警机制

### 5.1 触发条件

`currentStock < safetyThreshold` → 状态 = `LOW_STOCK`
`currentStock == 0` → 状态 = `OUT_OF_STOCK`

### 5.2 预警展示

- 统计卡预警数：`LOW_STOCK + OUT_OF_STOCK` 之和
- `[库存预警]` 按钮点击 → 弹出预警列表弹窗（仅显示 LOW_STOCK / OUT_OF_STOCK 商品）

### 5.3 反馈循环联动

库存预警触发 → 建议生成 `ReplenishmentDemand`（步骤1）：
- 自动填入：`demandType = REPLENISHMENT`，`productCode`，`quantity = safetyStock - currentStock`

---

## 6. 状态枚举（骨架）

```
  LISTED ──[库存<阈值]──▶ LOW_STOCK
     │                          │
     └──[库存=0]──▶ OUT_OF_STOCK│
                                  │
                     ──[重新上架]──▶ LISTED
     │
     └──[停止销售]──▶ DISCONTINUED
```

| 状态 | 颜色 | 说明 |
|------|------|------|
| LISTED | 绿色 | 正常销售 |
| LOW_STOCK | 橙色 | 库存低于预警值 |
| OUT_OF_STOCK | 红色 | 库存为零 |
| DISCONTINUED | 灰色 | 已下架/停售 |

---

## 7. 反馈循环设计

### 循环A — 补货闭环

```
库存预警 (LOW_STOCK / OUT_OF_STOCK)
    │
    ├──[一键补货]──▶ ReplenishmentDemand(demandType=REPLENISHMENT)
    │                    预填充: productCode, quantity, destination, japanLead
    │
    └──[新品需求]──▶ ReplenishmentDemand(demandType=NEW_PURCHASE)
                       来源: 客户询价 / 销售预测
```

### 循环B — 质量反馈

```
退货数量累计 ──[退货率 > 5%]──▶ QcRecord.qcStandard 更新
                                    来源: returnedQuantity 统计
```

### 循环C — 工厂评级

```
退货率 > X% ──[关联 Factory]──▶ Factory.status = INACTIVE
```

---

## 8. API 集成（骨架）

| 操作 | Method | Endpoint | 状态 |
|------|--------|----------|------|
| 分页查询 | GET | `/api/v1/sales-records?page=&pageSize=&productCode=&salesChannel=&status=` | 🔴待实现 |
| 详情 | GET | `/api/v1/sales-records/{id}` | 🔴待实现 |
| 创建上架 | POST | `/api/v1/sales-records` | 🔴待实现 |
| 更新 | PATCH | `/api/v1/sales-records/{id}` | 🔴待实现 |
| 库存扣减 | PATCH | `/api/v1/sales-records/{id}/decrement-stock` | 🔴待实现 |
| 库存预警列表 | GET | `/api/v1/sales-records/alerts` | 🔴待实现 |
| 生成补货需求 | POST | `/api/v1/sales-records/{id}/generate-replenishment` | 🔴待实现 |
| 下架 | PATCH | `/api/v1/sales-records/{id}/discontinue` | 🔴待实现 |

---

## 9. 缺口阻塞

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 销售数据来源 | **P0** | 平台 API（Amazon/メルカリ）接入 or 纯人工维护？ |
| 销售渠道枚举 | **P0** | 与运营方确认所有销售渠道 |
| 库存实时更新 | P1 | 订单成交后自动扣减库存，需对接销售平台 API |
| 退货数据来源 | P1 | 退货数量需手动录入还是自动同步？ |
| 补货需求生成 | P1 | 低库存自动/手动触发 ReplenishmentDemand |
| 安全库存阈值 | P2 | 每种商品单独设定 or 全局默认？ |

---

## 10. 组件拆分建议

| 组件 | 职责 |
|------|------|
| `SalesOperationsPage.vue` | 容器：列表 + 筛选 + 统计卡 |
| `SalesTable.vue` | 表格 + 分页 |
| `SalesFilters.vue` | 筛选栏 |
| `SalesFormDialog.vue` | 新规上架表单 |
| `SalesDetailDrawer.vue` | 详情抽屉 |
| `StockAlertPanel.vue` | 库存预警列表弹窗 |
| `StockTrendChart.vue` | 库存/销量趋势图（ECharts） |
| `ReplenishmentQuickCreate.vue` | 快速生成补货需求弹窗 |
| `useSalesOperations.ts` | API + 状态逻辑（composable） |
