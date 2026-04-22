-- ============================================================
-- Flyway 迁移脚本：商品目录表
-- 文件命名：V{版本号}__{简短描述}.sql
-- 版本号使用 4 位数字，如 V0001、V0002、V20250315_001
-- 重要：禁止修改已合并的 V1 版本，所有变更走新版本脚本
-- ============================================================

-- DDL 规范：
-- 1. 禁止删除列（使用 is_deleted 逻辑删除）
-- 2. 禁止修改列类型（新增列或建新表）
-- 3. 新增列必须设置 DEFAULT 值（保证历史数据兼容）
-- 4. 索引命名规范：idx_{表名}_{列名}，唯一索引：uk_{表名}_{列名}

-- ============================================================
-- 表1：product（商品目录）
-- 对应：SPEC-B10 · DB-11-product.md
-- ============================================================
CREATE TABLE IF NOT EXISTS product (
    -- 主键
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 系统审计字段（由 BaseEntity + JPA AuditListener 注入）
    create_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by               VARCHAR(64)  NOT NULL DEFAULT '',
    update_by               VARCHAR(64)  NOT NULL DEFAULT '',
    is_deleted              TINYINT(1)    NOT NULL DEFAULT 0,

    -- 货号（复合唯一索引）
    master_code            VARCHAR(32)  NOT NULL COMMENT '主货号（如 odn012）',
    sub_code               VARCHAR(64)  COMMENT '子货号/色号（如 re=红色，可为空）',

    -- 多语言名称
    name_ja               VARCHAR(128) COMMENT '日文名称（日本用）',
    name_en               VARCHAR(255) COMMENT '英文名称（报关用）',
    name_zh               VARCHAR(255) COMMENT '中文名称（中国用）',

    -- 图片
    image_url              VARCHAR(512) COMMENT '商品图片 URL',

    -- 基础属性
    color_name            VARCHAR(64)  COMMENT '颜色名称',
    material              VARCHAR(64)  COMMENT '材质',
    category              VARCHAR(20)  COMMENT 'OEM / ORDINARY / FACTORY_DIRECT',
    origin                VARCHAR(100) COMMENT '原产国',
    unit                  VARCHAR(50)  COMMENT '计量单位（个/台/套）',

    -- 单品尺寸
    length_cm             DECIMAL(8,2) COMMENT '单品长(cm)',
    width_cm              DECIMAL(8,2) COMMENT '单品宽(cm)',
    height_cm             DECIMAL(8,2) COMMENT '单品高(cm)',
    volume_cbm            DECIMAL(10,6) COMMENT '单品体积(m³)，自动计算',

    -- 重量
    net_weight_kg         DECIMAL(10,4) COMMENT '净重(kg)',
    gross_weight_kg       DECIMAL(10,4) COMMENT '毛重(kg)',

    -- 价格
    unit_price_rmb         DECIMAL(12,4) COMMENT '含税单价(CNY)',
    tax_point              DECIMAL(5,4) DEFAULT 1.1000 COMMENT '票点（默认1.1=含税）',
    tax_rate               DECIMAL(5,4) DEFAULT 0.1000 COMMENT '增值税率（默认10%）',

    -- 报关
    hs_code                VARCHAR(20) COMMENT 'HS编码（8-10位）',
    declaration_elements   TEXT COMMENT '申报要素，如：材质|用途|品牌',

    -- 外箱包装
    units_per_package      INT COMMENT '段ボール入数（每箱数量）',
    package_length_cm      DECIMAL(8,2) COMMENT '外箱长(cm)',
    package_width_cm       DECIMAL(8,2) COMMENT '外箱宽(cm)',
    package_height_cm      DECIMAL(8,2) COMMENT '外箱高(cm)',
    package_volume_cbm    DECIMAL(10,6) COMMENT '外箱体积(m³)',
    package_weight_kg     DECIMAL(10,4) COMMENT '外箱毛重(kg)',

    -- 仓库/质检
    warehouse             VARCHAR(64)  COMMENT '仓库归属',
    requires_qc           TINYINT(1)  COMMENT '是否需要检测',

    -- 其他
    remarks               VARCHAR(512) COMMENT '备注',
    last_used_date        DATE COMMENT '最近使用日期（来自 goods.sql.last_used）',

    -- 索引与约束
    UNIQUE KEY uk_master_sub (master_code, sub_code),
    INDEX idx_master_code (master_code),
    INDEX idx_hs_code (hs_code),
    INDEX idx_name_zh (name_zh),
    INDEX idx_create_time (create_time)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商品目录（Product 聚合根）';

-- ============================================================
-- 表2：product_factory（商品-工厂多对多关联）
-- 对应：SPEC-B10 §2.2 + DB-10 §2
-- ============================================================
CREATE TABLE IF NOT EXISTS product_factory (
    -- 主键
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 系统审计字段
    create_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    -- 外键（多对多）
    product_id             BIGINT NOT NULL COMMENT 'FK → product.id',
    factory_id             BIGINT NOT NULL COMMENT 'FK → factory.id',

    -- 工厂特定属性
    supplier_sku           VARCHAR(64) COMMENT '供应商内部货号（工厂给的产品编号）',
    moq                    INT DEFAULT 1 COMMENT '最小起订量',
    lead_time_days         INT COMMENT '交货周期(天)',
    unit_price_rmb         DECIMAL(12,4) COMMENT '该工厂的含税单价（各工厂可能不同）',
    is_preferred           TINYINT(1) DEFAULT FALSE COMMENT '是否为首选供应商',

    -- 索引与约束
    UNIQUE KEY uk_product_factory (product_id, factory_id),
    INDEX idx_product_id (product_id),
    INDEX idx_factory_id (factory_id),

    -- 外键约束
    CONSTRAINT fk_pf_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT fk_pf_factory FOREIGN KEY (factory_id) REFERENCES factory(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商品-工厂多对多关联（ProductFactory 关联实体）';
