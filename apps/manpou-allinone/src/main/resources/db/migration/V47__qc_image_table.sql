-- V47: 验货图片表（COS 存储，SPEC-C12）
CREATE TABLE IF NOT EXISTS qc_image (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    qc_record_id  BIGINT NULL COMMENT '关联验货记录ID（nullable，支持先上传后关联）',
    filename       VARCHAR(255) NOT NULL COMMENT 'COS 对象名（不含前缀路径）',
    original_name  VARCHAR(255) NOT NULL COMMENT '原始文件名',
    url            VARCHAR(512) NOT NULL COMMENT '完整访问 URL',
    size           BIGINT NOT NULL COMMENT '文件大小（字节）',
    mime_type      VARCHAR(64) NOT NULL COMMENT 'MIME 类型',
    uploaded_by    BIGINT NULL COMMENT '上传用户ID',
    create_time    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '上传时间',
    is_deleted     TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删标记',
    deleted_at     DATETIME(3) NULL COMMENT '删除时间',
    deleted_by     BIGINT NULL COMMENT '删除操作人',
    INDEX idx_qc_record_id (qc_record_id),
    INDEX idx_filename (filename),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验货图片表（腾讯云COS存储）';
