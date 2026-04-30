-- V7: 用户主表
CREATE TABLE user (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code             VARCHAR(32)  NOT NULL UNIQUE COMMENT '用户编码 U-XXXX',
    username              VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录账号',
    password_hash         VARCHAR(255) NOT NULL COMMENT 'BCrypt哈希',
    name_cn               VARCHAR(64)  COMMENT '中文姓名',
    name_jp               VARCHAR(64)  COMMENT '日文姓名',
    email                 VARCHAR(128) NOT NULL UNIQUE,
    phone                 VARCHAR(32)  COMMENT '手机号',
    avatar_url            VARCHAR(512) COMMENT '头像URL',
    company_id            BIGINT COMMENT '所属公司 FK → company.id',
    department_id         BIGINT COMMENT '所属部门 FK → department.id',
    customs_code          VARCHAR(64)  COMMENT '报关员备案号',
    customs_license       VARCHAR(128) COMMENT '报关员证号',
    language              VARCHAR(8)   DEFAULT 'zh',
    timezone              VARCHAR(16)  DEFAULT 'CST',
    status                TINYINT      DEFAULT 1 COMMENT '1=正常 0=禁用',
    last_login_time       DATETIME(3) COMMENT '最后登录时间',
    last_login_ip         VARCHAR(45)  COMMENT '最后登录IP',
    registration_status   VARCHAR(16)  DEFAULT 'APPROVED' COMMENT 'APPROVED/PENDING/REJECTED',
    reject_reason         VARCHAR(256) COMMENT '拒绝原因（REJECTED时填写）',
    create_time           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time           DATETIME(3)  NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
    create_by             VARCHAR(64)  NOT NULL DEFAULT 'SYSTEM',
    update_by             VARCHAR(64)  NOT NULL DEFAULT 'SYSTEM',
    is_deleted            TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_code (user_code),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_company (company_id),
    KEY idx_dept (department_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置管理员（admin/admin123，BCrypt strength=12）
INSERT INTO user (user_code, username, password_hash, name_cn, email, company_id, department_id, status, registration_status, create_by, update_time)
VALUES ('U-0001', 'admin', '$2a$12$7h/vq53JivkyCYK7SjBU1Oai0DSxhnIf3iY12BAE64XK4EO3rjsCa', '系统管理员', 'admin@manpou.cn', 1, 1, 1, 'APPROVED', 'SYSTEM', NOW(3));
