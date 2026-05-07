# 页面规格 — 需求单总览

> **版本**: 1.1.0
> **创建**: 2026-04-27
> **更新**: 2026-05-07（v1.1.0：修正 API 路径 orders/demands）
> **路由**: `/base/overview/demand/:demandId`
> **组件**: `DemandOverviewPage.vue`
> **对应后端**: `GET /api/v1/orders/demands/{demandId}/overview`
> **API**: `orderOverviewApi.getDemandOverview(demandId)`

---

## 1. 页面定位

**需求单总览**是从需求单（Demand）视角查看全链路的入口。以 Demand.id 为锚点，向上追溯该需求从录入到最终销售的全链路状态。入口为订单总览（`/base/overview`）中需求单 Tab 的「总览」按钮。

---

## 2. 入口与路由

### 入口

| 入口位置 | 触发行为 |
|---------|---------|
| `/base/overview` 需求单 Tab 操作列 `[总览]` | 进入 `/base/overview/demand/{demandId}` |

### 路由

```
/base/overview/demand/:demandId  →  需求单总览详情页
```

---

## 3. 页面布局

```
┌──────────────────────────────────────────────────────────────────────────┐
│  [← 返回列表]  订单总览                                                 │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  状态进度条（8步，需求单视角，仅步骤1可能高亮）                          │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                     │
│  │ 1 │→│ 2 │→│ 3 │→│ 4 │→│ 5 │→│ 6 │→│ 7 │→│ 8 │                     │
│  └───┘ └───┘ └───┘ └───┘ └───┘ └───┘ └───┘ └───┘                     │
│  补货  发注  验货  调配  国报  日报  退税  运营                           │
│   ●    ○    ○    ○    ○    ○    ○    ○    ← 需求单未转采购时全灰       │
│                                                                          │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ 步骤1：补货需求                                      [PENDING]     │   │
│  │  ─────────────────────────────────────────────────────────────    │   │
│  │  需求编号：D-20260401-001  │  需求类型：新品采购                  │   │
│  │  货号：odn012               │  子货号明细：odn012-re × 200 台州    │   │
│  │  担当：田中                │  状态：[PENDING]                    │   │
│  │                                                                   │   │
│  │  [转采购]  ← 仅 PENDING 状态时可见                                │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ 步骤2：发注单信息                                      [未开始]     │   │
│  │  暂无记录                                                         │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│  ... (步骤3-8 同上，均显示"暂无记录")                                  │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 步骤卡片

### 步骤1 — 补货需求（有数据时）

| 字段 | 标签 | 说明 |
|------|------|------|
| `demand.demandCode` | 需求编号 | 唯一编号 |
| `demand.demandType` | 需求类型 | 补货 / 新品采购 |
| `demand.productCode` | 货号 | 主货号 |
| `subProductSummary` | 子货号明细 | `subProductCode` + `quantity` + `destination` |
| `demand.japanLead` | 担当 | 日本担当姓名 |
| `demand.status` | 状态 | PENDING/CONVERTED/CANCELLED + tag 颜色 |

**操作**：`[转采购]`（仅 `status === 'PENDING'` 时可见）

点击后弹出「选择工厂」弹窗，确认后调用 `demandApi.convertToProcurement`，成功后跳转到 `/base/overview/procurement/{procurementId}`。

---

### 步骤2-8 — 均显示「暂无记录」

无论需求单状态如何，步骤2-8 卡片均显示 `orderOverview.step.notStarted`（"暂无记录"），因为这些数据属于发注单视角。

---

## 5. 转采购弹窗

**触发**：`[转采购]` 按钮
**标题**：`demand.dialog.convertDialog.title`（"选择工厂并转采购"）
**宽度**：500px

| 字段 | 类型 | 说明 |
|------|------|------|
| 工厂选择 | el-select (filterable + remote) | 远程搜索工厂，回显 `factoryName (factoryCode)` |

**操作按钮**：取消 / 确认

确认后调用：
```
POST /api/v1/demands/{demandId}/convert
Body: { factoryId }
```

成功响应中取 `linkedProcurementId`，跳转到发注单总览。

---

## 6. 状态进度条

来自 `StatusProgressBar` 组件，`stepStatuses` 数组 8 个元素。

- 步骤1：`overview.stepStatuses[0]`（来自后端）
- 步骤2-8：`'NOT_STARTED'`（需求单阶段均未开始）

---

## 7. 组件结构

| 组件 | 文件 | 职责 |
|------|------|------|
| `DemandOverviewPage` | `procurement/DemandOverviewPage.vue` | 容器 |
| `StatusProgressBar` | `procurement/components/StatusProgressBar.vue` | 8步状态条 |
| `StepCard` | `procurement/components/StepCard.vue` | 单步骤卡片 |

---

## 8. API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/orders/demand/{demandId}/overview` | 获取需求单视角总览 |

返回 `DemandOverviewVO`：

| 字段 | 类型 | 说明 |
|------|------|------|
| `stepStatuses` | `string[8]` | 8步状态数组 |
| `demand` | `DemandVO` | 需求单数据（可能为空） |

---

## 9. i18n 标签（与 OrderOverviewPage 共用）

所有步骤标题、字段标签、提示信息均来自 `orderOverview.*` 命名空间，与 `OrderOverviewPage.vue` 完全共享。
