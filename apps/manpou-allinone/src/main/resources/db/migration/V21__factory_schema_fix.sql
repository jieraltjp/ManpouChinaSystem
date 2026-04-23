-- ============================================================
-- Flyway 迁移脚本：Schema 一致性修复
-- 修复 G-01 + G-03 + M-01 + M-04（INTJ 审计 2026-04-22）
-- ============================================================
-- 对应审计报告：
--   G-01: factory.contact_person → contact_name (列名与 Java 实体对齐)
--   G-03: factory.rough_location VARCHAR(255) → VARCHAR(500) (与 DB-10 文档对齐)
--   M-01: factory.idx_factory_name 索引缺失
--   M-04: product.category VARCHAR(20) → VARCHAR(32) (与 SPEC-B10 文档对齐)
-- ============================================================
-- 依赖：V4__product_tables.sql, V5__factory_migration.sql
-- 兼容性：V5 种子数据未填充 contact_person，可安全重命名
-- ============================================================

-- 1. 重命名 contact_person → contact_name，并同步宽度 VARCHAR(100) → VARCHAR(64)
ALTER TABLE factory
    CHANGE COLUMN contact_person contact_name VARCHAR(64) COMMENT '联系人姓名';

-- 2. 修复 rough_location 宽度：VARCHAR(255) → VARCHAR(500)，与文档 DB-10 对齐
ALTER TABLE factory
    MODIFY COLUMN rough_location VARCHAR(500) COMMENT '粗略地址';

-- 3. 补充索引：idx_factory_name（V4 建表时遗漏，Java 实体已声明）
CREATE INDEX idx_factory_name ON factory(factory_name);

-- 4. 修复 product.category 宽度：VARCHAR(20) → VARCHAR(32)，与 SPEC-B10 文档对齐
ALTER TABLE product
    MODIFY COLUMN category VARCHAR(32) COMMENT 'OEM / ORDINARY / FACTORY_DIRECT';

-- 验证
SELECT
    'Factory.columns'       AS chk_type,
    COLUMN_NAME            AS col,
    COLUMN_TYPE            AS db_type,
    COLUMN_COMMENT         AS note
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'factory'
  AND COLUMN_NAME IN ('contact_name', 'rough_location');

SELECT
    'Factory.indexes'      AS chk_type,
    INDEX_NAME             AS idx,
    COLUMN_NAME            AS col
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'factory'
  AND INDEX_NAME   = 'idx_factory_name';
