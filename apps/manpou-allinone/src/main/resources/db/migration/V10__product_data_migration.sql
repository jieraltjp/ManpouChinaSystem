-- ============================================================
-- Flyway 迁移脚本：商品数据迁移（goods_staging → product 表）
-- 执行前提：V4 已创建 factory + product + product_factory 表，V9 已导入 goods_staging
-- H2 兼容性：移除 MySQL 特有语法（REGEXP、UPDATE...JOIN、嵌套语句）
-- ============================================================

-- ============================================================
-- 步骤1：goods_staging → product
-- ============================================================
INSERT INTO product (
    create_by, update_by, is_deleted,
    master_code, sub_code,
    name_en, name_zh,
    origin, unit,
    length_cm, width_cm, height_cm,
    net_weight_kg, gross_weight_kg,
    unit_price_rmb, tax_point, tax_rate,
    hs_code, declaration_elements,
    units_per_package,
    remarks, last_used_date
)
SELECT
    'system_migration'           AS create_by,
    'system_migration'           AS update_by,
    FALSE                        AS is_deleted,

    CASE WHEN LOCATE('-', sku) > 0
         THEN SUBSTRING(sku, 1, LOCATE('-', sku) - 1)
         ELSE sku
    END                          AS master_code,

    CASE WHEN LOCATE('-', sku) > 0
         THEN SUBSTRING(sku, LOCATE('-', sku) + 1)
         ELSE NULL
    END                          AS sub_code,

    NULLIF(name_en, '')         AS name_en,
    NULLIF(name_zh, '')         AS name_zh,
    NULLIF(origin, '')           AS origin,
    TRIM(NULLIF(unit, ''))      AS unit,

    NULL                          AS length_cm,
    NULL                          AS width_cm,
    NULL                          AS height_cm,

    weight_net                    AS net_weight_kg,
    weight_gross                  AS gross_weight_kg,

    unit_price                    AS unit_price_rmb,
    1.1                          AS tax_point,
    CASE WHEN tax_rate IS NOT NULL AND tax_rate > 0
         THEN tax_rate / 100.0
         ELSE 0.10
    END                          AS tax_rate,

    CASE WHEN hs_code IS NOT NULL AND hs_code > 0
         THEN CAST(hs_code AS CHAR)
         ELSE NULL
    END                          AS hs_code,

    NULLIF(declaration_elements, '') AS declaration_elements,

    CASE
        WHEN box_qty IS NOT NULL AND box_qty ~ '^[0-9]+$'
        THEN CAST(box_qty AS INTEGER)
        ELSE NULL
    END                          AS units_per_package,

    NULLIF(remark, '')           AS remarks,
    last_used                    AS last_used_date

FROM goods_staging
WHERE
    sku IS NOT NULL
    AND sku != ''
    AND sku NOT LIKE '%厂家%'
    AND sku NOT LIKE '%各一个%'
    AND LENGTH(sku) != CHAR_LENGTH(sku)
    AND LENGTH(sku) > 1
    AND (name_en IS NOT NULL OR name_zh IS NOT NULL)
    AND unit_price IS NOT NULL
    AND unit_price > 0;

-- ============================================================
-- 步骤2：更新 last_used_date（H2 不支持 UPDATE...JOIN，改用子查询）
-- ============================================================
UPDATE product
SET last_used_date = (
    SELECT MAX(latest.max_last_used)
    FROM (
        SELECT
            CASE WHEN LOCATE('-', sku) > 0
                 THEN SUBSTRING(sku, 1, LOCATE('-', sku) - 1)
                 ELSE sku
            END AS master_code,
            MAX(last_used) AS max_last_used
        FROM goods_staging
        WHERE last_used IS NOT NULL
        GROUP BY 1
    ) latest
    WHERE latest.master_code = product.master_code
)
WHERE master_code IN (
    SELECT
        CASE WHEN LOCATE('-', sku) > 0
             THEN SUBSTRING(sku, 1, LOCATE('-', sku) - 1)
             ELSE sku
        END
    FROM goods_staging
    WHERE last_used IS NOT NULL
);
