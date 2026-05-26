-- ============================================================
-- V59: dispatch 导出权限
-- 日期：2026-05-25
-- ============================================================
INSERT INTO permission (permission_code, permission_name, module, description, is_deleted, create_time, update_time)
SELECT 'dispatch:export', '货物发送导出', 'dispatch', '货物发送记录 CSV 导出', FALSE, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permission WHERE permission_code = 'dispatch:export'
);
