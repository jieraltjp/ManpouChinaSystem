# SPEC-B03 — 验货记录业务规格（步骤3）

> **版本**: 1.2.0
> **更新**: 2026-05-08（v1.2.0：Entity补shipmentBatchId字段）
> **更新**: 2026-05-07（v1.1.2：注⚠️QcRecord.taxRefund字段Entity仍存在，与文档v1.1.1不一致）
> **更新**: 2026-04-27（v1.1.1：移除 QcRecord.taxRefund，退税由采购层 billingType 决定）
> **更新**: 2026-04-27（v1.1.0：新增验货完成后自动推进采购单状态；新增选择采购后代入字段自动预填）
> **更新**: 2026-04-23（补充元数据字段）
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **业务步号**: 03（验货记录）
> **对应 UI 文档**: `docs/ui/pages/03-inspection.md`
> **对应数据库文档**: `docs/database/DB-03-inspection.md`
> **前置**: 步骤2（Procurement 已下单）
> **后续**: 步骤4（LogisticsPlan 调配计划）

---

## 1. 业务背景

货物到仓后进行验货，记录检品数、合格数、不良数。验货结果 PASS → 进入调配（步骤4）；FAIL → `status = RETURN_REQUESTED`（退货流程，QcRecord 进入终态）。

> FAIL 后退货处理完成后，工厂重新发货，需由验货员新建另一条 QcRecord 重新验货，原 QcRecord 不再变更。

**关键设计**：QcRecord 从内嵌值对象升级为独立聚合根，可独立查询和追踪。

**v1.1.1 字段移除**：`taxRefund`（是否退税）已从 QcRecord 移除。退税决策属于采购层，由 `Procurement.billingType`（报关类型）决定。

> ⚠️ **v1.1.2 补充**：Entity `QcRecord.java` 仍含 `taxRefund` 字段（DB列未DROP），与文档v1.1.1不一致。此为代码与文档的差距，待决策是否从Entity中移除。

**v1.1.0 新增自动行为**：
- 创建验货记录时，若 `sellerName` 未填，自动从关联 Procurement.factoryId → Factory.factoryName 代入（K-03 规则）
- 验货通过（COMPLETED）后，自动推进关联 Procurement 状态（`ProcurementQcPassedEventListener`）

---

## 2. 聚合根

### QcRecord（验货记录）

```
QcRecord（聚合根）
├── id: Long
├── qcCode: String              # Q-YYYYMMDD-NNN
├── procurementId: Long          # 关联采购单（FK）
├── shipmentBatchId: Long        # 关联出货批次（V43新增，JPA只读，DB管理）
├── sellerName: String          # 卖家名称（来自 Factory.factoryName）
├── productCode: String          # 货号
├── subProductCode: String      # 子货号
├── qcUserId: Long             # 验货负责人
├── qcType: QcType             # ONSITE(仓库验货) / REMOTE(现场异地验货)
├── qcDate: LocalDate          # 验货日期
├── result: QcResult          # PASS / FAIL
├── status: QcStatus          # PENDING → COMPLETED / RETURN_REQUESTED
├── inspectionCount: Integer    # 检品数
├── passedCount: Integer      # 合格数量
├── defectiveCount: Integer    # 不良数量（自动计算）
├── boxCount: Integer          # 箱数
├── boxLengthCm: BigDecimal    # 箱子长(cm)
├── boxWidthCm: BigDecimal     # 箱子宽(cm)
├── boxHeightCm: BigDecimal    # 箱子高(cm)
├── netWeightPerUnit: BigDecimal # 单个净重(kg)
├── grossWeight: BigDecimal    # 毛重(kg)
├── taxInclusivePrice: BigDecimal # 含税价（元）
├── material: String          # 材质
├── qcStandard: String       # 验收标准
├── remarks: String           # 备注
├── images: String            # 缺陷照片URL列表（JSON数组）
├── destination: String       # 目的地
├── quantity: Integer        # 订购数量
├── orderDate: LocalDate     # 下单日
└── 领域方法
    ├── calculateDefectiveCount()  # 不良数 = inspectionCount - passedCount
    └── updateStatus(QcStatus)      # 状态推进（含FSM校验）
```

---

## 3. 枚举

```java
public enum QcType {
    ONSITE,   // 検品 — 仓库验货
    REMOTE    // 現地検品 — 现场异地验货
}

public enum QcResult {
    PASS,  // 验货通过
    FAIL   // 验货不通过
}

public enum QcStatus {
    PENDING,          // 待验货
    COMPLETED,        // 已完成（终态）
    RETURN_REQUESTED; // 发起退货（终态）

    public boolean isTerminal() { return this == COMPLETED || this == RETURN_REQUESTED; }

    public boolean canTransitionTo(QcStatus target) {
        if (this == target) return true;
        return transitions().getOrDefault(this, Set.of()).contains(target);
    }

    private static Map<QcStatus, Set<QcStatus>> transitions() {
        return Map.of(
            PENDING,          Set.of(PENDING, COMPLETED, RETURN_REQUESTED),
            COMPLETED,        Set.of(),
            RETURN_REQUESTED, Set.of()
        );
    }
}
```

---

## 4. 状态流转

```
  PENDING ──[PASS]──▶ COMPLETED [终态]
     │
     └──[FAIL]──▶ RETURN_REQUESTED [终态]
```

---

## 5. 自动计算

| 字段 | 公式 | 实现位置 |
|------|------|---------|
| defectiveCount | `inspectionCount - passedCount` | `QcRecord.calculateDefectiveCount()` |
| result | `defectiveCount == 0 ? PASS : FAIL` | 由验货员手动判断 |
| sellerName | 未填时自动代入 Factory.factoryName | `QcRecordUseCase.create()` → K-03 规则 |

## 5.1 验货完成后自动推进采购单

| 事件 | 触发条件 | 副作用 |
|------|---------|--------|
| `QcRecordCompletedEvent` | 验货记录 `status = COMPLETED` 时发布 | `ProcurementQcPassedEventListener` 自动推进关联 Procurement 状态 |

**状态推进规则（SPEC-B02 §5）：**

| QC 类型 | 条件 | Procurement 新状态 |
|---------|------|------------------|
| 現地検品(REMOTE) | — | メーカー直送 |
| 倉庫検品(ONSITE) | 体积 ≤ 0.5m³ | エア便 |
| 倉庫検品(ONSITE) | 体积 > 0.5m³ | 輸出 |

> 仅当 Procurement 当前状态处于「検品」或「現地検品」时才推进（幂等）。

---

## 6. API 设计

```
GET    /api/v1/qc-records?page=&pageSize=&procurementId=&productCode=&result=&status=
GET    /api/v1/qc-records/{id}
POST   /api/v1/qc-records
PATCH  /api/v1/qc-records/{id}
DELETE /api/v1/qc-records/{id}
```

---

## 7. 代码实现状态

- [x] ✅ `QcRecord` 聚合根实体（含 `calculateDefectiveCount()`）
- [x] ✅ `QcStatus` 枚举（含 `isTerminal()` + `canTransitionTo()` + FSM map）
- [x] ✅ `QcResult` 枚举
- [x] ✅ `QcType` 枚举
- [x] ✅ `QcRecordRepository` 领域接口 + JPA 适配器
- [x] ✅ `QcRecordUseCase` 用例服务
- [x] ✅ `QcRecordAssembler` DTO 转换器
- [x] ✅ `QcRecordController` REST 控制器
- [x] ✅ `QcRecordUseCaseTest` 单元测试（10个用例，全部通过）
- [x] ✅ `@/api/inspection.ts` 前端 API 客户端
- [x] ✅ `QcRecordPage.vue` 页面（已对接真实 API）
- [x] ✅ `ProcurementQcPassedEventListener` 验货完成后自动推进采购单状态
- [x] ✅ `QcRecordCompletedEvent` 领域事件
