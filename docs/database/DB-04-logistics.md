# DB-04 — 调配计划数据库设计

> **版本**: 1.1.0
> **创建**: 2026-04-22
> **更新**: 2026-04-23（v1.1.0：补全缺失索引，与 DB 实际结构对齐）
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
| 2 | `container` | Container | 🔴 占位 |
| 3 | `consolidation_pool` | ConsolidationPool | 🔴 占位 |
| 4 | `consolidation_pool_item` | ConsolidationPoolItem | 🔴 占位 |

---

## 1. logistics_plan（调配计划）

**对应**: `LogisticsPlan` 聚合根

```sql
CREATE TABLE logistics_plan (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_code          VARCHAR(32)  NOT NULL UNIQUE COMMENT '调配编号 L-YYYYMMDD-NNN',
    procurement_id      BIGINT COMMENT '关联采购单 FK → procurement.id（拼柜时可为空）',
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
    container_id       BIGINT COMMENT '货柜ID（装柜后赋值）',
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
    INDEX idx_logistics_procurement (procurement_id),
    INDEX idx_logistics_status (status),
    INDEX idx_logistics_plan_type (plan_type),
    INDEX idx_logistics_factory (factory_id),
    INDEX idx_lp_product_code (product_code),
    INDEX idx_lp_estimated_ship_date (estimated_ship_date),
    INDEX idx_lp_create_time (create_time),
    INDEX idx_lp_is_deleted (is_deleted)
);
```

---

## 2. container（货柜）🔴占位

**对应**: `Container` 聚合根（待实现）

```sql
-- TODO: 字段待确认
-- 预期字段: container_code / container_no / container_type / seal_no /
--           departure_port / arrival_port / estimated_departure_date /
--           actual_departure_date / status / remarks
CREATE TABLE container (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    container_code     VARCHAR(32)  NOT NULL UNIQUE COMMENT 'C-YYYYMMDD-NNN',
    container_no       VARCHAR(32)  NOT NULL COMMENT '货柜号（船公司提供）',
    container_type     VARCHAR(10)  NOT NULL COMMENT '20GP / 40GP / 40HC / 45HC',
    seal_no            VARCHAR(32) COMMENT '封条号',
    departure_port     VARCHAR(64) COMMENT '起运港',
    arrival_port       VARCHAR(64) COMMENT '目的港',
    estimated_departure_date DATE COMMENT '预计开船日',
    actual_departure_date   DATE COMMENT '实际开船日',
    estimated_arrival_date  DATE COMMENT '预计到港日',
    actual_arrival_date     DATE COMMENT '实际到港日',
    status             VARCHAR(20) NOT NULL DEFAULT 'LOADING' COMMENT 'LOADING / DEPARTED / ARRIVED / DELIVERED',
    remarks            VARCHAR(512),
    create_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by          VARCHAR(64) NOT NULL,
    update_by          VARCHAR(64) NOT NULL,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE
);
```

---

## 3. consolidation_pool（拼柜池）🔴占位

**对应**: `ConsolidationPool` 聚合根（待实现）

```sql
-- TODO: 字段待确认
CREATE TABLE consolidation_pool (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_code             VARCHAR(32) NOT NULL UNIQUE COMMENT 'P-YYYYMMDD-NNN',
    destination            VARCHAR(128) NOT NULL COMMENT '目的地（同一目的港的货物合并）',
    departure_port        VARCHAR(64) COMMENT '起运港',
    arrival_port          VARCHAR(64) COMMENT '目的港',
    total_weight          DECIMAL(12,4) COMMENT '总重量(kg) = SUM',
    total_volume          DECIMAL(12,6) COMMENT '总体积(m³) = SUM',
    plan_type             VARCHAR(20) COMMENT '默认 CONSOLIDATION',
    container_id          BIGINT COMMENT '分配货柜ID',
    status                VARCHAR(20) NOT NULL DEFAULT 'POOL_PENDING' COMMENT 'POOL_PENDING / POOL_READY / LOADED / DEPARTED',
    remarks               VARCHAR(512),
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by             VARCHAR(64) NOT NULL,
    update_by             VARCHAR(64) NOT NULL,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE
);
```

---

## 代码实现状态

- [x] ✅ `LogisticsPlan` 聚合根实体（含 `calculateVolume()`）
- [x] ✅ `LogisticsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()` + FSM map）
- [x] ✅ `PlanType` 枚举
- [x] ✅ `LogisticsPlanRepository` 领域接口
- [x] ✅ `LogisticsPlanJpaRepository` JPA 适配器
- [x] ✅ `LogisticsPlanUseCase` 用例服务
- [x] ✅ `LogisticsPlanController` REST 控制器
- [x] ✅ `LogisticsPlanAssembler` DTO 转换器
- [x] ✅ `LogisticsPlanUseCaseTest` 单元测试（12 个用例，全部通过）
- [x] ✅ `@/api/logistics.ts` 前端 API 客户端
- [x] ✅ `LogisticsPage.vue` 页面（已对接真实 API）
- [ ] 🔴 `Container` 聚合根实体
- [ ] 🔴 `ConsolidationPool` 聚合根实体
- [ ] 🔴 `ConsolidationPoolItem` 聚合根实体
