-- ============================================================
-- V38: demand status 新增 CONFIRMED 状态
-- 背景: 补货人员需要确认/待确认两个状态来回切换
-- 对应 DemandStatus: PENDING / CONFIRMED / CONVERTED / CANCELLED
-- ============================================================

-- 修复 DB 中 status 列类型（可能是 ENUM 或 VARCHAR 不足）
-- VARCHAR(32) 可存储所有状态值
ALTER TABLE replenishment_demand
    MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
    COMMENT '状态：PENDING=待确认，CONFIRMED=已确认，CONVERTED=已转采购，CANCELLED=已取消';
