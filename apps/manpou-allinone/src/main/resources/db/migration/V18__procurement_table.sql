-- ============================================================
-- Flyway 迁移脚本：发注（采购）管理表
-- INTJ 审计 2026-04-22 补充迁移
-- ============================================================
-- 对应: docs/business/SPEC-B02-发注管理流程.md §2.1
-- 实体: Procurement.java
-- ============================================================
-- 索引命名: idx_{表名}_{列名} | 唯一: uk_{表名}_{列名}
-- 审计字段: create_time/update_time/create_by/update_by/is_deleted
-- ============================================================

CREATE TABLE IF NOT EXISTS procurement (
    -- 主键
    id                          BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 审计字段
    create_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time                 DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    update_by                   VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted                  TINYINT(1)   NOT NULL DEFAULT 0,

    -- 关联
    factory_id                 BIGINT COMMENT '关联工厂ID',

    -- 商品信息
    product_code               VARCHAR(32)  NOT NULL COMMENT '货号',
    sub_product_code           VARCHAR(64)  COMMENT '子货号/色号',
    material                  VARCHAR(64)  COMMENT '材质',

    -- 质检要求
    requires_qc               TINYINT(1) COMMENT '是否需要验货',

    -- 数量
    quantity                  INT          NOT NULL COMMENT '采购数量',

    -- 价格
    price_rmb                 DECIMAL(12,4) NOT NULL COMMENT '含税人民币单价',
    exchange_rate              DECIMAL(10,4) NOT NULL COMMENT '汇率',
    tax_point                 DECIMAL(5,4) NOT NULL COMMENT '税点',

    -- 开票
    billing_type               VARCHAR(32) COMMENT '开票类型：NORMAL/REBATE/VOID',

    -- 日元估算价
    estimated_price_jpy       DECIMAL(14,2) COMMENT '预估日元含税单价',

    -- 备注与说明书
    customs_remarks            VARCHAR(512) COMMENT '报关备注',
    instruction_manual         TEXT COMMENT '说明书/作业指导书',

    -- 日期
    order_date                DATE COMMENT '下单日期',
    factory_ship_date          DATE COMMENT '工厂发货日期',
    planned_ship_date          DATE COMMENT '计划发货日期',
    actual_ship_date          DATE COMMENT '实际发货日期',

    -- Lead Time
    product_lead               VARCHAR(64) COMMENT '生产交期',
    japan_lead                VARCHAR(64) COMMENT '日本段交期',
    china_lead                VARCHAR(64) COMMENT '国内段交期',

    -- 目的地与客户
    destination               VARCHAR(128) COMMENT '目的国/地区',
    customer_company           VARCHAR(128) COMMENT '客户公司',

    -- 状态
    status                    VARCHAR(32)  NOT NULL COMMENT '发注状态',

    -- 索引
    INDEX idx_proc_factory_id (factory_id),
    INDEX idx_proc_product_code (product_code),
    INDEX idx_proc_status (status),
    INDEX idx_proc_order_date (order_date),
    INDEX idx_proc_planned_ship_date (planned_ship_date),
    INDEX idx_proc_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发注管理（Procurement 聚合根）';
