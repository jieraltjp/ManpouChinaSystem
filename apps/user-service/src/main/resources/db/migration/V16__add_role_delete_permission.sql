-- V16: 补充 role:delete 权限
-- =============================================================
-- RoleController.java 的 @PreAuthorize("hasAuthority('role:delete')") 使用了
-- role:delete 权限，但 V8 permission 表中缺失（只有 read/create/update/assign）。
-- 补充缺失的 role:delete 权限，并分配给 ADMIN 角色。
-- INTJ 审计修复 2026-05-11
-- =============================================================

INSERT INTO permission (permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order) VALUES
('role:delete', '删除角色', '役割を削除', 'role', 'DELETE', 91);

-- ADMIN (role_id=1) 拥有新权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission
WHERE permission_code = 'role:delete' AND is_deleted = 0
ON DUPLICATE KEY UPDATE id=id;
