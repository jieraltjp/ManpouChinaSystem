-- ============================================================
-- V14__sales_record_table.sql
-- 销售记录表（步骤8 运营销售）
-- 对应: docs/business/SPEC-B08-运营销售-步骤8.md §2.1
-- ============================================================

CREATE TABLE IF NOT EXISTS sales_record (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_code            VARCHAR(32)  DEFAULT NULL COMMENT '销售记录编号（SA-YYYYMMDD-NNN）',
    procurement_id         BIGINT       DEFAULT NULL COMMENT '关联采购单ID',
    product_code           VARCHAR(64)  DEFAULT NULL COMMENT '主货号',
    sub_product_code       VARCHAR(64)  DEFAULT NULL COMMENT '子货号（颜色）',
    sales_channel          VARCHAR(32)  DEFAULT NULL COMMENT '销售渠道',
    status                 VARCHAR(32)  NOT NULL DEFAULT 'LISTED' COMMENT '状态：LISTED/LOW_STOCK/OUT_OF_STOCK/DISCONTINUED',
    listing_date           DATE         DEFAULT NULL COMMENT '上架日期',
    initial_stock         INT          DEFAULT NULL COMMENT '初始上架库存',
    current_stock         INT          DEFAULT NULL COMMENT '当前库存',
    safety_stock          INT          DEFAULT NULL COMMENT '安全库存阈值',
    sales_quantity         INT          DEFAULT NULL COMMENT '累计销量',
    returned_quantity      INT          DEFAULT NULL COMMENT '退货数量',
    return_rate            DECIMAL(6,4) DEFAULT NULL COMMENT '退货率',
    selling_price_jpy     DECIMAL(14,2) DEFAULT NULL COMMENT '实际销售价（JPY）',
    remarks               VARCHAR(512) DEFAULT NULL COMMENT '备注',
    -- 审计字段（BaseEntity）
    create_time            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by              VARCHAR(64)  NOT NULL DEFAULT '',
    update_by              VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',

    INDEX idx_sr_product_code (product_code),
    INDEX idx_sr_procurement_id (procurement_id),
    INDEX idx_sr_status (status),
    INDEX idx_sr_sales_channel (sales_channel),
    INDEX idx_sr_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售记录';
