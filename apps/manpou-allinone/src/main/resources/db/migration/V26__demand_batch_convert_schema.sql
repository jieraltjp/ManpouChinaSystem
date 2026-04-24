-- ============================================================
-- V26__demand_batch_convert_schema.sql
-- FEATURE: 补货需求批量转采购（v1.6.0）
-- 变更：
--   1. replenishment_demand.sub_product_code → 扩展到 2048，存 SubProductItem JSON
--   2. replenishment_demand.linked_demand_items → 新增 JSON 字段，存 LinkedDemandItem[]
--   3. procurement.linked_demand_id → 新增，关联需求单
--   4. procurement.linked_demand_item_id → 新增，关联需求单子货号索引
-- ============================================================

-- 1. 扩展 sub_product_code 列（存储 SubProductItem JSON，v1.6.0）
ALTER TABLE replenishment_demand
    MODIFY COLUMN sub_product_code VARCHAR(2048) DEFAULT NULL
    COMMENT '子货号明细 JSON（v1.6.0）：[{"subCode":"be","quantity":100,"destination":"久留米"},...]';

-- 2. 新增 linked_demand_items 列（存储关联发注表明细，v1.6.0）
ALTER TABLE replenishment_demand
    ADD COLUMN linked_demand_items VARCHAR(2048) DEFAULT NULL
    COMMENT '关联发注表明细 JSON（v1.6.0）：[{"linkedProcurementId":101,"subCode":"be"},...]';

-- 3. 新增 procurement.linked_demand_id（v1.6.0）
ALTER TABLE procurement
    ADD COLUMN linked_demand_id BIGINT DEFAULT NULL
    COMMENT '关联需求单 ID（v1.6.0）';

-- 4. 新增 procurement.linked_demand_item_id（v1.6.0）
ALTER TABLE procurement
    ADD COLUMN linked_demand_item_id BIGINT DEFAULT NULL
    COMMENT '关联需求单子货号索引（v1.6.0，对应 subProductItems 数组下标）';

-- 5. 为新列添加索引
ALTER TABLE procurement
    ADD INDEX idx_procurement_linked_demand (linked_demand_id);
