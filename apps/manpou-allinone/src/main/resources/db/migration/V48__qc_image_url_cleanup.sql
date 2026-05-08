-- ============================================================
-- V48__qc_image_url_cleanup.sql
-- 修复 qc_image 表中 URL 和 filename 格式错误
--
-- Bug 根因: CosService.upload() 返回的 URL 带 ?response-content-disposition=inline，
-- 但 QcImageController 存储时直接用，没剥离 query string 到 filename
--
-- 修复目标:
--   url      = 完整COS访问URL（含 ?response-content-disposition=inline）
--   filename = 纯 basename（如 xxx.jpg），无路径、无 query
-- ============================================================

-- 1. 预览脏数据
SELECT id, qc_record_id, filename, url FROM qc_image WHERE
    url NOT LIKE '%?response-content-disposition=inline%'
    OR filename LIKE '%qc-images/%'
    OR filename LIKE '%?%';

-- 2. 修复 url: 没有 ?response-content-disposition=inline 的追加上
UPDATE qc_image
SET url = CONCAT(url, '?response-content-disposition=inline')
WHERE url NOT LIKE '%?response-content-disposition=inline%';

-- 3. 修复 filename: 去掉路径前缀和 query string，只保留 basename
UPDATE qc_image
SET filename = SUBSTRING_INDEX(
    REPLACE(SUBSTRING_INDEX(filename, '?', 1), CONCAT('qc-images/', SUBSTRING_INDEX(SUBSTRING_INDEX(filename, '?', 1), '/', -2), '/'), ''),
    '/', -1
)
WHERE filename LIKE '%qc-images/%';

-- 更简洁的修复：直接用 SUBSTRING_INDEX 从最后一个 / 取 basename
UPDATE qc_image
SET filename = SUBSTRING_INDEX(
    REPLACE(filename, CONCAT('qc-images/', SUBSTRING_INDEX(filename, '/', 2), '/'), ''),
    '?', 1
)
WHERE filename LIKE '%qc-images/%';

-- 去掉 query string
UPDATE qc_image
SET filename = SUBSTRING_INDEX(filename, '?', 1)
WHERE filename LIKE '%?%';

-- 4. 验证
SELECT id, qc_record_id, filename, url FROM qc_image ORDER BY id;
