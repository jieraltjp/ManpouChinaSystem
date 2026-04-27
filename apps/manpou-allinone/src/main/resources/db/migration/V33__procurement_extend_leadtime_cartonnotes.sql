-- ============================================================
-- V33__procurement_extend_leadtime_cartonnotes.sql
-- 变更：procurement 表新增交货期天数和纸箱备注字段
-- 日期：2026-04-27
-- 业务步号：02
-- ============================================================

ALTER TABLE procurement
    ADD COLUMN lead_time_days INT COMMENT '交货期天数（30/45/60），新建时默认 30',
    ADD COLUMN carton_notes VARCHAR(512) COMMENT '纸箱备注';

-- 默认值仅对新插入记录生效，历史数据可手动 UPDATE
-- UPDATE procurement SET lead_time_days = 30 WHERE lead_time_days IS NULL;
