-- V37__demand_image_url.sql
-- 功能: replenishment_demand 新增 image_url 字段（v2.1.0）
-- 场景: DemandPage 子货号列后面展示商品图片
--       数据来源: demand.product_code = product.master_code，查 product.image_url
-- 作者: claude
-- 日期: 2026-04-27

-- ============================================================
-- 1. 新增 image_url 字段（反规范化，查询零成本）
-- ============================================================
ALTER TABLE replenishment_demand
    ADD COLUMN image_url VARCHAR(512) DEFAULT NULL COMMENT '商品图片URL（v2.1.0，反规范化自 product.image_url）' AFTER remarks;

-- ============================================================
-- 2. 回填存量数据（根据 demand.product_code = product.master_code）
-- ============================================================
UPDATE replenishment_demand d,
    (SELECT p.master_code, p.image_url
     FROM product p
     WHERE p.image_url IS NOT NULL AND p.image_url != '') AS p
SET d.image_url = p.image_url
WHERE d.product_code = p.master_code
  AND d.image_url IS NULL;

-- ============================================================
-- 3. 回滚（若需要）
-- ============================================================
-- ALTER TABLE replenishment_demand DROP COLUMN image_url;
