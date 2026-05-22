-- logistics_plan 表新增总重量和总体积字段（v2.0）
ALTER TABLE logistics_plan
ADD COLUMN total_weight_kg DECIMAL(12,4) DEFAULT NULL COMMENT '货物总重量(kg)' AFTER customs_clearance_no,
ADD COLUMN total_volume_cbm DECIMAL(12,4) DEFAULT NULL COMMENT '货物总体积(m³)' AFTER total_weight_kg;
