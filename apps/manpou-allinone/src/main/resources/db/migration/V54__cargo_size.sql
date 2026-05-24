-- V54__cargo_size.sql
-- SPEC-B15 货物尺寸表
-- 创建：2026-05-24

CREATE TABLE cargo_size (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    master_code          VARCHAR(32)  NOT NULL COMMENT '主货号',
    sub_code             VARCHAR(64) COMMENT '子货号',
    code                 VARCHAR(96)  NOT NULL COMMENT '完整货号',
    legacy_id            BIGINT COMMENT '旧系统ID',
    update_time          DATETIME COMMENT '旧系统更新时间',
    input_user           VARCHAR(64) COMMENT '录入人',

    length_cm            DECIMAL(8,2) COMMENT '单品长(cm)',
    width_cm             DECIMAL(8,2) COMMENT '单品宽(cm)',
    height_cm            DECIMAL(8,2) COMMENT '单品高(cm)',
    net_weight_kg        DECIMAL(10,4) COMMENT '净重(kg)',

    pack_height_cm       DECIMAL(8,2) COMMENT '外箱高(cm)',
    pack_width_cm        DECIMAL(8,2) COMMENT '外箱宽(cm)',
    pack_depth_cm        DECIMAL(8,2) COMMENT '外箱深(cm)',
    package_weight_kg    DECIMAL(10,4) COMMENT '外箱毛重(kg)',
    units_per_package    INT COMMENT '每箱数量',

    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    product_id           BIGINT COMMENT '升格后对应的product.id',
    promoted_by          VARCHAR(64) COMMENT '升格操作人',
    promoted_at          DATETIME COMMENT '升格时间',
    remarks              VARCHAR(512) COMMENT '备注',
    show_flag            VARCHAR(10)  NOT NULL DEFAULT '0',

    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by            VARCHAR(64),

    UNIQUE KEY uk_code (code),
    INDEX idx_master_code (master_code),
    INDEX idx_status (status),
    INDEX idx_show_flag (show_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物尺寸表（item_size 未匹配记录）';

-- 权限数据（SPEC-B15 §4）
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action, description)
VALUES
    (120, 'cargo_size:read', '查看货物尺寸', '貨物サイズ查看', 'cargo_size', 'READ', '查看货物尺寸管理页面'),
    (121, 'cargo_size:import', '导入货物尺寸', '貨物サイズインポート', 'cargo_size', 'CREATE', '触发从 item_size 导入货物尺寸'),
    (122, 'cargo_size:promote', '升格货物尺寸', '货物サイズ昇格', 'cargo_size', 'CREATE', '将货物尺寸升格为正式商品'),
    (123, 'cargo_size:discard', '废弃货物尺寸', '货物サイズ廃棄', 'cargo_size', 'DELETE', '软废弃货物尺寸'),
    (124, 'cargo_size:update', '编辑货物尺寸', '货物サイズ編集', 'cargo_size', 'UPDATE', '编辑货物尺寸备注等')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- ADMIN 角色拥有全部 cargo_size 权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'cargo_size:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- MANAGER 角色拥有全部 cargo_size 权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'MANAGER' AND p.permission_code LIKE 'cargo_size:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- OPERATOR 角色只拥有 cargo_size:read
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'OPERATOR' AND p.permission_code = 'cargo_size:read'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- VIEWER 角色只拥有 cargo_size:read
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'VIEWER' AND p.permission_code = 'cargo_size:read'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
