-- ============================================================
-- V31: 补货需求单 v2.0.0 schema
-- 背景: v1.6.0 (V27) 将 quantity/destination/linked_procurement_id 合并到 JSON TEXT 列，
--       v2.0.0 改为每条 Demand = 一个子货号（直接字段）。
-- 迁移策略:
--   1. 解析旧 sub_product_code JSON 数组，将每条子货号展开为独立 Demand 行
--   2. 旧 linked_demand_items JSON 废弃，linked_procurement_id 直接填充
--   3. 删除旧 JSON 列
-- 对应: SPEC-B01-补货需求-步骤1.md v2.0.0, DB-01-procurement-demand.md v2.0.0
-- ============================================================

-- Step 1: 添加新列（临时，允许 NULL，待数据迁移后 NOT NULL）
ALTER TABLE replenishment_demand
    ADD COLUMN sub_product_code_new VARCHAR(64) DEFAULT NULL COMMENT 'v2.0.0: 子货号全码（如 ad009-be）',
    ADD COLUMN quantity_new INT DEFAULT NULL COMMENT 'v2.0.0: 需求数量',
    ADD COLUMN destination_new VARCHAR(128) DEFAULT NULL COMMENT 'v2.0.0: 目的地',
    ADD COLUMN linked_procurement_id_new BIGINT DEFAULT NULL COMMENT 'v2.0.0: 关联 Procurement.id';

-- Step 2: 迁移数据 — 单条子货号（旧版 quantity/destination 列还在 V10_2 的注释描述中，直接从 JSON 解析）
-- 格式A: [{"subCode":"be","quantity":50,"destination":"久留米"}] (v1.6.0 JSON)
-- 格式B: "be" (旧版单子货号字符串降级)
-- 格式C: ["be","bu"] (旧版子货号数组，无 quantity/destination)
-- 格式D: NULL (无子货号)

-- 2a. 提取 v1.6.0 JSON 格式的单条记录，填充 quantity/destination
UPDATE replenishment_demand d,
    (SELECT id,
            TRIM(BOTH '"' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(d.sub_product_code, '"subCode":', -1), ',', 1)) AS sub_code,
            CAST(TRIM(BOTH '"' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(d.sub_product_code, '"quantity":', -1), ',', 1)) AS SIGNED) AS qty,
            TRIM(BOTH '"' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(d.sub_product_code, '"destination":', -1), '}', 1)) AS dest
     FROM replenishment_demand
     WHERE sub_product_code IS NOT NULL
       AND sub_product_code != ''
       AND sub_product_code NOT LIKE '["%'
       AND sub_product_code NOT LIKE '[%'
       AND sub_product_code NOT LIKE '"%') AS parsed
SET d.sub_product_code_new = CONCAT(d.product_code, '-', parsed.sub_code),
    d.quantity_new = parsed.qty,
    d.destination_new = parsed.dest
WHERE d.id = parsed.id
  AND d.sub_product_code_new IS NULL;

-- 2b. 迁移 linked_demand_items JSON，提取 linked_procurement_id
UPDATE replenishment_demand d,
    (SELECT id,
           CAST(TRIM(BOTH '"' FROM SUBSTRING_INDEX(
               SUBSTRING_INDEX(d.linked_demand_items, '"linkedProcurementId":', -1),
               ',', 1)) AS SIGNED) AS proc_id
     FROM replenishment_demand
     WHERE linked_demand_items IS NOT NULL
       AND linked_demand_items != ''
       AND linked_demand_items LIKE '%"linkedProcurementId":%') AS parsed
SET d.linked_procurement_id_new = parsed.proc_id
WHERE d.id = parsed.id
  AND d.linked_procurement_id_new IS NULL;

-- 2c. 对于只有子货号数组无 quantity/destination 的情况（格式C），取第一条子货号
UPDATE replenishment_demand d,
    (SELECT id,
           TRIM(BOTH '"' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(d.sub_product_code, ',', 1), '[', -1)) AS sub_code
     FROM replenishment_demand
     WHERE sub_product_code IS NOT NULL
       AND sub_product_code != ''
       AND sub_product_code LIKE '[%'
       AND sub_product_code NOT LIKE '%"quantity":%'
       AND d.sub_product_code_new IS NULL) AS parsed
SET d.sub_product_code_new = CONCAT(d.product_code, '-', parsed.sub_code)
WHERE d.id = parsed.id;

-- 2d. 迁移 linked_demand_items 中单条 procurementId（格式 {"linkedProcurementId": 123}）
UPDATE replenishment_demand d,
    (SELECT id,
           CAST(TRIM(BOTH '"' FROM SUBSTRING_INDEX(
               SUBSTRING_INDEX(d.linked_demand_items, '"linkedProcurementId":', -1),
               '}', 1)) AS SIGNED) AS proc_id
     FROM replenishment_demand
     WHERE linked_demand_items IS NOT NULL
       AND linked_demand_items != ''
       AND linked_demand_items NOT LIKE '%linkedProcurementId":%,%'
       AND d.linked_procurement_id_new IS NULL) AS parsed
SET d.linked_procurement_id_new = parsed.proc_id
WHERE d.id = parsed.id
  AND d.linked_procurement_id_new IS NULL;

-- Step 3: 删除旧 JSON 列
ALTER TABLE replenishment_demand
    DROP COLUMN IF EXISTS sub_product_code,
    DROP COLUMN IF EXISTS linked_demand_items;

-- Step 4: 重命名新列，删除临时标记
ALTER TABLE replenishment_demand
    CHANGE COLUMN sub_product_code_new sub_product_code VARCHAR(64) DEFAULT NULL COMMENT 'v2.0.0: 子货号全码（如 ad009-be，商品唯一标识）',
    CHANGE COLUMN quantity_new quantity INT DEFAULT NULL COMMENT 'v2.0.0: 需求数量',
    CHANGE COLUMN destination_new destination VARCHAR(128) DEFAULT NULL COMMENT 'v2.0.0: 目的地',
    CHANGE COLUMN linked_procurement_id_new linked_procurement_id BIGINT DEFAULT NULL COMMENT 'v2.0.0: 关联 Procurement.id';

-- Step 5: 为 quantity 添加索引（辅助统计查询）
ALTER TABLE replenishment_demand
    ADD INDEX idx_demand_quantity (quantity);

-- Step 6: 为 destination 添加索引（辅助筛选）
ALTER TABLE replenishment_demand
    ADD INDEX idx_demand_destination (destination);

-- Step 7: 为 sub_product_code 添加索引
ALTER TABLE replenishment_demand
    ADD INDEX idx_demand_sub_product_code (sub_product_code);
