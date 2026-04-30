-- V11: 补充 BaseEntity 审计列到 permission 和 role 表
-- BaseEntity 要求子类表包含 create_by, update_by 列
-- V8 permission/role 表缺少这些列，导致 JPA schema-validation 失败

-- permission: 添加缺失的审计列
ALTER TABLE permission
    ADD COLUMN create_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM' AFTER is_deleted,
    ADD COLUMN update_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM' AFTER create_by,
    ADD COLUMN update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) AFTER update_by;

-- role: 添加缺失的审计列（create_by, update_by；update_time 已存在）
ALTER TABLE role
    ADD COLUMN create_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM' AFTER is_deleted,
    ADD COLUMN update_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM' AFTER create_by;
