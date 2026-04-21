# 验货记录 — 规格文档

> **版本**: 1.0.0
> **创建**: 2026-04-21
> **状态**: 设计中
> **依据**: `SPEC-发注管理流程.md` §3.3 + `06-发注管理UI设计-步骤1-4.md` §3.4
> **前置依赖**: Procurement ✅ 已实现

---

## 1. 业务背景

验货记录（QcRecord）是业务流第三步。货物到仓后，由验货负责人对商品进行数量清点、外观检查、缺陷记录，生成验货报告，作为调配和报关的前置依据。

**核心规则：**
- 验货记录关联一个采购单（Procurement）
- 验货完成后，Procurement 状态推进至「倉庫着」→「検品」
- 验货结果 PASS → 进入调配；FAIL → 触发退货流程
- 不良率 > 3% 需审批才可继续

---

## 2. 实体定义

### 2.1 QcRecord 聚合根

| 字段 | 来源 | 必填 | 说明 |
|------|------|------|------|
| `id` | 系统 | — | 主键 |
| `qcCode` | 系统生成 | — | 格式：`Q-YYYYMMDD-NNN`（如 Q-20260421-001），同需求单编号规则 |
| `procurementId` | 用户选择 | ✅ | 关联采购单（FK → procurement.id） |
| `sellerName` | 用户输入 | | 卖家/供应商名称（来自工厂名） |
| `productCode` | 自动带入 | ✅ | 主货号（来自 Procurement） |
| `subProductCode` | 自动带入 | | 子货号/颜色（来自 Procurement） |
| `qcUserId` | 用户选择 | ✅ | 验货负责人（开单人） |
| `qcType` | 用户选择 | | 验货方式：`ONSITE`（现场）/ `REMOTE`（仓库异地） |
| `qcDate` | 用户输入 | ✅ | 验货日期 |
| `result` | 用户选择 | ✅ | 验货结果：`PASS`（合格）/ `FAIL`（不合格） |
| `status` | 系统 | | 默认 `PENDING` → `COMPLETED`（终态） |
| `inspectionCount` | 用户输入 | ✅ | 检品数（实际检验数量） |
| `passedCount` | 用户输入 | ✅ | 合格数量 |
| `defectiveCount` | 用户输入 | | 不良数量（手动输入或自动 = inspectionCount - passedCount） |
| `boxCount` | 用户输入 | | 箱数 |
| `boxLengthCm` | 用户输入 | | 箱子长度(cm) |
| `boxWidthCm` | 用户输入 | | 箱子宽度(cm) |
| `boxHeightCm` | 用户输入 | | 箱子高度(cm) |
| `netWeightPerUnit` | 用户输入 | | 单个净重(kg) |
| `grossWeight` | 用户输入 | | 毛重(kg) |
| `taxInclusivePrice` | 用户输入 | | 含税价（元） |
| `material` | 自动带入 | | 材质（来自 Procurement） |
| `taxRefund` | 用户选择 | | 是否退税 |
| `qcStandard` | 用户输入 | | 验收标准 |
| `remarks` | 用户输入 | | 备注 |
| `images` | 用户上传 | | 缺陷照片URL列表（JSON数组） |
| `destination` | 自动带入 | | 目的地（来自 Procurement） |
| `quantity` | 自动带入 | | 订购数量（来自 Procurement） |
| `orderDate` | 自动带入 | | 下单日（来自 Procurement） |

> 🔴 规划中：`defectRate`（不良率 = defectiveCount / inspectionCount × 100%）字段待后续实现

---

## 3. 状态机

```
PENDING（待验货）
    ↓ 提交验货结果
COMPLETED（已完成）[终态]
    ↓ 若 result=FAIL
RETURN_REQUESTED（发起退货）
```

| 当前状态 | 触发动作 | 下一状态 |
|----------|----------|----------|
| PENDING | 提交结果 | COMPLETED |
| PENDING | 发起退货 | RETURN_REQUESTED |

> **注意**：QcRecord 本身不推进 Procurement 状态，由操作员在发注单页面手动推进。

---

## 4. 编号生成规则

与 ReplenishmentDemand 相同，`Q-YYYYMMDD-NNN`，SEQ 在进程内原子递增。

---

## 5. API 设计（后端）

### 5.1 列表查询
```
GET /api/v1/qc-records?page=0&pageSize=20&qcCode=&productCode=&result=&qcDateFrom=&qcDateTo=
```

### 5.2 详情查询
```
GET /api/v1/qc-records/{id}
```

### 5.3 创建验货记录
```
POST /api/v1/qc-records
Body: QcRecordCreateCmd
```

### 5.4 更新验货记录（仅 PENDING 状态可修改）
```
PATCH /api/v1/qc-records/{id}
Body: QcRecordUpdateCmd
```

### 5.5 删除验货记录（仅 PENDING 状态可删除）
```
DELETE /api/v1/qc-records/{id}
```

---

## 6. 前端页面（InspectionPage.vue）

### 6.1 列表页

- **路由**: `/procurement/inspection`
- **统计卡**: 全部记录 / 合格(PASS) / 不合格(FAIL)
- **筛选**: 验货编号、货号、验货日期范围、结果
- **表格列**: 验货编号、货号、子货号、检品数、合格数、不良数、箱数、验货日期、结果、操作
- **操作**: 详情（抽屉）

### 6.2 新规验货弹窗

关联采购单选择 → 自动带入字段 → 用户补充数量/箱货/结果信息。

**字段联动**：
- 选择采购单 → 自动填充 productCode、subProductCode、quantity、material、destination、sellerName（来自关联工厂名）、orderDate
- 检品数/合格数填写后 → 自动计算不良数、不良率
- 毛重 = boxCount × netWeightPerUnit（用户可手动覆盖）

### 6.3 详情抽屉

展示完整验货记录，关联采购单可点击跳转。

---

## 7. 代码实现清单

> 🔴 待实现  ✅ 已实现

- [ ] 🔴 `qc_record` 数据库表（实体已定义，JPA 自动建表）
- [x] ✅ `QcRecord` 实体（聚合根，28字段）
- [x] ✅ `QcResult` 枚举（PASS / FAIL）
- [x] ✅ `QcStatus` 枚举（PENDING / COMPLETED / RETURN_REQUESTED）
- [x] ✅ `QcRecordRepository` 接口
- [x] ✅ `QcRecordAssembler` 转换器
- [x] ✅ `QcRecordCreateCmd` / `QcRecordUpdateCmd` / `QcRecordPageQuery` DTO
- [x] ✅ `QcRecordUseCase` 业务用例
- [x] ✅ `QcRecordController` REST 接口
- [x] ✅ `@/api/inspection.ts` 前端 API 客户端
- [x] ✅ `InspectionPage.vue` 列表页（接真实 API，含 images 字段）
- [x] ✅ 新规验货弹窗（关联采购单 + 自动带入）
- [x] ✅ 验货详情抽屉（含 images 展示）
- [x] ✅ SPEC 进度更新至 `SPEC-发注管理流程.md`
