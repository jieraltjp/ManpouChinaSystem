# 页面规格 — 出货批次

> **版本**: 1.0.0
> **创建**: 2026-05-07
> **状态**: ✅ 已实现（ShipmentBatchController + ShipmentBatchPage.vue）
> **对应业务步号**: 02（发注单 → 出货批次）
> **对应后端**: `POST/GET/PATCH/DELETE /api/v1/shipment-batches`
> **对应 API**: `shipmentBatchApi`
> **对应路由**: `/procurement/shipment-batch`

---

## 1. 页面定位

出货批次页面管理验货完成后的出货批次记录。用于追踪每个采购单的实际发货数量与发货时间。入口：从发注单列表「创建批次」按钮进入，或验货列表「创建批次」。

**前置**: 步骤2（发注单已下单）
**后续**: 步骤4（调配计划 LogisticsPlan）

---

## 2. 列表页

### 2.1 筛选器

| 字段 | 控件 | 说明 |
|------|------|------|
| 采购单ID | `el-input-number` | 精确搜索 |
| 状态 | `el-select`（全部 / 待验货 / 验货中 / 已验货 / 已取消） | 多选 |
| 重置 / 搜索 | `el-button` | — |

### 2.2 表格

| 列 | 说明 | 备注 |
|----|------|------|
| 批次编号 batchCode | min-width=160 | show-overflow-tooltip |
| 发货数量 shipmentQuantity | 右对齐，locale 格式 | — |
| 厂家发货日 factoryShipDate | 日期 | — |
| 实际发货日 actualShipDate | 日期 | — |
| 状态 status | tag：`待验货`=info / `验货中`=warning / `已验货`=success / `已取消`=danger | — |
| 备注 remarks | overflow tooltip | — |
| 创建人 createBy | — | — |
| 创建时间 createTime | locale 格式 | — |
| 操作 | 编辑 + 删除 | 删除仅 `待验货` 状态可用 |

### 2.3 状态机

```
待验货 → 验货中 / 已取消
验货中 → 验货中（回退）/ 已验货 / 已取消
已验货 → 验货中（撤销）
已取消 → 终态（不可变更）
```

---

## 3. 新增弹窗

### 3.1 表单字段

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 采购单ID procurementId | `el-input-number` | ✅ | — |
| 发货数量 shipmentQuantity | `el-input-number`（正整数） | ✅ | — |
| 厂家发货日 factoryShipDate | `el-date-picker` | 否 | — |
| 备注 remarks | `el-input textarea`（max 512） | 否 | — |

---

## 4. 编辑弹窗

在新增字段基础上额外增加：

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 实际发货日 actualShipDate | `el-date-picker` | 否 | — |
| 状态 status | `el-select` | ✅ | 含全部 4 个枚举值 |

---

## 5. API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/shipment-batches` | 分页列表 |
| GET | `/api/v1/shipment-batches/{id}` | 详情 |
| POST | `/api/v1/shipment-batches` | 新增 |
| PATCH | `/api/v1/shipment-batches/{id}` | 更新 |
| DELETE | `/api/v1/shipment-batches/{id}` | 删除 |

---

## 6. i18n key 前缀

`shipmentBatch.*`
