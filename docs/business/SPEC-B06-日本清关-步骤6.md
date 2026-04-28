# 日本清关 — 业务规格（步骤6）

> **版本**: 1.2.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v1.2.0：补充 subProductCode 字段，全链路子货号追踪完整）
> **状态**: ✅ 已实现（B06 日本清关核心 CRUD + 生命周期流转 + subProductCode）
> **对应前端**: `JapanCustomsRecordPage.vue`（`apps/web/src/pages/customs/JapanCustomsRecordPage.vue`）· `docs/ui/pages/06-japan-customs.md`
> **前置**: DomesticCustomsRecord.status = CLEARED
> **后续**: TaxRefundRecord（步骤7）— ✅ 已实现

---

## 1. 业务背景

货物到达日本港口后，向日本海关办理进口清关手续，缴纳进口关税和消费税，获取放行通知后货物方可提取，进入日本仓库或直接配送。

**预期流程**：
```
DomesticCustomsRecord.status = CLEARED
    │
    └── 自动/手动创建 JapanCustomsRecord
            │
            ├── 录入清关资料（入境报关号/到达日期/清关行）
            ├── 开始清关
            ├── 缴纳税费（关税/消费税）
            └── 放行
```

---

## 2. 聚合根定义

### 2.1 JapanCustomsRecord

> ⚠️ 以下字段为占位，待业务方提供真实清关文件样本后确认。

```
JapanCustomsRecord（聚合根）
├── id: Long
├── procurementId: Long                  # 关联采购单
├── domesticCustomsId: Long              # 关联国内报关单
├── logisticsPlanId: Long                # 关联调配计划
├── subProductCode: String               # 子货号/颜色（来自 Procurement，v1.6.1 全链路追踪）
├── status: JapanCustomsStatus           # PENDING / IN_PROGRESS / CLEARED / FAILED
├── customsEntryNo: String               # 入境报关号
├── arrivalDate: LocalDate               # 到达日期
├── customsBroker: String                # 清关行
├── brokerPhone: String                  # 清关行电话
├── brokerContact: String                # 清关行联系人
├── importDutyPaid: BigDecimal          # 进口关税（JPY）
├── consumptionTaxPaid: BigDecimal       # 消费税（JPY）
├── clearanceDate: LocalDate             # 清关完成日期
├── arrivalPort: String                  # 目的港（来自 LogisticsPlan）
├── declaredWeightKg: BigDecimal         # 申报重量（来自 LogisticsPlan）
├── declaredVolumeCbm: BigDecimal        # 申报体积（来自 LogisticsPlan）
├── remarks: String                      # 备注
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
└── 领域方法
    ├── startClearance()                 # 开始清关 → status = IN_PROGRESS
    ├── complete(importDuty, consumptionTax)  # 放行 → status = CLEARED
    ├── fail(reason)                     # 失败 → status = FAILED
    └── isTerminal()                     # CLEARED / FAILED 为终态
```

---

## 3. 状态枚举

```java
public enum JapanCustomsStatus {
    PENDING,       // 待清关
    IN_PROGRESS,   // 清关中
    CLEARED,       // 已放行（终态）
    FAILED         // 清关失败（终态）
}
```

### 状态流转

```
  PENDING ──[开始]──▶ IN_PROGRESS ──[完成]──▶ CLEARED [终态]
                                          └──[失败]──▶ FAILED [终态]
```

---

## 4. 自动触发规则

**规则**：DomesticCustomsRecord.status = CLEARED 时，自动创建 JapanCustomsRecord（status = PENDING）。

---

## 5. API 设计

### JapanCustomsController

```
GET    /api/v1/japan-customs?page=&pageSize=&procurementId=&status=
GET    /api/v1/japan-customs/{id}
POST   /api/v1/japan-customs
PATCH  /api/v1/japan-customs/{id}
PATCH  /api/v1/japan-customs/{id}/start    # 开始清关
PATCH  /api/v1/japan-customs/{id}/complete # 完成清关（含缴纳税费）
PATCH  /api/v1/japan-customs/{id}/fail     # 清关失败
```

---

## 6. 缺口阻塞（字段待确认）

| 字段 | 现状 | 阻塞原因 |
|------|------|---------|
| customsEntryNo | 占位 | 入境报关号由谁提供？系统生成还是海关返回？ |
| 税费计算 | 占位 | 进口关税/消费税是否有标准计算公式？ |
| 清关费用记账 | 占位 | 税费是否需要生成 FinanceRecord？ |
| customsBroker 管理 | 占位 | 清关行是固定合作方还是有选择列表？ |

---

## 7. 代码实现清单

- [x] ✅ `JapanCustomsRecord` 聚合根实体
- [x] ✅ `JapanCustomsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [x] ✅ `JapanCustomsRepository` 领域接口（直接继承 JpaRepository）
- [x] ✅ `JapanCustomsAssembler` DTO ↔ Entity 转换器
- [x] ✅ `JapanCustomsUseCase` 用例服务
- [x] ✅ `JapanCustomsController` REST 控制器
- [x] ✅ `@/api/japanCustoms.ts` 前端 API 客户端
- [x] ✅ `JapanCustomsRecordPage.vue` 前端页面（`apps/web/src/pages/customs/JapanCustomsRecordPage.vue`）
- [x] ✅ `OrderOverviewUseCase` 已集成 JapanCustomsRecord（步骤6）
- [x] ✅ DB迁移脚本 `V12__japan_customs_record_table.sql`
- [ ] 🔴 `JapanCustomsUseCaseTest` 单元测试
