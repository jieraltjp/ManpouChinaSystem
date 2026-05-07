# 数据库设计文档

> ManpouChinaSystem 数据库表设计 — 按业务步号索引

---

## 编号规则

| 编号 | 含义 |
|------|------|
| DB-01 ~ DB-09, DB-10 | 数据库步号（与业务步号对应；DB-10 为工厂专项扩展） |

---

## 文档索引

| 编号 | 文档 | 业务步号 | 状态 | 说明 |
|------|------|---------|------|------|
| DB-01 | [DB-01-procurement-demand.md](DB-01-procurement-demand.md) | 01 | ✅ | replenishment_demand 表 |
| DB-02 | [DB-02-procurement-order.md](DB-02-procurement-order.md) | 02 | ✅ | procurement 表 |
| DB-10 | [DB-10-factory.md](DB-10-factory.md) | 02 | 🔄 | factory 表（v1.4.0 字段扩展） |
| DB-03 | [DB-03-inspection.md](DB-03-inspection.md) | 03 | ✅ | qc_record 表 |
| DB-04 | [DB-04-logistics.md](DB-04-logistics.md) | 04 | ✅ | logistics_plan + container + consolidation_pool 均已实现 |
| DB-05 | [DB-05-domestic-customs.md](DB-05-domestic-customs.md) | 05 | ✅ | domestic_customs_record 表 |
| DB-06 | [DB-06-japan-customs.md](DB-06-japan-customs.md) | 06 | ✅ | japan_customs_record 表 |
| DB-07 | [DB-07-tax-refund.md](DB-07-tax-refund.md) | 07 | ✅ | tax_refund_record 表 |
| DB-08 | [DB-08-sales.md](DB-08-sales.md) | 08 | ✅ | sales_record 表 |
| DB-09 | [DB-09-order-overview.md](DB-09-order-overview.md) | 09 | ✅ | 聚合查询设计（无新表） |
| DB-11 | [DB-11-product.md](DB-11-product.md) | — | ✅ | product + product_factory 表 |
| DB-12 | [DB-12-procurement-factory-link.md](DB-12-procurement-factory-link.md) | — | ✅ | procurement ↔ factory 关联 |
| — | [DB-漫普中国系统数据库设计.md](DB-漫普中国系统数据库设计.md) | — | ✅ | 系统总览 |
| — | [DB-发注管理数据库设计-步骤1-4.md](DB-发注管理数据库设计-步骤1-4.md) | — | ✅ | 历史文档（步骤1-4合并版） |

---

## 业务步号映射

| 步号 | 业务文档 | UI文档 | 数据库文档 |
|------|---------|--------|-----------|
| 01 | B-01 补货需求 | `01-demand.md` | DB-01 procurement-demand |
| 02 | B-02 发注单+工厂 | `02-procurement.md` | DB-02 procurement-order |
| 03 | B-03 验货记录 | `03-inspection.md` | DB-03 inspection |
| 04 | B-04 调配计划 | `04-logistics.md` | DB-04 logistics |
| 05 | B-05 国内报关 | `05-domestic-customs.md` | DB-05 domestic-customs |
| 06 | B-06 日本清关 | `06-japan-customs.md` | DB-06 japan-customs |
| 07 | B-07 退税 | `07-tax-refund.md` | DB-07 tax-refund |
| 08 | B-08 运营销售 | `08-sales.md` | DB-08 sales |
| 09 | B-09 订单总览 | `09-order-overview.md` | DB-09 order-overview |

---

## 表清单（按步号）

### DB-01 补货需求 ✅

> `replenishment_demand` 表已移至 DB-02（与发注流程紧耦合）

### DB-02 发注单 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `procurement` | Procurement | ✅ |
| `replenishment_demand` | ReplenishmentDemand | ✅ |

### DB-10 工厂 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `factory` | Factory | ✅ |

### DB-11 商品目录 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `product` | Product | ✅ |
| `product_factory` | ProductFactory | ✅ |

### DB-03 验货记录 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `qc_record` | QcRecord | ✅ |

### DB-04 调配计划 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `logistics_plan` | LogisticsPlan | ✅ |
| `container` | Container | ✅ |
| `consolidation_pool` | ConsolidationPool | ✅ |
| `consolidation_pool_item` | ConsolidationPoolItem | ✅ |

### DB-05 国内报关 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `domestic_customs_record` | DomesticCustomsRecord | ✅ 已实现 |

### DB-06 日本清关 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `japan_customs_record` | JapanCustomsRecord | ✅ 已实现 |

### DB-07 退税 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `tax_refund_record` | TaxRefundRecord | ✅ 已实现 |

### DB-08 运营销售 ✅

| 表名 | 聚合根 | 状态 |
|------|--------|------|
| `sales_record` | SalesRecord | ✅ 已实现 |

### DB-09 订单总览 🟡

无新表；以 Procurement.id 为锚点聚合查询。

---

## 基础设施表

| 表名 | 用途 | 状态 |
|------|------|------|
| `flyway_schema_history` | Flyway 迁移历史 | 系统 |
| `outbox` | Outbox 消息投递 | ✅ |
| `saga_log` | Saga 补偿事务日志 | ✅ |
| `signing_key` | 密钥签名 | ✅ |

## 外部参考表（保留查询用）

| 编号 | 文档 | 表名 | 用途 | 状态 |
|------|------|------|------|------|
| — | [DB-HS-CN.md](DB-HS-CN.md) | `cn_hs_code` | 中国 HS 编码（12030条） | ✅ |
| — | [DB-HS-JP.md](DB-HS-JP.md) | `jp_hs_code` | 日本 HS 税番（9694条） | ✅ |
