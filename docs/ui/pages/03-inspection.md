# 页面规格 — 步骤3：验货记录

> **版本**: 1.1.2
> **创建**: 2026-04-22
> **更新**: 2026-05-07（v1.1.2：修正跳转目标 /procurement/logistics → /procurement/logistics-plan）
> **路由**: `/procurement/qc-record`
> **组件**: `QcRecordPage.vue`
> **对应后端**: `QcRecord` 聚合根
> **依赖文档**: `SPEC-B00-全链路总览.md` · `SPEC-B03-验货记录-步骤3.md`
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
│ ┌────────────┐ ┌────────────┐ ┌────────────┐                       │
│ │   合计     │ │   PASS     │ │   FAIL     │                       │
│ │     N      │ │     N      │ │     N      │                       │
│ └────────────┘ └────────────┘ └────────────┘                       │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏                                                               │
│ 验货号 [________]  采购单号 [____]  结果 [全部▼]  状态 [全部▼]    │
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
| 采购单号 | `procurementId` | 关联采购单（显示数字 ID） |
| 货号 | `productCode` | 主货号 |
| 子货号 | `subProductCode` | 子货号 |
| 卖家名称 | `sellerName` | 来自 Factory.factoryName |
| 检品数 | `inspectionCount` | 检品总数 |
| 合格数 | `passedCount` | 合格数量 |
| 不良数 | `defectiveCount` | 自动计算（后端），表格/抽屉只读 |
| 结果 | `result` | PASS（绿色）/ FAIL（红色） |
| 状态 | `status` | PENDING / COMPLETED / RETURN_REQUESTED |
| 验货日期 | `qcDate` | — |
| 操作 | — | 详情 / **总览** / 编辑 / 删除 |

> **前端表格布局**：列宽用 `min-width`，不写 `table-layout="fixed"`，操作列不写 `fixed="right"`。详见 [docs/ui/ARCHITECTURE.md §8](../ARCHITECTURE.md#8-element-plus-表格布局规范)。

---

## 4. 新规验货弹窗

### 4.1 触发

点击 `[+ 新规验货]` → 弹出验货表单弹窗。

### 4.2 表单结构（紧凑行布局，820px 弹窗）

弹窗宽度 820px，label-width 86px，行内字段用 `el-row gutter=10` 紧凑排列。

| 行 | 字段组合 |
|----|---------|
| Row1 | 关联采购单（span 14）+ 卖家名称（span 10，disabled） |
| Row2 | 货号（span 12）+ 子货号（span 12，disabled） |
| Row3 | 验货类型（span 6）+ 验货日期（span 6）+ 数量（span 6）+ 状态（span 6） |
| Row4 | 检品数（span 6）+ 合格数（span 6）+ 验货结果（span 6）+ 箱数（span 6） |
| Row5 | 材质（span 12）+ 目的地（span 12） |
| Row6 | 箱子长（span 8）+ 箱子宽（span 8）+ 箱子高（span 8） |
| Row7 | 单件净重（span 8）+ 毛重（span 8）+ 含税价（span 8） |
| Row8 | 验收标准（textarea rows=1） |
| Row9 | 备注（textarea rows=1） |
| Row10 | 缺陷照片（textarea rows=1） |

> **注意**：`defectiveCount` 不在前端输入，后端自动计算 `inspectionCount - passedCount`。
> **注意**：`是否退税（taxRefund）` 已移除，退税由采购层 `billingType` 决定。

### 4.3 自动计算

- `defectiveCount = inspectionCount - passedCount`（前端实时显示；后端 `QcRecordAssembler.copyUpdate()` 也会触发 `calculateDefectiveCount()`）
- `result` 由验货员根据检品数和合格数判断，手动选择 `PASS` 或 `FAIL`
- 箱子尺寸分别存入 `boxLengthCm` / `boxWidthCm` / `boxHeightCm`

### 4.4 业务规则

- 选择采购单后，以下字段自动代入（disabled）：
  - `sellerName` → Factory.factoryName（disabled，不可编辑）
  - `productCode` / `subProductCode` → Procurement（disabled，不可编辑）
  - `quantity` → Procurement
  - `destination` → Procurement
  - `material` → Procurement
- `qcType` 默认 `ONSITE`，`qcDate` 默认当天日期，数字字段默认 0
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

- 点击 → 跳转 `/procurement/logistics-plan?fromQc={id}`
- 调配页面预填充：
  - `procurementId`（来自 QcRecord）
  - `productCode` / `subProductCode`（来自 QcRecord）
  - `cargoLengthCm` / `cargoWidthCm` / `cargoHeightCm`（来自 QcRecord.boxLengthCm / boxWidthCm / boxHeightCm）
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
| 分页查询 | GET | `/api/v1/qc-records?page=&pageSize=&qcCode=&procurementId=&result=&status=` |
| 详情 | GET | `/api/v1/qc-records/{id}` |
| 创建 | POST | `/api/v1/qc-records` |
| 更新（编辑） | PATCH | `/api/v1/qc-records/{id}` |
| 删除 | DELETE | `/api/v1/qc-records/{id}` |
| 验货完成 | PATCH | `/api/v1/qc-records/{id}`（body: `{status: "COMPLETED"}`）|
| 关联采购单搜索 | GET | `/api/v1/procurements?page=&pageSize=&productCode=` |

---

## 8. 缺口与 TODO

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 统计卡 | P1 | 当前为 PASS/FAIL/合计 3个，应改为 PENDING/COMPLETED/RETURN_REQUESTED/合计 4个 |
| sellerName 自动代入 | ✅ | ✅ 已实现（disabled 代入） |
| 货号自动代入 | ✅ | ✅ 已实现（disabled 代入） |
| 编辑/删除功能 | ✅ | ✅ 已实现：PATCH / DELETE |
| 验货完成按钮 | ✅ | ✅ 已实现：`PATCH /api/v1/qc-records/{id}` + body `{status: "COMPLETED"}` |
| 统计卡增加状态筛选 | ✅ | ✅ 已实现 |
| 序列号输入 | P2 | 暂不规划；QcRecord 无此字段 |
| 缺陷照片上传 | P2 | MinIO 文件上传，显示缩略图 |
| 创建调配预填充 | P1 | LogisticsPlanPage.vue 解析 `fromQc` query 参数 |

---

## 9. 组件拆分建议

| 组件 | 职责 |
|------|------|
| `QcRecordPage.vue` | 容器：列表 + 筛选 + 统计卡 |
| `InspectionFilters.vue` | 筛选栏 |
| `InspectionTable.vue` | 表格 + 分页 |
| `InspectionFormDialog.vue` | 新规/编辑验货表单 |
| `InspectionDetailDrawer.vue` | 详情抽屉 |
| `QcResultDisplay.vue` | 验货结果实时计算展示 |
| `useInspection.ts` | API + 状态逻辑（composable） |
