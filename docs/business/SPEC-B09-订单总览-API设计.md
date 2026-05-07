# 订单总览 — API 设计

> **版本**: 3.0.0
> **创建**: 2026-04-22
> **更新**: 2026-04-28（v3.0.0：彻底移除双 Tab 架构，改为单一 v_order_chain 视图；一行 = 一个 Demand，LEFT JOIN 串联全链路8步）
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

## 2. 核心接口

### 2.1 列表接口 — `/api/v1/orders/chain`

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
      "step4Status": "NOT_STARTED",
      "step5Status": "NOT_STARTED",
      "step6Status": "NOT_STARTED",
      "step7Status": "NOT_STARTED",
      "step8Status": "NOT_STARTED"
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

### 2.2 详情接口 — `/api/v1/orders/chain/{demandId}`

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

### 3.1 OrderChainVO（列表行）

| 字段 | 类型 | 来源表 | 说明 |
|------|------|--------|------|
| `demandId` | Long | replenishment_demand | 需求单 ID |
| `demandCode` | String | replenishment_demand | 需求单号 |
| `demandType` | String | replenishment_demand | REPLENISHMENT / NEW_PURCHASE |
| `demandProductCode` | String | replenishment_demand | 主货号 |
| `demandSubProductCode` | String | replenishment_demand | 子货号全码 |
| `demandQuantity` | Integer | replenishment_demand | 需求数量 |
| `demandDestination` | String | replenishment_demand | 目的地 |
| `demandJapanLead` | String | replenishment_demand | 日本担当 |
| `demandStatus` | String | replenishment_demand | PENDING / CONFIRMED |
| `linkedProcurementId` | Long | replenishment_demand | 关联的发注单 ID |
| `productNameZh` | String | product | 商品中文名 |
| `productCategory` | String | product | OEM / ORDINARY / FACTORY_DIRECT |
| `step1Status` | String | computed | COMPLETED / NOT_STARTED |
| `step2Status` | String | computed | COMPLETED / NOT_STARTED |
| `step3Status` | String | computed | COMPLETED / NOT_STARTED |
| `step4Status` | String | computed | COMPLETED / NOT_STARTED |
| `step5Status` | String | computed | COMPLETED / NOT_STARTED |
| `step6Status` | String | computed | COMPLETED / NOT_STARTED |
| `step7Status` | String | computed | COMPLETED / NOT_STARTED |
| `step8Status` | String | computed | COMPLETED / NOT_STARTED |

### 3.2 OrderChainDetailVO（详情）

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

### 4.2 UseCase

```java
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
  step5Status: string
  step6Status: string
  step7Status: string
  step8Status: string
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
| 前端 API 层 `orderChain.ts` | 🔲 待建 | |
| 前端 `OrderOverviewPage.vue` 单列表 | 🔲 待改 | 移除双 Tab，改为单列表 |

> **Phase 1 scope**：Procurement/QcRecord/LogisticsPlan/DomesticCustoms/JapanCustoms/TaxRefund/SalesRecord 字段返回 NULL 占位。

### Phase 2：步骤 5~8（国内报关 → 日本清关 → 退税 → 运营销售）

| 项目 | 状态 |
|------|------|
| VIEW 升级为完整 8 步 | 🔲 待建 |
| 步骤 5~8 字段补全 | 🔲 待建 |
| 详情抽屉全 8 步展示 | 🔲 待改 |
