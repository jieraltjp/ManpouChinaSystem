# 订单总览 — API 设计

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: 设计中
> **对应前端**: `OrderOverviewPage.vue` · `docs/ui/pages/09-order-overview.md`
> **核心**: 以 Procurement.id 为锚点，聚合全链路 8 步数据

---

## 1. 设计背景

目前系统各步骤（补货/发注/验货/调配/报关/清关/退税/运营）数据独立存储、分散展示。用户需要切换多个页面才能了解一个订单的完整生命周期。

**订单总览接口**解决这个问题：以一条 Procurement 为锚点，一次调用返回全链路聚合数据。

---

## 2. 接口契约

### 2.1 聚合查询（核心）

```
GET /api/v1/orders/{procurementId}/overview
```

**响应 200**：

```json
{
  "procurementId": 1,
  "procurement": { ... },
  "demand": { ... },
  "qcRecord": { ... },
  "logisticsPlan": { ... },
  "domesticCustoms": { ... },
  "japanCustoms": { ... },
  "taxRefund": { ... },
  "salesRecord": { ... },
  "factory": { ... },
  "product": { ... }
}
```

> ⚠️ 响应体各步骤字段为占位，待对应聚合根实现后填充完整。

**响应 404**：`Procurement` 不存在或已删除

---

## 3. 聚合数据模型

### 3.1 锚点 — Procurement（必定返回）

```json
{
  "id": 1,
  "factoryId": 10,
  "productCode": "odn012",
  "subProductCode": "re",
  "material": "木质",
  "requiresQc": true,
  "quantity": 200,
  "priceRmb": "100.00",
  "exchangeRate": "21.5000",
  "taxPoint": "1.1000",
  "billingType": "CHAO_HUI_TUI_SHUI",
  "estimatedPriceJpy": "2593.00",
  "orderDate": "2026-04-01",
  "factoryShipDate": "2026-04-10",
  "plannedShipDate": "2026-04-20",
  "actualShipDate": "2026-04-18",
  "productLead": "张三",
  "japanLead": "田中",
  "chinaLead": "李四",
  "destination": "久留米",
  "customerCompany": "久留米商事",
  "status": "倉庫着",
  "createTime": "2026-04-01T10:00:00Z"
}
```

### 3.2 步骤1 — ReplenishmentDemand（可选）

```json
{
  "id": 5,
  "demandCode": "D-20260401-001",
  "demandType": "NEW_PURCHASE",
  "productCode": "odn012",
  "subProductCode": "re",
  "quantity": 200,
  "destination": "久留米",
  "japanLead": "田中",
  "status": "CONVERTED",
  "createTime": "2026-04-01T09:00:00Z"
}
```

### 3.3 步骤2 — Factory（可选）

```json
{
  "id": 10,
  "factoryCode": "F-20260401-001",
  "factoryName": "义乌XX家具厂",
  "location": "浙江省金华市",
  "roughLocation": "义乌工业区",
  "contactName": "王五",
  "contactPhone": "13800138000",
  "status": "ACTIVE"
}
```

### 3.4 步骤3 — QcRecord（可选）

```json
{
  "id": 20,
  "qcCode": "Q-20260418-003",
  "procurementId": 1,
  "sellerName": "义乌XX家具厂",
  "productCode": "odn012",
  "subProductCode": "re",
  "result": "PASS",
  "inspectionCount": 200,
  "passedCount": 198,
  "defectiveCount": 2,
  "boxCount": 10,
  "boxLengthCm": "60.00",
  "boxWidthCm": "40.00",
  "boxHeightCm": "30.00",
  "netWeightPerUnit": "5.5000",
  "grossWeight": "1100.0000",
  "qcDate": "2026-04-18",
  "qcUserId": 3,
  "status": "COMPLETED"
}
```

### 3.5 步骤4 — LogisticsPlan（可选）

```json
{
  "id": 30,
  "planCode": "L-20260419-001",
  "procurementId": 1,
  "factoryId": 10,
  "productCode": "odn012",
  "subProductCode": "re",
  "planType": "SEA",
  "status": "IN_TRANSIT",
  "cargoLengthCm": "60.00",
  "cargoWidthCm": "40.00",
  "cargoHeightCm": "30.00",
  "cargoVolumeCbm": "0.0720",
  "cargoWeightKg": "1100.0000",
  "quantity": 200,
  "requiresQc": true,
  "containerId": 1,        // ⚠️ containerNo/departurePort/arrivalPort 待 Container 实体实现后补充
  "estimatedShipDate": "2026-04-20",
  "actualShipDate": "2026-04-19"
}
```

### 3.6 步骤5 — DomesticCustomsRecord（可选）

> ⚠️ 占位，待 DomesticCustomsRecord 实现后填充。

```json
{
  "id": 40,
  "procurementId": 1,
  "logisticsPlanId": 30,
  "status": "SUBMITTED",
  "customsDeclarationNo": "CUS-20260420-001",
  "hsCode": "940360",
  "productCode": "odn012",
  "productName": "木制家具",
  "declarationDate": "2026-04-20",
  "exportPort": "宁波",
  "declaredValueRmb": "20000.00"
}
```

### 3.7 步骤6 — JapanCustomsRecord（可选）

> ⚠️ 占位，待 JapanCustomsRecord 实现后填充。

```json
{
  "id": 50,
  "procurementId": 1,
  "domesticCustomsId": 40,
  "status": "CLEARED",
  "customsEntryNo": "JP-20260428-001",
  "arrivalDate": "2026-04-28",
  "customsBroker": "東京通関株式会社",
  "brokerPhone": "+81-3-xxxx-xxxx",
  "importDutyPaid": 150000,
  "consumptionTaxPaid": 30000,
  "clearanceDate": "2026-04-29"
}
```

### 3.8 步骤7 — TaxRefundRecord（可选）

> ⚠️ 占位，待 TaxRefundRecord 实现后填充。

```json
{
  "id": 60,
  "procurementId": 1,
  "japanCustomsId": 50,
  "status": "COMPLETED",
  "billingType": "CHAO_HUI_TUI_SHUI",
  "priceRmb": "100.00",
  "quantity": 200,
  "taxPoint": "1.1000",
  "estimatedRefundRmb": "2000.00",
  "actualRefundRmb": "2000.00",
  "refundDate": "2026-04-30"
}
```

### 3.9 步骤8 — SalesRecord（可选）

> ⚠️ 占位，待 SalesRecord 实现后填充。

```json
{
  "id": 70,
  "procurementId": 1,
  "japanCustomsId": 50,
  "productCode": "odn012",
  "subProductCode": "re",
  "salesChannel": "AMAZON",
  "listingDate": "2026-05-01",
  "initialStock": 200,
  "currentStock": 180,
  "salesQuantity": 20,
  "returnedQuantity": 2,
  "returnRate": "0.10",
  "sellingPriceJpy": "2800.00",
  "estimatedPriceJpy": "2593.00",
  "safetyThreshold": 50,
  "status": "LISTED"
}
```

---

## 4. 后端实现

### 4.1 控制器

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOverviewController {

    private final OrderOverviewUseCase orderOverviewUseCase;

    @GetMapping("/{procurementId}/overview")
    public OrderOverviewVO getOverview(@PathVariable Long procurementId) {
        return orderOverviewUseCase.getOverview(procurementId);
    }
}
```

### 4.2 UseCase

```java
@Service
@RequiredArgsConstructor
public class OrderOverviewUseCase {

    private final ProcurementRepository procurementRepository;
    private final ReplenishmentDemandRepository demandRepository;
    private final QcRecordRepository qcRecordRepository;
    private final LogisticsPlanRepository logisticsPlanRepository;
    private final FactoryRepository factoryRepository;
    // 步骤5-8 Repository（待实现）

    @Transactional(readOnly = true)
    public OrderOverviewVO getOverview(Long procurementId) {
        Procurement procurement = procurementRepository
                .findByIdAndIsDeletedFalse(procurementId)
                .orElseThrow(() -> BusinessException.notFound("Procurement", procurementId));

        return OrderOverviewVO.builder()
                .procurementId(procurement.getId())
                .procurement(mapProcurement(procurement))
                .demand(findDemand(procurement))
                .factory(findFactory(procurement.getFactoryId()))
                .qcRecord(findQcRecord(procurementId))
                .logisticsPlan(findLogisticsPlan(procurementId))
                .domesticCustoms(findDomesticCustoms(procurementId))   // 待实现
                .japanCustoms(findJapanCustoms(procurementId))           // 待实现
                .taxRefund(findTaxRefund(procurementId))                 // 待实现
                .salesRecord(findSalesRecord(procurementId))             // 待实现
                .build();
    }
}
```

### 4.3 VO 结构

```java
@Data
@Builder
public class OrderOverviewVO {
    private Long procurementId;
    private ProcurementVO procurement;          // 必定非空
    private ReplenishmentDemandVO demand;       // 可能为空
    private FactoryVO factory;                   // 可能为空
    private QcRecordVO qcRecord;                // 可能为空
    private LogisticsPlanVO logisticsPlan;     // 可能为空
    private DomesticCustomsVO domesticCustoms; // 可能为空（待实现）
    private JapanCustomsVO japanCustoms;        // 可能为空（待实现）
    private TaxRefundVO taxRefund;              // 可能为空（待实现）
    private SalesRecordVO salesRecord;          // 可能为空（待实现）
}
```

---

## 5. 前端集成

### 5.1 API 客户端

```typescript
// apps/web/src/api/orderOverview.ts
import { client } from './client'

export interface OrderOverviewVO {
  procurementId: number
  procurement: ProcurementVO
  demand?: ReplenishmentDemandVO
  factory?: FactoryVO
  qcRecord?: QcRecordVO
  logisticsPlan?: LogisticsPlanVO
  domesticCustoms?: DomesticCustomsVO     // 待实现
  japanCustoms?: JapanCustomsVO           // 待实现
  taxRefund?: TaxRefundVO                 // 待实现
  salesRecord?: SalesRecordVO             // 待实现
}

export const orderOverviewApi = {
  getOverview(procurementId: number) {
    return client.get<OrderOverviewVO>(`/orders/${procurementId}/overview`)
  }
}
```

### 5.2 composable

```typescript
// apps/web/src/pages/procurement/useOrderOverview.ts
import { ref, computed } from 'vue'
import { orderOverviewApi, type OrderOverviewVO } from '@/api/orderOverview'

export function useOrderOverview(procurementId: Ref<number>) {
  const overview = ref<OrderOverviewVO | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const fetch = async () => {
    loading.value = true
    error.value = null
    try {
      overview.value = await orderOverviewApi.getOverview(procurementId.value)
    } catch (e: any) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  const currentStep = computed(() => {
    if (!overview.value) return 0
    // 计算当前步骤（0-8）
  })

  return { overview, loading, error, currentStep, fetch }
}
```

---

## 6. 缺口与TODO

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 聚合接口实现 | P0 | GET `/api/v1/orders/{id}/overview` 后端实现 |
| 步骤5-8 填充 | P1 | DomesticCustoms/JapanCustoms/TaxRefund/SalesRecord 实现后填充 |
| 前端页面实现 | P1 | `OrderOverviewPage.vue` + 各步骤卡片组件 |
| 路由注册 | P1 | `/procurement/overview/:id` 注册到 router |
| 列表页入口 | P2 | Procurement/Inspection/Logistics 列表页「总览」按钮 |
