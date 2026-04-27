-- V36__domestic_customs_container_no.sql
-- 功能: domestic_customs_record 新增 container_no 货柜号字段（v1.3.0）
-- 场景: 货柜级报关——1货柜号 = N个 LogisticsPlan = N个 DomesticCustomsRecord
--       用户从 LogisticsPage 点击「创建报关」，跳转到 CustomsPage 并带入货柜号
-- 作者: claude
-- 日期: 2026-04-27

-- ============================================================
-- 1. 新增 container_no 字段
-- ============================================================
ALTER TABLE domestic_customs_record
    ADD COLUMN container_no VARCHAR(32) DEFAULT NULL COMMENT '货柜号（v1.3.0，来自 LogisticsPlan.containerNo）' AFTER customs_code;

-- ============================================================
-- 2. 添加索引（支持按货柜号模糊查询）
-- ============================================================
CREATE INDEX idx_dc_container_no ON domestic_customs_record(container_no);

-- ============================================================
-- 3. 回滚（若需要）
-- ============================================================
-- DROP INDEX idx_dc_container_no ON domestic_customs_record;
-- ALTER TABLE domestic_customs_record DROP COLUMN container_no;
