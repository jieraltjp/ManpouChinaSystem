-- V55: 线下订单管理权限
-- 模块: offline_order，4条CRUD权限

INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES
    (130, 'offline_order:read',   '查看线下订单', 'オフライン注文を表示', 'offline_order', 'READ',   130, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (131, 'offline_order:create', '创建线下订单', 'オフライン注文を作成', 'offline_order', 'CREATE', 131, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (132, 'offline_order:update', '编辑线下订单', 'オフライン注文を編集', 'offline_order', 'UPDATE', 132, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (133, 'offline_order:delete', '删除线下订单', 'オフライン注文を削除', 'offline_order', 'DELETE', 133, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- ADMIN: 全部权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'offline_order:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- MANAGER: 全部权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'MANAGER' AND p.permission_code LIKE 'offline_order:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- OPERATOR: CRUD
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'OPERATOR' AND p.permission_code LIKE 'offline_order:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- VIEWER: 只读
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'VIEWER' AND p.permission_code = 'offline_order:read'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);