# DB-10 — 商品目录数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: 🟡设计阶段
> **对应业务文档**: `docs/business/SPEC-B10-商品目录-产品管理.md`
> **对应 UI 文档**: `docs/ui/pages/10-product.md`
> **历史参考**: `docs/database/sql/goods.sql`（782条原始数据）

---

## 表清单

| 序号 | 表名 | 类型 | 状态 |
|------|------|------|------|
| 1 | `product` | 聚合根 | 🟡设计阶段 |
| 2 | `product_factory` | 关联表 | 🟡设计阶段 |

---

## 1. product（商品目录）

**对应**: `Product` 聚合根

```sql
CREATE TABLE product (
    -- 主键
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 货号（复合唯一索引）
    master_code            VARCHAR(32)  NOT NULL COMMENT '主货号（如 odn012）',
    sub_code               VARCHAR(64)  COMMENT '子货号/色号（如 re=红色，可为空）',

    -- 多语言名称
    name_ja               VARCHAR(128) COMMENT '日文名称（日本用）',
    name_en               VARCHAR(255) COMMENT '英文名称（报关用）',
    name_zh               VARCHAR(255) COMMENT '中文名称（中国用）',

    -- 图片
    image_url              VARCHAR(512) COMMENT '商品图片 URL',

    -- 基础属性
    color_name            VARCHAR(64)  COMMENT '颜色名称',
    material              VARCHAR(64)  COMMENT '材质',
    category              VARCHAR(20)  COMMENT 'OEM / ORDINARY / FACTORY_DIRECT',
    origin                VARCHAR(100) COMMENT '原产国',
    unit                  VARCHAR(50)  COMMENT '计量单位（个/台/套）',

    -- 单品尺寸
    length_cm             DECIMAL(8,2) COMMENT '单品长(cm)',
    width_cm              DECIMAL(8,2) COMMENT '单品宽(cm)',
    height_cm             DECIMAL(8,2) COMMENT '单品高(cm)',
    volume_cbm            DECIMAL(10,6) COMMENT '单品体积(m³)，自动计算',

    -- 重量
    net_weight_kg         DECIMAL(10,4) COMMENT '净重(kg)',
    gross_weight_kg       DECIMAL(10,4) COMMENT '毛重(kg)',

    -- 价格
    unit_price_rmb         DECIMAL(12,4) COMMENT '含税单价(CNY)',
    tax_point              DECIMAL(5,4) DEFAULT 1.1 COMMENT '票点（默认1.1=含税）',
    tax_rate               DECIMAL(5,4) DEFAULT 0.1000 COMMENT '增值税率（默认10%）',

    -- 报关
    hs_code                VARCHAR(20) COMMENT 'HS编码（8-10位）',
    declaration_elements   TEXT COMMENT '申报要素，如：材质|用途|品牌',

    -- 外箱包装
    units_per_package      INT COMMENT '段ボール入数（每箱数量）',
    package_length_cm      DECIMAL(8,2) COMMENT '外箱长(cm)',
    package_width_cm       DECIMAL(8,2) COMMENT '外箱宽(cm)',
    package_height_cm      DECIMAL(8,2) COMMENT '外箱高(cm)',
    package_volume_cbm    DECIMAL(10,6) COMMENT '外箱体积(m³)',
    package_weight_kg     DECIMAL(10,4) COMMENT '外箱毛重(kg)',

    -- 仓库/质检
    warehouse             VARCHAR(64)  COMMENT '仓库归属',
    requires_qc           BOOLEAN COMMENT '是否需要检测',

    -- 其他
    remarks               VARCHAR(512) COMMENT '备注',
    last_used_date        DATE COMMENT '最近使用日期（来自 goods.sql.last_used）',

    -- 系统字段
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by             VARCHAR(64),
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,

    -- 索引
    UNIQUE KEY uk_master_sub (master_code, sub_code),
    INDEX idx_master_code (master_code),
    INDEX idx_hs_code (hs_code),
    INDEX idx_name_zh (name_zh)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品目录';
```

---

## 2. product_factory（商品-工厂关联）

**对应**: `ProductFactory` 关联实体（非聚合根，由 Product 聚合根管理）

```sql
CREATE TABLE product_factory (
    -- 主键
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 外键（多对多）
    product_id             BIGINT NOT NULL COMMENT 'FK → product.id',
    factory_id             BIGINT NOT NULL COMMENT 'FK → factory.id',

    -- 工厂特定属性
    supplier_sku           VARCHAR(64) COMMENT '供应商内部货号（工厂给的产品编号）',
    moq                    INT DEFAULT 1 COMMENT '最小起订量',
    lead_time_days          INT COMMENT '交货周期(天)',
    unit_price_rmb         DECIMAL(12,4) COMMENT '该工厂的含税单价（各工厂可能不同）',
    is_preferred           BOOLEAN DEFAULT FALSE COMMENT '是否为首选供应商',

    -- 系统字段
    create_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 索引与约束
    UNIQUE KEY uk_product_factory (product_id, factory_id),
    INDEX idx_product_id (product_id),
    INDEX idx_factory_id (factory_id),

    -- 外键约束
    CONSTRAINT fk_pf_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT fk_pf_factory FOREIGN KEY (factory_id) REFERENCES factory(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品-工厂多对多关联';
```

---

## 3. 数据迁移脚本

### 3.1 goods.sql → product 表

```sql
-- 货号拆分规则：sku='in041-a' → master_code='in041', sub_code='a'
-- 货号='in041'（无连字符）→ master_code='in041', sub_code=NULL

INSERT INTO product (
    master_code, sub_code,
    name_en, name_zh,
    unit_price_rmb, tax_rate,
    gross_weight_kg, net_weight_kg,
    hs_code, declaration_elements,
    units_per_package,
    origin, remarks, last_used_date,
    unit
)
SELECT
    CASE
        WHEN LOCATE('-', sku) > 0 THEN SUBSTRING_INDEX(sku, '-', 1)
        ELSE sku
    END AS master_code,
    CASE
        WHEN LOCATE('-', sku) > 0 THEN SUBSTRING(sku, LOCATE('-', sku) + 1)
        ELSE NULL
    END AS sub_code,
    name_en, name_zh,
    unit_price, tax_rate / 100,
    weight_gross / 1000, weight_net / 1000,  -- g → kg
    hs_code, declaration_elements,
    CAST(box_qty AS UNSIGNED),
    origin, remark, last_used,
    unit
FROM esagf_oem.goods
WHERE sku IS NOT NULL AND sku != '';
```

### 3.2 factory_name → product_factory 关联

> ⚠️ 需要先完成 `factory.name` 标准化，然后通过名称匹配关联。

```sql
-- 临时：按工厂名称模糊匹配（不精确，仅作迁移参考）
INSERT INTO product_factory (product_id, factory_id, is_preferred)
SELECT
    p.id,
    f.id,
    TRUE
FROM esagf_oem.goods g
JOIN product p ON p.master_code = (
    CASE WHEN LOCATE('-', g.sku) > 0 THEN SUBSTRING_INDEX(g.sku, '-', 1) ELSE g.sku END
)
JOIN factory f ON f.factory_name LIKE CONCAT('%', g.factory_name, '%')
LIMIT 100;  -- 先迁移100条验证
```

### 3.3 box_desc 解析外箱尺寸

```sql
-- 从 box_desc 中提取外箱尺寸
-- 格式示例：'一个一箱', '5个装一箱', '20个一箱'
-- 尺寸示例：'193*23*28cm'（解析为长宽高）

UPDATE product p
JOIN esagf_oem.goods g ON (
    CASE WHEN LOCATE('-', g.sku) > 0 THEN SUBSTRING_INDEX(g.sku, '-', 1) ELSE g.sku END
) = p.master_code
SET p.package_length_cm = CAST(SUBSTRING_INDEX(g.box_desc, '*', 1) AS DECIMAL(8,2));
```

---

## 4. 与 DB-02 旧 product 表的差异

| 差异 | 旧表（DB-02） | 新表（DB-10） |
|------|--------------|--------------|
| 货号结构 | `master_code` + `sub_code`（已对齐） | 同左 |
| 名称字段 | `name`（日文） | `name_ja` + `name_en` + `name_zh` |
| 重量字段 | `weight_kg`（净重） | `net_weight_kg` + `gross_weight_kg` |
| 价格字段 | 无 | `unit_price_rmb` + `tax_point` + `tax_rate` |
| HS编码 | 待新增 | `hs_code` |
| 报关要素 | 无 | `declaration_elements` |
| 外箱字段 | `package_height/width/depth` | + `package_volume_cbm` + `package_weight_kg` |
| 图片 | 无 | `image_url` |
| 原产国 | 无 | `origin` |
| 多工厂关联 | 无 | `product_factory` 关联表 |
| 系统字段 | 无 | `is_deleted` 软删除 |

---

## 5. E-R 图

```
┌──────────────────────┐
│       factory         │
├──────────────────────┤
│ PK id                │
│    factory_code       │
│    factory_name       │
│    ...                │
└──────┬───────────────┘
       │
       │ 1:N
       ▼
┌─────────────────────────────────────────┐
│          product_factory                │  多对多关联表
├─────────────────────────────────────────┤
│ PK id                                    │
│ FK product_id ──────────────────────────▶│
│ FK factory_id                            │
│    supplier_sku                          │
│    moq                                   │
│    lead_time_days                        │
│    unit_price_rmb (工厂定价)              │
│    is_preferred                          │
└──────▲──────────────────────────────────┘
       │
       │ N:1
┌──────┴──────────────────────────┐
│          product               │
├────────────────────────────────┤
│ PK id                          │
│    master_code + sub_code (UK) │
│    name_ja / name_en / name_zh │
│    image_url                   │
│    material / color_name       │
│    category / origin / unit    │
│    length/width/height/volume  │
│    net/gross_weight            │
│    unit_price_rmb / tax_point  │
│    hs_code / declaration_elems │
│    package_* fields            │
│    warehouse / requires_qc    │
│    remarks / last_used_date    │
└────────────────────────────────┘
```

---

## 6. 缺口阻塞

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 数据清洗 | **P0** | goods.sql 中部分 sku/box_desc 格式不一致 |
| 工厂名称匹配 | **P0** | factory_name 不精确，需人工核准 |
| `box_desc` 尺寸解析 | **P1** | 部分 box_desc 含尺寸（如 `193*23*28cm`）需正则提取 |
| 图片 URL | P1 | 现有数据无图片字段，需补充 |
| `sub_code` 规范 | P1 | 颜色代码标准化（re/bl/wt 等） |
