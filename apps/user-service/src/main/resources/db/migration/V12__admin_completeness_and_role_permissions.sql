-- V12: 补全 admin 用户数据 + 分配 MANAGER/OPERATOR/VIEWER 角色权限
-- ================================================================
-- 1. admin 用户补全：name_jp / phone / avatar_url / language / timezone
-- 2. admin 职务：分配 MGR（经理）至 user_position
-- 3. MANAGER（role_id=2）：全部业务权限（含审批/完成/启动，不含系统管理写操作）
-- 4. OPERATOR（role_id=3）：业务 CRUD + order:read（不含系统管理）
-- 5. VIEWER（role_id=4）：业务模块只读（不含系统管理）

-- =============================================
-- 1. 补全 admin 用户基础数据
-- =============================================
UPDATE user SET
    name_jp     = 'システム管理者',
    phone       = '+86-138-0000-0001',
    avatar_url  = NULL,
    language    = 'zh',
    timezone    = 'Asia/Shanghai',
    update_time = NOW(3)
WHERE username = 'admin';

-- =============================================
-- 2. admin 职务分配（分配 经理 MGR = position.id=5）
-- =============================================
INSERT INTO user_position (user_id, position_id, create_time)
SELECT u.id, 5, NOW(3) FROM user u WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE position_id = 5;

-- =============================================
-- 3. MANAGER（role_id=2）：全部业务权限
--    含审批/完成/启动，不含 user:delete/user:approve/user:reset_password/
--    role:create/role:update/role:assign/permission:read
-- =============================================
INSERT INTO role_permission (role_id, permission_id)
SELECT 2, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.permission_code NOT IN (
        'user:delete', 'user:approve', 'user:reset_password',
        'role:create', 'role:update', 'role:assign',
        'permission:read'
  )
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- =============================================
-- 4. OPERATOR（role_id=3）：业务 CRUD + order:read
--    不含系统管理（user/role/permission/audit 全部排除）
-- =============================================
INSERT INTO role_permission (role_id, permission_id)
SELECT 3, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.module IN (
        'demand', 'procurement', 'shipment', 'qc',
        'logistics', 'consolidation', 'container',
        'customs', 'japan_customs',
        'tax_refund', 'sales', 'factory', 'product'
      )
  AND p.action_ IN ('READ', 'CREATE', 'UPDATE', 'DELETE')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- OPERATOR 补充 order:read（order 模块仅有 READ 动作，上方条件已覆盖）
-- INSERT INTO role_permission (role_id, permission_id)
-- SELECT 3, p.id FROM permission p
-- WHERE p.is_deleted = 0 AND p.permission_code = 'order:read'
-- ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
-- 注：order 模块只有 READ 动作，已被上方 IN (READ,CREATE,UPDATE,DELETE) 覆盖

-- =============================================
-- 5. VIEWER（role_id=4）：业务模块只读（不含系统管理）
-- =============================================
INSERT INTO role_permission (role_id, permission_id)
SELECT 4, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.module IN (
        'demand', 'procurement', 'shipment', 'qc',
        'logistics', 'consolidation', 'container',
        'customs', 'japan_customs',
        'tax_refund', 'sales', 'factory', 'product', 'order'
      )
  AND p.action_ = 'READ'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
