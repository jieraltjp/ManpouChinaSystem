-- V35__logistics_plan_container_no.sql
-- 功能: logistics_plan 新增 container_no 货柜号字段
-- 场景: 多条 LogisticsPlan 可填入相同货柜号，用于追踪同一货柜内的所有货物
-- 作者: claude
-- 日期: 2026-04-27

-- ============================================================
-- 1. 新增 container_no 字段
-- ============================================================
ALTER TABLE logistics_plan
    ADD COLUMN container_no VARCHAR(32) DEFAULT NULL COMMENT '货柜号（船公司提供，同批次货物填入相同货柜号）' AFTER plan_code;

-- ============================================================
-- 2. 添加索引（支持按货柜号模糊查询）
-- ============================================================
CREATE INDEX idx_lp_container_no ON logistics_plan(container_no);

-- ============================================================
-- 3. 回滚（若需要）
-- ============================================================
-- DROP INDEX idx_lp_container_no ON logistics_plan;
-- ALTER TABLE logistics_plan DROP COLUMN container_no;
