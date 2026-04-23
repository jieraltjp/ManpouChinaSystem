-- ============================================================
-- Flyway 迁移脚本：FinanceExample 表隔离
-- INTJ 审计 2026-04-22 补充迁移
-- ============================================================
-- 修复: FinanceExample/LogisticsExample/ProductExample
--       原全部映射到 V1 的 example 表，互相冲突
-- 实体: FinanceExample.java → finance_example 表
-- ============================================================
-- 依赖: V1__init_schema.sql, V14__finance_warehouse_fix.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS finance_example (
    -- 主键
    id                          BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 审计字段
    create_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    update_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted                  TINYINT(1)   NOT NULL DEFAULT 0,

    -- 业务字段
    name                        VARCHAR(128) NOT NULL COMMENT '名称',
    status                      VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',

    -- 索引
    UNIQUE KEY uk_finance_example_name (name),
    INDEX idx_finance_example_create_time (create_time),
    INDEX idx_finance_example_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务示例（FinanceExample 隔离表）';
