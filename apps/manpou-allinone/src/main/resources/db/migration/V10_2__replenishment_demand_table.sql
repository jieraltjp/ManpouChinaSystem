-- ============================================================
-- V10__replenishment_demand_table.sql
-- FEATURE: 补货需求单表（replenishment_demand）
-- 对应: docs/business/SPEC-B01-补货需求-步骤1.md §2 聚合根
-- 此前 V7__demand_sub_product_extend.sql 试图 ALTER 此表但表未创建，故补建。
-- ============================================================

CREATE TABLE IF NOT EXISTS replenishment_demand (
    id                       BIGINT PRIMARY KEY AUTO_INCREMENT,
    demand_code              VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '需求编号（DM-YYYYMMDD-NNN）',
    demand_type              VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '需求类型：REPLENISHMENT=补货，NEW_PURCHASE=新品采购',
    product_code             VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '主货号（关联商品目录）',
    sub_product_code         VARCHAR(512) DEFAULT NULL COMMENT '子货号/色号：单个时存字符串（如 re），多个时存 JSON 数组（如 ["re","wh","bk"]）',
    quantity                 INT          NOT NULL DEFAULT 0 COMMENT '需求量',
    destination              VARCHAR(128) DEFAULT NULL COMMENT '目的地（出货仓库）',
    japan_lead               VARCHAR(64)  DEFAULT NULL COMMENT '日本担当',
    status                   VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待确认，CONVERTED=已转采购，CANCELLED=已取消',
    linked_procurement_id    BIGINT       DEFAULT NULL COMMENT '关联采购单ID（status=CONVERTED 时赋值）',
    remarks                  VARCHAR(512) DEFAULT NULL COMMENT '备注',
    -- 审计字段（BaseEntity）
    create_time              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by                VARCHAR(64)  NOT NULL DEFAULT '',
    update_by                VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted               TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',

    UNIQUE KEY uk_demand_code (demand_code),
    INDEX idx_demand_status (status),
    INDEX idx_demand_type (demand_type),
    INDEX idx_demand_product_code (product_code),
    INDEX idx_demand_linked_procurement (linked_procurement_id),
    INDEX idx_demand_create_time (create_time),
    INDEX idx_demand_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补货需求单';
