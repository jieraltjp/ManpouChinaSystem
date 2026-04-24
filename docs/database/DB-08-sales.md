# DB-08 — 运营销售数据库设计

> **版本**: 1.1.0
> **创建**: 2026-04-22
> **更新**: 2026-04-23（v1.1.0：补全 sales_channel 列及索引，与 DB 实际对齐）
> **状态**: ✅ 已实现

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `sales_record` | SalesRecord | ✅ 已实现 |

---

## 1. sales_record（运营销售）

**对应**: `SalesRecord` 聚合根

```sql
CREATE TABLE sales_record (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_code          VARCHAR(32) COMMENT '销售记录编号（SA-YYYYMMDD-NNN）',
    procurement_id       BIGINT COMMENT '关联采购单 FK → procurement.id',
    product_code        VARCHAR(64) COMMENT '主货号',
    sub_product_code    VARCHAR(64) COMMENT '子货号（颜色）',
    sales_channel       VARCHAR(32) COMMENT '销售渠道 AMAZON / MERCALI / SELF_SITE / OTHER',
    status              VARCHAR(32) NOT NULL DEFAULT 'LISTED' COMMENT 'LISTED / LOW_STOCK / OUT_OF_STOCK / DISCONTINUED',
    listing_date         DATE COMMENT '上架日期',
    initial_stock       INT COMMENT '初始上架库存',
    current_stock       INT COMMENT '当前库存（实时更新）',
    safety_stock        INT COMMENT '安全库存阈值',
    sales_quantity      INT COMMENT '累计销量',
    returned_quantity   INT COMMENT '累计退货数量',
    return_rate         DECIMAL(6,4) COMMENT '退货率（自动计算）',
    selling_price_jpy   DECIMAL(14,2) COMMENT '实际销售价（JPY）',
    remarks             VARCHAR(512) COMMENT '备注',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by           VARCHAR(64) NOT NULL,
    update_by           VARCHAR(64) NOT NULL,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_sr_product_code (product_code),
    INDEX idx_sr_procurement_id (procurement_id),
    INDEX idx_sr_status (status),
    INDEX idx_sr_sales_channel (sales_channel),
    INDEX idx_sr_create_time (create_time),
    INDEX idx_sr_is_deleted (is_deleted)
);
```

---

## 字段映射

| 实体字段 | 数据库列 | 状态 |
|---------|---------|------|
| id | `id` | ✅ |
| recordCode | `record_code` | ✅ |
| procurementId | `procurement_id` | ✅ |
| productCode | `product_code` | ✅ |
| subProductCode | `sub_product_code` | ✅ |
| salesChannel | `sales_channel` | ✅ |
| status | `status` | ✅ |
| listingDate | `listing_date` | ✅ |
| initialStock | `initial_stock` | ✅ |
| currentStock | `current_stock` | ✅ |
| safetyStock | `safety_stock` | ✅ |
| salesQuantity | `sales_quantity` | ✅ |
| returnedQuantity | `returned_quantity` | ✅ |
| returnRate | `return_rate` | ✅（自动计算） |
| sellingPriceJpy | `selling_price_jpy` | ✅ |
| remarks | `remarks` | ✅ |

---

## 代码实现状态

- [x] ✅ `SalesRecord` 聚合根实体
- [x] ✅ `SalesStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [x] ✅ `SalesRecordRepository` 领域接口
- [x] ✅ `SalesRecordJpaRepository` JPA 适配器
- [x] ✅ `SalesRecordAssembler` DTO 转换器
- [x] ✅ `SalesRecordUseCase` 用例服务（含库存管理）
- [x] ✅ `SalesRecordController` REST 控制器
