-- ============================================================
-- V43: QcRecord 增加 shipment_batch_id FK（SPEC-B11 §6.3）
-- 说明：
--   1. 新增 shipment_batch_id 列（可空）
--   2. 为存量 QcRecord 按 procurement_id 找到或创建 ShipmentBatch 并关联
--   3. FK 约束待历史数据关联后再开启（避免现有数据约束冲突）
--   4. procurement_id 改为 nullable（不再强制唯一关联）
-- ============================================================

-- Step 1: 新增 shipment_batch_id 列
ALTER TABLE qc_record ADD COLUMN shipment_batch_id BIGINT NULL COMMENT '关联出货批次ID';

-- Step 2: 创建索引
ALTER TABLE qc_record ADD INDEX idx_qc_shipment_batch (shipment_batch_id);

-- Step 3: 存量数据回填
-- 策略：每个 procurement_id 找到或创建一个 ShipmentBatch（status=已验货），然后关联
INSERT INTO shipment_batch
    (procurement_id, batch_code, shipment_quantity, factory_ship_date, actual_ship_date,
     status, remarks, create_time, update_time, create_by, update_by, is_deleted)
SELECT
    qc.procurement_id,
    CONCAT('SB-HISTORY-', qc.procurement_id, '-', MIN(qc.id)),
    COALESCE(SUM(qc.inspection_count), 0),
    MIN(qc.qc_date),
    MIN(qc.qc_date),
    '已验货',
    '历史数据迁移（V43）',
    NOW(3),
    NOW(3),
    'SYSTEM',
    'SYSTEM',
    0
FROM qc_record qc
WHERE qc.deleted = false
  AND qc.procurement_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM shipment_batch sb WHERE sb.procurement_id = qc.procurement_id AND sb.remarks LIKE '%历史数据迁移%'
  )
GROUP BY qc.procurement_id;

-- Step 4: 关联存量 QcRecord 到回填的 ShipmentBatch
UPDATE qc_record qc
JOIN (
    SELECT MIN(id) AS batch_id, procurement_id
    FROM shipment_batch
    WHERE remarks LIKE '%历史数据迁移%'
    GROUP BY procurement_id
) sb ON sb.procurement_id = qc.procurement_id
SET qc.shipment_batch_id = sb.batch_id
WHERE qc.deleted = false
  AND qc.shipment_batch_id IS NULL
  AND qc.procurement_id IS NOT NULL;

-- Step 5: procurement_id 改为 nullable
ALTER TABLE qc_record MODIFY COLUMN procurement_id BIGINT NULL COMMENT '关联采购单（保留作为审计追溯，V43后不再强制）';
