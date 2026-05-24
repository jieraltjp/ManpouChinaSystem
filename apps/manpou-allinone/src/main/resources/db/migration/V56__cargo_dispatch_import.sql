-- ============================================================
-- V56: cargo_dispatch 表创建 + list8 历史数据导入
-- 日期：2026-05-24
-- 依赖：V53（legacy_import_list8 临时表 + Factory 导入）
--
-- 前置步骤（已在 V53 执行）：
--   1. legacy_import_list8 表已创建并加载 3661 条记录
--   2. sed 导入命令：
--      sed "s/INSERT INTO \`list8\`/INSERT INTO legacy_import_list8/g" \
--          "d:/Programme/database/20260524/list8.sql" \
--      | mysql -u root -p manpou
-- ============================================================

-- =============================================
-- Step 1: 创建 cargo_dispatch 表（匹配 JPA Entity）
-- ============================================================
CREATE TABLE IF NOT EXISTS cargo_dispatch (
  `id`             BIGINT       AUTO_INCREMENT PRIMARY KEY,
  `code`          TEXT         NOT NULL COMMENT '发货编号',
  `manager`       TEXT         NOT NULL COMMENT '负责人',
  `destination`   TEXT         NOT NULL COMMENT '目的地',
  `tax`           TEXT         COMMENT '税种',
  `material`      TEXT         COMMENT '材质',
  `kensa`         VARCHAR(255) DEFAULT NULL COMMENT '检验',
  `quantity`      INT          NOT NULL DEFAULT 0 COMMENT '数量',
  `pieces`        INT          NOT NULL DEFAULT 0 COMMENT '件数',
  `weight`        DOUBLE       NOT NULL DEFAULT 0 COMMENT '净重',
  `weight2`       DOUBLE       NOT NULL DEFAULT 0 COMMENT '毛重',
  `length`        DOUBLE       NOT NULL DEFAULT 0 COMMENT '长度',
  `location`      TEXT         COMMENT '地点',
  `dispatch_date` DATE         DEFAULT NULL COMMENT '发货日期',
  `status`        TEXT         COMMENT '状态',
  `other`         TEXT         COMMENT '其他备注',
  `unit_price`    DOUBLE       NOT NULL DEFAULT 0 COMMENT '单价',
  `rate`          DOUBLE       NOT NULL DEFAULT 0 COMMENT '汇率',
  `warehouse`     TEXT         COMMENT '仓库',
  `factory_addr`  VARCHAR(255) DEFAULT NULL COMMENT '工厂地址',
  `show_flag`     INT          NOT NULL DEFAULT 0 COMMENT '显示标识 0=活跃 1=归档',
  `rireki`        LONGTEXT     DEFAULT NULL COMMENT '历史记录',
  -- BaseEntity 审计字段
  `create_time`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `create_by`     VARCHAR(64)  NOT NULL DEFAULT 'SYSTEM',
  `update_time`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `update_by`     VARCHAR(64)  NOT NULL DEFAULT 'SYSTEM',
  `is_deleted`    TINYINT(1)   NOT NULL DEFAULT FALSE,
  INDEX `idx_dispatch_code`       (`code`(64)),
  INDEX `idx_dispatch_destination`(`destination`(64)),
  INDEX `idx_dispatch_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='货物发送整理（list8 历史数据，V56 导入）';

-- =============================================
-- Step 2: 从 legacy_import_list8 导入数据
-- 列映射：
--   list8.ID          → (AUTO)
--   list8.code        → code
--   list8.manager     → manager
--   list8.destination → destination
--   list8.tax         → tax
--   list8.material    → material
--   list8.kensa       → kensa
--   list8.num         → quantity
--   list8.pieces      → pieces
--   list8.weight      → weight
--   list8.weight2     → weight2
--   list8.length      → length
--   list8.location    → location
--   list8.date1       → dispatch_date
--   list8.status      → status
--   list8.other       → other
--   list8.unit_ch     → unit_price
--   list8.rate        → rate
--   list8.souko       → warehouse
--   list8.factory_addr→ factory_addr
--   list8.updateuser  → update_by
--   list8.updatetime  → update_time
--   list8.showFlag    → show_flag
--   list8.rireki      → rireki
-- =============================================
INSERT INTO cargo_dispatch (
  code, manager, destination, tax, material, kensa,
  quantity, pieces, weight, weight2, length,
  location, dispatch_date, status, other,
  unit_price, rate, warehouse, factory_addr,
  show_flag, rireki,
  create_by, create_time, update_by, update_time, is_deleted
)
SELECT
  TRIM(src.code)                                              AS code,
  TRIM(src.manager)                                            AS manager,
  TRIM(src.destination)                                        AS destination,
  COALESCE(NULLIF(TRIM(src.tax), ''), '')                     AS tax,
  COALESCE(NULLIF(TRIM(src.material), ''), '')                 AS material,
  NULLIF(TRIM(src.kensa), '')                                  AS kensa,
  COALESCE(src.num, 0)                                         AS quantity,
  COALESCE(src.pieces, 0)                                      AS pieces,
  COALESCE(src.weight, 0)                                      AS weight,
  COALESCE(src.weight2, 0)                                     AS weight2,
  COALESCE(src.length, 0)                                      AS length,
  COALESCE(NULLIF(TRIM(src.location), ''), '')                 AS location,
  -- date1 可能为 '0000-00-00'，转为 NULL
  CASE
    WHEN src.date1 IS NULL OR src.date1 <= '1970-01-01' THEN NULL
    ELSE src.date1
  END                                                          AS dispatch_date,
  COALESCE(NULLIF(TRIM(src.status), ''), '')                   AS status,
  COALESCE(NULLIF(TRIM(src.other), ''), '')                    AS other,
  COALESCE(src.unit_ch, 0)                                     AS unit_price,
  COALESCE(src.rate, 0)                                        AS rate,
  COALESCE(NULLIF(TRIM(src.souko), ''), '')                    AS warehouse,
  src.factory_addr                                              AS factory_addr,
  COALESCE(src.showFlag, 0)                                    AS show_flag,
  src.rireki                                                   AS rireki,
  'SYSTEM'                                                     AS create_by,
  NOW(3)                                                       AS create_time,
  TRIM(src.updateuser)                                         AS update_by,
  COALESCE(src.updatetime, NOW(3))                             AS update_time,
  FALSE                                                        AS is_deleted
FROM legacy_import_list8 src
WHERE TRIM(src.code) != '' AND TRIM(src.code) IS NOT NULL;

-- =============================================
-- Step 3: 验证
-- =============================================
SELECT
  'cargo_dispatch' AS tbl,
  COUNT(*)          AS total_rows,
  SUM(IF(is_deleted = FALSE, 1, 0)) AS active_rows,
  SUM(IF(show_flag = 1, 1, 0))      AS archived_rows,
  MIN(update_time)  AS oldest_update,
  MAX(update_time)  AS newest_update
FROM cargo_dispatch;
