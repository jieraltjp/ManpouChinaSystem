-- V50: v_order_chain_v2 状态三元化（NOT_STARTED / IN_PROGRESS / COMPLETED）
-- 根因：V48 原版只有 COMPLETED/NOT_STARTED 二元值，导致前端状态与实际步骤数据不对应
--
-- 各步骤终端状态（isTerminal）定义：
--   Step3  shipment_batch        终端：已验货/已取消；    进行中：待验货/验货中
--   Step4  qc_record            终端：COMPLETED/RETURN_REQUESTED； 进行中：PENDING
--   Step5  logistics_plan       终端：DELIVERED；        进行中：PLANNED/BOOKED/IN_TRANSIT
--   Step6  domestic_customs      终端：CLEARED/REJECTED； 进行中：PENDING/SUBMITTED
--   Step7  japan_customs         终端：CLEARED/FAILED；   进行中：PENDING/IN_PROGRESS
--   Step8  tax_refund_record    终端：COMPLETED/NO_REFUND；进行中：APPLYING
--   Step9  sales_record          终端：DISCONTINUED；     进行中：LISTED/LOW_STOCK/OUT_OF_STOCK
--
-- 幂等：CREATE OR REPLACE VIEW（重复执行安全）。
CREATE OR REPLACE VIEW v_order_chain_v2 AS
SELECT
  -- ====== 锚点：Demand（Step1 补货需求）======
  d.id                              AS demand_id,
  d.demand_code                     AS demand_code,
  d.demand_type                     AS demand_type,
  d.product_code                    AS demand_product_code,
  d.sub_product_code                AS demand_sub_product_code,
  d.quantity                        AS demand_quantity,
  d.destination                     AS demand_destination,
  d.japan_lead                      AS demand_japan_lead,
  d.status                          AS demand_status,
  d.linked_procurement_id           AS linked_procurement_id,
  d.image_url                       AS demand_image_url,
  d.create_time                    AS demand_create_time,
  d.update_time                     AS demand_update_time,

  -- ====== Step2：Procurement（发注单）— 仅必要列 ======
  p.id                              AS procurement_id,
  p.product_code                    AS procurement_product_code,
  p.sub_product_code                AS procurement_sub_product_code,
  p.quantity                        AS procurement_quantity,
  p.price_rmb                       AS procurement_price_rmb,
  p.tax_point                       AS procurement_tax_point,
  p.exchange_rate                   AS procurement_exchange_rate,
  p.billing_type                    AS procurement_billing_type,
  p.estimated_price_jpy             AS procurement_estimated_price_jpy,
  p.destination                    AS procurement_destination,
  p.status                          AS procurement_status,
  p.return_reason                   AS procurement_return_reason,
  p.create_time                    AS procurement_create_time,
  p.create_by                       AS procurement_create_by,

  -- ====== 工厂快照（来自 procurement_snapshot，下单时刻不变）======
  snap.factory_id                   AS snapshot_factory_id,
  snap.factory_code                 AS snapshot_factory_code,
  snap.factory_name                 AS snapshot_factory_name,
  snap.factory_province             AS snapshot_factory_province,
  snap.factory_city                 AS snapshot_factory_city,
  snap.factory_contact_name         AS snapshot_factory_contact_name,
  snap.factory_contact_phone        AS snapshot_factory_contact_phone,
  snap.product_name_zh              AS snapshot_product_name_zh,
  snap.product_name_ja              AS snapshot_product_name_ja,
  snap.product_category             AS snapshot_product_category,

  -- ====== 9步状态汇总（与 SPEC-B00 步骤编号对齐）======
  -- Step1：Demand 存在即完成
  CASE WHEN d.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END  AS step1_status,
  -- Step2：Procurement 存在即完成
  CASE WHEN p.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END  AS step2_status,
  -- Step3：ShipmentBatch 终端=已验货/已取消；进行中=待验货/验货中
  CASE
    WHEN NOT EXISTS(SELECT 1 FROM shipment_batch sb2 WHERE sb2.procurement_id = p.id AND sb2.is_deleted = FALSE)
      THEN 'NOT_STARTED'
    WHEN EXISTS(SELECT 1 FROM shipment_batch sb2 WHERE sb2.procurement_id = p.id AND sb2.is_deleted = FALSE
                AND sb2.status NOT IN ('已验货', '已取消'))
      THEN 'IN_PROGRESS'
    ELSE 'COMPLETED'
  END AS step3_status,
  -- Step4：QcRecord 终端=COMPLETED/RETURN_REQUESTED；进行中=PENDING
  CASE
    WHEN NOT EXISTS(SELECT 1 FROM qc_record qr2 WHERE qr2.procurement_id = p.id AND qr2.is_deleted = FALSE)
      THEN 'NOT_STARTED'
    WHEN EXISTS(SELECT 1 FROM qc_record qr2 WHERE qr2.procurement_id = p.id AND qr2.is_deleted = FALSE
                AND qr2.status = 'PENDING')
      THEN 'IN_PROGRESS'
    ELSE 'COMPLETED'
  END AS step4_status,
  -- Step5：LogisticsPlan 终端=DELIVERED；进行中=PLANNED/BOOKED/IN_TRANSIT
  CASE
    WHEN NOT EXISTS(SELECT 1 FROM logistics_plan lp2 WHERE lp2.procurement_id = p.id AND lp2.is_deleted = FALSE)
      THEN 'NOT_STARTED'
    WHEN EXISTS(SELECT 1 FROM logistics_plan lp2 WHERE lp2.procurement_id = p.id AND lp2.is_deleted = FALSE
                AND lp2.status != 'DELIVERED')
      THEN 'IN_PROGRESS'
    ELSE 'COMPLETED'
  END AS step5_status,
  -- Step6：DomesticCustoms 终端=CLEARED/REJECTED；进行中=PENDING/SUBMITTED
  CASE
    WHEN NOT EXISTS(SELECT 1 FROM domestic_customs_record dc2
                    WHERE dc2.logistics_plan_id IN (SELECT id FROM logistics_plan WHERE procurement_id = p.id AND is_deleted = FALSE)
                      AND dc2.is_deleted = FALSE)
      THEN 'NOT_STARTED'
    WHEN EXISTS(SELECT 1 FROM domestic_customs_record dc2
                    WHERE dc2.logistics_plan_id IN (SELECT id FROM logistics_plan WHERE procurement_id = p.id AND is_deleted = FALSE)
                      AND dc2.is_deleted = FALSE
                      AND dc2.status IN ('PENDING', 'SUBMITTED'))
      THEN 'IN_PROGRESS'
    ELSE 'COMPLETED'
  END AS step6_status,
  -- Step7：JapanCustoms 终端=CLEARED/FAILED；进行中=PENDING/IN_PROGRESS
  CASE
    WHEN NOT EXISTS(SELECT 1 FROM japan_customs_record jc2
                    WHERE jc2.logistics_plan_id IN (SELECT id FROM logistics_plan WHERE procurement_id = p.id AND is_deleted = FALSE)
                      AND jc2.is_deleted = FALSE)
      THEN 'NOT_STARTED'
    WHEN EXISTS(SELECT 1 FROM japan_customs_record jc2
                    WHERE jc2.logistics_plan_id IN (SELECT id FROM logistics_plan WHERE procurement_id = p.id AND is_deleted = FALSE)
                      AND jc2.is_deleted = FALSE
                      AND jc2.status IN ('PENDING', 'IN_PROGRESS'))
      THEN 'IN_PROGRESS'
    ELSE 'COMPLETED'
  END AS step7_status,
  -- Step8：TaxRefundRecord 终端=COMPLETED/NO_REFUND；进行中=APPLYING
  CASE
    WHEN NOT EXISTS(SELECT 1 FROM tax_refund_record tr2 WHERE tr2.procurement_id = p.id AND tr2.is_deleted = FALSE)
      THEN 'NOT_STARTED'
    WHEN EXISTS(SELECT 1 FROM tax_refund_record tr2 WHERE tr2.procurement_id = p.id AND tr2.is_deleted = FALSE
                AND tr2.status = 'APPLYING')
      THEN 'IN_PROGRESS'
    ELSE 'COMPLETED'
  END AS step8_status,
  -- Step9：SalesRecord 终端=DISCONTINUED；进行中=LISTED/LOW_STOCK/OUT_OF_STOCK
  CASE
    WHEN NOT EXISTS(SELECT 1 FROM sales_record sr2 WHERE sr2.procurement_id = p.id AND sr2.is_deleted = FALSE)
      THEN 'NOT_STARTED'
    WHEN EXISTS(SELECT 1 FROM sales_record sr2 WHERE sr2.procurement_id = p.id AND sr2.is_deleted = FALSE
                AND sr2.status != 'DISCONTINUED')
      THEN 'IN_PROGRESS'
    ELSE 'COMPLETED'
  END AS step9_status

FROM replenishment_demand d
  LEFT JOIN procurement p ON p.id = d.linked_procurement_id AND p.is_deleted = FALSE
  LEFT JOIN procurement_snapshot snap ON snap.procurement_id = p.id AND snap.is_deleted = FALSE
WHERE d.is_deleted = FALSE;
