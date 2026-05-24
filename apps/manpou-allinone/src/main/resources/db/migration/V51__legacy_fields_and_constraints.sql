-- ============================================================
-- V51: list1/list8 历史数据导入 — Schema 扩展
-- 日期：2026-05-24
-- 内容：
--   1. procurement 表新增 legacy 溯源字段
--   2. logistics_plan 表新增 legacy 溯源字段
--   3. factory 表新增 legacy 溯源字段
--   4. legacy ID 映射表（新增）
--   5. legacy 记录 NOT NULL 约束放宽
--   6. 索引
-- ============================================================

-- =============================================
-- 1. Procurement legacy 溯源字段
-- =============================================
ALTER TABLE procurement
    ADD COLUMN legacy_list1_id    BIGINT           COMMENT 'list1.ID 溯源',
    ADD COLUMN legacy_order_group VARCHAR(128)     COMMENT 'list1.order-group 批次号',
    ADD COLUMN legacy_item_name   VARCHAR(512)     COMMENT 'list1.item-name 日文品名',
    ADD COLUMN legacy_img         VARCHAR(512)     COMMENT 'list1.img 图片文件名',
    ADD COLUMN legacy_inspect_qty INT DEFAULT 0    COMMENT 'list1.inspect-count 验货数量',
    ADD COLUMN legacy_fba_stock   INT DEFAULT 0    COMMENT 'list1.fba-stock FBA库存',
    ADD COLUMN legacy_container_no VARCHAR(128)    COMMENT 'list1.container 货柜号',
    ADD COLUMN legacy_updater     VARCHAR(64)      COMMENT 'list1.updater 原始更新人',
    ADD COLUMN legacy_updatetime  DATETIME(3)      COMMENT 'list1.updatetime 原始更新时间',
    ADD COLUMN legacy_is_legacy   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否历史导入';

-- =============================================
-- 2. LogisticsPlan legacy 溯源字段
-- =============================================
ALTER TABLE logistics_plan
    ADD COLUMN legacy_list8_id    BIGINT           COMMENT 'list8.ID 溯源',
    ADD COLUMN legacy_pieces     INT              COMMENT 'list8.pieces 件数',
    ADD COLUMN legacy_destination VARCHAR(128)     COMMENT 'list8.destination 目的港',
    ADD COLUMN legacy_warehouse  VARCHAR(128)     COMMENT 'list8.souko 原始仓库名（城市或工厂名）',
    ADD COLUMN legacy_location   VARCHAR(128)     COMMENT 'list8.location 原始位置',
    ADD COLUMN legacy_material   VARCHAR(64)      COMMENT 'list8.material 材质',
    ADD COLUMN legacy_kensa      VARCHAR(255)     COMMENT 'list8.kensa 检品标识',
    ADD COLUMN legacy_show_flag  TINYINT(1)      COMMENT 'list8.showFlag 原归档标志',
    ADD COLUMN legacy_status     VARCHAR(64)      COMMENT 'list8.status 原状态文本',
    ADD COLUMN legacy_unit_ch    DECIMAL(12,4)  COMMENT 'list8.unit_ch 单价(元)，参考值（80%为0）',
    ADD COLUMN legacy_rate       DECIMAL(10,4)  COMMENT 'list8.rate 汇率',
    ADD COLUMN legacy_updater    VARCHAR(64)     COMMENT 'list8.updateuser 原始更新人（装箱操作人）',
    ADD COLUMN legacy_updatetime DATETIME(3)    COMMENT 'list8.updatetime 原始更新时间',
    ADD COLUMN legacy_is_legacy TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否历史导入';

-- =============================================
-- 3. Factory legacy 字段
-- =============================================
ALTER TABLE factory
    ADD COLUMN legacy_source        VARCHAR(32)    COMMENT '来源：list8',
    ADD COLUMN legacy_souko        VARCHAR(128)   COMMENT 'list8.souko 原始值',
    ADD COLUMN legacy_location      VARCHAR(128)   COMMENT 'list8.location 原始值',
    ADD COLUMN legacy_is_warehouse TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否来自list8仓库/供应商';

-- =============================================
-- 4. Legacy ID 映射表（新增）
-- 用于记录 list1.ID / list8.ID → 新系统主键 的完整映射
-- =============================================
CREATE TABLE legacy_id_mapping (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_table        VARCHAR(32)  NOT NULL COMMENT 'list1 或 list8',
    legacy_id           BIGINT      NOT NULL COMMENT '原始表主键（list1.ID / list8.ID）',
    target_table        VARCHAR(32)  NOT NULL COMMENT '目标表：procurement / logistics_plan',
    target_id          BIGINT       COMMENT '新系统主键（可NULL：未匹配时）',
    product_code        VARCHAR(64)  COMMENT '货号（用于对账）',
    import_status       VARCHAR(32)  NOT NULL DEFAULT 'imported' COMMENT 'imported=已导入/skipped=跳过/duplicate=重复',
    skip_reason         VARCHAR(255) COMMENT '跳过原因（import_status=skipped 时填充）',
    create_time         DATETIME(3)  NOT NULL DEFAULT NOW(3),

    UNIQUE KEY uk_source_legacy_id (source_table, legacy_id),
    INDEX idx_target (target_table, target_id),
    INDEX idx_product_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='legacy ID 映射表：记录 list1/list8.ID → 新系统主键';

-- =============================================
-- 5. NOT NULL 约束放宽
-- legacy 数据中部分字段大量为空，强制 NOT NULL 会导致导入失败
-- 新记录由业务层 Java 代码保证非空
-- =============================================

-- Procurement
ALTER TABLE procurement
    MODIFY COLUMN price_rmb     DECIMAL(12,4)           COMMENT '人民币单价（legacy可空）',
    MODIFY COLUMN exchange_rate DECIMAL(10,4)           COMMENT '汇率（legacy可空）',
    MODIFY COLUMN tax_point     DECIMAL(5,4)            COMMENT '票点（legacy可空，默认1.1）';

-- LogisticsPlan
ALTER TABLE logistics_plan
    MODIFY COLUMN cargo_weight_kg DECIMAL(10,4) COMMENT '重量(kg)（legacy可空）';

-- =============================================
-- 6. 索引
-- =============================================
ALTER TABLE procurement
    ADD INDEX idx_proc_legacy_list1     (legacy_list1_id),
    ADD INDEX idx_proc_legacy_order     (legacy_order_group),
    ADD INDEX idx_proc_legacy_flag      (legacy_is_legacy),
    ADD INDEX idx_proc_legacy_container (legacy_container_no);

ALTER TABLE logistics_plan
    ADD INDEX idx_lp_legacy_list8    (legacy_list8_id),
    ADD INDEX idx_lp_legacy_flag     (legacy_is_legacy),
    ADD INDEX idx_lp_legacy_product (product_code, legacy_is_legacy);

ALTER TABLE factory
    ADD INDEX idx_factory_legacy (legacy_source);
