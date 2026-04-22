
-- ============================================================
-- V4__factory_migration.sql
-- 从 companies 迁移到 factory
-- 对应: DB-10-factory.md §3 数据迁移策略
-- ============================================================

-- Step 1: 扩展 factory 表（新增字段）
-- 如果 factory 表已由 Hibernate ddl-auto=update 创建，需要补充以下列
ALTER TABLE factory
    ADD COLUMN IF NOT EXISTS category           VARCHAR(32)  NOT NULL  DEFAULT 'OTHER'  COMMENT '分类: TOOLS/TEXTILE/PLASTIC/ELECTRONICS/FURNITURE/AUTO_PARTS/SPORTS/PET/MEDICAL/CRAFTS/CHEMICAL/OTHER'  AFTER factory_name,
    ADD COLUMN IF NOT EXISTS province           VARCHAR(64)  NOT NULL  DEFAULT ''       COMMENT '省'  AFTER category,
    ADD COLUMN IF NOT EXISTS city               VARCHAR(64)  NOT NULL  DEFAULT ''       COMMENT '市'  AFTER province,
    ADD COLUMN IF NOT EXISTS county             VARCHAR(64)  NOT NULL  DEFAULT ''       COMMENT '县/区'  AFTER city,
    ADD COLUMN IF NOT EXISTS longitude          DECIMAL(11,8)          DEFAULT NULL     COMMENT '经度'  AFTER county,
    ADD COLUMN IF NOT EXISTS latitude           DECIMAL(11,8)          DEFAULT NULL     COMMENT '纬度'  AFTER longitude,
    ADD COLUMN IF NOT EXISTS contact_wechat     VARCHAR(64)            DEFAULT NULL     COMMENT '微信号'  AFTER contact_phone,
    ADD COLUMN IF NOT EXISTS contact_qq         VARCHAR(32)            DEFAULT NULL     COMMENT 'QQ号'  AFTER contact_wechat,
    ADD COLUMN IF NOT EXISTS cooperation_status VARCHAR(32) NOT NULL  DEFAULT 'POTENTIAL'  COMMENT '合作状态: ACTIVE/SUSPENDED/ELIMINATED/POTENTIAL'  AFTER contact_qq,
    ADD COLUMN IF NOT EXISTS payment_terms      VARCHAR(64) NOT NULL  DEFAULT 'NET_30'  COMMENT '账期: CASH/NET_30/NET_60/NET_90/CREDIT'  AFTER cooperation_status,
    ADD COLUMN IF NOT EXISTS notes             VARCHAR(500)           DEFAULT NULL      COMMENT '备注'  AFTER payment_terms;

-- Step 2: 添加缺失索引
ALTER TABLE factory
    ADD INDEX IF NOT EXISTS idx_factory_category (category),
    ADD INDEX IF NOT EXISTS idx_factory_cooperation_status (cooperation_status),
    ADD INDEX IF NOT EXISTS idx_factory_province (province),
    ADD INDEX IF NOT EXISTS idx_factory_city (city);

-- Step 3: 删除旧列（可选，安全做法是先注释掉，确认后再执行）
-- ALTER TABLE factory DROP COLUMN IF EXISTS location;

-- Step 4: 从 companies 迁移数据到 factory
-- 注意: is_deleted=1 的记录不迁移（已删除）

INSERT INTO factory (
    id, factory_code, factory_name, category,
    province, city, county, rough_location,
    longitude, latitude,
    cooperation_status, payment_terms,
    create_time, create_by, update_by, is_deleted
) VALUES
;

-- 迁移完成: 0 条记录写入 factory 表