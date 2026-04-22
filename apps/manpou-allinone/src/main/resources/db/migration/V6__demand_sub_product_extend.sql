-- ============================================================
-- V6__demand_sub_product_extend.sql
-- FEATURE: 补货需求子货号扩展 + 主货号自动补全
-- 对应: docs/design/FEATURE-货号自动补全与多子货号选择.md
-- ============================================================

-- 1. 扩展 sub_product_code 字段以支持 JSON 数组存储
-- 旧数据直接保留（单个子货号字符串），前端 JSON 解析失败时降级为单个字符串
ALTER TABLE replenishment_demand
    MODIFY COLUMN sub_product_code VARCHAR(512) DEFAULT NULL
    COMMENT '子货号：单个时存字符串（如 "re"），多个时存 JSON 数组（如 ["re","wh","bk"]）';

-- 2. 为 product_code 列添加模糊搜索索引（支持 autocomplete 查询）
-- MySQL 索引不支持 LIKE '%xxx%' 前缀匹配，但索引用于 = / LIKE 'xxx%' 查询
-- 自动补全接口使用 DISTINCT master_code 查询，索引可加速此查询
ALTER TABLE replenishment_demand
    ADD INDEX idx_demand_product_code (product_code);
