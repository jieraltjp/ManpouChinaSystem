-- V5: 部门表（支持树形）
CREATE TABLE department (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_id      BIGINT       NOT NULL,
    dept_code       VARCHAR(32)  NOT NULL,
    dept_name_cn    VARCHAR(64)  NOT NULL,
    dept_name_jp    VARCHAR(64),
    parent_id       BIGINT,
    sort_order      INT          DEFAULT 0,
    status          TINYINT      DEFAULT 1,
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_company_dept (company_id, dept_code),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置部门
INSERT INTO department (company_id, dept_code, dept_name_cn, dept_name_jp, parent_id, create_time, update_time)
VALUES (1, 'IT', '信息中心', '情報センター', NULL, NOW(3), NOW(3));

INSERT INTO department (company_id, dept_code, dept_name_cn, dept_name_jp, parent_id, create_time, update_time)
VALUES (1, 'PUR', '采购部', '調達部', NULL, NOW(3), NOW(3));

INSERT INTO department (company_id, dept_code, dept_name_cn, dept_name_jp, parent_id, create_time, update_time)
VALUES (1, 'SALES', '销售部', '営業部', NULL, NOW(3), NOW(3));
