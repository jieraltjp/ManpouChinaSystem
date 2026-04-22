# DB-08 — 运营销售数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: 🔴 占位（字段和触发时机待确认）
> **业务步号**: 08（运营销售）
> **对应业务文档**: `SPEC-B00-全链路总览.md` · `SPEC-B08-运营销售-步骤8.md`
> **对应 UI 文档**: `docs/ui/pages/08-sales.md`
> **对应后端聚合根**: `SalesRecord`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `sales_record` | SalesRecord | 🔴 占位 |

---

## 1. sales_record（运营销售）🔴占位

> ⚠️ 字段为占位，触发时机和库存管理规则待运营方确认。

**对应**: `SalesRecord` 聚合根

```sql
-- TODO: 字段待运营方确认后补充完整
CREATE TABLE sales_record (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    procurement_id       BIGINT NOT NULL COMMENT '关联采购单 FK → procurement.id',
    japan_customs_id    BIGINT COMMENT '关联日本清关单 FK → japan_customs_record.id',
    product_code        VARCHAR(32) NOT NULL COMMENT '主货号',
    sub_product_code    VARCHAR(64) COMMENT '子货号',
    sales_channel       VARCHAR(20) NOT NULL COMMENT 'AMAZON / MERCARI / 自社サイト / OTHER',
    listing_date         DATE NOT NULL COMMENT '上架日期',
    initial_stock       INT NOT NULL COMMENT '初始库存（上架时数量）',
    current_stock       INT NOT NULL COMMENT '当前库存（实时更新）',
    sales_quantity      INT DEFAULT 0 COMMENT '累计销量',
    returned_quantity   INT DEFAULT 0 COMMENT '累计退货数量',
    return_rate         DECIMAL(5,4) DEFAULT 0 COMMENT '退货率（自动计算）',
    selling_price_jpy   BIGINT COMMENT '实际销售价（JPY）',
    estimated_price_jpy BIGINT COMMENT '估算批发价（来自 procurement，参考）',
    safety_threshold    INT DEFAULT 50 COMMENT '安全库存阈值',
    status              VARCHAR(20) NOT NULL DEFAULT 'LISTED' COMMENT 'LISTED / LOW_STOCK / OUT_OF_STOCK / DISCONTINUED',
    remarks             VARCHAR(512),
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by           VARCHAR(64) NOT NULL,
    update_by           VARCHAR(64) NOT NULL,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_sr_product (product_code),
    INDEX idx_sr_channel (sales_channel),
    INDEX idx_sr_status (status)
);
```

---

## 字段映射（占位）

| 实体字段 | 数据库列 | 状态 |
|---------|---------|------|
| id | `id` | ✅ |
| procurementId | `procurement_id` | ✅ |
| japanCustomsId | `japan_customs_id` | ✅ |
| productCode | `product_code` | ✅ |
| subProductCode | `sub_product_code` | ✅ |
| salesChannel | `sales_channel` | 🔴待确认枚举值 |
| listingDate | `listing_date` | ✅ |
| initialStock | `initial_stock` | ✅ |
| currentStock | `current_stock` | 🔴待确认库存更新机制 |
| salesQuantity | `sales_quantity` | 🔴待确认销量来源 |
| returnedQuantity | `returned_quantity` | 🔴待确认退货来源 |
| returnRate | `return_rate` | ✅（自动计算） |
| sellingPriceJpy | `selling_price_jpy` | ✅ |
| estimatedPriceJpy | `estimated_price_jpy` | ✅ |
| safetyThreshold | `safety_threshold` | ✅ |
| status | `status` | ✅ |

---

## 代码实现状态

- [ ] 🔴 `SalesRecord` 聚合根实体
- [ ] 🔴 `SalesStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [ ] 🔴 `SalesChannel` 枚举
- [ ] 🔴 `SalesRecordRepository` 领域接口
- [ ] 🔴 `SalesRecordJpaRepository` JPA 适配器
- [ ] 🔴 `SalesRecordAssembler` DTO 转换器
- [ ] 🔴 `SalesRecordUseCase` 用例服务（含库存管理）
- [ ] 🔴 `SalesRecordController` REST 控制器
- [ ] 🔴 `@/api/salesRecord.ts` 前端 API 客户端
- [ ] 🔴 `SalesOperationsPage.vue` 页面
- [ ] 🔴 `SalesRecordUseCaseTest` 单元测试
