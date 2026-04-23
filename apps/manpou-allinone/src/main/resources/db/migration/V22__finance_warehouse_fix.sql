-- ============================================================
-- Flyway 迁移脚本：精度修复 + Example 表冲突隔离
-- INTJ 审计 2026-04-22 扩展修复
-- ============================================================
-- 修复：
--   FIN-01: tax_refund_record.price_rmb DECIMAL(14,2) → (14,4)（与 Java 对齐，防止截断）
--   FIN-02: tax_refund_record.tax_point DECIMAL(5,4) → (6,4)（与 Java taxPoint 对齐）
--   FIN-03: tax_refund_record.estimated/actual_refund_rmb DECIMAL(14,2) → (14,4)
--   JP-01: japan_customs_record.customs_entry_no VARCHAR(32) → (64)（与 DB-06 文档对齐）
--   JP-02: japan_customs_record.declared_weight_kg DECIMAL(10,3) → (10,4)
--   WARE-01: warehouse_example 表隔离（原 WarehouseExample 错误映射到 example 表）
--   NOTE-01: notification_example 表隔离（原 NotificationExample 错误映射到 example 表）
-- ============================================================
-- 依赖：V13__tax_refund_record_table.sql, V12__japan_customs_record_table.sql
-- ============================================================

-- ===== 1. Finance 精度修复 =====

-- price_rmb: DB 2位小数，但 Java taxPoint 可能传4位，截断风险
ALTER TABLE tax_refund_record
    MODIFY COLUMN price_rmb DECIMAL(14,4) COMMENT '含税人民币单价';

-- tax_point: DB 最大值 9.9999，但票点计算可能超（如 1.1300 = scale 4）
-- precision 从 5→6（6位总精度含小数点左侧）
ALTER TABLE tax_refund_record
    MODIFY COLUMN tax_point DECIMAL(6,4) COMMENT '税点（如1.13）';

-- estimated/actual_refund_rmb: 退税金额可能含4位小数精度
ALTER TABLE tax_refund_record
    MODIFY COLUMN estimated_refund_rmb DECIMAL(14,4) COMMENT '预估退税金额（RMB）';

ALTER TABLE tax_refund_record
    MODIFY COLUMN actual_refund_rmb DECIMAL(14,4) COMMENT '实际退税金额（RMB）';

-- ===== 2. Japan Customs 精度修复 =====

-- customs_entry_no: VARCHAR(32) → VARCHAR(64)，与 DB-06 文档对齐
ALTER TABLE japan_customs_record
    MODIFY COLUMN customs_entry_no VARCHAR(64) COMMENT '入境报关号';

-- declared_weight_kg: scale 3→4，与 DB-06 文档对齐
ALTER TABLE japan_customs_record
    MODIFY COLUMN declared_weight_kg DECIMAL(10,4) COMMENT '申报重量（kg）';

-- ===== 2. Warehouse Example → warehouse_record 表隔离 =====
-- 原 WarehouseExample 错误映射到 V1 的 example 表，
-- 与 FinanceExample/LogisticsExample/NotificationExample/ProductExample 冲突
-- 创建独立表（注意：warehouse_example 实为 "仓库记录"，建议后续重命名为 WarehouseRecord）
CREATE TABLE IF NOT EXISTS warehouse_example (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by               VARCHAR(64)  NOT NULL DEFAULT '',
    update_by               VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted              TINYINT(1)   NOT NULL DEFAULT 0,
    name                    VARCHAR(128) NOT NULL COMMENT '仓库名称',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    location               VARCHAR(255) COMMENT '仓库地址',
    capacity                INT COMMENT '容量（立方米）',
    UNIQUE KEY uk_warehouse_example_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库记录（WarehouseExample 隔离表）';

-- ===== 3. Notification Example → notification_record 表隔离 =====
CREATE TABLE IF NOT EXISTS notification_example (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by               VARCHAR(64)  NOT NULL DEFAULT '',
    update_by               VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted              TINYINT(1)   NOT NULL DEFAULT 0,
    title                   VARCHAR(128) NOT NULL COMMENT '通知标题',
    content                 TEXT COMMENT '通知内容',
    recipient               VARCHAR(128) COMMENT '接收人',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'UNREAD' COMMENT 'UNREAD/READ',
    INDEX idx_notification_recipient (recipient),
    INDEX idx_notification_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知记录（NotificationExample 隔离表）';

-- ===== 验证 =====
SELECT COLUMN_NAME, COLUMN_TYPE, NUMERIC_PRECISION, NUMERIC_SCALE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'tax_refund_record'
  AND COLUMN_NAME IN ('price_rmb', 'tax_point', 'estimated_refund_rmb', 'actual_refund_rmb');

SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   IN ('warehouse_example', 'notification_example');
