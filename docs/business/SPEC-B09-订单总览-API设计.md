# 订单总览 — API 设计

> **版本**: 3.3.0
> **创建**: 2026-04-22
> **更新**: 2026-05-13（v3.3.0：补充 Procurement/Demand 双锚点4个端点；OrderOverviewController.java 实际无语法错误）
> **更新**: 2026-05-08（v3.2.0：TypeScript接口和JSON示例移除不存在的step5-8Status字段）
> **更新**: 2026-05-07（v3.1.0：OrderChainVO字段修正；productNameZh嵌套SnapshotVO；仅step1-4Status）
> **状态**: ✅ 已实现（v3.0.0 单视图架构 + OrderOverviewPage.vue）
> **对应前端**: `OrderOverviewPage.vue` · `docs/ui/pages/09-order-overview.md`
> **对应 DB 文档**: `docs/database/DB-09-order-overview.md`
> **核心**: 单视图架构 — Demand 锚点（每行 = 一个子货号，从步骤1到步骤8的完整链路，通过 v_order_chain 视图暴露）

---

## 1. 设计背景（v3.0.0）

**问题（v2.x 双 Tab 架构）**：
- `/base/overview` 分"需求单"和"发注单"两个 Tab，本质上是同一个订单在不同阶段的状态
- 用户困惑：为什么同一个订单要切换两个 Tab 才能看到全貌？
- 状态不统一：Procurement 锚点和 Demand 锚点数据分离，维护两套 API

**解决方案（v3.0.0）**：
- **一张单子 = replenishment_demand 为锚点，逐步 LEFT JOIN 串联全链路8步**
- 单一列表视图，一行 = 一个 Demand，显示8步完成状态
- 点击行进入详情，展示该 Demand 对应全链路8步数据

**数据聚合关系**：

```
replenishment_demand (锚点)
  └── linked_procurement_id → procurement.id
                              ├── factory_id → factory.id
                              ├── procurement_id → qc_record.id（1:1）
                              ├── qc_record.id → logistics_plan.qc_record_id（1:1）
                              │                  ├── logistics_plan_id → domestic_customs_record.id
                              │                  └── logistics_plan_id → japan_customs_record.id
                              ├── procurement_id → tax_refund_record.id
                              └── procurement_id → sales_record.id
```

---

## 2. 核心接口（双锚点）

`OrderOverviewController.java` 暴露 **6 个端点**，分属两个架构入口：

| 锚点 | 端点 | 用途 |
|------|------|------|
| **Procurement 锚点** | `GET /api/v1/orders/procurement/{id}/overview` | 以发注单为锚，聚合全链路 |
| **Procurement 锚点** | `GET /api/v1/orders/procurement/selector` | 发注单下拉选择器 |
| **Demand 锚点** | `GET /api/v1/orders/demands` | 需求单下拉选择器 |
| **Demand 锚点** | `GET /api/v1/orders/demands/{id}/overview` | 以需求单为锚，聚合全链路（v1.6.0） |
| **v_order_chain 视图** | `GET /api/v1/orders/chain` | 订单总览列表（Demand 行） |
| **v_order_chain 视图** | `GET /api/v1/orders/chain/{demandId}` | 订单总览详情（全链路） |

> **v3.0.0 vs v1.6.0**：原 `OrderOverviewPageVO`（Procurement 锚点）已降级为内部 DTO；v1.6.0 新增 `DemandOverviewVO`（Demand 锚点）作为 `/demands/{id}/overview` 的精确响应类型。

### 2.1 Procurement 锚点 — `/api/v1/orders/procurement/{procurementId}/overview`

以 `Procurement.id` 为锚点，聚合全链路 8 步数据。

**路径参数**：`procurementId` — Procurement.id

**权限**：`procurement:read`

**响应 200**：`OrderOverviewPageVO`

```json
{
  "procurementId": 1,
  "procurement": {
    "id": 1,
    "procurementCode": "PC-2026-001",
    "factoryId": 10,
    "factoryName": "温州XX工厂",
    "productCode": "ad009",
    "subProductCode": "ad009-be",
    "quantity": 100,
    "destination": "久留米",
    "status": "CONFIRMED",
    "stepStatuses": [
      { "step": 1, "status": "COMPLETED" },
      { "step": 2, "status": "COMPLETED" },
      { "step": 3, "status": "IN_PROGRESS" },
      ...
    ]
  },
  "demand": { "id": 5, "demandCode": "DM-20260401-001", ... },
  "factory": { "id": 10, "factoryName": "温州XX工厂", ... },
  "qcRecord": { "id": 3, "result": "PASS", ... },
  "logisticsPlan": { "id": 2, "status": "PLANNED", ... },
  "domesticCustoms": null,
  "japanCustoms": null,
  "taxRefund": null,
  "salesRecord": null
}
```

### 2.2 Procurement 选择器 — `/api/v1/orders/procurement/selector`

发注单下拉选择器（分页列表，排除已删除）。

**权限**：`procurement:read`

**Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 0 |
| `pageSize` | int | 否 | 每页，默认 20，上限 100 |
| `keyword` | string | 否 | 搜索关键词 |

**响应 200**：`Page<OrderProcurementSelectorDTO>`

```json
{
  "content": [
    {
      "id": 1,
      "factoryName": "温州XX工厂",
      "productCode": "ad009",
      "subProductCode": "ad009-be",
      "quantity": 100,
      "priceRmb": 5000.00,
      "exchangeRate": 21.5,
      "destination": "久留米",
      "status": "CONFIRMED",
      "createTime": "2026-04-01T10:00:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 3
}
```

### 2.3 Demand 选择器 — `/api/v1/orders/demands`

需求单下拉选择器（分页列表，排除已删除）。

**权限**：`procurement:read`

**Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 0 |
| `pageSize` | int | 否 | 每页，默认 20，上限 100 |
| `status` | string | 否 | 需求单状态：PENDING / CONVERTED / CANCELLED |
| `keyword` | string | 否 | 搜索关键词（需求单号/子货号/目的地） |

**响应 200**：`Page<OrderDemandSelectorDTO>`

```json
{
  "content": [
    {
      "id": 5,
      "demandCode": "DM-20260401-001",
      "demandType": "REPLENISHMENT",
      "productCode": "ad009",
      "subProductCode": "ad009-be",
      "quantity": 100,
      "destination": "久留米",
      "japanLead": "田中",
      "status": "PENDING",
      "createTime": "2026-04-01T09:00:00"
    }
  ],
  "totalElements": 30,
  "totalPages": 2
}
```

### 2.4 Demand 锚点 — `/api/v1/orders/demands/{demandId}/overview`

以 `ReplenishmentDemand.id` 为锚点，聚合全链路数据（v1.6.0，替代 `OrderOverviewPageVO`）。

**路径参数**：`demandId` — ReplenishmentDemand.id

**权限**：`procurement:read`

**响应 200**：`DemandOverviewVO`

```json
{
  "demandId": 5,
  "demand": {
    "id": 5,
    "demandCode": "DM-20260401-001",
    "demandType": "REPLENISHMENT",
    "productCode": "ad009",
    "subProductCode": "ad009-be",
    "quantity": 100,
    "destination": "久留米",
    "japanLead": "田中",
    "status": "PENDING",
    "createTime": "2026-04-01T09:00:00"
  },
  "stepStatuses": ["COMPLETED", "NOT_STARTED", "NOT_STARTED", "NOT_STARTED", "NOT_STARTED", "NOT_STARTED", "NOT_STARTED", "NOT_STARTED"]
}
```

> **v1.6.0 说明**：`DemandOverviewVO` 与 `OrderOverviewPageVO` 的区别在于锚点不同，前者 Step1 必定有数据，Step2-8 固定为 `NOT_STARTED`（尚未转换到发注）。

---

### 2.5 列表接口 — `/api/v1/orders/chain`

**Query 参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 0 |
| `pageSize` | int | 否 | 每页条数，默认 20 |
| `demandStatus` | string | 否 | 筛选 Demand 状态：PENDING / CONFIRMED |
| `keyword` | string | 否 | 搜索关键词（需求单号/子货号/目的地） |

**响应 200**：`Page<OrderChainVO>`

```json
{
  "content": [
    {
      "demandId": 5,
      "demandCode": "DM-20260401-001",
      "demandType": "REPLENISHMENT",
      "demandProductCode": "ad009",
      "demandSubProductCode": "ad009-be",
      "demandQuantity": 100,
      "demandDestination": "久留米",
      "demandJapanLead": "田中",
      "demandStatus": "CONFIRMED",
      "linkedProcurementId": 1,
      "productNameZh": "木制椅子",
      "productCategory": "OEM",
      "step1Status": "COMPLETED",
      "step2Status": "COMPLETED",
      "step3Status": "COMPLETED",
      "step4Status": "NOT_STARTED"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

> **step1-step8 状态说明**：
> - `COMPLETED`：该步骤已有数据（LEFT JOIN 结果非 NULL）
> - `NOT_STARTED`：该步骤尚无数据（LEFT JOIN 结果为 NULL）

### 2.6 详情接口 — `/api/v1/orders/chain/{demandId}`

**路径参数**：`demandId` — ReplenishmentDemand.id

**响应 200**：`OrderChainDetailVO`

```json
{
  "demandId": 5,
  "demand": { ... },
  "procurement": { ... },
  "factory": { ... },
  "qcRecord": { ... },
  "logisticsPlan": { ... },
  "domesticCustoms": { ... },
  "japanCustoms": { ... },
  "taxRefund": { ... },
  "salesRecord": { ... }
}
```

**响应 404**：`ReplenishmentDemand` 不存在或已删除

---

## 3. 数据模型

### 3.1 OrderOverviewPageVO（Procurement 锚点，8步聚合）

> ⚠️ **v1.6.0 降级**：此 VO 仍被 `OrderOverviewUseCase` 使用，但 `/demands/{id}/overview` 已改用更精确的 `DemandOverviewVO`。

| 字段 | 类型 | 说明 |
|------|------|------|
| `procurementId` | Long | 发注单 ID（锚点） |
| `procurement` | ProcurementVO | 发注单数据 |
| `factory` | FactoryVO | 关联工厂 |
| `demand` | DemandVO | 关联需求单（via linked_procurement_id） |
| `qcRecord` | QcRecordVO | 步骤3 |
| `logisticsPlan` | LogisticsPlanVO | 步骤4 |
| `domesticCustoms` | DomesticCustomsVO | 步骤5 |
| `japanCustoms` | JapanCustomsVO | 步骤6 |
| `taxRefund` | TaxRefundVO | 步骤7 |
| `salesRecord` | SalesRecordVO | 步骤8 |
| `stepStatuses` | StepStatus[] | 8步进度枚举数组 |

### 3.2 OrderProcurementSelectorDTO（Procurement 选择器行）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 发注单 ID |
| `factoryName` | String | 工厂名称 |
| `productCode` | String | 主货号 |
| `subProductCode` | String | 子货号 |
| `quantity` | Integer | 数量 |
| `destination` | String | 目的地 |
| `status` | String | 状态 |
| `createTime` | LocalDateTime | 创建时间 |

### 3.3 OrderDemandSelectorDTO（Demand 选择器行）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 需求单 ID |
| `demandCode` | String | 需求单号 |
| `demandType` | String | REPLENISHMENT / NEW_PURCHASE |
| `productCode` | String | 主货号 |
| `subProductCode` | String | 子货号 |
| `quantity` | Integer | 需求数量 |
| `destination` | String | 目的地 |
| `japanLead` | String | 日本担当 |
| `status` | String | PENDING / CONVERTED / CANCELLED |
| `createTime` | LocalDateTime | 创建时间 |

### 3.4 DemandOverviewVO（Demand 锚点，v1.6.0）

> **替代 `OrderOverviewPageVO`**：精确描述 Demand 锚点，Step1=COMPLETED，Step2-8=NOT_STARTED。

| 字段 | 类型 | 说明 |
|------|------|------|
| `demandId` | Long | 需求单 ID |
| `demand` | DemandVO | 步骤1数据（必定有） |
| `stepStatuses` | StepStatus[] | 固定 [COMPLETED, NOT_STARTED×7] |

### 3.5 OrderChainVO（列表行）

| 字段 | 类型 | 说明 |
|------|------|------|
| `demandId` | Long | 需求单 ID |
| `demandCode` | String | 需求单号 |
| `demandType` | String | REPLENISHMENT / NEW_PURCHASE |
| `demandProductCode` | String | 主货号 |
| `demandSubProductCode` | String | 子货号全码 |
| `demandQuantity` | Integer | 需求数量 |
| `demandDestination` | String | 目的地 |
| `demandJapanLead` | String | 日本担当 |
| `demandStatus` | String | PENDING / CONFIRMED |
| `linkedProcurementId` | Long | 关联的发注单 ID |
| `demandImageUrl` | String | ⚠️ 文档未列，Entity有 |
| `demandCreateTime` | LocalDateTime | ⚠️ 文档未列，Entity有 |
| `demandUpdateTime` | LocalDateTime | ⚠️ 文档未列，Entity有 |
| `snapshot` | SnapshotVO ⚠️ | ⚠️ productNameZh/category嵌套在此，非直接字段 |
| `step1Status` | String | COMPLETED / NOT_STARTED |
| `step2Status` | String | COMPLETED / NOT_STARTED |
| `step3Status` | String | COMPLETED / NOT_STARTED |
| `step4Status` | String | COMPLETED / NOT_STARTED |

> ⚠️ **v3.1.0 修正**：
> - `productNameZh`/`productCategory` 嵌套在 `SnapshotVO` 内，非直接字段
> - 仅 `step1Status`~`step4Status`（无 step5~step8Status）
> - `demandImageUrl`/`demandCreateTime`/`demandUpdateTime` 文档未列但 Entity 有

### 3.6 OrderChainDetailVO（详情）

| 字段 | 类型 | 说明 |
|------|------|------|
| `demandId` | Long | 需求单 ID |
| `demand` | ReplenishmentDemandVO | 步骤1数据（必定有） |
| `procurement` | ProcurementVO | 步骤2数据（可能为 null） |
| `factory` | FactoryVO | 步骤2关联工厂（可能为 null） |
| `qcRecord` | QcRecordVO | 步骤3数据（可能为 null） |
| `logisticsPlan` | LogisticsPlanVO | 步骤4数据（可能为 null） |
| `domesticCustoms` | DomesticCustomsVO | 步骤5数据（可能为 null） |
| `japanCustoms` | JapanCustomsVO | 步骤6数据（可能为 null） |
| `taxRefund` | TaxRefundVO | 步骤7数据（可能为 null） |
| `salesRecord` | SalesRecordVO | 步骤8数据（可能为 null） |

---

## 4. 后端实现

### 4.1 Repository — v_order_chain 视图

```java
@Repository
public interface OrderChainViewRepository extends JpaRepository<OrderChainView, Long> {

    // 列表查询（支持分页 + 状态筛选 + 关键词搜索）
    @Query("""
        SELECT v FROM OrderChainView v
        WHERE (:demandStatus IS NULL OR v.demandStatus = :demandStatus)
          AND (:keyword IS NULL
               OR v.demandCode LIKE %:keyword%
               OR v.demandSubProductCode LIKE %:keyword%
               OR v.demandDestination LIKE %:keyword%)
        ORDER BY v.demandCreateTime DESC
        """)
    Page<OrderChainView> findChainList(
        @Param("demandStatus") String demandStatus,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    // 详情查询
    Optional<OrderChainView> findByDemandId(Long demandId);
}
```

### 4.2 UseCase（双架构）

```java
// Procurement 锚点
@Service
@RequiredArgsConstructor
public class OrderOverviewUseCase {
    public OrderOverviewPageVO getOverview(Long procurementId) { ... }
    public DemandOverviewVO getDemandOverview(Long demandId) { ... }
    public OrderDemandSelectorDTO toDemandSelectorDto(ReplenishmentDemand demand) { ... }
}

// Demand 锚点（v_order_chain 视图）
@Service
@RequiredArgsConstructor
public class OrderChainUseCase {

    private final OrderChainViewRepository chainViewRepository;

    @Transactional(readOnly = true)
    public Page<OrderChainVO> getChainList(String demandStatus, String keyword, Pageable pageable) {
        return chainViewRepository.findChainList(demandStatus, keyword, pageable)
            .map(this::toChainVO);
    }

    @Transactional(readOnly = true)
    public OrderChainDetailVO getChainDetail(Long demandId) {
        OrderChainView view = chainViewRepository.findByDemandId(demandId)
            .orElseThrow(() -> BusinessException.notFound("Demand", demandId));
        return toDetailVO(view);
    }
}
```

---

## 5. 前端集成

### 5.1 API 客户端

```typescript
// apps/web/src/api/orderChain.ts
import { client } from './client'

export interface OrderChainVO {
  demandId: number
  demandCode: string
  demandType: string
  demandProductCode: string
  demandSubProductCode: string
  demandQuantity: number
  demandDestination: string
  demandJapanLead: string
  demandStatus: string
  linkedProcurementId: number | null
  productNameZh: string
  productCategory: string
  step1Status: string
  step2Status: string
  step3Status: string
  step4Status: string
}

export interface OrderChainDetailVO {
  demandId: number
  demand: ReplenishmentDemandVO
  procurement: ProcurementVO | null
  factory: FactoryVO | null
  qcRecord: QcRecordVO | null
  logisticsPlan: LogisticsPlanVO | null
  domesticCustoms: DomesticCustomsVO | null
  japanCustoms: JapanCustomsVO | null
  taxRefund: TaxRefundVO | null
  salesRecord: SalesRecordVO | null
}

export const orderChainApi = {
  // Procurement 锚点
  getProcurementOverview(procurementId: number) {
    return client.get<OrderOverviewPageVO>(`/orders/procurement/${procurementId}/overview`)
  },
  getProcurementSelector(params: { page?: number, pageSize?: number, keyword?: string }) {
    return client.get<Page<OrderProcurementSelectorDTO>>('/orders/procurement/selector', { params })
  },
  // Demand 锚点
  getDemands(params: { page?: number, pageSize?: number, status?: string, keyword?: string }) {
    return client.get<Page<OrderDemandSelectorDTO>>('/orders/demands', { params })
  },
  getDemandOverview(demandId: number) {
    return client.get<DemandOverviewVO>(`/orders/demands/${demandId}/overview`)
  },
  // v_order_chain 视图
  getChainList(params: { page?: number, pageSize?: number, demandStatus?: string, keyword?: string }) {
    return client.get<Page<OrderChainVO>>('/orders/chain', { params })
  },
  getChainDetail(demandId: number) {
    return client.get<OrderChainDetailVO>(`/orders/chain/${demandId}`)
  }
}
```

---

## 6. 分阶段实现计划

### Phase 1：步骤 1~4（补货 → 发注 → 验货 → 调配）

| 项目 | 状态 | 说明 |
|------|------|------|
| `v_order_chain_v1` MySQL VIEW | ✅ 已建 | 视图包含 Demand/Procurement/Factory/QcRecord/LogisticsPlan |
| `OrderChainView` JPA Entity | ✅ 已建 | 映射 `v_order_chain_v1` |
| `OrderChainRepository` | ✅ 已建 | JPA Repository |
| `GET /api/v1/orders/chain` 列表接口 | ✅ 已建 | 分页 + 筛选 |
| `GET /api/v1/orders/chain/{demandId}` 详情接口 | ✅ 已建 | 步骤1~4数据 |
| `OrderChainVO` / `OrderChainDetailVO` | ✅ 已建 | 响应对象 |
| `OrderOverviewUseCase`（Procurement 锚点） | ✅ 已建 | `OrderOverviewController` 注入 |
| `GET /api/v1/orders/procurement/selector` | ✅ 已建 | 发注单选择器 |
| `GET /api/v1/orders/procurement/{id}/overview` | ✅ 已建 | Procurement 锚点全链路 |
| `GET /api/v1/orders/demands` | ✅ 已建 | 需求单选择器 |
| `GET /api/v1/orders/demands/{id}/overview`（v1.6.0） | ✅ 已建 | Demand 锚点全链路 |
| 前端 API 层 `orderChain.ts` | 🔲 待建 | |

> **Phase 1 scope**：Procurement/QcRecord/LogisticsPlan/DomesticCustoms/JapanCustoms/TaxRefund/SalesRecord 字段返回 NULL 占位。

### Phase 2：步骤 5~8（国内报关 → 日本清关 → 退税 → 运营销售）

| 项目 | 状态 |
|------|------|
| VIEW 升级为完整 8 步 | 🔲 待建 |
| 步骤 5~8 字段补全 | 🔲 待建 |
| 详情抽屉全 8 步展示 | 🔲 待改 |
