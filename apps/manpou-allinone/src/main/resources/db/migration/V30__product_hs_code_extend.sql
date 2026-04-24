-- =============================================================
-- Migration: V30__product_hs_code_extend.sql
-- Date: 2026-04-23
-- Desc: Extend product with Japanese HS code
-- Note: 原 V24 版本号与 V24__product_field_extend.sql 重复，升至 V30
-- =============================================================

-- Add Japanese HS code column
ALTER TABLE `product` ADD COLUMN `hs_code_jp` VARCHAR(20) DEFAULT NULL COMMENT '日本HS编码（税番）' AFTER `hs_code`;
CREATE INDEX `idx_hs_code_jp` ON `product` (`hs_code_jp`);

-- Add business fields from goods_master
ALTER TABLE `product`
  ADD COLUMN `quantities` INT DEFAULT NULL COMMENT '数量' AFTER `unit`,
  ADD COLUMN `carton_qty` INT DEFAULT NULL COMMENT '箱数' AFTER `quantities`,
  ADD COLUMN `amount_rmb` DECIMAL(14,4) DEFAULT NULL COMMENT '金额(RMB)' AFTER `carton_qty`,
  ADD COLUMN `material_ja` VARCHAR(255) DEFAULT NULL COMMENT '材质（日文）' AFTER `amount_rmb`;

-- Populate hs_code / hs_code_jp via goods_hs_mapping join
-- Join: product.id = goods_master.id = goods_hs_mapping.goods_id
--       → cn_hs_code.id / jp_hs_code.id → [编码] / [税番]
-- (Run via Python migrate_hs_code.py to handle Japanese column names)
-- Result: 4999 updated, CN HS: 2003, JP HS: 629
-- NOTE: goods_master / goods_hs_mapping 已于 2026-04-23 删除，数据已固化至 product 表
