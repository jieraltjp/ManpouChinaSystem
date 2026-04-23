# SPEC-B04 — 调配计划业务规格（步骤4）

> **版本**: 1.1.0
> **创建**: 2026-04-22
> **更新**: 2026-04-22 — 同步状态（无代码变更）
> **状态**: ✅ 已实现（LogisticsPlan）；⚡占位（Container/ConsolidationPool）
> **业务步号**: 04（调配计划）
> **对应 UI 文档**: `docs/ui/pages/04-logistics.md`
> **对应数据库文档**: `docs/database/DB-04-logistics.md`
> **前置**: 步骤3（QcRecord.result = PASS）
> **后续**: 步骤5（DomesticCustomsRecord 国内报关）

---

## 1. 业务背景

验货通过后，安排货物的运输方式（海运/空运/拼柜），生成调配记录。调配类型决定了后续报关和运输的流程差异。

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
├── procurementId: Long           # 关联采购单（拼柜时可为空）
├── factoryId: Long              # 关联工厂
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

### Container（货柜）⚡占位

```
Container（聚合根）
├── id: Long
├── containerCode: String         # C-YYYYMMDD-NNN
├── containerNo: String          # 货柜号
├── containerType: ContainerType  # 20GP / 40GP / 40HC / 45HC
├── sealNo: String               # 封条号
├── departurePort: String        # 起运港
├── arrivalPort: String         # 目的港
├── status: ContainerStatus     # LOADING → DEPARTED → ARRIVED → DELIVERED
└── 领域方法
    └── closeLoading()          # 装柜完成
```

### ConsolidationPool（拼柜池）⚡占位

```
ConsolidationPool（聚合根）
├── id: Long
├── poolCode: String             # P-YYYYMMDD-NNN
├── destination: String         # 目的港
├── totalWeight: BigDecimal    # 总重量(kg)
├── totalVolume: BigDecimal    # 总体积(m³)
├── containerId: Long          # 实际货柜（装柜后赋值）
├── status: PoolStatus         # POOL_PENDING → POOL_READY → LOADED
└── 领域方法
    ├── add(procurementId)      # 加入拼柜
    ├── remove(procurementId)   # 移出
    └── consolidate(containerId) # 触发装柜
```

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

### Container ⚡占位

```
  LOADING ──[开船]──▶ DEPARTED ──[到港]──▶ ARRIVED ──[送达]──▶ DELIVERED
```

---

## 5. 自动触发规则

- **LogisticsPlan.status = IN_TRANSIT** → 自动创建 DomesticCustomsRecord（步骤5）
- **检品合格后** → 可创建 LogisticsPlan

---

## 6. API 设计

```
# LogisticsPlan
GET    /api/v1/logistics-plans?page=&pageSize=&planType=&productCode=&status=
GET    /api/v1/logistics-plans/{id}
POST   /api/v1/logistics-plans
PATCH  /api/v1/logistics-plans/{id}
DELETE /api/v1/logistics-plans/{id}

# Container ⚡占位
GET    /api/v1/containers?page=&pageSize=&status=
POST   /api/v1/containers
PATCH  /api/v1/containers/{id}

# ConsolidationPool ⚡占位
GET    /api/v1/consolidation-pools?page=&pageSize=&status=
POST   /api/v1/consolidation-pools
POST   /api/v1/consolidation-pools/{poolId}/items   # 添加货物
DELETE /api/v1/consolidation-pools/{poolId}/items/{itemId}
```

---

## 7. 代码实现状态

- [x] ✅ `LogisticsPlan` 聚合根实体（含 `calculateVolume()`）
- [x] ✅ `LogisticsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()` + FSM map）
- [x] ✅ `PlanType` 枚举
- [x] ✅ `LogisticsPlanRepository` 领域接口 + JPA 适配器
- [x] ✅ `LogisticsPlanUseCase` 用例服务
- [x] ✅ `LogisticsPlanAssembler` DTO 转换器
- [x] ✅ `LogisticsPlanController` REST 控制器
- [x] ✅ `LogisticsPlanUseCaseTest` 单元测试（12个用例，全部通过）
- [x] ✅ `@/api/logistics.ts` 前端 API 客户端
- [x] ✅ `LogisticsPage.vue` 页面（已对接真实 API）
- [ ] 🔴 `Container` 聚合根实体
- [ ] 🔴 `ConsolidationPool` 聚合根实体
- [ ] 🔴 `ConsolidationPoolItem` 聚合根实体
