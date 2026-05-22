-- V24: 订单总览视图 v_order_chain_v1
-- Entity: com.manpou.allinone.order.domain.view.OrderChainView
-- 说明: 基于 procurement_snapshot 提供下单时刻工厂/商品快照，保证历史数据不变。
--       支持前端 /api/v1/orders/chain 列表与详情接口。
-- 幂等: CREATE OR REPLACE VIEW（重复执行安全）
CREATE OR REPLACE VIEW v_order_chain_v1 AS
SELECT
  -- ====== 锚点：Demand（步骤1）======
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
  d.create_time                     AS demand_create_time,
  d.update_time                     AS demand_update_time,

  -- ====== 步骤2：Procurement ======
  p.id                              AS procurement_id,
  NULL                              AS procurement_code,
  p.factory_id                      AS procurement_factory_id,
  p.product_code                    AS procurement_product_code,
  p.sub_product_code                AS procurement_sub_product_code,
  p.quantity                        AS procurement_quantity,
  p.price_rmb                      AS procurement_price_rmb,
  p.tax_point                      AS procurement_tax_point,
  p.exchange_rate                  AS procurement_exchange_rate,
  p.billing_type                   AS procurement_billing_type,
  p.estimated_price_jpy             AS procurement_estimated_price_jpy,
  p.order_date                     AS procurement_order_date,
  p.factory_ship_date              AS procurement_factory_ship_date,
  p.planned_ship_date              AS procurement_planned_ship_date,
  p.actual_ship_date               AS procurement_actual_ship_date,
  p.lead_time_days                 AS procurement_lead_time_days,
  p.product_lead                   AS procurement_product_lead,
  p.japan_lead                     AS procurement_japan_lead,
  p.china_lead                     AS procurement_china_lead,
  p.destination                     AS procurement_destination,
  p.customer_company               AS procurement_customer_company,
  p.status                         AS procurement_status,
  p.return_reason                  AS procurement_return_reason,
  p.create_time                    AS procurement_create_time,

  -- ====== 工厂快照（来自 procurement_snapshot，下单时刻不变）======
  snap.factory_id                   AS snapshot_factory_id,
  snap.factory_code                 AS snapshot_factory_code,
  snap.factory_name                 AS snapshot_factory_name,
  snap.factory_province            AS snapshot_factory_province,
  snap.factory_city                 AS snapshot_factory_city,
  snap.factory_contact_name        AS snapshot_factory_contact_name,
  snap.factory_contact_phone       AS snapshot_factory_contact_phone,

  -- ====== 商品快照（来自 procurement_snapshot，下单时刻不变）======
  snap.product_name_zh              AS snapshot_product_name_zh,
  snap.product_name_ja              AS snapshot_product_name_ja,
  snap.product_category            AS snapshot_product_category,

  -- ====== 步骤3：验货记录 ======
  q.id                              AS qc_record_id,
  q.qc_code                         AS qc_code,
  q.result                          AS qc_result,
  q.inspection_count                AS qc_inspection_count,
  q.passed_count                    AS qc_passed_count,
  q.defective_count                 AS qc_defective_count,
  q.box_count                       AS qc_box_count,
  q.box_length_cm                  AS qc_box_length_cm,
  q.box_width_cm                   AS qc_box_width_cm,
  q.box_height_cm                  AS qc_box_height_cm,
  q.net_weight_per_unit            AS qc_net_weight_per_unit,
  q.gross_weight                   AS qc_gross_weight,
  q.qc_date                        AS qc_date,
  q.status                         AS qc_status,
  q.create_time                    AS qc_create_time,

  -- ====== 步骤4：调配计划 ======
  l.id                              AS logistics_plan_id,
  l.plan_code                       AS logistics_plan_code,
  l.container_no                    AS logistics_container_no,
  l.plan_type                      AS logistics_plan_type,
  l.cargo_length_cm                AS logistics_cargo_length_cm,
  l.cargo_width_cm                 AS logistics_cargo_width_cm,
  l.cargo_height_cm                AS logistics_cargo_height_cm,
  l.cargo_volume_cbm               AS logistics_cargo_volume_cbm,
  l.cargo_weight_kg                AS logistics_cargo_weight_kg,
  l.requires_qc                    AS logistics_requires_qc,
  l.estimated_ship_date            AS logistics_estimated_ship_date,
  l.actual_ship_date               AS logistics_actual_ship_date,
  l.status                         AS logistics_status,
  l.create_time                    AS logistics_create_time,

  -- ====== 步骤5：国内报关 ======
  dc.id                             AS domestic_customs_id,
  dc.status                         AS domestic_customs_status,

  -- ====== 步骤6：日本清关 ======
  jc.id                             AS japan_customs_id,
  jc.status                         AS japan_customs_status,

  -- ====== 步骤7：退税 ======
  tr.id                             AS tax_refund_id,
  tr.status                         AS tax_refund_status,

  -- ====== 步骤8：运营销售 ======
  sr.id                             AS sales_record_id,
  sr.status                         AS sales_status,

  -- ====== 8步状态汇总 ======
  CASE WHEN d.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step1_status,
  CASE WHEN p.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step2_status,
  CASE WHEN q.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step3_status,
  CASE WHEN l.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step4_status,
  CASE WHEN dc.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step5_status,
  CASE WHEN jc.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step6_status,
  CASE WHEN tr.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step7_status,
  CASE WHEN sr.id IS NOT NULL THEN 'COMPLETED' ELSE 'NOT_STARTED' END AS step8_status

FROM replenishment_demand d
  LEFT JOIN procurement p ON p.id = d.linked_procurement_id AND p.is_deleted = FALSE
  LEFT JOIN procurement_snapshot snap ON snap.procurement_id = p.id AND snap.is_deleted = FALSE
  LEFT JOIN qc_record q ON q.procurement_id = p.id AND q.is_deleted = FALSE
  LEFT JOIN logistics_plan l ON l.qc_record_id = q.id AND l.is_deleted = FALSE
  LEFT JOIN domestic_customs_record dc ON dc.logistics_plan_id = l.id AND dc.is_deleted = FALSE
  LEFT JOIN japan_customs_record jc ON jc.logistics_plan_id = l.id AND jc.is_deleted = FALSE
  LEFT JOIN tax_refund_record tr ON tr.procurement_id = p.id AND tr.is_deleted = FALSE
  LEFT JOIN sales_record sr ON sr.procurement_id = p.id AND sr.is_deleted = FALSE

WHERE d.is_deleted = FALSE;
