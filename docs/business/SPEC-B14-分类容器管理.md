# SPEC-B14 — 分类容器管理（分类容器管理）

> **版本**: v1.1.0
> **日期**: 2026-05-24
> **状态**: ✅ Phase 1 实施完成
> **对应数据库**: list7（原始表，来自 `sddb0040100537`，共 3685 条记录）
> **对应 Flyway**: V27（新增字段 + 数据迁移，保留原始 ID 1:1）
> **对应前端**: `pages/procurement/ContainerPage.vue`（已扩展）

---

## 1. 业务背景与现状问题

### 1.1 原始数据来源

`list7` 来自外部系统 `sddb0040100537`，字段结构如下：

| 列名 | 类型 | 含义 | 示例 |
|------|------|------|------|
| `ID` | INT | 主键（**需 1:1 保留原始 ID**） | 6, 7, 8 … 3685 |
| `container_no` | VARCHAR | **批次号**（对单用，非 ISO 货柜号） | `21008`, `test`, `发` |
| `ship_name` | VARCHAR | 船名（原始文本，非 FK） | `SITC YOKOHAMA V.2112E` |
| `ship_number` | VARCHAR | 航次号（原始文本，非 FK） | `2112E`, `V.2112E` |
| `cabinet_no` | VARCHAR | **箱号**（ISO 标准格式） | `CBHU4225619`, `TCNU4520650` |
| `cabinet_time` | DATE | 装柜日期 | `2021-03-10` |
| `arrive_time` | DATE | 预计到港日期 | `2021-03-10`, `0000-00-00` |
| `period` | VARCHAR | 到港时段 | `凌晨0-6`, `早上6-12`, `下午12-18`, `晚上18-24` |
| `status` | VARCHAR | 出运状态 | `未出`, `出完`, `""`, `<待定>` |
| `arrival_address` | VARCHAR | 目的港/送达地 | `福冈`, `名古屋` |
| `memo` | VARCHAR | 备注 | `地方`, `00` |
| `updater` | VARCHAR | 最后更新人 | `張峻瑜`, `殷元` |
| `updatetime` | DATETIME | 最后更新时间 | `2021-03-10 00:00:00` |
| `showFlag` | INT | 显示标志（0=归档, 1=活跃） | 0, 1 |
| `rireki` | LONGTEXT | 历史记录（JSON） | null |

### 1.2 关键发现：container_no 是批次号，不是货柜号

从 list7 数据分析，同一 `container_no` 出现多次，每次 `cabinet_no` 不同：

```
ID=7:  container_no=21008, cabinet_no=CBHU4225619  ← 批次21008，第1箱
ID=19: container_no=21008, cabinet_no=CBHU4225619  ← 批次21008，第2箱（不同 record，cabinet_time 不同）
ID=22: container_no=21008, cabinet_no=CBHU4225619  ← 批次21008，第3箱
```

- `container_no` = **批次号**（对单用，用于报关/物流计划关联）
- `cabinet_no` = **箱号**（ISO 货柜标准格式，如 CBHU4225619）
- 每条 record = 一次装运记录（一个箱子上了一条船）

**重要区分**：

| 概念 | list7 字段 | 现有 Container 字段 | 说明 |
|------|-----------|-------------------|------|
| 批次/对单号 | `container_no` | `container_no` | **语义相同**（但现有 Container 名称叫"货柜号"，实际存批次号）|
| 物理箱号 | `cabinet_no` | — | **缺失**，需新增 |
| 船次/航次 | `ship_name+ship_number` | `ship_id` | list7 是文本，现有是 FK |
| 到港时段 | `period` | `time_slot`（存周次）| 含义不同，需新增字段 |

---

## 2. 两套容器体系的区分

### 2.1 体系 A：系统内调配体系（已有）

```
LogisticsPlan
    ├── planCode = "L-20260421-001"
    ├── containerId → Container.id（调配计划加入的货柜）
    ├── containerNo = "21008"（批次号，迁入自 list7）
    │
    ▼
Container（logistics 模块，SPEC-B00/B12）
    ├── containerNo = "C-20260501-0001"（系统生成码）
    ├── containerType = GP20
    ├── status = CREATED/LOADED/DEPARTED/ARRIVED（FSM）
    ├── shipId → Ship.id（分配船只后）
    │
    ▼
DomesticCustomsRecord
    └── containerNo = "21008"（批次号，用于报关）

JapanCustomsRecord
    └── containerNo = "21008"（批次号，用于日本报关）
```

### 2.2 体系 B：list7 分类容器体系（待迁入）

```
list7（每条 record = 一个箱子上了一条船）
    ├── container_no = "21008"（批次号）
    ├── cabinet_no = "CBHU4225619"（ISO 箱号）
    ├── ship_name = "SITC YOKOHAMA V.2112E"
    ├── period = "晚上18-24"
    └── showFlag = 1（活跃）
```

### 2.3 核心问题：两套体系的关键差异

| 维度 | 体系 A（Container）| 体系 B（list7）| 差异 |
|------|-------------------|----------------|------|
| **主键来源** | 自增 ID | list7.ID（需 1:1 保留）| 不同 |
| **container_no 语义** | 系统生成码 | 批次号（对单用）| 语义相似但格式不同 |
| **cabinet_no** | 无此字段 | ISO 箱号 | **缺失** |
| **与 Ship 关联** | `ship_id` FK | 文本 `ship_name/number` | list7 是文本，FK 需要匹配 |
| **时段** | `time_slot`（周次 WXX）| `period`（时段）| **语义完全不同** |
| **显示标志** | 无 | `showFlag` | **缺失** |
| **原始更新时间人** | 无 | `updater/updatetime` | **缺失** |

---

## 3. 合并方案分析："容器合并到船只"

### 3.1 方案对比

| 方案 | 说明 | 优点 | 缺点 |
|------|------|------|------|
| **A：Container 扩展** | 直接在 Container 表加字段，list7 数据迁入 container 表 | 最小改动，不需要新建 Entity | 语义混乱：`container_no` 既是系统货柜号又是批次号 |
| **B：新建 ShipmentRecord Entity** | 创建新实体存 list7 数据，Container 独立 | 语义清晰，数据隔离 | 两套 Entity 并存，前端需维护两套 |
| **C：Container 合并到 Ship（推荐）** | Container 数据归入 Ship 下管理，ShipPage 作为主要入口 | 用户视角一致：船→柜一体化管理 | Container 与 list7 语义不同，强行合并有违概念 |

### 3.2 推荐方案：C（Container 合并到 Ship）

**核心思路**：以 **Ship 为父实体**，在 ShipPage 中统一管理货柜视图。

**变更点**：
1. `/base/container` 路由降级，原有 ContainerPage 保留但隐藏入口（或改为仅管理员可见）
2. ShipPage 增加"货柜/分类容器"Tab，展示该船所有 Container 记录
3. list7 数据作为**历史批次记录**，迁入 Container 表，关联对应 Ship
4. 新增字段（cabinetNo/period/legacyStatus/showFlag/legacyId 等）扩展到 Container 表

### 3.3 两套入口的定位

| 入口 | 路径 | 定位 | 说明 |
|------|------|------|------|
| 船只管理 | `/base/ship` | **主入口** | ShipPage 增加"货柜"Tab，展示该船所有货柜 |
| 货柜管理 | `/base/container` | 辅助入口 | 保留全量 Container 列表，不在菜单显示 |
| 分类容器（历史）| `/base/container-category` | 历史数据 | 展示 list7 迁入的全部记录，按船分组 |

**数据流向**：
```
ShipPage [货柜 Tab]
    │
    ├── Ship (船只基本信息)
    ├── Container（系统调配货柜 + list7 历史货柜）
    │       ├── containerNo = "21008"（批次号，来自 list7）
    │       ├── cabinetNo = "CBHU4225619"（箱号）
    │       ├── period = "晚上18-24"
    │       └── legacyId = 7（list7.ID）
    │
    └── LogisticsPlan（该船关联的调配计划）
```

---

## 4. 数据模型扩展

### 4.1 新增数据库字段（ALTER）

```sql
-- V27: 分类容器管理（SPEC-B14）
-- 新增字段：箱号 / 到港时段 / 原始出运状态 / 显示标志 / 原始ID / 原始更新时间人

ALTER TABLE container
    ADD COLUMN cabinet_no        VARCHAR(16)    COMMENT '箱号（ISO格式，如 CBHU4225619）',
    ADD COLUMN period           VARCHAR(16)    COMMENT '到港时段：凌晨0-6 / 早上6-12 / 下午12-18 / 晚上18-24',
    ADD COLUMN legacy_status   VARCHAR(32)    COMMENT '原始出运状态（未出 / 出完 / 待定 等）',
    ADD COLUMN show_flag       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '显示标志（0=归档，1=活跃）',
    ADD COLUMN legacy_id       BIGINT         COMMENT '原始数据主键（来自 list7.ID，1:1保留）',
    ADD COLUMN legacy_updater  VARCHAR(64)    COMMENT '原始最后更新人',
    ADD COLUMN legacy_updatetime DATETIME(3)  COMMENT '原始最后更新时间',

    ADD INDEX idx_container_cabinet_no (cabinet_no),
    ADD INDEX idx_container_show_flag  (show_flag),
    ADD INDEX idx_container_legacy_id  (legacy_id);
```

### 4.2 Entity 扩展

**`Container.java` 新增字段**

```java
// ===== list7 原始字段映射（SPEC-B14）=====

/**
 * 箱号（ISO 标准格式）。
 * 对应 list7.cabinet_no，如 CBHU4225619。
 * 注意：与 containerNo（批次号/对单号）是不同概念。
 */
@Column(name = "cabinet_no", length = 16)
private String cabinetNo;

/**
 * 到港时段。
 * 对应 list7.period，值：凌晨0-6 / 早上6-12 / 下午12-18 / 晚上18-24。
 * 现有 timeSlot 存周次（WXX 格式），两者语义不同。
 */
@Column(name = "period", length = 16)
private String period;

/**
 * 原始出运状态。
 * 对应 list7.status，原始值：未出 / 出完 / 待定 等。
 * 与现有 ContainerStatus（CREATED→ARRIVED）是不同状态体系，**不合并**。
 */
@Column(name = "legacy_status", length = 32)
private String legacyStatus;

/**
 * 显示标志（0=归档，1=活跃）。
 * 对应 list7.showFlag。
 * 前端筛选默认仅显示活跃记录（show_flag=1）。
 */
@Column(name = "show_flag", nullable = false)
private Boolean showFlag = true;

/**
 * 原始数据主键（1:1 保留）。
 * 对应 list7.ID，用于数据溯源。
 */
@Column(name = "legacy_id")
private Long legacyId;

/**
 * 原始最后更新人。
 * 对应 list7.updater。
 */
@Column(name = "legacy_updater", length = 64)
private String legacyUpdater;

/**
 * 原始最后更新时间。
 * 对应 list7.updatetime。
 */
@Column(name = "legacy_updatetime")
private LocalDateTime legacyUpdatetime;
```

---

## 5. 各字段详细说明

### 5.1 字段对照表

| list7 列 | container 列 | 类型 | 说明 |
|-----------|-------------|------|------|
| `ID` | `legacy_id`（新增）| BIGINT | 原始主键 1:1 保留 |
| `container_no` | `container_no` | VARCHAR(32) | **批次号**（原语义为系统货柜号，迁入后实际存批次号语义） |
| `cabinet_no` | `cabinet_no`（新增）| VARCHAR(16) | **箱号**（ISO 格式，如 CBHU4225619）|
| `ship_name` | `ship_id`（已有）| BIGINT FK | 文本匹配 ship.ship_name → ship.id |
| `ship_number` | `ship_id`（已有）| BIGINT FK | 文本匹配 ship.ship_number |
| `cabinet_time` | `load_date`（已有）| DATE | `0000-00-00` → NULL |
| `arrive_time` | `arrival_date`（已有）| DATE | `0000-00-00` → NULL |
| `period` | `period`（新增）| VARCHAR(16) | 到港时段（凌晨/早上/下午/晚上），与 `timeSlot`（周次）不同 |
| `status` | `legacy_status`（新增）| VARCHAR(32) | 原始出运状态（未出/出完），与 FSM 状态分离 |
| `arrival_address` | `arrival_location`（已有）| VARCHAR(128) | 目的港 |
| `memo` | `remarks`（已有）| VARCHAR(512) | 备注 |
| `showFlag` | `show_flag`（新增）| TINYINT(1) | 0→FALSE，1→TRUE |
| `updater` | `legacy_updater`（新增）| VARCHAR(64) | 原始更新人 |
| `updatetime` | `legacy_updatetime`（新增）| DATETIME(3) | 原始更新时间 |
| — | `id` | BIGINT | 迁入时设为 list7.ID（保留原始主键）|
| — | `container_type` | ENUM | 默认 GP20（list7 无此字段）|
| — | `status` | ENUM | 默认 CREATED（FSM 状态）|
| — | `is_deleted` | TINYINT(1) | 默认 FALSE |

### 5.2 container_no 语义说明（重要）

**现有 Container.container_no** = 系统生成的货柜编码（格式 `C-YYYYMMDD-NNNN`）
**list7.container_no** = 批次号（对单用，如 `21008`）

迁入后，**Container.container_no 实际存 list7 的批次号**。字段名称不变，但：
- 前端 UI 标签改为"**批次号**"（`$t('logistics.container.column.batchNo')`）
- 日志/文档中说明：此字段在历史数据迁入时存批次号
- 新建 Container 记录时，仍可输入任意字符串（无校验约束）

### 5.3 两套状态并存

| 状态类型 | 字段 | 体系 | 说明 |
|---------|------|------|------|
| **FSM 状态** | `status` | 系统内部 | CREATED → LOADED → DEPARTED → ARRIVED |
| **原始出运状态** | `legacyStatus` | list7 原始 | 未出 / 出完 / 待定（空） |

两者**独立存储，互不影响**：
- list7 迁入时：`status = CREATED`（系统默认），`legacyStatus = list7.status`
- 用户操作不改变 `legacyStatus`
- 前端分类容器页面按 `legacyStatus` 分组展示

---

## 6. 受影响的现有代码分析

### 6.1 直接引用 container 的模块

| 模块 | 文件 | 引用方式 | 影响 |
|------|------|---------|------|
| **logistics** | `ContainerUseCase.java` | `Container` Entity CRUD | ✅ 向后兼容，新增字段无影响 |
| **logistics** | `ContainerAssembler.java` | `toDto / toEntity / copyUpdate` | ⚠️ 需扩展字段映射 |
| **logistics** | `ContainerController.java` | REST 接口 | ✅ 自动包含新字段 |
| **logistics** | `ContainerQuery.java` | 查询参数 | ⚠️ 需新增 showFlag/legacyStatus/cabinetNo 筛选 |
| **logistics** | `LogisticsPlan.java` | `containerNo` + `containerId` | ✅ 无影响（两个字段独立）|
| **logistics** | `ContainerUseCase.addPlan()` | 设置 containerId + 同步 containerNo | ✅ 向后兼容 |
| **customs** | `DomesticCustomsRecord.java` | `containerNo` | ✅ 无影响（String 字段不变）|
| **customs** | `JapanCustomsRecord.java` | `containerNo` | ✅ 无影响（String 字段不变）|
| **order** | `OrderOverviewAssembler.java` | 展示 containerNo | ✅ 无影响（String 字段不变）|
| **order** | `OrderOverviewPageVO.java` | VO 中 containerNo | ✅ 无影响 |
| **前端** | `api/logistics.ts` | `ContainerVO` | ⚠️ 需扩展 TypeScript 类型 |
| **前端** | `pages/procurement/ContainerPage.vue` | 货柜管理页面 | ✅ 兼容（el-table 自动渲染新列）|
| **前端** | `pages/base/OrderOverviewPage.vue` | 概览页 | ✅ 无影响 |

### 6.2 container_no 的引用链

```
ContainerUseCase.addPlan()
    └── LogisticsPlan.containerId = container.id
    └── LogisticsPlan.containerNo = container.containerNo（回填批次号）

LogisticsPlan（containerNo = 批次号）
    │
    ├──→ DomesticCustomsRecord.containerNo（报关批次号）
    │         └──→ CustomsUseCase.batchCreate()
    │
    └──→ JapanCustomsRecord.containerNo（日本报关批次号）
              └──→ JapanCustomsUseCase.batchCreate()

OrderOverviewPage.vue
    ├── step6: detailData.domesticCustoms.containerNo
    └── step7: detailData.japanCustoms.containerNo
```

**结论**：`containerNo` 作为批次号在 customs 和 order overview 中展示，不受 container 实体字段扩展影响。

### 6.3 无影响的代码

| 文件 | 原因 |
|------|------|
| `ConsolidationPool.java` | 独立实体，无 container 字段 |
| `Ship.java` | 独立实体，仅被 container 引用 |
| `ShipUseCase.java` | 仅操作 Ship 实体 |
| `ShipController.java` | 独立接口 |
| `ShipPage.vue` | 船只管理页面 |

---

## 7. ShipPage 扩展设计

### 7.1 ShipPage 新增"货柜"Tab

```
ShipPage
    ├── 基本信息（现有 Tab）
    │       ├── 船名 / 船号 / 船公司 / 出发港 / 目的港
    │       └── 编辑 / 删除（现有功能）
    │
    └── 货柜列表（新增 Tab）← list7 迁入数据 + 新建货柜
            ├── 筛选：批次号 / 箱号 / 出运状态 / 显示标志
            ├── 表格列：批次号 | 箱号 | 船名 | 装柜日期 | 到港日期 | 到港时段 | 目的港 | 出运状态 | 归档 | 备注 | 操作
            └── 操作：编辑 / 分配船只（已有）/ 解除船只（已有）
```

### 7.2 货柜 Tab 数据来源

| 数据类型 | 来源 | 说明 |
|---------|------|------|
| list7 历史货柜 | Container 表（legacyId 非空）| showFlag=0/1，关联对应 Ship |
| 系统新建货柜 | Container 表（legacyId 为空）| 新调配流程创建 |

### 7.3 前端新增路由

```
/base/container        →  pages/base/ContainerPage.vue（分类容器总览，辅助入口）
/base/ship             →  pages/logistics/ShipPage.vue（主入口，含货柜 Tab）
```

> 菜单中 `/base/container` 可隐藏（不显示在导航栏），仅通过 ShipPage 货柜 Tab 访问。

---

## 8. 数据迁移方案

### 8.1 迁移策略

- **全部导入**（showFlag=0 归档记录也导入，不丢弃）
- **ID 1:1 保留**：`legacy_id` = list7.ID，`id` = list7.ID
- **日期清洗**：`0000-00-00` → NULL
- **ship_id 匹配**：根据 `ship_name + ship_number` 文本匹配 `ship` 表
  - 匹配规则：`ship.ship_name = list7.ship_name AND ship.ship_number = list7.ship_number`
  - 无法匹配时：`ship_id = NULL`（保留文本，待手动关联）

### 8.2 迁移 SQL（V27）

```sql
-- V27: 分类容器管理数据迁移（SPEC-B14）
-- 导入 list7 全部 3685 条记录，保留原始 ID

-- Step 1: 从外部库导出 list7 数据（示例）
-- 在 sddb0040100537 库执行：
-- SELECT * FROM list7 INTO OUTFILE '/tmp/list7.csv'
--   FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n';

-- Step 2: 导入临时表（略，可使用 LOAD DATA INFILE）

-- Step 3: 按原始 ID 插入 container 记录
SET IDENTITY_INSERT container ON;

INSERT INTO container (
    id, container_no, cabinet_no,
    load_date, arrival_date, period, legacy_status,
    arrival_location, remarks, show_flag,
    legacy_id, legacy_updater, legacy_updatetime,
    container_type, status,
    create_by, create_time, update_by, update_time, is_deleted
)
SELECT
    ID,
    container_no,                                          -- 批次号
    NULLIF(cabinet_no, ''),                              -- 箱号（空白→NULL）
    NULLIF(cabinet_time, '0000-00-00'),                 -- load_date
    NULLIF(arrive_time, '0000-00-00'),                   -- arrival_date
    NULLIF(period, ''),                                   -- period
    NULLIF(status, ''),                                   -- legacy_status
    NULLIF(arrival_address, ''),                         -- arrival_location
    NULLIF(memo, ''),                                    -- remarks
    IF(showFlag = 0, FALSE, TRUE),                      -- show_flag
    ID,                                                   -- legacy_id
    NULLIF(updater, ''),                                 -- legacy_updater
    STR_TO_DATE(updatetime, '%Y-%m-%d %H:%i:%s'),       -- legacy_updatetime
    'GP20',                                              -- container_type（默认）
    'CREATED',                                           -- status（FSM 默认）
    'SYSTEM',                                            -- create_by
    NOW(3),                                              -- create_time
    'SYSTEM',                                            -- update_by
    NOW(3),                                              -- update_time
    FALSE                                                 -- is_deleted
FROM list7_external;   -- 外部库或 CSV 导入的临时表

SET IDENTITY_INSERT container OFF;

-- Step 4: 匹配 ship_id（ship_name + ship_number → ship.id）
UPDATE container c
JOIN ship s ON s.ship_name = c.container_no_temp_ship_name
            AND s.ship_number = c.container_no_temp_ship_number
            AND s.is_deleted = FALSE
SET c.ship_id = s.id;
```

### 8.3 迁移注意事项

1. **外部数据源**：list7 在 `sddb0040100537` 库，需 `SELECT INTO OUTFILE` 导出 CSV，再 `LOAD DATA INFILE`
2. **ID 冲突检测**：执行前检查现有 container 表最大 ID，确保 list7.ID 范围（1-3685）不冲突
3. **ship 匹配率**：list7 有约 3000+ 不同船名，`ship` 表数据量有限，大量 ship_id 关联失败属正常
4. **container_type**：list7 无此字段，迁入时统一设为 `GP20`

---

## 9. API 设计

### 9.1 现有接口扩展

沿用 `ContainerController`（`/api/v1/containers`），新增字段自动包含在响应中，无需新增端点。

**GET `/api/v1/containers` 新增查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `showFlag` | Boolean | 显示标志筛选（默认仅返回 `showFlag=true`）|
| `legacyStatus` | String | 原始出运状态筛选（`未出` / `出完` / `待定`）|
| `cabinetNo` | String | 箱号模糊搜索 |
| `shipId` | Long | 船只筛选（已有）|

### 9.2 新增查询接口

**按船只分组查询（ShipPage 货柜 Tab）**

| 方法 | 路径 | @PreAuthorize | 说明 |
|------|------|:-------------:|------|
| `GET` | `/api/v1/containers/by-ship` | `container:read` | 按船只分组的货柜列表（ShipPage Tab 数据源）|

**响应格式**
```json
{
  "code": 200,
  "data": [
    {
      "shipId": 1,
      "shipName": "SITC YOKOHAMA V.2112E",
      "shipNumber": "2112E",
      "totalCount": 15,
      "activeCount": 10,
      "archivedCount": 5,
      "containers": [
        {
          "id": 7,
          "containerNo": "21008",
          "cabinetNo": "CBHU4225619",
          "legacyStatus": "未出",
          "showFlag": true,
          "period": "晚上18-24",
          "loadDate": "2021-03-04",
          "arrivalDate": "2021-03-10",
          "arrivalLocation": "福冈",
          "remarks": ""
        }
      ]
    }
  ],
  "message": "ok"
}
```

---

## 10. 前端 UI 设计

### 10.1 路由

```
/base/container        →  pages/base/ContainerPage.vue（新建，分类容器总览）
/base/ship             →  pages/logistics/ShipPage.vue（扩展货柜 Tab）
```

> 菜单：`/base/container` 不在 AppLayout 显示，通过 ShipPage Tab 访问。

### 10.2 ShipPage 货柜 Tab 表格列定义

| 列 | 字段 | 说明 |
|----|------|------|
| 批次号 | `containerNo` | list7.container_no，对单标识 |
| 箱号 | `cabinetNo` | ISO 标准箱号（如 CBHU4225619）|
| 装柜日期 | `loadDate` | cabinet_time |
| 到港日期 | `arrivalDate` | arrive_time |
| 到港时段 | `period` | 凌晨0-6 / 早上6-12 / 下午12-18 / 晚上18-24 |
| 目的港 | `arrivalLocation` | arrival_address |
| 出运状态 | `legacyStatus` | 未出 / 出完 |
| 显示 | `showFlag` | 归档标记（开关）|
| 备注 | `remarks` | memo |
| 原始更新人 | `legacyUpdater` | 仅详情可见 |
| 原始更新时间 | `legacyUpdatetime` | 仅详情可见 |

### 10.3 编辑弹窗字段

| 字段 | 输入控件 | 说明 |
|------|---------|------|
| 批次号 | el-input | |
| 箱号 | el-input | |
| 装柜日期 | el-date-picker | |
| 到港日期 | el-date-picker | |
| 到港时段 | el-select | 凌晨0-6 / 早上6-12 / 下午12-18 / 晚上18-24 |
| 目的港 | el-input | |
| 出运状态 | el-select | 未出 / 出完 |
| 显示标志 | el-switch | 归档/活跃 |
| 备注 | el-input（textarea）| |

---

## 11. 实施记录（2026-05-24 Phase 1）

| 改动 | 文件 | 说明 |
|------|------|------|
| 数据库迁移 | `V27__container_category_fields.sql`（新建）| ALTER TABLE container 新增 7 个字段 + 3 个索引 |
| Entity 扩展 | `Container.java` | 追加 7 个字段 + import LocalDateTime |
| DTO 扩展 | `ContainerPageQuery.java` | 追加 7 个字段 |
| CMD 扩展 | `ContainerCreateCmd.java` | 追加 cabinetNo / period / legacyStatus / showFlag |
| CMD 扩展 | `ContainerUpdateCmd.java` | 追加 cabinetNo / period / legacyStatus / showFlag |
| Assembler 扩展 | `ContainerAssembler.java` | 追加 7 个字段映射（toDto / toDtoFromArray / toEntity / copyUpdate）|
| 查询扩展 | `ContainerQuery.java` | 追加 showFlag / legacyStatus / cabinetNo 筛选参数 |
| Repository 扩展 | `ContainerRepository.java` + `JpaContainerRepository.java` | 追加 3 个新查询方法 |
| UseCase 扩展 | `ContainerUseCase.pageQuery()` | 追加 list7 筛选分支逻辑 |
| 前端 API | `api/logistics.ts` | ContainerVO / CreateContainerRequest / UpdateContainerRequest 追加字段 + list 参数扩展 |
| 前端页面 | `ContainerPage.vue` | formData 追加字段 + 表格追加 4 列（cabinetNo/period/legacyStatus/showFlag）+ 编辑弹窗新增字段 + copyColumns 更新 |
| i18n | zh.json / ja.json | logistics.container.column 追加 4 个 key + logistics.container.showFlag 分组 |
| 文档 | SPEC-B14 | 状态更新为 Phase 1 实施完成 |

## 12. 实施计划

> **Flyway 版本**：V27__container_category_fields.sql

### Phase 1 — 数据库 + Entity ✅（2026-05-24）

1. 新增 Flyway `V27__container_category_fields.sql`
   - ALTER TABLE container（新增字段 + 索引）
2. 扩展 `Container.java` Entity（新增 7 个字段）
3. 扩展 `ContainerPageQuery.java`（新增 7 个字段）
4. 扩展 `ContainerCreateCmd.java`（新增字段）
5. 扩展 `ContainerUpdateCmd.java`（新增字段）
6. 扩展 `ContainerAssembler.java`（字段映射）
7. 扩展 `ContainerQuery.java`（新增筛选参数）
8. 扩展 `JpaContainerRepository.java`（新增查询方法）
9. 扩展 `ContainerUseCase.pageQuery()`（处理新增筛选条件）
10. 扩展 `api/logistics.ts`（ContainerVO / CMD / list 参数）
11. 扩展 `ContainerPage.vue`（表单 + 表格列 + 编辑弹窗）
12. i18n（zh.json / ja.json）新增 key

### Phase 2 — 数据迁移

1. 从 `sddb0040100537` 导出 list7 数据（CSV / SELECT INTO OUTFILE）
2. 编写迁移脚本（INSERT + ship_id 匹配）
3. 验证数据完整性（3685 条，legacy_id 1:1 对应）

### Phase 3 — ShipPage 货柜 Tab + 分类容器总览

1. ShipPage.vue 新增"货柜"Tab（展示该船所有 Container）
2. 新增 `GET /api/v1/containers/by-ship` 端点（按船只分组）
3. 新建 `api/containerCategory.ts`
4. 新建 `pages/base/ContainerPage.vue`（分类容器总览）
5. 路由注册（`/base/container`，菜单隐藏或显示）
6. AppLayout.vue 菜单加入 `/base/container` 入口

---

## 12. 技术债务

| 项目 | 说明 | 优先级 |
|------|------|--------|
| `container_no` 语义变更 | 原为系统货柜号，迁入后存批次号，需告知用户 | P1 |
| `ship` 表匹配率低 | list7 有 3000+ 不同船名，`ship` 表数据量有限 | P1 |
| `period` 非枚举 | 目前存 VARCHAR，建议后续改为 ENUM | P2 |
| `container_type` 默认 GP20 | list7 无此字段，真实类型需补录 | P2 |
| `rireki` 未导入 | list7 有 `rireki` LONGTEXT 历史记录字段，当前未处理 | P3 |
| `/base/container` 入口隐藏 | 该路由不在菜单显示，通过 ShipPage Tab 访问，用户可能找不到 | P2 |

---

## 13. 附录：字段对照表

| list7 列 | container 列 | 变换规则 |
|-----------|-------------|---------|
| `ID` | `legacy_id`（新增）| 直接映射 |
| `container_no` | `container_no` | 直接映射（批次号语义）|
| `cabinet_no` | `cabinet_no`（新增）| 空白 → NULL |
| `ship_name` | `ship_id`（已有）| 文本匹配 ship.ship_name |
| `ship_number` | `ship_id`（已有）| 文本匹配 ship.ship_number |
| `cabinet_time` | `load_date`（已有）| `0000-00-00` → NULL |
| `arrive_time` | `arrival_date`（已有）| `0000-00-00` → NULL |
| `period` | `period`（新增）| 空白 → NULL |
| `status` | `legacy_status`（新增）| 空白 → NULL |
| `arrival_address` | `arrival_location`（已有）| 空白 → NULL |
| `memo` | `remarks`（已有）| 空白 → NULL |
| `showFlag` | `show_flag`（新增）| 0→FALSE，1→TRUE |
| `updater` | `legacy_updater`（新增）| 空白 → NULL |
| `updatetime` | `legacy_updatetime`（新增）| 直接映射 |
| — | `id` | 设为 list7.ID |
| — | `container_type` | 默认 GP20 |
| — | `status` | 默认 CREATED |
| — | `is_deleted` | 默认 FALSE |
