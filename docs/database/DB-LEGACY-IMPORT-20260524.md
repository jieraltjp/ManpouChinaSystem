# 旧系统数据导入分析文档

> 日期：2026-05-24
> 源文件：`d:\Programme\database\20260524\list8.sql`、`list1.sql`
> 源库：`sddb0040100537`（192.168.12.178）

---

## 一、数据概览

| 表名 | 源记录数 | 字段数 | 引擎 | 字符集 | 说明 |
|------|----------|--------|------|--------|------|
| `list8` | ~3,661 | 26 | InnoDB | utf8 | 货柜货物装箱明细 |
| `list1` | ~71,797 | 39 | MyISAM | utf8mb4 | 订单/商品主档 |

---

## 二、list8 — 货柜货物装箱明细

### 2.1 源表结构

```sql
CREATE TABLE `list8` (
  `ID`           int(11)  PK,           -- 自增主键
  `code`         text     NOT NULL,     -- ★货号（关联list1/Procurement）
  `manager`      text     NOT NULL,     -- 担当（人名简写：段/雪/刘萌/王/张）
  `destination` text     NOT NULL,     -- 目的地（名古屋/大阪/福岡）
  `tax`          text     NOT NULL,     -- 税（多为空）
  `material`     text     NOT NULL,     -- 材质（铁/塑料/ABS/铝合金/木板等）
  `kensa`        varchar(255),          -- 检品（多为NULL）
  `num`          int(11)  NOT NULL,     -- 数量
  `pieces`       int(11)  NOT NULL,     -- 件数（与num有时相同有时不同）
  `weight`       double   NOT NULL,     -- 重量(kg)
  `weight2`      double   NOT NULL,     -- 重量2(kg)（多为0，与weight关系待确认）
  `length`       double   NOT NULL,     -- 长度(cm)
  `location`     text     NOT NULL,     -- 仓库/位置（菏泽/高碑/河北廊坊等）
  `date1`        date     NOT NULL,     -- 日期
  `status`       text     NOT NULL,     -- 状态（多为"完成"/""空）
  `other`        text     NOT NULL,     -- 其他
  `unit_ch`      double   NOT NULL,     -- 单价(元/CNY)
  `rate`         double   NOT NULL,     -- 汇率
  `souko`        text     NOT NULL,     -- 仓库名（菏泽/高碑/东莞等）
  `factory_addr` varchar(255),          -- 工厂地址（多为NULL）
  `updatetime`   datetime NOT NULL,     -- 最后更新时间
  `updateuser`   text     NOT NULL,     -- 更新人
  `showFlag`     int(11)  NOT NULL DEFAULT 0,  -- 0=归档/1=活跃
  `rireki`       longtext,             -- 历史/备注（多为NULL）
);
```

### 2.2 业务语义

**list8 = 货物装箱记录**（货物已打包，等待装柜/已装柜）

- 与 `LogisticsPlan` 最接近（productCode/quantity/cargoWeight/warehouse/destination）
- `code` = 货号，对应 `list1.code`（主商品档）
- `manager` = 担当，与 `Procurement.japanLead / chinaLead` 对应
- `location` / `souko` = 仓库/位置，与 LogisticsPlan 的 warehouse 概念对应
- `num` / `pieces` = 装箱数量/件数
- `weight` / `weight2` = 货物重量
- `length` = 长（cm）
- `showFlag` = 归档标志（0=已处理/1=待处理）
- `date1` = 装箱日期
- `unit_ch` = 采购单价(元)
- `rate` = 汇率

**数据特点：**
- 同一 `code` 有多条记录（不同颜色/规格的子项，如 `ee377-s/m/l`）
- 同一 `code` 在不同 `date1` 有重复（分期出货）
- `showFlag=0` 的记录已归档，`showFlag=1` 为活跃
- `weight2` 大量为 0，含义待确认（可能是净重/毛重分离前的单一重量字段）
- `date1` 存在 `0000-00-00` 无效日期（数据质量问题）

### 2.3 与现有实体的关联分析

| list8 字段 | 映射目标 | 映射方式 |
|-----------|---------|---------|
| `code` | `Procurement.product_code` 或 `LogisticsPlan.product_code` | 通过 code 匹配 |
| `manager` | `Procurement.japanLead` / `User.username` | 通过人名模糊匹配 |
| `destination` | `Procurement.destination` | 目的地枚举匹配 |
| `material` | `Procurement.material` | 直接映射 |
| `num` | `LogisticsPlan.quantity` | 直接映射 |
| `weight` | `LogisticsPlan.cargo_weight_kg` 或 `LogisticsPlan.gross_weight_kg` | 直接映射 |
| `length` | `LogisticsPlan.cargo_length_cm` | 直接映射（仅长，宽高缺失） |
| `location` / `souko` | `LogisticsPlan.warehouse` 或新建 `Factory.name` | 通过名称匹配 |
| `date1` | `LogisticsPlan.actual_ship_date` 或装箱日期 | 直接映射 |
| `unit_ch` | `Procurement.price_rmb` | 直接映射 |
| `rate` | `Procurement.exchange_rate` | 直接映射 |
| `updatetime` | 审计字段 | `create_time` / `update_time` |
| `updateuser` | 审计字段 | 通过人名匹配 `User.username` |
| `showFlag` | 状态筛选 | 筛选条件 |

**注意：** `LogisticsPlan` 目前没有仓库字段（warehouse/souko），宽度不够。需要扩展或新建 legacy 字段。

---

## 三、list1 — 订单/商品主档

### 3.1 源表结构

```sql
CREATE TABLE `list1` (
  `ID`              int(11)    PK,
  `lockuser-id`     int(11),            -- 锁定用户ID
  `lockuser`        text,               -- 锁定用户名
  `locktime`        datetime,            -- 锁定时间
  `updater-id`      int(11),            -- 更新人ID
  `updater`         text,               -- 更新人
  `updatetime`      datetime NOT NULL,  -- 更新时间
  `code`            text,               -- ★货号（主键外键）
  `sub-code`        text,               -- 子货号/颜色（ブルー/グリーン/红色等）
  `img`             text,               -- 图片文件名
  `item-name`       text,               -- 商品名称（日文品名，如"洞洞鞋"）
  `order-group`     text      NOT NULL, -- 订单组（批次号）
  `order-count`     int(11)   NOT NULL DEFAULT 0,  -- 订单数量
  `inspect-count`   int(11)   NOT NULL DEFAULT 0,  -- 验货数量
  `yoyaku-hasoubi`  date      NOT NULL,            -- 预约发货日
  `arrival-depo`    text      NOT NULL,            -- 到货仓库/据点
  `departure`       date      NOT NULL,            -- 出发日期
  `arrival`         date      NOT NULL,            -- 到达日期
  `arrival-jikan`   int(11)   NOT NULL DEFAULT 0,  -- 到货时间(小时)
  `arrival-flag`    int(11)   NOT NULL DEFAULT 0,  -- 到货标志
  `unit-ch`         double    NOT NULL DEFAULT 0, -- 单价(CNY)
  `total-ch`        double    NOT NULL DEFAULT 0, -- 总价(CNY)
  `unit-jp`         double    NOT NULL DEFAULT 0, -- 单价(JPY)
  `total-jp`        int(11)   NOT NULL DEFAULT 0, -- 总价(JPY)
  `rate`            double    NOT NULL DEFAULT 0, -- 汇率
  `fba-stock`       int(11)   NOT NULL DEFAULT 0, -- FBA库存
  `houkoku`         varchar(50) NOT NULL DEFAULT '', -- 报告/申报
  `kaitsuke`        decimal(10,2),       -- 買付(元) 采购价
  `hyoten`          decimal(5,4),        -- 票点
  `kanpu`           varchar(10),         -- 還付 退税
  `ne-stock`        text      NOT NULL,  -- 在库/库存
  `container`       text,               -- 货柜号
  `box-num`         text,               -- 箱号
  `box-count`       int(11)   NOT NULL DEFAULT 0, -- 箱数
  `kg`              double    NOT NULL DEFAULT 0,  -- 重量(kg)
  `one-m3`          double    NOT NULL DEFAULT 0,  -- 单件体积(m³)
  `all-m3`          double    NOT NULL DEFAULT 0,  -- 总体积(m³)
  `material`        text,               -- 材质
  `material-ch`     text,               -- 材质(中)
  `height`          double    NOT NULL DEFAULT 0, -- 高(cm)
  `width`           double    NOT NULL DEFAULT 0,  -- 宽(cm)
  `depth`           double    NOT NULL DEFAULT 0,  -- 深(cm)
  `info-file1`      text,               -- 资料文件1
  `info-file2`      text,               -- 资料文件2
  `note`            text      NOT NULL, -- 备注
  `receive`         text,               -- 收货人
);
```

### 3.2 业务语义

**list1 = 订单商品明细主档**（包含商品信息、销售价、采购价、库存、物流）

- 每行 = 一个 SKU（货号+颜色）
- `code` + `sub-code` 组合 = 商品唯一标识（对应 Procurement.productCode + subProductCode）
- `order-group` = 批次/订单组（关联多行同一批次）
- `unit-ch` / `total-ch` = 采购人民币单价/总价
- `unit-jp` / `total-jp` = 销售日元单价/总价
- `kaitsuke` = 实际采购价（元，decimal）
- `hyoten` = 票点（decimal，汇率调整系数）
- `kanpu` = 还付（退税字段）
- `height`/`width`/`depth` = 货物尺寸(cm)（三维）
- `one-m3` / `all-m3` = 单件/总体积(m³)
- `box-count` / `box-num` = 箱数/箱号
- `container` = 货柜号
- `fba-stock` = FBA库存数
- `arrival-depo` = 到货仓库/据点（与 list8.destination 不同，arrival-depo 是日本仓库）
- `yoyaku-hasoubi` = 预约发货日
- `departure` / `arrival` = 离港/到港日期

**数据特点：**
- 数据跨度：2017年至今（~8年历史）
- 大量 `order-count = inspect-count` 说明大部分已验货完成
- `unit-jp` / `unit-ch` / `rate` 完整，适合作为采购价格历史
- `0000-00-00` 无效日期问题同样存在
- `arrival-depo` 与 `destination` 在 list1/list8 中都出现，语义不同需区分

### 3.3 与现有实体的关联分析

| list1 字段 | 映射目标 | 映射方式 |
|-----------|---------|---------|
| `code` | `Procurement.product_code` | 直接匹配 |
| `sub-code` | `Procurement.sub_product_code` | 直接映射 |
| `item-name` | `Product.name` / `Product.description` | 需要新建映射 |
| `order-count` | `Procurement.quantity` | 直接映射 |
| `unit-ch` | `Procurement.price_rmb` | 直接映射 |
| `total-ch` | （计算值：unit_ch × order_count） | 可验证 |
| `rate` | `Procurement.exchange_rate` | 直接映射 |
| `kaitsuke` | `Procurement.price_rmb`（精确版） | 覆盖或优先取 |
| `hyoten` | `Procurement.tax_point` | 直接映射 |
| `kanpu` | 新建字段 `customs_rebate` | 直接映射 |
| `height`/`width`/`depth` | `LogisticsPlan.cargo_height/width_cm` | 直接映射 |
| `one-m3` | `LogisticsPlan.cargo_volume_cbm` | 直接映射 |
| `kg` | `LogisticsPlan.cargo_weight_kg` | 直接映射 |
| `box-count` | 新建字段或 `LogisticsPlan.packed_box_count` | 直接映射 |
| `container` | `Container.container_no` | 匹配 |
| `arrival-depo` | `Container.arrival_location` | 直接映射 |
| `yoyaku-hasoubi` | `Procurement.planned_ship_date` | 直接映射 |
| `departure` | `LogisticsPlan.actual_ship_date` | 直接映射 |
| `arrival` | `Container.arrival_date` | 直接映射 |
| `fba-stock` | 新建 `LogisticsPlan.fba_stock` | 直接映射 |
| `material` | `Procurement.material` | 直接映射 |
| `note` | `Procurement.customs_remarks` | 直接映射 |
| `updatetime` / `updater` | 审计字段 | 映射 |

---

## 四、两表关系分析

```
list1 (订单商品主档)
  code + sub-code  ──┐
                     ├──▶ Procurement (productCode + subProductCode)
list8 (装箱明细)      │
  code ──────────────┘

list1.order-group  ──?──▶ LogisticsPlan（或作为批次号）
list1.container    ──?──▶ Container.container_no
list8.location     ──?──▶ Factory.name / warehouse
```

**两表共有字段（可交叉验证）：**
- `code` — 货号，两表均有
- `destination`/`arrival-depo` — 目的地/到货据点
- `material` — 材质
- `updatetime` / `updater` — 审计

---

## 五、目标表设计方案

### 方案 A（推荐）：独立历史表 + 软关联

**核心思路：** 将旧表作为只读历史表导入，通过 `legacy_id` 保留原始主键，通过 `code` 与现有 `Procurement` 软关联。

#### V51：新建 legacy_import_list8

```sql
CREATE TABLE `legacy_import_list8` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `legacy_id`       INT      NOT NULL COMMENT '原list8.ID',

  -- 商品关联
  `code`            VARCHAR(64)  NOT NULL COMMENT '货号（原code）',
  `manager`         VARCHAR(64)  NOT NULL COMMENT '担当',
  `destination`     VARCHAR(128) NOT NULL COMMENT '目的地',
  `material`        VARCHAR(128) NOT NULL COMMENT '材质',
  `kensa`           VARCHAR(255) COMMENT '检品',
  `num`             INT         NOT NULL COMMENT '数量',
  `pieces`          INT         NOT NULL COMMENT '件数',

  -- 重量/尺寸
  `weight`          DECIMAL(12,4) NOT NULL COMMENT '重量(kg)',
  `weight2`         DECIMAL(12,4) NOT NULL COMMENT '重量2(kg)',
  `length_cm`       DECIMAL(8,2)  NOT NULL COMMENT '长度(cm)',

  -- 仓库/物流
  `warehouse`       VARCHAR(128) NOT NULL COMMENT '仓库（原souko+location）',
  `factory_addr`    VARCHAR(255) COMMENT '工厂地址',
  `date1`           DATE         COMMENT '装箱日期（0000-00-00转为NULL）',
  `status`          VARCHAR(64)  NOT NULL COMMENT '原状态',
  `other`           TEXT COMMENT '其他',
  `show_flag`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0=归档/1=活跃',
  `rireki`         LONGTEXT COMMENT '历史备注',

  -- 价格
  `unit_ch`         DECIMAL(12,4) NOT NULL COMMENT '单价(元)',
  `rate`            DECIMAL(10,4) NOT NULL COMMENT '汇率',

  -- 审计
  `updatetime`      DATETIME     NOT NULL,
  `updateuser`      VARCHAR(64)  NOT NULL,

  -- 关联
  `procurement_id`  BIGINT COMMENT '关联procurement.id（通过code匹配， nullable）',

  INDEX `idx_l8_code`       (`code`),
  INDEX `idx_l8_procurement`(`procurement_id`),
  INDEX `idx_l8_show_flag`  (`show_flag`),
  INDEX `idx_l8_date1`      (`date1`)
) COMMENT='list8历史数据导入（2026-05-24）';
```

#### V52：新建 legacy_import_list1

```sql
CREATE TABLE `legacy_import_list1` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `legacy_id`       INT      NOT NULL COMMENT '原list1.ID',

  -- 商品关联
  `code`            VARCHAR(64)   COMMENT '货号（原code）',
  `sub_code`        VARCHAR(64)   COMMENT '子货号/颜色（原sub-code）',
  `img`             TEXT COMMENT '图片文件名',
  `item_name`       VARCHAR(512) COMMENT '日文品名',

  -- 订单批次
  `order_group`     VARCHAR(128) NOT NULL COMMENT '订单组/批次号',
  `order_count`     INT          NOT NULL DEFAULT 0,
  `inspect_count`   INT          NOT NULL DEFAULT 0,

  -- 日期
  `yoyaku_hasoubi`  DATE COMMENT '预约发货日',
  `departure`       DATE COMMENT '离港日',
  `arrival`         DATE COMMENT '到港日',
  `arrival_jikan`   INT DEFAULT 0 COMMENT '到货时间(小时)',
  `arrival_flag`    TINYINT(1)  NOT NULL DEFAULT 0,
  `arrival_depo`    VARCHAR(128) NOT NULL COMMENT '到货仓库/据点',

  -- 价格(元)
  `unit_ch`         DECIMAL(12,4) NOT NULL DEFAULT 0,
  `total_ch`        DECIMAL(14,4) NOT NULL DEFAULT 0,
  `kaitsuke`        DECIMAL(12,2) COMMENT '買付(元)',
  `rate`            DECIMAL(10,4) NOT NULL DEFAULT 0,

  -- 价格(日元)
  `unit_jp`         DECIMAL(12,2) NOT NULL DEFAULT 0,
  `total_jp`        BIGINT NOT NULL DEFAULT 0,

  -- 票点/退税
  `hyoten`          DECIMAL(6,4) COMMENT '票点',
  `kanpu`           VARCHAR(16) COMMENT '還付',

  -- 库存
  `fba_stock`       INT NOT NULL DEFAULT 0,
  `ne_stock`        VARCHAR(64) NOT NULL COMMENT '在库状态',

  -- 货柜/装箱
  `container`       VARCHAR(64) COMMENT '货柜号',
  `box_num`         VARCHAR(128) COMMENT '箱号',
  `box_count`       INT NOT NULL DEFAULT 0,

  -- 体积/重量
  `kg`              DECIMAL(12,4) NOT NULL DEFAULT 0,
  `one_m3`          DECIMAL(10,6) NOT NULL DEFAULT 0,
  `all_m3`          DECIMAL(14,4) NOT NULL DEFAULT 0,
  `height_cm`       DECIMAL(8,2) NOT NULL DEFAULT 0,
  `width_cm`        DECIMAL(8,2) NOT NULL DEFAULT 0,
  `depth_cm`        DECIMAL(8,2) NOT NULL DEFAULT 0,

  -- 材质
  `material`        TEXT,
  `material_ch`     TEXT,

  -- 文件/备注
  `info_file1`      TEXT,
  `info_file2`      TEXT,
  `note`            TEXT NOT NULL,
  `receive`         TEXT,
  `houkoku`         VARCHAR(64) NOT NULL DEFAULT '',

  -- 审计
  `updatetime`      DATETIME NOT NULL,
  `updateuser`      VARCHAR(64),
  `updateuser_id`   INT,

  -- 关联
  `procurement_id`  BIGINT COMMENT '关联procurement.id（通过code匹配）',
  `container_id`    BIGINT COMMENT '关联container.id（通过container_no匹配）',

  INDEX `idx_l1_code`       (`code`),
  INDEX `idx_l1_order_group`(`order_group`),
  INDEX `idx_l1_procurement`(`procurement_id`),
  INDEX `idx_l1_container`  (`container`),
  INDEX `idx_l1_departure`  (`departure`),
  INDEX `idx_l1_updatetime`  (`updatetime`)
) COMMENT='list1历史数据导入（2026-05-24）';
```

---

## 六、数据清洗规则

### 6.1 日期清洗

```sql
-- list8.date1 / list1.departure / arrival / yoyaku_hasoubi
-- 0000-00-00 转换为 NULL
UPDATE legacy_import_list8 SET date1 = NULL WHERE date1 = '0000-00-00';
UPDATE legacy_import_list_list1 SET departure = NULL WHERE departure = '0000-00-00';
-- 同理处理 arrival, yoyaku_hasoubi
```

### 6.2 代码清洗

- `code`：去除空格，统一大小写（大写优先）
- `sub-code`：`text` → `VARCHAR(64)`，空值存 NULL
- `order-group`：去除前后空格

### 6.3 人名标准化

- `list8.manager` / `updateuser`：中文人名，直接作为 `VARCHAR(64)` 存储
- `list1.updater`：同上
- **需与现有 `user` 表匹配**：创建 `legacy_user_mapping` 表（old_name → user_id）

---

## 七、实施计划

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| Phase 0 | 数据质量分析：统计 null 率 / 无效日期率 / code 重复率 | **P0（先行）** |
| Phase 1 | 创建 Flyway V51 + V52 迁移脚本 | P1 |
| Phase 2 | 编写数据导入脚本（处理日期清洗 + 字符集） | P1 |
| Phase 3 | 建立 procurement 关联（通过 code 匹配） | P2 |
| Phase 4 | 建立 container 关联（通过货柜号匹配） | P2 |
| Phase 5 | 前端页面：历史数据查询（只读，按 code/manager/date 查询） | P3 |

---

## 八、数据质量评估

> 待 Phase 0 分析后填充，以下为初判：

| 问题 | 严重程度 | 处理方式 |
|------|---------|---------|
| `0000-00-00` 无效日期 | 高 | 置 NULL |
| `code` 重复（同一货号多条不同颜色记录） | 低 | 正常业务语义，保留 |
| `text` 类型过长 | 中 | 截断至 VARCHAR |
| 汇率 `rate=0` | 中 | 检查并标记 |
| `weight2` 全为 0 | 低 | 仅作参考字段 |
| `sub-code` 为空 | 低 | 允许 NULL |
| `kaitsuke` / `hyoten` 为 NULL | 中 | 允许 NULL，前端显示"-" |

---

## 九、附：字段对照总表

### list8 → legacy_import_list8 + Procurement/LogisticsPlan

| list8 字段 | 目标表 | 目标字段 | 备注 |
|-----------|-------|---------|------|
| ID | legacy_import_list8 | legacy_id | 主键 |
| code | legacy_import_list8 + Procurement | code / product_code | 软关联 |
| manager | legacy_import_list8 | manager | 待匹配user |
| destination | legacy_import_list8 | destination | |
| material | legacy_import_list8 + Procurement | material | |
| kensa | legacy_import_list8 | kensa | |
| num | legacy_import_list8 + LogisticsPlan | num / quantity | |
| pieces | legacy_import_list8 | pieces | |
| weight | legacy_import_list8 | weight | |
| weight2 | legacy_import_list8 | weight2 | |
| length | legacy_import_list8 | length_cm | |
| location | legacy_import_list8 | warehouse | |
| date1 | legacy_import_list8 | date1 | 清洗0000-00-00 |
| status | legacy_import_list8 | status | |
| other | legacy_import_list8 | other | |
| unit_ch | legacy_import_list8 + Procurement | unit_ch / price_rmb | |
| rate | legacy_import_list8 + Procurement | rate / exchange_rate | |
| souko | legacy_import_list8 | warehouse | 与location合并 |
| factory_addr | legacy_import_list8 | factory_addr | |
| updatetime | legacy_import_list8 | updatetime | |
| updateuser | legacy_import_list8 | updateuser | |
| showFlag | legacy_import_list8 | show_flag | |
| rireki | legacy_import_list8 | rireki | |

### list1 → legacy_import_list1 + Procurement/LogisticsPlan/Container

| list1 字段 | 目标表 | 目标字段 | 备注 |
|-----------|-------|---------|------|
| ID | legacy_import_list1 | legacy_id | 主键 |
| code | legacy_import_list1 + Procurement | code / product_code | 软关联 |
| sub-code | legacy_import_list1 + Procurement | sub_code / sub_product_code | |
| img | legacy_import_list1 | img | |
| item-name | legacy_import_list1 | item_name | |
| order-group | legacy_import_list1 | order_group | |
| order-count | legacy_import_list1 + Procurement | order_count / quantity | |
| inspect-count | legacy_import_list1 | inspect_count | |
| yoyaku-hasoubi | legacy_import_list1 + Procurement | yoyaku_hasoubi / planned_ship_date | |
| arrival-depo | legacy_import_list1 + Container | arrival_depo / arrival_location | |
| departure | legacy_import_list1 + LogisticsPlan | departure / actual_ship_date | |
| arrival | legacy_import_list1 + Container | arrival / arrival_date | |
| arrival-jikan | legacy_import_list1 | arrival_jikan | |
| arrival-flag | legacy_import_list1 | arrival_flag | |
| unit-ch | legacy_import_list1 + Procurement | unit_ch / price_rmb | |
| total-ch | legacy_import_list1 | total_ch | |
| unit-jp | legacy_import_list1 | unit_jp | |
| total-jp | legacy_import_list1 | total_jp | |
| rate | legacy_import_list1 + Procurement | rate / exchange_rate | |
| fba-stock | legacy_import_list1 | fba_stock | |
| houkoku | legacy_import_list1 | houkoku | |
| kaitsuke | legacy_import_list1 + Procurement | kaitsuke / price_rmb | |
| hyoten | legacy_import_list1 + Procurement | hyoten / tax_point | |
| kanpu | legacy_import_list1 | kanpu | |
| ne-stock | legacy_import_list1 | ne_stock | |
| container | legacy_import_list1 + Container | container / container_no | |
| box-num | legacy_import_list1 | box_num | |
| box-count | legacy_import_list1 | box_count | |
| kg | legacy_import_list1 + LogisticsPlan | kg / cargo_weight_kg | |
| one-m3 | legacy_import_list1 | one_m3 | |
| all-m3 | legacy_import_list1 | all_m3 | |
| material | legacy_import_list1 + Procurement | material | |
| material-ch | legacy_import_list1 | material_ch | |
| height | legacy_import_list1 + LogisticsPlan | height_cm / cargo_height_cm | |
| width | legacy_import_list1 + LogisticsPlan | width_cm / cargo_width_cm | |
| depth | legacy_import_list1 + LogisticsPlan | depth_cm / cargo_depth_cm | |
| info-file1 | legacy_import_list1 | info_file1 | |
| info-file2 | legacy_import_list1 | info_file2 | |
| note | legacy_import_list1 + Procurement | note / customs_remarks | |
| receive | legacy_import_list1 | receive | |
| updatetime | legacy_import_list1 | updatetime | |
| updater | legacy_import_list1 | updateuser | |
| updater-id | legacy_import_list1 | updateuser_id | |

---

## 十、决策事项

- [ ] **Q1**: list1 的 `order-group` 在新系统中如何对应？是作为 LogisticsPlan 的批次号还是新建 `batch_code` 字段？
- [ ] **Q2**: `list8.manager` / `list1.updater` 人名如何与现有 user 表关联？是否新建 `legacy_user_mapping`？
- [ ] **Q3**: 数据量 71k+ 是否全量导入？是否需要按日期范围（如近2年）过滤？
- [ ] **Q4**: list1 的 `fba-stock` / `ne-stock` 库存字段是否需要单独的库存表？
- [ ] **Q5**: 导入后数据是否仅供查询，还是需要能够反向修改？
  - 若仅查询 → 独立表，无业务逻辑
  - 若需修改 → 作为 Procurement/LogisticsPlan 的历史快照副本
