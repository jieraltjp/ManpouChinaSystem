# 页面规格 — 步骤3：验货记录

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **路由**: `/procurement/inspection`
> **组件**: `InspectionPage.vue`
> **对应后端**: `QcRecord` 聚合根
> **依赖文档**: `SPEC-全链路业务流索引.md` §第三步 · `SPEC-验货记录流程.md`
> **前置步骤**: 步骤2（发注单 Procurement 已下单）
> **后续步骤**: 步骤4（调配计划 LogisticsPlan）

---

## 1. 页面定位

验货记录管理页面。对应业务流第三步。货物到仓后进行验货，验货结果 PASS → 进入调配（步骤4），FAIL → 触发退货流程。

---

## 2. 布局结构

```
┌────────────────────────────────────────────────────────────────────┐
│ 页面标题：验货记录                                   [+ 新规验货]   │
├────────────────────────────────────────────────────────────────────┤
│ 统计卡                                                               │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│ │ PENDING    │ │ COMPLETED  │ │ RETURN_    │ │   合计    │       │
│ │ 待验货     │ │ 已完成     │ │ REQUESTED  │ │            │       │
│ │            │ │            │ │ 退货待处理 │ │            │       │
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘       │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏                                                               │
│ 关联采购单 [________]  货号 [________]  结果 [全部▼]  状态 [全部▼] │
│                                                [搜索]  [重置]       │
├────────────────────────────────────────────────────────────────────┤
│ 表格                                                               │
│ ┌──────┬────────┬──────────┬────────┬───────┬──────┬────────┐     │
│ │验货号 │采购单号│货号      │卖家名称│检品数 │合格数 │结果    │ ... │
│ │Q-xx  │P-xx    │odn012-re │xxx工厂 │100    │ 98    │PASS    │     │
│ └──────┴────────┴──────────┴────────┴───────┴──────┴────────┘     │
│                                                            [分页]  │
└────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表格列定义

| 列名 | 字段 | 说明 |
|------|------|------|
| 验货号 | `qcCode` | 格式 `Q-YYYYMMDD-NNN` |
| 采购单号 | `procurementId` | 关联采购单（显示 procurementId） |
| 货号 | `productCode` + `subProductCode` | 主货号 + 子货号 |
| 卖家名称 | `sellerName` | 来自 Factory.factoryName |
| 检品数 | `inspectionCount` | 检品总数 |
| 合格数 | `passedCount` | 合格数量 |
| 不良数 | `defectiveCount` | 自动 = inspectionCount - passedCount |
| 结果 | `result` | PASS（绿色）/ FAIL（红色） |
| 状态 | `status` | PENDING / COMPLETED / RETURN_REQUESTED |
| 是否退税 | `taxRefund` | 开关，显示 ✓/✗ |
| 验货日期 | `qcDate` | — |
| 操作 | — | 详情 / 编辑 / 删除 |

---

## 4. 新规验货弹窗

### 4.1 触发

点击 `[+ 新规验货]` → 弹出验货表单弹窗。

### 4.2 表单结构（两栏布局）

**左栏 — 采购单关联 + 基础信息**：

| 字段 | 控件 | 必填 | 来源 |
|------|------|------|------|
| 关联采购单 | `el-select` + 搜索框 | ✅ | 调用 `GET /api/v1/procurements?page=0&pageSize=20` |
| 卖家名称 | `el-input` | | 选采购单后代入 Factory.factoryName |
| 货号 | `el-input`（只读） | ✅ | 选采购单后代入 |
| 子货号 | `el-input`（只读） | | 选采购单后代入 |
| 开单人 | `el-select` | | 担当者列表 |
| 验货日期 | `el-date-picker` | | 默认今天 |
| 是否退税 | `el-switch` | | 根据采购单 billingType 推断 |

**右栏 — 验货结果**：

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 检品数 | `el-input-number` | ✅ | 验货总数 |
| 合格数量 | `el-input-number` | ✅ | 合格数 ≤ 检品数 |
| 箱数 | `el-input-number` | | — |
| 箱子长(cm) | `el-input-number` | | — |
| 箱子宽(cm) | `el-input-number` | | — |
| 箱子高(cm) | `el-input-number` | | — |
| 单个净重(kg) | `el-input-number` | | — |
| 毛重(kg) | `el-input-number` | | — |
| 含税价(元) | `el-input-number` | | — |
| 材质 | `el-input` | | 来自采购单 |
| 验收标准 | `el-input`（textarea） | | — |
| 备注 | `el-input`（textarea） | | — |

### 4.3 自动计算

- `defectiveCount = inspectionCount - passedCount`（前端实时显示；后端 `QcRecordAssembler.copyUpdate()` 也会触发 `calculateDefectiveCount()`）
- `result` 由验货员根据检品数和合格数判断，手动选择 `PASS` 或 `FAIL`
- `boxDimensions` 存入 `boxLengthCm × boxWidthCm × boxHeightCm`

### 4.4 业务规则

- 选择采购单后，以下字段自动代入（disabled）：
  - `sellerName` → Factory.factoryName
  - `productCode` / `subProductCode` → Procurement
  - `quantity` → Procurement
  - `destination` → Procurement
  - `material` → Procurement
  - `taxRefund` → 由 billingType 推断（超慧退税=true）
- 验货结果 PASS → 状态 = `COMPLETED` → 可创建调配计划
- 验货结果 FAIL → 状态 = `RETURN_REQUESTED` → 触发退货流程

### 4.5 提交

- POST `/api/v1/qc-records`
- 成功后刷新列表；弹窗关闭

---

## 5. 创建调配计划（PASS 后操作）

### 5.1 触发条件

验货结果 = `PASS`，操作列 `[创建调配]` 按钮。

### 5.2 行为

- 点击 → 跳转 `/procurement/logistics?fromQc={id}`
- 调配页面预填充：
  - `procurementId`（来自 QcRecord）
  - `productCode` / `subProductCode`（来自 QcRecord）
  - `cargoLengthCm / WidthCm / HeightCm`（来自 QcRecord.boxDimensions）
  - `cargoWeightKg`（来自 QcRecord.grossWeight）

---

## 6. 状态流转

```
  PENDING ──[验货完成·PASS]──▶ COMPLETED
     │
     └──[验货完成·FAIL]──▶ RETURN_REQUESTED
```

| 状态 | 颜色 | 可用操作 |
|------|------|---------|
| PENDING | `#FEF3C7` 黄色 | 编辑 / 删除 / 验货完成 |
| COMPLETED | `#DCFCE7` 绿色 | 创建调配 / 详情 |
| RETURN_REQUESTED | `#FEE2E2` 红色 | 详情 / 退货处理 |

---

## 7. API 集成

| 操作 | Method | Endpoint |
|------|--------|----------|
| 分页查询 | GET | `/api/v1/qc-records?page=&pageSize=&procurementId=&productCode=&result=&status=` |
| 详情 | GET | `/api/v1/qc-records/{id}` |
| 创建 | POST | `/api/v1/qc-records` |
| 更新 | PATCH | `/api/v1/qc-records/{id}` |
| 删除 | DELETE | `/api/v1/qc-records/{id}` |
| 验货完成 | PATCH | `/api/v1/qc-records/{id}`（body: `{status: "COMPLETED"}`）|
| 关联采购单搜索 | GET | `/api/v1/procurements?page=&pageSize=&productCode=` |

---

## 8. 缺口与 TODO

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 采购单搜索选择器 | P1 | InspectionPage.vue 已有（fromDemand 逻辑可复用） |
| sellerName 自动代入 | P1 | 选采购单后查询 Factory.factoryName |
| 验货完成按钮 | P1 | 需实现 PATCH `/api/v1/qc-records/{id}/complete` |
| 序列号输入 | P2 | `serialNumbers` 为 JSON 数组，界面待设计 |
| 缺陷照片上传 | P2 | MinIO 文件上传，显示缩略图 |
| 创建调配预填充 | P1 | LogisticsPage.vue 解析 `fromQc` query 参数 |

---

## 9. 组件拆分建议

| 组件 | 职责 |
|------|------|
| `InspectionPage.vue` | 容器：列表 + 筛选 + 统计卡 |
| `InspectionFilters.vue` | 筛选栏 |
| `InspectionTable.vue` | 表格 + 分页 |
| `InspectionFormDialog.vue` | 新规/编辑验货表单 |
| `InspectionDetailDrawer.vue` | 详情抽屉 |
| `QcResultDisplay.vue` | 验货结果实时计算展示 |
| `useInspection.ts` | API + 状态逻辑（composable） |
