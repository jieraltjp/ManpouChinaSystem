-- ============================================================
-- V5__procurement_factory_link.sql
-- procurement 表新增 factory_id 关联
-- 对应: DB-11 §3
-- ============================================================

ALTER TABLE procurement
    ADD COLUMN factory_id BIGINT DEFAULT NULL COMMENT '关联工厂ID → factory.id';

ALTER TABLE procurement
    ADD INDEX idx_procurement_factory_id (factory_id);
