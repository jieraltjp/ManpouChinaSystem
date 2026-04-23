-- ============================================================
-- V13__tax_refund_record_table.sql
-- 出口退税记录表（步骤7）
-- 对应: docs/business/SPEC-B07-出口退税-步骤7.md §2.1
-- ============================================================

CREATE TABLE IF NOT EXISTS tax_refund_record (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    refund_code             VARCHAR(32)  DEFAULT NULL COMMENT '退税单号（TR-YYYYMMDD-NNN）',
    procurement_id         BIGINT       DEFAULT NULL COMMENT '关联采购单ID',
    japan_customs_id        BIGINT       DEFAULT NULL COMMENT '关联日本清关单ID',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'APPLYING' COMMENT '状态：APPLYING/COMPLETED/NO_REFUND',
    billing_type            VARCHAR(32)  DEFAULT NULL COMMENT '开票类型：NORMAL/REBATE/VOID',
    price_rmb              DECIMAL(14,2) DEFAULT NULL COMMENT '含税人民币单价',
    quantity                INT          DEFAULT NULL COMMENT '数量',
    tax_point               DECIMAL(5,4) DEFAULT NULL COMMENT '税点（如1.13）',
    estimated_refund_rmb   DECIMAL(14,2) DEFAULT NULL COMMENT '预估退税金额（RMB）',
    actual_refund_rmb      DECIMAL(14,2) DEFAULT NULL COMMENT '实际退税金额（RMB）',
    exchange_rate          DECIMAL(10,6) DEFAULT NULL COMMENT '汇率',
    refund_date             DATE         DEFAULT NULL COMMENT '实际退税日期',
    refund_bank             VARCHAR(128) DEFAULT NULL COMMENT '退税银行',
    remarks                VARCHAR(512) DEFAULT NULL COMMENT '备注',
    -- 审计字段（BaseEntity）
    create_time            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by              VARCHAR(64)  NOT NULL DEFAULT '',
    update_by              VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',

    INDEX idx_tr_procurement_id (procurement_id),
    INDEX idx_tr_japan_customs_id (japan_customs_id),
    INDEX idx_tr_status (status),
    INDEX idx_tr_refund_date (refund_date),
    INDEX idx_tr_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出口退税记录';
