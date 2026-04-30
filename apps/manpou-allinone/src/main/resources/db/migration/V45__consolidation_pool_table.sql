-- ============================================================
-- V45__consolidation_pool_table.sql
-- 拼柜池表（v1.5.0，SPEC-B00 Issue #8）
--
-- 业务说明：
--   同一目的港的多个 LogisticsPlan 可加入同一拼柜池，
--   当 totalCbm >= containerThreshold 时，触发装柜事件。
--
-- 状态机：OPEN（开放入库）→ PENDING（待装柜）→ LOADED（已装柜）→ SHIPPED（已出港）
--   OPEN/PENDING 可新增/移除 LogisticsPlan
--   LOADED 之后禁止修改 plans（终态）
-- ============================================================

CREATE TABLE consolidation_pool (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_code       VARCHAR(32) NOT NULL COMMENT '池编号，如 CP-20260430-001',
    destination_port VARCHAR(64) NOT NULL COMMENT '目的港（如 Tokyo, Yokohama）',

    -- 体积与重量统计
    total_cbm       DECIMAL(12, 4) DEFAULT 0 COMMENT '当前总体积(m³)',
    total_weight_kg DECIMAL(12, 4) DEFAULT 0 COMMENT '当前总重量(kg)',
    plan_count      INT DEFAULT 0 COMMENT '关联计划数',

    -- 触发阈值
    container_threshold_cbm DECIMAL(10, 4) DEFAULT 70 COMMENT '触发装柜的体积阈值(m³)，默认70m³',

    -- 状态
    status          VARCHAR(24) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN|PENDING|LOADED|SHIPPED',

    -- 审计字段
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    update_by       VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_pool_code UNIQUE (pool_code),
    INDEX idx_pool_status (status),
    INDEX idx_pool_destination (destination_port)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼柜池';
