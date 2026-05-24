-- V49: logistics_plan 追加净重/毛重字段（从 QC record 带入）
-- 用途：区分验货单件的净重与毛重，便于报关/订舱精确计算
ALTER TABLE logistics_plan
    ADD COLUMN net_weight_kg  DECIMAL(10, 4) NULL COMMENT '单品净重(kg)',
    ADD COLUMN gross_weight_kg DECIMAL(10, 4) NULL COMMENT '单品毛重(kg)';
