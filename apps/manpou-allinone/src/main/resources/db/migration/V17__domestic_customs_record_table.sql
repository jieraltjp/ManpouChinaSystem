-- ============================================================
-- Flyway 迁移脚本：国内报关记录表
-- INTJ 审计 2026-04-22 补充迁移
-- ============================================================
-- 对应: docs/business/SPEC-B05-国内报关-步骤5.md §2.1
-- 实体: DomesticCustomsRecord.java
-- ============================================================
-- 索引命名: idx_{表名}_{列名} | 唯一: uk_{表名}_{列名}
-- 审计字段: create_time/update_time/create_by/update_by/is_deleted
-- ============================================================

CREATE TABLE IF NOT EXISTS domestic_customs_record (
    -- 主键
    id                          BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 审计字段
    create_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    update_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted                  TINYINT(1)   NOT NULL DEFAULT 0,

    -- 核心字段
    customs_code               VARCHAR(32)  NOT NULL COMMENT '报关单号（DC-YYYYMMDD-NNN）',
    procurement_id             BIGINT COMMENT '关联发注单ID',
    logistics_plan_id          BIGINT COMMENT '关联调配计划ID',
    factory_id                 BIGINT COMMENT '关联工厂ID',

    -- 商品信息
    product_code               VARCHAR(32)  NOT NULL COMMENT '货号',
    sub_product_code           VARCHAR(64)  COMMENT '子货号/色号',
    quantity                  INT COMMENT '报关数量',

    -- 价值
    estimated_value_cny        DECIMAL(14,2) COMMENT '预估货值(CNY)',

    -- 状态
    status                    VARCHAR(24)  NOT NULL COMMENT '状态：PENDING/SUBMITTED/CLEARED/REJECTED',

    -- 备注
    remarks                   VARCHAR(512) COMMENT '备注',

    -- 索引
    UNIQUE KEY uk_domestic_customs_code (customs_code),
    INDEX idx_dc_procurement_id (procurement_id),
    INDEX idx_dc_logistics_plan_id (logistics_plan_id),
    INDEX idx_dc_factory_id (factory_id),
    INDEX idx_dc_product_code (product_code),
    INDEX idx_dc_status (status),
    INDEX idx_dc_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='国内报关记录（DomesticCustomsRecord 聚合根）';
