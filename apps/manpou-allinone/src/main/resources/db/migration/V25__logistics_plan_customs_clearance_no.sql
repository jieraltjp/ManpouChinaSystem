-- logistics_plan 表新增通关单号字段（v2.0）
ALTER TABLE logistics_plan
ADD COLUMN customs_clearance_no VARCHAR(64) DEFAULT NULL COMMENT '通关单号' AFTER remarks;
