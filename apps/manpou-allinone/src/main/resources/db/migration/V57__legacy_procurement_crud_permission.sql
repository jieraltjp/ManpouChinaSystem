-- ============================================================
-- V57: 旧发注管理 CRUD 权限
-- 日期：2026-05-24
-- 内容：legacy_procurement:create/update/delete 权限 + ADMIN/MANAGER 角色授权
-- ============================================================

-- 1. 插入 legacy_procurement:create 权限
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, description)
VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM permission p),
    'legacy_procurement:create',
    '旧发注管理-新建',
    '旧発注管理-新規作成',
    'legacy_procurement',
    'CREATE',
    '旧发注管理新建权限'
)
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- 2. 插入 legacy_procurement:update 权限
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, description)
VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM permission p),
    'legacy_procurement:update',
    '旧发注管理-编辑',
    '旧発注管理-編集',
    'legacy_procurement',
    'UPDATE',
    '旧发注管理编辑权限'
)
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- 3. 插入 legacy_procurement:delete 权限
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, description)
VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM permission p),
    'legacy_procurement:delete',
    '旧发注管理-删除',
    '旧発注管理-削除',
    'legacy_procurement',
    'DELETE',
    '旧发注管理删除权限'
)
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- 4. 为 ADMIN/MANAGER 角色授权
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.role_code IN ('ADMIN', 'MANAGER')
  AND p.permission_code IN ('legacy_procurement:create', 'legacy_procurement:update', 'legacy_procurement:delete')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
