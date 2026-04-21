-- ============================================================
-- Flyway 迁移脚本：签名密钥表（RS256 密钥轮换）
-- 文件命名：V{版本号}__{简短描述}.sql
-- 详见 docs/pro/02-user-service.md §认证授权
-- ============================================================

CREATE TABLE IF NOT EXISTS signing_key (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    kid              VARCHAR(64)  NOT NULL COMMENT '密钥 ID（写入 JWT kid header）',
    public_key_pem   TEXT         NOT NULL COMMENT '公钥 PEM（不含私钥）',
    private_key_path VARCHAR(255) NOT NULL COMMENT '私钥文件路径（不含私钥内容）',
    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=INACTIVE，1=ACTIVE',
    create_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '密钥创建时间',

    UNIQUE INDEX idx_kid (kid),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RS256 签名密钥表';

-- DDL 规范：
-- 1. 禁止删除列（历史密钥必须保留用于旧 Token 验签）
-- 2. 禁止修改列类型
-- 3. 最多保留 3 个历史密钥（定期清理超龄 INACTIVE 密钥）
--
-- 注：signing_key 为 append-only 基础设施表（密钥不可修改，只可新增/停用），
--     不遵循标准审计字段规范（无 update_time / create_by / update_by / is_deleted）。
