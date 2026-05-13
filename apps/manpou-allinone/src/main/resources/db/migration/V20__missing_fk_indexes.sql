-- V20: 补建缺失的外键索引，避免 JOIN / WHERE 时的全表扫描
-- 2026-05-14
-- 关联审计: docs/check/110-audit-2026-05-13.md §9.4

-- logistics_plan.container_id → container.id（JOIN 时频繁使用）
ALTER TABLE logistics_plan ADD INDEX idx_lp_container_id (container_id);

-- logistics_plan.pool_id → consolidation_pool.id（JOIN 时频繁使用）
ALTER TABLE logistics_plan ADD INDEX idx_lp_pool_id (pool_id);

-- japan_customs_record.factory_id → factory.id（WHERE factoryId 筛选/JOIN）
ALTER TABLE japan_customs_record ADD INDEX idx_jp_factory_id (factory_id);
