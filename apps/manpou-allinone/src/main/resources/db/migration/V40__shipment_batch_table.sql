-- ============================================================
-- V40: 出货批次表（SPEC-B11 §11）
-- 关联：procurement.id → shipment_batch.procurement_id
-- ============================================================

CREATE TABLE IF NOT EXISTS shipment_batch (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    procurement_id       BIGINT NOT NULL COMMENT '关联采购单ID',
    batch_code           VARCHAR(32) NOT NULL COMMENT '批次编号（SB-YYYYMMDD-NNN）',
    shipment_quantity    INT NOT NULL COMMENT '本次出货数量（厂家实际发出）',
    factory_ship_date    DATE COMMENT '厂家发货日',
    actual_ship_date     DATE COMMENT '实际到仓日',
    status               VARCHAR(16) NOT NULL DEFAULT '待验货' COMMENT '状态：待验货/验货中/已验货/已取消',
    remarks              VARCHAR(512) COMMENT '备注',
    create_time          DATETIME(3) NOT NULL,
    update_time          DATETIME(3) NOT NULL,
    create_by            VARCHAR(64) NOT NULL,
    update_by            VARCHAR(64) NOT NULL,
    is_deleted           TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_sb_procurement (procurement_id),
    INDEX idx_sb_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出货批次表';
