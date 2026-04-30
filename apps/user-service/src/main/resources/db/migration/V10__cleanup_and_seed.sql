-- V10: 清理 V1 example 数据 + 修正 admin 用户角色
-- 问题：V1 创建的 example user (username='admin', role=USER) 与 V7/V8 真实数据冲突
--       登录时查到的角色为 USER 而非 ADMIN
-- 修复：
--   1. 更新现有 admin 用户的 password_hash（确保 BCrypt 正确）
--   2. 确保 ADMIN/MANAGER/OPERATOR/VIEWER 角色存在
--   3. 建立 admin → ADMIN 的角色关联

-- 清理 V1 example role（role_code='USER'）
DELETE FROM role WHERE role_code = 'USER';

-- 清理 V1 example permissions
DELETE FROM permission WHERE permission_code IN ('example:read', 'example:write');

-- 清理 V1 example user（V1 用 email='admin@example.com' 标记，V7 用 admin@manpou.cn）
DELETE FROM user WHERE email = 'admin@example.com';

-- 更新 admin 用户密码（V7 BCrypt hash: admin123）
UPDATE user SET
    password_hash = '$2a$12$t7mRpfsCDNFgj6LET1Y47eH7J2.MJ5i5nAYwYL6SfKdWE7LN.vqUG',
    name_cn = '系统管理员',
    status = 1,
    registration_status = 'APPROVED',
    update_time = NOW(3)
WHERE username = 'admin';

-- 确保 ADMIN 角色存在（V8 的 INSERT 可能因 id=1 已被占而失败）
INSERT INTO role (id, role_code, role_name_cn, role_name_jp, description, status, is_deleted, create_time, update_time)
VALUES (1, 'ADMIN', '系统管理员', 'システム管理者', '拥有全部权限', 1, 0, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
    role_name_cn = VALUES(role_name_cn),
    role_name_jp = VALUES(role_name_jp),
    description = VALUES(description);

-- 确保 MANAGER/OPERATOR/VIEWER 角色存在
INSERT INTO role (id, role_code, role_name_cn, role_name_jp, description, status, is_deleted, create_time, update_time)
VALUES (2, 'MANAGER', '运营主管', '運営マネージャー', '拥有全部权限含审批', 1, 0, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE role_name_cn = VALUES(role_name_cn), role_name_jp = VALUES(role_name_jp);

INSERT INTO role (id, role_code, role_name_cn, role_name_jp, description, status, is_deleted, create_time, update_time)
VALUES (3, 'OPERATOR', '普通运营', '一般運営者', 'CRUD 权限，限本人数据', 1, 0, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE role_name_cn = VALUES(role_name_cn), role_name_jp = VALUES(role_name_jp);

INSERT INTO role (id, role_code, role_name_cn, role_name_jp, description, status, is_deleted, create_time, update_time)
VALUES (4, 'VIEWER', '查看者', '閲覧者', '只读权限', 1, 0, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE role_name_cn = VALUES(role_name_cn), role_name_jp = VALUES(role_name_jp);

-- 建立 admin → ADMIN 的角色关联（user_role 表结构：id, user_id, role_id, create_time）
INSERT INTO user_role (user_id, role_id) VALUES ((SELECT id FROM (SELECT id FROM user WHERE username = 'admin') AS t), 1)
ON DUPLICATE KEY UPDATE role_id = 1;
