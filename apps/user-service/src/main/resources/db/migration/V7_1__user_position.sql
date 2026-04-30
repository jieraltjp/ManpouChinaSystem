-- V7.1: 用户-职务关联表（M-N 规范化）
CREATE TABLE user_position (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    position_id  BIGINT       NOT NULL,
    create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user_position (user_id, position_id),
    KEY idx_position (position_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
