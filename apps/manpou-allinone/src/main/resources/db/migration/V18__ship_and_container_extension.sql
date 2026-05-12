-- V18: 货柜与船只管理扩展
-- 对应: SPEC-B12 / DB-14
-- 作者: jieraltjp
-- 日期: 2026-05-12
-- 注意: V17 已被 japan_customs_update_permission 占用, 本迁移从 V18 开始
--
-- 内容:
--   Step 1: 创建 ship 表（船名/船号/出发港/目的港 + 审计字段）
--   Step 2: container 表扩展字段（ship_id FK / time_slot / arrival_location / remarks）
--   Step 3: container → ship 外键约束
--   Step 4: ship 权限 seed（ID 115~118）
--   Step 5: ship 角色-权限关联 seed

-- Step 1: 创建 ship 表
CREATE TABLE ship (
    id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
    ship_name       VARCHAR(64)    NOT NULL COMMENT '船名',
    ship_number     VARCHAR(32)    NOT NULL COMMENT '船号/航次号',
    carrier         VARCHAR(64)    COMMENT '船公司',
    departure_port  VARCHAR(64)    COMMENT '出发港',
    arrival_port    VARCHAR(64)    COMMENT '目的港',
    create_by       VARCHAR(64)    NOT NULL DEFAULT 'SYSTEM' COMMENT '创建人',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       VARCHAR(64)    NOT NULL DEFAULT 'SYSTEM' COMMENT '更新人',
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '软删除（0=正常，1=已删除）',
    UNIQUE KEY uk_ship_number (ship_number),
    INDEX idx_ship_arrival_port (arrival_port),
    INDEX idx_ship_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='船只信息表';

-- Step 2: container 表扩展字段（ship_id FK / time_slot / arrival_location / remarks）
ALTER TABLE container
    ADD COLUMN ship_id           BIGINT        COMMENT '关联船只ID FK → ship.id';

ALTER TABLE container
    ADD COLUMN time_slot         VARCHAR(32)   COMMENT '时间段，如 2026-W24',
    ADD COLUMN arrival_location  VARCHAR(128)  COMMENT '到岗地点/最终送达地址',
    ADD COLUMN remarks           VARCHAR(512)   COMMENT '备注';

ALTER TABLE container
    ADD INDEX idx_container_ship_id (ship_id);

-- Step 3: container → ship 外键（ship 表已存在，可添加 FK）
ALTER TABLE container
    ADD CONSTRAINT fk_container_ship FOREIGN KEY (ship_id) REFERENCES ship(id);

-- Step 4: ship 权限 seed（4条，ID 115~118）
-- 注意: container:read/create/update/delete（ID 29-32）已存在于 V15 baseline，不重复新增
INSERT IGNORE INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES
    (115, 'ship:read',    '查看船只', '船舶を表示', 'ship', 'READ',    115, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (116, 'ship:create',  '创建船只', '船舶を作成', 'ship', 'CREATE',  116, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (117, 'ship:update',  '编辑船只', '船舶を編集', 'ship', 'UPDATE',  117, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (118, 'ship:delete',  '删除船只', '船舶を削除', 'ship', 'DELETE',  118, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM');

-- Step 5: 角色-权限关联（ship CRUD 分配给 ADMIN/MANAGER/OPERATOR/VIEWER）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM (SELECT id FROM role WHERE code IN ('ADMIN','MANAGER','OPERATOR','VIEWER')) r
CROSS JOIN (SELECT id FROM permission WHERE code LIKE 'ship:%') p;
