-- ============================================================
-- V12__japan_customs_record_table.sql
-- 日本清关记录表（步骤6）
-- 对应: docs/business/SPEC-B06-日本清关-步骤6.md §2.1
-- ============================================================

CREATE TABLE IF NOT EXISTS japan_customs_record (
    id                       BIGINT PRIMARY KEY AUTO_INCREMENT,
    customs_entry_no         VARCHAR(32)  DEFAULT NULL COMMENT '入境报关号（JC-YYYYMMDD-NNN）',
    procurement_id           BIGINT       DEFAULT NULL COMMENT '关联采购单ID',
    domestic_customs_id      BIGINT       DEFAULT NULL COMMENT '关联国内报关单ID',
    logistics_plan_id       BIGINT       DEFAULT NULL COMMENT '关联调配计划ID',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/IN_PROGRESS/CLEARED/FAILED',
    arrival_date            DATE         DEFAULT NULL COMMENT '到达日期',
    customs_broker          VARCHAR(128) DEFAULT NULL COMMENT '清关行',
    broker_phone            VARCHAR(32)  DEFAULT NULL COMMENT '清关行电话',
    broker_contact          VARCHAR(64)  DEFAULT NULL COMMENT '清关行联系人',
    import_duty_paid        DECIMAL(14,2) DEFAULT NULL COMMENT '进口关税（JPY）',
    consumption_tax_paid    DECIMAL(14,2) DEFAULT NULL COMMENT '消费税（JPY）',
    clearance_date          DATE         DEFAULT NULL COMMENT '清关完成日期',
    arrival_port            VARCHAR(64)  DEFAULT NULL COMMENT '目的港',
    declared_weight_kg      DECIMAL(10,3) DEFAULT NULL COMMENT '申报重量（kg）',
    declared_volume_cbm     DECIMAL(10,4) DEFAULT NULL COMMENT '申报体积（m³）',
    remarks                 VARCHAR(512) DEFAULT NULL COMMENT '备注',
    -- 审计字段（BaseEntity）
    create_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by               VARCHAR(64)  NOT NULL DEFAULT '',
    update_by               VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted              TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',

    INDEX idx_jp_procurement_id (procurement_id),
    INDEX idx_jp_domestic_customs_id (domestic_customs_id),
    INDEX idx_jp_logistics_plan_id (logistics_plan_id),
    INDEX idx_jp_status (status),
    INDEX idx_jp_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日本清关记录';
