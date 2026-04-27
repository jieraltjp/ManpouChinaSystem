# 退税 — 业务规格（步骤7）

> **版本**: 1.2.0
> **创建**: 2026-04-22
> **更新**: 2026-04-27（v1.2.0：修正触发规则为手动创建；原「JapanClear → 自动创建」为错误描述，代码中无对应实现）
> **状态**: ✅ 已实现（手动创建）
> **对应前端**: `TaxRefundPage.vue` · `docs/ui/pages/07-tax-refund.md`
> **前置**: JapanCustomsRecord.status = CLEARED（货物抵达日本）
> **后续**: SalesRecord（步骤8）/ FinanceRecord

---

## 1. 业务背景

货物出口后，符合退税条件的企业可申请出口退税。根据采购单的报关类型（BillingType）：
- `ZHE_LU_KAI_PIAO`（浙鲁开票）→ 可退税
- `CHAO_HUI_TUI_SHUI`（超慧退税）→ 可退税
- `NO_REFUND`（不退税）→ 无需退税流程

**触发时机**：JapanCustomsRecord.status = CLEARED 时，根据 BillingType 判断是否创建退税记录。

---

## 2. 聚合根定义

### 2.1 TaxRefundRecord

```
TaxRefundRecord（聚合根）
├── id: Long
├── procurementId: Long                  # 关联采购单
├── japanCustomsId: Long                # 关联日本清关单
├── status: TaxRefundStatus              # APPLYING / COMPLETED / NO_REFUND
├── billingType: BillingType             # 报关类型（来自 Procurement）
├── priceRmb: BigDecimal                # 单价（来自 Procurement）
├── quantity: Integer                   # 采购数量（来自 Procurement）
├── taxPoint: BigDecimal                # 票点（来自 Procurement）
├── estimatedRefundRmb: BigDecimal       # 理论退税额（自动计算）
├── actualRefundRmb: BigDecimal         # 实际退税额（用户填入）
├── exchangeRate: BigDecimal             # 汇率（来自 Procurement；注：当前退税公式不涉及此字段，保留用于未来扩展）
├── refundDate: LocalDate               # 实际退税日期
├── refundBank: String                  # 退税银行账户
├── remarks: String                     # 备注
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
└── 领域方法
    ├── calculateEstimatedRefund()        # 计算理论退税额
    ├── complete(actualAmount)           # 标记退税完成
    ├── markNoRefund()                   # 标记为不退税
    └── isTerminal()                    # COMPLETED / NO_REFUND 为终态
```

### 理论退税计算公式

```
理论退税额(RMB) = priceRmb × quantity × (taxPoint - 1)
```

> 例：priceRmb = 100, quantity = 200, taxPoint = 1.1
> → 理论退税 = 100 × 200 × 0.1 = 2000 CNY

---

## 3. 状态枚举

```java
public enum TaxRefundStatus {
    APPLYING,     // 退税申请中
    COMPLETED,    // 已退税（终态）
    NO_REFUND     // 不退税（终态）
}
```

### 状态流转

```
  APPLYING ──[完成]──▶ COMPLETED [终态]
       │
       └──[不退税]──▶ NO_REFUND [终态]
```

---

## 4. 触发规则（v1.2.0 修正）

**手动创建**：用户在 TaxRefundPage 点击「新建」，录入退税信息。

> ⚠️ 原「JapanCustomsRecord.status = CLEARED → 自动创建 TaxRefundRecord」为错误描述，
> 代码中无对应事件+监听器实现，已修正为手动创建。

---

## 5. API 设计

### TaxRefundController

```
GET    /api/v1/tax-refunds?page=&pageSize=&procurementId=&status=
GET    /api/v1/tax-refunds/{id}
POST   /api/v1/tax-refunds
PATCH  /api/v1/tax-refunds/{id}/complete    # 标记完成
PATCH  /api/v1/tax-refunds/{id}/no-refund   # 标记不退税
DELETE /api/v1/tax-refunds/{id}            # 软删除（终态禁止删除）
```

---

## 6. 缺口阻塞

| 字段 | 现状 | 阻塞原因 |
|------|------|---------|
| 理论退税计算公式 | 占位 | 与财务确认是否按此公式计算 |
| 触发时机 | 占位 | JapanClear 后自动创建还是手动触发？ |
| 实际退税额差异 | 无 | 实际退税额与理论不符的处理规则？ |
| 财务记账 | 无 | COMPLETED 后是否自动生成 FinanceRecord？ |

---

## 7. 代码实现清单

- [x] ✅ `TaxRefundRecord` 聚合根实体
- [x] ✅ `TaxRefundStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [x] ✅ `TaxRefundRepository` 领域接口
- [x] ✅ `TaxRefundAssembler` DTO ↔ Entity 转换器
- [x] ✅ `TaxRefundUseCase` 用例服务（含理论退税计算）
- [x] ✅ `TaxRefundController` REST 控制器（`/api/v1/tax-refunds`）
- [x] ✅ `@/api/taxRefund.ts` 前端 API 客户端
- [x] ✅ `TaxRefundPage.vue` 页面（`apps/web/src/pages/finance/TaxRefundPage.vue`）
- [x] ✅ DB migration `V13__tax_refund_record_table.sql`
- [x] ✅ 聚合接口 `GET /api/v1/orders/{id}/overview` step7 集成（`OrderOverviewUseCase`）
- [ ] 🔴 `TaxRefundUseCaseTest` 单元测试（含理论计算验证）
