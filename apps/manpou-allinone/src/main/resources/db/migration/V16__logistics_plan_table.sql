-- ============================================================
-- Flyway 迁移脚本：物流调配计划表
-- INTJ 审计 2026-04-22 补充迁移
-- ============================================================
-- 对应: docs/business/SPEC-B03-调配计划流程.md §2.1
-- 实体: LogisticsPlan.java
-- ============================================================
-- 索引命名: idx_{表名}_{列名} | 唯一: uk_{表名}_{列名}
-- 审计字段: create_time/update_time/create_by/update_by/is_deleted
-- ============================================================

CREATE TABLE IF NOT EXISTS logistics_plan (
    -- 主键
    id                          BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 审计字段
    create_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    update_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted                  TINYINT(1)   NOT NULL DEFAULT 0,

    -- 核心字段
    plan_code                  VARCHAR(32)  NOT NULL COMMENT '调配计划号（LP-YYYYMMDD-NNN）',
    procurement_id             BIGINT COMMENT '关联发注单ID',
    factory_id                 BIGINT COMMENT '关联工厂ID',

    -- 商品信息
    product_code               VARCHAR(32)  NOT NULL COMMENT '货号',
    sub_product_code           VARCHAR(64)  COMMENT '子货号/色号',

    -- 调配类型与状态
    plan_type                  VARCHAR(20)  NOT NULL COMMENT '调配类型',
    status                     VARCHAR(24)  NOT NULL COMMENT '状态',

    -- 货物尺寸/重量/体积
    cargo_length_cm            DECIMAL(8,2) COMMENT '货物长(cm)',
    cargo_width_cm             DECIMAL(8,2) COMMENT '货物宽(cm)',
    cargo_height_cm            DECIMAL(8,2) COMMENT '货物高(cm)',
    cargo_weight_kg            DECIMAL(10,4) COMMENT '货物重量(kg)',
    cargo_volume_cbm           DECIMAL(10,6) COMMENT '货物体积(m³)',

    -- 数量
    quantity                  INT COMMENT '调配数量',

    -- 质检要求
    requires_qc               TINYINT(1) COMMENT '是否需要验货',

    -- 集装箱/集拼
    container_id              BIGINT COMMENT '集装箱ID',
    pool_id                   BIGINT COMMENT '集拼池ID',

    -- 日期
    estimated_ship_date        DATE COMMENT '预计发货日期',
    actual_ship_date          DATE COMMENT '实际发货日期',

    -- 备注
    remarks                   VARCHAR(512) COMMENT '备注',

    -- 索引
    UNIQUE KEY uk_plan_code (plan_code),
    INDEX idx_lp_procurement_id (procurement_id),
    INDEX idx_lp_factory_id (factory_id),
    INDEX idx_lp_product_code (product_code),
    INDEX idx_lp_status (status),
    INDEX idx_lp_estimated_ship_date (estimated_ship_date),
    INDEX idx_lp_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流调配计划（LogisticsPlan 聚合根）';
