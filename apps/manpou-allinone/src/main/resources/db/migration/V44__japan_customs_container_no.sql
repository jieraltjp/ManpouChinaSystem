-- ============================================================
-- V44__japan_customs_container_no.sql
-- 日本清关记录增加货柜号（v1.4.0）
--
-- 变更说明：
--   1. container_no：货柜号（新增，必填，与国内报关步骤5一致）
--   2. product_code：货号（新增，用于前端展示）
--   3. factory_id：关联工厂（新增）
--
-- JPA ddl-auto: update 模式下此脚本仅作文档记录；
-- JPA 会自动添加列。正式生产部署时执行此脚本。
-- ============================================================

-- 新增 container_no 列
ALTER TABLE japan_customs_record
    ADD COLUMN container_no VARCHAR(32) DEFAULT NULL COMMENT '货柜号（v1.4.0 必填）' AFTER customs_entry_no;

-- 新增 factory_id 列
ALTER TABLE japan_customs_record
    ADD COLUMN factory_id BIGINT DEFAULT NULL COMMENT '关联工厂（v1.4.0 新增）' AFTER procurement_id;

-- 新增 product_code 列
ALTER TABLE japan_customs_record
    ADD COLUMN product_code VARCHAR(32) DEFAULT NULL COMMENT '货号（v1.4.0 新增）' AFTER factory_id;

-- 新增 container_no 索引
CREATE INDEX idx_jp_container_no ON japan_customs_record (container_no);
