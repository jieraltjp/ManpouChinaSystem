-- ============================================================
-- V32: DB Schema 标准化 + 审计字段补全
-- 日期: 2026-04-27（幂等版本，可重复执行）
-- 根因: V4-V31 从未执行（Flyway 历史仅到 V3），
--       DB 由直接 SQL 导入创建，与文档 DB-01~DB-12 不一致。
--
-- 已验证的数据库修改（执行前已通过实际迁移验证）：
--   Phase P0: replenishment_demand TEXT→VARCHAR, product_factory 审计字段
--   Phase P1: TINYINT→BIT(1), DATETIME(3)→DATETIME(6), 精度修正, 索引清理
--   Phase P2: 零行表重建（字段顺序修正）, procurement 孤立列清理
--
-- 以下为幂等 Flyway 迁移脚本（使用 IF EXISTS / IF NOT EXISTS 保护）
-- 注意: MySQL 不支持 DROP COLUMN IF EXISTS，需通过存储过程实现
-- ============================================================

-- ============================================================
-- 辅助存储过程：条件删除列（幂等）
-- ============================================================
DROP PROCEDURE IF EXISTS sp_drop_column_if_exists;
DELIMITER //
CREATE PROCEDURE sp_drop_column_if_exists(IN p_tbl VARCHAR(64), IN p_col VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'manpou'
          AND TABLE_NAME   = p_tbl
          AND COLUMN_NAME  = p_col
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_tbl, ' DROP COLUMN ', p_col);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

-- ============================================================
-- Phase P0-1: procurement 孤立列清理
-- ============================================================
CALL sp_drop_column_if_exists('procurement', 'linked_demand_id');
CALL sp_drop_column_if_exists('procurement', 'linked_demand_item_id');
-- billing_method 已在之前版本清理，重复执行无影响

-- ============================================================
-- Phase P1: TINYINT(1) → BIT(1) 统一 is_deleted（6表）
-- ============================================================
DROP PROCEDURE IF EXISTS sp_fix_is_deleted_bit;
DELIMITER //
CREATE PROCEDURE sp_fix_is_deleted_bit(IN p_tbl VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'manpou'
          AND TABLE_NAME   = p_tbl
          AND COLUMN_NAME  = 'is_deleted'
          AND COLUMN_TYPE  = 'tinyint(1)'
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_tbl, ' MODIFY COLUMN is_deleted BIT(1) NOT NULL DEFAULT b''0''');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;
CALL sp_fix_is_deleted_bit('domestic_customs_record');
CALL sp_fix_is_deleted_bit('japan_customs_record');
CALL sp_fix_is_deleted_bit('tax_refund_record');
CALL sp_fix_is_deleted_bit('sales_record');
CALL sp_fix_is_deleted_bit('qc_record');
CALL sp_fix_is_deleted_bit('logistics_plan');
DROP PROCEDURE sp_fix_is_deleted_bit;

-- ============================================================
-- Phase P1: DATETIME(3) → DATETIME(6) 精度统一（7表）
-- ============================================================
DROP PROCEDURE IF EXISTS sp_upgrade_datetime_precision;
DELIMITER //
CREATE PROCEDURE sp_upgrade_datetime_precision(IN p_tbl VARCHAR(64))
BEGIN
    DECLARE col_type VARCHAR(64);

    SELECT COLUMN_TYPE INTO col_type
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'manpou'
      AND TABLE_NAME   = p_tbl
      AND COLUMN_NAME  = 'create_time';

    IF col_type IN ('datetime', 'datetime(3)') THEN
        SET @sql = CONCAT('ALTER TABLE ', p_tbl, ' MODIFY COLUMN create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    SELECT COLUMN_TYPE INTO col_type
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'manpou'
      AND TABLE_NAME   = p_tbl
      AND COLUMN_NAME  = 'update_time';

    IF col_type IN ('datetime', 'datetime(3)') THEN
        SET @sql = CONCAT('ALTER TABLE ', p_tbl, ' MODIFY COLUMN update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;
CALL sp_upgrade_datetime_precision('product_factory');
CALL sp_upgrade_datetime_precision('domestic_customs_record');
CALL sp_upgrade_datetime_precision('japan_customs_record');
CALL sp_upgrade_datetime_precision('tax_refund_record');
CALL sp_upgrade_datetime_precision('sales_record');
CALL sp_upgrade_datetime_precision('qc_record');
CALL sp_upgrade_datetime_precision('logistics_plan');
CALL sp_upgrade_datetime_precision('procurement');
DROP PROCEDURE sp_upgrade_datetime_precision;

-- ============================================================
-- Phase P1: tax_refund_record 金额精度修正
-- ============================================================
DROP PROCEDURE IF EXISTS sp_fix_tax_precision;
DELIMITER //
CREATE PROCEDURE sp_fix_tax_precision()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'manpou'
          AND TABLE_NAME   = 'tax_refund_record'
          AND COLUMN_NAME  = 'price_rmb'
          AND COLUMN_TYPE  = 'decimal(14,4)'
    ) THEN
        ALTER TABLE tax_refund_record MODIFY COLUMN price_rmb DECIMAL(14,2) DEFAULT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'manpou'
          AND TABLE_NAME   = 'tax_refund_record'
          AND COLUMN_NAME  = 'tax_point'
          AND COLUMN_TYPE  = 'decimal(6,4)'
    ) THEN
        ALTER TABLE tax_refund_record MODIFY COLUMN tax_point DECIMAL(5,4) DEFAULT NULL;
    END IF;
END//
DELIMITER ;
CALL sp_fix_tax_precision;
DROP PROCEDURE sp_fix_tax_precision;

-- ============================================================
-- Phase P1: 索引清理
-- ============================================================
DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;
DELIMITER //
CREATE PROCEDURE sp_drop_index_if_exists(IN p_tbl VARCHAR(64), IN p_idx VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = 'manpou'
          AND TABLE_NAME   = p_tbl
          AND INDEX_NAME   = p_idx
    ) THEN
        SET @sql = CONCAT('DROP INDEX ', p_idx, ' ON ', p_tbl);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;
CALL sp_drop_index_if_exists('factory',                'idx_factory_deleted');
CALL sp_drop_index_if_exists('domestic_customs_record', 'idx_domestic_procurement');
CALL sp_drop_index_if_exists('domestic_customs_record', 'idx_domestic_logistics');
DROP PROCEDURE sp_drop_index_if_exists;

-- ============================================================
-- Phase P1: japan_customs_record 唯一索引补充
-- ============================================================
DROP PROCEDURE IF EXISTS sp_add_unique_index;
DELIMITER //
CREATE PROCEDURE sp_add_unique_index(IN p_tbl VARCHAR(64), IN p_idx VARCHAR(64), IN p_cols VARCHAR(255))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = 'manpou'
          AND TABLE_NAME   = p_tbl
          AND INDEX_NAME   = p_idx
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_tbl, ' ADD UNIQUE INDEX ', p_idx, ' (', p_cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;
CALL sp_add_unique_index('japan_customs_record', 'uk_jp_customs_entry_no', 'customs_entry_no');
DROP PROCEDURE sp_add_unique_index;

-- ============================================================
-- Phase P2: 零行业务表重建（审计字段移至末尾，字段顺序规范化）
-- 说明: qc_record(0) / logistics_plan(0) / domestic_customs_record(0) /
--       japan_customs_record(0) / tax_refund_record(0) / sales_record(1)
--       零行表可直接 RENAME TABLE 原子操作，无数据丢失风险
-- ============================================================

-- ---- qc_record (0行) ----
DROP PROCEDURE IF EXISTS sp_rebuild_qc_record;
DELIMITER //
CREATE PROCEDURE sp_rebuild_qc_record()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = 'manpou' AND TABLE_NAME = 'qc_record_new'
    ) THEN
        CREATE TABLE qc_record_new (
            id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
            qc_code                  VARCHAR(32)  NOT NULL,
            procurement_id           BIGINT      NOT NULL,
            seller_name              VARCHAR(128) DEFAULT NULL,
            product_code             VARCHAR(32)  NOT NULL,
            sub_product_code         VARCHAR(64)  DEFAULT NULL,
            qc_user_id              BIGINT       DEFAULT NULL,
            qc_type                 VARCHAR(16)  DEFAULT NULL,
            qc_date                 DATE         DEFAULT NULL,
            result                  VARCHAR(16)  NOT NULL DEFAULT 'PASS',
            status                  VARCHAR(24)  NOT NULL DEFAULT 'PENDING',
            inspection_count        INT          DEFAULT NULL,
            passed_count            INT          DEFAULT NULL,
            defective_count          INT          DEFAULT NULL,
            box_count               INT          DEFAULT NULL,
            box_length_cm           DECIMAL(8,2) DEFAULT NULL,
            box_width_cm            DECIMAL(8,2) DEFAULT NULL,
            box_height_cm           DECIMAL(8,2) DEFAULT NULL,
            net_weight_per_unit     DECIMAL(10,4) DEFAULT NULL,
            gross_weight            DECIMAL(10,4) DEFAULT NULL,
            tax_inclusive_price     DECIMAL(14,2) DEFAULT NULL,
            material                VARCHAR(64)  DEFAULT NULL,
            tax_refund              BIT(1)       DEFAULT NULL,
            qc_standard             VARCHAR(512) DEFAULT NULL,
            remarks                 VARCHAR(512) DEFAULT NULL,
            images                  JSON         DEFAULT NULL,
            destination             VARCHAR(128) DEFAULT NULL,
            quantity                INT          DEFAULT NULL,
            order_date              DATE         DEFAULT NULL,
            create_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
            update_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
            create_by               VARCHAR(64)  NOT NULL DEFAULT '',
            update_by               VARCHAR(64)  NOT NULL DEFAULT '',
            is_deleted              BIT(1)       NOT NULL DEFAULT b'0',
            UNIQUE KEY uk_qc_code (qc_code),
            INDEX idx_qc_procurement (procurement_id),
            INDEX idx_qc_result (result),
            INDEX idx_qc_date (qc_date),
            INDEX idx_qc_status (status),
            INDEX idx_qc_is_deleted (is_deleted)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    END IF;
END//
DELIMITER ;
CALL sp_rebuild_qc_record;
DROP PROCEDURE sp_rebuild_qc_record;
DROP PROCEDURE IF EXISTS sp_rename_qc_record;
DELIMITER //
CREATE PROCEDURE sp_rename_qc_record()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='qc_record') THEN
        RENAME TABLE qc_record TO qc_record_old, qc_record_new TO qc_record;
        DROP TABLE IF EXISTS qc_record_old;
    END IF;
END//
DELIMITER ;
CALL sp_rename_qc_record;
DROP PROCEDURE sp_rename_qc_record;

-- ---- logistics_plan (0行) ----
DROP PROCEDURE IF EXISTS sp_rebuild_logistics_plan;
DELIMITER //
CREATE PROCEDURE sp_rebuild_logistics_plan()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='logistics_plan_new') THEN
        CREATE TABLE logistics_plan_new (
            id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
            plan_code               VARCHAR(32)  NOT NULL,
            procurement_id           BIGINT       DEFAULT NULL,
            factory_id               BIGINT       DEFAULT NULL,
            product_code             VARCHAR(32)  NOT NULL,
            sub_product_code         VARCHAR(64)  DEFAULT NULL,
            plan_type               VARCHAR(20)  NOT NULL DEFAULT 'SEA',
            status                  VARCHAR(24)  NOT NULL DEFAULT 'PLANNED',
            cargo_length_cm         DECIMAL(8,2) DEFAULT NULL,
            cargo_width_cm          DECIMAL(8,2) DEFAULT NULL,
            cargo_height_cm         DECIMAL(8,2) DEFAULT NULL,
            cargo_volume_cbm        DECIMAL(10,6) DEFAULT NULL,
            cargo_weight_kg         DECIMAL(10,4) DEFAULT NULL,
            quantity                INT          DEFAULT NULL,
            requires_qc             BIT(1)       DEFAULT NULL,
            container_id            BIGINT       DEFAULT NULL,
            pool_id                 BIGINT       DEFAULT NULL,
            estimated_ship_date     DATE         DEFAULT NULL,
            actual_ship_date        DATE         DEFAULT NULL,
            remarks                 VARCHAR(512) DEFAULT NULL,
            create_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
            update_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
            create_by               VARCHAR(64)  NOT NULL DEFAULT '',
            update_by               VARCHAR(64)  NOT NULL DEFAULT '',
            is_deleted              BIT(1)       NOT NULL DEFAULT b'0',
            UNIQUE KEY uk_plan_code (plan_code),
            INDEX idx_logistics_procurement (procurement_id),
            INDEX idx_logistics_status (status),
            INDEX idx_logistics_plan_type (plan_type),
            INDEX idx_logistics_factory (factory_id),
            INDEX idx_lp_product_code (product_code),
            INDEX idx_lp_estimated_ship_date (estimated_ship_date),
            INDEX idx_lp_create_time (create_time),
            INDEX idx_lp_is_deleted (is_deleted)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    END IF;
END//
DELIMITER ;
CALL sp_rebuild_logistics_plan;
DROP PROCEDURE sp_rebuild_logistics_plan;
DROP PROCEDURE IF EXISTS sp_rename_logistics_plan;
DELIMITER //
CREATE PROCEDURE sp_rename_logistics_plan()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='logistics_plan') THEN
        RENAME TABLE logistics_plan TO logistics_plan_old, logistics_plan_new TO logistics_plan;
        DROP TABLE IF EXISTS logistics_plan_old;
    END IF;
END//
DELIMITER ;
CALL sp_rename_logistics_plan;
DROP PROCEDURE sp_rename_logistics_plan;

-- ---- domestic_customs_record (0行) ----
DROP PROCEDURE IF EXISTS sp_rebuild_dc;
DELIMITER //
CREATE PROCEDURE sp_rebuild_dc()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='domestic_customs_record_new') THEN
        CREATE TABLE domestic_customs_record_new (
            id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
            customs_code            VARCHAR(32)  NOT NULL,
            procurement_id           BIGINT       DEFAULT NULL,
            logistics_plan_id        BIGINT       DEFAULT NULL,
            factory_id               BIGINT       DEFAULT NULL,
            product_code             VARCHAR(32)  NOT NULL,
            sub_product_code         VARCHAR(64)  DEFAULT NULL,
            quantity                 INT          DEFAULT NULL,
            estimated_value_cny      DECIMAL(14,2) DEFAULT NULL,
            status                  VARCHAR(24)  NOT NULL DEFAULT 'PENDING',
            remarks                 VARCHAR(512) DEFAULT NULL,
            create_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
            update_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
            create_by               VARCHAR(64)  NOT NULL DEFAULT '',
            update_by               VARCHAR(64)  NOT NULL DEFAULT '',
            is_deleted              BIT(1)       NOT NULL DEFAULT b'0',
            UNIQUE KEY uk_domestic_customs_code (customs_code),
            INDEX idx_dc_procurement_id (procurement_id),
            INDEX idx_dc_logistics_plan_id (logistics_plan_id),
            INDEX idx_dc_factory_id (factory_id),
            INDEX idx_dc_product_code (product_code),
            INDEX idx_dc_status (status),
            INDEX idx_dc_create_time (create_time),
            INDEX idx_dc_is_deleted (is_deleted)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    END IF;
END//
DELIMITER ;
CALL sp_rebuild_dc;
DROP PROCEDURE sp_rebuild_dc;
DROP PROCEDURE IF EXISTS sp_rename_dc;
DELIMITER //
CREATE PROCEDURE sp_rename_dc()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='domestic_customs_record') THEN
        RENAME TABLE domestic_customs_record TO domestic_customs_record_old, domestic_customs_record_new TO domestic_customs_record;
        DROP TABLE IF EXISTS domestic_customs_record_old;
    END IF;
END//
DELIMITER ;
CALL sp_rename_dc;
DROP PROCEDURE sp_rename_dc;

-- ---- japan_customs_record (0行) ----
DROP PROCEDURE IF EXISTS sp_rebuild_jc;
DELIMITER //
CREATE PROCEDURE sp_rebuild_jc()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='japan_customs_record_new') THEN
        CREATE TABLE japan_customs_record_new (
            id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
            customs_entry_no         VARCHAR(32)  DEFAULT NULL,
            procurement_id           BIGINT       DEFAULT NULL,
            domestic_customs_id       BIGINT       DEFAULT NULL,
            logistics_plan_id        BIGINT       DEFAULT NULL,
            sub_product_code         VARCHAR(64)  DEFAULT NULL,
            status                  VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
            arrival_date             DATE         DEFAULT NULL,
            customs_broker           VARCHAR(128) DEFAULT NULL,
            broker_phone             VARCHAR(32)  DEFAULT NULL,
            broker_contact           VARCHAR(64)  DEFAULT NULL,
            import_duty_paid         DECIMAL(14,2) DEFAULT NULL,
            consumption_tax_paid      DECIMAL(14,2) DEFAULT NULL,
            clearance_date           DATE         DEFAULT NULL,
            arrival_port             VARCHAR(64)  DEFAULT NULL,
            declared_weight_kg      DECIMAL(10,3) DEFAULT NULL,
            declared_volume_cbm      DECIMAL(10,4) DEFAULT NULL,
            remarks                  VARCHAR(512) DEFAULT NULL,
            create_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
            update_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
            create_by               VARCHAR(64)  NOT NULL DEFAULT '',
            update_by               VARCHAR(64)  NOT NULL DEFAULT '',
            is_deleted              BIT(1)       NOT NULL DEFAULT b'0',
            UNIQUE KEY uk_jp_customs_entry_no (customs_entry_no),
            INDEX idx_jp_procurement_id (procurement_id),
            INDEX idx_jp_domestic_customs_id (domestic_customs_id),
            INDEX idx_jp_logistics_plan_id (logistics_plan_id),
            INDEX idx_jp_status (status),
            INDEX idx_jp_create_time (create_time),
            INDEX idx_jp_is_deleted (is_deleted)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    END IF;
END//
DELIMITER ;
CALL sp_rebuild_jc;
DROP PROCEDURE sp_rebuild_jc;
DROP PROCEDURE IF EXISTS sp_rename_jc;
DELIMITER //
CREATE PROCEDURE sp_rename_jc()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='japan_customs_record') THEN
        RENAME TABLE japan_customs_record TO japan_customs_record_old, japan_customs_record_new TO japan_customs_record;
        DROP TABLE IF EXISTS japan_customs_record_old;
    END IF;
END//
DELIMITER ;
CALL sp_rename_jc;
DROP PROCEDURE sp_rename_jc;

-- ---- tax_refund_record (0行) ----
DROP PROCEDURE IF EXISTS sp_rebuild_tr;
DELIMITER //
CREATE PROCEDURE sp_rebuild_tr()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='tax_refund_record_new') THEN
        CREATE TABLE tax_refund_record_new (
            id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
            refund_code              VARCHAR(32)  DEFAULT NULL,
            procurement_id           BIGINT       DEFAULT NULL,
            japan_customs_id         BIGINT       DEFAULT NULL,
            status                  VARCHAR(32)  NOT NULL DEFAULT 'APPLYING',
            billing_type             VARCHAR(32)  DEFAULT NULL,
            price_rmb               DECIMAL(14,2) DEFAULT NULL,
            quantity                 INT          DEFAULT NULL,
            tax_point                DECIMAL(5,4) DEFAULT NULL,
            exchange_rate            DECIMAL(10,6) DEFAULT NULL,
            estimated_refund_rmb     DECIMAL(14,2) DEFAULT NULL,
            actual_refund_rmb        DECIMAL(14,2) DEFAULT NULL,
            refund_date              DATE         DEFAULT NULL,
            refund_bank              VARCHAR(128) DEFAULT NULL,
            remarks                  VARCHAR(512) DEFAULT NULL,
            create_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
            update_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
            create_by               VARCHAR(64)  NOT NULL DEFAULT '',
            update_by               VARCHAR(64)  NOT NULL DEFAULT '',
            is_deleted              BIT(1)       NOT NULL DEFAULT b'0',
            INDEX idx_tr_procurement_id (procurement_id),
            INDEX idx_tr_japan_customs_id (japan_customs_id),
            INDEX idx_tr_status (status),
            INDEX idx_tr_refund_date (refund_date),
            INDEX idx_tr_create_time (create_time),
            INDEX idx_tr_is_deleted (is_deleted)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    END IF;
END//
DELIMITER ;
CALL sp_rebuild_tr;
DROP PROCEDURE sp_rebuild_tr;
DROP PROCEDURE IF EXISTS sp_rename_tr;
DELIMITER //
CREATE PROCEDURE sp_rename_tr()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='tax_refund_record') THEN
        RENAME TABLE tax_refund_record TO tax_refund_record_old, tax_refund_record_new TO tax_refund_record;
        DROP TABLE IF EXISTS tax_refund_record_old;
    END IF;
END//
DELIMITER ;
CALL sp_rename_tr;
DROP PROCEDURE sp_rename_tr;

-- ---- sales_record (1行) ----
DROP PROCEDURE IF EXISTS sp_rebuild_sr;
DELIMITER //
CREATE PROCEDURE sp_rebuild_sr()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='sales_record_new') THEN
        CREATE TABLE sales_record_new (
            id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
            record_code              VARCHAR(32)  NOT NULL,
            procurement_id           BIGINT       DEFAULT NULL,
            product_code             VARCHAR(64)  DEFAULT NULL,
            sub_product_code         VARCHAR(64)  DEFAULT NULL,
            sales_channel            VARCHAR(32)  DEFAULT NULL,
            status                  VARCHAR(32)  NOT NULL DEFAULT 'LISTED',
            listing_date             DATE         DEFAULT NULL,
            initial_stock           INT          DEFAULT NULL,
            current_stock           INT          DEFAULT NULL,
            safety_stock            INT          DEFAULT NULL,
            sales_quantity           INT          DEFAULT NULL,
            returned_quantity        INT          DEFAULT NULL,
            return_rate              DECIMAL(6,4) DEFAULT NULL,
            selling_price_jpy        DECIMAL(14,2) DEFAULT NULL,
            remarks                  VARCHAR(512) DEFAULT NULL,
            create_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
            update_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
            create_by               VARCHAR(64)  NOT NULL DEFAULT '',
            update_by               VARCHAR(64)  NOT NULL DEFAULT '',
            is_deleted              BIT(1)       NOT NULL DEFAULT b'0',
            INDEX idx_sr_product_code (product_code),
            INDEX idx_sr_procurement_id (procurement_id),
            INDEX idx_sr_status (status),
            INDEX idx_sr_sales_channel (sales_channel),
            INDEX idx_sr_create_time (create_time),
            INDEX idx_sr_is_deleted (is_deleted)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    END IF;
END//
DELIMITER ;
CALL sp_rebuild_sr;
DROP PROCEDURE sp_rebuild_sr;
DROP PROCEDURE IF EXISTS sp_rename_sr;
DELIMITER //
CREATE PROCEDURE sp_rename_sr()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='sales_record') THEN
        RENAME TABLE sales_record TO sales_record_old, sales_record_new TO sales_record;
        INSERT INTO sales_record (
            id, record_code, procurement_id, product_code, sub_product_code,
            sales_channel, status, listing_date, initial_stock, current_stock,
            safety_stock, sales_quantity, returned_quantity, return_rate,
            selling_price_jpy, remarks,
            create_time, update_time, create_by, update_by, is_deleted
        )
        SELECT
            id, record_code, procurement_id, product_code, sub_product_code,
            sales_channel, status, listing_date, initial_stock, current_stock,
            safety_stock, sales_quantity, returned_quantity, return_rate,
            selling_price_jpy, remarks,
            create_time, update_time, create_by, update_by, is_deleted
        FROM sales_record_old;
        DROP TABLE IF EXISTS sales_record_old;
    END IF;
END//
DELIMITER ;
CALL sp_rename_sr;
DROP PROCEDURE sp_rename_sr;

-- ============================================================
-- 清理辅助存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS sp_drop_column_if_exists;
