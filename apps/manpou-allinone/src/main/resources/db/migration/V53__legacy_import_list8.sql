-- ============================================================
-- V53: list8 历史数据导入 logistics_plan + factory
-- 日期：2026-05-24
-- 方式：INSERT...SELECT 直接从 legacy_import_list8 读取
--
-- 前置步骤（手动执行）：
--   1. 创建临时表 legacy_import_list8（见下方）
--   2. 用 sed 将原始 SQL 表名替换后导入：
--      sed "s/INSERT INTO \`list8\`/INSERT INTO legacy_import_list8/g" \
--          "d:/Programme/database/20260524/list8.sql" \
--      | mysql -u root -p manpou
--   3. 验证：SELECT COUNT(*) FROM legacy_import_list8;
-- ============================================================

-- =============================================
-- Step 1: 创建源表 legacy_import_list8
-- =============================================
CREATE TABLE IF NOT EXISTS legacy_import_list8 (
  `ID`           int(11)      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `code`         text         NOT NULL,
  `manager`       text         NOT NULL,
  `destination`  text         NOT NULL,
  `tax`          text         NOT NULL,
  `material`     text         NOT NULL,
  `kensa`        varchar(255) DEFAULT NULL,
  `num`          int(11)      NOT NULL,
  `pieces`       int(11)      NOT NULL,
  `weight`       double       NOT NULL,
  `weight2`      double       NOT NULL,
  `length`       double       NOT NULL,
  `location`     text         NOT NULL,
  `date1`        date         DEFAULT NULL,
  `status`       text         NOT NULL,
  `other`        text         NOT NULL,
  `unit_ch`      double       NOT NULL,
  `rate`         double       NOT NULL,
  `souko`        text         NOT NULL,
  `factory_addr` varchar(255) DEFAULT NULL,
  `updatetime`   datetime     NOT NULL,
  `updateuser`   text         NOT NULL,
  `showFlag`     int(11)      NOT NULL DEFAULT 0,
  `rireki`       longtext,
  KEY `idx_l8_code`     (`code`(64)),
  KEY `idx_l8_updatetime` (`updatetime`)
) ENGINE=InnoDB AUTO_INCREMENT=3662 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='list8历史数据临时表（2026-05-24导入）';

-- 【手动执行】加载数据：
-- \! sed "s/INSERT INTO \`list8\`/INSERT INTO legacy_import_list8/g" \
--       "d:/Programme/database/20260524/list8.sql" \
--   | mysql -u root -p manpou

-- =============================================
-- Step 1b: 修补零日期（0000-00-00 → NULL）
-- 注意：MySQL strict 模式下需用方案①或②（见下）
-- =============================================
-- 方案①（推荐）：sed 在导入时直接替换 date1 值：
--   sed -e "s/'0000-00-00'/NULL/g" list8.sql | mysql ...
-- 方案②（备选）：导入后用字符串替换方式清理：
-- UPDATE legacy_import_list8 SET date1 = NULL WHERE date1 <= '1970-01-01' AND date1 IS NOT NULL;
-- （用数值比较绕过 date literal 解析问题）


-- =============================================
-- Step 2: 创建 list8 仓库/工厂 Factory
-- list8.souko 90%+ 为空字符串，降级用 location
-- =============================================

-- 类型A：location 为城市名 → 创建 WAREHOUSE 类型 Factory
INSERT INTO factory (factory_code, factory_name, province, city, county, category,
                     cooperation_status, payment_terms, needs_qc,
                     legacy_source, legacy_souko, legacy_location,
                     legacy_is_warehouse, create_by, create_time,
                     update_by, update_time, is_deleted)
SELECT DISTINCT
    CONCAT('F-L8-', LEFT(MD5(CONCAT(TRIM(src.location), '-', src.ID)), 6)) AS factory_code,
    CONCAT(TRIM(src.location), '-', src.ID)         AS factory_name,
    CASE
        WHEN TRIM(src.location) IN ('菏泽','济南','青岛','烟台','泰安')
            THEN '山东省'
        WHEN TRIM(src.location) IN ('东莞','深圳','广州','佛山','惠州','中山','揭阳')
            THEN '广东省'
        WHEN TRIM(src.location) IN ('保定','廊坊','邢台','沧州','衡水','武强','河北廊坊','河北武强','河北保定','河北霸州')
            THEN '河北省'
        WHEN TRIM(src.location) IN ('泉州','厦门','福州','丽水')
            THEN '福建省'
        WHEN TRIM(src.location) IN ('常州','苏州','无锡','南通')
            THEN '江苏省'
        WHEN TRIM(src.location) IN ('嘉兴','宁波','杭州','温州','义乌')
            THEN '浙江省'
        WHEN TRIM(src.location) IN ('高碑')
            THEN '山东省'
        WHEN TRIM(src.location) LIKE '%河南%'
            THEN '河南省'
        WHEN TRIM(src.location) LIKE '%河北%'
            THEN '河北省'
        WHEN TRIM(src.location) LIKE '%山东%'
            THEN '山东省'
        WHEN TRIM(src.location) LIKE '%广东%'
            THEN '广东省'
        WHEN TRIM(src.location) LIKE '%浙江%'
            THEN '浙江省'
        ELSE '山东省'
    END                                              AS province,
    TRIM(src.location)                              AS city,
    ''                                                AS county,
    'OTHER'                                          AS category,
    'ACTIVE'                                         AS cooperation_status,
    'NET_60'                                         AS payment_terms,
    FALSE                                             AS needs_qc,
    'list8'                                         AS legacy_source,
    NULLIF(TRIM(src.souko), '')                   AS legacy_souko,
    TRIM(src.location)                              AS legacy_location,
    TRUE                                             AS legacy_is_warehouse,
    'SYSTEM', NOW(3), 'SYSTEM', NOW(3), FALSE

FROM legacy_import_list8 src
WHERE TRIM(src.code) != '' AND TRIM(src.code) IS NOT NULL
  AND TRIM(src.location) != '' AND TRIM(src.location) IS NOT NULL
  -- 排除非城市名
  AND TRIM(src.location) NOT LIKE '%有限公司%'
  AND TRIM(src.location) NOT LIKE '%株式会社%'
  -- 排除已存在
  AND NOT EXISTS (
      SELECT 1 FROM factory f
      WHERE f.legacy_source = 'list8'
        AND f.legacy_location = TRIM(src.location)
  );

-- 类型B：souko 含公司名（工厂）→ 创建 Factory
INSERT INTO factory (factory_code, factory_name, province, city, county, rough_location, category,
                     cooperation_status, payment_terms, needs_qc,
                     legacy_source, legacy_souko, legacy_location,
                     legacy_is_warehouse, create_by, create_time,
                     update_by, update_time, is_deleted)
SELECT DISTINCT
    CONCAT('F-L8-', LEFT(MD5(CONCAT(TRIM(src.souko), '-', src.ID)), 6)) AS factory_code,
    CONCAT(TRIM(SUBSTRING_INDEX(src.souko, '有限公司', 1)), '-', src.ID) AS factory_name,
    CASE
        WHEN TRIM(src.factory_addr) LIKE '%山东%'  THEN '山东省'
        WHEN TRIM(src.factory_addr) LIKE '%广东%'  THEN '广东省'
        WHEN TRIM(src.factory_addr) LIKE '%河北%'  THEN '河北省'
        WHEN TRIM(src.factory_addr) LIKE '%河南%'  THEN '河南省'
        WHEN TRIM(src.factory_addr) LIKE '%浙江%'  THEN '浙江省'
        WHEN TRIM(src.factory_addr) LIKE '%江苏%'  THEN '江苏省'
        ELSE '山东省'
    END                                              AS province,
    TRIM(src.souko)                                AS city,
    ''                                                AS county,
    TRIM(src.factory_addr)                          AS rough_location,
    'OTHER'                                          AS category,
    'ACTIVE'                                         AS cooperation_status,
    'NET_60'                                         AS payment_terms,
    FALSE                                             AS needs_qc,
    'list8'                                         AS legacy_source,
    TRIM(src.souko)                                AS legacy_souko,
    TRIM(src.location)                              AS legacy_location,
    TRUE                                             AS legacy_is_warehouse,
    'SYSTEM', NOW(3), 'SYSTEM', NOW(3), FALSE

FROM legacy_import_list8 src
WHERE TRIM(src.code) != '' AND TRIM(src.code) IS NOT NULL
  AND TRIM(src.souko) != '' AND TRIM(src.souko) IS NOT NULL
  AND TRIM(src.souko) != TRIM(src.location)
  AND (TRIM(src.souko) LIKE '%有限公司%'
       OR TRIM(src.souko) LIKE '%株式会社%')
  AND NOT EXISTS (
      SELECT 1 FROM factory f
      WHERE f.legacy_source = 'list8'
        AND f.legacy_souko = TRIM(src.souko)
  );

-- =============================================
-- Step 3: INSERT...SELECT 写入 logistics_plan
-- =============================================
INSERT INTO logistics_plan (
    -- === 主业务字段 ===
    plan_code, product_code, quantity,
    cargo_length_cm, cargo_weight_kg, net_weight_kg, gross_weight_kg,
    actual_ship_date,
    remarks, status,
    plan_type,

    -- === legacy 溯源 ===
    legacy_list8_id, legacy_pieces, legacy_destination,
    legacy_warehouse, legacy_location, legacy_material, legacy_kensa,
    legacy_show_flag, legacy_status,
    legacy_unit_ch, legacy_rate,
    legacy_updater, legacy_updatetime,
    legacy_is_legacy,

    -- === 审计字段 ===
    create_by, create_time, update_by, update_time,
    is_deleted
)
SELECT
    -- plan_code: 唯一约束，用 legacy ID 生成
    CONCAT('L-LEGACY-', src.ID)                    AS plan_code,

    -- product_code: UPPER + TRIM
    UPPER(TRIM(src.code))                          AS product_code,

    -- quantity: num（装箱数量）
    src.num                                        AS quantity,

    -- cargo_length_cm: list8 只有长
    NULLIF(src.length, 0)                         AS cargo_length_cm,

    -- weight 策略：
    -- weight2 > weight → weight2=毛重，weight=净重 → cargo_weight_kg=weight(净重)
    -- 否则 → cargo_weight_kg = weight（总重）
    CASE
        WHEN src.weight2 > src.weight AND src.weight2 > 0
            THEN src.weight   -- 净重
        ELSE src.weight      -- 总重
    END                                            AS cargo_weight_kg,

    -- net_weight_kg: 仅 weight2>weight 时填充
    CASE WHEN src.weight2 > src.weight AND src.weight2 > 0
         THEN src.weight ELSE NULL END             AS net_weight_kg,

    -- gross_weight_kg: 仅 weight2>weight 时填充
    CASE WHEN src.weight2 > src.weight AND src.weight2 > 0
         THEN src.weight2 ELSE NULL END            AS gross_weight_kg,

    -- actual_ship_date: date1，已在 Step 1b 清理过 NULL 值
    src.date1                                        AS actual_ship_date,

    -- remarks: other
    NULLIF(TRIM(src.other), '')                   AS remarks,

    -- status: showFlag=0 + status='完成' → DELIVERED；否则 → PACKED
    CASE
        WHEN src.showFlag = 0 AND src.status = '完成'
            THEN 'DELIVERED'
        ELSE 'PACKED'
    END                                            AS status,

    -- plan_type: 默认海运
    'SEA'                                          AS plan_type,

    -- === legacy 溯源 ===
    src.ID                                         AS legacy_list8_id,
    src.pieces                                     AS legacy_pieces,
    TRIM(src.destination)                           AS legacy_destination,
    TRIM(src.souko)                               AS legacy_warehouse,
    TRIM(src.location)                              AS legacy_location,
    TRIM(src.material)                            AS legacy_material,
    src.kensa                                      AS legacy_kensa,
    src.showFlag                                   AS legacy_show_flag,
    src.status                                     AS legacy_status,
    NULLIF(src.unit_ch, 0)                        AS legacy_unit_ch,
    NULLIF(src.rate, 0)                          AS legacy_rate,
    TRIM(src.updateuser)                          AS legacy_updater,
    src.updatetime                               AS legacy_updatetime,
    1                                              AS legacy_is_legacy,

    -- === 审计字段 ===
    'SYSTEM'                                       AS create_by,
    src.updatetime                                 AS create_time,
    'SYSTEM'                                       AS update_by,
    src.updatetime                                 AS update_time,
    FALSE                                          AS is_deleted

FROM legacy_import_list8 src
WHERE TRIM(src.code) != '' AND TRIM(src.code) IS NOT NULL
  -- 排除新系统已有记录（按 code 查重）
  AND UPPER(TRIM(src.code)) NOT IN (
      SELECT UPPER(product_code) FROM logistics_plan
      WHERE is_deleted = FALSE
        AND legacy_is_legacy = FALSE
  );

-- =============================================
-- Step 4: 关联 factory_id（两步：souko→location）
-- =============================================

-- 优先用 souko 匹配（工厂名精确）
UPDATE logistics_plan lp
JOIN factory f ON f.legacy_souko = lp.legacy_warehouse
SET lp.factory_id = f.id
WHERE lp.legacy_is_legacy = TRUE
  AND lp.factory_id IS NULL
  AND f.legacy_is_warehouse = TRUE
  AND f.legacy_souko IS NOT NULL
  AND TRIM(f.legacy_souko) != '';

-- 其次用 location 匹配（城市名）
UPDATE logistics_plan lp
JOIN factory f ON f.legacy_location = lp.legacy_location
SET lp.factory_id = f.id
WHERE lp.legacy_is_legacy = TRUE
  AND lp.factory_id IS NULL
  AND f.legacy_is_warehouse = TRUE;

-- =============================================
-- Step 5: 关联 procurement_id（通过 product_code）
-- =============================================
UPDATE logistics_plan lp
JOIN procurement p
    ON p.product_code = lp.product_code
   AND (p.sub_product_code = lp.sub_product_code
        OR p.sub_product_code IS NULL
        OR lp.sub_product_code IS NULL)
SET lp.procurement_id = p.id
WHERE lp.legacy_is_legacy = TRUE
  AND lp.procurement_id IS NULL;

-- =============================================
-- Step 6: 记录 legacy ID 映射
-- =============================================

-- 6a. 成功导入的映射
INSERT INTO legacy_id_mapping (source_table, legacy_id, target_table, target_id, product_code, import_status)
SELECT
    'list8'                                 AS source_table,
    src.ID                                  AS legacy_id,
    'logistics_plan'                        AS target_table,
    lp.id                                   AS target_id,
    UPPER(TRIM(src.code))                   AS product_code,
    'imported'                              AS import_status
FROM legacy_import_list8 src
JOIN logistics_plan lp ON lp.legacy_list8_id = src.ID
WHERE TRIM(src.code) != '' AND TRIM(src.code) IS NOT NULL
  AND UPPER(TRIM(src.code)) NOT IN (
      SELECT UPPER(product_code) FROM logistics_plan
      WHERE is_deleted = FALSE AND legacy_is_legacy = FALSE
  );

-- 6b. 跳过的映射（新系统已存在）
INSERT INTO legacy_id_mapping (source_table, legacy_id, target_table, target_id, product_code, import_status, skip_reason)
SELECT
    'list8'                            AS source_table,
    src.ID                             AS legacy_id,
    'logistics_plan'                   AS target_table,
    NULL                               AS target_id,
    UPPER(TRIM(src.code))              AS product_code,
    'skipped'                          AS import_status,
    'duplicate_code_in_logistics_plan'  AS skip_reason
FROM legacy_import_list8 src
WHERE TRIM(src.code) != '' AND TRIM(src.code) IS NOT NULL
  AND UPPER(TRIM(src.code)) IN (
      SELECT UPPER(product_code) FROM logistics_plan
      WHERE is_deleted = FALSE AND legacy_is_legacy = FALSE
  );

-- =============================================
-- Step 7: 清理临时表
-- =============================================
-- DROP TABLE IF EXISTS legacy_import_list8;
