# DB-04 — 调配计划数据库设计

> **版本**: 1.8.0
> **创建**: 2026-04-22
> **更新**: 2026-05-12（v1.8.0：ConsolidationPoolItem 已废弃；实际设计采用 LogisticsPlan.poolId FK + planCount 聚合计数器）
> **状态**: ✅ 已实现
> **业务步号**: 04（调配计划）
> **对应业务文档**: `SPEC-B00-全链路总览.md` · `SPEC-B04-调配计划-步骤4.md`
> **对应 UI 文档**: `docs/ui/pages/04-logistics.md`
> **对应后端聚合根**: `LogisticsPlan`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `logistics_plan` | LogisticsPlan | ✅ 已实现 |
| 2 | `container` | Container | ✅ 已实现 |
| 3 | `consolidation_pool` | ConsolidationPool | ✅ 已实现 |
| 4 | `consolidation_pool_item` | ConsolidationPoolItem | ❌ 未实现（设计变更：采用 LogisticsPlan.poolId FK 代替显式关联表） |

---

## 1. logistics_plan（调配计划）

**对应**: `LogisticsPlan` 聚合根

```sql
CREATE TABLE logistics_plan (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_code          VARCHAR(32)  NOT NULL UNIQUE COMMENT '调配编号 L-YYYYMMDD-NNN',
    qc_record_id       BIGINT COMMENT '关联验货记录 FK → qc_record.id（调配锚点，v1.2.0新增）',
    procurement_id      BIGINT COMMENT '关联采购单 FK → procurement.id（拼柜时可为空，保留兼容）',
    factory_id         BIGINT COMMENT '关联工厂 FK → factory.id',
    product_code       VARCHAR(32)  NOT NULL COMMENT '货号',
    sub_product_code   VARCHAR(64) COMMENT '子货号/颜色',
    plan_type          VARCHAR(20)  NOT NULL COMMENT 'SEA(海运) / AIR(空运) / CONSOLIDATION(拼柜)',
    status             VARCHAR(24)  NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED / BOOKED / IN_TRANSIT / DELIVERED',
    cargo_length_cm    DECIMAL(8,2) COMMENT '货物长(cm)',
    cargo_width_cm     DECIMAL(8,2) COMMENT '货物宽(cm)',
    cargo_height_cm    DECIMAL(8,2) COMMENT '货物高(cm)',
    cargo_volume_cbm   DECIMAL(10,6) COMMENT '体积(m³) = 长×宽×高 / 1,000,000',
    cargo_weight_kg    DECIMAL(10,4) COMMENT '重量(kg)',
    quantity           INT COMMENT '数量',
    requires_qc       BOOLEAN COMMENT '是否需要检测',
    container_no      VARCHAR(32) COMMENT '货柜号（船公司提供，同批次货物填入相同货柜号，v1.3.0新增）',
    container_id      BIGINT COMMENT '货柜ID（装柜后赋值，关联 container 表）',
    pool_id            BIGINT COMMENT '拼柜池ID',
    estimated_ship_date DATE COMMENT '预计发货日',
    actual_ship_date   DATE COMMENT '实际发货日',
    remarks            VARCHAR(512) COMMENT '备注',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by          VARCHAR(64)  NOT NULL,
    update_by          VARCHAR(64)  NOT NULL,
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_plan_code (plan_code),
    INDEX idx_logistics_qc_record (qc_record_id),
    INDEX idx_logistics_procurement (procurement_id),
    INDEX idx_logistics_status (status),
    INDEX idx_logistics_plan_type (plan_type),
    INDEX idx_logistics_factory (factory_id),
    INDEX idx_lp_product_code (product_code),
    INDEX idx_lp_container_no (container_no),           -- v1.3.0
    INDEX idx_lp_estimated_ship_date (estimated_ship_date),
    INDEX idx_lp_create_time (create_time),
    INDEX idx_lp_is_deleted (is_deleted)
);
```

---

## 2. container（货柜）✅已实现 → ⚠️ v2.0 待扩展（见 DB-14）

> ⚠️ 本节仅记录 v1.5.0 已有字段。v2.0 扩展字段（ship_id / time_slot / arrival_location / remarks）见 `DB-14-container-ship.md`。

**对应**: `Container` 聚合根（v1.5.0，V46）

```sql
CREATE TABLE container (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    container_no    VARCHAR(32)  NOT NULL UNIQUE COMMENT '货柜号（如 TEMU1234567）',
    container_type  VARCHAR(24)  NOT NULL DEFAULT 'GP20' COMMENT 'GP20|GP40|HC40|HC45 (@Enumerated(EnumType.STRING))',
    total_cbm       DECIMAL(10, 4) DEFAULT 0 COMMENT '已装载总体积(m³)',
    total_weight_kg DECIMAL(12, 4) DEFAULT 0 COMMENT '已装载总重量(kg)',
    plan_count      INT DEFAULT 0 COMMENT '关联计划数',
    pool_id         BIGINT COMMENT '关联拼柜池ID',
    status          VARCHAR(24) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED|LOADED|DEPARTED|ARRIVED',
    load_date       DATE COMMENT '装柜日期',
    departure_date  DATE COMMENT '离港日期',
    arrival_date    DATE COMMENT '到港日期',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    update_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
```

> ⚠️ Entity中不存在：`container_code`（无此字段）、`seal_no`、`departure_port`、`arrival_port`、`estimated_*` 日期字段

---

## 3. consolidation_pool（拼柜池）✅已实现

**对应**: `ConsolidationPool` 聚合根（v1.5.0，V45）

```sql
CREATE TABLE consolidation_pool (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_code       VARCHAR(32) NOT NULL UNIQUE COMMENT '池编号，如 CP-20260430-001',
    destination_port VARCHAR(64) NOT NULL COMMENT '目的港（如 Tokyo, Yokohama）',
    total_cbm       DECIMAL(12, 4) DEFAULT 0 COMMENT '当前总体积(m³)',
    total_weight_kg DECIMAL(12, 4) DEFAULT 0 COMMENT '当前总重量(kg)',
    plan_count      INT DEFAULT 0 COMMENT '关联计划数',
    container_threshold_cbm DECIMAL(10, 4) DEFAULT 70 COMMENT '触发装柜的体积阈值(m³)',
    status          VARCHAR(24) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN|PENDING|LOADED|SHIPPED',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    update_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
```

> ⚠️ Entity中不存在：`departure_port`、`arrival_port`、`plan_type`、`container_id`（containerId字段）、`remarks`

### 3.1 consolidation_pool_item（已废弃设计）

> ⚠️ **v1.7.0 设计变更**：原计划使用 `consolidation_pool_item` 显式关联表，**实际实现**采用 `LogisticsPlan.pool_id` FK + `planCount` 聚合计数器，**无需此表**。

实际关联关系：
```
consolidation_pool
  └── poolId → LogisticsPlan.pool_id（软外键，planCount 记录总数）
```

- `ConsolidationPoolUseCase.addPlan()` 设置 `plan.poolId = poolId`
- `ConsolidationPoolUseCase.removePlan()` 设置 `plan.poolId = null`
- 无需 `consolidation_pool_item` 中间表

---

## 代码实现状态

- [x] ✅ `LogisticsPlan` 聚合根实体（含 `calculateVolume()`，v1.2.0 新增 `qcRecordId`，v1.3.0 新增 `containerNo`）
- [x] ✅ `LogisticsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()` + FSM map）
- [x] ✅ `PlanType` 枚举
- [x] ✅ `LogisticsPlanRepository` 领域接口（v1.2.0 新增 `findByQcRecordIdAndDeletedIsFalse`）
- [x] ✅ `LogisticsPlanJpaRepository` JPA 适配器
- [x] ✅ `LogisticsPlanUseCase` 用例服务（v1.2.0 校验 qcRecord 存在且 result=PASS）
- [x] ✅ `LogisticsPlanController` REST 控制器
- [x] ✅ `LogisticsPlanAssembler` DTO 转换器（v1.2.0 新增 qcRecordId 映射）
- [x] ✅ `LogisticsPlanUseCaseTest` 单元测试（12 个用例，全部通过）
- [x] ✅ `@/api/logistics.ts` 前端 API 客户端（v1.2.0 qcRecordId 类型）
- [x] ✅ `LogisticsPlanPage.vue` 页面（v1.2.0 验货记录下拉替代采购单下拉）
- [x] ✅ `Container` 聚合根实体（V46，字段：containerNo/containerType/totalCbm/totalWeightKg/planCount/poolId/status/loadDate/departureDate/arrivalDate）
- [x] ✅ `ConsolidationPool` 聚合根实体（V45，字段：poolCode/destinationPort/totalCbm/totalWeightKg/planCount/containerThresholdCbm/status）
- [x] ✅ `ConsolidationPoolUseCase.addPlan/removePlan`（通过 LogisticsPlan.poolId 软关联）
- [ ] 🔴 `consolidation_pool_item` 表已废弃（采用 LogisticsPlan.poolId FK 代替显式关联表）
