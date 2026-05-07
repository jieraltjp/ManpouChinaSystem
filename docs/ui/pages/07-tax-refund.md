# 页面规格 — 步骤7：退税

> **版本**: 1.1.0
> **创建**: 2026-04-22
> **实现**: 2026-04-22
> **状态**: ✅ 已实现
> **路由**: `/finance/tax-refund-record`
> **组件**: `TaxRefundRecordPage.vue`（`apps/web/src/pages/finance/TaxRefundRecordPage.vue`）
> **后端**: `TaxRefundController` at `/api/v1/tax-refunds`
> **前置步骤**: 步骤6（日本清关已放行 JapanCustomsRecord.status = CLEARED）
> **后续步骤**: 步骤8（运营销售 SalesRecord）

---

## 1. 页面定位

退税管理页面。对应业务流第七步。货物抵达日本后，根据采购单的报关类型（BillingType）判断是否需要退税，以及退税金额。

---

## 2. 布局结构

```
┌────────────────────────────────────────────────────────────────────┐
│ 页面标题：退税管理                                   [+ 新规退税记录] │
├────────────────────────────────────────────────────────────────────┤
│ 统计卡                                                               │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│ │ 申请中     │ │ 已退税     │ │ 不退税     │ │   合计     │       │
│ │ APPLYING   │ │ COMPLETED  │ │ NO_REFUND  │ │            │       │
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘       │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏                                                               │
│ 采购单号 [________]  报关类型 [全部▼]  状态 [全部▼]  日期范围     │
│                                                [搜索]  [重置]       │
├────────────────────────────────────────────────────────────────────┤
│ 表格                                                            │
│ ┌────────┬──────────┬────────┬────────┬────────┬────────┐         │
│ │记录编号│采购单号  │报关类型│理论退税│实际退税│退税日期│状态    │ ...
│ └────────┴──────────┴────────┴────────┴────────┴────────┘         │
└────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表格列定义

| 列名 | 字段 | 来源 | 说明 |
|------|------|------|------|
| 记录编号 | `refundCode` | 系统生成 | 格式 `TR-YYYYMMDD-NNN` |
| 采购单号 | `procurementId` | 关联 | — |
| 货号 | `productCode` | 来自 Procurement | — |
| 报关类型 | `billingType` | 来自 Procurement | 浙鲁开票/超慧退税/不退税 |
| 采购价(CNY) | `priceRmb` | 来自 Procurement | — |
| 采购数量 | `quantity` | 来自 Procurement | — |
| 理论退税额(RMB) | `estimatedRefundRmb` | 自动计算 | `priceRmb × quantity × (taxPoint - 1)` |
| 实际退税额(RMB) | `actualRefundRmb` | 用户输入 | — |
| 退税日期 | `refundDate` | 用户输入 | — |
| 退税银行 | `refundBank` | 用户输入 | — |
| 状态 | `status` | 系统 | APPLYING / COMPLETED / NO_REFUND |
| 操作 | — | — | 详情 / 编辑 |

> **前端表格布局**：列宽用 `min-width`，不写 `table-layout="fixed"`，操作列不写 `fixed="right"`。详见 [docs/ui/ARCHITECTURE.md §8](../ARCHITECTURE.md#8-element-plus-表格布局规范)。

---

## 4. 新规退税记录弹窗（骨架）

### 4.1 触发

点击 `[+ 新规退税记录]` → 弹出表单弹窗。

### 4.2 表单字段

**关联信息（自动代入）**：

| 字段 | 来源 | 说明 |
|------|------|------|
| 采购单 | 用户选择 / 自动查询 | 只显示 JapanCustomsRecord.status = CLEARED 且 billingType ≠ NO_REFUND 的采购单 |
| 货号 | 自动代入 | — |
| 报关类型 | 自动代入 | — |
| 理论退税额 | 自动计算 | `priceRmb × quantity × (taxPoint - 1)` |

**用户填入（待确认）**：

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 实际退税额(RMB) | `el-input-number` | | — |
| 退税日期 | `el-date-picker` | | — |
| 退税银行账户 | `el-input` | | — |
| 备注 | `el-input`（textarea） | | — |

---

## 5. 状态枚举（骨架）

```
  APPLYING ──[退税完成]──▶ COMPLETED
       │
       └──[不退税]──▶ NO_REFUND
```

| 状态 | 颜色 | 说明 |
|------|------|------|
| APPLYING | 黄色 | 退税申请中 |
| COMPLETED | 绿色 | 已退税 |
| NO_REFUND | 灰色 | 不退税（如 billingType = NO_REFUND） |

---

## 6. 自动触发规则

建议：**JapanCustomsRecord.status = CLEARED 时，若 Procurement.billingType ∈ {ZHE_LU_KAI_PIAO, CHAO_HUI_TUI_SHUI}，自动创建 TaxRefundRecord（status = APPLYING）**。

---

## 7. 理论退税计算公式

```
理论退税额(RMB) = priceRmb × quantity × (taxPoint - 1)
```

> 例：priceRmb=100, quantity=50, taxPoint=1.1 → 理论退税 = 100 × 50 × 0.1 = 500 CNY

---

## 8. API 集成

| 操作 | Method | Endpoint | 状态 |
|------|--------|----------|------|
| 分页查询 | GET | `/api/v1/tax-refunds?page=&pageSize=&procurementId=&status=` | ✅ |
| 详情 | GET | `/api/v1/tax-refunds/{id}` | ✅ |
| 创建 | POST | `/api/v1/tax-refunds` | ✅ |
| 标记完成 | PATCH | `/api/v1/tax-refunds/{id}/complete` | ✅ |
| 标记不退税 | PATCH | `/api/v1/tax-refunds/{id}/no-refund` | ✅ |
| 删除 | DELETE | `/api/v1/tax-refunds/{id}` | ✅ |

---

## 9. 缺口阻塞

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 退税计算公式确认 | **P0** | 与财务确认理论退税额计算公式 |
| 触发时机确认 | **P0** | JapanClear 后自动创建 or 手动创建 |
| 退税银行管理 | P1 | 需银行账户管理 |
| 财务记账联动 | P2 | COMPLETED 后应生成 FinanceRecord |

---

## 10. 组件拆分建议

| 组件 | 职责 |
|------|------|
| `TaxRefundRecordPage.vue` | 容器：列表 + 筛选 + 统计卡 |
| `TaxRefundTable.vue` | 表格 + 分页 |
| `TaxRefundFormDialog.vue` | 新规/编辑表单 |
| `TaxRefundDetailDrawer.vue` | 详情抽屉 |
| `TaxRefundCalculator.vue` | 理论退税额实时计算展示 |
| `useTaxRefund.ts` | API 调用（composable） |
