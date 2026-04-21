-- ============================================================
-- Flyway 迁移脚本：Outbox 消息表
-- 文件命名：V{版本号}__{简短描述}.sql
-- 版本号使用 4 位数字，如 V0001、V0002
-- 详见 docs/pro/02-user-service.md §Outbox 模式
-- ============================================================

-- Outbox 表：用于可靠消息投递（Outbox 模式）
CREATE TABLE IF NOT EXISTS outbox (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '聚合根类型：Order/Payment',
    aggregate_id     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '聚合根 ID',
    event_type       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '事件类型：OrderCreated/PaymentCompleted',
    payload          JSON         NOT NULL COMMENT '事件载荷',
    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=待发送，1=发送中，2=已发送，3=发送失败',
    retry_count      INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    max_retries      INT          NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    error_msg        TEXT COMMENT '错误信息',
    trace_id         VARCHAR(64) COMMENT '链路追踪 ID',
    create_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    INDEX idx_status_create (status, create_time),
    INDEX idx_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息 Outbox 表';

-- Saga 执行日志表：用于分布式事务补偿追踪
CREATE TABLE IF NOT EXISTS saga_log (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    saga_id      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'Saga 实例 ID',
    step_name    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '步骤名称',
    step_order   INT          NOT NULL DEFAULT 0 COMMENT '步骤顺序',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=待执行，1=成功，2=补偿中，3=已补偿，4=补偿失败',
    error_msg    TEXT COMMENT '错误信息',
    trace_id     VARCHAR(64) COMMENT '链路追踪 ID',
    create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    INDEX idx_saga_id (saga_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Saga 执行日志表';

-- DDL 规范：
-- 1. 禁止删除列
-- 2. 禁止修改列类型
-- 3. 新增列必须设置 DEFAULT 值
