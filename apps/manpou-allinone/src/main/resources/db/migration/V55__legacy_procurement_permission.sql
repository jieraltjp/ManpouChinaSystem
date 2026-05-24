-- ============================================================
-- V55: 旧发注管理权限 + 路由标题映射
-- 日期：2026-05-24
-- 内容：legacy_procurement:read 权限 + ADMIN/MANAGER 角色授权
-- ============================================================

-- 1. 插入 legacy_procurement:read 权限
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, description)
VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM permission p),
    'legacy_procurement:read',
    '旧发注管理-只读',
    '旧発注管理-参照専用',
    'legacy_procurement',
    'READ',
    '旧发注管理页面只读权限'
)
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- 2. 为 ADMIN/MANAGER/OPERATOR/VIEWER 角色授权
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.role_code IN ('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')
  AND p.permission_code = 'legacy_procurement:read'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
