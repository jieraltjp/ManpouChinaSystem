# SPEC-B11 — 出货批次与数量链路（步骤2.5）

> **版本**: 1.5.0
> **创建**: 2026-04-29
> **更新**: 2026-04-29（v1.5.0：前端+监听器+Assembler 审计，补充第二条断裂事件链（LogisticsPlanInTransitEvent）、三个新跨模块违规、Assembler 含 IO、QcRecordAssembler 含业务逻辑、前端 toggle 语义冲突、i18n 缺口等 7 个新基因）
> **状态**: 🔲 分析完成，待实施
> **关联**: SPEC-B01 · SPEC-B02 · SPEC-B03 · SPEC-B04

---

## 1. 业务背景

在"下单（Procurement）"与"验货（QcRecord）"之间，存在缺失的中间环节：**出货（Shipment）**。

厂家发货不是一次性完成的：库存不足时分多次发出、可能有之前遗留库存、出货与验货相互独立。

---

## 2. 七条核心规则

| # | 规则 | 结论 |
|---|------|------|
| Q1 | 出货录入方式 | **手动登记**，用户在验货前主动创建 ShipmentBatch |
| Q2 | 超额出货 | **允许**，SUM(ShipmentBatch) 可 > Procurement.quantity（有之前遗留库存） |
| Q3 | 出货→验货顺序 | **强顺序**：出货必然导致验货，每批出货都应有关联的验货记录 |
| Q4 | 出货↔验货数量独立 | **不强制数量对应**：SUM(QcRecord.passedCount) 可 < SUM(ShipmentBatch.shipmentQuantity)，差额为不良品 |
| Q5 | 不良品处理 | **不影响批次状态**，由人工另行处理（不影响 ShipmentBatchStatus FSM） |
| Q6 | Demand↔Procurement 关联约束 | **相同子货号才能关联**，校验 subProductCode 一致性 |
| Q7 | Phase2「已下单/已出货」 | **保留**，由 Assembler 派生计算（batchCount > 0） |

**Q3/Q4 的精确含义**：

```
出货与验货是顺序关系（先出货后验货），而非无关系。
独立性指的是数量上的偏差（100件出货，80件合格，20件不良品）。
批次的状态流转（待验货→验货中→已验货）由验货结果驱动。
```

**一个 ShipmentBatch 可以有多条 QcRecord**（一次出货，多次验货，如分批到货、分批检验），
**每条 QcRecord 必属于某个 ShipmentBatch**（验货是出货的后置动作）。

---

## 3. 方案判定（建模三路分析）

| 维度 | 方案A：独立表 + FK | 方案B：纯聚合派生 | 方案C：Mapping N:N 联立 |
|---|---|---|---|
| 粒度 | 每行一个出货事件 | 无行，按需聚合 | 每行一个关联记录 |
| 订单关联 | `procurement_id` 直接挂 | 无关联列，靠 JOIN 算 | `procurement_id` 中转 |
| 审计追踪 | ✅ 每行可追溯 | ❌ 无原始记录 | ✅ 但过度设计 |
| 实现复杂度 | 低 | 最低 | 高 |
| 适用场景 | **✅ 正确** | ❌ 出货/验货必须独立存储 | ❌ N:N 在当前业务不存在 |

**判定：A 正确，B/C 均不适用。**

出货事件是独立业务事实——有自己的生命周期（待验货→已验货）、自己的属性（厂家发货日、批次编号），不能被「派生」掉。方案B在第二次出货时尚未验货时无法记录。方案C的 N:N 联立是为多对多场景设计的，当前出货→采购是明确的一对多。

---

## 4. 链路总图（核心：出货→验货强顺序）

```
ReplenishmentDemand D001
  │ productCode=ad009, subProductCode=ad009-be, quantity=500
  │
  ├────────── DemandProcurementMapping ───────────────────────────┤
  │                                                              │
  │  Procurement P-001  (subProductCode=ad009-be, quantity=300)   │
  │       │                                                        │
  │       ├── ShipmentBatch SB-001  出货量=200   2026-04-10     │
  │       │       │                                               │
  │       │       ├── QcRecord Q-001  passedCount=200  PASS     │
  │       │       │   │                                          │
  │       │       │   └──→ LogisticsPlan（合格品进入调配）       │
  │       │       │                                               │
  │       │       └── QcRecord Q-002  passedCount=50   FAIL     │
  │       │           │                                           │
  │       │           └──→ 不良品人工处理（不影响 SB-001 状态）  │
  │       │                                                       │
  │       ├── ShipmentBatch SB-002  出货量=150   2026-04-18     │
  │       │       │                                               │
  │       │       └── QcRecord Q-003  passedCount=145  PASS     │
  │       │                                                       │
  │  Procurement P-002  (subProductCode=ad009-be, quantity=200)  │
  │       │                                                        │
  │       └── ShipmentBatch SB-003  出货量=200   2026-04-25     │
  │               │                                               │
  │               └── QcRecord Q-004  passedCount=200  PASS     │
  │                                                              │
  └──────────────────────────────────────────────────────────────┘

关键约束：
  ① 出货 → 验货：强顺序，每批出货必然有至少一条验货记录
  ② 1:N 关系：一个 ShipmentBatch 可对应多条 QcRecord（分批到货/多次验货）
  ③ 数量独立：SUM(QcRecord.passedCount) ≤ SUM(ShipmentBatch.shipmentQuantity)
  ④ 可超额出货：SUM(ShipmentBatch) 可 > Procurement.quantity

汇总：D001 需求 500 ───────────────────────────────────────────▶ 实际到货 490 件 ✅
     P-001 出货 200+150=350，P-002 出货 200，共 550 出货，490 合格
```

---

## 5. 数量等式

```
Demand.quantity（需求总量）
  = SUM(DemandProcurementMapping.allocatedQuantity)     [分配总量]
  = SUM(Procurement.quantity)                          [采购总量]
  ≥ SUM(ShipmentBatch.shipmentQuantity)              [出货总量]  ← 可超额（Q2）
  ≥ SUM(QcRecord.inspectionCount)                      [验货总量]
  = SUM(QcRecord.passedCount)                        [合格总量]
  ≥ SUM(SalesRecord.initialStock)                       [上架库存]
```

**关键数量链（按采购单维度）**：

```
某张 Procurement.quantity = 300

  ShipmentBatch SB-001  出货量=200
    ├── QcRecord Q-001  inspection=200, passed=200  ✅
    └── QcRecord Q-002  inspection=60,  passed=50   ❌ 不良品10

  ShipmentBatch SB-002  出货量=150
    └── QcRecord Q-003  inspection=150, passed=145  ❌ 不良品5

本采购单验货汇总：passed = 200+50+145 = 395（超额，不良品15）
```

**核心不等式**：

| 不等式 | 含义 |
|--------|------|
| `SUM(QcRecord.passedCount) ≤ SUM(ShipmentBatch.shipmentQuantity)` | 合格量不会超过出货量（Q4） |
| `SUM(ShipmentBatch.shipmentQuantity) ≥ Procurement.quantity` | 出货量可超过采购量（Q2，遗留库存） |
| `SUM(QcRecord.passedCount) ≥ SUM(SalesRecord.initialStock)` | 上架库存不超过合格量 |

**不良品不在数量链中**：`QcRecord.defectiveCount` 仅记录，不参与数量链流转，由人工另行处理（Q5）。

---

## 6. 实体规格

### 6.1 ShipmentBatch（出货批次）

```
ShipmentBatch（聚合根）
├── id: Long
├── procurementId: Long              # FK → procurement.id（必填）
├── batchCode: String                # 批次编号（SB-YYYYMMDD-NNN）
├── shipmentQuantity: Integer        # 本次出货数量（厂家实际发出）
├── factoryShipDate: LocalDate       # 厂家发货日
├── actualShipDate: LocalDate        # 实际到仓日（由验货时填入，非必填）
├── status: ShipmentBatchStatus     # 状态枚举
└── remarks: String                 # 备注
```

> **设计说明**：ShipmentBatch 不持有 QcRecord 的列表引用——关联方向由 **QcRecord 持有 shipmentBatchId FK**，这是行业标准（SAP QM、金蝶、用友均如此）。FK 在数据库层强制约束，比 JSON 列更可靠，JPA `@ManyToOne` 映射也更自然。

**ShipmentBatchStatus FSM：**

```
待验货 ──[创建验货记录并关联]──▶ 验货中 ──[所有 QcRecord COMPLETED]──▶ 已验货
      │                              │
      │                              └──[新增 QcRecord]──────────▶ 验货中（状态回归）
      │
      └──[人工取消]──────▶ 已取消
```

**状态流转详解**：

| 当前状态 | 触发事件 | 目标状态 | 说明 |
|---------|---------|---------|------|
| `待验货` | 用户在验货创建时关联到本批次 | `验货中` | 第一条验货记录入场，状态推进 |
| `验货中` | 某条 QcRecord 变为 COMPLETED | `验货中` | 其他 QcRecord 可能还在进行 |
| `验货中` | **所有** QcRecord 都 COMPLETED | `已验货` | 全部验货完成，批次结案 |
| `验货中` | 用户取消 | `已取消` | 批次作废 |
| `已验货` | 新增 QcRecord（如迟到货物） | `验货中` | 状态回归，允许后续到货继续验货 |
| `已验货` | 人工重置 | `待验货` | 极少用，保留撤销能力 |

**约束**：
- 无数量上限约束（Q2 规则：允许超额出货）
- 不良品不影响 ShipmentBatch 状态（Q5 规则）
- 每条 QcRecord **必须**且**只能**属于一个 ShipmentBatch（FK 唯一约束）
- 验货是出货的后置动作（Q3 规则）

---

### 6.2 DemandProcurementMapping（需求-采购分配映射）

```
DemandProcurementMapping（关联实体）
├── id: Long
├── demandId: Long                  # FK → replenishment_demand.id（必填）
├── procurementId: Long             # FK → procurement.id（必填）
├── allocatedQuantity: Integer       # 本次分配的需求数量
├── status: MappingStatus            # 进行中 / 已完成 / 已取消
└── createTime: LocalDateTime
```

**关联约束（Q5 规则）**：
- `ReplenishmentDemand.subProductCode == Procurement.subProductCode` 时才允许创建
- 违反时抛出 `BusinessException`，提示"子货号不一致，无法关联"

**MappingStatus：**

| 状态 | 含义 | 触发条件 |
|------|------|----------|
| 进行中 | 分配已建立，等待验货完成 | 创建时默认 |
| 已完成 | 验货合格量 ≥ allocatedQuantity | 系统自动推进（见 §9.1） |
| 已取消 | 取消本次分配 | 人工操作 |

---

### 6.3 QcRecord 改造（v1.3.0 新增）

**设计决策：QcRecord → ShipmentBatch（FK 方向）**

```
旧设计（❌ JSON 列）：
ShipmentBatch ──JSON──▶ List<QcRecord>
问题：JSON 列无法在 DB 层强制唯一约束，JPA 映射为 @Transient 易出错

新设计（✅ 标准 FK）：
QcRecord ──────────────────────────────▶ ShipmentBatch
         └── shipmentBatchId: Long (FK, NOT NULL)

行业参照：SAP QM（Goods Receipt → Inspection Lot）、金蝶（收料通知单 → 检验单），
         用友（到货单 → 报检单）均采用「入库/到货单 → 检验单」的 1:N 方向。
```

**QcRecord 字段变更**：

| 变更 | 字段 | 说明 |
|------|------|------|
| **新增** | `shipmentBatchId: Long` | FK → shipment_batch.id（关联到出货批次） |
| **保留（nullable）** | `procurementId: Long` | 保留作为审计追溯，V43 迁移后变为 nullable |
| **保留** | `quantity: Integer` | 含义为「验货数量」，非订单数量 |

**V43 迁移（存量 QcRecord 回填 shipmentBatchId）**：

```sql
-- 策略：每个现有 QcRecord 按 procurement_id 找到或创建一个 ShipmentBatch，然后关联
-- 对于每个已有的 QcRecord：
--   ① 查找是否已存在对应 procurement 的 ShipmentBatch
--      - 存在：直接关联
--      - 不存在：自动创建一个 ShipmentBatch（status='已验货'，remark='历史数据迁移'）
--   ② 更新 qc_record.shipment_batch_id

ALTER TABLE qc_record ADD COLUMN shipment_batch_id BIGINT NULL;
ALTER TABLE qc_record ADD INDEX idx_qc_shipment_batch (shipment_batch_id);

UPDATE qc_record qc
JOIN (
  SELECT id AS batch_id, procurement_id
  FROM shipment_batch
  WHERE status = '已验货' AND remarks LIKE '%历史数据迁移%'
) sb ON sb.procurement_id = qc.procurement_id
SET qc.shipment_batch_id = sb.batch_id
WHERE qc.shipment_batch_id IS NULL;

-- V43 之后：procurement_id 改为 nullable
ALTER TABLE qc_record MODIFY COLUMN procurement_id BIGINT NULL;
```

**link-qc API 幂等性定义**：

```java
// ShipmentBatchUseCase.linkQc()
public void linkQc(Long batchId, Long qcRecordId) {
    // 幂等：已关联则跳过
    if (qcRecord.getShipmentBatchId() != null) {
        if (qcRecord.getShipmentBatchId().equals(batchId)) {
            return;  // 已关联到同一批次，幂等跳过
        }
        throw new BusinessException("qc.already_linked_to_another_batch");
    }
    qcRecord.setShipmentBatchId(batchId);
    batch.advanceStatusOnQcLink();  // 待验货 → 验货中
}
```

---

## 7. Phase2 状态语义

### 7.1 派生规则

「已下单/已出货」由 Assembler 层计算，不存储在 DB：

```java
// ProcurementAssembler.toDto()
long batchCount = shipmentBatchRepository.countByProcurementId(entity.getId());
String phase2Status = batchCount > 0 ? "已出货" : "已下单";
```

| 状态 | 派生条件 | 说明 |
|------|----------|------|
| **已下单** | batchCount == 0 | 已下单，但还没有任何出货批次 |
| **已出货** | batchCount > 0 | 至少有一个出货批次（不论是否验货） |

**注意**：`qcRecordIds` 是列表，不再作为"已出货"的判断依据。「已出货」由 `batchCount > 0` 派生（Q7），与验货记录数量无关。

### 7.2 监听器触发条件兼容

`ProcurementQcPassedEventListener` 的 Phase2 兼容性：

```java
// ProcurementQcPassedEventListener.advanceProcurementStatus()
ShipmentStatus current = procurement.getStatus();
if (current != ShipmentStatus.検品
    && current != ShipmentStatus.現地検品
    && current != ShipmentStatus.已出货) {   // ← 新增：Phase2 状态可触发
    return;
}
```

**触发后的状态推进逻辑**：

| 当前状态 | QC 类型/条件 | 目标状态 | 说明 |
|----------|-------------|----------|------|
| `已出货` | REMOTE | `メーカー直送` | 现场验货后直送 |
| `已出货` | ONSITE + 体积≤0.5m³ | `エア便` | 仓库验货后轻小件空运 |
| `已出货` | ONSITE + 体积>0.5m³ | `輸出` | 仓库验货后海运 |
| `検品` | — | 同上 | 旧状态兼容 |

> **关键**：Phase2「已出货」状态触发后，推进到「倉庫着」之后的路径，与旧状态「検品」一致。不会跳回「已下单」或原地不动。

---

## 8. 链路审计发现（v1.2.0 新增）

### 8.1 死代码：QcRecordCompletedEvent 永远不会被触发

**现状**（`QcRecordUseCase.update()`）：
```java
// ❌ 只有状态变更，没有任何事件发布
entity.updateStatus(QcStatus.COMPLETED);
qcRecordRepository.save(entity);
// ← 缺失：eventPublisher.publishEvent(new QcRecordCompletedEvent(...))
```

**后果**：`ProcurementQcPassedEventListener` 定义了监听器但永不被调用，Phase2「验货完成后自动推进发注单状态」完全失效。

**修复**：在 `QcRecordUseCase.update()` 中，状态推进为 COMPLETED 时发布 `QcRecordCompletedEvent`。

### 8.2 QcRecordCompletedEvent 被定义但从未 publish

| 文件 | 现状 |
|------|------|
| `qc/domain/event/QcRecordCompletedEvent.java` | ✅ 已定义 |
| `qc/domain/event/QcRecordCompletedEventPublisher.java` | ❌ 不存在 |
| `QcRecordUseCase.update()` | ❌ 未发布事件 |
| `ProcurementQcPassedEventListener` | ❌ 永远不会被触发 |

---

## 9. 下游事件链修正

### 9.1 Mapping 状态自动推进

**触发时机**：每当有 QcRecord 状态变为 COMPLETED 时

**规则**：`SUM(QcRecord.passedCount WHERE procurementId=X) >= Mapping.allocatedQuantity`
→ 将 Mapping.status 推进为"已完成"

**实现位置**：新增 `MappingStatusAutoUpdater`（Domain Service 或 UseCase）

### 9.2 SalesRecord.initialStock 修正（P0 级别）

**当前错误代码**（`JapanCustomsClearedEventListener.java:74`）：

```java
// ❌ 错误：用下单量作为初始库存
int quantity = (procurement.getQuantity() != null ? procurement.getQuantity() : 0);
record.setInitialStock(quantity);
record.setCurrentStock(quantity);
```

**修正后**（通过 ShipmentBatch 链路聚合 QcRecord）：

```java
// ✅ 正确：从关联的 ShipmentBatch 聚合 QcRecord 验货合格量
List<Long> batchIds = shipmentBatchRepository.findIdsByProcurementId(procurementId);
Integer passed = qcRecordRepository.sumPassedCountByBatchIds(batchIds);
int stock = (passed != null) ? passed : 0;
record.setInitialStock(stock);
record.setCurrentStock(stock);
```

**前置**：
1. `ShipmentBatchRepository.findIdsByProcurementId(Long)` — 查询某采购单下的所有批次 ID
2. `QcRecordRepository.sumPassedCountByBatchIds(List<Long>)` — 按批次 ID 列表聚合 passedCount

**为什么不用直接按 procurementId 聚合**：QcRecord 通过 ShipmentBatch 关联到 Procurement（间接 N:1），
只有关联到 ShipmentBatch 的 QcRecord 才算作该采购单的验货记录。
直接按 procurementId 聚合会错误计入独立创建的 QcRecord。

### 9.3 LogisticsPlan.quantity 修正（P2 级别）

**当前代码**（`LogisticsPlanAssembler.java:87`）：`quantity` 直接从 `LogisticsPlanCreateCmd` 的 quantity 字段代入，未做来源约束。

**问题**：代入的是用户在调配计划表单填写的量，而非实际验货合格量。

**修正后**：在 `LogisticsPlanUseCase.create()` 中，从关联 QcRecord 聚合实际调运数量：

```java
// ✅ 从 QcRecord 聚合
Integer passed = qcRecordRepository.sumPassedCountByProcurementId(cmd.getProcurementId());
entity.setQuantity(passed != null ? passed : cmd.getQuantity());
```

### 9.4 全链路事件链审计（v1.5.0 新增）

**四条事件链状态**：

| 链 | 路径 | 状态 | 说明 |
|----|------|------|------|
| Chain 1 | SalesRecord → ReplenishmentDemand | ✅ 正常 | 低库存触发补货需求 |
| Chain 2 | JapanCustoms → SalesRecord | ✅ 正常 | 清关完成自动建销售记录 |
| Chain 3 | QcRecord → Procurement | ❌ **断裂** | QC 完成推进发注单状态 |
| Chain 4 | LogisticsPlan → DomesticCustoms | ❌ **断裂** | 物流在途自动创建报关记录 |

**Chain 3（已断裂）**：
```
QcRecordUseCase.update()
    │
    ├── entity.setStatus(QcStatus.COMPLETED)  ✅ 状态变更
    └── ❌ 无 ApplicationEventPublisher 从未发布 QcRecordCompletedEvent
            │
            └── ❌ ProcurementQcPassedEventListener 永远不被触发
                    │
                    └── ❌ Phase2「验货完成→推进发注单状态」完全失效
```

**Chain 4（已断裂）**：
```
LogisticsPlanUseCase.update()
    │
    ├── entity.setStatus(LogisticsStatus.IN_TRANSIT)  ✅ 状态变更
    └── ❌ 无 ApplicationEventPublisher 从未发布 LogisticsPlanInTransitEvent
            │
            └── ❌ LogisticsInTransitEventListener 永远不被触发
                    │
                    └── ❌ 「物流在途→自动创建国内报关记录」完全失效
```

**监听器跨模块违规**：

| 监听器 | 模块 | 违规 | 应改为 |
|--------|------|------|--------|
| `SalesLowStockEventListener` | replenishment | 直接注入 `ProcurementRepository` | `ProcurementQueryPort` |
| `LogisticsInTransitEventListener` | logistics | 直接注入 `DomesticCustomsRepository` | `CustomsCommandPort` |

---

## 10. API 设计

### 10.1 ShipmentBatch API

```
GET    /api/v1/shipment-batches?page=&pageSize=&procurementId=&status=
GET    /api/v1/shipment-batches/{id}
POST   /api/v1/shipment-batches
PATCH  /api/v1/shipment-batches/{id}
DELETE /api/v1/shipment-batches/{id}       # 仅待验货状态可删除
POST   /api/v1/shipment-batches/{id}/link-qc   # 追加验货记录到批次（1:N）
GET    /api/v1/shipment-batches/{id}/qc-records   # 查询批次关联的所有验货记录
```

**`POST /shipment-batches/{id}/link-qc` 请求体**：
```json
{ "qcRecordId": 123 }
```

**响应**：更新后批次状态自动推进（`待验货`→`验货中`），返回更新后的 ShipmentBatch 详情。

### 10.2 DemandProcurementMapping API

```
GET    /api/v1/demand-mappings?page=&pageSize=&demandId=
GET    /api/v1/demand-mappings/{id}
POST   /api/v1/demand-mappings                           # 关联（含子货号校验）
DELETE /api/v1/demand-mappings/{id}                       # 取消分配
```

---

## 11. 数据库迁移

| 序号 | 文件 | 内容 | 风险 | 优先级 |
|------|------|------|------|--------|
| V40 | `V40__shipment_batch_table.sql` | 新建 `shipment_batch` 表 | 低 | P1 |
| V41 | `V41__demand_procurement_mapping.sql` | 新建 `demand_procurement_mapping` 表 | 低 | P1 |
| V42 | `V42__migrate_linked_procurement_to_mapping.sql` | 存量 `linked_procurement_id` 数据回填 | 中 | P2 |
| V43 | `V43__qc_record_add_shipment_batch_id.sql` | QcRecord 增加 `shipment_batch_id` FK；`procurement_id` 改为 nullable | 中 | P1 |

**当前 Flyway 状态**：最新版本为 V39（`V39__procurement_after_sales_deadline.sql`），V40 起始。

**注意**：项目 `ddl-auto: update` 模式下，JPA 不会创建新表。V40/V41 必须在应用启动时通过 `@PostConstruct` 幂等迁移组件执行（见 Lesson 59）。

**V40 DDL（ShipmentBatch 表，与 QcRecord 解耦）**：

```sql
CREATE TABLE shipment_batch (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  procurement_id   BIGINT NOT NULL,
  batch_code       VARCHAR(32) NOT NULL,
  shipment_quantity INT NOT NULL,
  factory_ship_date DATE,
  actual_ship_date DATE,
  status           VARCHAR(16) NOT NULL DEFAULT '待验货',
  remarks          VARCHAR(512),
  create_time      DATETIME(3) NOT NULL,
  update_time      DATETIME(3) NOT NULL,
  create_by        VARCHAR(64) NOT NULL,
  update_by        VARCHAR(64) NOT NULL,
  is_deleted       TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_sb_procurement (procurement_id),
  INDEX idx_sb_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**V43 DDL（QcRecord 增加 shipment_batch_id FK）**：

```sql
ALTER TABLE qc_record ADD COLUMN shipment_batch_id BIGINT NULL;
ALTER TABLE qc_record ADD INDEX idx_qc_shipment_batch (shipment_batch_id);
ALTER TABLE qc_record ADD CONSTRAINT fk_qc_shipment_batch
  FOREIGN KEY (shipment_batch_id) REFERENCES shipment_batch(id);
ALTER TABLE qc_record MODIFY COLUMN procurement_id BIGINT NULL;  -- V43 之后变为 nullable
```

**V42 回填逻辑**：

```sql
INSERT INTO demand_procurement_mapping
  (demand_id, procurement_id, allocated_quantity, status, create_time)
SELECT id, linked_procurement_id, quantity, '已完成', NOW()
FROM replenishment_demand
WHERE linked_procurement_id IS NOT NULL AND deleted = false;
```

---

## 12. 完整改动清单

> 以下清单包含所有受影响的文件，按优先级排序。

### P0 — 立即修复（不依赖 ShipmentBatch）

| 文件 | 改动 | 类型 |
|------|------|------|
| `QcRecordUseCase.java` | ① 注入 `ApplicationEventPublisher`；② `update()` 中 COMPLETED 时发布 `QcRecordCompletedEvent`；③ 改用 `entity.updateStatus()` 而非 `setStatus()`；④ result==PASS 才推进 COMPLETED | 修改 |
| `QcRecordCompletedEventPublisher.java` | 新增事件发布辅助方法 | 新增 |
| `LogisticsPlanUseCase.java` | 注入 `ApplicationEventPublisher`；状态推进 IN_TRANSIT 时发布 `LogisticsPlanInTransitEvent`（EV-121） | 修改 |
| `ShipmentBatchRepository.java` | 新增 `findIdsByProcurementId(Long)` — 查某采购单下所有批次 ID | 新增 |
| `QcRecordRepository.java` | 新增 `sumPassedCountByBatchIds(List<Long>)` — 按批次 ID 聚合合格量 | 修改 |
| `JapanCustomsClearedEventListener.java` | 注入 `ShipmentBatchRepository`；`initialStock` 改为通过 ShipmentBatch 链路聚合 QcRecord | 修改 |

### P0 — 跨模块防腐（Lesson 1 违规修复）

| 文件 | 改动 | 类型 |
|------|------|------|
| `QcQueryPort.java` | 新增 `sumPassedCountByBatchIds(List<Long>)` 方法到 Port 接口 | 修改 |
| `JpaQcQueryPort.java` | 实现 `sumPassedCountByBatchIds()` | 新增 |
| `ShipmentBatchQueryPort.java` | 新增接口：查询批次列表 | 新增 |
| `ProcurementQueryPort.java` | 新增接口：procurement 模块查询（EV-122） | 新增 |
| `ProcurementAssembler.java` | 移除 `QcRecordRepository` 直接注入；改用 `QcQueryPort` | 修改 |
| `SalesLowStockEventListener.java` | 移除 `ProcurementRepository`；改用 `ProcurementQueryPort`（EV-122） | 修改 |
| `LogisticsInTransitEventListener.java` | 移除 `DomesticCustomsRepository`；改用 `CustomsQueryPort`（EV-123） | 修改 |
| `MappingStatusAutoUpdater.java` | 只依赖 Port 接口，不直接注入 QcRecordRepository / ShipmentBatchRepository（EV-120） | 新增 |

### P1 — QcRecord 改造（v1.3.0 新增）

| 文件 | 改动 | 类型 |
|------|------|------|
| `QcRecord.java` | 新增 `shipmentBatchId: Long` 字段；`procurementId` 保留（V43 后 nullable） | 修改 |
| `QcRecordAssembler.java` | 新增 `shipmentBatchId` 映射 | 修改 |
| `V43__qc_record_add_shipment_batch_id.sql` | 增加 shipment_batch_id FK 列及索引 | 新增 |
| `QcRecordUseCase.java` | `create()` 中可选接受 shipmentBatchId 参数 | 修改 |
| `ShipmentBatchUseCase.java` | `linkQc()` 含幂等检查：已关联则跳过；跨批次则抛异常 | 新增 |

### P1 — DB + Entity 基础建模

| 文件 | 改动 | 类型 |
|------|------|------|
| `V40__shipment_batch_table.sql` | 新建 shipment_batch 表（`qc_record_ids` 为 JSON 列） | 新增 |
| `V41__demand_procurement_mapping.sql` | 新建 demand_procurement_mapping 表 | 新增 |
| `V42__migrate_linked_procurement_to_mapping.sql` | 存量数据回填 | 新增 |
| `ShipmentBatch.java` | 实体（`qcRecordIds: List<Long>`，JSON 列映射） | 新增 |
| `ShipmentBatchStatus.java` | 枚举（含 FSM） | 新增 |
| `DemandProcurementMapping.java` | 实体 | 新增 |
| `MappingStatus.java` | 枚举 | 新增 |
| `ShipmentBatchRepository.java` | 领域接口 | 新增 |
| `JpaShipmentBatchRepository.java` | JPA 实现（含 `@Qualifier`） | 新增 |
| `DemandProcurementMappingRepository.java` | 领域接口 | 新增 |
| `JpaDemandProcurementMappingRepository.java` | JPA 实现（含 `@Qualifier`） | 新增 |

### P1 — 核心业务逻辑

| 文件 | 改动 | 类型 |
|------|------|------|
| `ProcurementAssembler.java` | 注入 ShipmentBatchRepository；`batchCount > 0` 派生 Phase2 状态；移除 qcRecordId 依赖 | 修改 |
| `ProcurementPageQuery.java` | 新增 `batchCount`、`totalShippedQuantity`、`totalPassedQuantity` 字段 | 修改 |
| `ShipmentBatchUseCase.java` | CRUD + 状态推进（含并发乐观锁）+ linkQc 幂等 | 新增 |
| `DemandProcurementMappingUseCase.java` | 分配 + 取消 + 子货号校验（含唯一约束冲突处理） | 新增 |
| `ProcurementQcPassedEventListener.java` | Phase2 状态触发条件兼容（增加 `已出货`） | 修改 |
| `StepStatusCalculator.java` | 从 `OrderOverviewAssembler.computeStepStatuses()` 提取（EV-114）：CC=13 → ≤8，调用各 `isTerminal()` 枚举方法 | 新增 |
| `OrderOverviewAssembler.java` | 移除 `computeStepStatuses()` 业务逻辑；改调用 `StepStatusCalculator`（EV-105） | 修改 |
| `DomesticCustomsStatus.java` | 新增 `isTerminal()` 方法：`return this == CLEARED || this == FAILED`（EV-115） | 修改 |
| `OrderOverviewUseCase.java` | 消除重复 factoryRepository 查询：缓存首次结果（EV-117） | 修改 |
| `MappingStatusAutoUpdater.java` | Mapping 状态自动推进（含幂等：检查当前状态再推进） | 新增 |
| `QcRecordAssembler.java` | `toDto()` 移除跨模块 IO 查询，数据聚合下沉至 `QcRecordUseCase`（EV-124） | 修改 |
| `QcRecordUseCase.java` | `generateQcCode()` 从 Assembler 移入 UseCase，改用数据库序列（EV-125） | 修改 |

### P2 — 下游链路修复

| 文件 | 改动 | 类型 |
|------|------|------|
| `LogisticsPlanUseCase.java` | `create()` 中聚合 QcRecord passedCount | 修改 |
| `LogisticsPlanAssembler.java` | `quantity` 字段改为从 QcRecord 聚合代入（保留 cmd 作为兜底） | 修改 |

### P2 — 前端

| 文件 | 改动 | 类型 |
|------|------|------|
| `procurement.ts` | `ProcurementPageVO` 新增 `batchCount`、`totalShippedQuantity`、`totalPassedQuantity` | 修改 |
| `ProcurementPage.vue` | 状态判断改为 `batchCount > 0`；新增出货批次抽屉 | 修改 |
| `DemandPage.vue` | 显示出货分配进度 | 修改 |
| `QcRecordPage.vue` | 验货创建可选关联 ShipmentBatch | 修改 |

### P1 — 视图升级

| 文件 | 改动 | 类型 |
|------|------|------|
| `v_order_chain_v2.sql` | 新建聚合视图（含批次汇总字段） | 新增 |
| `OrderChainVO.java` | 新增 `totalAllocatedQuantity`、`totalShippedQuantity`、`totalPassedQuantity`、`batchCount` | 修改 |
| `OrderOverviewUseCase.java` | 对接 v2 视图 | 修改 |

### P3 — OrderOverview 重构

| 文件 | 改动 | 类型 |
|------|------|------|
| `StepStatusCalculator.java` | 从 `OrderOverviewAssembler.computeStepStatuses()` 提取为领域服务 | 新增 |
| `OrderOverviewAssembler.java` | 调用 `StepStatusCalculator` 而非内联逻辑 | 修改 |
| `Procurement.java` | 增加 `@Version` 乐观锁（EV-104） | 修改 |

### P3 — SPEC 文档同步

| 文件 | 改动 | 类型 |
|------|------|------|
| `SPEC-B01.md` | 补充 `DemandProcurementMapping` 的 N:1 说明 | 修改 |
| `SPEC-B02.md` | 移除 `convertToProcurement()` 方法引用（已被反向关联替代） | 修改 |
| `DOMAIN-发注管理领域模型.md` | 移除 `QcRecord.taxRefund` 字段（已在 SPEC-B03 v1.1.1 删除） | 修改 |

---

## 13. 不需要修改的文件（审计确认）

以下文件经审计确认不受 SPEC-B11 影响，**无需修改**：

| 文件 | 原因 |
|------|------|
| `TaxRefundAssembler.java` | `TaxRefundRecord.quantity` 是自身字段（本次退税申报数量），不由 Procurement.quantity 派生 |
| `QcRecordAssembler.copyCreate()` / `copyUpdate()` | 纯字段映射部分不受影响；`generateQcCode()` 和 `toDto()` 含 IO 的部分需单独修复（EV-124/EV-125） |
| `ReplenishmentDemandAssembler.java` | 同上，Demand.quantity 是需求数量，不是衍生值 |
| `ReplenishmentDemandUseCase.java` | linkToProcurement / unlinkProcurement 方法不受影响，linkedProcurementId 保留兼容 |
| `ProcurementUseCase.java` | 创建/更新逻辑不涉及 ShipmentBatch，可独立演进 |
| `ProcurementCreateCmd.java` | ShipmentBatch 在验货步骤创建，不在发注单创建时处理 |
| `ProcurementUpdateCmd.java` | 同上 |

---

## 14. 冲突分析与 SPEC 同步

### 14.1 SPEC 文档内部冲突（v1.2.0 新增）

| 冲突 | B01 v2.2.0 | B02 | B09 | 正确值 |
|------|-----------|-----|-----|--------|
| DemandStatus 枚举值 | PENDING / CONFIRMED | 含 CONVERTED/CANCELLED | PENDING / CONFIRMED | B01 正确，B02 需同步 |
| Demand↔Procurement 基数 | 1 Demand → N Procurement（N:1） | 1:1（convert 模式） | 1:1（单锚点） | B01 N:1 正确，B02/B09 需同步 |
| LogisticsPlan.quantity 来源 | — | — | — | 应从 QcRecord 聚合（见 §9.3） |

**修复策略**：
- SPEC-B01 补充：`DemandProcurementMapping` 支持 N:1（一个需求关联多个采购单）
- SPEC-B02：移除 `convertToProcurement()` 方法描述，改为 Demand 主动关联 Procurement
- SPEC-B09：1:1 锚点降级为「首个关联」，全链路走 `v_order_chain_v2` 聚合视图

### 14.2 Phase2 状态 vs 验货监听器冲突

**问题**：`ProcurementQcPassedEventListener` 的触发条件不包含 `已出货` 状态，导致 Phase2 模式下验货完成后不推进 Procurement 状态。

**解决方案**（见 §7.2）：在监听器触发条件中增加 `已出货` 状态。

### 14.3 qcRecordId 单值 vs 多 QC 记录冲突

**问题**：`ProcurementAssembler` 当前取第一条 QC 记录的 ID 作为 `qcRecordId`，多记录时语义模糊。

**解决方案**：移除 `qcRecordId` 作为状态判断依据，改为 `batchCount > 0` 派生。

### 14.4 linkedProcurementId 保留 vs N:1 关联冲突

**问题**：旧字段 `linkedProcurementId` 是单值，与 N:1 关联冲突。

**解决方案**：`linkedProcurementId` 保留（向后兼容），新增 `DemandProcurementMapping` 表走新链路。读取时优先 Mapping 表，兼容旧数据（V42 回填）。

### 14.5 OrderChainView 单值锚点 vs 多 Procurement 冲突

**问题**：视图 `linked_procurement_id` 单值，多个 Procurement 时只能展示第一个。

**解决方案**：新建 `v_order_chain_v2` 聚合视图，汇总字段展示全链路数量。

---

## 15. 向后兼容策略

- `linkedProcurementId` 保留，前端快速展示"第一个关联 Procurement"继续使用
- 存量数据通过 V42 回填至 `DemandProcurementMapping` 表
- `qcRecordId` 在 DTO 中保留，但降级为参考字段
- Phase2 状态由 Assembler 派生，不破坏现有枚举存储
- `QcRecordCompletedEvent` 修复后，向后兼容：旧数据无 QC 记录时 listener 自然跳过

---

## 16. 黑匣子溯源

```
基因ID: EV-101
触发场景: 一次补货需求分多次采购，或一次采购分多次出货，当前系统只能记录第一条
根因铁律: 基数不对称律 — 将 1:N 误建模为 1:1，导致三层链条全部扁平化
手术: 引入 ShipmentBatch + DemandProcurementMapping

基因ID: EV-102
触发场景: JapanCustomsClearedEventListener 使用 procurement.getQuantity() 作为 SalesRecord.initialStock
根因铁律: 单一真相律 — 用错了数量语义（下单量 ≠ 实际到货量）
手术: 从 QcRecord 聚合 passedCount，P0 可立即修复

基因ID: EV-103
触发场景: ProcurementQcPassedEventListener 触发条件不包含 Phase2「已出货」状态
根因铁律: 形式化验证律 — 状态转换未覆盖 Phase2 新增状态
手术: 在触发条件中增加已出货状态，V40 落地后修复

基因ID: EV-104
触发场景: Procurement.updateStatus() 无乐观锁，高并发下 FSM 转换竞态
根因铁律: 隔离性 — 状态转换无并发控制
手术: Procurement 实体增加 @Version 注解

基因ID: EV-105
触发场景: OrderOverviewAssembler.computeStepStatuses() 含 74 行业务逻辑在 Assembler 层
根因铁律: 防腐层 — Adapter 不应含业务判断
手术: 提取为 StepStatusCalculator 领域服务

基因ID: EV-106（v1.2.0 新增）
触发场景: QcRecordUseCase.update() 从未发布 QcRecordCompletedEvent，导致 ProcurementQcPassedEventListener 永远不被触发
根因铁律: 幂等性 — 事件发布链断裂
手术: 在 QcRecordUseCase.update() 中发布事件

基因ID: EV-107（v1.2.0 新增）
触发场景: ProcurementAssembler 直接注入 QcRecordRepository，跨模块依赖
根因铁律: 防腐层 — 模块间不得直接依赖 domain repository
手术: 通过 QcQueryPort 隔离

基因ID: EV-108（v1.2.0 新增）
触发场景: LogisticsPlan.quantity 从 CreateCmd 直接代入，未从 QcRecord 聚合
根因铁律: 单一真相律 — 表单填写量 ≠ 实际验货合格量
手术: 在 UseCase 层从 QcRecord 聚合

基因ID: EV-109（v1.2.0 新增）
触发场景: SPEC-B01 v2.2.0 / SPEC-B02 / SPEC-B09 的 DemandStatus 和关联模型互相冲突
根因铁律: 文档一致性 — 多文档同一实体定义必须同步
手术: SPEC-B02 同步 B01，SPEC-B09 降级锚点

基因ID: EV-110（v1.2.0 新增）
触发场景: 第一版设计用 JSON 列存储 qcRecordIds（1:N），DB 层无约束，JPA 映射不自然
根因铁律: 形式化验证律 — 缺少数据库层 FK 约束
手术: 改为 QcRecord.shipmentBatchId FK 列，V43 迁移脚本回填历史数据

基因ID: EV-111（v1.3.0 新增）
触发场景: 多条 QcRecord 同时 COMPLETED 时，ShipmentBatch 状态推进存在竞态
根因铁律: 隔离性 — 状态更新无乐观锁，高并发下 FSM 转换竞态
手术: ShipmentBatch.updateStatus() 加 @Version；batchCount > 0 判断加@Transactional(readOnly=true) 快照读

基因ID: EV-112（v1.4.0 新增）
触发场景: QcRecordUseCase 无 ApplicationEventPublisher 字段，QcRecordCompletedEvent 从未发布，整条事件链断裂
根因铁律: 链路完整性 — 监听器存在但永不被触发，Phase2 状态推进完全失效
手术: QcRecordUseCase 注入 ApplicationEventPublisher，update() 推进 COMPLETED 时发布事件

基因ID: EV-113（v1.4.0 新增）
触发场景: QcRecordUseCase.update() 第 105 行直接调用 entity.setStatus(QcStatus.COMPLETED)，绕过了 entity.updateStatus() 的 FSM 验证
根因铁律: 形式化验证律 — 状态变更绕过 FSM，导致 FAIL 记录也能变成 COMPLETED
手术: 改用 entity.updateStatus(QcStatus.COMPLETED)；增加 result==PASS 前置校验

基因ID: EV-114（v1.4.0 新增）
触发场景: OrderOverviewAssembler.computeStepStatuses() 圈复杂度=13（判定点13个），远超阈值8
根因铁律: 认知负荷 — 单方法承担过多分支，测试覆盖率难以保证
手术: 提取为 StepStatusCalculator，CC 降至 ≤8；每步状态判断独立方法

基因ID: EV-115（v1.4.0 新增）
触发场景: DomesticCustomsStatus 无 isTerminal() 方法，而 JapanCustomsStatus 有；StepStatusCalculator 无法统一调用
根因铁律: 形式化验证律 — 同类实体枚举接口不一致
手术: DomesticCustomsStatus 增加 isTerminal() 方法（return CLEARED || FAILED）

基因ID: EV-116（v1.4.0 新增）
触发场景: 所有 VO（OrderOverviewPageVO、QcRecordVO 等）用 String 存储 status，Assembler 中用字符串比较而非枚举方法
根因铁律: 类型安全 — 字符串比较容易写错，IDE 无法提示
手术: VO 中 status 改为枚举类型（QcStatus、LogisticsStatus 等），Assembler 中调用 .isTerminal()

基因ID: EV-117（v1.4.0 新增）
触发场景: OrderOverviewUseCase 第 70-73 行和第 130-133 行对同一 procurement.FactoryId 查询了两次 factoryRepository
根因铁律: 重复代码 — 同一查询执行两次，浪费数据库连接
手术: 首次查询后缓存 factoryName 和 FactoryVO，第二次直接引用缓存

基因ID: EV-118（v1.4.0 新增）
触发场景: DemandProcurementMapping 表无唯一约束，同一 (demand_id, procurement_id) 可重复插入
根因铁律: 形式化验证律 — 数据库层无约束保证唯一性
手术: V41 DDL 增加 UNIQUE KEY uk_demand_procurement (demand_id, procurement_id)

基因ID: EV-119（v1.4.0 新增）
触发场景: QcRecordUpdateCmd 逻辑：当 result != null 时自动将 status 设为 COMPLETED，不校验 result 是否为 PASS
根因铁律: 业务正确性 — FAIL 结果的记录也会变成 COMPLETED 状态
手术: 改为只有在 result==PASS 时才推进 COMPLETED；FAIL → RETURN_REQUESTED

基因ID: EV-120（v1.4.0 新增）
触发场景: MappingStatusAutoUpdater 需要同时访问 QcRecord（qc 模块）和 ShipmentBatch（procurement 模块），若直接注入 repository 则违反 Lesson 1
根因铁律: 防腐层 — replenishment 模块直接依赖 qc 和 procurement 的 domain repository
手术: 新增 QcQueryPort.sumPassedCountByBatchIds()、ShipmentBatchQueryPort 接口，MappingStatusAutoUpdater 只依赖 Port

基因ID: EV-121（v1.5.0 新增）
触发场景: `LogisticsPlanInTransitEvent` 定义了，`LogisticsInTransitEventListener` 存在，但 `LogisticsPlanUseCase` 从未发布该事件，导致物流在途→自动创建国内报关记录链断裂
根因铁律: 链路完整性 — 事件发布链断裂，第二条断裂链
手术: LogisticsPlanUseCase 注入 ApplicationEventPublisher，状态推进 IN_TRANSIT 时发布事件

基因ID: EV-122（v1.5.0 新增）
触发场景: `SalesLowStockEventListener` 直接注入 `ProcurementRepository`（procurement 模块），违反防腐层原则
根因铁律: 防腐层 — replenishment 模块直接依赖 procurement domain repository
手术: 新增 `ProcurementQueryPort` 接口，listener 只依赖 Port

基因ID: EV-123（v1.5.0 新增）
触发场景: `LogisticsInTransitEventListener` 直接注入 `DomesticCustomsRepository`（customs 模块），违反防腐层原则
根因铁律: 防腐层 — logistics 模块直接依赖 customs domain repository
手术: 已有 `CustomsQueryPort`，扩展为 `CustomsCommandPort`，或直接使用现有 Port 读+Command混合

基因ID: EV-128（v1.5.0 新增）
触发场景: `ProcurementQcPassedEventListener` 仅监听 `検品`/`現地検品` 两个状态，但 Phase2 新增 `已出货` 作为 QC 关联后的中间状态，验货时采购单状态已为 `已出货`，导致事件到达但监听器跳过
根因铁律: 状态机演化 — 新增 Phase2 状态后，监听器触发条件未同步扩展
手术: 监听器触发条件增加 `已出货`：`current != ShipmentStatus.検品 && current != ShipmentStatus.現地検品 && current != ShipmentStatus.已出货`

基因ID: EV-124（v1.5.0 新增）
触发场景: `QcRecordAssembler.toDto()` 方法中执行跨模块实时 IO 查询（`procurementPort.findFactoryIdById()` + `factoryQueryPort.findByIdAndDeletedIsFalse()`），违反 Assembler 纯映射原则
根因铁律: 分型 — Assembler 承担了 UseCase 的职责（数据聚合），不含业务判断的转换层不应有副作用
手术: `toDto()` 只接收已聚合好的数据，跨模块查询下沉至 `QcRecordUseCase` 聚合后传入

基因ID: EV-125（v1.5.0 新增）
触发场景: `QcRecordAssembler.generateQcCode()` 在 Assembler 层执行 `AtomicLong SEQ.incrementAndGet()`，序列号生成是业务逻辑且非幂等（JVM 重启归零）
根因铁律: 幂等性 — 单 JVM 序列号在高并发/多实例下碰撞；Assembler 不应含业务逻辑
手术: 移至 `QcRecordUseCase.create()`，改用数据库序列或分布式 ID

基因ID: EV-126（v1.5.0 新增）
触发场景: 前端 `ProcurementPage.vue` 在有 QC 记录时阻止所有状态切换（`if (row.qcRecordId) return`），但后端 FSM 允许 `已出货 → 已下单`；用户无法通过 UI 撤销
根因铁律: 交互闭环 — 前端过度限制导致合法后端操作无法在 UI 执行
手术: 前端在有 ShipmentBatch 但无 COMPLETED QC 时允许切换，或前端逻辑改为读取后端实际状态

基因ID: EV-127（v1.5.0 新增）
触发场景: 前端 `QcRecordVO` 和后端 `QcRecordPageQuery` 均无 `shipmentBatchId` 字段，Phase2 ShipmentBatch 功能上线时需双向同步扩展
根因铁律: 接口一致性 — API 契约变更需前后端同步
手术: Phase2 实施时同步添加 `shipmentBatchId` 到前端 TS 接口和后端 DTO；i18n 需补充 shipmentBatch 相关 key（~15-20 条）
```

---

## 附录：INTJ 审计发现（v1.2.0 追加）

### INTJ-1. 熵增诊断

| # | 决策点 | 违反铁律 | 手术方向 |
|---|--------|----------|---------|
| 1 | `qcRecordId` 单值用于状态判断 | 基数对称律 | 移除，改用 batchCount 计数 |
| 2 | `JapanCustomsClearedEventListener.initialStock = procurement.quantity` | 单一真相律 | 从 QcRecord 聚合 passedCount |
| 3 | `ProcurementQcPassedEventListener` 触发条件缺失 `已出货` | 形式化验证律 | 增加 `已出货` 状态触发条件 |
| 4 | `LogisticsPlanAssembler.quantity = cmd.quantity` | 单一真相律 | 从 QcRecord 聚合 |
| 5 | `OrderChainView.linked_procurement_id` 单值锚点 | 基数对称律 | 新建 v2 聚合视图 |
| 6 | `QcRecordUseCase` 无 ApplicationEventPublisher，事件链断裂 | 链路完整性 | 注入 publisher，COMPLETED 时发布事件 |
| 7 | `ProcurementAssembler` 直接注入 `QcRecordRepository` | 防腐层 | 改用 QcQueryPort |
| 8 | `OrderOverviewAssembler.computeStepStatuses()` CC=13 | 认知负荷 | 提取为 StepStatusCalculator，CC≤8 |
| 9 | SPEC-B01/B02/B09 DemandStatus 冲突 | 文档一致性 | 同步 B01，修正 B02/B09 |
| 10 | `Procurement` 无 `@Version` | 隔离性 | 增加乐观锁 |
| 11 | `QcRecordUseCase.update()` 直接 setStatus() 绕过 FSM | 形式化验证律 | 改用 entity.updateStatus() |
| 12 | `DomesticCustomsStatus` 无 isTerminal() | 一致性 | 增加 isTerminal() |
| 13 | VO status 用 String 而非枚举 | 类型安全 | 改枚举类型 |
| 14 | `OrderOverviewUseCase` 重复查询 factoryRepository | 重复代码 | 缓存首次结果 |
| 15 | `DemandProcurementMapping` 无唯一约束 | 形式化验证律 | 加 UNIQUE KEY |
| 16 | `QcRecordUpdateCmd` result==FAIL 也会推进 COMPLETED | 业务正确性 | 增加 result==PASS 前置校验 |
| 17 | `LogisticsPlanInTransitEvent` 从未发布，物流→报关链断裂 | 链路完整性 | 在 LogisticsPlanUseCase 发布事件 |
| 18 | `SalesLowStockEventListener` 直接注入 `ProcurementRepository` | 防腐层 | 改用 ProcurementQueryPort |
| 19 | `LogisticsInTransitEventListener` 直接注入 `DomesticCustomsRepository` | 防腐层 | 改用 CustomsQueryPort |
| 20 | `QcRecordAssembler.toDto()` 含跨模块 IO 查询 | 分型 | IO 下沉至 UseCase |
| 21 | `QcRecordAssembler.generateQcCode()` 用 JVM 内存序列 | 幂等性 | 改用数据库序列 |
| 22 | 前端 toggle 阻止所有状态切换，与后端 FSM 不一致 | 交互闭环 | toggle 逻辑与后端对齐 |

### INTJ-2. 铁律判定表

| 判定 | 触发 | 说明 |
|------|------|------|
| 判定2（嵌套 ≥ 3 层） | ✅ 触发 | `ProcurementAssembler.toDto()` → `findByProcurementId()` → `stream.findFirst()` 三层嵌套；拆分为纯聚合函数 |
| 判定8（状态用 if 判断） | ✅ 触发 | `ProcurementQcPassedEventListener` 用 if 判断触发条件；改为 switch 或 Map 查找 |
| 判定9（副作用无幂等） | ✅ 触发 | `MappingStatusAutoUpdater` 未明确幂等性；必须在方法签名加 `@Transactional` 并检查当前状态 |
| 判定12（状态转换无并发控制） | ✅ 触发 | `Procurement.updateStatus()` 和 `ShipmentBatch` 状态更新均无乐观锁；解决： `@Version` |
| 判定13（Adapter 含业务判断） | ✅ 触发 | `OrderOverviewAssembler.computeStepStatuses()` CC=13，属于领域服务，不应放在 Assembler；手术：提取为 `StepStatusCalculator` |
| 判定15（重复提交） | ✅ 触发 | `MappingStatusAutoUpdater` 无幂等防护；`linkQc` 未检查重复关联 |
| 判定16（重复代码） | ✅ 触发 | `OrderOverviewUseCase` 对同一 FactoryId 查询了两次 factoryRepository |
| 其他判定 | ❌ 未触发 | core/ 无循环依赖；无全局 static；无第三方 API 渗透 |

### INTJ-3. 架构层级检查

所有新增实体 DDD 分层正确：

| 实体 | 应属层 | 判定 |
|------|--------|------|
| `ShipmentBatch.java` | `procurement/domain/model/` | ✅ |
| `ShipmentBatchStatus.java` | `procurement/domain/model/` | ✅ |
| `DemandProcurementMapping.java` | `replenishment/domain/model/` | ✅ |
| `MappingStatus.java` | `replenishment/domain/model/` | ✅ |
| `ShipmentBatchRepository` | `procurement/domain/repository/` | ✅ |
| `DemandProcurementMappingRepository` | `replenishment/domain/repository/` | ✅ |
| `ShipmentBatchUseCase` | `procurement/application/usecase/` | ✅ |
| `DemandProcurementMappingUseCase` | `replenishment/application/usecase/` | ✅ |

### INTJ-4. 新增基因（已归档至 §16）

| 基因ID | 根因 | 触发场景 | 手术 |
|--------|------|----------|------|
| EV-104 | 并发控制缺失 | `Procurement.updateStatus()` 无锁，高并发下 FSM 竞态 | 增加 `@Version` 乐观锁 |
| EV-105 | Adapter 含业务逻辑 | `OrderOverviewAssembler.computeStepStatuses()` 放错层 | 提取为 `StepStatusCalculator` 领域服务 |
| EV-106 | 事件发布链断裂 | `QcRecordUseCase` 不发布事件，监听器死代码 | 发布 `QcRecordCompletedEvent` |
| EV-107 | 跨模块依赖 | `ProcurementAssembler` 直接注入 `QcRecordRepository` | 扩展 `QcQueryPort` |
| EV-108 | 单一真相律 | `LogisticsPlan.quantity` 表单填写量 | 从 QcRecord 聚合 |
| EV-109 | 文档不一致 | SPEC 文档间 DemandStatus 和关联模型冲突 | 同步 B01/B02/B09 |

### INTJ-5. 量化评估

```
重构前熵密度 = 13（CC=13 computeStepStatuses）× 4（错误抽象）/ 2（实体）= 26.0  ⚠️ 极高危
重构后预期熵密度 = 0.5（v2 视图同步延迟残留）
熵减贡献：
  EV-106/112/121 事件链修复（2条断裂链）：-5.0
  EV-113/119 FSM 绕过修复：-2.0
  EV-102 initialStock 修复：-2.0
  EV-107/120/122/123 跨模块防腐（4个违规）：-5.0
  EV-105/114 StepStatusCalculator：-2.0
  EV-124/125 Assembler 净化：-1.5
风险项：v2 视图同步延迟 + 历史 COMPLETED 事件补偿 + 前端 toggle 语义冲突
```

### INTJ-6. 向后兼容风险（已补充）

| 场景 | 风险 | 缓解 |
|------|------|------|
| 旧链路触发时无 QcRecord | `sumPassedCount` 返回 0，`initialStock` 为 0 | 修复前存量数据不受影响；修复后从 0 开始统计是正确行为 |
| `linked_procurement_id` 有值但无 Mapping 记录 | 分配进度显示为空 | V42 回填保证存量数据迁移；新链路写入 Mapping 时幂等检查 |
| V42 回填与新 Mapping 创建并发 | 重复 Mapping | Mapping 表加唯一约束 `(demand_id, procurement_id)`，重复插入时 DB 报错阻止 |
| QcRecordCompletedEvent 发布后旧 QC 记录 | 事件发布前已有的 COMPLETED 记录不触发 listener | 一次性批量发布历史 COMPLETED 事件，或由 Mapping 状态推进替代 |
| `QcRecordCompletedEvent` 发布后，`ProcurementQcPassedEventListener` 立即被触发 | Phase2 状态被意外推进 | 监听器已有状态前置检查，不在 `已出货`/`検品`/`現地検品` 状态的直接跳过 |

### INTJ-7. 手术优先级（v1.5.0 最终版）

| 优先级 | 手术 | 熵减 | 基因 |
|--------|------|------|------|
| P0 | `QcRecordUseCase` 注入 ApplicationEventPublisher + 发布 QcRecordCompletedEvent | -3.0 | EV-106, EV-112 |
| P0 | `LogisticsPlanUseCase` 注入 ApplicationEventPublisher + 发布 LogisticsPlanInTransitEvent | -2.0 | EV-121 |
| P0 | `QcRecordUseCase.update()` 改用 `entity.updateStatus()` + result==PASS 才推进 COMPLETED | -2.0 | EV-113, EV-119 |
| P0 | `ProcurementQcPassedEventListener` 触发条件增加 `已出货` | -1.0 | EV-128 |
| P0 | `JapanCustomsClearedEventListener.initialStock` 通过 ShipmentBatch 链路聚合 passedCount | -2.0 | EV-102 |
| P0 | `ProcurementQueryPort` + `SalesLowStockEventListener` 改用 Port（EV-122） | -1.5 | EV-122 |
| P0 | `LogisticsInTransitEventListener` 改用 `CustomsQueryPort`（EV-123） | -1.0 | EV-123 |
| P0 | `QcQueryPort` + `ShipmentBatchQueryPort` + `MappingStatusAutoUpdater` 全部通过 Port 隔离 | -2.5 | EV-107, EV-120 |
| P1 | `ShipmentBatch` + `DemandProcurementMapping` Entity + Repository + V40/V41/V43 | -2.0 | EV-101 |
| P1 | `StepStatusCalculator` 从 Assembler 提取（CC=13→≤8） | -2.0 | EV-105, EV-114 |
| P1 | `DomesticCustomsStatus.isTerminal()` + VO status 改枚举类型 | -1.5 | EV-115, EV-116 |
| P1 | `ProcurementAssembler` 移除 qcRecordId 依赖，改用 batchCount | -1.5 | EV-103 |
| P1 | `ProcurementQcPassedEventListener` 增加 `已出货` 触发 | -1.0 | EV-103 |
| P1 | v2 聚合视图替代单值锚点 | -1.0 | EV-109 |
| P2 | `LogisticsPlanUseCase` 从 QcRecord 聚合 quantity | -0.5 | EV-108 |
| P2 | `OrderOverviewUseCase` 消除重复 factory 查询 | -0.5 | EV-117 |
| P2 | SPEC 文档同步（B01/B02/B09） | -0.5 | EV-109 |
| P2 | `QcRecordAssembler.toDto()` 移除跨模块 IO，下沉至 UseCase（EV-124） | -1.0 | EV-124 |
| P2 | `QcRecordUseCase.generateQcCode()` 改用数据库序列（EV-125） | -0.5 | EV-125 |
| P3 | `Procurement.updateStatus()` 加 `@Version` 乐观锁 | -0.5 | EV-104 |
| P3 | `ProcurementPage.vue` toggle 语义与后端 FSM 对齐（EV-126） | -0.5 | EV-126 |

**预期最终熵密度**：0.3（仅剩 v2 视图同步延迟 + 历史 COMPLETED 事件补偿风险）
