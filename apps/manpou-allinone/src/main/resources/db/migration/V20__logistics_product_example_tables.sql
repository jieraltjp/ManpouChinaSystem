-- ============================================================
-- Flyway 迁移脚本：LogisticsExample + ProductExample 表隔离
-- INTJ 审计 2026-04-22 补充迁移
-- ============================================================
-- 修复: 5个 Example 实体原全部映射 V1 的 example 表，互相冲突
-- 对应实体:
--   LogisticsExample.java → logistics_example 表
--   ProductExample.java   → product_example 表
-- ============================================================
-- 依赖: V1__init_schema.sql, V14__finance_warehouse_fix.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS logistics_example (
    id                          BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    update_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted                  TINYINT(1)   NOT NULL DEFAULT 0,
    name                        VARCHAR(128) NOT NULL COMMENT '名称',
    status                      VARCHAR(32)  NOT NULL DEFAULT 'PLANNED' COMMENT '状态',
    UNIQUE KEY uk_logistics_example_name (name),
    INDEX idx_logistics_example_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流示例（LogisticsExample 隔离表）';

CREATE TABLE IF NOT EXISTS product_example (
    id                          BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    update_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted                  TINYINT(1)   NOT NULL DEFAULT 0,
    name                        VARCHAR(128) NOT NULL COMMENT '名称',
    status                      VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    UNIQUE KEY uk_product_example_name (name),
    INDEX idx_product_example_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品示例（ProductExample 隔离表）';
