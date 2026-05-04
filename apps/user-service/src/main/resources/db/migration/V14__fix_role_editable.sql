-- V14: 修正角色 isEditable 属性
-- =============================================================
-- V8 初始 is_editable=1，V10 却用 UPDATE 将 ADMIN/MANAGER/OPERATOR/VIEWER 改回了 0
-- 修复：确保所有预置角色的 is_editable=1（开发环境可编辑权限）

UPDATE role SET is_editable = 1
WHERE role_code IN ('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')
  AND is_deleted = 0;