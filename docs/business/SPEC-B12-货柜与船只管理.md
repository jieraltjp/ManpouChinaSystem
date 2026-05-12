# SPEC-B12 — 货柜与船只管理

> **版本**: v1.2.0
> **日期**: 2026-05-12
> **状态**: 设计中
> **对应 UI**: UI-19（货柜管理）· UI-20（船只管理）
> **对应 DB**: DB-04 v1.5.0（container 已有）· DB-14（ship 表 + container 扩展，待 V18）
> **对应 Flyway**: V18（ship + container 扩展）
> **对应前端**: `ShipManagementPage.vue` · `ContainerManagementPage.vue`（扩展）

---

## 1. 业务背景与现状问题

### 现状

现有 `container` 表（`logistics` 模块）已管理货柜基础信息，但存在三个根本性缺陷：

| 缺陷 | 说明 |
|------|------|
| **船只数据缺失** | 没有 `ship` / `vessel` 表，无法记录船名、船号 |
| **柜-船关联缺失** | `container` 表无 `ship_id` 外键，货柜与船只之间无法建立关联 |
| **物流信息不完整** | 缺少到岗地点、时间段、备注等字段 |

### 业务场景

货柜生命周期：
```
货柜创建（待配船） → 分配船只（已装柜） → 离港 → 到港
```

当前痛点：做好了的柜子，无法关联到具体船只，导致物流信息断层。

---

## 2. 数据模型

### 2.1 字段归属分析

| 字段 | 类型 | 归属 | 说明 |
|------|------|------|------|
| `id` | BIGINT PK | — | 主键（审计字段） |
| `container_no` | VARCHAR(32) | 固有属性 | 货柜号（船公司提供），唯一 |
| `container_type` | ENUM | 固有属性 | GP20 / GP40 / HC40 / HC45（已有） |
| `total_cbm` | DECIMAL | 固有属性 | 已装载总体积（已有） |
| `total_weight_kg` | DECIMAL | 固有属性 | 已装载总重量（已有） |
| `plan_count` | INT | 固有属性 | 关联计划数（已有） |
| `pool_id` | BIGINT FK | 固有属性 | 拼柜池（已有） |
| `status` | ENUM | 状态机 | CREATED → LOADED → DEPARTED → ARRIVED（已有） |
| `load_date` | DATE | 物流属性 | 装柜日期（已有） |
| `departure_date` | DATE | 物流属性 | 离港日期（已有） |
| `arrival_date` | DATE | 物流属性 | 到港日期（已有） |
| `ship_id` | BIGINT FK | 物流属性 | 关联船只（新增） |
| `time_slot` | VARCHAR(32) | 物流属性 | 时间段，如"2026-W24"（新增） |
| `arrival_location` | VARCHAR(128) | 物流属性 | 到岗地点/最终送达地址（新增） |
| `remarks` | VARCHAR(512) | 物流属性 | 备注（新增） |
| `create_by` | VARCHAR(64) | 审计字段 | 创建人（已有） |
| `create_time` | DATETIME | 审计字段 | 创建时间（已有） |
| `update_by` | VARCHAR(64) | 审计字段 | 更新人（已有） |
| `update_time` | DATETIME | 审计字段 | 更新时间（已有） |
| `is_deleted` | TINYINT(1) | 审计字段 | 软删除（已有） |

> **说明**：所有审计字段均遵循 V15 规范（`create_by/create_time/update_by/update_time/is_deleted TINYINT(1)`），与现有 BaseEntity 保持一致。

### 2.2 新建 `ship` 表

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

### 2.3 扩展 `container` 表（ALTER）

```sql
ALTER TABLE container
    ADD COLUMN ship_id           BIGINT        COMMENT '关联船只ID FK → ship.id',
    ADD COLUMN time_slot         VARCHAR(32)   COMMENT '时间段，如 2026-W24',
    ADD COLUMN arrival_location  VARCHAR(128)  COMMENT '到岗地点/最终送达地址',
    ADD COLUMN remarks           VARCHAR(512)   COMMENT '备注',
    ADD INDEX idx_container_ship_id (ship_id);

ALTER TABLE container
    ADD CONSTRAINT fk_container_ship FOREIGN KEY (ship_id) REFERENCES ship(id);
```

### 2.4 实体关系

```
ship (1) ←—— (N) container
     │
     ├── id
     ├── ship_name
     ├── ship_number  ← 唯一约束 uk_ship_number
     ├── carrier
     ├── departure_port
     ├── arrival_port
     ├── create_by / create_time
     ├── update_by / update_time
     └── is_deleted

container
     ├── id
     ├── container_no  ← 唯一约束 uk_container_no
     ├── container_type
     ├── status (CREATED→LOADED→DEPARTED→ARRIVED)
     ├── load_date
     ├── departure_date
     ├── arrival_date
     ├── total_cbm
     ├── total_weight_kg
     ├── plan_count
     ├── pool_id
     ├── ship_id  ──────────────→ ship.id（N:1）
     ├── time_slot
     ├── arrival_location
     ├── remarks
     ├── create_by / create_time
     ├── update_by / update_time
     └── is_deleted
```

---

## 3. 货柜状态机

```
CREATED ──→ LOADED ──→ DEPARTED ──→ ARRIVED
  │           │            │            │
  │           │            │            │
 ①          ②           ③            ④
```

| 状态 | 触发时机 | 说明 |
|------|----------|------|
| `CREATED` | 货柜记录创建时 | 初始状态，柜子待分配船只 |
| `LOADED` | `ship_id` 被赋值且 `load_date` 已填 | 柜子已装上船 |
| `DEPARTED` | `departure_date` 已填 | 船只已离港 |
| `ARRIVED` | `arrival_date` 已填 | 船只已到港 |

**反向不可逆**：状态只能向前推进，不能回退。

---

## 4. 货柜与船只关系

- **关系类型**：N:1（多个货柜归属同一艘船，一艘船可装多个货柜）
- **关联时机**：货柜独立创建（CREATED），后续分配船只后变为 LOADED
- **解绑处理**：ship_id 设为 NULL → 状态回退至 CREATED（需确认业务是否允许）
- **查询入口**：
  - 从船查柜：`GET /api/v1/ships/{id}/containers`
  - 从柜查船：`GET /api/v1/containers/{id}` → 返回关联 ship 信息
- **级联删除保护**：`ship` 表删除前必须确认无关联货柜（ship_id IS NULL）

---

## 5. API 设计

### 5.1 船只管理（Ship CRUD）

| 方法 | 路径 | @PreAuthorize | 说明 |
|------|------|:--------------:|------|
| `GET` | `/api/v1/ships` | `ship:read` | 分页查询船只 |
| `GET` | `/api/v1/ships/{id}` | `ship:read` | 船只详情 |
| `POST` | `/api/v1/ships` | `ship:create` | 创建船只 |
| `PUT` | `/api/v1/ships/{id}` | `ship:update` | 编辑船只 |
| `DELETE` | `/api/v1/ships/{id}` | `ship:delete` | 删除船只（需无关联货柜） |
| `GET` | `/api/v1/ships/{id}/containers` | `ship:read` | 查询某船所有货柜 |

### 5.2 货柜管理（Container 扩展）

| 方法 | 路径 | @PreAuthorize | 说明 |
|------|------|:-------------:|------|
| `GET` | `/api/v1/containers` | `container:read` | 分页查询货柜（含 ship 信息） |
| `GET` | `/api/v1/containers/{id}` | `container:read` | 货柜详情（含船名/船号） |
| `POST` | `/api/v1/containers` | `container:create` | 创建货柜 |
| `PUT` | `/api/v1/containers/{id}` | `container:update` | 编辑货柜 |
| `DELETE` | `/api/v1/containers/{id}` | `container:delete` | 删除货柜 |
| `PUT` | `/api/v1/containers/{id}/assign-ship` | `container:update` | 分配船只 |
| `PUT` | `/api/v1/containers/{id}/unassign-ship` | `container:update` | 解除船只关联 |

### 5.3 请求/响应 VO 概要

**ShipCreateCmd**
```typescript
{
  shipName: string        // 船名（必填）
  shipNumber: string     // 船号（必填，唯一）
  carrier?: string       // 船公司
  departurePort?: string // 出发港
  arrivalPort?: string  // 目的港
}
```

**ShipVO（响应）**
```typescript
{
  id: number
  shipName: string
  shipNumber: string
  carrier?: string
  departurePort?: string
  arrivalPort?: string
  createBy: string
  createTime: string
  updateTime: string
}
```

**ContainerUpdateCmd（扩展字段）**
```typescript
{
  // 固有字段（已有）
  containerNo?: string
  containerType?: string
  // 物流字段（新增/扩展）
  shipId?: number       // 关联船只（null = 解除关联）
  timeSlot?: string     // 时间段
  arrivalLocation?: string // 到岗地点
  remarks?: string      // 备注
  loadDate?: string
  departureDate?: string
  arrivalDate?: string
}
```

**AssignShipCmd**
```typescript
{
  shipId: number        // 必填
  loadDate?: string     // 装柜日期（可选）
}
```

---

## 6. 前端 UI 草图

### 6.1 入口

```
基础数据（Base）
  ├── 商品目录
  ├── 工厂管理
  ├── 职务管理
  ├── 组织管理
  ├── 船只管理        ← 新增（UI-20）
  └── 货柜管理        ← 扩展（UI-19 v2.0）
```

### 6.2 船只管理页（ShipManagementPage.vue）

```
┌──────────────────────────────────────────────────────────────┐
│ 船只管理                                    [+ 新增船只]    │
├──────────────────────────────────────────────────────────────┤
│ 船名: [___]  船号: [___]  目的港: [___]   [查询] [重置]  │
├──────────────────────────────────────────────────────────────┤
│ # │ 船名   │ 船号     │ 船公司 │ 出发港 │ 目的港 │ 操作   │
│ 1 │ 日章丸 │ V2026A   │ …     │ 上海   │ 東京   │ [编辑] │
│ 2 │ 大洋号 │ V2026B   │ …     │ 深圳   │ 大阪   │ [编辑] │
└──────────────────────────────────────────────────────────────┘
```

### 6.3 货柜管理页（ContainerManagementPage.vue）

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 货柜管理                                      [+ 新增货柜]               │
├──────────────────────────────────────────────────────────────────────────┤
│ 货柜号: [___]  状态: [全部▼]  船只: [全部▼]  [查询] [重置]           │
├──────────────────────────────────────────────────────────────────────────┤
│ # │ 货柜号    │ 类型 │ 体积m³ │ 重量kg │ 状态    │ 船名  │ 船号 │ 操作│
│ 1 │ TEMU001  │ GP20 │ 65.3   │ 12500  │ 已离港  │ 日章丸│V2026A│[详情]│
│ 2 │ TEMU002  │ GP40 │ 70.1   │ 18000  │ 待配船  │  —    │  —   │[详情]│
│ 3 │ TEMU003  │ HC40 │ 68.0   │ 15200  │ 已到港  │ 大洋号│V2026B│[详情]│
└──────────────────────────────────────────────────────────────────────────┘
```

### 6.4 货柜详情抽屉

```
┌──────────────────────────────────────────────┐
│ 货柜详情                          [关闭]    │
├──────────────────────────────────────────────┤
│ 货柜号           TEMU001                   │
│ 货柜类型         GP20                       │
│ 总体积(m³)       65.3                       │
│ 总重量(kg)       12500                      │
│ 状态             已离港                       │
├──────────────────────────────────────────────┤
│ ▼ 物流信息（关联船只）                       │
│ 船名             日章丸                      │
│ 船号             V2026A                     │
│ 出发港           上海港                      │
│ 目的港           東京港                      │
│ 装柜日期         2026-05-01                │
│ 离港日期         2026-05-03                │
│ 到港日期         —                          │
│ 时间段           2026-W19                   │
│ 到岗地点         东京仓库A                   │
│ 备注             需加急清关                  │
└──────────────────────────────────────────────┘
```

---

## 7. 权限编码

| 编码 | 说明 | ADMIN | MANAGER | OPERATOR | VIEWER |
|------|------|:-----:|:-------:|:--------:|:------:|
| `ship:read` | 查看船只 | ✅ | ✅ | ✅ | ✅ |
| `ship:create` | 创建船只 | ✅ | ✅ | ❌ | ❌ |
| `ship:update` | 编辑船只 | ✅ | ✅ | ❌ | ❌ |
| `ship:delete` | 删除船只 | ✅ | ❌ | ❌ | ❌ |

> 遵循 SPEC-B11 §4 权限编码规范。`container:read/create/update/delete`（ID 29-32）已存在于 V15 baseline，本模块不新增。ship 模块新增 **4 条**权限，V18 seed 使用 ID 115-118。

---

## 8. 操作日志记录规则

### 8.1 触发时机

| 动作 | 触发时机 | detail 内容 |
|------|----------|------------|
| `CREATE` | POST 成功（2xx） | `{ "newData": { shipName, shipNumber, carrier, departurePort, arrivalPort } }` |
| `UPDATE` | PUT/PATCH 成功（2xx） | `{ "oldData": {...}, "newData": {...}, "changedFields": ["shipName"] }` |
| `DELETE` | DELETE 成功（2xx） | `{ "oldData": { id, shipName, shipNumber } }` |
| `ASSIGN_SHIP` | assign-ship 接口成功 | `{ "containerId": 1, "containerNo": "TEMU001", "shipId": 1, "shipName": "日章丸", "shipNumber": "V2026A" }` |
| `UNASSIGN_SHIP` | unassign-ship 接口成功 | `{ "containerId": 1, "containerNo": "TEMU001" }` |

### 8.2 不记录的内容

- GET 请求（查询类）
- `is_deleted`、`update_time`、`update_by` 等审计字段变更

### 8.3 实现方式

遵循 SPEC-B11 §6 规范，使用 `@AuditLog` 自定义注解：

```java
@AuditLog(module = "ship", action = "CREATE")
@PostMapping
public Result<ShipVO> create(@Valid @RequestBody ShipCreateCmd cmd) { ... }

@AuditLog(module = "ship", action = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable Long id) { ... }

@AuditLog(module = "container", action = "ASSIGN_SHIP")
@PutMapping("/{id}/assign-ship")
public Result<Void> assignShip(@PathVariable Long id, @RequestBody @Valid AssignShipCmd cmd) { ... }
```

---

## 9. 实施计划

> **Flyway 版本**：V18__ship_and_container_extension.sql
> **生产激活**：`SPRING_PROFILES_ACTIVE=production`

### Phase 1 — 数据库 + 船只 CRUD

1. 新增 Flyway `V18__ship_and_container_extension.sql`
   - 创建 `ship` 表（含审计字段 + 索引）
   - 扩展 `container` 表（ship_id FK + time_slot + arrival_location + remarks）
2. 新建 `Ship` Entity（继承 BaseEntity） + `ShipRepository`
3. 新建 `ShipService` + `ShipController`（CRUD + /ships/{id}/containers）
4. 扩展 `ContainerService` + `ContainerController`（新增字段 + assign-ship/unassign-ship）
5. `@PreAuthorize` 注解（8 个端点）
6. `@AuditLog` 注解（ship CRUD 4 接口 + container assign/unassign 2 接口）
7. 编写单元测试

### Phase 2 — 前端船只管理

1. 新增 `ship.ts` API 客户端（shipApi）
2. 新增 `ShipManagementPage.vue`
3. 菜单注册（`base` 分组，与 FactoryPage 同级）
4. i18n 同步（zh.json / ja.json）：`ship.*` key
5. 权限守卫（router 路由守卫 + hasPermission 按钮控制）

### Phase 3 — 前端货柜管理（v2.0）

1. 扩展 `ContainerPage.vue`（已有）：
   - 列表新增"船只"筛选 + "船名/船号"列
   - 编辑弹窗新增 shipId（下拉选船只）+ timeSlot + arrivalLocation + remarks
   - 详情抽屉新增物流信息区块（船名/船号/时间段/到岗地点/备注）
   - 新增"分配船只"快捷按钮
2. i18n 同步：`logistics.container.*` + 新增 key
3. 权限守卫

---

## 10. 技术债务

| 项目 | 说明 | 优先级 |
|------|------|--------|
| `consolidation_pool` 与 `container.ship_id` 共存 | 拼柜池模式与 ship 模式是否二选一，需业务确认 | P1 |
| 历史货柜数据补录 | 现有 container 记录的 ship_id 等字段需批量回填 | P2 |
| 状态机自动化推进 | load_date / departure_date / arrival_date 填写时自动推进状态 | P2 |
| `consolidation_pool_item` Entity | DB-04 标注为"不存在"，需确认是否仍在使用 | P2 |
