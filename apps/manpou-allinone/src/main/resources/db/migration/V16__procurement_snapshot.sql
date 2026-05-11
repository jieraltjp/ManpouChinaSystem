-- ============================================================
-- V16: 发注单快照表（procurement_snapshot）
-- Entity: com.manpou.allinone.order.domain.model.ProcurementSnapshot
-- 说明: 记录下单时刻的工厂和商品信息，保证历史订单数据不变。
--       由 ProcurementUseCase 在创建发注单时自动填充。
-- 幂等: CREATE TABLE IF NOT EXISTS（V15 已包含表结构，此文件仅做兜底）
-- ============================================================

CREATE TABLE IF NOT EXISTS procurement_snapshot (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    procurement_id       BIGINT NOT NULL COMMENT '关联发注单ID（UNIQUE）',
    factory_id           BIGINT COMMENT '工厂ID',
    factory_code         VARCHAR(32) COMMENT '工厂编号（下单时刻）',
    factory_name         VARCHAR(128) COMMENT '工厂名称（下单时刻）',
    factory_province     VARCHAR(64) COMMENT '工厂省份（下单时刻）',
    factory_city         VARCHAR(64) COMMENT '工厂城市（下单时刻）',
    factory_contact_name VARCHAR(64) COMMENT '工厂联系人（下单时刻）',
    factory_contact_phone VARCHAR(32) COMMENT '工厂电话（下单时刻）',
    product_name_zh      VARCHAR(255) COMMENT '商品中文名（下单时刻）',
    product_name_ja      VARCHAR(128) COMMENT '商品日文名（下单时刻）',
    product_category     VARCHAR(32) COMMENT '商品分类（下单时刻）',
    create_time          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    update_by            VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    is_deleted           BIT(1) NOT NULL DEFAULT b'0',
    UNIQUE KEY uk_snapshot_procurement (procurement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='发注单快照（记录下单时刻的工厂和商品信息，历史数据不变）';
