-- ============================================================
-- V41: 需求-采购分配映射表（SPEC-B11 §11）
-- 关联：replenishment_demand.id → procurement.id（N:1）
-- 约束：(demand_id, procurement_id) 唯一，防止重复分配
-- ============================================================

CREATE TABLE IF NOT EXISTS demand_procurement_mapping (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    demand_id            BIGINT NOT NULL COMMENT '关联需求单ID',
    procurement_id       BIGINT NOT NULL COMMENT '关联采购单ID',
    allocated_quantity   INT NOT NULL COMMENT '本次分配的需求数量',
    status               VARCHAR(16) NOT NULL DEFAULT '进行中' COMMENT '状态：进行中/已完成/已取消',
    create_time          DATETIME(3) NOT NULL,
    update_time          DATETIME(3) NOT NULL,
    create_by            VARCHAR(64) NOT NULL,
    update_by            VARCHAR(64) NOT NULL,
    is_deleted           TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_demand_procurement (demand_id, procurement_id),
    INDEX idx_mapping_demand (demand_id),
    INDEX idx_mapping_procurement (procurement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求-采购分配映射表';
