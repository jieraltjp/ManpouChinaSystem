-- ============================================================
-- Flyway 迁移脚本：商品数据迁移（goods.sql → product 表）
-- 文件命名：V{版本号}__{简短描述}.sql
-- 执行前提：V3__product_tables.sql 已执行，product + factory 表已创建
-- 数据来源：docs/database/sql/goods.sql（781条，迁移自 esagf_oem.goods 表）
-- 迁移策略：先将 goods.sql 导出为 CSV 并导入临时 staging 表，再迁入 product
-- ============================================================

-- ============================================================
-- 步骤0：创建 staging 表（临时存放原始 goods 数据）
-- 注意：goods.sql 为外部系统数据，此 staging 表仅作为迁移中间层
-- ============================================================
CREATE TABLE IF NOT EXISTS goods_staging (
    id                  BIGINT PRIMARY KEY,
    sku                 VARCHAR(50)  DEFAULT NULL,
    hs_code             BIGINT       DEFAULT NULL,
    name_en             VARCHAR(255) DEFAULT NULL,
    name_zh             VARCHAR(255) DEFAULT NULL,
    unit_price          DECIMAL(12,4) DEFAULT NULL,
    tax_rate            DECIMAL(5,2) DEFAULT NULL,
    unit                VARCHAR(50)  DEFAULT NULL,
    weight_gross        DECIMAL(10,3) DEFAULT NULL,
    weight_net          DECIMAL(10,3) DEFAULT NULL,
    declaration_elements TEXT         DEFAULT NULL,
    box_qty             VARCHAR(50)  DEFAULT NULL,
    box_desc            VARCHAR(255) DEFAULT NULL,
    origin              VARCHAR(100) DEFAULT NULL,
    factory_name        VARCHAR(255) DEFAULT NULL,
    buyer               VARCHAR(100) DEFAULT NULL,
    remark              TEXT         DEFAULT NULL,
    last_used           DATE         DEFAULT NULL
);

-- ============================================================
-- 步骤0.5：导入 goods.sql 数据到 staging 表
-- 执行方式（任选一种）：
--
-- 方式A：MySQL 命令行（推荐）
--   $ mysql -h 192.168.12.198 -u root -p esagf_oem -e "SELECT * FROM goods" > goods.csv
--   $ mysql -u root -p your_database -e "LOAD DATA LOCAL INFILE 'goods.csv' INTO TABLE goods_staging FIELDS TERMINATED BY ',' ENCLOSED BY '''' LINES TERMINATED BY '\n';"
--
-- 方式B：如果 goods 表已在同一数据库
--   INSERT INTO goods_staging SELECT * FROM goods WHERE 1=1;
--
-- 方式C：直接执行 goods.sql 中的 INSERT 语句
--   $ mysql -u root -p your_database < docs/database/sql/goods.sql
--   -- 然后：INSERT INTO goods_staging SELECT * FROM goods;
-- ============================================================

-- ============================================================
-- 步骤1：数据清洗与迁移（goods_staging → product）
-- 过滤条件：
--   - sku 不能为空
--   - sku 不能为纯中文描述（占位记录，如 "各一个厂家"）
--   - sku 不能为通用中文名词（如 "纸袋"）
--   - name_zh 或 name_en 不能同时为空
-- ============================================================

-- 货号拆分规则：
--   sku='in041-a' → master_code='in041', sub_code='a'
--   sku='in041'    → master_code='in041',  sub_code=NULL

INSERT INTO product (
    -- 系统审计字段
    create_by,
    update_by,
    is_deleted,

    -- 货号
    master_code,
    sub_code,

    -- 多语言名称
    name_en,
    name_zh,

    -- 基础属性
    origin,
    unit,

    -- 单品尺寸（从 declaration_elements 解析）
    length_cm,
    width_cm,
    height_cm,

    -- 重量（g → kg）
    net_weight_kg,
    gross_weight_kg,

    -- 价格
    unit_price_rmb,
    tax_point,
    tax_rate,

    -- 报关
    hs_code,
    declaration_elements,

    -- 外箱数量
    units_per_package,

    -- 其他
    remarks,
    last_used_date
)
SELECT
    'system_migration'       AS create_by,
    'system_migration'       AS update_by,
    FALSE                    AS is_deleted,

    -- 货号拆分
    CASE
        WHEN LOCATE('-', sku) > 0
        THEN SUBSTRING(sku, 1, LOCATE('-', sku) - 1)
        ELSE sku
    END                    AS master_code,

    CASE
        WHEN LOCATE('-', sku) > 0
        THEN SUBSTRING(sku, LOCATE('-', sku) + 1)
        ELSE NULL
    END                    AS sub_code,

    -- 多语言名称
    NULLIF(name_en, '')    AS name_en,
    NULLIF(name_zh, '')  AS name_zh,

    -- 基础属性
    NULLIF(origin, '')    AS origin,
    TRIM(NULLIF(unit, '')) AS unit,

    -- 尺寸：尝试从 declaration_elements 解析
    -- 格式如："折叠床：休息用|牛津布+钢管|未装软垫|193*23*28cm"
    -- 或："收纳柜：收纳|塑料|49.5*34.5*99.5cm"
    NULL                    AS length_cm,
    NULL                    AS width_cm,
    NULL                    AS height_cm,

    -- 重量：goods.sql weight_gross/weight_net 单位为 kg（decimal 3位小数），直接映射
    weight_net             AS net_weight_kg,

    weight_gross            AS gross_weight_kg,

    -- 价格（字段原始单位为 CNY）
    unit_price             AS unit_price_rmb,
    1.1                    AS tax_point,
    CASE WHEN tax_rate IS NOT NULL AND tax_rate > 0
         THEN tax_rate / 100.0
         ELSE 0.10
    END                    AS tax_rate,

    -- 报关
    CASE WHEN hs_code IS NOT NULL AND hs_code > 0
         THEN CAST(hs_code AS CHAR)
         ELSE NULL
    END                    AS hs_code,

    NULLIF(declaration_elements, '') AS declaration_elements,

    -- 外箱数量：box_qty 字段示例 "1" / "5" / "500"（纯数字才迁移）
    CASE
        WHEN box_qty IS NOT NULL
             AND box_qty REGEXP '^[0-9]+$'
        THEN CAST(box_qty AS UNSIGNED)
        ELSE NULL
    END                    AS units_per_package,

    -- 其他
    NULLIF(remark, '')    AS remarks,
    last_used              AS last_used_date

FROM goods_staging
WHERE
    -- 必须有 SKU
    sku IS NOT NULL
    AND sku != ''
    -- 过滤占位记录
    AND sku NOT LIKE '%厂家%'
    AND sku NOT LIKE '%各一个%'
    -- 过滤纯中文货号（MySQL REGEXP 写法）
    AND NOT (
        LENGTH(sku) = CHAR_LENGTH(sku)
        AND sku REGEXP '^[一-龥]'
    )
    AND LENGTH(sku) > 1
    -- 至少有一个名称
    AND (name_en IS NOT NULL OR name_zh IS NOT NULL)
    -- 过滤无意义记录（unit_price IS NOT NULL 且 > 0）
    AND unit_price IS NOT NULL
    AND unit_price > 0
;

-- ============================================================
-- 步骤2：关联 product_factory（goods.factory_name → factory.id）
-- 注意：factory_name 需与 factory.factory_name 精确匹配
--       部分 goods.factory_name 含空格/简称，需人工核准
-- ============================================================

INSERT INTO product_factory (
    product_id,
    factory_id,
    supplier_sku,
    moq,
    unit_price_rmb,
    is_preferred,
    create_time,
    update_time
)
SELECT
    p.id                      AS product_id,
    f.id                      AS factory_id,
    g.sku                     AS supplier_sku,
    1                         AS moq,
    g.unit_price              AS unit_price_rmb,
    TRUE                      AS is_preferred,
    NOW()                     AS create_time,
    NOW()                     AS update_time
FROM goods_staging g
JOIN product p ON (
    CASE WHEN LOCATE('-', g.sku) > 0
         THEN SUBSTRING(g.sku, 1, LOCATE('-', g.sku) - 1)
         ELSE g.sku
    END = p.master_code
    AND
    CASE WHEN LOCATE('-', g.sku) > 0
         THEN SUBSTRING(g.sku, LOCATE('-', g.sku) + 1)
         ELSE NULL
    END = p.sub_code
)
JOIN factory f ON f.factory_name = g.factory_name
WHERE
    g.sku IS NOT NULL
    AND g.sku != ''
    AND g.factory_name IS NOT NULL
    AND g.factory_name != ''
    AND g.sku NOT LIKE '%厂家%'
    AND g.sku NOT LIKE '%各一个%'
    AND NOT (
        LENGTH(g.sku) = CHAR_LENGTH(g.sku)
        AND g.sku REGEXP '^[一-龥]'
    )
-- 防止重复插入：先删除已存在的关联，再插入新记录
-- 注意：ON DUPLICATE KEY UPDATE 对 product_factory JOIN 场景不适用，改用 DELETE + INSERT 模式
DELETE FROM product_factory
WHERE product_id IN (
    SELECT p.id
    FROM goods_staging g
    JOIN product p ON (
        CASE WHEN LOCATE('-', g.sku) > 0
             THEN SUBSTRING(g.sku, 1, LOCATE('-', g.sku) - 1)
             ELSE g.sku
        END = p.master_code
    )
    JOIN factory f ON f.factory_name = g.factory_name
    WHERE g.sku IS NOT NULL AND g.sku != ''
);

-- 再插入新关联
INSERT INTO product_factory (
    product_id, factory_id, supplier_sku, moq, unit_price_rmb, is_preferred, create_time, update_time
)
SELECT
    p.id, f.id, g.sku, 1, g.unit_price, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM goods_staging g
JOIN product p ON (
    CASE WHEN LOCATE('-', g.sku) > 0
         THEN SUBSTRING(g.sku, 1, LOCATE('-', g.sku) - 1)
         ELSE g.sku
    END = p.master_code
)
JOIN factory f ON f.factory_name = g.factory_name
WHERE
    g.sku IS NOT NULL AND g.sku != ''
    AND g.factory_name IS NOT NULL AND g.factory_name != ''
    AND g.sku NOT LIKE '%厂家%'
    AND g.sku NOT LIKE '%各一个%'
    AND NOT (
        LENGTH(g.sku) = CHAR_LENGTH(g.sku)
        AND g.sku REGEXP '^[一-龥]'
    );

-- ============================================================
-- 步骤3：更新 last_used_date（按 master_code 取最大日期）
-- ============================================================

UPDATE product p
JOIN (
    SELECT
        CASE WHEN LOCATE('-', sku) > 0
             THEN SUBSTRING(sku, 1, LOCATE('-', sku) - 1)
             ELSE sku
        END AS master_code,
        MAX(last_used) AS max_last_used
    FROM goods_staging
    WHERE last_used IS NOT NULL
    GROUP BY 1
) latest ON latest.master_code = p.master_code
SET p.last_used_date = latest.max_last_used;

-- ============================================================
-- 步骤4：迁移报告
-- ============================================================
SELECT
    'product'          AS entity,
    COUNT(*)           AS count,
    '总商品数（含子货号）' AS note
FROM product
WHERE create_by = 'system_migration'

UNION ALL

SELECT
    'product_factory'   AS entity,
    COUNT(DISTINCT product_id) AS count,
    '关联商品数'       AS note
FROM product_factory

UNION ALL

SELECT
    'product'           AS entity,
    COUNT(DISTINCT master_code) AS count,
    '去重主货号数'    AS note
FROM product
WHERE create_by = 'system_migration'

UNION ALL

SELECT
    '未匹配工厂'        AS entity,
    COUNT(DISTINCT factory_name) AS count,
    'goods.factory_name 无对应 factory.id' AS note
FROM goods_staging g
WHERE
    g.factory_name IS NOT NULL
    AND g.factory_name != ''
    AND NOT EXISTS (
        SELECT 1 FROM factory f WHERE f.factory_name = g.factory_name
    );

-- ============================================================
-- 步骤5：清理 staging 表（可选）
-- 迁移完成后可删除，保留以便后续人工核准
-- DROP TABLE IF EXISTS goods_staging;
-- ============================================================
