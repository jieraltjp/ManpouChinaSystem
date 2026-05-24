-- V58__cargo_size_create_permission.sql
-- 新增 cargo_size:create 权限（新增货物尺寸）

-- 插入权限记录（id=125，跳过现有 id）
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module_code, permission_type, description)
VALUES (125, 'cargo_size:create', '新增货物尺寸', '貨物サイズ新規作成', 'cargo_size', 'CREATE', '新增货物尺寸记录')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- ADMIN 角色拥有 cargo_size:create
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code = 'cargo_size:create'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- MANAGER 角色拥有 cargo_size:create
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.role_code = 'MANAGER' AND p.permission_code = 'cargo_size:create'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- OPERATOR 角色拥有 cargo_size:create
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.role_code = 'OPERATOR' AND p.permission_code = 'cargo_size:create'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- VIEWER 角色不拥有 cargo_size:create（只读）
