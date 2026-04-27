-- ============================================================
-- Flyway 迁移脚本：QC 验货记录表
-- INTJ 审计 2026-04-22 补充迁移
-- ============================================================
-- 对应: docs/business/SPEC-B04-验货-步骤4.md §2.1
-- 实体: QcRecord.java
-- ============================================================
-- 索引命名: idx_{表名}_{列名} | 唯一: uk_{表名}_{列名}
-- 审计字段: create_time/update_time/create_by/update_by/is_deleted
-- ============================================================

CREATE TABLE IF NOT EXISTS qc_record (
    -- 主键
    id                          BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 审计字段
    create_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    update_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted                  TINYINT(1)   NOT NULL DEFAULT 0,

    -- 核心字段
    qc_code                    VARCHAR(32)  NOT NULL COMMENT '验货单号（QC-YYYYMMDD-NNN）',
    procurement_id             BIGINT       NOT NULL COMMENT '关联发注单ID',

    -- 商品信息
    seller_name                VARCHAR(128) COMMENT '卖家名称',
    product_code               VARCHAR(32)  NOT NULL COMMENT '货号',
    sub_product_code           VARCHAR(64)  COMMENT '子货号/色号',
    material                  VARCHAR(64)  COMMENT '材质',

    -- 验货信息
    qc_user_id                BIGINT COMMENT '验货员ID',
    qc_type                   VARCHAR(16)  COMMENT '验货类型',
    qc_date                   DATE COMMENT '验货日期',
    result                    VARCHAR(16)  NOT NULL COMMENT '验货结果',
    status                    VARCHAR(24)  NOT NULL COMMENT '状态',

    -- 数量统计
    inspection_count          INT COMMENT '抽检数量',
    passed_count              INT COMMENT '合格数量',
    defective_count           INT COMMENT '不合格数量',
    box_count                 INT COMMENT '箱数',
    quantity                  INT COMMENT '总数量',

    -- 尺寸/重量
    box_length_cm             DECIMAL(8,2) COMMENT '外箱长(cm)',
    box_width_cm              DECIMAL(8,2) COMMENT '外箱宽(cm)',
    box_height_cm             DECIMAL(8,2) COMMENT '外箱高(cm)',
    net_weight_per_unit       DECIMAL(10,4) COMMENT '单品净重(kg)',
    gross_weight              DECIMAL(10,4) COMMENT '毛重(kg)',

    -- 财务
    tax_inclusive_price       DECIMAL(14,2) COMMENT '含税单价',

    -- 质检配置
    tax_refund                TINYINT(1) COMMENT '是否退税（v1.1.1 前端废弃，列保留）',
    qc_standard              VARCHAR(512) COMMENT '验货标准',

    -- 图片/备注
    images                    JSON COMMENT '验货图片(JSON数组)',
    remarks                   VARCHAR(512) COMMENT '备注',
    destination               VARCHAR(128) COMMENT '目的国',

    -- 订单日期
    order_date                DATE COMMENT '下单日期',

    -- 索引
    UNIQUE KEY uk_qc_code (qc_code),
    INDEX idx_qc_procurement_id (procurement_id),
    INDEX idx_qc_product_code (product_code),
    INDEX idx_qc_status (status),
    INDEX idx_qc_date (qc_date),
    INDEX idx_qc_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='QC 验货记录（QcRecord 聚合根）';
