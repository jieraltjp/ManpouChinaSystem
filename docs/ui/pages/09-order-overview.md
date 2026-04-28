# 页面规格 — 订单总览（核心视图）

> **版本**: 2.0.0
> **创建**: 2026-04-22
> **更新**: 2026-04-28（v2.0.0：移除双 Tab 架构，改为单一列表 + 详情抽屉；Demand 锚点，v_order_chain 视图驱动）
> **路由**: `/base/overview`
> **组件**: `OrderOverviewPage.vue`
> **对应后端**: `GET /api/v1/orders/chain` 列表 + `GET /api/v1/orders/chain/{demandId}` 详情
> **对应 DB**: `docs/database/DB-09-order-overview.md`
> **依赖文档**: `SPEC-B00-全链路总览.md` · `SPEC-B09-订单总览-API设计.md`
> **状态**: 🔄 Phase1 开发中

---

## 1. 页面定位

**订单总览是整个系统的核心视图**。以一条 ReplenishmentDemand（需求单）为锚点，纵向展示该订单从「需求录入」到「运营销售」的全链路8步状态。解决业务痛点：此前分"需求单"和"发注单"两个 Tab，用户困惑于同一订单为何要切换查看。

**设计原则（v2.0.0）**：
- **Demand 锚点**：每行 = 一个子货号的完整链路（步骤1→步骤8）
- **单一列表**：不再分 Tab，一屏展示所有需求单及其8步状态
- **状态可见**：step1-step8 状态一目了然，当前卡点一目了然
- **点击详情**：点击行进入详情抽屉，展示全链路8步完整数据

---

## 2. 入口与路由

### 2.1 入口

| 入口位置 | 触发行为 |
|---------|---------|
| 侧边栏菜单 | 进入 `/base/overview`（订单总览列表页） |
| 发注单列表操作列 `[总览]` | 进入 `/base/overview`（打开详情抽屉） |
| 验货列表操作列 `[总览]` | 进入 `/base/overview`（打开详情抽屉） |
| 调配列表操作列 `[总览]` | 进入 `/base/overview`（打开详情抽屉） |

### 2.2 路由

```
/base/overview                           → 订单总览列表页（单一表格）
```

> **v2.0.0 变更**：移除 `/procurement/overview/{procurementId}` 路由，改为列表页点击行 → 详情抽屉

---

## 3. 页面布局

### 3.1 列表页布局

```
┌──────────────────────────────────────────────────────────────────────────┐
│  订单总览                                              [刷新] [筛选▼]   │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ 全表格（单一表格，一行 = 一个 Demand）                               │ │
│  │ 列：需求单号 | 货号 | 子货号 | 数量 | 目的地 | 担当 | 状态 | 8步状态 │ │
│  │                                                                      │ │
│  │  DM-20260401-001 | ad009 | ad009-be | 100 | 久留米 | 田中 | CONFIRMED │ ●●●●○○○○ │ │
│  │  DM-20260401-002 | ad009 | ad009-bu | 50  | 名古屋 | 铃木 | PENDING   │ ●○○○○○○○ │ │
│  │  ...                                                                │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  分页控件                                                              │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.2 详情抽屉布局

```
┌────────────────────────────────────────────────────────────────────────┐
│  [← 返回]  订单总览详情  需求单号: DM-20260401-001           [全屏]   │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  状态进度条（8步横穿）                                                 │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                  │
│  │ 1 │→│ 2 │→│ 3 │→│ 4 │→│ 5 │→│ 6 │→│ 7 │→│ 8 │                  │
│  └───┘ └───┘ └───┘ └───┘ └───┘ └───┘ └───┘ └───┘                  │
│  补货  发注  验货  调配  国报  日报  退税  运营                       │
│   ●    ●    ●    ◐    ○    ○    ○    ○    ← 当前状态指示           │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ 步骤1：补货需求                                      [展开▼/收起▲] │  │
│  │  ─────────────────────────────────────────────────────────────    │  │
│  │  需求类型：新品采购  │  货号：odn012  │  数量：200  │  担当者：田中 │  │
│  │  目的地：久留米     │  状态：CONFIRMED              [编辑]        │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  ...（步骤2-8 同前版）                                                 │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 列表页表格列

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 需求单号 | `demandCode` | 140px | DM-YYYYMMDD-NNN |
| 主货号 | `demandProductCode` | 100px | 如 ad009 |
| 子货号 | `demandSubProductCode` | 120px | 如 ad009-be |
| 数量 | `demandQuantity` | 80px | 数字 |
| 目的地 | `demandDestination` | 100px | 如久留米/名古屋 |
| 日本担当 | `demandJapanLead` | 80px | 姓名 |
| 状态 | `demandStatus` | 100px | PENDING(待确认) / CONFIRMED(已关联) |
| 8步状态 | step1-step8 | 160px | ●完成/○未开始，横向排列 |

> **8步状态列**：展示 `step1Status` ~ `step8Status`，用 ● / ◐ / ○ 图标表示 COMPLETED / IN_PROGRESS / NOT_STARTED

---

## 5. 步骤卡片字段总览（详情抽屉）

### 步骤1 — 补货需求（ReplenishmentDemand）

| 字段 | 说明 |
|------|------|
| `demandCode` | 需求编号 |
| `demandType` | 补货 / 新品采购 |
| `productCode` + `subProductCode` | 货号 |
| `quantity` | 需求量 |
| `destination` | 目的地 |
| `japanLead` | 日本担当 |
| `status` | PENDING / CONFIRMED |

**操作**：`[编辑]`（PENDING 时）

---

### 步骤2 — 发注单（Procurement）

| 字段 | 说明 |
|------|------|
| `procurementCode` | 发注编号 |
| `factoryName` | 工厂名称 |
| `productCode` + `subProductCode` | 货号 |
| `quantity` | 订购数量 |
| `priceRmb` | 单价(CNY) |
| `taxPoint` | 票点 |
| `exchangeRate` | 汇率 |
| `billingType` | 报关类型 |
| `estimatedPriceJpy` | 估算批发价 |
| `orderDate` | 下单日 |
| `factoryShipDate` | 厂家出货日 |
| `plannedShipDate` | 交货期 |
| `actualShipDate` | 实际出货日 |
| `productLead` / `japanLead` / `chinaLead` | 担当 |
| `status` | 采购单状态 |

**操作**：`[编辑发注单]`

---

### 步骤3 — 验货记录（QcRecord）

| 字段 | 说明 |
|------|------|
| `qcCode` | 验货编号 |
| `result` | PASS / FAIL |
| `inspectionCount` | 检品数 |
| `passedCount` | 合格数 |
| `defectiveCount` | 不良数 |
| `boxCount` | 箱数 |
| `grossWeight` | 毛重 |
| `qcDate` | 验货日期 |
| `status` | PENDING / COMPLETED / RETURN_REQUESTED |

**操作**：`[详情]` `[,创建调配]`（PASS 且无 LogisticsPlan 时）

---

### 步骤4 — 调配计划（LogisticsPlan）

| 字段 | 说明 |
|------|------|
| `planCode` | 调配编号 |
| `planType` | 海运 / 空运 / 拼柜 |
| `cargoLengthCm` / `cargoWidthCm` / `cargoHeightCm` | 货物尺寸（cm） |
| `cargoWeightKg` | 重量 |
| `departurePort` | 出发港 |
| `arrivalPort` | 目的港 |
| `estimatedShipDate` | 预计发货日 |
| `actualShipDate` | 实际发货日 |
| `status` | PLANNED / BOOKED / IN_TRANSIT / DELIVERED |

**操作**：`[详情调配]`

---

### 步骤5 — 国内报关（DomesticCustomsRecord）

| 字段 | 说明 |
|------|------|
| `customsDeclarationNo` | 报关单号 |
| `hsCode` | HS编码 |
| `declarationDate` | 申报日期 |
| `exportPort` | 出口口岸 |
| `declaredValueRmb` | 申报价值 |
| `status` | PENDING / SUBMITTED / CLEARED / FAILED |

**操作**：`[详情报关]`

---

### 步骤6 — 日本清关（JapanCustomsRecord）

| 字段 | 说明 |
|------|------|
| `customsEntryNo` | 入境报关号 |
| `arrivalDate` | 到达日期 |
| `customsBroker` | 清关行 |
| `importDutyPaid` | 关税 |
| `consumptionTaxPaid` | 消费税 |
| `status` | PENDING / IN_PROGRESS / CLEARED / FAILED |

**操作**：`[详情清关]`

---

### 步骤7 — 退税（TaxRefundRecord）

| 字段 | 说明 |
|------|------|
| `refundCode` | 退税编号 |
| `billingType` | 报关类型 |
| `estimatedRefundRmb` | 理论退税额 |
| `actualRefundRmb` | 实际退税额 |
| `refundDate` | 退税日期 |
| `status` | APPLYING / COMPLETED / NO_REFUND |

---

### 步骤8 — 运营销售（SalesRecord）

| 字段 | 说明 |
|------|------|
| `salesChannel` | 销售渠道 |
| `listingDate` | 上架日期 |
| `initialStock` | 初始库存 |
| `currentStock` | 当前库存 |
| `salesQuantity` | 累计销量 |
| `returnedQuantity` | 退货数量 |
| `returnRate` | 退货率 |
| `sellingPriceJpy` | 实际销售价 |
| `status` | LISTED / LOW_STOCK / OUT_OF_STOCK / DISCONTINUED |

**操作**：`[编辑销售]` `[,生成补货需求]`（LOW_STOCK / OUT_OF_STOCK 时）

---

## 6. 状态进度条

横穿页面顶部的 8 步进度条，每步用图标+颜色表示状态：

| 步 | 名称 | 完成 | 进行中 | 未开始 |
|----|------|------|--------|--------|
| 1 | 补货 | ● 蓝色 | ◐ 蓝色 | ○ 灰色 |
| 2 | 发注 | ● 蓝色 | ◐ 橙色 | ○ 灰色 |
| 3 | 验货 | ● 绿色 | ◐ 蓝色 | ○ 灰色 |
| 4 | 调配 | ● 蓝色 | ◐ 橙色 | ○ 灰色 |
| 5 | 国报 | ● 蓝色 | ◐ 蓝色 | ○ 灰色 |
| 6 | 日报 | ● 蓝色 | ◐ 蓝色 | ○ 灰色 |
| 7 | 退税 | ● 绿色 | ◐ 蓝色 | ○ 灰色 |
| 8 | 运营 | ● 绿色 | ◐ 橙色 | ○ 灰色 |

- **完成**（●）：该步骤对应记录已创建且状态为终态或进行中
- **进行中**（◐）：有记录，状态非终态
- **未开始**（○）：无对应记录（LEFT JOIN 结果为 NULL）

---

## 7. 分阶段实现计划

### Phase 1：步骤 1~4（补货 → 发注 → 验货 → 调配）

| 项目 | 状态 | 说明 |
|------|------|------|
| 单一列表页表格 | 🔲 待改 | OrderOverviewPage.vue 移除双 Tab |
| 详情抽屉（步骤1~4） | 🔲 待改 | 详情抽屉展示前4步数据 |
| v_order_chain API 集成 | 🔲 待做 | `/api/v1/orders/chain` |
| 4步状态列 | 🔲 待做 | 横向 ●●○○○○○○ 展示 |
| 步骤5~8卡片 | 🔲 待做 | Phase2 再展示 |

### Phase 2：步骤 5~8（国内报关 → 日本清关 → 退税 → 运营销售）

| 项目 | 状态 |
|------|------|
| 详情抽屉步骤5~8 | 🔲 待改 |
| 8步状态列补全 | 🔲 待改 |
| 路由更新 | 🔲 待改 |
