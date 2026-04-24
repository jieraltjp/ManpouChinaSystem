-- V27: 修正 replenishment_demand 表 JSON 列容量不足
-- sub_product_code 和 linked_demand_items 在 v1.6.0 存储 JSON 数组，
-- 8个以上子货号时超出 VARCHAR(2048) 限制（中文多字节字符导致字节超限）
-- 改为 TEXT 消除容量限制

ALTER TABLE replenishment_demand
    MODIFY COLUMN sub_product_code TEXT;

ALTER TABLE replenishment_demand
    MODIFY COLUMN linked_demand_items TEXT;
