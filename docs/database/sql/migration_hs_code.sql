-- =============================================================
-- Migration: Add Japanese HS code to product via goods_hs_mapping
-- Generated: 2026-04-23
-- Join path:
--   product.id = goods_master.id = goods_hs_mapping.goods_id
--   goods_hs_mapping.cn_hs_id → cn_hs_code.id → cn_hs_code.[编码]
--   goods_hs_mapping.jp_hs_id → jp_hs_code.id → jp_hs_code.[税番]
--
-- Results:
--   Total products   : 4999
--   Has CN HS code   : 2003  (product.hs_code)
--   Has JP HS code   : 629   (product.hs_code_jp)
--   Has both         : 629
-- =============================================================

SET FOREIGN_KEY_CHECKS=0;

-- Step 1: Import goods_hs_mapping data (if not already present)
-- The goods_hs_mapping table links goods_master.id → cn/jp hs_code.id
-- Run separately: mysql ... < /path/to/goods_hs_mapping.sql

-- Step 2: Add hs_code_jp column
ALTER TABLE `product`
  ADD COLUMN `hs_code_jp` varchar(20) DEFAULT NULL COMMENT '日本HS编码' AFTER `hs_code`;

-- Step 3: Update HS codes from mapping join
-- Note: column names [税番] and [编码] contain Japanese characters
--       use Python migrate_hs_code.py to execute (handles encoding)
-- SQL equivalent (requires proper character set connection):
/ *
UPDATE product p
INNER JOIN goods_master   gm ON p.id = gm.id
INNER JOIN goods_hs_mapping m ON gm.id = m.goods_id
LEFT  JOIN cn_hs_code  cn ON m.cn_hs_id = cn.id
LEFT  JOIN jp_hs_code  jp ON m.jp_hs_id = jp.id
SET
  p.hs_code     = cn.`编码`,        -- Chinese HS code (8-10 digits)
  p.hs_code_jp  = jp.`税番`,       -- Japanese HS code (8 digits)
  p.update_by   = 'system',
  p.update_time = NOW(6);
* /

SET FOREIGN_KEY_CHECKS=1;

-- Step 4: Remove temporary goods_hs_mapping table
-- DROP TABLE IF EXISTS `goods_hs_mapping`;
