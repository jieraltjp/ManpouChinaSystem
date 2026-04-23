# DB-07 — 退税数据库设计

> **版本**: 1.1.0
> **创建**: 2026-04-22
> **更新**: 2026-04-23（v1.1.0：price_rmb DECIMAL(14,2)，补全索引与 DB 实际对齐）
> **状态**: ✅ 已实现
> **业务步号**: 07（退税）
> **对应业务文档**: `SPEC-B00-全链路总览.md` · `SPEC-B07-退税-步骤7.md`
> **对应 UI 文档**: `docs/ui/pages/07-tax-refund.md`
> **对应后端聚合根**: `TaxRefundRecord`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `tax_refund_record` | TaxRefundRecord | 🔴 占位 |

---

## 1. tax_refund_record（退税记录）🔴占位

> ⚠️ 字段为占位，理论退税计算公式待财务确认。

**对应**: `TaxRefundRecord` 聚合根

**理论退税公式**：
```
理论退税额(RMB) = priceRmb × quantity × (taxPoint - 1)
```

```sql
CREATE TABLE tax_refund_record (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_code          VARCHAR(32) COMMENT '退税编号 TR-YYYYMMDD-NNN（自动生成）',
    procurement_id       BIGINT COMMENT '关联采购单 FK → procurement.id',
    japan_customs_id    BIGINT COMMENT '关联日本清关单 FK → japan_customs_record.id',
    status               VARCHAR(32)  NOT NULL DEFAULT 'APPLYING' COMMENT 'APPLYING / COMPLETED / NO_REFUND',
    billing_type         VARCHAR(32) COMMENT '报关类型 NORMAL / REBATE / VOID',
    price_rmb            DECIMAL(14,2) COMMENT '含税人民币单价（来自 procurement）',
    quantity             INT COMMENT '采购数量（来自 procurement）',
    tax_point            DECIMAL(5,4) COMMENT '票点（来自 procurement）',
    exchange_rate        DECIMAL(10,6) COMMENT '汇率（来自 procurement）',
    estimated_refund_rmb DECIMAL(14,2) COMMENT '理论退税额（自动计算）',
    actual_refund_rmb   DECIMAL(14,2) COMMENT '实际退税额（用户填入）',
    refund_date          DATE COMMENT '实际退税日期',
    refund_bank          VARCHAR(128) COMMENT '退税银行账户',
    remarks              VARCHAR(512),
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by            VARCHAR(64)  NOT NULL,
    update_by            VARCHAR(64)  NOT NULL,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    INDEX idx_tr_procurement_id (procurement_id),
    INDEX idx_tr_japan_customs_id (japan_customs_id),
    INDEX idx_tr_status (status),
    INDEX idx_tr_refund_date (refund_date),
    INDEX idx_tr_create_time (create_time),
    INDEX idx_tr_is_deleted (is_deleted)
);
```

---

## 字段映射

| 实体字段 | 数据库列 | 状态 |
|---------|---------|------|
| id | `id` | ✅ |
| refundCode | `refund_code` | ✅ |
| procurementId | `procurement_id` | ✅ |
| japanCustomsId | `japan_customs_id` | ✅ |
| status | `status` | ✅ |
| billingType | `billing_type` | ✅ |
| priceRmb | `price_rmb` | ✅ |
| quantity | `quantity` | ✅ |
| taxPoint | `tax_point` | ✅ |
| exchangeRate | `exchange_rate` | ✅ |
| estimatedRefundRmb | `estimated_refund_rmb` | ✅（自动计算） |
| actualRefundRmb | `actual_refund_rmb` | ✅ |
| refundDate | `refund_date` | ✅ |
| refundBank | `refund_bank` | ✅ |

---

## 代码实现状态

- [x] ✅ `TaxRefundRecord` 聚合根实体
- [x] ✅ `TaxRefundStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [x] ✅ `TaxRefundRepository` 领域接口
- [x] ✅ `TaxRefundJpaRepository` JPA 适配器
- [x] ✅ `TaxRefundAssembler` DTO 转换器
- [x] ✅ `TaxRefundUseCase` 用例服务（含理论退税计算）
- [x] ✅ `TaxRefundController` REST 控制器
