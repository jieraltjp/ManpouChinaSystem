-- V15: 补充 warehouse/notification 模块权限
-- =============================================================
-- WarehouseController 和 NotificationController 使用了
-- warehouse:* / notification:* 权限，但 V8 中未定义。
-- 补充缺失的 8 条权限，并将其分配给 ADMIN 角色。
-- INTJ 审计修复 2026-05-08
-- =============================================================

-- 仓储模块
INSERT INTO permission (permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order) VALUES
('warehouse:read',    '查看仓储记录', '倉庫記録を表示',   'warehouse',    'READ',    101),
('warehouse:create', '创建仓储记录', '倉庫記録を作成',   'warehouse',    'CREATE',  102),
('warehouse:update', '编辑仓储记录', '倉庫記録を編集',   'warehouse',    'UPDATE',  103),
('warehouse:delete', '删除仓储记录', '倉庫記録を削除',   'warehouse',    'DELETE',  104);

-- 通知模块
INSERT INTO permission (permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order) VALUES
('notification:read',    '查看通知', '通知を表示',       'notification', 'READ',    111),
('notification:create',  '创建通知', '通知を作成',       'notification', 'CREATE',  112),
('notification:update',  '编辑通知', '通知を編集',       'notification', 'UPDATE',  113),
('notification:delete',  '删除通知', '通知を削除',       'notification', 'DELETE',  114);

-- ADMIN (role_id=1) 拥有新权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission
WHERE permission_code IN ('warehouse:read','warehouse:create','warehouse:update','warehouse:delete',
                          'notification:read','notification:create','notification:update','notification:delete')
  AND is_deleted = 0
ON DUPLICATE KEY UPDATE id=id;
