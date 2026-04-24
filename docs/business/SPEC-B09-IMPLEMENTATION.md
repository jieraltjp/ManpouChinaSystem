# SPEC-B09 — 订单总览 · 实现设计

> **版本**: 1.3.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v1.3.0：双入口架构 — Demand 中心化，新建需求单立即出现在 Overview）
> **状态**: 🟢 已完成
> **前置**: SPEC-B09-API设计 · DB-09 · docs/ui/pages/09-order-overview.md · SPEC-B09-全链路子货号追踪分析.md
> **INTJ 编号**: DOC-B09-IMPL-001

---

## 1. INTJ 干跑

**本质（一句话）：** Overview 页面以 Demand 为入口，新建需求单立即可见；Procurement 为业务链锚点，聚合全链路 8 步数据。

**双入口设计**：
- 入口 A：需求单（`/demands`）→ Demand 锚点 → Step1 有数据，Step2-8 待填 → 支持转采购
- 入口 B：发注单（`/procurement/selector`）→ Procurement 锚点 → 8 步全链路聚合

**三行推演：**
1. 用户在 DemandPage 创建需求单 → 立即出现在 Overview 需求单 Tab
2. 用户点进 Demand 详情 → 看到 Step1 数据，支持转采购
3. 转采购后 → Procurement 出现在发注单 Tab，对应 Procurement 详情有完整 8 步

**禁止项：**
- 禁止前端独立查 8 个接口凑数据（网络次数 × 8 = 灾难）
- 禁止后端 UseCase 混入视图逻辑（状态颜色计算在前端）
- 禁止步骤间直接修改上游数据（单向流约束）

---

## 2. 模块拓扑

```
后端
├── order/
│   ├── application/
│   │   ├── dto/
│   │   │   ├── OrderOverviewCmd.java        # 采购单选择参数
│   │   │   └── OrderOverviewPageVO.java    # 聚合响应（含8步子VO）
│   │   ├── assembler/
│   │   │   └── OrderOverviewAssembler.java  # 实体 → VO 转换
│   │   └── usecase/
│   │       └── OrderOverviewUseCase.java    # 聚合查询编排
│   └── interfaces/
│       └── controller/
│           └── OrderOverviewController.java # GET /api/v1/orders/{id}/overview
│                                              GET /api/v1/orders/selector（采购单列表）
└── （复用已有 Repository）
    ├── ProcurementRepository
    ├── ReplenishmentDemandRepository
    ├── QcRecordRepository
    ├── LogisticsPlanRepository
    ├── FactoryRepository
    ├── DomesticCustomsRepository
    ├── JapanCustomsRepository
    ├── TaxRefundRepository
    └── SalesRepository

前端
├── api/
│   └── orderOverview.ts              # API 客户端 + 类型（含 DemandSelectorVO / ProcurementSelectorVO）
└── pages/
    └── procurement/
        ├── OrderOverviewPage.vue     # 列表页（Demands Tab + Procurements Tab）
        ├── DemandOverviewPage.vue     # Demand 详情（Step1 有数据，Step2-8 未开始，支持转采购）
        └── ProcurementOverviewPage.vue # Procurement 详情（8 步全链路聚合）
```

---

## 3. 数据模型

### 3.1 顶层 VO — OrderOverviewPageVO

```java
public class OrderOverviewPageVO {

    // ===== 锚点（必定非空）=====
    private Long procurementId;
    private ProcurementVO procurement;
    private FactoryVO factory;          // 通过 factoryId 补全

    // ===== 步骤1-8（可能为空）=====
    private DemandVO demand;            // linked_procurement_id → demand
    private QcRecordVO qcRecord;       // procurement_id → qc_record
    private LogisticsPlanVO logisticsPlan;  // procurement_id → logistics_plan
    private DomesticCustomsVO domesticCustoms;  // procurement_id → domestic_customs_record
    private JapanCustomsVO japanCustoms;       // procurement_id → japan_customs_record
    private TaxRefundVO taxRefund;           // procurement_id → tax_refund_record
    private SalesRecordVO salesRecord;       // procurement_id → sales_record

    // ===== 计算属性（前端用于进度条）=====
    private StepStatus[] stepStatuses;  // 长度8，值：NOT_STARTED / IN_PROGRESS / COMPLETED
}
```

### 3.2 DemandVO（v1.6.0 适配）

```java
// DemandVO — v1.6.0 不再返回 quantity/destination
// 改为 subProductItemsSummary: "be:100久留米, bu:50名古屋"
public static class DemandVO {
    private Long id;
    private String demandCode;
    private String demandType;           // REPLENISHMENT / NEW_PURCHASE
    private String productCode;
    private String subProductItemsSummary; // v1.6.0: "be:100久留米, bu:50名古屋" 或 null
    private String japanLead;
    private String status;
    private LocalDateTime createTime;
}
```

> **v1.6.0 背景**：ReplenishmentDemand.subProductItemsRaw 从单字段改为 JSON 数组，
> 前端 Step1 卡片通过 `subProductItemsSummary` 展示（而非单独 quantity/destination）。

### 3.3 StepStatus 枚举（前端用）

```typescript
// 前端用，不与后端共享
type StepStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

// 进度条颜色映射
const STEP_COLORS = {
  NOT_STARTED:  '#909399',   // 灰色
  IN_PROGRESS:   '#E6A23C',   // 橙色
  COMPLETED:     '#67C23A',   // 绿色（终态时）
}
```

### 3.3 步骤状态计算规则

| 步骤 | 完成条件 | 进行中条件 |
|------|---------|-----------|
| 1 补货 | demand != null | — |
| 2 发注 | procurement != null | — |
| 3 验货 | qcRecord.status == COMPLETED | qcRecord != null && status != COMPLETED |
| 4 调配 | logisticsPlan.status == DELIVERED | logisticsPlan != null && status != DELIVERED |
| 5 国报 | domesticCustoms.status == CLEARED | domesticCustoms != null && status != CLEARED |
| 6 日报 | japanCustoms.status == CLEARED | japanCustoms != null && status != CLEARED |
| 7 退税 | taxRefund.status == COMPLETED | taxRefund != null && status != COMPLETED |
| 8 运营 | salesRecord.status == DISCONTINUED | salesRecord != null |

> 规则在后端计算，前端只做渲染（防腐：后端不感知前端颜色逻辑）

---

## 4. API 契约

### 4.1 获取订单总览

```
GET /api/v1/orders/{procurementId}/overview
```

**响应 200**：
```json
{
  "code": "0",
  "data": {
    "procurementId": 1,
    "procurement": { "id": 1, "procurementCode": "P-20260421-001", ... },
    "factory": { "id": 10, "factoryName": "义乌XX家具厂", ... },
    "demand": null,
    "qcRecord": null,
    "logisticsPlan": null,
    "domesticCustoms": null,
    "japanCustoms": null,
    "taxRefund": null,
    "salesRecord": null,
    "stepStatuses": [
      "COMPLETED", "COMPLETED", "NOT_STARTED", "NOT_STARTED",
      "NOT_STARTED", "NOT_STARTED", "NOT_STARTED", "NOT_STARTED"
    ]
  }
}
```

**响应 404**：`Procurement` 不存在或已删除

### 4.2 采购单选择器（列表）

```
GET /api/v1/orders/selector?page=0&pageSize=20&keyword=
```

> 复用已有 `ProcurementUseCase.pageQuery()`，Controller 独立出口

---

## 5. 后端实现

### 5.1 Controller

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOverviewController {

    private final OrderOverviewUseCase orderOverviewUseCase;
    private final ProcurementUseCase procurementUseCase;  // 复用选择器

    @GetMapping("/{procurementId}/overview")
    public Result<OrderOverviewPageVO> getOverview(@PathVariable Long procurementId) {
        return Result.ok(orderOverviewUseCase.getOverview(procurementId));
    }

    @GetMapping("/selector")
    public Result<Page<ProcurementPageVO>> selector(ProcurementQuery query) {
        return Result.ok(procurementUseCase.pageQuery(query));
    }
}
```

### 5.2 UseCase（聚合编排）

```java
@Service
@RequiredArgsConstructor
public class OrderOverviewUseCase {

    private final ProcurementRepository procurementRepository;
    private final ProcurementAssembler procurementAssembler;
    private final FactoryRepository factoryRepository;
    private final FactoryAssembler factoryAssembler;
    private final ReplenishmentDemandRepository demandRepository;
    private final QcRecordRepository qcRecordRepository;
    private final LogisticsPlanRepository logisticsPlanRepository;
    private final DomesticCustomsRepository domesticCustomsRepository;
    private final JapanCustomsRepository japanCustomsRepository;
    private final TaxRefundRepository taxRefundRepository;
    private final SalesRepository salesRepository;
    private final OrderOverviewAssembler assembler;

    @Transactional(readOnly = true)
    public OrderOverviewPageVO getOverview(Long procurementId) {
        Procurement procurement = procurementRepository
                .findByIdAndIsDeletedFalse(procurementId)
                .orElseThrow(() -> BusinessException.notFound("Procurement", procurementId));

        // 锚点
        ProcurementVO procurementVO = procurementAssembler.toOverviewVO(procurement);

        // 工厂（通过 factoryId 补全 factoryName）
        FactoryVO factoryVO = procurement.getFactoryId() != null
                ? factoryRepository.findByIdAndIsDeletedFalse(procurement.getFactoryId())
                        .map(factoryAssembler::toOverviewVO).orElse(null)
                : null;

        // 步骤1：补货需求（通过 linked_procurement_id 关联）
        DemandVO demandVO = demandRepository
                .findByLinkedProcurementIdAndIsDeletedFalse(procurementId)
                .map(demand -> assembler.toDemandVO(demand)).orElse(null);

        // 步骤3：验货记录
        QcRecordVO qcVO = qcRecordRepository
                .findFirstByProcurementIdAndIsDeletedFalseOrderByCreateTimeDesc(procurementId)
                .map(qc -> assembler.toQcVO(qc)).orElse(null);

        // 步骤4：调配计划
        LogisticsPlanVO lpVO = logisticsPlanRepository
                .findFirstByProcurementIdAndIsDeletedFalseOrderByCreateTimeDesc(procurementId)
                .map(lp -> assembler.toLogisticsVO(lp)).orElse(null);

        // 步骤5：国内报关
        DomesticCustomsVO dcVO = domesticCustomsRepository
                .findByProcurementIdAndIsDeletedFalse(procurementId)
                .map(dc -> assembler.toDomesticCustomsVO(dc)).orElse(null);

        // 步骤6：日本清关
        JapanCustomsVO jpVO = japanCustomsRepository
                .findByProcurementIdAndIsDeletedFalse(procurementId)
                .map(jp -> assembler.toJapanCustomsVO(jp)).orElse(null);

        // 步骤7：退税
        TaxRefundVO trVO = taxRefundRepository
                .findByProcurementIdAndIsDeletedFalse(procurementId)
                .map(tr -> assembler.toTaxRefundVO(tr)).orElse(null);

        // 步骤8：运营销售
        SalesRecordVO srVO = salesRepository
                .findByProcurementIdAndIsDeletedFalse(procurementId)
                .map(sr -> assembler.toSalesVO(sr)).orElse(null);

        // 计算8步状态
        StepStatus[] stepStatuses = assembler.computeStepStatuses(
                procurement, demandVO, qcVO, lpVO, dcVO, jpVO, trVO, srVO);

        return OrderOverviewPageVO.builder()
                .procurementId(procurementId)
                .procurement(procurementVO)
                .factory(factoryVO)
                .demand(demandVO)
                .qcRecord(qcVO)
                .logisticsPlan(lpVO)
                .domesticCustoms(dcVO)
                .japanCustoms(jpVO)
                .taxRefund(trVO)
                .salesRecord(srVO)
                .stepStatuses(stepStatuses)
                .build();
    }
}
```

### 5.3 领域事件（无）

订单总览是只读聚合视图，不发布领域事件。

### 5.4 防腐边界

- `OrderOverviewAssembler` 只做类型映射，不含业务判断
- `ProcurementAssembler` 新增 `toOverviewVO()` 方法（只取总览所需字段子集）
- `FactoryAssembler` 新增 `toOverviewVO()` 方法

---

## 6. 前端实现

### 6.1 OrderOverviewPage.vue 容器

```
路由: /procurement/overview/:procurementId?
状态: procurementId（从 route params 或选择器获得）
```

**逻辑流程：**
```
有 procurementId → 调用 useOrderOverview(procurementId) → 渲染进度条 + 8个卡片
无 procurementId  → 渲染 OrderSelector（采购单列表）→ 选择后更新 route
```

**无ID时（选择器模式）：**
- 显示最近 20 条 Procurement 列表（日期倒序）
- 支持按货号/工厂名搜索
- 点击行 → `router.push('/procurement/overview/' + id)`

### 6.2 StatusProgressBar.vue

**Props**：`stepStatuses: StepStatus[]`（长度 8）

**渲染**：8 个圆圈 + 箭头连接，含当前步骤高亮

### 6.3 StepCard.vue（统一包装）

所有步骤卡片复用统一包装：
```
Props: stepNumber, title, icon, status, children
Events: onEdit, onCreate
```

| status | 边框颜色 | 右上角徽章 |
|--------|---------|-----------|
| NOT_STARTED | #DCDFE6 | — |
| IN_PROGRESS | #E6A23C | 进行中 |
| COMPLETED | #67C23A | 已完成 |

### 6.4 useOrderOverview.ts

```typescript
export function useOrderOverview(procurementId: Ref<number>) {
  const overview = ref<OrderOverviewPageVO | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetch() {
    loading.value = true
    error.value = null
    try {
      const res = await orderOverviewApi.getOverview(procurementId.value)
      overview.value = res.data.data
    } catch (e: any) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  const currentStep = computed(() => {
    if (!overview.value?.stepStatuses) return 0
    const s = overview.value.stepStatuses
    for (let i = 0; i < s.length; i++) {
      if (s[i] !== 'COMPLETED') return i + 1
    }
    return 8
  })

  watch(procurementId, fetch, { immediate: true })

  return { overview, loading, error, currentStep, fetch }
}
```

---

## 7. 实现顺序

### Phase 1 — 后端聚合（骨架，P0）
1. 新建 `order/` 模块（application + interfaces）
2. 实现 `OrderOverviewController`（2个端点）
3. 实现 `OrderOverviewUseCase`（步骤1-4，步骤5-8 返回 null）
4. 新增 `ProcurementAssembler.toOverviewVO()`、`FactoryAssembler.toOverviewVO()`
5. 新增 `ReplenishmentDemandRepository.findByLinkedProcurementIdAndIsDeletedFalse()`
6. 补全步骤5-8 Repository（已在 customs/logistics 模块定义，直接注入）

### Phase 2 — 前端骨架（P0）✅ 已完成
1. `api/orderOverview.ts`（类型 + API）
2. `OrderOverviewPage.vue`（选择器模式 + 详情模式，含内联逻辑）
3. `StatusProgressBar.vue`
4. 步骤1-4 卡片（对应已有聚合根）
5. 步骤5-8 卡片（DomesticCustoms/JapanCustoms/TaxRefund/SalesRecord 字段已完整）

### Phase 3 — 完善（P1）✅ 已完成
1. 步骤5-8 卡片填充（DomesticCustoms/JapanCustoms/TaxRefund/SalesRecord 字段已完整）
2. 各步骤编辑按钮跳转逻辑
3. 创建调配按钮（验货 PASS 且无 logisticsPlan 时显示）

### Phase 4 — 体验优化（P2）✅ 已完成
1. 列表页「总览」按钮（ProcurementPage / InspectionPage / LogisticsPage）

### Phase 5 — Demand 中心化（P0）✅ v1.3.0 已完成
1. `OrderDemandSelectorDTO.java`（新建 Demand 选择器 DTO）
2. Controller 新增 `/demands` selector + `/demands/{id}/overview` 端点
3. `OrderOverviewUseCase.getDemandOverview()`（Demand 锚点，step1=COMPLETED，step2-8=NOT_STARTED）
4. `OrderOverviewAssembler.computeDemandStepStatuses()`（Demand 专用状态数组）
5. 前端 `api/orderOverview.ts` 新增 `listDemandSelector` + `getDemandOverview` API
6. 路由改造：`/overview/demand/:demandId` + `/overview/procurement/:procurementId`
7. `OrderOverviewPage.vue` 改为双 Tab 列表（需求单 / 发注单）
8. 新建 `DemandOverviewPage.vue`（Step1 数据，支持转采购）
9. 新建 `ProcurementOverviewPage.vue`（8步全链路，迁自原 OrderOverviewPage 详情部分）

---

## 8. 缺口与依赖

| 项目 | 优先级 | 依赖 | 状态 |
|------|--------|------|------|
| Phase 1 后端 | P0 | 步骤5-8 Repository 已在 customs/logistics 定义 | 🟢 已完成 |
| 步骤5 DomesticCustoms 字段 | P1 | DB-05 · SPEC-B05 | 🟢 已完成 |
| 步骤6 JapanCustoms 字段 | P1 | DB-06 · SPEC-B06 | 🟢 已完成 |
| 步骤7 TaxRefund 字段 | P1 | DB-07 · SPEC-B07 | 🟢 已完成 |
| 步骤8 SalesRecord 字段 | P1 | DB-08 · SPEC-B08 | 🟢 已完成 |
| v1.6.0 前端 Step1 卡片修复 | P0 | DemandVO.subProductItemsSummary | 🟢 已完成 |
| Phase 5 Demand 中心化 | P0 | 双入口架构 | 🟢 已完成 |
| JapanCustomsRecord 补充 subProductCode | P2 | DB 迁移 + 实体 + Assembler | 🟡 可选 |

---

## 9. v1.3.0 双入口架构说明

> 详细分析见：`SPEC-B09-全链路子货号追踪分析.md`

### 核心变更

v1.3.0：Overview 从 Procurement 中心 → Demand 中心，支持双入口。

```
/base/overview                          → 双 Tab 列表（需求单 / 发注单）
/base/overview/demand/:demandId           → Demand 详情（Step1 有数据，Step2-8 未开始）
/base/overview/procurement/:procurementId → Procurement 详情（8 步全链路）
```

### 双入口数据流

```
新建需求单
    ↓
出现在 Overview → 需求单 Tab（立即可见）
    ↓
点击进入 Demand 详情 → Step1 有数据，支持"转采购"
    ↓
转采购成功 → 自动跳转到 Procurement 详情
           → 需求单 Tab 中该条变为 CONVERTED 状态
           → 发注单 Tab 中出现新的 Procurement 记录
```

### v1.6.0 子货号追踪（保留）

```
Demand → N×Procurement → N×DomesticCustoms → LogisticsPlan → JapanCustoms → SalesRecord
                  ↓
              N×SalesRecord（从 Procurement 直接触发）
```

| 步骤 | 实体 | 子货号精确 | 备注 |
|------|------|:---:|------|
| 需求单 | ReplenishmentDemand | ✅ | JSON 数组 |
| 发注单 | Procurement | ✅ | 每条 SubProductItem 一个 |
| 国内报关 | DomesticCustomsRecord | ✅ | 事件驱动自动 |
| 调配计划 | LogisticsPlan | ✅ | 可按批次合并 |
| 日本清关 | JapanCustomsRecord | ⚠️ | 无 subProductCode（可延后补充） |
| 退税 | TaxRefundRecord | — | 按采购单总额，维度不同 |
| 销售记录 | SalesRecord | ✅ | 事件驱动自动 |

---

## 10. INTJ 审计

| 判定 | 说明 |
|------|------|
| 防腐 | 后端 VO 不渗透到前端组件，前端用 `OrderOverviewPageVO` 经 `api/orderOverview.ts` 转换 |
| 依赖倒置 | UseCase 只依赖 Repository 接口，不直接操作 JPA |
| 单一职责 | Controller 只做路由分发，计算逻辑全在 UseCase |
| 不可变 | 前端 `overview` 用 `ref`，`fetch` 整体替换引用 |
| 熵减 | 步骤卡片统一用 `StepCard` 包装，消除重复 UI 代码 |
| 分型 | `useOrderOverview` 只管数据获取和计算，不感知 UI 渲染 |

---

*文档版本: 1.3.0 · 状态: 已完成 · 2026-04-24*
