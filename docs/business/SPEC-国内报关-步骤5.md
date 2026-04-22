# 国内报关 — 业务规格（步骤5）

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: 占位（字段待确认）
> **对应前端**: `DomesticCustomsPage.vue` · `docs/ui/pages/07-domestic-customs.md`
> **前置**: LogisticsPlan.status = IN_TRANSIT
> **后续**: JapanCustomsRecord（步骤6）

---

## 1. 业务背景

国内出口报关是跨境贸易的必要环节。货物离港前，向中国海关提交出口申报，获取放行通知后货物方可装船出运。

**预期流程**：
```
LogisticsPlan.status = IN_TRANSIT
    │
    └── 自动/手动创建 DomesticCustomsRecord
            │
            ├── 录入报关资料（HS编码/申报价值/出口口岸）
            ├── 提交海关
            └── 放行 / 驳回
```

---

## 2. 聚合根定义

### 2.1 DomesticCustomsRecord

> ⚠️ 以下字段为占位，待业务方提供真实报关单样本后确认。

```
DomesticCustomsRecord（聚合根）
├── id: Long
├── procurementId: Long                  # 关联采购单（FK → procurement.id）
├── logisticsPlanId: Long                # 关联调配计划（FK → logistics_plan.id）
├── status: DomesticCustomsStatus        # PENDING / SUBMITTED / CLEARED / FAILED
├── customsDeclarationNo: String         # 报关单号（系统生成/海关返回？）
├── hsCode: String                       # HS编码（来自 Product.hsCode？）
├── productCode: String                  # 货号（来自 Procurement）
├── productName: String                  # 商品名称（来自 Product.nameZh）
├── declarationDate: LocalDate           # 申报日期
├── declarant: String                   # 申报人
├── exportPort: String                  # 出口口岸（宁波/上海/大连/天津/其他）
├── declaredValueRmb: BigDecimal         # 申报价值（CNY）
├── grossWeightKg: BigDecimal            # 毛重（kg）
├── quantity: Integer                    # 数量
├── inspectionResult: String             # 商检结果
├── remarks: String                     # 备注
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
└── 领域方法
    ├── submit()                         # 提交海关 → status = SUBMITTED
    ├── clear()                          # 放行 → status = CLEARED
    ├── reject(reason)                   # 驳回 → status = FAILED
    └── isTerminal()                     # CLEARED / FAILED 为终态
```

---

## 3. 状态枚举

```java
public enum DomesticCustomsStatus {
    PENDING,      // 待申报
    SUBMITTED,    // 已提交海关
    CLEARED,      // 已放行（终态）
    FAILED        // 被驳回（终态）
}
```

### 状态流转

```
  PENDING ──[提交]──▶ SUBMITTED ──[放行]──▶ CLEARED [终态]
                                    └──[驳回]──▶ FAILED [终态]
```

---

## 4. 自动触发规则

**规则**：LogisticsPlan.status = IN_TRANSIT 时，自动创建 DomesticCustomsRecord（status = PENDING）。

**实现方式**：
- 方案A：后端 UseCase 监听 LogisticsPlan 状态变更事件
- 方案B：前端 LogisticsPage.vue 在操作「确认发货」时调用 POST `/api/v1/domestic-customs`

> ⚠️ 推荐方案A（后端事件驱动），但当前 Kafka 尚未引入，Phase 0 建议使用方案B。

---

## 5. API 设计

### DomesticCustomsController

```
GET    /api/v1/domestic-customs?page=&pageSize=&procurementId=&status=
GET    /api/v1/domestic-customs/{id}
POST   /api/v1/domestic-customs
PATCH  /api/v1/domestic-customs/{id}
PATCH  /api/v1/domestic-customs/{id}/submit      # 提交
PATCH  /api/v1/domestic-customs/{id}/clear       # 放行
PATCH  /api/v1/domestic-customs/{id}/reject     # 驳回
```

---

## 6. 缺口阻塞（字段待确认）

| 字段 | 现状 | 阻塞原因 |
|------|------|---------|
| customsDeclarationNo | 占位 | 报关单号由系统生成还是海关返回？ |
| hsCode | 占位 | 是否与 Product.hsCode 关联？ |
| 商检流程 | 无 | 部分商品需先商检再报关，商检字段缺失 |
| exportPort 枚举 | 占位 | 具体枚举值待确认 |
| declaredValueRmb | 占位 | 由用户填入还是从采购单价计算？ |

---

## 7. 代码实现清单

- [ ] 🔴 `DomesticCustomsRecord` 聚合根实体
- [ ] 🔴 `DomesticCustomsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [ ] 🔴 `DomesticCustomsRepository` 领域接口
- [ ] 🔴 `DomesticCustomsJpaRepository` JPA 适配器
- [ ] 🔴 `DomesticCustomsAssembler` DTO ↔ Entity 转换器
- [ ] 🔴 `DomesticCustomsUseCase` 用例服务
- [ ] 🔴 `DomesticCustomsController` REST 控制器
- [ ] 🔴 `@/api/domesticCustoms.ts` 前端 API 客户端
- [ ] 🔴 `DomesticCustomsPage.vue` 页面（`docs/ui/pages/07-domestic-customs.md`）
- [ ] 🔴 `DomesticCustomsUseCaseTest` 单元测试
- [ ] 🔴 聚合接口 `GET /api/v1/orders/{id}/overview` 更新
