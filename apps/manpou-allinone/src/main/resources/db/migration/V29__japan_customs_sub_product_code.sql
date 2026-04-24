-- V29: japan_customs_record 补充 sub_product_code 列
-- 说明：全链路子货号追踪在步骤6断裂，补充此列后步骤2→步骤6子货号可查
-- 触发方式：手动创建 JapanCustomsRecord 时，清关员可从 Procurement 复制 subProductCode

ALTER TABLE japan_customs_record
ADD COLUMN sub_product_code VARCHAR(64) COMMENT '子货号/颜色（来自 Procurement）' AFTER logistics_plan_id;
