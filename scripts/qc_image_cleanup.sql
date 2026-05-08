-- ============================================================
-- qc_image_url_cleanup.sql
-- 直接在 MySQL 客户端执行
-- 连接: mysql -h 192.168.13.202 -P 23306 -u manpou -pManpou2024 manpou
-- ============================================================

-- 1. 查看脏数据
SELECT id, filename, LEFT(url, 120) AS url_preview FROM qc_image ORDER BY id;

-- 2. 修复 url: 没有 ?response-content-disposition=inline 的追加上
UPDATE qc_image
SET url = CONCAT(url, '?response-content-disposition=inline')
WHERE url NOT LIKE '%?response-content-disposition=inline%';

-- 3. 修复 filename: 去掉 qc-images/yyyymmdd/ 前缀，只保留 xxx.jpg
UPDATE qc_image
SET filename = SUBSTRING_INDEX(filename, '/', -1)
WHERE filename LIKE '%qc-images/%'
   OR filename LIKE '%/%';

-- 4. 去掉 filename 中的 query string
UPDATE qc_image
SET filename = SUBSTRING_INDEX(filename, '?', 1)
WHERE filename LIKE '%?%';

-- 5. 验证修复结果
SELECT id, filename, LEFT(url, 120) AS url_preview FROM qc_image ORDER BY id;
