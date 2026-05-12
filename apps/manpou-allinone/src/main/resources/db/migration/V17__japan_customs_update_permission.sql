-- V17: 修复 japan_customs:update 缺失
-- Bug: ALL_PERMISSIONS Set 和 JapanCustomsController @PreAuthorize 引用了 japan_customs:update，
--      但 V15 permission 表缺少此记录，导致 MANAGER/OPERATOR/VIEWER 调用 PUT /{id}、PATCH /{id}/fail 时 403。
--
-- 修复: INSERT japan_customs:update (ID=51)，INSERT IGNORE 幂等
--
-- japan_customs 模块 ID 46-50 已存在，51 空闲。
-- MANAGER 的 role_permission INSERT（V15）通过 action_='UPDATE' 条件自动包含 ID=51，
-- OPERATOR 通过 module='japan_customs' AND action_='UPDATE' 自动包含 ID=51。
INSERT IGNORE INTO permission (id, permission_code, permission_name_cn, permission_name_jp, module, action_, sort_order, status, create_time, is_deleted, create_by, update_by)
VALUES (51, 'japan_customs:update', '编辑日本清关', '日本通関を編集', 'japan_customs', 'UPDATE', 51, 1, NOW(3), 0, 'SYSTEM', 'SYSTEM');
