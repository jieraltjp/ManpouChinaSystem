# SPEC-B01 — 补货需求业务规格（步骤1）

> **版本**: 2.0.0
> **更新**: 2026-04-24（v2.0.0：每条 Demand = 一个子货号（主货号+子货号 = 商品唯一标识））
> **更新**: 2026-04-24（v1.6.0：子货号明细表单 — 每个子货号独立数量+目的地；转采购改为批量模式）
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **业务步号**: 01（补货需求/新品采购）
> **对应 UI 文档**: `docs/ui/pages/01-demand.md`
> **对应数据库文档**: `docs/database/DB-01-procurement-demand.md`

---

## 1. 业务背景

补货需求是整个采购链路的入口。非新品走补货流程，新品走新品采购流程。录入后转为发注单（Procurement），进入步骤2。

**核心设计原则（v2.0.0）**：
- 商品唯一标识 = 主货号 + 子货号，如 `ad009-be`
- **每条 ReplenishmentDemand = 一个子货号**（不再用 JSON 数组存多个子货号）
- 转采购：1 Demand → 1 Procurement（一对一）

---

## 2. 聚合根

### ReplenishmentDemand（补货需求单，v2.0.0）

```
ReplenishmentDemand（聚合根）
├── id: Long
├── demandCode: String           # DM-YYYYMMDD-NNN
├── demandType: DemandType        # REPLENISHMENT(补货) / NEW_PURCHASE(新品采购)
├── productCode: String           # 主货号（如 ad009）
├── subProductCode: String        # 子货号全码（如 ad009-be，商品唯一标识）
├── quantity: Integer             # 该子货号的需求数量
├── destination: String           # 目的地（如久留米/名古屋）
├── japanLead: String             # 日本担当
├── status: DemandStatus          # PENDING → CONVERTED → CANCELLED
├── linkedProcurementId: Long     # 关联的 Procurement.id（CONVERTED 时填充）
└── 领域方法
    ├── markAsConverted(procurementId)  # 标记转采购
    └── revertConversion()              # 撤销转换（回滚 PENDING）
```

> **设计背景（v2.0.0）**：同一个主货号（如 ad009）有多个子货号（颜色 be/bu/re），每个子货号独立一条 Demand。
> 例：
> - Demand A: productCode=ad009, subProductCode=ad009-be, qty=100, destination=久留米
> - Demand B: productCode=ad009, subProductCode=ad009-bu, qty=50, destination=名古屋
> - Demand C: productCode=ad009, subProductCode=ad009-re, qty=75, destination=大阪
>
> `/base/overview` 中，需求单 Tab 每行代表一个子货号的完整链路（步骤1→步骤8）。

---

## 3. 枚举

```java
public enum DemandType {
    REPLENISHMENT,   // 非新品 — 补货
    NEW_PURCHASE      // 新品 — 采购
}

public enum DemandStatus {
    PENDING,     // 待确认（录入后默认）
    CONVERTED,  // 已转采购（生成 Procurement 后推进至此）
    CANCELLED   // 已取消
}
```

---

## 4. 状态流转

```
  PENDING ──[转采购]──▶ CONVERTED
     │
     └──[取消]──▶ CANCELLED
```

### 转采购语义（v2.0.0）

- **1 Demand → 1 Procurement（一对一）**
- 每个子货号的数量/目的地独立
- 撤销转换：删除关联 Procurement，回滚 Demand → PENDING

---

## 5. API 设计

### 5.1 补货需求 API

```
GET    /api/v1/demands?page=&pageSize=&demandType=&productCode=&status=
GET    /api/v1/demands/{id}
POST   /api/v1/demands
PATCH  /api/v1/demands/{id}
POST   /api/v1/demands/{id}/convert    # 转采购（1:1）
POST   /api/v1/demands/{id}/revert     # 撤销转换
DELETE /api/v1/demands/{id}
GET    /api/v1/demands/{id}/procurement # 查看关联的采购单
```

### 5.2 创建请求体（v2.0.0）

```json
{
  "demandType": "REPLENISHMENT",
  "productCode": "ad009",
  "subProductCode": "ad009-be",
  "quantity": 100,
  "destination": "久留米",
  "japanLead": "田中"
}
```

### 5.3 转采购响应（v2.0.0）

```json
{
  "code": "ok",
  "data": {
    "demandStatus": "CONVERTED",
    "linkedProcurementId": 101
  }
}
```

---

## 6. 代码实现状态

- [x] ✅ `ReplenishmentDemand` 聚合根实体（v2.0.0：直接字段 subProductCode/quantity/destination）
- [x] ✅ `ReplenishmentDemandCreateCmd`（v2.0.0：单条 subProductCode + quantity + destination）
- [x] ✅ `ReplenishmentDemandUpdateCmd`
- [x] ✅ `ReplenishmentDemandPageQuery`
- [x] ✅ `ConvertDemandResponse`（v2.0.0：linkedProcurementId 单值）
- [x] ✅ `DemandType` 枚举
- [x] ✅ `DemandStatus` 枚举
- [x] ✅ `ReplenishmentDemandRepository`
- [x] ✅ `ReplenishmentDemandUseCase`（v2.0.0：1:1 转采购）
- [x] ✅ `ReplenishmentDemandController`
- [x] ✅ `ReplenishmentDemandAssembler`
- [x] ✅ `SalesLowStockEventListener`（v2.0.0：自动生成 Demand）
- [x] ✅ `DemandPage.vue`（v2.0.0：简化为直接字段）
- [x] ✅ `demand.ts` API 客户端
- [x] ✅ `OrderOverviewPage.vue`（v2.0.0：列表含 subProductCode/quantity/destination）
- [x] ✅ `OrderOverviewAssembler` / `OrderOverviewUseCase`
- [x] ✅ `ReplenishmentDemandUseCaseTest` 单元测试（v2.0.0）
- [x] ✅ `DevTestDataInitializer` 初始化数据（v2.0.0：subProductCode/quantity/destination/linkedProcurementId）
- [x] ✅ 数据库迁移脚本（V31：删除 JSON 列，新增 quantity/destination/sub_product_code）
