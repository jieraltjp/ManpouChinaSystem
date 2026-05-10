# SPEC-B04 — 调配计划业务规格（步骤4）

> **版本**: 1.7.0
> **创建**: 2026-04-22
> **更新**: 2026-05-11（v1.7.0：修正ContainerType枚举值 GP20/GP40/HC40/HC45（代码vs doc）；DB-04同步）
> **更新**: 2026-05-07（v1.5.0：ConsolidationPool字段名修正；Container.containerCode缺失注⚠️）
> **状态**: ✅ 已实现（LogisticsPlan + Container + ConsolidationPool）
> **业务步号**: 04（调配计划）
> **对应 UI 文档**: `docs/ui/pages/04-logistics.md`
> **对应数据库文档**: `docs/database/DB-04-logistics.md`
> **前置**: 步骤3（QcRecord.result = PASS）
> **后续**: 步骤5（DomesticCustomsRecord 国内报关）

---

## 1. 业务背景

验货通过后，安排货物的运输方式（海运/空运/拼柜），生成调配记录。调配类型决定了后续报关和运输的流程差异。

**业务锚点变更（v1.2.0）**：
> 调配计划关联 **验货记录（QcRecord）**，而非采购单（Procurement）。原因：验完货才知道实际装箱尺寸（长×宽×高）和毛重，用于判断 SEA/AIR 路径和订舱计算。采购单仅有计划数量，无实际测量数据。

**三种调配路径**：
- `SEA`：体积/重量超标，优先海运
- `AIR`：轻量货物，走空运
- `CONSOLIDATION`：多张小单合并入同一货柜，共享舱位

---

## 2. 聚合根

### LogisticsPlan（调配计划）

```
LogisticsPlan（聚合根）
├── id: Long
├── planCode: String             # L-YYYYMMDD-NNN
├── qcRecordId: Long              # 关联验货记录（QC通过后才知道实际装箱尺寸，用于订舱）
├── procurementId: Long           # 关联采购单（拼柜场景可空；正常流程由 qcRecordId 反查）
├── factoryId: Long              # 关联工厂（由 qcRecord.procurement.factoryId 代入）
├── productCode: String          # 货号
├── subProductCode: String       # 子货号/颜色
├── planType: PlanType           # SEA / AIR / CONSOLIDATION
├── status: LogisticsStatus      # PLANNED → BOOKED → IN_TRANSIT → DELIVERED
├── cargoLengthCm: BigDecimal   # 长(cm)
├── cargoWidthCm: BigDecimal    # 宽(cm)
├── cargoHeightCm: BigDecimal   # 高(cm)
├── cargoVolumeCbm: BigDecimal  # 体积(m³) = 长×宽×高 / 1,000,000
├── cargoWeightKg: BigDecimal   # 重量(kg)
├── quantity: Integer           # 数量
├── requiresQc: Boolean         # 是否需要检测
├── containerId: Long           # 货柜ID（装柜后赋值）
├── poolId: Long               # 拼柜池ID
├── estimatedShipDate: LocalDate
├── actualShipDate: LocalDate
├── remarks: String
└── 领域方法
    ├── calculateVolume()       # 体积计算
    └── updateStatus(LogisticsStatus)  # 状态推进（含FSM校验）
```

### Container（货柜）

```
Container（聚合根）
├── id: Long
├── containerNo: String            # 货柜号（TEMU1234567，UNIQUE）
├── containerType: ContainerType   # GP20 / GP40 / HC40 / HC45（@Enumerated(EnumType.STRING)）
├── totalCbm: BigDecimal          # 总体积（m³）
├── totalWeightKg: BigDecimal      # 总重量（kg）
├── planCount: Integer            # 关联计划数
├── poolId: Long                 # 关联拼柜池
├── status: ContainerStatus      # CREATED / LOADED / DEPARTED / ARRIVED
├── loadDate: LocalDate          # 装柜日期
├── departureDate: LocalDate     # 离港日期
├── arrivalDate: LocalDate       # 到港日期
└── 领域方法
    ├── addPlan(volumeCbm, weightKg)  # 新增关联计划（累加体积/重量）
    └── advanceStatus(newStatus)      # 状态推进（含FSM校验）
```

> ⚠️ Entity中不存在：`sealNo`（封条号）、`departurePort`（起运港）、`arrivalPort`（目的港）、`closeLoading()` — 均未实现

### ConsolidationPool（拼柜池）

```
ConsolidationPool（聚合根）
├── id: Long
├── poolCode: String             # CP-YYYYMMDD-NNN（前缀CP-）
├── destinationPort: String      # 目的港
├── totalCbm: BigDecimal        # 总体积（m³）
├── totalWeightKg: BigDecimal   # 总重量（kg）
├── planCount: Integer          # 关联计划数
├── containerThresholdCbm: BigDecimal  # 触发装柜阈值（默认70m³）
├── status: ConsolidationPoolStatus  # OPEN / PENDING / LOADED / SHIPPED
└── 领域方法
    ├── addPlan(volumeCbm, weightKg)    # 加入拼柜（累加体积/重量）
    ├── removePlan(volumeCbm, weightKg) # 移出拼柜（扣除体积/重量）
    ├── isReadyToLoad()                 # 总体积 >= 阈值
    ├── closeToPending()                # 封池（OPEN → PENDING）
    ├── markLoaded()                    # 装柜完成（PENDING → LOADED）
    ├── markShipped()                   # 出港（LOADED → SHIPPED）
    └── advanceStatus(newStatus)         # 状态推进（含FSM校验）
```

> ⚠️ Entity中不存在：`containerId`（装柜后赋值字段）、`add(qcRecordId)`、`remove(qcRecordId)`、`consolidate(containerId)`

---

## 3. 枚举

```java
public enum PlanType {
    SEA,             // 海运
    AIR,             // 空运
    CONSOLIDATION    // 拼柜
}

public enum LogisticsStatus {
    PLANNED,    // 调配计划已创建
    BOOKED,     // 舱位已预订
    IN_TRANSIT, // 运输中
    DELIVERED;  // 已送达（终态）

    public boolean isTerminal() { return this == DELIVERED; }

    public boolean canTransitionTo(LogisticsStatus target) {
        if (this == target) return true;
        return transitions().getOrDefault(this, Set.of()).contains(target);
    }

    private static Map<LogisticsStatus, Set<LogisticsStatus>> transitions() {
        return Map.of(
            PLANNED,    Set.of(PLANNED, BOOKED),
            BOOKED,     Set.of(BOOKED, IN_TRANSIT),
            IN_TRANSIT, Set.of(IN_TRANSIT, DELIVERED),
            DELIVERED,  Set.of()
        );
    }
}
```

---

## 4. 状态流转

### LogisticsPlan

```
  PLANNED ──[订舱]──▶ BOOKED ──[实际发货]──▶ IN_TRANSIT ──[到货]──▶ DELIVERED [终态]
```

### Container

```
  CREATED ──[装柜完成]──▶ LOADED ──[开船]──▶ DEPARTED ──[到港]──▶ ARRIVED
```

---

## 5. 触发规则（v1.3.0 修正）

**不自动触发，由用户在 LogisticsPlanPage 手动发起报关。**

> 原因：1货柜 = N 个 LogisticsPlan = N 个 DomesticCustomsRecord（按商品+工厂分组），自动触发无法正确处理多计划场景。

**操作路径**：
```
LogisticsPlanPage → 选中某货柜号下的计划 → 点击「创建报关」
    → 跳转 /procurement/domestic-customs?containerNo=XXX
    → DomesticCustomsPage 自动填入货柜号，用户按商品+工厂分别创建报关单
```

- **检品合格后** → 可创建 LogisticsPlan

---

## 6. API 设计

```
# LogisticsPlan
GET    /api/v1/logistics-plans?page=&pageSize=&planType=&productCode=&status=&qcRecordId=
GET    /api/v1/logistics-plans/{id}
POST   /api/v1/logistics-plans
PATCH  /api/v1/logistics-plans/{id}
DELETE /api/v1/logistics-plans/{id}

# Container
GET    /api/v1/containers?page=&pageSize=&status=
POST   /api/v1/containers
PATCH  /api/v1/containers/{id}

# ConsolidationPool
GET    /api/v1/consolidation-pools?page=&pageSize=&status=
POST   /api/v1/consolidation-pools
POST   /api/v1/consolidation-pools/{poolId}/items   # 添加货物
DELETE /api/v1/consolidation-pools/{poolId}/items/{itemId}
```

---

## 7. 代码实现状态

- [x] ✅ `LogisticsPlan` 聚合根实体（含 `calculateVolume()`，v1.2.0 新增 `qcRecordId`）
- [x] ✅ `LogisticsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()` + FSM map）
- [x] ✅ `PlanType` 枚举
- [x] ✅ `LogisticsPlanRepository` 领域接口 + JPA 适配器（v1.2.0 新增 `findByQcRecordId`）
- [x] ✅ `LogisticsPlanUseCase` 用例服务（v1.2.0 校验 qcRecord 存在且 result=PASS）
- [x] ✅ `LogisticsPlanAssembler` DTO 转换器（v1.2.0 新增 qcRecordId 映射）
- [x] ✅ `LogisticsPlanController` REST 控制器
- [x] ✅ `LogisticsPlanUseCaseTest` 单元测试（12个用例，全部通过）
- [x] ✅ `@/api/logistics.ts` 前端 API 客户端（v1.2.0 qcRecordId 类型）
- [x] ✅ `LogisticsPlanPage.vue` 页面（v1.2.0 验货记录下拉替代采购单下拉）
- [x] ✅ `Container` 聚合根实体（含 `containerCode` C-YYYYMMDD-NNNN）
- [x] ✅ `ConsolidationPool` 聚合根实体（含 `poolCode` CP-YYYYMMDD-NNN）
- [x] ✅ `ConsolidationPoolItem` 聚合根实体
