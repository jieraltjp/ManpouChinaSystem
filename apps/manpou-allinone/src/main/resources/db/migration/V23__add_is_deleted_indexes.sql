-- V23: add missing is_deleted indexes on all business tables
-- 背景：软删除查询 findByXxxAndIsDeletedFalse() 缺少索引，导致全表扫描
-- 规则：所有含 is_deleted 列的表均需 idx_{table}_is_deleted

-- factory
ALTER TABLE factory ADD INDEX idx_factory_is_deleted (is_deleted);

-- product
ALTER TABLE product ADD INDEX idx_product_is_deleted (is_deleted);

-- product_factory
ALTER TABLE product_factory ADD INDEX idx_product_factory_is_deleted (is_deleted);

-- procurement (procurement_order in queries)
ALTER TABLE procurement ADD INDEX idx_proc_is_deleted (is_deleted);

-- qc_record
ALTER TABLE qc_record ADD INDEX idx_qc_is_deleted (is_deleted);

-- logistics_plan
ALTER TABLE logistics_plan ADD INDEX idx_lp_is_deleted (is_deleted);

-- domestic_customs_record
ALTER TABLE domestic_customs_record ADD INDEX idx_dc_is_deleted (is_deleted);

-- japan_customs_record
ALTER TABLE japan_customs_record ADD INDEX idx_jp_is_deleted (is_deleted);

-- tax_refund_record
ALTER TABLE tax_refund_record ADD INDEX idx_tr_is_deleted (is_deleted);

-- sales_record
ALTER TABLE sales_record ADD INDEX idx_sr_is_deleted (is_deleted);
