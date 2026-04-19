-- ============================================================
-- Flyway 迁移脚本规范
-- 文件命名：V{版本号}__{简短描述}.sql
-- 版本号使用 4 位数字，如 V0001、V0002、V20250315_001
-- 重要：禁止修改已合并的 V1 版本，所有变更走新版本脚本
-- ============================================================

-- 示例：创建示例表（替换为实际业务表）
CREATE TABLE IF NOT EXISTS example (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by    VARCHAR(64)  NOT NULL DEFAULT '',
    update_by    VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted   TINYINT(1)    NOT NULL DEFAULT 0,

    -- 业务字段（根据实际业务修改）
    name         VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '名称',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=正常',

    INDEX idx_create_time (create_time),
    INDEX idx_update_time (update_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='示例表';

-- 重要 DDL 规范：
-- 1. 禁止删除列（使用 is_deleted 逻辑删除）
-- 2. 禁止修改列类型（新增列或建新表）
-- 3. 新增列必须设置 DEFAULT 值（保证历史数据兼容）
-- 4. 索引命名规范：idx_{表名}_{列名}，唯一索引：uk_{表名}_{列名}
