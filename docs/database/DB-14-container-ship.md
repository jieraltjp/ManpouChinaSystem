# DB-14 — 货柜与船只数据库设计

> **版本**: v1.2.0
> **创建**: 2026-05-12
> **更新**: 2026-05-14（v1.2.0：表清单 ship/container 状态修正 ✅；SPEC-B12 Phase 1/2/3 完成）
> **状态**: ✅ 已完成（对应 SPEC-B12 · 对应 Flyway V18/V19）
> **业务步号**: B-12
> **对应业务文档**: `SPEC-B12-货柜与船只管理.md`
> **对应 UI 文档**: `docs/ui/pages/19-container.md` · `docs/ui/pages/20-ship.md`

---

## 变更历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0.0 | 2026-05-12 | 初稿：ship 表 + container 扩展字段 |

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `ship` | Ship | ✅ 已完成（V18，SPEC-B12 Phase 1） |
| 2 | `container`（扩展） | Container | ✅ 已完成（V18，SPEC-B12 Phase 2） |

---

## 1. ship（船只信息表）

### 1.1 DDL

```sql
CREATE TABLE ship (
    id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
    ship_name       VARCHAR(64)    NOT NULL COMMENT '船名',
    ship_number     VARCHAR(32)    NOT NULL COMMENT '船号/航次号',
    carrier         VARCHAR(64)    COMMENT '船公司',
    departure_port  VARCHAR(64)    COMMENT '出发港',
    arrival_port    VARCHAR(64)    COMMENT '目的港',
    create_by       VARCHAR(64)    NOT NULL DEFAULT 'SYSTEM' COMMENT '创建人',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       VARCHAR(64)    NOT NULL DEFAULT 'SYSTEM' COMMENT '更新人',
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '软删除（0=正常，1=已删除）',
    UNIQUE KEY uk_ship_number (ship_number),
    INDEX idx_ship_arrival_port (arrival_port),
    INDEX idx_ship_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='船只信息表';
```

### 1.2 字段说明

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `ship_name` | VARCHAR(64) | NOT NULL | 船名 |
| `ship_number` | VARCHAR(32) | NOT NULL, UNIQUE | 船号/航次号，如 "V2026A" |
| `carrier` | VARCHAR(64) | — | 船公司 |
| `departure_port` | VARCHAR(64) | — | 出发港 |
| `arrival_port` | VARCHAR(64) | — | 目的港 |
| `create_by` | VARCHAR(64) | NOT NULL, DEFAULT 'SYSTEM' | 创建人（审计字段，V15 规范） |
| `create_time` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间（审计字段） |
| `update_by` | VARCHAR(64) | NOT NULL, DEFAULT 'SYSTEM' | 更新人（审计字段） |
| `update_time` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | TINYINT(1) | NOT NULL, DEFAULT 0 | 软删除（V15 规范） |

### 1.3 JPA Entity 要点

```java
@Entity
@Table(name = "ship")
public class Ship extends BaseEntity {   // 继承审计字段
    @Column(name = "ship_name", nullable = false, length = 64)
    private String shipName;

    @Column(name = "ship_number", nullable = false, unique = true, length = 32)
    private String shipNumber;

    @Column(name = "carrier", length = 64)
    private String carrier;

    @Column(name = "departure_port", length = 64)
    private String departurePort;

    @Column(name = "arrival_port", length = 64)
    private String arrivalPort;
}
```

### 1.4 约束说明

| 约束 | 说明 |
|------|------|
| `uk_ship_number` UNIQUE | 船号唯一，避免同一航次重复录入 |
| `idx_ship_arrival_port` | 目的港筛选（船只管理页常用） |
| `idx_ship_is_deleted` | 软删除过滤（所有查询须加 is_deleted = 0） |

---

## 2. container（扩展字段）

> 本表已在 DB-04 v1.5.0 定义，以下仅列出 **v2.0 扩展的新增字段**。

### 2.1 扩展 DDL（ALTER）

```sql
-- 新增船只关联字段
ALTER TABLE container
    ADD COLUMN ship_id           BIGINT        COMMENT '关联船只ID FK → ship.id';

-- 新增物流扩展字段
ALTER TABLE container
    ADD COLUMN time_slot         VARCHAR(32)   COMMENT '时间段，如 2026-W24',
    ADD COLUMN arrival_location  VARCHAR(128)  COMMENT '到岗地点/最终送达地址',
    ADD COLUMN remarks           VARCHAR(512)   COMMENT '备注';

-- 外键约束（ship 表创建后方可执行）
ALTER TABLE container
    ADD CONSTRAINT fk_container_ship FOREIGN KEY (ship_id) REFERENCES ship(id);

-- 索引
ALTER TABLE container
    ADD INDEX idx_container_ship_id (ship_id);
```

> **执行顺序**：必须先创建 `ship` 表，再执行 container 的 ALTER + FK 约束。

### 2.2 新增字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `ship_id` | BIGINT FK | 关联船只，NULL = 待配船 |
| `time_slot` | VARCHAR(32) | 时间段，如 "2026-W24"，用于批次管理 |
| `arrival_location` | VARCHAR(128) | 到岗地点/最终送达地址，清关后填写 |
| `remarks` | VARCHAR(512) | 备注，可记录特殊处理要求 |

### 2.3 JPA Entity 扩展要点

```java
@Entity
@Table(name = "container")
public class Container extends BaseEntity {
    // 已有字段省略...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ship_id")
    private Ship ship;

    @Column(name = "time_slot", length = 32)
    private String timeSlot;

    @Column(name = "arrival_location", length = 128)
    private String arrivalLocation;

    @Column(name = "remarks", length = 512)
    private String remarks;

    // 状态推进逻辑（由 Service 层实现）
    // loadDate != null → status = LOADED
    // departureDate != null → status = DEPARTED
    // arrivalDate != null → status = ARRIVED
}
```

---

## 3. Flyway 迁移脚本

**文件**：`V18__ship_and_container_extension.sql`

```sql
-- V18__ship_and_container_extension.sql
-- 对应 SPEC-B12 / DB-14
-- 注意：V17 已被 japan_customs_update_permission 占用，本迁移从 V18 开始

-- Step 1: 创建 ship 表
CREATE TABLE ship (
    id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
    ship_name       VARCHAR(64)    NOT NULL COMMENT '船名',
    ship_number     VARCHAR(32)    NOT NULL COMMENT '船号/航次号',
    carrier         VARCHAR(64)    COMMENT '船公司',
    departure_port  VARCHAR(64)    COMMENT '出发港',
    arrival_port    VARCHAR(64)    COMMENT '目的港',
    create_by       VARCHAR(64)    NOT NULL DEFAULT 'SYSTEM' COMMENT '创建人',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       VARCHAR(64)    NOT NULL DEFAULT 'SYSTEM' COMMENT '更新人',
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '软删除',
    UNIQUE KEY uk_ship_number (ship_number),
    INDEX idx_ship_arrival_port (arrival_port),
    INDEX idx_ship_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='船只信息表';

-- Step 2: 扩展 container 表
ALTER TABLE container
    ADD COLUMN ship_id          BIGINT        COMMENT '关联船只ID FK → ship.id',
    ADD COLUMN time_slot        VARCHAR(32)   COMMENT '时间段',
    ADD COLUMN arrival_location VARCHAR(128)  COMMENT '到岗地点',
    ADD COLUMN remarks          VARCHAR(512)   COMMENT '备注',
    ADD INDEX idx_container_ship_id (ship_id);

-- Step 3: 外键（ship 表存在后才能添加）
ALTER TABLE container
    ADD CONSTRAINT fk_container_ship FOREIGN KEY (ship_id) REFERENCES ship(id);

-- Step 4: V18 seed 新增 ship 权限（4条，ID 115~118）
-- 注意：container:read/create/update/delete（ID 29-32）已存在于 V15 baseline，不重复新增
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES
    (115, 'ship:read',    '查看船只', '船舶を表示', 'ship', 'READ',    115, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (116, 'ship:create',  '创建船只', '船舶を作成', 'ship', 'CREATE',  116, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (117, 'ship:update',  '编辑船只', '船舶を編集', 'ship', 'UPDATE',  117, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (118, 'ship:delete',  '删除船只', '船舶を削除', 'ship', 'DELETE',  118, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM');

-- Step 5: 角色-权限关联（ship CRUD 分配给 ADMIN/MANAGER/OPERATOR/VIEWER，container 已在 V15 分配）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM (SELECT id FROM role WHERE code IN ('ADMIN','MANAGER','OPERATOR','VIEWER')) r
CROSS JOIN (SELECT id FROM permission WHERE code LIKE 'ship:%') p;
```

---

## 4. 代码实现状态

| 组件 | 状态 | 说明 |
|------|------|------|
| `Ship` Entity | ✅ 已完成 | 继承 BaseEntity |
| `ShipRepository` | ✅ 已完成 | 继承 JpaRepository + JpaSpecificationExecutor |
| `Container` Entity 扩展 | ✅ 已完成 | 新增 ship / timeSlot / arrivalLocation / remarks 字段 |
| `ContainerRepository` 扩展 | ✅ 已完成 | 新增 findByShipId() |
| `ShipService` | ✅ 已完成 | CRUD + assign/unassign |
| `ShipController` | ✅ 已完成 | REST API |
| `ContainerService` 扩展 | ✅ 已完成 | assignShip / unassignShip |
| `@PreAuthorize` 注解 | ✅ 已完成 | 8 个端点 |
| `@AuditLog` 注解 | ✅ 已完成 | 6 个写操作 |
