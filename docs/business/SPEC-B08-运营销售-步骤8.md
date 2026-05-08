# 运营销售 — 业务规格（步骤8）

> **版本**: 1.6.0
> **创建**: 2026-04-22
> **更新**: 2026-05-08（v1.6.0：safetyStock字段移除冗余⚠️标注（字段名已正确）；v1.5.0确认有recalculateReturnRate()/relist()）
> **更新**: 2026-05-07（v1.4.0：修正 japanCustomsId缺失/safetyThreshold→safetyStock/补全实现清单）
> **更新**: 2026-04-23（v1.2.0：实现 JapanCustoms→SalesRecord 自动创建 + SalesRecord→ReplenishmentDemand 反馈循环 + SalesChannel 枚举）
> **状态**: ✅ 已实现
> **路由**: `/sales/sales-record`
> **对应前端**: `SalesRecordPage.vue` · `docs/ui/pages/08-sales.md`
> **前置**: TaxRefundRecord / JapanCustomsRecord 已完成
> **反馈**: 步骤8 → 步骤1（补货需求 / 新品立项）

---

## 1. 业务背景

货物在日本清关完成后，上架至销售渠道（Amazon / メルカリ / 自社サイト等），追踪库存、销售和退货情况。通过销售数据驱动补货决策，形成完整正向循环。

**反馈循环**：
```
销售数据 ──库存预警──▶ 补货需求 ──新品需求──▶ 新品采购
       ──退货率高──▶ 验货标准升级
       ──退货率高──▶ 工厂评级下调
```

---

## 2. 聚合根定义

### 2.1 SalesRecord

> ⚠️ 以下字段为占位，待运营方确认。

```
SalesRecord（聚合根）
├── id: Long
├── procurementId: Long                  # 关联采购单
├── japanCustomsId: Long ⚠️             # ⚠️ Entity中不存在（审计v1.9.0：断裂业务链）
├── productCode: String                 # 主货号
├── subProductCode: String              # 子货号
├── salesChannel: SalesChannel          # 销售渠道
├── listingDate: LocalDate              # 上架日期
├── initialStock: Integer               # 初始库存（上架时的数量）
├── currentStock: Integer                # 当前库存（实时更新）
├── salesQuantity: Integer              # 累计销量
├── returnedQuantity: Integer           # 累计退货数量
├── returnRate: BigDecimal              # 退货率（自动计算）
├── sellingPriceJpy: BigDecimal         # 实际销售价（JPY）
├── safetyStock: Integer                # 安全库存阈值
├── status: SalesStatus                 # LISTED / LOW_STOCK / OUT_OF_STOCK / DISCONTINUED
├── remarks: String                    # 备注
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
└── 领域方法
    ├── updateStock(sold, returned)   # 复合方法：currentStock += returned - sold
    ├── recalculateReturnRate()        # 退货率 = returnedQuantity / salesQuantity
    ├── isLowStock()                  # currentStock < safetyStock
    ├── discontinue()                  # 下架 → status = DISCONTINUED
    ├── relist()                       # 重新上架（OUT_OF_STOCK → LISTED）
    └── isTerminal()                   # DISCONTINUED 为终态
```

### 状态枚举

```java
public enum SalesStatus {
    LISTED,         // 正常销售
    LOW_STOCK,     // 库存低于预警值
    OUT_OF_STOCK,  // 库存为零
    DISCONTINUED   // 已下架（终态）
}

```java
// SalesStatus 已在上方定义

public enum SalesChannel {
    AMAZON,     // Amazon 销售平台
    MERCALI,   // メルカリ二手平台
    SELF_SITE, // 自社サイト自有网站
    OTHER      // 其他渠道
}
```

---

## 3. 触发规则

### 3.1 上架触发

**规则**：JapanCustomsRecord.status = CLEARED 时：
```
自动创建 SalesRecord
    listingDate = today
    initialStock = Procurement.quantity
    currentStock = Procurement.quantity
    status = LISTED
```

### 3.2 库存预警

**规则**：`currentStock < safetyStock` → status = LOW_STOCK
`currentStock == 0` → status = OUT_OF_STOCK

---

## 4. 反馈循环联动

### 4.1 补货建议

```
SalesRecord.isLowStock() = true
    │
    └── generateReplenishment() → ReplenishmentDemand
            demandType = REPLENISHMENT
            productCode = this.productCode
            subProductCode = this.subProductCode
            quantity = safetyStock - currentStock
            destination = Procurement.destination（通过 procurementId 查询 Procurement 获取）
            japanLead = this.japanLead（来自 Procurement）
```

### 4.2 质量反馈

```
SalesRecord.returnRate > 0.05  // 退货率 > 5%
    │
    └── notifyQcTeam() → 更新 QcRecord.qcStandard（未来扩展）
```

### 4.3 工厂评级

```
Aggregate.returnRate > 0.10（某工厂商品的退货率）
    │
    └── flagFactory(factoryId, reason) → Factory.status = INACTIVE（需人工确认）
```

---

## 5. API 设计

### SalesRecordController

```
GET    /api/v1/sales-records?page=&pageSize=&productCode=&salesChannel=&status=
GET    /api/v1/sales-records/alerts                   # 库存预警列表
GET    /api/v1/sales-records/{id}
POST   /api/v1/sales-records                         # 新规上架
PUT    /api/v1/sales-records/{id}                    # 更新信息
PATCH  /api/v1/sales-records/{id}/stock              # 库存更新（卖出/退货）
PATCH  /api/v1/sales-records/{id}/discontinue        # 下架
PATCH  /api/v1/sales-records/{id}/relist             # 重新上架
DELETE /api/v1/sales-records/{id}                   # 删除（非终态）
```

---

## 6. 缺口阻塞

| 字段 | 现状 | 阻塞原因 |
|------|------|---------|
| 销售数据来源 | 占位 | Amazon/メルカリ API 接入还是人工录入？ |
| 库存实时扣减 | 占位 | 销售出库如何触发？平台 webhook？定时同步？ |
| 退货数据来源 | 占位 | 退货入库如何触发？ |
| 销售渠道枚举 | 占位 | 具体枚举值需运营方确认 |
| 补货建议算法 | 无 | safetyStock 按商品设置还是全局默认值？ |

---

## 7. 代码实现清单

- [x] ✅ `SalesRecord` 聚合根实体
- [x] ✅ `SalesStatus` 枚举（含 `isTerminal()` + `canTransitionTo()` + `isListed()`）

### ⚠️ 设计缺口（v1.4.0 补充）

| 缺口 | 说明 | 来源 |
|------|------|------|
| `japanCustomsId` 字段 | Entity不存在，断裂 JapanCustoms→Sales 业务链 | 审计v1.9.0 §29 |
| `list(channel, price, stock)` | Entity无此方法 | 审计v1.9.0 §29 |
| `decrementStock/incrementStock` | Entity仅`updateStock(sold,returned)`复合方法 | 审计v1.9.0 §29 |
- [x] ✅ `SalesRecordRepository` 领域接口
- [x] ✅ `SalesRecordAssembler` DTO ↔ Entity 转换器
- [x] ✅ `SalesRecordUseCase` 用例服务（含库存管理 + 退货率计算）
- [x] ✅ `SalesRecordController` REST 控制器（`/api/v1/sales-records`）
- [x] ✅ `@/api/salesOperations.ts` 前端 API 客户端
- [x] ✅ `SalesRecordPage.vue` 页面（`apps/web/src/pages/sales/SalesRecordPage.vue`）
- [x] ✅ DB migration `V14__sales_record_table.sql`
- [x] ✅ 聚合接口 `GET /api/v1/orders/{id}/overview` step8 集成（`OrderOverviewUseCase`）
- [x] ✅ `SalesChannel` 枚举（AMAZON / MERCALI / SELF_SITE / OTHER）
- [x] ✅ JapanCustomsClearedEvent + JapanCustomsClearedEventListener（清关完成→自动创建 SalesRecord）
- [x] ✅ ReplenishmentDemandNeededEvent + SalesLowStockEventListener（低库存→自动创建 ReplenishmentDemand）
- [ ] 🔴 第三方销售平台 API 接入（Amazon/メルカリ）
