-- ============================================================
-- V42: 存量 linked_procurement_id 数据回填（SPEC-B11 §11）
-- 策略：将 replenishment_demand.linked_procurement_id 存量数据
--       回填至 demand_procurement_mapping 表
-- ============================================================

-- 仅当映射表有数据时跳过（幂等）
INSERT INTO demand_procurement_mapping
    (demand_id, procurement_id, allocated_quantity, status, create_time, update_time, create_by, update_by, is_deleted)
SELECT
    d.id,
    d.linked_procurement_id,
    d.quantity,
    CASE
        WHEN EXISTS (
            SELECT 1 FROM qc_record qc
            JOIN shipment_batch sb ON sb.id = qc.shipment_batch_id
            WHERE sb.procurement_id = d.linked_procurement_id
            AND qc.result = 'PASS'
            AND qc.status = 'COMPLETED'
        ) THEN '已完成'
        ELSE '进行中'
    END AS mapping_status,
    NOW(3),
    NOW(3),
    'SYSTEM',
    'SYSTEM',
    0
FROM replenishment_demand d
WHERE d.linked_procurement_id IS NOT NULL
  AND d.deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM demand_procurement_mapping m
      WHERE m.demand_id = d.id
        AND m.procurement_id = d.linked_procurement_id
        AND m.is_deleted = false
  );
