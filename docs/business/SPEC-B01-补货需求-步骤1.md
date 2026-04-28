# SPEC-B01 — 补货需求业务规格（步骤1）

> **版本**: 2.2.0
> **更新**: 2026-04-28（v2.2.0：彻底移除转采购流程，改为在发注单页面关联需求；状态精简为 PENDING/CONFIRMED 两级）
> **更新**: 2026-04-28（v2.1.0：DemandStatus 新增 CONFIRMED 状态 + toggle-confirm 接口 PENDING↔CONFIRMED 切换）
> **更新**: 2026-04-24（v2.0.0：每条 Demand = 一个子货号（主货号+子货号 = 商品唯一标识））
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **业务步号**: 01（补货需求/新品采购）
> **对应 UI 文档**: `docs/ui/pages/01-demand.md`
> **对应数据库文档**: `docs/database/DB-01-procurement-demand.md`

---

## 1. 业务背景

补货需求是整个采购链路的入口。非新品走补货流程，新品走新品采购流程。
录入后，由发注单（Procurement）新建时主动关联需求 Demand，进入步骤2。

**核心设计原则（v2.2.0）**：
- 商品唯一标识 = 主货号 + 子货号，如 `ad009-be`
- **每条 ReplenishmentDemand = 一个子货号**
- Demand 本身不生成 Procurement，由 Procurement 关联 Demand（反向关联）
- Demand 状态只有两级：PENDING（待确认）和 CONFIRMED（已关联发注单）

---

## 2. 聚合根

### ReplenishmentDemand（补货需求单，v2.2.0）

```
ReplenishmentDemand（聚合根）
├── id: Long
├── demandCode: String           # DM-YYYYMMDD-NNN
├── demandType: DemandType      # REPLENISHMENT(补货) / NEW_PURCHASE(新品采购)
├── productCode: String          # 主货号（如 ad009）
├── subProductCode: String       # 子货号全码（如 ad009-be，商品唯一标识）
├── quantity: Integer            # 该子货号的需求数量
├── destination: String          # 目的地（如久留米/名古屋）
├── japanLead: String           # 日本担当
├── status: DemandStatus         # PENDING / CONFIRMED
├── linkedProcurementId: Long   # 关联的 Procurement.id（CONFIRMED 时填充）
├── imageUrl: String             # 商品图片URL（反规范自 Product 表）
└── 领域方法
    ├── markAsLinked(procurementId)    # 标记已关联发注单 → status=CONFIRMED
    └── unlinkProcurement()            # 取消关联 → status=PENDING，linkedProcurementId=null
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
    PENDING,     // 待确认（录入后默认，或取消关联后）
    CONFIRMED    // 已确认（已关联发注单，由发注单页面关联时写入 linkedProcurementId）
}
```

---

## 4. 状态流转

```
  PENDING（待确认）
     │
     └──[发注单页面关联 Demand]──▶ CONFIRMED（已确认）
     │                                   │
     └──[Demand 列表点击状态标签]◀────────┘（取消关联）
```

### 状态语义（v2.2.0）

- **PENDING（待确认）**：Demand 已录入，还未关联到任何发注单。可以在 Demand 列表点击状态标签取消关联（恢复 PENDING）。
- **CONFIRMED（已确认）**：Demand 已关联到某个 Procurement（`linkedProcurementId` 有值）。在发注单列表或 Demand 列表均可操作。
- **无终态**：任何状态均可删除 Demand（软删除）。
- **取消关联**：点击 CONFIRMED 状态标签 → 调用 `unlinkProcurement()` → 状态回 PENDING，linkedProcurementId 清空。

### 关联语义（v2.2.0 — 反向关联）

- **Demand ← Procurement**：Demand 不生成 Procurement，而是由 Procurement 主动关联 Demand
- **1 Demand → 多个 Procurement**：不限制（同一个需求可被多个发注单引用）
- 关联操作在 `ProcurementPage.vue` 新建/编辑弹窗中选择关联需求时写入 `linkedProcurementId`

---

## 5. API 设计

### 5.1 补货需求 API

```
GET    /api/v1/demands?page=&pageSize=&demandType=&productCode=     # 不筛选 status，只显示 PENDING/CONFIRMED
GET    /api/v1/demands/{id}
POST   /api/v1/demands
PATCH  /api/v1/demands/{id}
DELETE /api/v1/demands/{id}                     # 任何状态均可删除
POST   /api/v1/demands/{id}/link               # 关联到发注单（v2.2.0）
POST   /api/v1/demands/{id}/unlink             # 取消关联（v2.2.0）
GET    /api/v1/demands/{id}/procurement        # 查看关联的采购单
```

> **注意（v2.2.0）**：移除了 `POST /{id}/convert`（转采购）和 `POST /{id}/revert`（撤销转换）接口，移除了 `status` 筛选参数。

### 5.2 创建请求体（v2.0.0）

```json
{
  "demandType": "REPLENISHMENT",
  "productCode": "ad009",
  "subProductCode": "ad009-be",
  "quantity": 100,
  "destination": "久留米",
  "japanLead": "田中",
  "remarks": ""
}
```

### 5.3 关联/取消关联

```
POST /api/v1/demands/{id}/link?procurementId={procurementId}
→ status=CONFIRMED，linkedProcurementId={procurementId}

POST /api/v1/demands/{id}/unlink
→ status=PENDING，linkedProcurementId=null
```

---

## 6. 与发注单（Procurement）的关联关系

- **关联入口**：`ProcurementPage.vue` 新建/编辑弹窗 → 选择关联需求下拉 → 选中后调用 `POST /api/v1/demands/{id}/link`
- **取消关联入口**：
  - `ProcurementPage.vue` 编辑弹窗 → 取消选择关联需求 → 调用 `POST /api/v1/demands/{id}/unlink`
  - `DemandPage.vue` → 点击 CONFIRMED 状态标签 → 调用 `POST /api/v1/demands/{id}/unlink`
- **已关联的 Demand 在 DemandPage 中显示为 CONFIRMED 状态（绿色标签）**

---

## 7. 代码实现状态

- [x] ✅ `ReplenishmentDemand` 聚合根实体（v2.2.0：markAsLinked/unlinkProcurement）
- [x] ✅ `ReplenishmentDemandCreateCmd`
- [x] ✅ `ReplenishmentDemandUpdateCmd`
- [x] ✅ `ReplenishmentDemandPageQuery`
- [x] ✅ `DemandType` 枚举
- [x] ✅ `DemandStatus` 枚举（v2.2.0：仅 PENDING/CONFIRMED）
- [x] ✅ `ReplenishmentDemandRepository`
- [x] ✅ `ReplenishmentDemandUseCase`（v2.2.0：link/unlink 方法）
- [x] ✅ `ReplenishmentDemandController`（v2.2.0：link/unlink 接口）
- [x] ✅ `ReplenishmentDemandAssembler`
- [x] ✅ `DemandPage.vue`（v2.2.0：无转采购弹窗，CONFIRMED 标签可点击取消关联）
- [x] ✅ `ProcurementPage.vue`（v2.2.0：关联需求下拉，选中时调用 link 接口）
- [x] ✅ `demand.ts` API 客户端（v2.2.0：link/unlink 方法）
- [x] ✅ `DevTestDataInitializer`（v2.2.0：全部初始化为 PENDING）
- [ ] 🔲 待实现：`ProcurementPage.vue` 关联需求下拉 → 调用 `POST /api/v1/demands/{id}/link`
- [ ] 🔲 待实现：`ProcurementPage.vue` 取消关联 → 调用 `POST /api/v1/demands/{id}/unlink`
