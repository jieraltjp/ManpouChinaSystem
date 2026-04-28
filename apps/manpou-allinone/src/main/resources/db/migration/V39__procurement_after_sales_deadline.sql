-- V39: procurement 表新增售后截止日字段
-- 对应 SPEC-B02 v1.10.0 / DB-02 v1.3.0
ALTER TABLE procurement
ADD COLUMN after_sales_deadline DATE COMMENT '售后截止日（v1.10.0 新增）' AFTER carton_notes;
