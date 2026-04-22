# DB-07 — 退税数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: 🔴 占位（字段和规则待确认）
> **业务步号**: 07（退税）
> **对应业务文档**: `SPEC-B01-全链路总览.md` §第七步 · `SPEC-退税-步骤7.md`
> **对应 UI 文档**: `docs/ui/pages/09-tax-refund.md`
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
-- TODO: 字段待财务确认后补充完整
CREATE TABLE tax_refund_record (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_code          VARCHAR(32)  NOT NULL UNIQUE COMMENT '退税编号 TR-YYYYMMDD-NNN',
    procurement_id       BIGINT      NOT NULL COMMENT '关联采购单 FK → procurement.id',
    japan_customs_id    BIGINT COMMENT '关联日本清关单 FK → japan_customs_record.id',
    status               VARCHAR(20)  NOT NULL DEFAULT 'APPLYING' COMMENT 'APPLYING / COMPLETED / NO_REFUND',
    billing_type         VARCHAR(20)  NOT NULL COMMENT '报关类型 ZHE_LU_KAI_PIAO / CHAO_HUI_TUI_SHUI / NO_REFUND',
    price_rmb            DECIMAL(12,2) NOT NULL COMMENT '单价（CNY）来自 procurement',
    quantity             INT          NOT NULL COMMENT '采购数量 来自 procurement',
    tax_point            DECIMAL(5,4) COMMENT '票点 来自 procurement',
    exchange_rate        DECIMAL(10,4) COMMENT '汇率 来自 procurement',
    estimated_refund_rmb DECIMAL(12,2) COMMENT '理论退税额（自动计算）',
    actual_refund_rmb   DECIMAL(12,2) COMMENT '实际退税额（用户填入）',
    refund_date          DATE COMMENT '实际退税日期',
    refund_bank          VARCHAR(128) COMMENT '退税银行账户',
    remarks              VARCHAR(512),
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by            VARCHAR(64)  NOT NULL,
    update_by            VARCHAR(64)  NOT NULL,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    INDEX idx_tr_procurement (procurement_id),
    INDEX idx_tr_status (status)
);
```

---

## 字段映射（占位）

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
| estimatedRefundRmb | `estimated_refund_rmb` | 🔴待确认计算公式 |
| actualRefundRmb | `actual_refund_rmb` | ✅ |
| refundDate | `refund_date` | ✅ |
| refundBank | `refund_bank` | ✅ |

---

## 代码实现状态

- [ ] 🔴 `TaxRefundRecord` 聚合根实体
- [ ] 🔴 `TaxRefundStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [ ] 🔴 `TaxRefundRepository` 领域接口
- [ ] 🔴 `TaxRefundJpaRepository` JPA 适配器
- [ ] 🔴 `TaxRefundAssembler` DTO 转换器
- [ ] 🔴 `TaxRefundUseCase` 用例服务（含理论退税计算）
- [ ] 🔴 `TaxRefundController` REST 控制器
- [ ] 🔴 `@/api/taxRefund.ts` 前端 API 客户端
- [ ] 🔴 `TaxRefundPage.vue` 页面
- [ ] 🔴 `TaxRefundUseCaseTest` 单元测试
