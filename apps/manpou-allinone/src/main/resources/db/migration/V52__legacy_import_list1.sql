-- ============================================================
-- V52: list1 历史数据导入 procurement
-- 日期：2026-05-24
-- 方式：INSERT...SELECT 直接从 legacy_import_list1 读取
--
-- 前置步骤（手动执行）：
--   1. 创建临时表 legacy_import_list1（见下方）
--   2. 用 sed 将原始 SQL 表名替换后导入：
--      sed "s/INSERT INTO \`list1\`/INSERT INTO legacy_import_list1/g" \
--          "d:/Programme/database/20260524/list1.sql" \
--      | mysql -u root -p manpou
--   3. 验证：SELECT COUNT(*) FROM legacy_import_list1;
-- ============================================================

-- =============================================
-- Step 1: 创建源表 legacy_import_list1
-- 结构同 sddb0040100537.list1，用于接收原始 INSERT
-- =============================================
CREATE TABLE IF NOT EXISTS legacy_import_list1 (
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
  `yoyaku-hasoubi`  date        DEFAULT NULL,
  `arrival-depo`    text        DEFAULT NULL,
  `departure`       date        DEFAULT NULL,
  `arrival`         date        DEFAULT NULL,
  `arrival-jikan`   int(11)     NOT NULL DEFAULT 0,
  `arrival-flag`    int(11)     NOT NULL DEFAULT 0,
  `unit-ch`         double      NOT NULL DEFAULT 0,
  `total-ch`        double      NOT NULL DEFAULT 0,
  `unit-jp`         double      NOT NULL DEFAULT 0,
  `total-jp`        int(11)    NOT NULL DEFAULT 0,
  `rate`            double      NOT NULL DEFAULT 0,
  `fba-stock`       int(11)     NOT NULL DEFAULT 0,
  `houkoku`         varchar(50) NOT NULL DEFAULT '',
  `kaitsuke`        decimal(10,2) DEFAULT NULL COMMENT '買付(元)',
  `hyoten`          decimal(5,4)  DEFAULT NULL COMMENT '票点',
  `kanpu`           varchar(10)  DEFAULT NULL COMMENT '還付',
  `ne-stock`        text        NOT NULL,
  `container`       text,
  `box-num`         text,
  `box-count`       int(11)     NOT NULL DEFAULT 0,
  `kg`              double      NOT NULL DEFAULT 0,
  `one-m3`          double      NOT NULL DEFAULT 0,
  `all-m3`          double      NOT NULL DEFAULT 0,
  `material`        text,
  `material-ch`     text,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='list1历史数据临时表（2026-05-24导入）';

-- =============================================
-- Step 2: 加载原始数据（前置步骤，参见文件头部注释）
-- 执行命令后继续往下
-- =============================================
-- 【手动执行】在 mysql 客户端或 shell 中执行：
-- \! sed "s/INSERT INTO \`list1\`/INSERT INTO legacy_import_list1/g" \
--       "d:/Programme/database/20260524/list1.sql" \
--   | mysql -u root -p manpou

-- 验证加载结果（确认 >70000 行）
-- SELECT COUNT(*) FROM legacy_import_list1;

-- =============================================
-- Step 2b: 修补零日期（0000-00-00 → NULL）
-- 注意：MySQL strict 模式下需用 sed 在导入时处理（见 docs/DB-LEGACY-*.md）
--   sed -e "s/'0000-00-00'/NULL/g" list1.sql | mysql ...
-- 此处不执行 UPDATE（sed 处理后数据已无 0000-00-00）


-- =============================================
-- Step 3: INSERT...SELECT 写入 procurement
-- =============================================
-- 说明：list1.code 是子货号，主货号通过 product 表 LEFT JOIN 获取
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
    -- product_code: product 表查主货号，查不到则用 code 本身
    COALESCE(p.master_code, TRIM(src.code))  AS product_code,
    -- sub_product_code: 子货号 = list1.code
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
      WHERE is_deleted = FALSE
        AND legacy_is_legacy = FALSE
  );

-- =============================================
-- =============================================
-- Step 4: 关联 Container（注：procurement 无 container_id FK，container_no 已在 legacy_container_no 中）
-- 如需关联（可选），用 legacy_container_no 匹配 Container.container_no：
-- UPDATE procurement p
-- JOIN container c ON c.container_no = p.legacy_container_no
-- SET p.container_id = c.id
-- WHERE p.legacy_is_legacy = TRUE AND p.container_id IS NULL AND c.id IS NOT NULL;


-- =============================================
-- Step 5: 记录 legacy ID 映射
-- =============================================

-- 5a. 记录成功导入的映射
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
WHERE src.code IS NOT NULL
  AND TRIM(src.code) != ''
  AND TRIM(src.code) != '無関係';

-- 5b. 记录跳过的映射（新系统已存在）
INSERT INTO legacy_id_mapping (source_table, legacy_id, target_table, target_id, product_code, import_status, skip_reason)
SELECT
    'list1'                            AS source_table,
    src.ID                             AS legacy_id,
    'procurement'                      AS target_table,
    NULL                               AS target_id,
    TRIM(src.code)              AS product_code,
    'skipped'                          AS import_status,
    'duplicate_code_in_procurement'     AS skip_reason
FROM legacy_import_list1 src
WHERE src.code IS NOT NULL
  AND TRIM(src.code) != ''
  AND TRIM(src.code) != '無関係'
  AND TRIM(src.code) IN (
      SELECT UPPER(product_code) FROM procurement
      WHERE is_deleted = FALSE AND legacy_is_legacy = FALSE
  );

-- =============================================
-- Step 6: 清理临时表
-- =============================================
-- DROP TABLE IF EXISTS legacy_import_list1;
