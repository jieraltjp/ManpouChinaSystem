# 页面规格 — 步骤6：日本清关

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **路由**: `/customs/japan`
> **组件**: `JapanCustomsPage.vue`（占位）
> **对应后端**: `JapanCustomsRecord` 聚合根（待实现）
> **依赖文档**: `SPEC-B00-全链路总览.md` · `SPEC-B06-日本清关-步骤6.md`
> **前置步骤**: 步骤5（国内报关已放行 DomesticCustomsRecord.status = CLEARED）
> **后续步骤**: 步骤7（退税 TaxRefundRecord）

---

## 1. 页面定位

日本进口清关管理。对应业务流第六步。货物到港后，办理日本进口清关手续。

> ⚠️ **占位页面** — 字段待业务方确认后补充完整设计。当前仅定义页面骨架。

---

## 2. 布局结构（骨架）

```
┌────────────────────────────────────────────────────────────────────┐
│ 页面标题：日本清关                                   [+ 新规清关] │
├────────────────────────────────────────────────────────────────────┤
│ 统计卡                                                               │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│ │ PENDING    │ │IN_PROGRESS │ │ CLEARED    │ │   合计     │       │
│ │ 待清关     │ │ 清关中     │ │ 已放行     │ │            │       │
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘       │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏                                                               │
│ 入境报关号 [________]  采购单号 [________]  状态 [全部▼]  日期范围   │
│                                                [搜索]  [重置]       │
├────────────────────────────────────────────────────────────────────┤
│ 表格（占位字段，待确认）                                             │
│ ┌────────┬──────────┬────────┬────────┬────────┬────────┬─────┐   │
│ │报关号  │采购单号  │目的港  │到达日期│清关行  │状态    │操作 │   │
│ └────────┴──────────┴────────┴────────┴────────┴────────┴─────┘   │
└────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表格列定义（待确认字段）

> ⚠️ 以下字段来源于 `SPEC-B06-日本清关-步骤6.md`，**待业务方提供真实清关文件样本后确认**。

| 列名 | 字段 | 来源 | 说明 |
|------|------|------|------|
| 入境报关号 | `customsEntryNo` | 系统/海关返回 | — |
| 采购单号 | `procurementId` | 关联 | — |
| 货号 | `productCode` | 来自 Procurement | — |
| 目的港 | `arrivalPort` | 来自 Container（Container 实体实现后补充） | — |
| 到达日期 | `arrivalDate` | 用户输入 | — |
| 清关行 | `customsBroker` | 用户输入 | — |
| 清关行电话 | `brokerPhone` | 用户输入 | — |
| 关税(JPY) | `importDutyPaid` | 用户输入 | — |
| 消费税(JPY) | `consumptionTaxPaid` | 用户输入 | — |
| 清关完成日 | `clearanceDate` | 用户输入 | — |
| 状态 | `status` | 系统 | PENDING / IN_PROGRESS / CLEARED / FAILED |
| 操作 | — | — | 详情 / 编辑 |

---

## 4. 新规清关弹窗（骨架）

### 4.1 触发

点击 `[+ 新规清关]` → 弹出表单弹窗。

### 4.2 表单字段（占位，待确认）

**关联信息（自动代入）**：

| 字段 | 来源 | 说明 |
|------|------|------|
| 关联采购单 | 用户选择 | — |
| 货号 | 自动代入 | — |
| 目的港 | 自动代入 | 来自 Container.arrivalPort（Container 实体实现后补充） |
| 申报重量 | 自动代入 | 来自 LogisticsPlan.cargoWeightKg |
| 申报体积 | 自动代入 | 来自 LogisticsPlan.cargoVolumeCbm |

**用户填入（待确认）**：

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 入境报关号 | `el-input` | | 系统生成 or 海关返回？ |
| 到达日期 | `el-date-picker` | ✅ | 货物到港日期 |
| 清关行 | `el-input` | ✅ | 清关代理公司 |
| 清关行联系人 | `el-input` | | — |
| 清关行电话 | `el-input` | | — |
| 进口关税(JPY) | `el-input-number` | | — |
| 消费税(JPY) | `el-input-number` | | — |
| 清关完成日 | `el-date-picker` | | — |
| 备注 | `el-input`（textarea） | | — |

---

## 5. 状态流转（骨架）

```
  PENDING ──[开始清关]──▶ IN_PROGRESS ──[完成]──▶ CLEARED
                                          └──[失败]──▶ FAILED
```

| 状态 | 颜色 | 说明 |
|------|------|------|
| PENDING | 黄色 | 待清关 |
| IN_PROGRESS | 蓝色 | 清关办理中 |
| CLEARED | 绿色 | 已放行 |
| FAILED | 红色 | 清关失败 |

---

## 6. 自动触发规则

建议：**DomesticCustomsRecord.status = CLEARED 时，自动创建 JapanCustomsRecord（status = PENDING）**。

---

## 7. API 集成（骨架）

| 操作 | Method | Endpoint | 状态 |
|------|--------|----------|------|
| 分页查询 | GET | `/api/v1/japan-customs?page=&pageSize=&procurementId=&status=` | 🔴待实现 |
| 详情 | GET | `/api/v1/japan-customs/{id}` | 🔴待实现 |
| 创建 | POST | `/api/v1/japan-customs` | 🔴待实现 |
| 更新 | PATCH | `/api/v1/japan-customs/{id}` | 🔴待实现 |
| 开始清关 | PATCH | `/api/v1/japan-customs/{id}/start` | 🔴待实现 |
| 完成清关 | PATCH | `/api/v1/japan-customs/{id}/clear` | 🔴待实现 |

---

## 8. 缺口阻塞

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 清关字段确认 | **P0** | 需提供真实清关文件样本 |
| 清关费用记账 | P1 | importDutyPaid / consumptionTaxPaid 应关联 FinanceRecord |
| 自动触发逻辑 | P1 | CLEARED → 自动创建 JapanCustomsRecord |
| 汇率计算 | P2 | 关税/消费税 JPY → CNY 换算记录 |

---

## 9. 组件拆分建议

| 组件 | 职责 |
|------|------|
| `JapanCustomsPage.vue` | 容器：列表 + 筛选 + 统计卡 |
| `JapanCustomsTable.vue` | 表格 + 分页 |
| `JapanCustomsFormDialog.vue` | 新规/编辑表单 |
| `JapanCustomsDetailDrawer.vue` | 详情抽屉 |
| `CustomsBrokerSelect.vue` | 清关行选择器（历史记录 + 新建） |
| `useJapanCustoms.ts` | API 调用（composable） |
