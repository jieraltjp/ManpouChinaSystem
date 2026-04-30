-- ============================================================
-- V46__container_table.sql
-- 货柜表（v1.5.0，SPEC-B00 Issue #8）
--
-- 业务说明：
--   当 ConsolidationPool 达到阈值后，创建货柜记录，
--   将关联的 LogisticsPlan.containerId / poolId 绑定至此货柜。
--
-- 状态机：CREATED → LOADED → DEPARTED → ARRIVED
--   CREATED：货柜已创建，待装货
--   LOADED：货物已装载
--   DEPARTED：已离港
--   ARRIVED：已到港（终态）
-- ============================================================

CREATE TABLE container (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    container_no    VARCHAR(32) NOT NULL COMMENT '货柜号（如 TEMU1234567）',
    container_type  VARCHAR(24) NOT NULL DEFAULT '20GP' COMMENT '20GP|40GP|40HC|45HC',

    -- 尺寸与载量
    total_cbm       DECIMAL(10, 4) DEFAULT 0 COMMENT '已装载总体积(m³)',
    total_weight_kg DECIMAL(12, 4) DEFAULT 0 COMMENT '已装载总重量(kg)',
    plan_count      INT DEFAULT 0 COMMENT '关联计划数',

    -- 关联拼柜池
    pool_id         BIGINT COMMENT '关联拼柜池ID',

    -- 状态
    status          VARCHAR(24) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED|LOADED|DEPARTED|ARRIVED',

    -- 关键日期
    load_date       DATE COMMENT '装柜日期',
    departure_date  DATE COMMENT '离港日期',
    arrival_date    DATE COMMENT '到港日期',

    -- 审计字段
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    update_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_container_no UNIQUE (container_no),
    INDEX idx_container_status (status),
    INDEX idx_container_pool (pool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='货柜';
