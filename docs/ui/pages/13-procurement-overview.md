# 页面规格 — 发注单总览（核心视图）

> **版本**: 1.0.0
> **创建**: 2026-04-27
> **路由**: `/base/overview/procurement/:procurementId`
> **组件**: `ProcurementOverviewPage.vue`
> **对应后端**: `GET /api/v1/orders/{procurementId}/overview`
> **API**: `orderOverviewApi.getOverview(procurementId)`

---

## 1. 页面定位

**发注单总览**是整个系统的核心视图。以 Procurement.id 为锚点，横向聚合该订单从「需求录入」到「运营销售」的完整 8 步链路信息。入口为各页面（发注单列表、验货列表、调配列表）的「总览」按钮，或需求单转采购后自动跳转。

---

## 2. 入口与路由

### 入口

| 入口位置 | 触发行为 |
|---------|---------|
| 发注单列表操作列 `[总览]` | 进入 `/base/overview/procurement/{procurementId}` |
| 验货列表操作列 `[总览]` | 进入 `/base/overview/procurement/{procurementId}` |
| 调配列表操作列 `[总览]` | 进入 `/base/overview/procurement/{procurementId}` |
| 需求单转采购成功后（自动跳转） | 进入 `/base/overview/procurement/{procurementId}` |

### 路由

```
/base/overview/procurement/:procurementId  →  发注单总览详情页
```

---

## 3. 页面布局

```
┌──────────────────────────────────────────────────────────────────────────┐
│  [← 返回列表]  订单总览                                                 │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  状态进度条（8步，真实反映各步骤状态）                                    │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                   │
│  │ 1 │→│ 2 │→│ 3 │→│ 4 │→│ 5 │→│ 6 │→│ 7 │→│ 8 │                   │
│  └───┘ └───┘ └───┘ └───┘ └───┘ └───┘ └───┘ └───┘                     │
│  补货  发注  验货  调配  国报  日报  退税  运营                           │
│   ●    ●    ◐    ○    ○    ○    ○    ○    ← 当前状态                   │
│                                                                          │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ 步骤1：补货需求                                      [已完成]     │   │
│  │  需求编号：D-20260401-001  │  需求类型：新品采购                  │   │
│  │  货号：odn012               │  子货号明细：odn012-re × 200 台州    │   │
│  │  担当：田中                │  状态：[CONVERTED]                  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ 步骤2：发注单信息                                      [已完成]     │   │
│  │  发注编号：P-20260401-001  │  工厂：义乌XX厂                      │   │
│  │  货号：odn012               │  数量：200                           │   │
│  │  单价(CNY)：100.00         │  票点：1.10                          │   │
│  │  报关类型：浙鲁开票         │  估算批发价(JPY)：2,593              │   │
│  │  下单日：2026-04-01        │  交货期：2026-04-20                  │   │
│  │  实际出货日：2026-04-18    │  状态：倉庫着                        │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ 步骤3：验货记录                                      [进行中]       │   │
│  │  验货编号：Q-20260418-001   │  结果：[PASS]                       │   │
│  │  检品数：200                │  合格数：198  │  不良数：2           │   │
│  │  验货日期：2026-04-18                                           │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ 步骤4：调配计划                                      [未开始]     │   │
│  │  暂无记录                                                         │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ... (步骤5-8 同上，无数据时均显示"暂无记录")                          │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 步骤卡片字段

### 步骤1 — 补货需求（Demand）

| 字段 | 标签 | 说明 |
|------|------|------|
| `demand.demandCode` | 需求编号 | 关联的需求单编号 |
| `demand.demandType` | 需求类型 | 补货 / 新品采购 |
| `demand.productCode` | 货号 | 主货号 |
| `subProductSummary` | 子货号明细 | `subProductCode` + `quantity` + `destination` |
| `demand.japanLead` | 担当 | 日本担当 |
| `demand.status` | 状态 | PENDING/CONVERTED/CANCELLED + tag |

> 注意：此步骤为关联数据，只读展示，无法从此页面编辑。

---

### 步骤2 — 发注单信息（Procurement）

| 字段 | 标签 | 说明 |
|------|------|------|
| `procurement.procurementCode` | 发注编号 | 主键编号 |
| `procurement.factoryName` | 工厂 | 工厂名称（无值显示 "—"） |
| `procurement.productCode` | 货号 | 主货号 |
| `procurement.quantity` | 数量 | 订购数量（无值显示 "—"） |
| `procurement.priceRmb` | 单价(CNY) | 格式：`¥{priceRmb}`（无值显示 "—"） |
| `procurement.taxPoint` | 票点 | 税点值（无值显示 "—"） |
| `procurement.billingType` | 报关类型 | `billingTypeLabel()` 格式化 |
| `procurement.estimatedPriceJpy` | 估算批发价(JPY) | 格式：`{n} JPY`（无值显示 "—"） |
| `procurement.orderDate` | 下单日 | 日期字符串（无值显示 "—"） |
| `procurement.plannedShipDate` | 交货期 | 计划出货日（无值显示 "—"） |
| `procurement.actualShipDate` | 实际出货日 | 实际出货日（无值显示 "—"） |
| `procurement.status` | 状态 | `procurementStatusLabel()` 格式化，显示中文状态 |

---

### 步骤3 — 验货记录（QcRecord）

| 字段 | 标签 | 说明 |
|------|------|------|
| `qcRecord.qcCode` | 验货编号 | 验货单编号 |
| `qcRecord.result` | 结果 | PASS → 绿色 tag / FAIL → 红色 tag |
| `qcRecord.inspectionCount` | 检品数 | 总检品数量（无值显示 "—"） |
| `qcRecord.passedCount` | 合格数 | 合格数量（无值显示 "—"） |
| `qcRecord.defectiveCount` | 不良数 | 不良数量（无值显示 "—"） |
| `qcRecord.qcDate` | 验货日期 | 日期（无值显示 "—"） |

无数据时显示 `orderOverview.step.notStarted`（"暂无记录"）。

---

### 步骤4 — 调配计划（LogisticsPlan）

| 字段 | 标签 | 说明 |
|------|------|------|
| `logisticsPlan.planCode` | 调配编号 | 调配计划编号 |
| `logisticsPlan.planType` | 运输方式 | SEA → 海运 / AIR → 空运 |
| `logisticsPlan.cargoVolumeCbm` | 体积(CBM) | 格式：`{n} m³`（无值显示 "—"） |
| `logisticsPlan.cargoWeightKg` | 重量(kg) | 格式：`{n} kg`（无值显示 "—"） |
| `logisticsPlan.estimatedShipDate` | 预计发货日 | 日期（无值显示 "—"） |
| `logisticsPlan.actualShipDate` | 实际发货日 | 日期（无值显示 "—"） |
| `logisticsPlan.status` | 状态 | `logisticsStatusLabel()` 格式化 |

无数据时显示 "暂无记录"。

---

### 步骤5 — 国内报关（DomesticCustomsRecord）

| 字段 | 标签 | 说明 |
|------|------|------|
| `domesticCustoms.customsCode` | 报关单号 | 报关单编号 |
| `domesticCustoms.productCode` | 货号 | 报关货号 |
| `domesticCustoms.status` | 状态 | PENDING/SUBMITTED/CLEARED/REJECTED |
| `domesticCustoms.estimatedValueCny` | 申报价值 | 格式：`¥{n}`（无值显示 "—"） |

无数据时显示 "暂无记录"。

---

### 步骤6 — 日本清关（JapanCustomsRecord）

| 字段 | 标签 | 说明 |
|------|------|------|
| `japanCustoms.customsEntryNo` | 入境报关号 | 入境报关编号（无值显示 "—"） |
| `japanCustoms.arrivalDate` | 到达日期 | 日期（无值显示 "—"） |
| `japanCustoms.clearanceDate` | 清关完成日 | 日期（无值显示 "—"） |
| `japanCustoms.customsBroker` | 清关行 | 清关行名称（无值显示 "—"） |
| `japanCustoms.importDutyPaid` | 进口关税(JPY) | 格式：`{n} JPY`（无值显示 "—"） |
| `japanCustoms.consumptionTaxPaid` | 消费税(JPY) | 格式：`{n} JPY`（无值显示 "—"） |
| `japanCustoms.arrivalPort` | 目的港 | 港口名称（无值显示 "—"） |
| `japanCustoms.status` | 状态 | PENDING/IN_PROGRESS/CLEARED/FAILED |

无数据时显示 "暂无记录"。

---

### 步骤7 — 退税（TaxRefundRecord）

| 字段 | 标签 | 说明 |
|------|------|------|
| `taxRefund.refundCode` | 退税编号 | 退税记录编号（无值显示 "—"） |
| `taxRefund.estimatedRefundRmb` | 理论退税额 | 格式：`¥{n}`（无值显示 "—"） |
| `taxRefund.actualRefundRmb` | 实际退税额 | 格式：`¥{n}`（无值显示 "—"） |
| `taxRefund.refundDate` | 退税日期 | 日期（无值显示 "—"） |
| `taxRefund.status` | 状态 | APPLYING/COMPLETED/NO_REFUND |

无数据时显示 "暂无记录"。

---

### 步骤8 — 运营销售（SalesRecord）

| 字段 | 标签 | 说明 |
|------|------|------|
| `salesRecord.recordCode` | 销售编号 | 销售记录编号（无值显示 "—"） |
| `salesRecord.salesChannel` | 销售渠道 | Amazon/Mercari/自有网站/其他 |
| `salesRecord.listingDate` | 上架日期 | 日期（无值显示 "—"） |
| `salesRecord.currentStock` | 当前库存 | 整数（无值显示 "—"） |
| `salesRecord.salesQuantity` | 累计销量 | 整数（无值显示 "—"） |
| `salesRecord.returnRate` | 退货率 | 格式：`{n}%`（无值显示 "—"） |
| `salesRecord.sellingPriceJpy` | 销售价(JPY) | 格式：`{n} JPY`（无值显示 "—"） |
| `salesRecord.status` | 状态 | LISTED/LOW_STOCK/OUT_OF_STOCK/DISCONTINUED |

无数据时显示 "暂无记录"。

---

## 5. 状态进度条

来自 `StatusProgressBar` 组件，`stepStatuses` 来自 `OrderOverviewVO.stepStatuses`（后端计算）：
- `COMPLETED` / 终态 → ● 蓝色
- 非终态（有数据）→ ◐ 进行中色
- 无数据 → ○ 灰色

---

## 6. 格式函数

| 函数 | 文件内位置 | 用途 |
|------|-----------|------|
| `qcResultLabel(result)` | 脚本 | `orderOverview.step3.qcResult.{PASS/FAIL}` |
| `demandTypeLabel(type)` | 脚本 | `demand.type.{REPLENISHMENT/NEW_PRODUCT}` |
| `demandStatusLabel(status)` | 脚本 | `demand.status.{status}` |
| `procurementStatusLabel(s)` | 脚本 | `orderOverview.enum.procurement.{s}` |
| `logisticsStatusLabel(s)` | 脚本 | `orderOverview.enum.logistics.{s}` |
| `logisticsPlanTypeLabel(s)` | 脚本 | `orderOverview.enum.planType.{s}` |
| `domesticCustomsStatusLabel(s)` | 脚本 | `orderOverview.enum.domesticCustoms.{s}` |
| `japanCustomsStatusLabel(s)` | 脚本 | `orderOverview.enum.japanCustoms.{s}` |
| `taxRefundStatusLabel(s)` | 脚本 | `orderOverview.enum.taxRefund.{s}` |
| `billingTypeLabel(s)` | 脚本 | `orderOverview.enum.billingType.{s}` |
| `salesStatusLabel(s)` | 脚本 | `orderOverview.enum.sales.{s}` |
| `salesChannelLabel(s)` | 脚本 | `orderOverview.enum.salesChannel.{s}` |
| `subProductSummary(demand)` | 脚本 | 拼接子货号 + 数量 + 目的地 |

---

## 7. 组件结构

| 组件 | 文件 | 职责 |
|------|------|------|
| `ProcurementOverviewPage` | `procurement/ProcurementOverviewPage.vue` | 容器，8步骤卡片 |
| `StatusProgressBar` | `procurement/components/StatusProgressBar.vue` | 8步状态进度条 |
| `StepCard` | `procurement/components/StepCard.vue` | 单步骤卡片（标题/状态/内容插槽） |

---

## 8. API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/orders/{procurementId}/overview` | 获取发注单视角全链路总览 |

返回 `OrderOverviewVO`：

| 字段 | 类型 | 说明 |
|------|------|------|
| `stepStatuses` | `string[8]` | 8步状态数组 |
| `demand` | `DemandVO` | 关联需求单（只读） |
| `procurement` | `ProcurementVO` | 发注单基本信息 |
| `qcRecord` | `QcRecordVO` | 验货记录（可能为空） |
| `logisticsPlan` | `LogisticsPlanVO` | 调配计划（可能为空） |
| `domesticCustoms` | `DomesticCustomsVO` | 国内报关（可能为空） |
| `japanCustoms` | `JapanCustomsVO` | 日本清关（可能为空） |
| `taxRefund` | `TaxRefundVO` | 退税记录（可能为空） |
| `salesRecord` | `SalesRecordVO` | 运营销售（可能为空） |

---

## 9. 与 DemandOverviewPage 的区别

| 维度 | DemandOverviewPage | ProcurementOverviewPage |
|------|--------------------|------------------------|
| 锚点 | `Demand.id` | `Procurement.id` |
| API | `getDemandOverview` | `getOverview` |
| 步骤1 | 核心数据（可转采购） | 关联需求单（只读） |
| 步骤2-8 | 全 NOT_STARTED | 真实数据（可能有值） |
| 主要用途 | 需求单视角追溯 | 发注单视角全链路查看 |
