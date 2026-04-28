# DB-09 — 订单总览数据库设计

> **版本**: 2.2.0
> **更新**: 2026-04-28（v2.2.0：procurement_snapshot 补全——Factory/Product 快照字段补全至完整清单；新增 V40 迁移计划）
> **创建**: 2026-04-22
> **状态**: 🔄 Phase1 开发中
> **业务步号**: 09（订单总览 — 核心视图）
> **对应业务文档**: `SPEC-B00-全链路总览.md` · `SPEC-B09-订单总览-API设计.md`
> **对应 UI 文档**: `docs/ui/pages/09-order-overview.md`

---

## 1. 设计原则

**一张单子 = replenishment_demand 为锚点，逐步 LEFT JOIN 串联全链路8步。**

```
Demand (PENDING) ──[关联]──▶ Procurement ──▶ QcRecord ──▶ LogisticsPlan ──┬─▶ DomesticCustoms
                                                                         └─▶ JapanCustoms
                                              TaxRefund ◀──
                                              SalesRecord ◀──
```

- **1:1 链条**：Demand ↔ Procurement ↔ QcRecord ↔ LogisticsPlan → DomesticCustoms / JapanCustoms
- **侧挂表**：TaxRefund（via procurement）、SalesRecord（via procurement）
- 所有 JOIN 均为 LEFT JOIN（未到该步骤时为 NULL）

---

## 2. 数据聚合关系

```
replenishment_demand (锚点)
  └── linked_procurement_id → procurement.id
                              ├── factory_id → factory.id
                              ├── procurement_id → qc_record.id（1:1）
                              ├── qc_record.id → logistics_plan.qc_record_id（1:1）
                              │                  ├── logistics_plan_id → domestic_customs_record.id
                              │                  └── logistics_plan_id → japan_customs_record.id
                              ├── procurement_id → tax_refund_record.id
                              └── procurement_id → sales_record.id
```

---

## 3. MySQL VIEW — v_order_chain_v1

```sql
CREATE OR REPLACE VIEW v_order_chain_v1 AS
SELECT
  -- ====== 锚点：Demand（步骤1）======
  d.id                        AS demand_id,
  d.demand_code               AS demand_code,
  d.demand_type              AS demand_type,
  d.product_code             AS demand_product_code,
  d.sub_product_code          AS demand_sub_product_code,
  d.quantity                 AS demand_quantity,
  d.destination              AS demand_destination,
  d.japan_lead              AS demand_japan_lead,
  d.status                  AS demand_status,          -- PENDING / CONFIRMED
  d.linked_procurement_id   AS linked_procurement_id,
  d.image_url              AS demand_image_url,
  d.create_time            AS demand_create_time,
  d.update_time            AS demand_update_time,

  -- ====== 步骤2：发注单 Procurement ======
  p.id                        AS procurement_id,
  p.procurement_code         AS procurement_code,
  p.factory_id               AS procurement_factory_id,
  p.product_code             AS procurement_product_code,
  p.sub_product_code         AS procurement_sub_product_code,
  p.quantity                AS procurement_quantity,
  p.price_rmb               AS procurement_price_rmb,
  p.tax_point               AS procurement_tax_point,
  p.exchange_rate           AS procurement_exchange_rate,
  p.billing_type            AS procurement_billing_type,
  p.estimated_price_jpy    AS procurement_estimated_price_jpy,
  p.order_date              AS procurement_order_date,
  p.factory_ship_date       AS procurement_factory_ship_date,
  p.planned_ship_date       AS procurement_planned_ship_date,
  p.actual_ship_date        AS procurement_actual_ship_date,
  p.lead_time_days         AS procurement_lead_time_days,
  p.product_lead           AS procurement_product_lead,
  p.japan_lead             AS procurement_japan_lead,
  p.china_lead             AS procurement_china_lead,
  p.destination            AS procurement_destination,
  p.customer_company       AS procurement_customer_company,
  p.status                 AS procurement_status,
  p.create_time            AS procurement_create_time,

  -- ====== 步骤2：关联工厂 Factory ======
  f.id                        AS factory_id,
  f.factory_code             AS factory_code,
  f.factory_name             AS factory_name,
  f.province                AS factory_province,
  f.city                   AS factory_city,
  f.county                 AS factory_county,
  f.contact_name           AS factory_contact_name,
  f.contact_phone          AS factory_contact_phone,

  -- ====== 步骤3：验货记录 QcRecord ======
  q.id                        AS qc_record_id,
  q.qc_code                  AS qc_code,
  q.result                  AS qc_result,             -- PASS / FAIL
  q.inspection_count         AS qc_inspection_count,
  q.passed_count           AS qc_passed_count,
  q.defective_count         AS qc_defective_count,
  q.box_count              AS qc_box_count,
  q.box_length_cm          AS qc_box_length_cm,
  q.box_width_cm           AS qc_box_width_cm,
  q.box_height_cm          AS qc_box_height_cm,
  q.net_weight_per_unit    AS qc_net_weight_per_unit,
  q.gross_weight           AS qc_gross_weight,
  q.qc_date               AS qc_date,
  q.status                 AS qc_status,
  q.create_time            AS qc_create_time,

  -- ====== 步骤4：调配计划 LogisticsPlan ======
  l.id                        AS logistics_plan_id,
  l.plan_code                AS logistics_plan_code,
  l.container_no             AS logistics_container_no,
  l.plan_type               AS logistics_plan_type,      -- SEA / AIR / LANDING
  l.cargo_length_cm         AS logistics_cargo_length_cm,
  l.cargo_width_cm          AS logistics_cargo_width_cm,
  l.cargo_height_cm         AS logistics_cargo_height_cm,
  l.cargo_volume_cbm       AS logistics_cargo_volume_cbm,
  l.cargo_weight_kg        AS logistics_cargo_weight_kg,
  l.requires_qc            AS logistics_requires_qc,
  l.estimated_ship_date     AS logistics_estimated_ship_date,
  l.actual_ship_date        AS logistics_actual_ship_date,
  l.status                 AS logistics_status,
  l.create_time            AS logistics_create_time,

  -- ====== 步骤5：国内报关 DomesticCustomsRecord ======
  dc.id                       AS domestic_customs_id,
  dc.customs_no             AS domestic_customs_no,
  dc.hs_code                AS domestic_hs_code,
  dc.declaration_date       AS domestic_declaration_date,
  dc.export_port            AS domestic_export_port,
  dc.declared_value_rmb    AS domestic_declared_value_rmb,
  dc.status                 AS domestic_customs_status,
  dc.create_time            AS domestic_create_time,

  -- ====== 步骤6：日本清关 JapanCustomsRecord ======
  jc.id                       AS japan_customs_id,
  jc.customs_entry_no       AS japan_customs_entry_no,
  jc.arrival_date           AS japan_arrival_date,
  jc.customs_broker         AS japan_customs_broker,
  jc.broker_phone          AS japan_broker_phone,
  jc.import_duty_paid       AS japan_import_duty_paid,
  jc.consumption_tax_paid  AS japan_consumption_tax_paid,
  jc.clearance_date          AS japan_clearance_date,
  jc.status                 AS japan_customs_status,
  jc.create_time            AS japan_create_time,

  -- ====== 步骤7：退税 TaxRefundRecord ======
  tr.id                       AS tax_refund_id,
  tr.tax_refund_amount      AS tax_refund_amount,
  tr.refund_date            AS tax_refund_date,
  tr.status                 AS tax_refund_status,
  tr.create_time            AS tax_refund_create_time,

  -- ====== 步骤8：运营销售 SalesRecord ======
  sr.id                       AS sales_record_id,
  sr.record_code             AS sales_record_code,
  sr.sales_channel          AS sales_channel,
  sr.listing_date           AS sales_listing_date,
  sr.initial_stock         AS sales_initial_stock,
  sr.current_stock         AS sales_current_stock,
  sr.safety_stock          AS sales_safety_stock,
  sr.sales_quantity        AS sales_quantity,
  sr.returned_quantity     AS sales_returned_quantity,
  sr.return_rate           AS sales_return_rate,
  sr.selling_price_jpy     AS sales_selling_price_jpy,
  sr.status                 AS sales_status,
  sr.create_time            AS sales_create_time,

  -- ====== 商品基础信息（来自 product 表）======
  prd.name_zh               AS product_name_zh,
  prd.name_ja               AS product_name_ja,
  prd.category             AS product_category,       -- OEM / ORDINARY / FACTORY_DIRECT

  -- ====== 8步状态汇总（一目了然）======
  CASE WHEN d.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step1_status,
  CASE WHEN p.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step2_status,
  CASE WHEN q.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step3_status,
  CASE WHEN l.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step4_status,
  CASE WHEN dc.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step5_status,
  CASE WHEN jc.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step6_status,
  CASE WHEN tr.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step7_status,
  CASE WHEN sr.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step8_status

FROM replenishment_demand d
  -- 步骤2：Procurement（通过 demand.linked_procurement_id 关联）
  LEFT JOIN procurement p ON p.id = d.linked_procurement_id AND p.is_deleted = FALSE
  -- 步骤2：关联工厂（通过 procurement.factory_id）
  LEFT JOIN factory f ON f.id = p.factory_id AND f.is_deleted = FALSE
  -- 步骤3：验货记录（通过 procurement.id）
  LEFT JOIN qc_record q ON q.procurement_id = p.id AND q.is_deleted = FALSE
  -- 步骤4：调配计划（通过 qc_record.id 作为锚点）
  LEFT JOIN logistics_plan l ON l.qc_record_id = q.id AND l.is_deleted = FALSE
  -- 步骤5：国内报关（通过 logistics_plan.id）
  LEFT JOIN domestic_customs_record dc ON dc.logistics_plan_id = l.id AND dc.is_deleted = FALSE
  -- 步骤6：日本清关（通过 logistics_plan.id）
  LEFT JOIN japan_customs_record jc ON jc.logistics_plan_id = l.id AND jc.is_deleted = FALSE
  -- 步骤7：退税（直接挂 procurement）
  LEFT JOIN tax_refund_record tr ON tr.procurement_id = p.id AND tr.is_deleted = FALSE
  -- 步骤8：运营销售（直接挂 procurement）
  LEFT JOIN sales_record sr ON sr.procurement_id = p.id AND sr.is_deleted = FALSE
  -- 商品基础信息（通过主货号）
  LEFT JOIN product prd ON prd.master_code = d.product_code AND prd.sub_code IS NULL AND prd.is_deleted = FALSE

WHERE d.is_deleted = FALSE;
```

---

## 4. procurement_snapshot 表（发注单快照）

> **v2.2.0 新增**：记录下单时刻的工厂和商品信息，保证历史订单数据不变。
> 由 `ProcurementUseCase` 在创建发注单时自动写入，允许事后修改。
> Entity: `com.manpou.allinone.order.domain.model.ProcurementSnapshot`

### 4.1 完整字段清单

| 分类 | 字段名 | 类型 | 说明 | 状态 |
|------|--------|------|------|------|
| **主键** | `id` | BIGINT | 自增主键 | ✅ 已有 |
| **关联** | `procurement_id` | BIGINT | 发注单ID（唯一） | ✅ 已有 |
| **工厂-基本信息** | `factory_id` | BIGINT | 工厂ID | ✅ 已有 |
| | `factory_code` | VARCHAR(32) | 工厂编号 | ✅ 已有 |
| | `factory_name` | VARCHAR(128) | 工厂名称 | ✅ 已有 |
| **工厂-地理** | `factory_province` | VARCHAR(64) | 省 | ✅ 已有 |
| | `factory_city` | VARCHAR(64) | 市 | ⚠️ 缺 |
| | `factory_county` | VARCHAR(64) | 县/区 | ⚠️ 缺 |
| | `factory_rough_location` | VARCHAR(500) | 详细地址 | ⚠️ 缺 |
| | `factory_longitude` | DECIMAL(11,8) | 经度 | ⚠️ 缺 |
| | `factory_latitude` | DECIMAL(11,8) | 纬度 | ⚠️ 缺 |
| **工厂-联系** | `factory_contact_name` | VARCHAR(64) | 联系人 | ✅ 已有 |
| | `factory_contact_phone` | VARCHAR(32) | 手机号 | ✅ 已有 |
| | `factory_contact_wechat` | VARCHAR(64) | 微信号 | ⚠️ 缺 |
| | `factory_contact_qq` | VARCHAR(32) | QQ号 | ⚠️ 缺 |
| **工厂-合作** | `factory_category` | VARCHAR(32) | 分类（枚举） | ⚠️ 缺 |
| | `factory_cooperation_status` | VARCHAR(32) | 合作状态 | ⚠️ 缺 |
| | `factory_payment_terms` | VARCHAR(64) | 账期 | ⚠️ 缺 |
| **商品-基本信息** | `product_master_code` | VARCHAR(32) | 主货号 | ⚠️ 缺 |
| | `product_sub_code` | VARCHAR(64) | 子货号 | ⚠️ 缺 |
| | `product_jan_code` | VARCHAR(64) | JAN码 | ⚠️ 缺 |
| | `product_name_zh` | VARCHAR(255) | 中文名称 | ✅ 已有 |
| | `product_name_ja` | VARCHAR(128) | 日文名称 | ✅ 已有 |
| | `product_name_en` | VARCHAR(255) | 英文名称 | ⚠️ 缺 |
| **商品-属性** | `product_category` | VARCHAR(32) | 分类 | ✅ 已有 |
| | `product_origin` | VARCHAR(100) | 原产国 | ⚠️ 缺 |
| | `product_material` | VARCHAR(64) | 材质 | ⚠️ 缺 |
| | `product_unit` | VARCHAR(50) | 计量单位 | ⚠️ 缺 |
| **商品-报关** | `product_hs_code` | VARCHAR(20) | 中国HS编码 | ⚠️ 缺 |
| | `product_hs_code_jp` | VARCHAR(20) | 日本HS编码 | ⚠️ 缺 |
| **商品-价格** | `product_unit_price_rmb` | DECIMAL(12,4) | 单价(CNY) | ⚠️ 缺 |
| | `product_tax_point` | DECIMAL(5,4) | 票点 | ⚠️ 缺 |
| **商品-图片** | `product_image_url` | VARCHAR(512) | 商品图片URL | ⚠️ 缺 |

> **图例**: ✅ = 已有字段，⚠️ = 缺失字段（需 V40 迁移添加）

### 4.2 迁移计划（V40）

```sql
-- V40：procurement_snapshot 补全 Factory/Product 快照字段
ALTER TABLE procurement_snapshot
  ADD COLUMN factory_city            VARCHAR(64)  DEFAULT '' COMMENT '市',
  ADD COLUMN factory_county         VARCHAR(64)  DEFAULT '' COMMENT '县/区',
  ADD COLUMN factory_rough_location VARCHAR(500) COMMENT '详细地址',
  ADD COLUMN factory_longitude      DECIMAL(11,8) COMMENT '经度',
  ADD COLUMN factory_latitude      DECIMAL(11,8) COMMENT '纬度',
  ADD COLUMN factory_contact_wechat VARCHAR(64)   DEFAULT '' COMMENT '微信号',
  ADD COLUMN factory_contact_qq    VARCHAR(32)   DEFAULT '' COMMENT 'QQ号',
  ADD COLUMN factory_category       VARCHAR(32)   DEFAULT '' COMMENT '分类',
  ADD COLUMN factory_cooperation_status VARCHAR(32) DEFAULT 'POTENTIAL' COMMENT '合作状态',
  ADD COLUMN factory_payment_terms VARCHAR(64)   DEFAULT 'NET_30' COMMENT '账期',
  ADD COLUMN product_master_code   VARCHAR(32)   DEFAULT '' COMMENT '主货号',
  ADD COLUMN product_sub_code       VARCHAR(64)   DEFAULT '' COMMENT '子货号',
  ADD COLUMN product_jan_code       VARCHAR(64)   DEFAULT '' COMMENT 'JAN码',
  ADD COLUMN product_name_en        VARCHAR(255)  DEFAULT '' COMMENT '英文名称',
  ADD COLUMN product_origin        VARCHAR(100)  DEFAULT '' COMMENT '原产国',
  ADD COLUMN product_material      VARCHAR(64)   DEFAULT '' COMMENT '材质',
  ADD COLUMN product_unit          VARCHAR(50)   DEFAULT '' COMMENT '计量单位',
  ADD COLUMN product_hs_code      VARCHAR(20)   DEFAULT '' COMMENT '中国HS编码',
  ADD COLUMN product_hs_code_jp   VARCHAR(20)   DEFAULT '' COMMENT '日本HS编码',
  ADD COLUMN product_unit_price_rmb DECIMAL(12,4) COMMENT '单价(CNY)',
  ADD COLUMN product_tax_point     DECIMAL(5,4) COMMENT '票点',
  ADD COLUMN product_image_url     VARCHAR(512)  DEFAULT '' COMMENT '商品图片URL';

-- 回填历史数据（基于 procurement.id 关联 factory/product 表）
UPDATE procurement_snapshot sn
  INNER JOIN procurement p ON p.id = sn.procurement_id
  INNER JOIN factory f ON f.id = p.factory_id
  INNER JOIN product prd ON prd.master_code = p.product_code
    AND (prd.sub_code IS NULL OR prd.sub_code = p.sub_product_code)
SET
  sn.factory_city = f.city,
  sn.factory_county = f.county,
  sn.factory_rough_location = f.rough_location,
  sn.factory_longitude = f.longitude,
  sn.factory_latitude = f.latitude,
  sn.factory_contact_wechat = f.contact_wechat,
  sn.factory_contact_qq = f.contact_qq,
  sn.factory_category = f.category,
  sn.factory_cooperation_status = f.cooperation_status,
  sn.factory_payment_terms = f.payment_terms,
  sn.product_master_code = prd.master_code,
  sn.product_sub_code = prd.sub_code,
  sn.product_jan_code = prd.jan_code,
  sn.product_name_en = prd.name_en,
  sn.product_origin = prd.origin,
  sn.product_material = prd.material,
  sn.product_unit = prd.unit,
  sn.product_hs_code = prd.hs_code,
  sn.product_hs_code_jp = prd.hs_code_jp,
  sn.product_unit_price_rmb = prd.unit_price_rmb,
  sn.product_tax_point = prd.tax_point,
  sn.product_image_url = prd.image_url
WHERE sn.factory_id IS NOT NULL;

-- 同步更新 ProcurementSnapshot.java 实体补全所有字段
-- 同步更新 ProcurementUseCase.java 填充新字段
-- 同步更新 OrderChainView.java / OrderChainDetailVO.java
```

---

## 5. 索引建议

```sql
-- 核心查询索引（Demand 列表 + 8步状态）
CREATE INDEX idx_chain_demand_status ON replenishment_demand(status, is_deleted);
CREATE INDEX idx_chain_linked_proc ON replenishment_demand(linked_procurement_id);

-- 逐步JOIN索引
CREATE INDEX idx_chain_procurement_del ON procurement(is_deleted, id);
CREATE INDEX idx_chain_qc_proc ON qc_record(procurement_id, is_deleted);
CREATE INDEX idx_chain_logistics_qc ON logistics_plan(qc_record_id, is_deleted);
CREATE INDEX idx_chain_domestic_lp ON domestic_customs_record(logistics_plan_id, is_deleted);
CREATE INDEX idx_chain_japan_lp ON japan_customs_record(logistics_plan_id, is_deleted);
CREATE INDEX idx_chain_tax_proc ON tax_refund_record(procurement_id, is_deleted);
CREATE INDEX idx_chain_sales_proc ON sales_record(procurement_id, is_deleted);
```

---

## 6. 前端使用方式

### 5.1 列表页（单一表格，一行 = 一个 Demand）

```
GET /api/v1/orders/chain?page=&pageSize=&demandStatus=&keyword=

响应：Page<OrderChainVO>
每行 = v_order_chain 的一行
```

### 5.2 详情页（点击行后进入）

```
GET /api/v1/orders/chain/{demandId}

响应：OrderChainVO（含全部8步数据，NULL=未到该步骤）
```

---

## 7. 分阶段实现计划

### Phase 1：步骤 1~4（补货 → 发注 → 验货 → 调配）

| # | 项目 | 状态 | 说明 |
|---|------|------|------|
| 1 | `v_order_chain_v1` MySQL VIEW（仅前4步+工厂） | ✅ 已建 | 视图包含 Demand/Procurement/Factory/QcRecord/LogisticsPlan |
| 2 | `OrderChainView` JPA Entity | ✅ 已建 | 映射 `v_order_chain_v1` 视图 |
| 3 | `OrderChainRepository` | ✅ 已建 | 分页查询 + 详情查询 |
| 4 | `OrderChainUseCase` | ✅ 已建 | 业务逻辑 |
| 5 | `OrderChainController` | ✅ 已建 | `/api/v1/orders/chain` |
| 6 | `OrderChainVO` / `OrderChainDetailVO` | ✅ 已建 | 响应对象 |
| 7 | 前端 API 层 `orderChain.ts` | 🔲 待建 | |
| 8 | 前端 `OrderOverviewPage.vue` 单列表 | 🔲 待改 | 移除双 Tab，改为单列表 |

> **Phase 1 JOIN 范围**（FROM replenishment_demand LEFT JOIN ...）：
> `procurement` → `factory` → `qc_record` → `logistics_plan`
> step5-step8 列在 Phase 1 视图中返回 NULL 占位。

### Phase 2：步骤 5~8（国内报关 → 日本清关 → 退税 → 运营销售）

| # | 项目 | 状态 |
|---|------|------|
| 9 | VIEW 升级为完整 8 步 | 🔲 待建 |
| 10 | DomesticCustoms / JapanCustoms / TaxRefund / SalesRecord 字段补全 | 🔲 待建 |
| 11 | 详情抽屉全 8 步展示 | 🔲 待改 |

---

## 8. Phase 1 视图 SQL（步骤 1~4 + 快照工厂/商品）

> **v2.0.0 变更（2026-04-28）**：`factory` / `product` 表 JOIN 改为 `procurement_snapshot`（下单时刻快照，历史数据不变）

```sql
CREATE OR REPLACE VIEW v_order_chain_v1 AS
SELECT
  -- ====== 锚点：Demand（步骤1）======
  d.id                           AS demand_id,
  d.demand_code                  AS demand_code,
  d.demand_type                  AS demand_type,
  d.product_code                 AS demand_product_code,
  d.sub_product_code             AS demand_sub_product_code,
  d.quantity                     AS demand_quantity,
  d.destination                  AS demand_destination,
  d.japan_lead                   AS demand_japan_lead,
  d.status                       AS demand_status,
  d.linked_procurement_id        AS linked_procurement_id,
  d.image_url                    AS demand_image_url,
  d.create_time                  AS demand_create_time,
  d.update_time                  AS demand_update_time,

  -- ====== 步骤2：Procurement ======
  p.id                           AS procurement_id,
  CONCAT('P-', CAST(p.id AS CHAR)) AS procurement_code,
  p.factory_id                   AS procurement_factory_id,
  p.product_code                 AS procurement_product_code,
  p.sub_product_code             AS procurement_sub_product_code,
  p.quantity                     AS procurement_quantity,
  p.price_rmb                    AS procurement_price_rmb,
  p.tax_point                    AS procurement_tax_point,
  p.exchange_rate                AS procurement_exchange_rate,
  p.billing_type                 AS procurement_billing_type,
  p.estimated_price_jpy           AS procurement_estimated_price_jpy,
  p.order_date                   AS procurement_order_date,
  p.factory_ship_date            AS procurement_factory_ship_date,
  p.planned_ship_date            AS procurement_planned_ship_date,
  p.actual_ship_date             AS procurement_actual_ship_date,
  p.lead_time_days               AS procurement_lead_time_days,
  p.product_lead                 AS procurement_product_lead,
  p.japan_lead                   AS procurement_japan_lead,
  p.china_lead                   AS procurement_china_lead,
  p.destination                  AS procurement_destination,
  p.customer_company             AS procurement_customer_company,
  p.status                       AS procurement_status,
  p.create_time                  AS procurement_create_time,

  -- ====== 工厂快照（来自 procurement_snapshot，下单时刻不变）======
  sn.factory_id                  AS snapshot_factory_id,
  sn.factory_code                AS snapshot_factory_code,
  sn.factory_name                AS snapshot_factory_name,
  sn.factory_province            AS snapshot_factory_province,
  sn.factory_city                AS snapshot_factory_city,
  sn.factory_contact_name        AS snapshot_factory_contact_name,
  sn.factory_contact_phone       AS snapshot_factory_contact_phone,

  -- ====== 商品快照（来自 procurement_snapshot，下单时刻不变）======
  sn.product_name_zh             AS snapshot_product_name_zh,
  sn.product_name_ja             AS snapshot_product_name_ja,
  sn.product_category            AS snapshot_product_category,

  -- ====== 步骤3：验货记录 ======
  q.id                           AS qc_record_id,
  q.qc_code                     AS qc_code,
  q.result                      AS qc_result,
  q.inspection_count             AS qc_inspection_count,
  q.passed_count                AS qc_passed_count,
  q.defective_count             AS qc_defective_count,
  q.box_count                   AS qc_box_count,
  q.box_length_cm               AS qc_box_length_cm,
  q.box_width_cm                AS qc_box_width_cm,
  q.box_height_cm               AS qc_box_height_cm,
  q.net_weight_per_unit         AS qc_net_weight_per_unit,
  q.gross_weight                AS qc_gross_weight,
  q.qc_date                     AS qc_date,
  q.status                      AS qc_status,
  q.create_time                 AS qc_create_time,

  -- ====== 步骤4：调配计划 ======
  l.id                           AS logistics_plan_id,
  l.plan_code                   AS logistics_plan_code,
  l.container_no                AS logistics_container_no,
  l.plan_type                   AS logistics_plan_type,
  l.cargo_length_cm             AS logistics_cargo_length_cm,
  l.cargo_width_cm              AS logistics_cargo_width_cm,
  l.cargo_height_cm             AS logistics_cargo_height_cm,
  l.cargo_volume_cbm            AS logistics_cargo_volume_cbm,
  l.cargo_weight_kg             AS logistics_cargo_weight_kg,
  l.requires_qc                 AS logistics_requires_qc,
  l.estimated_ship_date          AS logistics_estimated_ship_date,
  l.actual_ship_date            AS logistics_actual_ship_date,
  l.status                      AS logistics_status,
  l.create_time                 AS logistics_create_time,

  -- ====== 4步状态汇总 =======
  CASE WHEN d.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step1_status,
  CASE WHEN p.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step2_status,
  CASE WHEN q.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step3_status,
  CASE WHEN l.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step4_status,
  -- step5-step8 Phase1 占位
  NULL AS step5_status,
  NULL AS step6_status,
  NULL AS step7_status,
  NULL AS step8_status,

  -- ====== Phase1 步骤5~8 字段占位（与 OrderChainView.entity 对齐）======
  NULL AS domestic_customs_id,
  NULL AS domestic_customs_status,
  NULL AS japan_customs_id,
  NULL AS japan_customs_status,
  NULL AS tax_refund_id,
  NULL AS tax_refund_status,
  NULL AS sales_record_id,
  NULL AS sales_status

FROM replenishment_demand d
  LEFT JOIN procurement p ON p.id = d.linked_procurement_id AND p.is_deleted = b'0'
  -- 工厂+商品快照（通过 procurement.id → procurement_snapshot.procurement_id）
  LEFT JOIN procurement_snapshot sn ON sn.procurement_id = p.id
  -- 步骤3：验货记录（通过 procurement.id）
  LEFT JOIN qc_record q ON q.procurement_id = p.id AND q.is_deleted = b'0'
  -- 步骤4：调配计划（通过 qc_record.id 作为锚点）
  LEFT JOIN logistics_plan l ON l.qc_record_id = q.id AND l.is_deleted = b'0'

WHERE d.is_deleted = b'0';
```

### Phase 1 索引

```sql
-- 核心查询索引
CREATE INDEX idx_chain_demand_status ON replenishment_demand(status, is_deleted);
CREATE INDEX idx_chain_linked_proc ON replenishment_demand(linked_procurement_id);
CREATE INDEX idx_chain_procurement_del ON procurement(is_deleted, id);
CREATE INDEX idx_chain_qc_proc ON qc_record(procurement_id, is_deleted);
CREATE INDEX idx_chain_logistics_qc ON logistics_plan(qc_record_id, is_deleted);
```

> **Phase 2 升级**：ALTER VIEW 追加步骤 5~8 列 + 新增索引 `idx_chain_domestic_lp` / `idx_chain_japan_lp` / `idx_chain_tax_proc` / `idx_chain_sales_proc`
