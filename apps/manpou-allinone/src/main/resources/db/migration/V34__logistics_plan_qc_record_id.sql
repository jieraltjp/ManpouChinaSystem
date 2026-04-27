-- ============================================================
-- Flyway 迁移脚本：logistics_plan 表新增 qc_record_id 列
-- 日期: 2026-04-27
-- 版本: 1.2.0
-- 对应: SPEC-B04-v1.2.0 · DB-04-v1.2.0
-- 说明:
--   LogisticsPlan 业务锚点从 procurementId 改为 qcRecordId。
--   验完货才知道实际装箱尺寸（长×宽×高）和毛重，用于调配订舱判断。
--   保留 procurement_id 列（拼柜场景可空），新增 qc_record_id 作为主关联。
--   添加 qcRecordId 的索引 idx_logistics_qc_record(qc_record_id)。
-- ============================================================

ALTER TABLE logistics_plan
  ADD COLUMN qc_record_id BIGINT COMMENT '关联验货记录 FK → qc_record.id（v1.2.0 新增，调配锚点）' AFTER plan_code;

-- 新增索引
CREATE INDEX idx_logistics_qc_record ON logistics_plan (qc_record_id);
