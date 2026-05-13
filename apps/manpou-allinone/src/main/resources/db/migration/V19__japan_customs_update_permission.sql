-- V19: 修复 japan_customs:update 缺失
-- Bug: ALL_PERMISSIONS Set 和 JapanCustomsController @PreAuthorize 引用了 japan_customs:update，
--      但 permission 表缺少此记录，导致 MANAGER/OPERATOR/VIEWER 调用 PUT /{id}、PATCH /{id}/fail 时 403。
--
-- 修复: INSERT japan_customs:update (ID=119)，INSERT IGNORE 幂等
--
-- ID=119：V15 最大 ID=114，V18 ship CRUD 用 115-118，故跳至 119。
-- 注意：原本 V17 错误写成 ID=70，与 factory:create (ID=70) 冲突；
--       又错误写成 ID=92（与 permission:read ID=92 冲突），已更正为 119。
INSERT IGNORE INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES (119, 'japan_customs:update', '编辑日本清关', '日本通関を編集', 'japan_customs', 'UPDATE', 119, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM');

-- role_permission 同步：分配给 MANAGER (role_id=2) 和 OPERATOR (role_id=3)
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, 119
FROM role r
WHERE r.code IN ('MANAGER', 'OPERATOR');
