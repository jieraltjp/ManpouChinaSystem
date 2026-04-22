# DB-09 — 订单总览数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: 🟡 设计中
> **业务步号**: 09（订单总览 — 核心视图）
> **对应业务文档**: `SPEC-B01-全链路总览.md` · `SPEC-订单总览-API设计.md`
> **对应 UI 文档**: `docs/ui/pages/11-order-overview.md`
> **说明**: 订单总览不新增表，而是以 Procurement.id 为锚点，聚合查询全链路 8 步数据。

---

## 1. 数据聚合关系

```
Procurement (锚点)
  ├── ReplenishmentDemand   (步骤1)  → procurement.replenishment_demand_id
  ├── Factory              (步骤2)  → procurement.factory_id
  ├── QcRecord             (步骤3)  → qc_record.procurement_id
  ├── LogisticsPlan         (步骤4)  → logistics_plan.procurement_id
  │     ├── Container      (步骤4)  → logistics_plan.container_id
  │     └── ConsolidationPool (步骤4) → logistics_plan.pool_id
  ├── DomesticCustomsRecord (步骤5)  → domestic_customs_record.procurement_id
  ├── JapanCustomsRecord    (步骤6)  → japan_customs_record.procurement_id
  ├── TaxRefundRecord       (步骤7)  → tax_refund_record.procurement_id
  └── SalesRecord           (步骤8)  → sales_record.procurement_id
```

---

## 2. 聚合查询 SQL（步骤1-4 已实现，步骤5-8 占位）

```sql
-- 锚点 Procurement
SELECT
    p.id                         AS procurement_id,
    p.factory_id,
    p.product_code,
    p.sub_product_code,
    p.quantity,
    p.price_rmb,
    p.tax_point,
    p.exchange_rate,
    p.billing_type,
    p.estimated_price_jpy,
    p.status,
    p.order_date,
    p.factory_ship_date,
    p.planned_ship_date,
    p.actual_ship_date,
    p.product_lead,
    p.japan_lead,
    p.china_lead,
    p.destination,
    p.customer_company,
    p.create_time
FROM procurement p
WHERE p.id = :procurementId AND p.is_deleted = FALSE;

-- 步骤1：ReplenishmentDemand（JOIN via linked_procurement_id）
SELECT d.* FROM replenishment_demand d
WHERE d.linked_procurement_id = :procurementId AND d.is_deleted = FALSE
LIMIT 1;

-- 步骤2：Factory
SELECT f.* FROM factory f
WHERE f.id = (SELECT factory_id FROM procurement WHERE id = :procurementId)
  AND f.is_deleted = FALSE
LIMIT 1;

-- 步骤3：QcRecord
SELECT q.* FROM qc_record q
WHERE q.procurement_id = :procurementId AND q.is_deleted = FALSE
ORDER BY q.create_time DESC
LIMIT 1;

-- 步骤4：LogisticsPlan
SELECT l.* FROM logistics_plan l
WHERE l.procurement_id = :procurementId AND l.is_deleted = FALSE
ORDER BY l.create_time DESC
LIMIT 1;

-- 步骤5-8：待实现（字段占位）
-- domestic_customs_record.procurement_id = :procurementId
-- japan_customs_record.procurement_id = :procurementId
-- tax_refund_record.procurement_id = :procurementId
-- sales_record.procurement_id = :procurementId
```

---

## 3. 聚合接口

```java
// 后端实现
public interface OrderOverviewRepository {
    Optional<Procurement> findProcurement(Long procurementId);
    Optional<ReplenishmentDemand> findDemandByProcurement(Long procurementId);
    Optional<Factory> findFactoryByProcurement(Long procurementId);
    Optional<QcRecord> findQcRecordByProcurement(Long procurementId);
    Optional<LogisticsPlan> findLogisticsPlanByProcurement(Long procurementId);
    Optional<DomesticCustomsRecord> findDomesticCustomsByProcurement(Long procurementId);  // 待实现
    Optional<JapanCustomsRecord> findJapanCustomsByProcurement(Long procurementId);        // 待实现
    Optional<TaxRefundRecord> findTaxRefundByProcurement(Long procurementId);              // 待实现
    Optional<SalesRecord> findSalesRecordByProcurement(Long procurementId);                // 待实现
}
```

---

## 代码实现状态

- [x] 🟡 `OrderOverviewUseCase` 用例服务（骨架，待填充步骤5-8）
- [x] 🟡 `OrderOverviewController` REST 控制器（GET /api/v1/orders/{id}/overview）
- [x] 🟡 `OrderOverviewVO` 聚合响应对象
- [ ] 🔴 前端 `OrderOverviewPage.vue`
- [ ] 🔴 前端 `@/api/orderOverview.ts`
- [ ] 🔴 前端 `useOrderOverview.ts` composable
