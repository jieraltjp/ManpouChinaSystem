# DB-09 — 订单总览数据库设计

> **版本**: 1.2.0
> **更新**: 2026-04-24（v1.2.0：同步 v1.6.0 Demand 锚点关联；更新代码实现状态为已完成）
> **创建**: 2026-04-22
> **状态**: ✅ 已实现（步骤1-4 + Demand 锚点；步骤5-8 待填充）
> **业务步号**: 09（订单总览 — 核心视图）
> **对应业务文档**: `SPEC-B00-全链路总览.md` · `SPEC-B09-订单总览-API设计.md`
> **对应 UI 文档**: `docs/ui/pages/09-order-overview.md`
> **说明**: 订单总览不新增表，而是以 Procurement.id 为锚点，聚合查询全链路 8 步数据。

---

## 1. 数据聚合关系

```
Procurement (锚点)
  ├── ReplenishmentDemand   (步骤1)  → replenishment_demand.linked_procurement_id → procurement.id
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

> ⚠️ v2.0.0 关联关系：`Demand.linked_procurement_id` → `Procurement.id`（1 Demand = 1 Procurement）。

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

-- 步骤1：ReplenishmentDemand（v2.0.0: replenishment_demand.linked_procurement_id → procurement.id）
SELECT d.* FROM replenishment_demand d
WHERE d.linked_procurement_id = :procurementId
  AND d.is_deleted = FALSE
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

- [x] ✅ `OrderOverviewUseCase` 用例服务（含 Demand 锚点 + Procurement 锚点）
- [x] ✅ `OrderOverviewController` REST 控制器（GET /api/v1/orders/procurement/{procurementId}/overview + /demands 端点）
- [x] ✅ `OrderOverviewPageVO` 聚合响应对象（含 StepStatus 数组）
- [x] ✅ 前端 `OrderOverviewPage.vue`（双入口架构）
- [x] ✅ 前端 `DemandOverviewPage.vue`（Demand 锚点详情页）
- [x] ✅ 前端 `ProcurementOverviewPage.vue`（Procurement 锚点详情页）
- [x] ✅ 前端 `@/api/orderOverview.ts` + `@/api/demand.ts`
- [ ] 🟡 步骤5-8 聚合数据填充（DomesticCustoms/JapanCustoms/TaxRefund/SalesRecord）
