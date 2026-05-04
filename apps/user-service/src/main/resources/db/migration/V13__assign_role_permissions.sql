-- V13: 分配 MANAGER/OPERATOR/VIEWER 角色权限
-- =============================
-- MANAGER（role_id=2）：全部业务权限（含审批/完成/启动，不含系统管理写操作）
-- OPERATOR（role_id=3）：业务 CRUD（不含系统管理）
-- VIEWER（role_id=4）：业务只读（不含系统管理）

-- MANAGER：全部业务权限（除系统管理写操作）
INSERT INTO role_permission (role_id, permission_id)
SELECT 2, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.permission_code NOT IN (
        'user:delete', 'user:approve', 'user:reset_password',
        'role:create', 'role:update', 'role:assign',
        'permission:read'
  )
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- OPERATOR：业务模块 CRUD（不含 APPROVE/COMPLETE/START 等管理动作）
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

-- OPERATOR：order:read（只有 READ 动作，已被上方覆盖，此处保留注释说明）

-- VIEWER：业务模块只读
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