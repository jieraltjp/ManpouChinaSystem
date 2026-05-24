-- ============================================================
-- V53-TEST: list8 → logistics_plan + factory 测试导入（10 条样本）
-- 日期：2026-05-24
-- 用法：
--   1. mysql -u root -pmanpou23306 -h 192.168.13.202 -P 23306 manpou < 本文件
--   2. 验证：SELECT * FROM logistics_plan WHERE legacy_is_legacy = 1;
--   3. 验证：SELECT * FROM factory WHERE legacy_source = 'list8';
--   4. 验证：SELECT * FROM legacy_id_mapping WHERE source_table = 'list8';
-- ============================================================

-- =============================================
-- Step 1: 创建源表
-- =============================================
DROP TABLE IF EXISTS legacy_import_list8;
CREATE TABLE legacy_import_list8 (
  `ID`           int(11)      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `code`         text         NOT NULL,
  `manager`      text         NOT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=3662 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Step 2: 插入 10 条真实样本
-- 注意：date1 列使用 null 代替 '0000-00-00'（NO_ZERO_DATE 模式）
-- 来源：d:\Programme\database\20260524\list8.sql
-- 覆盖场景：
--   行1: ID=1,  weight2=0(无分离), showFlag=1(活跃), date1=有效
--   行2: ID=3,  showFlag=0, status=完成 → DELIVERED
--   行3: ID=4,  weight2=0, 目的地=名古屋
--   行4: ID=55, weight2=500>weight=500(等重,无分离), showFlag=0
--   行5: ID=56, weight2=560>weight=540(有分离) → net=540, gross=560, 有factory_addr
--   行6: ID=57, weight2=2922>weight=2790(有分离) → net=2790, gross=2922, 工厂名
--   行7: ID=46, 目的地=久留米, showFlag=0, 完成
--   行8: ID=50, 有other备注, showFlag=0, 完成
--   行9: ID=54, 其他=超长备注, showFlag=0, 完成
--   行10: ID=5,  date1=0000-00-00(无效日期), showFlag=1
-- =============================================
INSERT INTO legacy_import_list8 VALUES
-- 行1: ID=1, ny362, weight2=0(无分离), showFlag=1(活跃), unit_ch=0
('1', 'ny362', '段', '名古屋', '', '铁+木板商检', null, '12', '100', '1350', '0', '8', '菏泽', '2024-01-15', '', '', '0', '0', '', null, '2024-01-16 14:26:26', '中村賢司', '1', null),
-- 行2: ID=3, ny361, showFlag=0, status=完成 → DELIVERED
('3', 'ny361', '段', '大阪', '', '铁+木板商检', null, '12', '150', '1800', '0', '10', '菏泽', '2024-01-15', '完成', '', '0', '0', '', null, '2024-01-16 14:26:27', '趙暁剣', '0', null),
-- 行3: ID=4, ns019, 张, 名古屋, weight2=0, showFlag=0
('4', 'ns019', '张', '名古屋', '', '钢板', null, '14', '80', '4500', '0', '75', '河南', '2024-01-11', '完成', '', '0', '0', '', null, '2024-01-16 14:26:28', '趙暁剣', '0', null),
-- 行4: ID=55, ee321, weight2=500=weight=500(等重,无分离), showFlag=0, 有other备注
('55', 'ee321', '雪', '名古屋', '', '橡塑', null, '200', '200', '500', '600', '15', '廊坊', '2026-01-14', '完成', '毛重和净重等装柜时候再确认下', '0', '0', '', null, '2026-01-13 15:02:06', '赵金湘', '0', null),
-- 行5: ID=56, sg060, weight2=560>weight=540(有分离!), 有factory_addr
('56', 'sg060', '雪', '名古屋', '', '钢板', null, '20', '20', '540', '560', '3.5', '武强', '2026-03-31', '完成', 'null', '535', '10', '沧州米特尔汽车维修设备有限公司', null, '2026-04-01 14:26:03', '陈娅', '0', null),
-- 行6: ID=57, ee346, weight2=2922>weight=2790(有分离), 有factory_addr, rate=10
('57', 'ee346', '雪', '名古屋', '', '铁', null, '60', '120', '2790', '2922', '3', '武强', '2025-12-31', '完成', '本周装柜', '726', '10', '沧州米特尔汽车维修设备有限公司', null, '2025-12-31 13:00:22', '赵金湘', '0', null),
-- 行7: ID=46, sg111, 张, 久留米, showFlag=0, 完成
('46', 'sg111', '张', '久留米', '', '塑料', null, '2800', '2800', '4480', '0', '65', '河北霸州', '2024-02-28', '完成', '', '0', '0', '', null, '2024-03-01 11:36:24', '燕平', '0', null),
-- 行8: ID=50, od592-kf, 有other备注, showFlag=0
('50', 'od592-kf', '车', '名古屋', '', '塑料+木腿+铁', null, '100', '100', '820', '0', '13', '河北霸州', '2024-03-12', '完成', '一箱里面2把椅子', '0', '0', '', null, '2024-03-14 18:11:51', '燕平', '0', null),
-- 行9: ID=54, od592-bl, 其他=超长备注, showFlag=0
('54', 'od592-bl', '车', '名古屋', '', '塑料+木腿+铁', null, '150', '150', '1230', '0', '20', '河北霸州', '2024-03-12', '完成', '一箱里面2把椅子一条整柜能装548箱尺寸53*52.5*46', '0', '0', '', null, '2024-03-14 18:03:15', '燕平', '0', null),
-- 行10: ID=5, ee377-s, date1=0000-00-00(无效日期→Step1b补丁), showFlag=1(活跃), pieces=1
('5', 'ee377-s', '段', '名古屋', '', '铁', null, '12', '73', '416', '0', '1', '河北廊坊', null, '', '', '0', '0', '', null, '2024-01-15 16:22:28', '中村賢司', '1', null);

-- =============================================
-- Step 3: 创建 Factory（仓库）
-- =============================================

-- 类型A：location 为城市名
INSERT INTO factory (factory_code, factory_name, province, city, county, category,
                     cooperation_status, payment_terms, needs_qc,
                     legacy_source, legacy_souko, legacy_location,
                     legacy_is_warehouse, create_by, create_time,
                     update_by, update_time, is_deleted)
SELECT DISTINCT
    CONCAT('F-L8-', LEFT(MD5(CONCAT(TRIM(src.location), '-', src.ID)), 6)) AS factory_code,
    CONCAT(TRIM(src.location), '-', src.ID)         AS factory_name,
    CASE
        WHEN TRIM(src.location) IN ('菏泽','济南','青岛','烟台','泰安') THEN '山东省'
        WHEN TRIM(src.location) IN ('东莞','深圳','广州','佛山','惠州','中山','揭阳') THEN '广东省'
        WHEN TRIM(src.location) IN ('保定','廊坊','邢台','沧州','衡水','武强','河北廊坊','河北武强','河北保定','河北霸州') THEN '河北省'
        WHEN TRIM(src.location) LIKE '%河南%' THEN '河南省'
        WHEN TRIM(src.location) LIKE '%河北%' THEN '河北省'
        WHEN TRIM(src.location) LIKE '%山东%' THEN '山东省'
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
  AND TRIM(src.location) NOT LIKE '%有限公司%'
  AND TRIM(src.location) NOT LIKE '%株式会社%'
  AND NOT EXISTS (
      SELECT 1 FROM factory f
      WHERE f.legacy_source = 'list8'
        AND f.legacy_location = TRIM(src.location)
  );

-- 类型B：souko 含公司名（工厂）
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
-- Step 4: INSERT...SELECT → logistics_plan
-- =============================================
INSERT INTO logistics_plan (
    plan_code, product_code, quantity,
    cargo_length_cm, cargo_weight_kg, net_weight_kg, gross_weight_kg,
    actual_ship_date,
    remarks, status,
    plan_type,
    legacy_list8_id, legacy_pieces, legacy_destination,
    legacy_warehouse, legacy_location, legacy_material, legacy_kensa,
    legacy_show_flag, legacy_status,
    legacy_unit_ch, legacy_rate,
    legacy_updater, legacy_updatetime,
    legacy_is_legacy,
    create_by, create_time, update_by, update_time,
    is_deleted
)
SELECT
    CONCAT('L-LEGACY-', src.ID)                    AS plan_code,
    UPPER(TRIM(src.code))                          AS product_code,
    src.num                                        AS quantity,
    NULLIF(src.length, 0)                         AS cargo_length_cm,
    CASE
        WHEN src.weight2 > src.weight AND src.weight2 > 0
            THEN src.weight
        ELSE src.weight
    END                                            AS cargo_weight_kg,
    CASE WHEN src.weight2 > src.weight AND src.weight2 > 0
         THEN src.weight ELSE NULL END             AS net_weight_kg,
    CASE WHEN src.weight2 > src.weight AND src.weight2 > 0
         THEN src.weight2 ELSE NULL END            AS gross_weight_kg,
    src.date1                                        AS actual_ship_date,
    NULLIF(TRIM(src.other), '')                   AS remarks,
    CASE
        WHEN src.showFlag = 0 AND src.status = '完成'
            THEN 'DELIVERED'
        ELSE 'PACKED'
    END                                            AS status,
    'SEA'                                          AS plan_type,
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
    NULLIF(src.rate, 0)                           AS legacy_rate,
    TRIM(src.updateuser)                          AS legacy_updater,
    src.updatetime                               AS legacy_updatetime,
    1                                              AS legacy_is_legacy,
    'SYSTEM'                                       AS create_by,
    src.updatetime                                 AS create_time,
    'SYSTEM'                                       AS update_by,
    src.updatetime                                 AS update_time,
    FALSE                                          AS is_deleted
FROM legacy_import_list8 src
WHERE TRIM(src.code) != '' AND TRIM(src.code) IS NOT NULL
  AND UPPER(TRIM(src.code)) NOT IN (
      SELECT UPPER(product_code) FROM logistics_plan
      WHERE is_deleted = FALSE AND legacy_is_legacy = FALSE
  );

-- =============================================
-- Step 5: 关联 factory_id
-- =============================================
UPDATE logistics_plan lp
JOIN factory f ON f.legacy_souko = lp.legacy_warehouse
SET lp.factory_id = f.id
WHERE lp.legacy_is_legacy = TRUE
  AND lp.factory_id IS NULL
  AND f.legacy_is_warehouse = TRUE
  AND f.legacy_souko IS NOT NULL
  AND TRIM(f.legacy_souko) != '';

UPDATE logistics_plan lp
JOIN factory f ON f.legacy_location = lp.legacy_location
SET lp.factory_id = f.id
WHERE lp.legacy_is_legacy = TRUE
  AND lp.factory_id IS NULL
  AND f.legacy_is_warehouse = TRUE;

-- =============================================
-- Step 6: 关联 procurement_id
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
-- Step 7: 记录 legacy ID 映射
-- =============================================
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

-- =============================================
-- Step 8: 验证查询
-- =============================================
SELECT '=== logistics_plan legacy ===' COLLATE utf8mb4_unicode_ci AS msg
UNION ALL
SELECT CONCAT('id=',id,' plan_code=',plan_code,
               ' product=',product_code,
               ' qty=',quantity,
               ' weight=',cargo_weight_kg,
               ' net=',IFNULL(net_weight_kg,'null'),
               ' gross=',IFNULL(gross_weight_kg,'null'),
               ' status=',status,
               ' factory_id=',IFNULL(factory_id,'null'))
FROM logistics_plan WHERE legacy_is_legacy = 1
UNION ALL
SELECT '=== factory list8 ===' COLLATE utf8mb4_unicode_ci AS msg
UNION ALL
SELECT CONCAT('id=',id,' name=',factory_name,' province=',province,' city=',city)
FROM factory WHERE legacy_source = 'list8'
UNION ALL
SELECT '=== legacy_id_mapping ===' COLLATE utf8mb4_unicode_ci AS msg
UNION ALL
SELECT CONCAT('source=',source_table,' legacy_id=',legacy_id,
               ' target=',target_table,' target_id=',IFNULL(target_id,'null'),
               ' status=',import_status)
FROM legacy_id_mapping WHERE source_table = 'list8';

-- 清理临时表（保留用于审计）
-- DROP TABLE IF EXISTS legacy_import_list8;
