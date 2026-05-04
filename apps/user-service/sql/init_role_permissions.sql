-- =============================================================
-- user-service 角色权限初始化脚本（Flyway 禁用时手动执行）
-- =============================================================
-- 此脚本在 user_service 数据库执行，补全以下内容：
--   1. admin 用户 user_code 补全
--   2. admin 职务分配（经理 MGR）
--   3. MANAGER 角色权限（52条：全部业务权限含审批/完成/启动）
--   4. OPERATOR 角色权限（51条：业务 CRUD 不含系统管理写操作）
--   5. VIEWER 角色权限（14条：业务模块只读）

-- =============================================
-- 1. 补全 admin user_code
-- =============================================
UPDATE user SET user_code = 'U-0001', update_time = NOW(3)
WHERE username = 'admin' AND (user_code IS NULL OR user_code = '');

-- =============================================
-- 2. 分配 admin 职务（经理 MGR = position.id=5）
-- =============================================
INSERT INTO user_position (user_id, position_id, create_time)
SELECT u.id, 5, NOW(3) FROM user u WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE position_id = 5;

-- =============================================
-- 3. MANAGER（role_id=2）：全部业务权限（52条）
-- =============================================
INSERT INTO role_permission (role_id, permission_id)
SELECT 2, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.permission_code IN (
-- demand/procurement/shipment/qc: 全部 CRUD
'demand:read','demand:create','demand:update','demand:delete',
'procurement:read','procurement:create','procurement:update','procurement:delete',
'shipment:read','shipment:create','shipment:update','shipment:delete',
'qc:read','qc:create','qc:update','qc:delete',
-- logistics/consolidation/container: 全部 CRUD
'logistics:read','logistics:create','logistics:update','logistics:delete',
'consolidation:read','consolidation:create','consolidation:update','consolidation:delete',
'container:read','container:create','container:update','container:delete',
-- customs: 含审批
'customs:read','customs:create','customs:update','customs:delete','customs:approve',
-- japan_customs: 含启动/完成
'japan_customs:read','japan_customs:create','japan_customs:start',
'japan_customs:complete','japan_customs:delete',
-- tax_refund/sales/factory/product: 含完成
'tax_refund:read','tax_refund:create','tax_refund:update','tax_refund:complete','tax_refund:delete',
'sales:read','sales:create','sales:update','sales:delete',
'factory:read','factory:create','factory:update','factory:delete',
'product:read','product:create','product:update','product:delete',
-- order:read
'order:read'
)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- =============================================
-- 4. OPERATOR（role_id=3）：业务 CRUD（51条）
-- =============================================
INSERT INTO role_permission (role_id, permission_id)
SELECT 3, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.permission_code IN (
-- demand/procurement/shipment/qc: CRUD
'demand:read','demand:create','demand:update','demand:delete',
'procurement:read','procurement:create','procurement:update','procurement:delete',
'shipment:read','shipment:create','shipment:update','shipment:delete',
'qc:read','qc:create','qc:update','qc:delete',
-- logistics/consolidation/container: CRUD
'logistics:read','logistics:create','logistics:update','logistics:delete',
'consolidation:read','consolidation:create','consolidation:update','consolidation:delete',
'container:read','container:create','container:update','container:delete',
-- customs: CRUD（不含审批）
'customs:read','customs:create','customs:update','customs:delete',
-- japan_customs: CRUD（不含启动/完成）
'japan_customs:read','japan_customs:create','japan_customs:delete',
-- tax_refund/sales/factory/product: CRUD（不含完成）
'tax_refund:read','tax_refund:create','tax_refund:update','tax_refund:delete',
'sales:read','sales:create','sales:update','sales:delete',
'factory:read','factory:create','factory:update','factory:delete',
'product:read','product:create','product:update','product:delete',
-- order:read
'order:read'
)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- =============================================
-- 5. VIEWER（role_id=4）：业务只读（14条）
-- =============================================
INSERT INTO role_permission (role_id, permission_id)
SELECT 4, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.permission_code IN (
'demand:read','procurement:read','shipment:read','qc:read',
'logistics:read','consolidation:read','container:read',
'customs:read','japan_customs:read',
'tax_refund:read','sales:read','factory:read','product:read',
'order:read'
)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);