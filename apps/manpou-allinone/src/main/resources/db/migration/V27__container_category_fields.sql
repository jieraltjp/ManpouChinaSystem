-- V27: 分类容器管理（SPEC-B14）
-- 对应: SPEC-B14 / DB-15
-- 作者: jieraltjp
-- 日期: 2026-05-24
--
-- 内容:
--   Step 1: container 表扩展字段（箱号/到港时段/原始出运状态/显示标志/原始ID/原始更新时间人）
--   Step 2: 新增索引

-- Step 1: container 表扩展字段
ALTER TABLE container
    ADD COLUMN cabinet_no           VARCHAR(16)    COMMENT '箱号（ISO格式，如 CBHU4225619）',
    ADD COLUMN period              VARCHAR(16)    COMMENT '到港时段：凌晨0-6 / 早上6-12 / 下午12-18 / 晚上18-24',
    ADD COLUMN legacy_status      VARCHAR(32)    COMMENT '原始出运状态（未出 / 出完 / 待定 等）',
    ADD COLUMN show_flag          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '显示标志（0=归档，1=活跃）',
    ADD COLUMN legacy_id          BIGINT         COMMENT '原始数据主键（来自 list7.ID，1:1保留）',
    ADD COLUMN legacy_updater     VARCHAR(64)    COMMENT '原始最后更新人',
    ADD COLUMN legacy_updatetime  DATETIME(3)    COMMENT '原始最后更新时间';

-- Step 2: 新增索引
ALTER TABLE container
    ADD INDEX idx_container_cabinet_no  (cabinet_no),
    ADD INDEX idx_container_show_flag   (show_flag),
    ADD INDEX idx_container_legacy_id   (legacy_id);
