# 旧系统数据导入 — 集成分析文档（v2）

> 日期：2026-05-24
> 源文件：`d:\Programme\database\20260524\list8.sql`、`list1.sql`
> 源库：`sddb0040100537`（192.168.12.178）
> **目标：集成进现有 Manpou 系统（历史数据，不破坏现有业务逻辑）**

---

## 一、核心业务定位

### 1.1 两表的业务语义

| 表 | 记录数 | 时间跨度 | 业务含义 | 粒度 |
|---|--------|---------|---------|------|
| `list1` | 71,797 | 2017–2026 | **订单商品主档**（发注单+价格+库存+物流全链路） | SKU（货号+颜色） |
| `list8` | 3,661 | 2024–2026 | **装箱执行记录**（货物打包后在仓库等待/已装柜） | 货物批次（货号级，无颜色） |

**两表关系：**
```
list1 ←──code+sub-code──→ list8
         (同一货号，
          list1按颜色分row，
          list8按仓库批次分row)
```

**旧系统完整业务流程（list1 + list8）：**

```
list1 (发注单/订单)
  code + sub-code = SKU
  order-count = 订购数量
  unit_ch/rate = 采购价/汇率
  arrival-depo = 目的港
  ↓
  工厂生产
  ↓
list8 (装箱明细)
  code = 货号（无颜色）
  num = 实装数量（可能<order-count，部分发货）
  location/souko = 仓库/工厂
  weight/weight2 = 净重/毛重
  date1 = 计划装箱日
  showFlag = 完成标志
```

**新系统业务流程（当前）：**

```
ReplenishmentDemand（补货需求）
  ↓
Procurement（发注单）— 关联 factory + destination
  ↓
QcRecord（验货记录）— 尺寸/重量/质检
  ↓
LogisticsPlan（调配计划）— 装箱计划
  ↓
ConsolidationPool（拼柜池）
  ↓
Container（货柜）— 船公司箱号
  ↓
Ship（船只）
```

### 1.2 关键发现：旧系统 vs 新系统的概念差异

| 概念 | 旧系统（list8） | 新系统 |
|------|--------------|--------|
| 仓库 | `souko` = 供应商/工厂名（如"菏泽"、"徐州赵氏起重"） | `Factory.factoryName` / `Procurement` |
| 工厂地址 | `factory_addr` = 完整公司地址 | `Factory.province/city/county` |
| 验货 | 无独立记录（合并在装箱阶段） | `QcRecord` 独立实体 |
| 货柜 | `container`（仅 list1 有，文本字段） | `Container` 独立实体 |
| 装箱 | `list8` 单表记录 | `LogisticsPlan` 独立实体 |
| 颜色/规格 | `list1.sub-code`（仅 list1 有） | `Procurement.subProductCode` |
| 净重/毛重 | `weight`/`weight2`（部分记录有效） | `LogisticsPlan.netWeightKg/grossWeightKg` |

---

## 二、字段深度分析

### 2.1 list8 关键字段解读

**`weight` / `weight2` — 净重/毛重分离**

从数据实测（行55-57）：

| ID | code | weight | weight2 | 分析 |
|----|------|--------|---------|------|
| 55 | ee321 | 500 | 600 | weight=净重500, weight2=毛重600 |
| 56 | sg060 | 535 | 10 | weight=总重535, weight2=? (疑似单价或数量) |
| 57 | ee346 | 726 | 10 | 同上 |
| 69 | sg001 | 1900 | 1950 | weight=净重1900, weight2=毛重1950 |

**结论**：`weight2` 含义不稳定
- 近期记录（2025+）：`weight2` ≈ `weight + 5~10%` → 毛重
- 部分记录：`weight2=10`（异常值，可能已被覆盖/复用）
- 建议：仅在 `weight2 > weight` 时视为毛重，否则 `weight` 为总重

**`kensa` — 检品/验货标识**

| 值 | 含义 |
|----|------|
| NULL | 不需检品 |
| `带锂电池` | 特殊处理 |
| `木质商检` | 需木质包装商检 |

→ 映射至 `Procurement.requiresQc`（文本）或 `Factory.needsQc`

**`date1` — 装箱日期**

- 正常：`2024-01-15` ~ `2026-05-13`
- 无效：`0000-00-00`（大量，导入时置 NULL）
- `showFlag=0` → `status='完成'` → 已装箱
- `showFlag=1` → `status=''` → 待处理

**`unit_ch` / `rate` — 价格字段**

从行56/57：`unit_ch=535`, `rate=10` → 但 `weight=535` 逻辑矛盾

| 行 | unit_ch | rate | 分析 |
|----|---------|------|------|
| 56 | 535 | 10 | 535=单价(元)? 但此货为钢铁非数量 |
| 57 | 726 | 10 | 同上 |
| 55 | 0 | 0 | 无价格信息 |
| 85 | 83 | 10 | 2026年新记录，有价格 |

**结论**：
- `unit_ch=0/rate=0` 的记录 = 无价格信息（历史记录未录入）
- `unit_ch>0` 的记录 = 2025年后新增，价格才有录入
- **价格信息主要在 list1**（`kaitsuke`/`unit-ch` 字段），list8 的 `unit_ch/rate` 是补充/后补

**`souko` vs `location` — 仓库/位置**

| 行 | souko | location | 分析 |
|----|-------|----------|------|
| 53 | `菏泽` | `菏泽` | 相同 |
| 54 | `菏泽` | `河南` | location=省, souko=市 |
| 68 | `徐州赵氏起重机械有限公司` | `保定` | souko=工厂名, location=市 |
| 85 | `浙江康德意休闲椅有限公司` | `丽水` | souko=工厂全名 |

**结论**：
- `souko` 是核心：城市名 OR 工厂全名
- `location` 是城市/地区名（省/市）
- `souko` 含工厂名时 → 对应 `Factory` 实体
- `souko` 仅城市名时 → 对应 `Procurement` 的 warehouse/province 信息

**`other` — 备注/特殊要求**

高价值信息字段！包含：
- "一箱里面2把椅子" — 包装规格
- "一条整柜能装548箱尺寸53*52.5*46" — 装箱计算
- "本周装柜有托盘" — 物流要求
- "装货需要提前说" — 操作提示
- "带锂电池" — 特殊处理

→ 映射至 `Procurement.customsRemarks` 或 `LogisticsPlan.remarks`

**`factory_addr` — 工厂地址**

| 行 | factory_addr | 分析 |
|----|-------------|------|
| 56 | `沧州米特尔汽车维修设备有限公司` | 实为工厂名，非地址 |
| 68 | `保定` | 仅城市名 |
| 85 | `浙江省丽水市青田县祯埠镇小河坑工业区8号康德意。13715513407` | 完整地址+电话 |

**结论**：`factory_addr` 语义不稳定，需清洗：
- 含完整地址 → 解析 province/city/county
- 仅城市名 → 视为 location 冗余
- 公司全名 → 提取公司名作为 factory_name

### 2.2 list1 关键字段解读

**`kaitsuke` / `hyoten` — 买付/票点**

从实测数据：**这两字段在导出数据中全部为 NULL**（71,797 行均无值）

可能原因：导出时未 SELECT 这些字段，或历史记录从未录入

→ **不能依赖此字段**，需用 `unit-ch` 作为采购价替代

**`order-count` vs `inspect-count` — 订购 vs 验货数量**

- `order-count = inspect-count` → 已全验货
- `order-count > inspect-count` → 部分验货/待验货
- `inspect-count = 0` → 未开始验货

**`fba-stock` — FBA库存**

实测：大量为 0，仅少数记录有值（如行34: `fba-stock=7`）

→ 旧系统未广泛使用 FBA，`fba-stock` 字段参考价值有限

**`houkoku` — 报告/申报**

实测：几乎全部为空字符串 `''`，偶尔有内容（如 `ライター付きシガーケース`）

→ 非关键字段，仅作备注参考

**`rate` — 汇率（list1）**

2017年记录：`rate=17`
2025年记录：`rate=10`
2026年记录：`rate=9` 或 `rate=10`

→ 有实际汇率值，可用

**`arrival-flag` / `arrival-jikan` — 到货标志/小时**

| 值 | 含义 |
|----|------|
| 0 | 未到货 |
| 1 | 已到货 |
| `arrival-jikan` | 到货时间（小时，如 `8`=8小时） |

---

## 三、实体映射方案

### 3.1 映射架构图

```
旧系统                 新系统（扩展）
─────────────────     ─────────────────────────────────
list1 ──────────────► Procurement（扩展 legacy 字段）
  code               → productCode
  sub-code           → subProductCode
  order-count        → quantity
  unit-ch            → priceRmb（kaitsuke 为精确版）
  rate               → exchangeRate
  hyoten             → taxPoint
  arrival-depo       → destination
  yoyaku-hasoubi      → plannedShipDate
  departure           → actualShipDate
  height/width/depth  → LogisticsPlan (新记录)
  img                → Product.imageUrl
  item-name          → Product.name (日文)

list8 ──────────────► LogisticsPlan（扩展 legacy 字段）
  code               → productCode
  num                → quantity
  pieces             → packedBoxCount（新增）
  weight             → netWeightKg（当 weight2 > weight 时）
  weight2            → grossWeightKg（当 weight2 > weight 时）
  length             → cargoLengthCm
  souko              → Factory（匹配或新建）
  location           → 城市信息
  date1              → actualShipDate
  status='完成'       → LogisticsStatus.PACKED / DELIVERED
  showFlag           → legacyShowFlag
  other              → remarks
  kensa              → requiresQc

list1.container ──► Container
  container          → containerNo
  arrival            → arrivalDate
  arrival-depo       → arrivalLocation

Factory ←── list8.souko（工厂名/仓库名）
```

### 3.2 Procurement 字段填充方案

```
现有 Procurement 字段：
┌─────────────────────────────────────────────────────────────┐
│ productCode       ← list1.code                               │
│ subProductCode    ← list1.sub-code                           │
│ quantity          ← list1.order-count                         │
│ priceRmb          ← COALESCE(list1.kaitsuke, list1.unit-ch)   │
│ exchangeRate      ← list1.rate (若 rate > 0)                  │
│ taxPoint          ← list1.hyoten (若 hyoten > 0, else 1.1)    │
│ destination       ← list1.arrival-depo                        │
│ plannedShipDate   ← list1.yoyaku-hasoubi                      │
│ actualShipDate    ← list1.departure                           │
│ material          ← list1.material                            │
│ customsRemarks    ← list1.note                                │
│ japanLead         ← 从 updater 人名匹配                       │
│ status            ← 根据 inspect-count 推断                    │
└─────────────────────────────────────────────────────────────┘

需新增字段（Legacy 扩展）：
┌─────────────────────────────────────────────────────────────┐
│ legacy_list1_id    BIGINT     -- list1.ID 溯源                │
│ legacy_order_no    VARCHAR(64)-- list1.order-group (批次号)     │
│ legacy_img         VARCHAR(512)-- list1.img                    │
│ legacy_item_name  VARCHAR(512)-- list1.item-name (日文品名)   │
│ legacy_inspect_qty INT        -- list1.inspect-count          │
│ legacy_fba_stock  INT        -- list1.fba-stock               │
│ legacy_rate       DECIMAL     -- list1.rate (原始汇率)          │
│ legacy_houkoku    VARCHAR(64) -- list1.houkoku                 │
│ legacy_is_legacy  BOOLEAN    -- true = 历史导入数据             │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 LogisticsPlan 字段填充方案

```
现有 LogisticsPlan 字段：
┌─────────────────────────────────────────────────────────────┐
│ productCode    ← list8.code                                  │
│ quantity       ← list8.num                                   │
│ cargoWeightKg  ← list8.weight (或grossWeightKg)              │
│ cargoLengthCm  ← list8.length                                 │
│ factoryId      ← 从 list8.souko 匹配 Factory.id               │
│ actualShipDate ← list8.date1                                  │
│ remarks        ← list8.other                                  │
│ status         ← showFlag=0 → DELIVERED; showFlag=1 → PACKED  │
└─────────────────────────────────────────────────────────────┘

需新增/扩展字段：
┌─────────────────────────────────────────────────────────────┐
│ legacy_list8_id       BIGINT     -- list8.ID 溯源             │
│ pieces                INT        -- list8.pieces (件数)       │
│ netWeightKg           DECIMAL    -- list8.weight (毛重不明时)   │
│ grossWeightKg         DECIMAL    -- list8.weight2 (>weight时)  │
│ legacyDestination     VARCHAR(128)-- list8.destination         │
│ legacyWarehouse       VARCHAR(128)-- list8.souko               │
│ legacyMaterial        VARCHAR(64) -- list8.material            │
│ legacyKensa           VARCHAR(255)-- list8.kensa (检品标识)     │
│ legacyShowFlag        BOOLEAN    -- list8.showFlag            │
│ legacyStatus          VARCHAR(64) -- list8.status              │
│ legacyUnitCh          DECIMAL    -- list8.unit_ch              │
│ legacyRate            DECIMAL    -- list8.rate                 │
│ legacyIsLegacy        BOOLEAN    -- true = 历史导入数据          │
└─────────────────────────────────────────────────────────────┘
```

### 3.4 Factory 工厂映射方案（关键）

`list8.souko` 值域分析（从样本数据）：

| 类型 | 示例 | 匹配策略 |
|------|------|---------|
| 城市名 | `菏泽`、`东莞`、`青岛` | 城市名 → province/city |
| 工厂全名 | `徐州赵氏起重机械有限公司` | 精确匹配 factory_name |
| 地址+电话 | `浙江省丽水市青田县...13715513407` | 解析后提取公司名 |

**Factory 扩展字段：**

```
需新增：
┌─────────────────────────────────────────────────────────────┐
│ legacy_source      VARCHAR(32)  -- 'list8' 溯源标记          │
│ legacy_souko       VARCHAR(128) -- list8.souko 原始值         │
│ legacy_location    VARCHAR(128) -- list8.location 原始值      │
│ legacy_is_warehouse BOOLEAN    -- true = list8中的仓库/供应商  │
└─────────────────────────────────────────────────────────────┘
```

---

## 四、数据清洗规则（完整版）

### 4.1 日期清洗

```sql
-- 所有 0000-00-00 → NULL
UPDATE legacy_import_list8 SET date1 = NULL WHERE date1 = '0000-00-00';
UPDATE legacy_import_list1 SET yoyaku_hasoubi = NULL WHERE yoyaku_hasoubi = '0000-00-00';
UPDATE legacy_import_list1 SET departure = NULL WHERE departure = '0000-00-00';
UPDATE legacy_import_list1 SET arrival = NULL WHERE arrival = '0000-00-00';
```

### 4.2 代码清洗

```sql
-- code：去空格，统一大写
UPDATE legacy_import_list8 SET code = UPPER(TRIM(code));
UPDATE legacy_import_list1 SET code = UPPER(TRIM(code));

-- sub-code：去空格
UPDATE legacy_import_list1 SET sub_code = TRIM(sub_code) WHERE sub_code IS NOT NULL;

-- order-group：去空格
UPDATE legacy_import_list1 SET order_group = TRIM(order_group);
```

### 4.3 重量字段清洗

```sql
-- weight2 > weight 时视为毛重
UPDATE legacy_import_list8
SET gross_weight_kg = weight2, net_weight_kg = weight
WHERE weight2 > weight AND weight2 > 0;

-- weight2 = 0 或 weight2 <= weight 时：weight 为总重，无毛净分离
UPDATE legacy_import_list8
SET gross_weight_kg = weight, net_weight_kg = NULL
WHERE weight2 <= weight OR weight2 = 0;
```

### 4.4 仓库/工厂解析

```sql
-- 判断 souko 是否为工厂全名（包含"有限公司"/"株式会社"等关键词）
-- 是 → 解析为 factory_name，location 提取城市
-- 否 → 仅作为 warehouse/city 字段
```

### 4.5 人名标准化

**list8.manager 值域：** `段`、`雪`、`刘萌`、`王`、`张`、`车`、`孙`、`中村賢司`、`趙暁剣`、`燕平`、`陈娅`、`孫爽`、`孙雪丽`、`赵金湘`

**list1.updater 值域：** `中村さん`、`吕さん`、`中村賢司`、`趙暁剣`、`燕平`、`曉さん`、`平宏`、`増山千鶴`

→ 需建立 `legacy_user_mapping`：旧人名 → `user.id` 映射表

---

## 五、实施路径

### 阶段 0：数据质量统计（先行）

在 MySQL 中直接分析原始 SQL：

```sql
-- list8 质量
SELECT
  COUNT(*) AS total,
  SUM(date1 = '0000-00-00') AS invalid_dates,
  SUM(unit_ch = 0 AND rate = 0) AS no_price,
  SUM(weight2 > weight) AS has_gross_weight,
  SUM(showFlag = 1) AS active_records,
  COUNT(DISTINCT code) AS unique_codes,
  COUNT(DISTINCT souko) AS unique_soukos
FROM list8;

-- list1 质量
SELECT
  COUNT(*) AS total,
  SUM(kaitsuke IS NOT NULL) AS has_kaitsuke,
  SUM(hyoten IS NOT NULL) AS has_hyoten,
  SUM(unit_ch > 0) AS has_unit_price,
  SUM(rate > 0) AS has_rate,
  SUM(arrival_depo != '') AS has_depo,
  COUNT(DISTINCT code) AS unique_codes,
  COUNT(DISTINCT order_group) AS unique_orders
FROM list1;
```

### 阶段 1：Schema 扩展（V51）

**V51: Procurement 扩展 legacy 字段**

```sql
ALTER TABLE procurement ADD COLUMN legacy_list1_id INT COMMENT 'list1.ID 溯源';
ALTER TABLE procurement ADD COLUMN legacy_order_group VARCHAR(64) COMMENT 'list1.order-group 批次号';
ALTER TABLE procurement ADD COLUMN legacy_img VARCHAR(512) COMMENT 'list1.img 图片';
ALTER TABLE procurement ADD COLUMN legacy_item_name VARCHAR(512) COMMENT 'list1.item-name 日文品名';
ALTER TABLE procurement ADD COLUMN legacy_inspect_qty INT COMMENT 'list1.inspect-count 验货数量';
ALTER TABLE procurement ADD COLUMN legacy_fba_stock INT DEFAULT 0 COMMENT 'list1.fba-stock FBA库存';
ALTER TABLE procurement ADD COLUMN legacy_rate DECIMAL(10,4) COMMENT 'list1.rate 原始汇率';
ALTER TABLE procurement ADD COLUMN legacy_is_legacy BOOLEAN DEFAULT FALSE COMMENT '是否历史导入';
ALTER TABLE procurement ADD COLUMN legacy_houkoku VARCHAR(64) COMMENT 'list1.houkoku';
CREATE INDEX idx_procurement_legacy_list1 ON procurement(legacy_list1_id);
CREATE INDEX idx_procurement_legacy ON procurement(legacy_is_legacy);
```

**V52: LogisticsPlan 扩展 legacy 字段**

```sql
ALTER TABLE logistics_plan ADD COLUMN legacy_list8_id INT COMMENT 'list8.ID 溯源';
ALTER TABLE logistics_plan ADD COLUMN pieces INT COMMENT 'list8.pieces 件数';
ALTER TABLE logistics_plan ADD COLUMN gross_weight_kg DECIMAL(12,4) COMMENT '毛重(kg)';
ALTER TABLE logistics_plan ADD COLUMN net_weight_kg DECIMAL(12,4) COMMENT '净重(kg)';
ALTER TABLE logistics_plan ADD COLUMN legacy_destination VARCHAR(128) COMMENT 'list8.destination 目的地';
ALTER TABLE logistics_plan ADD COLUMN legacy_warehouse VARCHAR(128) COMMENT 'list8.souko 仓库名';
ALTER TABLE logistics_plan ADD COLUMN legacy_material VARCHAR(64) COMMENT 'list8.material 材质';
ALTER TABLE logistics_plan ADD COLUMN legacy_kensa VARCHAR(255) COMMENT 'list8.kensa 检品';
ALTER TABLE logistics_plan ADD COLUMN legacy_show_flag BOOLEAN COMMENT 'list8.showFlag';
ALTER TABLE logistics_plan ADD COLUMN legacy_status VARCHAR(64) COMMENT 'list8.status 原状态';
ALTER TABLE logistics_plan ADD COLUMN legacy_unit_ch DECIMAL(12,4) COMMENT 'list8.unit_ch 单价(元)';
ALTER TABLE logistics_plan ADD COLUMN legacy_rate DECIMAL(10,4) COMMENT 'list8.rate 汇率';
ALTER TABLE logistics_plan ADD COLUMN legacy_is_legacy BOOLEAN DEFAULT FALSE COMMENT '是否历史导入';
CREATE INDEX idx_lp_legacy_list8 ON logistics_plan(legacy_list8_id);
CREATE INDEX idx_lp_legacy ON logistics_plan(legacy_is_legacy);
```

**V53: Factory 扩展 legacy 字段**

```sql
ALTER TABLE factory ADD COLUMN legacy_source VARCHAR(32) COMMENT '来源表 list8';
ALTER TABLE factory ADD COLUMN legacy_souko VARCHAR(128) COMMENT 'list8.souko 原始值';
ALTER TABLE factory ADD COLUMN legacy_location VARCHAR(128) COMMENT 'list8.location 原始值';
ALTER TABLE factory ADD COLUMN legacy_is_warehouse BOOLEAN DEFAULT FALSE COMMENT '是否来自list8仓库';
CREATE INDEX idx_factory_legacy ON factory(legacy_source);
```

### 阶段 2：数据导入脚本

**步骤 2.1：建立 Factory 映射（souko → factory）**

```sql
-- 从 list8.souko 提取唯一工厂/仓库列表
CREATE TEMPORARY TABLE tmp_souko_list AS
SELECT DISTINCT TRIM(souko) AS souko, TRIM(location) AS location
FROM legacy_import_list8 WHERE souko != '';

-- 分类：工厂全名 vs 城市名
-- 城市名 → 插入 factory (category=OTHER, legacy_is_warehouse=true)
-- 工厂名 → 尝试匹配 factory_name，或插入新 factory
```

**步骤 2.2：导入 list1 → Procurement**

```sql
INSERT INTO procurement (
  product_code, sub_product_code, quantity,
  price_rmb, exchange_rate, tax_point,
  destination, planned_ship_date, actual_ship_date,
  material, customs_remarks,
  japan_lead, -- 从 updater 人名匹配
  legacy_list1_id, legacy_order_group, legacy_img,
  legacy_item_name, legacy_inspect_qty, legacy_fba_stock,
  legacy_rate, legacy_is_legacy, legacy_houkoku,
  create_time, update_time
)
SELECT
  UPPER(TRIM(code)),
  TRIM(sub_code),
  order_count,
  COALESCE(NULLIF(kaitsuke, 0), unit_ch),  -- kaitsuke优先，否则unit_ch
  NULLIF(rate, 0),
  COALESCE(NULLIF(hyoten, 0), 1.1),
  TRIM(arrival_depo),
  NULLIF(yoyaku_hasoubi, '0000-00-00'),
  NULLIF(departure, '0000-00-00'),
  material,
  note,
  updater,
  ID AS legacy_list1_id,
  TRIM(order_group),
  img,
  item_name,
  inspect_count,
  fba_stock,
  rate,
  TRUE,
  houkoku,
  FROM_UNIXTIME(UNIX_TIMESTAMP(updatetime)),
  FROM_UNIXTIME(UNIX_TIMESTAMP(updatetime))
FROM legacy_import_list1
WHERE code IS NOT NULL AND code != '';
```

**步骤 2.3：导入 list8 → LogisticsPlan**

```sql
INSERT INTO logistics_plan (
  plan_code, product_code, quantity,
  cargo_weight_kg, net_weight_kg, gross_weight_kg,
  cargo_length_cm, actual_ship_date,
  factory_id, -- 从 souko 匹配 factory.id
  remarks, status,
  legacy_list8_id, pieces, legacy_destination,
  legacy_warehouse, legacy_material, legacy_kensa,
  legacy_show_flag, legacy_status,
  legacy_unit_ch, legacy_rate, legacy_is_legacy,
  create_time, update_time
)
SELECT
  CONCAT('L-LEGACY-', ID) AS plan_code,  -- 生成唯一 plan_code
  UPPER(TRIM(code)),
  num,
  weight,
  CASE WHEN weight2 > weight THEN weight ELSE NULL END,
  CASE WHEN weight2 > weight THEN weight2 ELSE weight END,
  length,
  NULLIF(date1, '0000-00-00'),
  factory_id,  -- 匹配结果
  other,
  CASE show_flag WHEN 0 THEN 'DELIVERED' WHEN 1 THEN 'PACKED' END,
  ID AS legacy_list8_id,
  pieces,
  TRIM(destination),
  TRIM(souko),
  TRIM(material),
  kensa,
  show_flag,
  status,
  unit_ch,
  rate,
  TRUE,
  FROM_UNIXTIME(UNIX_TIMESTAMP(updatetime)),
  FROM_UNIXTIME(UNIX_TIMESTAMP(updatetime))
FROM legacy_import_list8;
```

### 阶段 3：后置关联（Import 后执行）

```sql
-- 3.1: list1 ↔ list8 通过 code 关联（logistics_plan → procurement）
UPDATE logistics_plan lp
JOIN procurement p ON p.product_code = lp.product_code
SET lp.procurement_id = p.id
WHERE lp.legacy_is_legacy = TRUE
  AND p.legacy_is_legacy = TRUE
  AND lp.procurement_id IS NULL;

-- 3.2: 人名匹配 → user.id
-- 需要建立 legacy_user_mapping 表
```

---

## 六、数据质量评估

### 6.1 预估问题统计（基于样本推断）

| 问题 | list8 估计 | list1 估计 | 影响程度 |
|------|-----------|-----------|---------|
| `0000-00-00` 无效日期 | ~15%（~549条） | ~60%（~43k条） | **高**（日期字段不准确） |
| `kaitsuke`/`hyoten` 全NULL | — | ~100%（71k条） | 中（需用 unit-ch 替代） |
| `unit_ch=0`（无价格） | ~80%（~2.9k条） | ~20%（~14k条） | **高**（历史价格缺失） |
| `rate=0`（无汇率） | ~80% | ~5% | 低（list1有汇率） |
| `code` 为空 | ~0.5% | ~1%（约700条） | 低（过滤） |
| `sub-code` 为空 | — | ~30%（~21k条） | 中（无颜色细分） |
| `weight2 <= weight`（无法分离净毛重） | ~85% | — | 中（毛净重混淆） |
| `factory_addr` 不规范 | ~30% | — | 中（需解析） |

### 6.2 可用数据估算

| 字段 | list8 可用率 | list1 可用率 |
|------|------------|------------|
| productCode | ~99.5% | ~99% |
| quantity | ~99% | ~99% |
| priceRmb | ~20%（仅2025+） | ~80%（unit-ch） |
| exchangeRate | ~20% | ~95% |
| destination | ~95% | ~95% |
| date/shipDate | ~85% | ~40% |
| net/gross weight | ~15%（仅 weight2>weight） | — |
| material | ~90% | ~70% |
| factory/souko | ~95% | — |

---

## 七、关键决策

### D1: 导入策略（最关键）

| 选项 | 描述 | 优缺点 |
|------|------|--------|
| **A（推荐）** | 扩展现有 `Procurement` + `LogisticsPlan` + `Factory`，加 `legacy_is_legacy` 标志，字段直接填充 | ✅ 历史和现行数据统一查询；⚠️ 需处理 ID 冲突/状态不一致 |
| **B** | 独立 `legacy_import_list1` / `legacy_import_list8` 表，仅查询不编辑 | ✅ 隔离、不破坏现有逻辑；⚠️ 两套查询、URL/关联复杂 |
| **C** | 仅导入 list1 → Procurement，list8 不导入（作为参考数据） | ✅ 最简单；⚠️ 装箱/重量数据丢失 |

**推荐选项 A**：历史数据导入现有表，`legacy_is_legacy=TRUE`，状态字段按"已完成"终态锁定，前端通过标志区分展示。

### D2: Plan_Code 冲突

`LogisticsPlan.plan_code` 有 UK 约束（`uk_plan_code`），旧数据 `plan_code` 为空。
- 方案：生成 `L-LEGACY-{list8.ID}` 格式
- 需确认：新系统 plan_code 格式是否为 `L-YYYYMMDD-NNN`？

### D3: 人名 → User 关联

`list1.updater`（日文：`中村さん`）与 `user.username`（中文：`中村賢司`）名称不完全一致。
- 需建立 `legacy_user_mapping (legacy_name, user_id)` 手工映射表
- 或在导入时保留 `legacy_updater_name`，不强制关联

### D4: Factory 仓库处理

`list8.souko` 是供应商/工厂混合概念（城市名 OR 工厂名）。
- 城市名（`菏泽`）→ 是否创建 Factory 记录？还是仅作为 Procurement.city 字段？
- 建议：**不创建仅城市的 Factory**，城市信息存入 LogisticsPlan.city

### D5: 导入批次

71k+3.6k = ~75k 条记录，是否分批导入？
- 建议：按 `updatetime` 年份分 3 批（2017-2019 / 2020-2022 / 2023-2026）

---

## 八、现有系统字段缺失清单（需新增）

| 实体 | 缺失字段 | 优先级 | 用途 |
|------|---------|--------|------|
| Procurement | `legacy_list1_id` | P1 | 溯源 |
| Procurement | `legacy_order_group` | P1 | 批次查询 |
| Procurement | `legacy_item_name` (日文品名) | P1 | 日文显示 |
| Procurement | `legacy_inspect_qty` | P2 | 验货进度 |
| Procurement | `legacy_img` | P2 | 商品图片 |
| LogisticsPlan | `pieces` (件数) | P1 | 包装数量 |
| LogisticsPlan | `gross_weight_kg` | P1 | 毛重（已有 V49） |
| LogisticsPlan | `legacy_list8_id` | P1 | 溯源 |
| LogisticsPlan | `legacy_warehouse` | P2 | 原始仓库名 |
| LogisticsPlan | `legacy_destination` | P2 | 原始目的地 |
| LogisticsPlan | `legacy_show_flag` | P2 | 归档标志 |
| Factory | `legacy_souko` | P1 | 原始 souko 值 |
| Factory | `legacy_is_warehouse` | P1 | 是否来自 list8 |

---

## 九、前端影响评估

### 9.1 采购发注页面（ProcurementPage）

- 需支持按 `legacy_order_group`（批次号）筛选
- `legacy_is_legacy=TRUE` 的记录：显示"历史"标签，编辑按钮禁用
- 日文品名 `legacy_item_name` 在商品名称字段展示

### 9.2 调配计划页面（LogisticsPlanPage）

- 需支持按 `legacy_show_flag` 筛选（活跃/归档）
- 毛重/净重列：legacy 记录显示 `grossWeightKg / netWeightKg`
- 件数列（`pieces`）：legacy 记录显示件数

### 9.3 订单概览（OrderOverview）

- `legacy_is_legacy` 记录：通过 `legacy_list1_id` 或 `legacy_order_group` 关联展示
- 历史数据：价格/日期字段可能不完整（显示 "—"）

---

## 十、附录：字段完整对照表

### A. list1 → Procurement

| list1 字段 | 是否必填 | 清洗规则 | 优先级 |
|-----------|---------|---------|--------|
| `code` | ✅ | UPPER/TRIM → productCode | P0 |
| `sub-code` | ○ | TRIM → subProductCode | P1 |
| `order-count` | ✅ | → quantity | P0 |
| `unit-ch` | ○ | kaitsuke优先，否则unit-ch → priceRmb | P1 |
| `rate` | ○ | >0 → exchangeRate | P1 |
| `hyoten` | ○ | >0 → taxPoint，else 1.1 | P2 |
| `arrival-depo` | ✅ | TRIM → destination | P0 |
| `yoyaku-hasoubi` | ○ | != 0000 → plannedShipDate | P1 |
| `departure` | ○ | != 0000 → actualShipDate | P1 |
| `material` | ○ | → material | P2 |
| `note` | ○ | → customsRemarks | P2 |
| `updater` | ○ | 保留 legacy_updater_name | P2 |
| `inspect-count` | ○ | → legacyInspectQty | P2 |
| `fba-stock` | ○ | → legacyFbaStock | P3 |
| `item-name` | ○ | → legacyItemName | P1 |
| `img` | ○ | → legacyImg | P2 |
| `houkoku` | ○ | → legacyHoukoku | P3 |
| `order-group` | ○ | → legacyOrderGroup | P1 |
| `ID` | ✅ | → legacyList1Id | P0 |

### B. list8 → LogisticsPlan

| list8 字段 | 是否必填 | 清洗规则 | 优先级 |
|-----------|---------|---------|--------|
| `code` | ✅ | UPPER/TRIM → productCode | P0 |
| `num` | ✅ | → quantity | P0 |
| `pieces` | ○ | → pieces | P1 |
| `weight` | ✅ | weight2>weight时→netWeightKg，否则→cargoWeightKg | P1 |
| `weight2` | ○ | weight2>weight → grossWeightKg | P1 |
| `length` | ✅ | → cargoLengthCm | P1 |
| `souko` | ✅ | → legacyWarehouse + factory匹配 | P1 |
| `location` | ○ | 城市信息 | P2 |
| `date1` | ○ | != 0000 → actualShipDate | P1 |
| `destination` | ✅ | → legacyDestination | P1 |
| `material` | ○ | → legacyMaterial | P2 |
| `kensa` | ○ | → legacyKensa | P2 |
| `other` | ○ | → remarks | P2 |
| `unit_ch` | ○ | → legacyUnitCh | P2 |
| `rate` | ○ | → legacyRate | P2 |
| `showFlag` | ✅ | → legacyShowFlag + 推断status | P1 |
| `status` | ○ | → legacyStatus | P2 |
| `ID` | ✅ | → legacyList8Id | P0 |
