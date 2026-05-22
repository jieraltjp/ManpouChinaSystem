# SPEC-B13 老厂家免验功能设计

> 状态：**实现完成**
> 日期：2026-05-22
> Flyway：V23（`V23__factory_needs_qc.sql`）

---

## 1. 业务背景

部分长期稳定合作的老厂家不需要每次发注都安排验货。需求：

1. **工厂层面**：增加「是否验货」字段（默认需要验货）
2. **自动跳过验货阶段**：创建出货批次时，若工厂不需要验货，自动生成一条免验验货记录
3. **验货类型**：新增「老厂家免验」类型，前端展示，状态直接终态

---

## 2. 现有模型分析

### 2.1 Factory 实体

| 字段 | 类型 | 说明 |
|------|------|------|
| `factoryCode` | String | 工厂编号 |
| `factoryName` | String | 工厂名称 |
| `cooperationStatus` | CooperationStatus | 合作状态 |

**无验货控制字段**，需要新增。

### 2.2 QcType 枚举（现有）

```java
public enum QcType {
    ONSITE,   // 検品 — 仓库验货
    REMOTE    // 現地検品 — 现场异地验货
}
```

需要新增 `EXEMPT`（老厂家免验）。

### 2.3 QcStatus 枚举（现有）

```java
public enum QcStatus {
    PENDING,           // 待验货
    COMPLETED,         // 已完成（终态）
    RETURN_REQUESTED;  // 发起退货
}
```

免验记录直接 `COMPLETED`，FSM 已是终态，无需修改。

### 2.4 QcResult 枚举（现有）

```java
public enum QcResult {
    PASS,   // 合格
    FAIL    // 不合格
}
```

免验记录结果为 `PASS`，无需修改。

### 2.5 QcRecordCreateCmd（现有）

`shipmentBatchId` 为必填；免验记录不填写验货员、验货日期、检品数等字段。

---

## 3. 设计方案

### 3.1 数据库变更（Flyway V48）

```sql
ALTER TABLE factory
  ADD COLUMN needs_qc TINYINT(1) NOT NULL DEFAULT 1
  COMMENT '是否需要验货：1=需要，0=免验（老厂家）'
  AFTER cooperation_status;
```

- 默认 `1`（需要验货），向后兼容
- 已创建的工厂不受影响

### 3.2 Factory 实体变更

```java
@Column(name = "needs_qc", nullable = false)
private Boolean needsQc = true;  // 是否需要验货
```

### 3.3 QcType 枚举变更

```java
public enum QcType {
    ONSITE,   // 検品 — 仓库验货
    REMOTE,   // 現地検品 — 现场异地验货
    EXEMPT    // 老厂家免验
}
```

### 3.4 免验 QcRecord 字段策略

| 字段 | 值 | 原因 |
|------|-----|------|
| `qcType` | `EXEMPT` | 标识类型 |
| `result` | `PASS` | 免验=合格 |
| `status` | `COMPLETED` | 终态，无需验货流程 |
| `qcUserId` | `null` | 无验货员 |
| `qcDate` | `null` | 无验货日期 |
| `inspectionCount` | `null` | 不检品 |
| `passedCount` | `null` | 不检品 |
| `remarks` | `"老厂家免验"` | 默认备注 |

### 3.5 自动创建逻辑

**触发点**：`ShipmentBatchUseCase.create()` 保存出货批次后

**前置条件**：工厂 `needsQc = false`

**实现方式**：事件驱动，解耦 `ShipmentBatchUseCase` 与 `QcRecordUseCase`

```
ShipmentBatchUseCase.create()
    → 保存 ShipmentBatch
    → 发布事件: ShipmentBatchCreatedEvent(batchId, procurementId, factoryId)
    → [异步] QcRecordAutoExemptListener 接收事件
        → 查询 Factory.needsQc
        → 若 needsQc=false，创建免验 QcRecord（status=COMPLETED, qcType=EXEMPT, result=PASS）
```

**替代方案（同步，注入 QcRecordRepository）**：不推荐，ShipmentBatchUseCase 不应直接依赖 QcRecordRepository。

### 3.6 事件设计

```java
// 领域事件
public record ShipmentBatchCreatedEvent(
    Long batchId,
    Long procurementId,
    Long factoryId
) {}
```

### 3.7 监听器设计

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class QcRecordAutoExemptListener {

    private final FactoryRepository factoryRepository;
    private final QcRecordRepository qcRecordRepository;
    private final QcRecordAssembler qcRecordAssembler;

    @Async  // 异步，不阻塞出货批次创建
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipmentBatchCreated(ShipmentBatchCreatedEvent event) {
        // 1. 查询工厂 needsQc
        // 2. 若 needsQc=false，构建 QcRecordCreateCmd，qcType=EXEMPT, status=COMPLETED
        // 3. qcRecordRepository.save(entity)
    }
}
```

---

## 4. 前端变更

### 4.1 工厂管理页

- 工厂创建/编辑表单增加「是否验货」开关（默认开启）
- i18n: `factory.needsQc` / `factory.needsQc.exempt`

### 4.2 验货记录页

- 验货类型列：展示「検品/現地検品/老厂家免验」（i18n）
- 免验记录只读，不可编辑

---

## 5. API 变更

| 接口 | 变更 |
|------|------|
| `POST /api/factories` | 增加 `needsQc` 字段 |
| `PUT /api/factories/{id}` | 增加 `needsQc` 字段 |
| `GET /api/factories/{id}` | 返回 `needsQc` 字段 |
| `POST /api/shipment-batches` | 无需变更（事件自动触发） |
| `GET /api/qc-records` | 无需变更（免验记录同普通记录查询） |

---

## 6. 变更范围汇总

| 层 | 文件 | 变更 |
|----|------|------|
| DB | `V48__factory_needs_qc.sql` | 新增 needs_qc 列 |
| Domain | `QcType.java` | 增加 EXEMPT |
| Domain | `Factory.java` | 增加 needsQc 字段 |
| Domain | `ShipmentBatchCreatedEvent.java` | 新增事件类 |
| Application | `QcRecordAutoExemptListener.java` | 新增监听器 |
| Application | `FactoryAssembler.java` | 增加 needsQc 映射 |
| Application | `QcRecordAssembler.java` | 增加 EXEMPT 场景映射 |
| Frontend | `FactoryPage.vue` | 表单增加开关 |
| Frontend | i18n | 增加 `factory.needsQc` 相关 key |

---

## 7. 风险与注意事项

1. **向后兼容**：已有工厂 `needs_qc` 默认为 `1`（需要验货），行为不变
2. **事务边界**：事件在 `@TransactionalEventListener(phase = AFTER_COMMIT)` 后触发，确保出货批次已提交
3. **幂等性**：同一条出货批次只触发一次免验创建（加唯一约束 `uk_qc_exempt_batch`）
4. **验货类型变更**：已有工厂改为免验后，已创建的 `PENDING` 验货记录仍需人工处理，不自动生成免验记录
5. **前端列表**：免验记录在验货记录列表中可见，需标注颜色区分
