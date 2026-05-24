-- V56: page_access权限 + dispatch模块 + cargo_size + legacy_procurement CRUD
-- 时间: 2026-05-24
-- G1: cargo_size:delete（V54漏了，ID 134）
-- G2: dispatch 4条CRUD（IDs 135~138）
-- G3: cargo_size:create（DB缺失，ID 119）
-- G4: legacy_procurement CRUD（DB只有read，IDs 126~128）
-- G5: page:*:access 18个模块（DB已有，幂等插入，IDs 140~157）
-- G6: page:cargo_size/dispatch/legacy_procurement:access（IDs 158~160）
-- G7: page:offline_order:access（ID 161）
-- G8: page:profile:access（ID 162）

-- ================================================================
-- 一、cargo_size:delete（V54漏了ID 134，ID 125 是 legacy_procurement:read）
-- ================================================================
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES (134, 'cargo_size:delete', '删除货物尺寸', '貨物サイズ削除', 'cargo_size', 'DELETE', 134, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code IN ('ADMIN','MANAGER') AND p.permission_code = 'cargo_size:delete'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ================================================================
-- 二、dispatch 模块（4条CRUD，IDs 135~138）
-- ================================================================
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES
    (135, 'dispatch:read',   '查看配送单', '配送單查看',   'dispatch', 'READ',   135, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (136, 'dispatch:create', '创建配送单', '配送單作成',   'dispatch', 'CREATE', 136, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (137, 'dispatch:update', '编辑配送单', '配送單編集',   'dispatch', 'UPDATE', 137, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (138, 'dispatch:delete', '删除配送单', '配送單削除',   'dispatch', 'DELETE', 138, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- ADMIN: 全部权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'dispatch:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- MANAGER: 全部权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'MANAGER' AND p.permission_code LIKE 'dispatch:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- OPERATOR: CRUD（可以操作）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'OPERATOR' AND p.permission_code LIKE 'dispatch:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- VIEWER: 只读
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'VIEWER' AND p.permission_code = 'dispatch:read'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ================================================================
-- 四、cargo_size:create（DB缺失，ALL_PERMISSIONS已有）
-- ================================================================
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES (119, 'cargo_size:create', '创建货物尺寸', '貨物サイズ作成', 'cargo_size', 'CREATE', 119, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code IN ('ADMIN','MANAGER') AND p.permission_code = 'cargo_size:create'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ================================================================
-- 五、legacy_procurement CRUD（DB只有read，缺少create/update/delete）
-- ================================================================
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES
    (126, 'legacy_procurement:create', '创建旧发注', '旧発注管理-作成',     'legacy_procurement', 'CREATE', 126, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (127, 'legacy_procurement:update', '编辑旧发注', '旧発注管理-編集',     'legacy_procurement', 'UPDATE', 127, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (128, 'legacy_procurement:delete', '删除旧发注', '旧発注管理-削除',     'legacy_procurement', 'DELETE', 128, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- ADMIN/MANAGER: 全部 legacy_procurement 权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code IN ('ADMIN','MANAGER') AND p.permission_code LIKE 'legacy_procurement:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- OPERATOR: 增删改
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'OPERATOR' AND p.permission_code LIKE 'legacy_procurement:%' AND p.action_ != 'READ'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
-- ================================================================
-- 六、page:*:access 路由守卫权限（18个模块，DB已有，幂等插入）
-- 注意：这些权限在DB已存在（IDs 140~157），此处仅确保幂等
-- ================================================================
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES
    (140, 'page:demand:access',       '进入补货需求页',   '補充需要ページアクセス',   'page:procurement', 'ACCESS', 140, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (141, 'page:procurement:access',   '进入发注单页',     '発注書ページアクセス',     'page:procurement', 'ACCESS', 141, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (142, 'page:shipment:access',      '进入厂家出货页',   'メーカー出荷ページアクセス','page:procurement', 'ACCESS', 142, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (143, 'page:qc:access',            '进入验货记录页',   '検品記録ページアクセス',   'page:procurement', 'ACCESS', 143, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (144, 'page:logistics:access',      '进入调配计划页',   '配送計画ページアクセス',   'page:procurement', 'ACCESS', 144, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (145, 'page:consolidation:access',  '进入拼柜池页',     'コンソリ頁アクセス',       'page:procurement', 'ACCESS', 145, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (146, 'page:container:access',      '进入货柜管理页',   'コンテナ頁アクセス',       'page:base', 'ACCESS', 146, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (147, 'page:ship:access',           '进入船只管理页',   '船舶管理頁アクセス',       'page:base', 'ACCESS', 147, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (148, 'page:customs:access',        '进入国内报关页',   '国内通関頁アクセス',       'page:procurement', 'ACCESS', 148, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (149, 'page:japan_customs:access', '进入日本清关页',   '日本通関頁アクセス',       'page:procurement', 'ACCESS', 149, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (150, 'page:tax_refund:access',     '进入出口退税页',   '輸出退税頁アクセス',       'page:sales', 'ACCESS', 150, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (151, 'page:sales:access',          '进入运营销售页',   '運営販売頁アクセス',       'page:sales', 'ACCESS', 151, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (152, 'page:factory:access',         '进入工厂管理页',   '工場管理頁アクセス',       'page:base', 'ACCESS', 152, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (153, 'page:product:access',         '进入商品目录页',   '商品ディレクトリ頁アクセス','page:base', 'ACCESS', 153, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (154, 'page:order:access',          '进入订单总览页',   '注文一覧頁アクセス',       'page:base', 'ACCESS', 154, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (155, 'page:user:access',           '进入用户管理页',   'ユーザー管理頁アクセス',   'page:system', 'ACCESS', 155, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (156, 'page:role:access',           '进入角色管理页',   '役割管理頁アクセス',       'page:system', 'ACCESS', 156, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (157, 'page:audit:access',          '进入操作日志页',   '操作ログ頁アクセス',       'page:system', 'ACCESS', 157, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- page:*:access 角色分配
-- ADMIN: 全部（已有 *:* 展开）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'page:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- MANAGER: 全部 page 权限（管理岗可进入所有页面）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'MANAGER' AND p.permission_code LIKE 'page:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- OPERATOR: 发注 + 物流 + 报关 + 工厂 + 商品（运营人员需要进入工作页面）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'OPERATOR'
  AND p.permission_code IN (
      'page:demand:access','page:procurement:access','page:shipment:access',
      'page:qc:access','page:logistics:access','page:consolidation:access',
      'page:container:access','page:ship:access',
      'page:customs:access','page:japan_customs:access',
      'page:factory:access','page:product:access','page:order:access'
  )
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- VIEWER: 全部只读页面（查看权限）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'VIEWER'
-- G6: cargo_size/dispatch/legacy_procurement page 访问权限（基础数据菜单守卫，IDs 158~160）
-- ================================================================
-- 七、page:cargo_size:access / page:dispatch:access / page:legacy_procurement:access（IDs 158~160）
-- ================================================================
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES
    (158, 'page:cargo_size:access',        '访问货物尺寸',       '貨物サイズ頁アクセス',     'page:base', 'ACCESS', 158, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (159, 'page:dispatch:access',          '访问配送单',         '配送單頁アクセス',         'page:base', 'ACCESS', 159, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM'),
    (160, 'page:legacy_procurement:access', '访问旧发注',        '旧発注管理頁アクセス',     'page:base', 'ACCESS', 160, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

-- ADMIN/MANAGER: 全部
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code IN ('ADMIN','MANAGER') AND p.permission_code LIKE 'page:%'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- OPERATOR: 货物尺寸 + 配送单（工作页面）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'OPERATOR' AND p.permission_code IN ('page:cargo_size:access','page:dispatch:access')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- VIEWER: 货物尺寸（可查看）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code = 'VIEWER' AND p.permission_code IN ('page:cargo_size:access','page:dispatch:access')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ================================================================
-- 八、page:offline_order:access（ID 161，/system/role 可控制入口）
-- ================================================================
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES (161, 'page:offline_order:access', '访问线下订单', 'オフライン注文頁アクセス', 'page:base', 'ACCESS', 161, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code IN ('ADMIN','MANAGER','OPERATOR','VIEWER') AND p.permission_code = 'page:offline_order:access'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ================================================================
-- 九、page:profile:access（ID 162，个人中心入口控制）
-- ================================================================
INSERT INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES (162, 'page:profile:access', '访问个人中心', 'プロフィール頁アクセス', 'page:system', 'ACCESS', 162, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE permission_name_cn = VALUES(permission_name_cn);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.role_code IN ('ADMIN','MANAGER','OPERATOR','VIEWER') AND p.permission_code = 'page:profile:access'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
