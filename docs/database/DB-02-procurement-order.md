# DB-02 — 发注单数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **业务步号**: 02（发注单+工厂）
> **对应业务文档**: `SPEC-B02-发注单-步骤2.md`
> **对应 UI 文档**: `docs/ui/pages/02-procurement.md`
> **对应后端聚合根**: `Procurement` · `Factory`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `procurement` | Procurement | ✅ 已实现 |
| 2 | `factory` | Factory | ✅ 已实现（字段更新见 DB-10） |
| 3 | `product` | Product | 🟡 部分实现 |

---

## 1. procurement（发注单）

**对应**: `Procurement` 聚合根

```sql
CREATE TABLE procurement (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    factory_id           BIGINT COMMENT '关联工厂 FK → factory.id',
    product_code         VARCHAR(32)  NOT NULL COMMENT '主货号',
    sub_product_code    VARCHAR(64) COMMENT '子货号/颜色（如 re=红色）',
    material             VARCHAR(64) COMMENT '材质',
    requires_qc         BOOLEAN COMMENT '是否需要检测',
    quantity             INT          NOT NULL COMMENT '订购数量',
    price_rmb           DECIMAL(12,2) NOT NULL COMMENT '人民币单价',
    exchange_rate        DECIMAL(10,4) NOT NULL COMMENT 'CNY→JPY 汇率',
    tax_point            DECIMAL(5,4) NOT NULL COMMENT '票点（默认 1.1）',
    billing_type         VARCHAR(32) COMMENT '报关类型 ZHE_LU_KAI_PIAO / CHAO_HUI_TUI_SHUI / NO_REFUND / OTHER',
    estimated_price_jpy  DECIMAL(14,2) COMMENT '估算批发价 JPY（自动计算）',
    customs_remarks      VARCHAR(512) COMMENT '报关备注',
    instruction_manual   TEXT COMMENT '说明书',
    order_date          DATE COMMENT '下单日',
    factory_ship_date   DATE COMMENT '厂家出货日',
    planned_ship_date   DATE COMMENT '预计出货日（交货期）',
    actual_ship_date    DATE COMMENT '实际出货日',
    product_lead        VARCHAR(64) COMMENT '商品担当',
    japan_lead          VARCHAR(64) COMMENT '日本担当',
    china_lead          VARCHAR(64) COMMENT '中国担当',
    destination         VARCHAR(128) COMMENT '发送目的地',
    customer_company     VARCHAR(128) COMMENT '客户公司',
    status              VARCHAR(32)  NOT NULL DEFAULT '未定' COMMENT '19态',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by           VARCHAR(64)  NOT NULL,
    update_by           VARCHAR(64)  NOT NULL,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    INDEX idx_procurement_product_code (product_code),
    INDEX idx_procurement_status (status),
    INDEX idx_procurement_create_time (create_time),
    INDEX idx_procurement_factory_id (factory_id),
    INDEX idx_procurement_sub_product_code (sub_product_code),
    INDEX idx_procurement_order_date (order_date)
);
```

---

## 2. factory（工厂）

**对应**: `Factory` 聚合根（完整设计见 `DB-10-factory.md`）

> **重大更新（v1.4.0）**: `location` 拆分为 `province`/`city`/`county`；`status` 替换为 `cooperationStatus`（CooperationStatus）；新增 `category`/`paymentTerms`/`contactWechat`/`contactQq`/`longitude`/`latitude` 等字段。详见 `DB-10-factory.md`。

---

## 3. product（商品目录）🟡部分

**对应**: `Product` 聚合根（完整设计见 `DB-11-product.md`）

> 本节列出步骤2发注单直接引用的核心字段。完整字段（含 hs_code、tax_point、unit_price_rmb、product_factory 多对多）见 `DB-11-product.md`。

```sql
-- 步骤2直接引用字段
CREATE TABLE product (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    master_code        VARCHAR(32)  NOT NULL COMMENT '主货号（如 odn012）',
    sub_code           VARCHAR(64)  COMMENT '子货号/色号（如 re=红色）',
    name               VARCHAR(128) COMMENT '日文名称',
    name_zh            VARCHAR(128) COMMENT '中文名称',
    name_en            VARCHAR(128) COMMENT '英文名称',
    color_name         VARCHAR(64)  COMMENT '颜色名称',
    material           VARCHAR(64)  COMMENT '材质',
    product_category   VARCHAR(20)  COMMENT 'OEM / ORDINARY / FACTORY_DIRECT',
    length_cm          DECIMAL(8,2) COMMENT '单品长(cm)',
    width_cm           DECIMAL(8,2) COMMENT '单品宽(cm)',
    height_cm          DECIMAL(8,2) COMMENT '单品高(cm)',
    weight_kg          DECIMAL(10,4) COMMENT '单品净重(kg)',
    units_per_package  INT COMMENT '段ボール入数',
    package_height_cm  DECIMAL(8,2) COMMENT '外箱高(cm)',
    package_width_cm   DECIMAL(8,2) COMMENT '外箱宽(cm)',
    package_depth_cm   DECIMAL(8,2) COMMENT '外箱深(cm)',
    package_weight_kg  DECIMAL(10,4) COMMENT '外箱毛重',
    warehouse          VARCHAR(64)  COMMENT '仓库归属',
    requires_qc        BOOLEAN COMMENT '是否需要检测',
    remarks            VARCHAR(512) COMMENT '备注',
    update_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by          VARCHAR(64),
    UNIQUE KEY uk_master_sub (master_code, sub_code),
    INDEX idx_master_code (master_code)
);

-- 完整字段见 DB-11-product.md：
-- hs_code / tax_point / unit_price_rmb / name_ja / gross_weight_kg
-- declaration_elements / image_url / origin / product_factory 多对多关联表
```

---

## 代码实现状态

- [x] ✅ `Procurement` 聚合根实体
- [x] ✅ `Factory` 聚合根实体
- [x] ✅ `Product` 聚合根实体（部分字段）
- [x] ✅ `ShipmentStatus` 枚举（19态含FSM）
- [x] ✅ `BillingType` 枚举
- [x] ✅ `ProcurementRepository` 领域接口 + JPA 适配器
- [x] ✅ `FactoryRepository` 领域接口 + JPA 适配器
- [x] ✅ `ProcurementUseCase` 用例服务
- [x] ✅ `FactoryUseCase` 用例服务
- [x] ✅ `ProcurementUseCaseTest` 14个用例全部通过
- [x] ✅ `FactoryUseCaseTest` 8个用例全部通过
- [ ] 🔴 Product 表新增 `hs_code` 字段
- [ ] 🔴 Product 表新增 `tax_point` 字段
