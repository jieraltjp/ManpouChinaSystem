# 页面规格 — 步骤6：日本清关

> **版本**: 1.2.0
> **创建**: 2026-04-22
> **更新**: 2026-04-27（v1.2.0：移除骨架状态标注，补全统计卡/筛选栏/表格列/表单完整字段）
> **状态**: ✅ 已完整实现
> **路由**: `/procurement/japan-customs`
> **组件**: `JapanCustomsPage.vue`（`apps/web/src/pages/customs/JapanCustomsPage.vue`）
> **后端**: `JapanCustomsController` at `/api/v1/japan-customs`
> **前置步骤**: 步骤5（国内报关已放行 DomesticCustomsRecord.status = CLEARED）
> **后续步骤**: 步骤7（退税 TaxRefundRecord — ✅ 已实现）

---

## 1. 页面定位

日本进口清关管理。对应业务流第六步。货物到港后，办理日本进口清关手续。

---

## 2. 布局结构

```
┌────────────────────────────────────────────────────────────────────┐
│ 页面标题：日本清关                                   [+ 新规清关] │
├────────────────────────────────────────────────────────────────────┤
│ 统计卡                                                               │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│ │ PENDING    │ │IN_PROGRESS │ │ CLEARED    │ │  FAILED   │       │
│ │ 待清关     │ │ 清关中     │ │ 已放行     │ │ 清关失败   │       │
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘       │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏                                                               │
│ 入境报关号 [________]  采购单号 [________]  状态 [全部▼]  日期范围   │
│                                                [搜索]  [重置]       │
├────────────────────────────────────────────────────────────────────┤
│ 表格                                                                 │
│ ┌──────────┬──────────┬────────┬────────┬──────────┬────────┬───┐ │
│ │ 验货编号 │ 采购单号 │ 货号  │ 目的港 │ 清关行   │ 状态   │操作│ │
│ └──────────┴──────────┴────────┴────────┴──────────┴────────┴───┘ │
└────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表格列定义

| 列名 | 字段 | 说明 |
|------|------|------|
| 验货编号 | `qcCode` | 关联验货单编号 |
| 采购单号 | `procurementId` | 关联发注单 ID |
| 货号 | `productCode` | 来自关联发注单 |
| 目的港 | `arrivalPort` | 到达港口 |
| 清关行 | `customsBroker` | 清关代理公司 |
| 状态 | `status` | PENDING / IN_PROGRESS / CLEARED / FAILED |
| 操作 | — | 详情 / 编辑 / 删除 |

> **前端表格布局**：列宽用 `min-width`，不写 `table-layout="fixed"`，操作列不写 `fixed="right"`。详见 [docs/ui/ARCHITECTURE.md §8](../ARCHITECTURE.md#8-element-plus-表格布局规范)。

---

## 4. 新规清关弹窗

### 4.1 触发

点击 `[+ 新规清关]` → 弹出表单弹窗（新建模式）。

### 4.2 表单字段

**关联信息（自动代入）**：

| 字段 | 来源 | 说明 |
|------|------|------|
| 关联采购单 | 用户选择 | 从已通过的国内报关记录中选择 |
| 货号 | 自动代入 | 来自选中的采购单 |
| 目的港 | 自动代入 | 来自 LogisticsPlan.arrivalPort |
| 申报重量 | 自动代入 | 来自 LogisticsPlan.cargoWeightKg |
| 申报体积 | 自动代入 | 来自 LogisticsPlan.cargoVolumeCbm |

**用户填入**：

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 入境报关号 | `el-input` | ✅ | 海关返回的入境报关号 |
| 到达日期 | `el-date-picker` | ✅ | 货物到港日期 |
| 清关行 | `el-input` | ✅ | 清关代理公司 |
| 清关行联系人 | `el-input` | | 联系人姓名 |
| 清关行电话 | `el-input` | | 联系电话 |
| 进口关税(JPY) | `el-input-number` | | 关税金额 |
| 消费税(JPY) | `el-input-number` | | 消费税金额 |
| 清关完成日 | `el-date-picker` | | 清关完成日期 |
| 备注 | `el-input`（textarea） | | 备注信息 |

---

## 5. 状态流转

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

## 7. API 集成

| 操作 | Method | Endpoint | 状态 |
|------|--------|----------|------|
| 分页查询 | GET | `/api/v1/japan-customs?page=&pageSize=&procurementId=&status=` | ✅ |
| 详情 | GET | `/api/v1/japan-customs/{id}` | ✅ |
| 创建 | POST | `/api/v1/japan-customs` | ✅ |
| 更新 | PUT | `/api/v1/japan-customs/{id}` | ✅ |
| 开始清关 | PATCH | `/api/v1/japan-customs/{id}/start` | ✅ |
| 完成清关 | PATCH | `/api/v1/japan-customs/{id}/complete` | ✅ |
| 标记失败 | PATCH | `/api/v1/japan-customs/{id}/fail` | ✅ |
| 删除 | DELETE | `/api/v1/japan-customs/{id}` | ✅ |

---

## 8. 缺口阻塞

| 项目 | 优先级 | 说明 |
|------|--------|------|
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
