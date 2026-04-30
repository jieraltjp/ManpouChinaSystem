# SPEC-B11-IMPLEMENT — QcRecord 关联出货批次（实施版）

> **版本**: 1.0.0
> **创建**: 2026-04-30
> **状态**: 🔲 实施中
> **前置条件**: SPEC-B11 v1.5.0 已审阅，V43 迁移已写入
> **关联**: SPEC-B11 · V43 迁移

---

## 1. 变更概述

### 当前状态（错误）

```
QcRecord ──procurementId FK──► Procurement
```

验货记录直接挂采购单，一个采购单只能验一次货，无法支持分批出货多次验货。

### 正确状态

```
Procurement ──procurementId──► ShipmentBatch ──shipmentBatchId FK──► QcRecord
```

三层链路：采购 → 出货批次 → 验货记录。

---

## 2. 数据库迁移（V43，已存在）

```sql
ALTER TABLE qc_record ADD COLUMN shipment_batch_id BIGINT NULL;
ALTER TABLE qc_record ADD INDEX idx_qc_shipment_batch (shipment_batch_id);
ALTER TABLE qc_record MODIFY COLUMN procurement_id BIGINT NULL;
```

**存量数据**：V43 迁移脚本内含回填逻辑，为每个 `procurement_id` 创建历史 `ShipmentBatch` 并关联。

---

## 3. 后端变更

### 3.1 QcRecord.java（Entity）

**文件**: `qc/domain/model/QcRecord.java`

**变更**：
- 新增字段 `shipmentBatchId`（`@Column(name = "shipment_batch_id")`）
- `procurementId` 改为 `@Column(nullable = true)`（JPA nullable 默认 true）
- 新增索引 `@Index(name = "idx_qc_shipment_batch", columnList = "shipment_batch_id")`

```java
@Column(name = "shipment_batch_id")
private Long shipmentBatchId;  // 关联出货批次（V43新增）

// 原 procurementId 改为 nullable（V43后不再强制）
@Column(name = "procurement_id")
private Long procurementId;
```

### 3.2 QcRecordCreateCmd.java（创建命令）

**文件**: `qc/application/dto/QcRecordCreateCmd.java`

**变更**：
- 新增 `shipmentBatchId: Long`（必填，新建时必须指定出货批次）
- `procurementId` 改为 nullable（保留用于审计追溯，不再强制）

```java
// V43: 新建验货必须关联出货批次
@NotNull(message = "关联出货批次不能为空")
private Long shipmentBatchId;

// procurementId 改为 nullable（审计追溯用）
private Long procurementId;
```

### 3.3 QcRecordPageQuery.java（分页查询）

**文件**: `qc/application/dto/QcRecordPageQuery.java`

**变更**：新增 `shipmentBatchId` 查询条件

```java
private Long shipmentBatchId;  // 按出货批次筛选
```

### 3.4 QcRecordAssembler.java（Assembler）

**文件**: `qc/application/assembler/QcRecordAssembler.java`

**变更**：`toEntity()` 中映射 `shipmentBatchId`；`toVO()` 中映射

```java
// toEntity()
entity.setShipmentBatchId(cmd.getShipmentBatchId());

// toVO()
vo.setShipmentBatchId(entity.getShipmentBatchId());
```

### 3.5 QcRecordUseCase.java（业务逻辑）

**文件**: `qc/application/usecase/QcRecordUseCase.java`

**变更**：`create()` 方法中校验 `shipmentBatchId` 必填

```java
// create() 开头校验
if (cmd.getShipmentBatchId() == null) {
    throw new BusinessException("qc.validation.shipmentBatchRequired", "关联出货批次不能为空");
}
```

### 3.6 QcRecordJpaRepository.java（Repository）

**文件**: `qc/infrastructure/persistence/jpa/QcRecordJpaRepository.java`

**变更**：新增按 `shipmentBatchId` 查询方法

```java
List<QcRecord> findByShipmentBatchId(Long shipmentBatchId);
long countByShipmentBatchIdAndDeletedFalse(Long shipmentBatchId);
```

### 3.7 QcRecordController.java（API）

**文件**: `qc/interfaces/controller/QcRecordController.java`

**变更**：`/qc/page` 查询参数支持 `shipmentBatchId`

---

## 4. 前端变更

### 4.1 inspection.ts（API 类型）

**文件**: `apps/web/src/api/inspection.ts`

**变更**：

```typescript
// QcRecordVO 新增
shipmentBatchId?: number;

// CreateQcRecordRequest 新增（必填）
shipmentBatchId: number;

// ListQcRecordRequest 新增（可选筛选）
shipmentBatchId?: number;
```

### 4.2 QcRecordPage.vue（页面）

**表单变更**：

| 变更 | 说明 |
|------|------|
| procurementId 下拉 → 移除 | 不再直接选采购单 |
| 新增 shipmentBatchId 下拉 | 必填，选出货批次 |
| procurementId 字段 | 保留（编辑时展示，审计追溯） |
| onProcurementSelected() | 改为加载对应采购单的出货批次列表 |
| watch(dialogVisible) | 改为加载 ShipmentBatch 列表 |

**下拉逻辑**：
1. 用户先选 ShipmentBatch（下拉列出所有批次，显示 `batchCode + productCode + status`）
2. 选完后自动从选中批次关联的 `procurementId` 回填商品信息

**列表变更**：
- `procurementId` 列 → 改为 `shipmentBatchId` 列（显示 batchCode）
- 或同时显示两列（shipmentBatchId + procurementId 作为审计）

**筛选变更**：
- `filterForm.procurementId` → 改为 `filterForm.shipmentBatchId`

### 4.3 i18n（zh.json / ja.json）

**新增 key**：

```json
"inspection": {
  "dialog": {
    "shipmentBatch": "出货批次",
    "shipmentBatchPlaceholder": "选择出货批次",
    "shipmentBatchRequired": "请选择出货批次"
  },
  "column": {
    "shipmentBatchId": "出货批次"
  },
  "filter": {
    "shipmentBatchId": "出货批次",
    "shipmentBatchIdPlaceholder": "选择出货批次"
  }
}
```

---

## 5. 字段对照表

| 层级 | 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| DB | `shipment_batch_id` | BIGINT NULL | V43后必填 | 新增 FK → shipment_batch.id |
| Entity | `shipmentBatchId` | Long | 创建时必填 | |
| CreateCmd | `shipmentBatchId` | Long | 必填 | 替换 procurementId 必填逻辑 |
| PageQuery | `shipmentBatchId` | Long | 可选 | 筛选条件 |
| VO | `shipmentBatchId` | Long | - | 展示用 |
| Frontend | `shipmentBatchId` | number | 表单必填 | |

---

## 6. 实施顺序

```
1. 后端 Entity（DROP COLUMN 后再加 shipmentBatchId）
     ↓
2. 后端 DTO（CreateCmd, PageQuery）
     ↓
3. 后端 Assembler + Repository
     ↓
4. 后端 UseCase 校验逻辑
     ↓
5. 后端 Controller API
     ↓
6. 前端 API 类型（inspection.ts）
     ↓
7. 前端页面（QcRecordPage.vue 下拉替换）
     ↓
8. i18n key 新增
     ↓
9. 本地验证（CREATE + LIST + 编辑）
```

---

## 7. 兼容性说明

- **存量数据**：V43 迁移已处理，为每个 procurement 关联创建历史 ShipmentBatch
- **API 兼容性**：`procurementId` 字段保留（nullable），现有 API 契约不变
- **前端兼容性**：编辑模式下保留 `procurementId` 展示（审计追溯），新建模式强制 `shipmentBatchId`
