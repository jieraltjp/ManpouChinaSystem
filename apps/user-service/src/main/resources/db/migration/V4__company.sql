-- V4: 公司主表
CREATE TABLE company (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_code     VARCHAR(32)  NOT NULL,
    company_name_cn  VARCHAR(128) NOT NULL,
    company_name_jp  VARCHAR(128),
    company_type     VARCHAR(32),
    tax_id           VARCHAR(64),
    address          VARCHAR(256),
    contact_person   VARCHAR(64),
    contact_phone    VARCHAR(32),
    status           TINYINT      DEFAULT 1,
    create_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time      DATETIME(3)  NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted       TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_code (company_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置公司
INSERT INTO company (company_code, company_name_cn, company_name_jp, company_type, create_time, update_time)
VALUES ('HAIT-001', '漫普贸易（中国）有限公司', 'マンプ貿易（中国）有限公司', 'TRADER', NOW(3), NOW(3));
