# 数据库设计文档

> ManpouChinaSystem 数据库表设计 — 按业务步号索引

---

## 编号规则

| 编号 | 含义 |
|------|------|
| DB-01 ~ DB-09 | 数据库步号（与业务步号/UI步号一一对应） |

---

## 文档索引

| 编号 | 文档 | 业务步号 | 状态 | 说明 |
|------|------|---------|------|------|
| DB-01 | [DB-01-procurement-demand.md](DB-01-procurement-demand.md) | 01 | ✅ | replenishment_demand 表 |
| DB-02 | [DB-02-procurement-order.md](DB-02-procurement-order.md) | 02 | ✅ | procurement + factory 表 |
| DB-03 | [DB-03-inspection.md](DB-03-inspection.md) | 03 | ✅ | qc_record 表 |
| DB-04 | [DB-04-logistics.md](DB-04-logistics.md) | 04 | ✅/⚡ | logistics_plan ✅ · container/consolidation_pool ⚡占位 |
| DB-05 | [DB-05-domestic-customs.md](DB-05-domestic-customs.md) | 05 | ⚡占位 | domestic_customs_record 表 |
| DB-06 | [DB-06-japan-customs.md](DB-06-japan-customs.md) | 06 | ⚡占位 | japan_customs_record 表 |
| DB-07 | [DB-07-tax-refund.md](DB-07-tax-refund.md) | 07 | ⚡占位 | tax_refund_record 表 |
| DB-08 | [DB-08-sales.md](DB-08-sales.md) | 08 | ⚡占位 | sales_record 表 |
| DB-09 | [DB-09-order-overview.md](DB-09-order-overview.md) | 09 | 🟡设计中 | 聚合查询设计（无新表） |
| — | [DB-漫普中国系统数据库设计.md](DB-漫普中国系统数据库设计.md) | — | ✅ | 系统总览 |
| — | [DB-发注管理数据库设计-步骤1-4.md](DB-发注管理数据库设计-步骤1-4.md) | — | ✅ | 历史文档（步骤1-4合并版） |

---

## 业务步号映射

| 步号 | 业务文档 | UI文档 | 数据库文档 |
|------|---------|--------|-----------|
| 01 | B-01 补货需求 | UI-06 01-demand | DB-01 procurement-demand |
| 02 | B-02 发注单+工厂 | UI-04 02-procurement | DB-02 procurement-order |
| 03 | B-03 验货记录 | UI-07 03-inspection | DB-03 inspection |
| 04 | B-04 调配计划 | UI-05 04-logistics | DB-04 logistics |
| 05 | B-05 国内报关 | UI-08 05-domestic-customs | DB-05 domestic-customs |
| 06 | B-06 日本清关 | UI-09 06-japan-customs | DB-06 japan-customs |
| 07 | B-07 退税 | UI-10 07-tax-refund | DB-07 tax-refund |
| 08 | B-08 运营销售 | UI-11 08-sales | DB-08 sales |
| 09 | B-09 订单总览 | UI-12 09-order-overview | DB-09 order-overview |

---

## 表清单（按步号）

### DB-01 补货需求 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `replenishment_demand` | ReplenishmentDemand | ✅ |

### DB-02 发注单+工厂 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `procurement` | Procurement | ✅ |
| `factory` | Factory | ✅ |
| `product` | Product | 🟡部分 |

### DB-03 验货记录 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `qc_record` | QcRecord | ✅ |

### DB-04 调配计划 ✅/⚡

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `logistics_plan` | LogisticsPlan | ✅ |
| `container` | Container | ⚡占位 |
| `consolidation_pool` | ConsolidationPool | ⚡占位 |
| `consolidation_pool_item` | ConsolidationPoolItem | ⚡占位 |

### DB-05 国内报关 ⚡

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `domestic_customs_record` | DomesticCustomsRecord | ⚡占位 |

### DB-06 日本清关 ⚡

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `japan_customs_record` | JapanCustomsRecord | ⚡占位 |

### DB-07 退税 ⚡

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `tax_refund_record` | TaxRefundRecord | ⚡占位 |

### DB-08 运营销售 ⚡

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `sales_record` | SalesRecord | ⚡占位 |

### DB-09 订单总览 🟡

无新表；以 Procurement.id 为锚点聚合查询。
