-- ============================================================
-- Flyway 迁移脚本：product_factory 关联数据
-- 执行前提：V10 已将 goods_staging 数据迁移到 product 表
-- 策略：通过 master_code + factory_name 匹配，生成商品-工厂关联
-- H2 兼容性：使用 INSERT...SELECT 代替 MySQL 特有语法
-- ============================================================

-- ============================================================
-- 步骤1：插入 product_factory 关联（按 master_code + factory_name 匹配）
-- goods_staging.factory_name → factory.factory_name
-- goods_staging.sku → product.master_code
-- ============================================================
INSERT INTO product_factory (
    product_id, factory_id,
    supplier_sku, moq,
    unit_price_rmb,
    is_preferred,
    create_time, update_time
)
SELECT
    p.id                      AS product_id,
    f.id                      AS factory_id,
    g.sku                     AS supplier_sku,
    1                         AS moq,
    g.unit_price              AS unit_price_rmb,
    TRUE                      AS is_preferred,
    CURRENT_TIMESTAMP(3)      AS create_time,
    CURRENT_TIMESTAMP(3)      AS update_time
FROM goods_staging g
JOIN product p ON (
    CASE WHEN LOCATE('-', g.sku) > 0
         THEN SUBSTRING(g.sku, 1, LOCATE('-', g.sku) - 1)
         ELSE g.sku
    END = p.master_code
)
JOIN factory f ON f.factory_name = g.factory_name
WHERE
    g.sku IS NOT NULL
    AND g.sku != ''
    AND g.factory_name IS NOT NULL
    AND g.factory_name != ''
    AND g.sku NOT LIKE '%厂家%'
    AND g.sku NOT LIKE '%各一个%'
    AND LENGTH(g.sku) != CHAR_LENGTH(g.sku);
