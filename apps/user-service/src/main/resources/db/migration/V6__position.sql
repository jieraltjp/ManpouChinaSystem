-- V6: 职务表
CREATE TABLE position (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    position_code    VARCHAR(32)  NOT NULL UNIQUE,
    position_name_cn VARCHAR(64)  NOT NULL,
    position_name_jp VARCHAR(64),
    level_           INT          DEFAULT 0 COMMENT '职级（数字越大越高）',
    company_id       BIGINT COMMENT 'NULL=全局职务，非NULL=该公司私有',
    status           TINYINT      DEFAULT 1,
    create_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time      DATETIME(3)  NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted       TINYINT      NOT NULL DEFAULT 0,
    KEY idx_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置职务（全局）
INSERT INTO position (position_code, position_name_cn, position_name_jp, level_, company_id, create_time, update_time) VALUES
('BOSS',      '总经理',     '社長',        100, NULL, NOW(3), NOW(3)),
('VP',        '副总经理',   '副社長',       90,  NULL, NOW(3), NOW(3)),
('DIRECTOR',  '总监',      'ディレクター',  80,  NULL, NOW(3), NOW(3)),
('MGR',       '经理',      'マネージャー',  60,  NULL, NOW(3), NOW(3)),
('ASST_MGR',  '主管',      'アシスタントマネージャー', 50, NULL, NOW(3), NOW(3)),
('SR_STAFF',  '高级专员',  'シニアスタッフ', 40, NULL, NOW(3), NOW(3)),
('STAFF',     '专员',      'スタッフ',    20,  NULL, NOW(3), NOW(3)),
('INTERN',    '实习生',    'インターン',   10,  NULL, NOW(3), NOW(3)),
('CUSTOMS_REP','报关员',    '通関士',       40,  NULL, NOW(3), NOW(3));
