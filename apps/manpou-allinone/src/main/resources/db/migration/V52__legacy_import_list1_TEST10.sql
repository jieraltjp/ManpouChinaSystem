-- ============================================================
-- V52-TEST: list1 → procurement 测试导入（10 条样本）
-- 日期：2026-05-24
-- 用法：
--   1. mysql -u root -pmanpou23306 -h 192.168.13.202 -P 23306 --protocol=tcp --default-character-set=utf8mb4 manpou < 本文件
--   2. 验证：SELECT * FROM procurement WHERE legacy_is_legacy = 1;
--   3. 验证：SELECT * FROM legacy_id_mapping WHERE source_table = 'list1';
-- ============================================================

-- =============================================
-- Step 1: 创建源表
-- =============================================
DROP TABLE IF EXISTS legacy_import_list1;
CREATE TABLE legacy_import_list1 (
  `ID`              int(11)     NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `lockuser-id`     int(11)     DEFAULT NULL,
  `lockuser`        text,
  `locktime`        datetime    DEFAULT NULL,
  `updater-id`      int(11)     DEFAULT NULL,
  `updater`         text,
  `updatetime`      datetime    NOT NULL,
  `code`            text,
  `sub-code`        text,
  `img`             text,
  `item-name`       text,
  `order-group`     text        NOT NULL,
  `order-count`     int(11)     NOT NULL DEFAULT 0,
  `inspect-count`   int(11)     NOT NULL DEFAULT 0,
  `yoyaku-hasoubi` date,
  `arrival-depo`    text,
  `departure`       date,
  `arrival`         date,
  `arrival-jikan`   int(11)     NOT NULL DEFAULT 0,
  `arrival-flag`    int(11)     NOT NULL DEFAULT 0,
  `unit-ch`         double      NOT NULL DEFAULT 0,
  `total-ch`        double      NOT NULL DEFAULT 0,
  `unit-jp`         double      NOT NULL DEFAULT 0,
  `total-jp`        int(11)     NOT NULL DEFAULT 0,
  `rate`            double      NOT NULL DEFAULT 0,
  `fba-stock`       int(11)     NOT NULL DEFAULT 0,
  `houkoku`         varchar(50) NOT NULL DEFAULT '',
  `kaitsuke`        decimal(10,2) DEFAULT NULL,
  `hyoten`          decimal(5,4)  DEFAULT NULL,
  `kanpu`           varchar(10)  DEFAULT NULL,
  `ne-stock`        text        NOT NULL,
  `container`       text,
  `box-num`         text,
  `box-count`       int(11)     NOT NULL DEFAULT 0,
  `kg`              double      NOT NULL DEFAULT 0,
  `one-m3`          double      NOT NULL DEFAULT 0,
  `all-m3`          double      NOT NULL DEFAULT 0,
  `material`        text,
  `material-ch`      text,
  `height`          double      NOT NULL DEFAULT 0,
  `width`           double      NOT NULL DEFAULT 0,
  `depth`           double      NOT NULL DEFAULT 0,
  `info-file1`      text,
  `info-file2`      text,
  `note`            text        NOT NULL,
  `receive`         text,
  KEY `idx_l1_code`        (`code`(64)),
  KEY `idx_l1_order_group` (`order-group`(64)),
  KEY `idx_l1_updatetime`  (`updatetime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Step 2: 插入 10 条真实样本
-- 来源：d:\Programme\database\20260524\list1.sql
-- 注意：date NOT NULL 字段已改为允许 NULL（Step 1 中已调整），
--       原始数据中的 0000-00-00 存入 NULL，
--       真实导入时会通过 Step 2b UPDATE 修补
-- =============================================
INSERT INTO legacy_import_list1 VALUES
-- 行1: ID=17, ad011 蓝色, 2017, unit-ch=33, rate=17, arrival-flag=0, destination=空
('17', '0', '', null, '0', '中村さん', '2017-03-24 16:22:13', 'ad011', 'ブルー', 'fk', '', '', '90', '91', null, null, null, null, '0', '0', '33', '2970', '561', '50490', '17', '0', '', null, null, null, '', '', '', '9', '89', '0', '0.65', '', '', '0', '0', '0', '0', '0', '', null),
-- 行2: ID=12, ad024 蓝色, order-count=inspect-count=303 已完成
('12', '0', '', null, '0', '中村さん', '2017-03-24 16:16:50', 'ad024', 'ブルー', 'fk', '', '', '600', '303', null, null, null, null, '0', '0', '46.8', '28080', '796', '477360', '17', '0', '', null, null, null, '', '', '', '0', '0', '0', '0', '', '', '0', '0', '0', '0', '0', '', null),
-- 行3: ID=11, 吕さん, zk093
('11', '0', '', null, '0', '吕さん', '2021-05-30 14:27:42', 'zk093', '', 'zk093.jpg', '', '', '400', '317', null, null, null, null, '0', '0', '5.3', '1680.1', '90', '28530', '17', '0', '', null, null, null, '', '', '', '1', '21', '0', '0', '', '', '0', '0', '0', '0', '0', '', null),
-- 行4: ID=14, ad068 ブルー×グレー, 有 inspect-count
('14', '0', '', null, '0', '中村さん', '2017-03-24 16:16:50', 'ad068', 'ブルー×グレー', 'fk', '', '', '330', '330', null, null, null, null, '0', '0', '51.5', '16995', '876', '288915', '17', '0', '', null, null, null, '', '', '', '53', '790', '0', '5', '', '', '0', '0', '0', '0', '0', '', null),
-- 行5: ID=34, ap027 洞洞鞋, 有 item-name
('34', '0', '', null, '0', '中村さん', '2017-03-24 16:30:17', 'ap027', '水色23', 'fk', '洞洞鞋', '', '30', '29', null, null, null, null, '0', '0', '33', '990', '561', '16830', '17', '0', '', null, null, null, '', '', '', '7', '362.6', '0', '1.4', '', '', '0', '0', '0', '0', '0', '', null),
-- 行6: ID=46, zk133 黒, 有 material-ch
('46', '0', '', null, '0', '中村さん', '2017-03-24 16:32:05', 'zk133', '黒', 'fk', '卡包', '', '60', '60', null, null, null, null, '0', '0', '15.8', '948', '269', '16116', '17', '0', '', null, null, null, '', '', '', '1', '11.6', '0', '0.05', '', '', '0', '0', '0', '0', '0', '', null),
-- 行7: ID=19, zk110 无颜色, unit-ch=410, rate=17
('19', '0', '', null, '0', '中村さん', '2017-03-24 16:22:13', 'zk110', '', 'fk', '', '', '73', '30', null, null, null, null, '0', '0', '410', '29930', '6970', '508810', '17', '0', '', null, null, null, '', '', '', '30', '558', '0', '2.4', '', '', '0', '0', '0', '0', '0', '', null),
-- 行8: ID=20, ad021 绿色, 吕さん, unit-jp=1190
('20', '0', '', null, '0', '吕さん', '2018-06-11 11:28:50', 'ad021', 'グリーン', 'fk', '', '', '500', '40', null, null, null, null, '0', '0', '70', '2800', '1190', '47600', '17', '0', '', null, null, null, '', '', '', '12', '241.2', '0', '0', '', '', '0', '0', '0', '0', '0', '', null),
-- 行9: ID=405, pa042 1億, 佐藤さん, 有 img
('405', '0', '', null, '0', '佐藤さん', '2017-05-02 15:40:56', 'pa042', '1億', '钱.png', '金箔钱', '', '200', '200', null, null, null, null, '0', '0', '1.4', '280', '24', '4760', '17', '0', '', null, null, null, '', '', '', '0', '0', '0', '0', '', '', '0', '0', '0', '0', '0', '', null),
-- 行10: ID=746, zk101 蓝, 晓さん, 有 fba-stock
('746', '0', '', null, '0', '晓さん', '2017-05-02 15:49:56', 'zk101', '蓝', 'fk', '', '', '20', '18', null, null, null, null, '0', '0', '42', '756', '714', '12852', '17', '0', '', null, null, null, '', '', '', '0', '0', '0', '0', '', '', '0', '0', '0', '0', '0', '', null);

-- =============================================
-- Step 3: INSERT...SELECT → procurement
-- =============================================
-- 说明：list1.code 是子货号（ny528-sihu），需通过 product 表查主货号
INSERT INTO procurement (
    product_code, sub_product_code, quantity,
    shipped_quantity,
    price_rmb, exchange_rate, tax_point,
    destination,
    planned_ship_date, actual_ship_date,
    material, customs_remarks,
    china_lead,
    status,
    billing_type,
    shiban,
    group_,
    remark,
    instruction_manual,
    legacy_list1_id, legacy_order_group, legacy_item_name,
    legacy_img, legacy_inspect_qty, legacy_fba_stock,
    legacy_container_no, legacy_updater, legacy_updatetime,
    legacy_is_legacy,
    create_by, create_time, update_by, update_time,
    is_deleted
)
SELECT
    -- product_code: 用 product 表查主货号
    COALESCE(p.master_code, TRIM(src.code)) AS product_code,
    -- sub_product_code: 子货号（list1.code 本身）
    TRIM(src.code)                          AS sub_product_code,
    src.`order-count`                              AS quantity,
    src.`inspect-count`                            AS shipped_quantity,
    NULLIF(src.`unit-ch`, 0)                       AS price_rmb,
    NULLIF(src.rate, 0)                            AS exchange_rate,
    NULLIF(src.hyoten, 0)                          AS tax_point,
    NULLIF(TRIM(src.`arrival-depo`), '')           AS destination,
    src.`yoyaku-hasoubi`                           AS planned_ship_date,
    src.departure                                  AS actual_ship_date,
    -- material: 中文材质（日本名在 product.material_ja）
    NULLIF(TRIM(p.material), '')                    AS material,
    NULLIF(TRIM(src.note), '')                      AS customs_remarks,
    NULLIF(TRIM(src.material), '')                  AS china_lead,
    '完了'                                          AS status,
    -- billing_type: houkoku → billing_type
    CASE
        WHEN src.houkoku = '超慧退税' THEN 'CHAO_HUI_TUI_SHUI'
        WHEN NULLIF(TRIM(src.houkoku), '') IS NOT NULL THEN 'OTHER'
        ELSE NULL
    END                                             AS billing_type,
    -- shiban: sub-code 支番（颜色）
    NULLIF(TRIM(src.`sub-code`), '')               AS shiban,
    -- group_: order-group 团体
    NULLIF(TRIM(src.`order-group`), '')             AS group_,
    -- remark: item-name 备注
    NULLIF(TRIM(src.`item-name`), '')               AS remark,
    -- instruction_manual: info-file1 + info-file2
    CONCAT_WS('，',
        CASE WHEN src.`info-file1` IS NOT NULL AND TRIM(src.`info-file1`) NOT IN ('', '×')
             THEN '自' END,
        CASE WHEN src.`info-file2` IS NOT NULL AND TRIM(src.`info-file2`) NOT IN ('', '×')
             THEN '工' END
    )                                                AS instruction_manual,
    src.ID                                          AS legacy_list1_id,
    TRIM(src.`order-group`)                         AS legacy_order_group,
    TRIM(src.`item-name`)                           AS legacy_item_name,
    TRIM(src.img)                                   AS legacy_img,
    src.`inspect-count`                             AS legacy_inspect_qty,
    src.`fba-stock`                                 AS legacy_fba_stock,
    NULLIF(TRIM(src.container), '')                AS legacy_container_no,
    TRIM(src.updater)                               AS legacy_updater,
    src.updatetime                                  AS legacy_updatetime,
    1                                               AS legacy_is_legacy,
    TRIM(src.updater)                               AS create_by,
    src.updatetime                                  AS create_time,
    TRIM(src.updater)                               AS update_by,
    src.updatetime                                  AS update_time,
    FALSE                                           AS is_deleted
FROM legacy_import_list1 src
LEFT JOIN product p ON p.sub_code COLLATE utf8mb4_unicode_ci = TRIM(src.code)
WHERE src.code IS NOT NULL
  AND TRIM(src.code) != ''
  AND TRIM(src.code) != '無関係'
  AND TRIM(src.code) NOT IN (
      SELECT UPPER(product_code) FROM procurement
      WHERE is_deleted = FALSE AND legacy_is_legacy = FALSE
  );

-- =============================================
-- Step 4: 关联 Container（注：procurement 无 container_id FK，container_no 已在 legacy_container_no 中）
-- 如需关联，用 container 表的 legacy_import_list1.container_no 匹配 Container.container_no
-- UPDATE procurement p
-- JOIN container c ON c.container_no = p.legacy_container_no
-- SET p.container_id = c.id
-- WHERE p.legacy_is_legacy = TRUE AND p.container_id IS NULL AND c.id IS NOT NULL;


-- =============================================
-- Step 5: 记录 legacy ID 映射
-- =============================================
INSERT INTO legacy_id_mapping (source_table, legacy_id, target_table, target_id, product_code, import_status)
SELECT
    'list1'             AS source_table,
    src.ID              AS legacy_id,
    'procurement'       AS target_table,
    p.id                AS target_id,
    TRIM(src.code) AS product_code,
    'imported'          AS import_status
FROM legacy_import_list1 src
JOIN procurement p ON p.legacy_list1_id = src.ID
WHERE src.code IS NOT NULL AND TRIM(src.code) != ''
  AND TRIM(src.code) != '無関係';

-- =============================================
-- Step 6: 验证查询
-- =============================================
SELECT '=== procurement legacy (10 rows) ===' AS info;
SELECT id, product_code, sub_product_code, quantity,
       price_rmb, exchange_rate, destination,
       planned_ship_date, actual_ship_date,
       shiban, group_, remark, instruction_manual, billing_type,
       status, legacy_list1_id, legacy_updater, legacy_is_legacy
FROM procurement WHERE legacy_is_legacy = 1;

SELECT '=== legacy_id_mapping list1 ===' AS info;
SELECT * FROM legacy_id_mapping WHERE source_table = 'list1';

-- 清理临时表（保留用于审计）
-- DROP TABLE IF EXISTS legacy_import_list1;
