# 页面规格 — 步骤6：日本清关

> **版本**: 1.4.0
> **创建**: 2026-04-22
> **更新**: 2026-04-27（v1.2.0：移除骨架状态标注，补全统计卡/筛选栏/表格列/表单完整字段）
> **更新**: 2026-04-30（v1.4.0：**containerNo 为主键字段 + domesticCustomsId 列 + productCode 列 + 新规清关弹窗**）
> **状态**: ✅ 已实现（v1.4.0 前端改造完成）
> **路由**: `/procurement/japan-customs`
> **组件**: `JapanCustomsRecordPage.vue`（`apps/web/src/pages/customs/JapanCustomsRecordPage.vue`）
> **后端**: `JapanCustomsController` at `/api/v1/japan-customs`
> **前置步骤**: 步骤5（国内报关已放行 DomesticCustomsRecord.status = CLEARED）
> **后续步骤**: 步骤7（退税 TaxRefundRecord — ✅ 已实现）

---

## 1. 页面定位

日本进口清关管理。对应业务流第六步。货物到港后，办理日本进口清关手续。

**v1.3.0 核心变更**：清关维度从采购单号改为货柜号（与国内报关步骤5一致）。

---

## 2. 布局结构

```
┌────────────────────────────────────────────────────────────────────┐
│ 页面标题：日本清关                                          [+ 新规清关] │
├────────────────────────────────────────────────────────────────────┤
│ 统计卡                                                               │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│ │   合计      │ │  PENDING  │ │IN_PROGRESS │ │  CLEARED   │       │
│ │            │ │  待清关   │ │  清关中    │ │  已放行    │       │
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘       │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏（v1.3.0）                                                   │
│ 货柜号 [________]  国内报关单号 [________]  状态 [全部▼]           │
│                                              [搜索]  [重置]         │
├────────────────────────────────────────────────────────────────────┤
│ 表格                                                                 │
│ ┌──────────┬──────────┬──────────┬────────┬──────────┬────────┬───┐│
│ │ 货柜号    │入境报关号 │国内报关  │目的港  │ 清关行   │ 状态    │操作││
│ │containerNo│customsEntry│domestic │arrival│customs  │status  │   ││
│ │           │           │CustomsId│ Port  │ Broker   │        │   ││
│ └──────────┴──────────┴──────────┴────────┴──────────┴────────┴───┘│
└────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表格列定义

**v1.3.0 变更**：`containerNo` 列优先展示，`domesticCustomsId` 新增

| 列名 | 字段 | 说明 |
|------|------|------|
| 货柜号 | `containerNo` | **v1.3.0 核心字段**（可点击跳转 DomesticCustomsPage） |
| 入境报关号 | `customsEntryNo` | 系统生成 JC-YYYYMMDD-NNN |
| 国内报关 | `domesticCustomsId` | 关联国内报关单（v1.3.0 新增） |
| 采购单号 | `procurementId` | 可选参考 |
| 目的港 | `arrivalPort` | 到达港口 |
| 清关行 | `customsBroker` | 清关代理公司 |
| 状态 | `status` | PENDING / IN_PROGRESS / CLEARED / FAILED |
| 操作 | — | 详情 / 编辑 / 删除 |

---

## 4. 新规清关弹窗

### 4.1 触发

点击 `[+ 新规清关]` → 弹出表单弹窗。

### 4.2 表单字段

**v1.3.0 字段优先级**：

| 字段 | 控件 | 说明 |
|------|------|------|
| **货柜号** | el-input | **必填，第一位**（v1.3.0 新增） |
| 国内报关单号 | el-input-number | domesticCustomsId，必填 |
| 采购单号 | el-input-number | 可选参考 |
| 货号 | el-input | productCode（v1.3.0 新增） |
| 子货号 | el-input | subProductCode |
| 到达日期 | el-date-picker | arrivalDate |
| 目的港 | el-input | arrivalPort |
| 清关行 | el-input | customsBroker |
| 申报重量 | el-input-number | declaredWeightKg |
| 申报体积 | el-input-number | declaredVolumeCbm |
| 备注 | el-textarea | remarks |

---

## 5. v1.4.0 改造清单

| 改造项 | 当前 | 目标 |
|--------|------|------|
| 列表页主筛选 | procurementId | containerNo ✅ |
| containerNo 列 | 无 | 有（可点击跳转 DomesticCustomsPage）✅ |
| domesticCustomsId 列 | 无 | 有（展示国内报关单状态）✅ |
| productCode 列 | 无 | 有 ✅ |
| 新建弹窗 | 无 | 有 ✅ |
| 新建弹窗 containerNo | 无 | **必填 el-input（第一位）**✅ |
| 新建弹窗 domesticCustomsId | 无 | **必填 el-input-number**✅ |
