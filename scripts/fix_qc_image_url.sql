-- ============================================================
-- Fix: 清除 qc_image 表中含 ?response-content-disposition=inline 的脏 URL
-- 修复 commit 08c1867 引入的 bug：CosService.upload() 返回的 URL
-- 带 query param，但存储时未剥离，导致前端 <img src="脏URL"> → 404
-- ============================================================

-- 1. 预览当前脏数据
SELECT id, qc_record_id, filename, url FROM qc_image WHERE url LIKE '%?response-content-disposition=inline%';

-- 2. 清理 url 字段：去掉 ?response-content-disposition=inline
UPDATE qc_image
SET url = REPLACE(url, '?response-content-disposition=inline', '')
WHERE url LIKE '%?response-content-disposition=inline%';

-- 3. 清理 filename 字段：去掉含路径的前缀（qc-images/yyyymmdd/）
--    只保留 basename（如 xxx.jpg），用 SUBSTRING_INDEX
UPDATE qc_image
SET filename = SUBSTRING_INDEX(SUBSTRING_INDEX(filename, '/', -1), '?', 1)
WHERE filename LIKE '%qc-images/%';

-- 4. 验证
SELECT id, qc_record_id, filename, url FROM qc_image ORDER BY id;
