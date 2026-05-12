-- V17: 修复 japan_customs:update 缺失
-- Bug: ALL_PERMISSIONS Set 和 JapanCustomsController @PreAuthorize 引用了 japan_customs:update，
--      但 permission 表缺少此记录，导致 MANAGER/OPERATOR/VIEWER 调用 PUT /{id}、PATCH /{id}/fail 时 403。
--
-- 修复: INSERT japan_customs:update (ID=70)，INSERT IGNORE 幂等
--
-- ID=70 为 permission 表当前最大 ID 后的第一个空闲值。
-- Flyway 历史状态: V15 (warehouse_notification_permissions) 失败，DB permission 表使用旧 ID 体系。
-- role_permission 表需同步添加 (role_id, permission_id) = (2,70) 和 (3,70)。
INSERT IGNORE INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES (70, 'japan_customs:update', '编辑日本清关', '日本通関を編集', 'japan_customs', 'UPDATE', 70, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM');
