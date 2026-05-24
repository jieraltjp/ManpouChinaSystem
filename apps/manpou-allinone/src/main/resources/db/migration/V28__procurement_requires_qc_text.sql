-- V28__procurement_requires_qc_text.sql
-- SPEC-B14: 发注单检测字段文本化
-- 将 requires_qc 从 BIT(1) 改为 VARCHAR(128)，支持自由填写检测类型或备注
ALTER TABLE procurement
  MODIFY COLUMN requires_qc VARCHAR(128) DEFAULT NULL COMMENT '检测类型/备注（文本，可为空）';
