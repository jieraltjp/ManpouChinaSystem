# SPEC-B13 — 直接采购功能设计

> **版本**: 1.1.0
> **更新**: 2026-05-20（v1.1.0：直接采购复用现有 demand 链路，自动生成空需求记录；新增 ProductType 四种商品类型）
> **创建**: 2026-05-20
> **状态**: 📋 设计中
> **业务背景**: 发注单需支持：无需求直接采购（自动生成空需求记录）以及样品/自用/配件/无关采购 四种商品类型。

---

## 1. 现状分析

### 1.1 当前能力

| 维度 | 现状 |
|------|------|
| 需求→采购链路 | ✅ `ReplenishmentDemand` → `/link` → `Procurement`，通过 `demand_procurement_mapping` 关联 |
| 直接采购 | ⚠️ `ProcurementCreateCmd` 无 `demandId` 必填字段，可创建无需求采购，但无显式入口 |
| 商品类型 | ⚠️ `ProductCategory` 枚举已存在（OEM/ORDINARY/FACTORY_DIRECT）但未挂到 `Procurement` entity |
| 列表区分 | ❌ 无法按商品类型筛选/统计 |

### 1.2 现有枚举

```java
// DemandType — 需求类型（已有，Demand.java 字段）
public enum DemandType {
    REPLENISHMENT,  // 补货（非新品）
    NEW_PURCHASE    // 新品采购
}

// ProductCategory — 商品分类（已有，未挂到 Procurement）
public enum ProductCategory {
    OEM,            // OEM定制产品
    ORDINARY,       // 普货
    FACTORY_DIRECT   // 厂家出口
}
```

### 1.3 问题

1. 直接采购无显式入口，用户不清楚应不选需求还是有专门操作
2. 样品/自用/配件/无关联采购 无商品类型区分，无法统计
3. 直接采购在数据上没有需求记录，散单无法与需求采购统一管理

---

## 2. 业务建模

### 2.1 ProductType 枚举（新增，发注单级别）

```java
/**
 * 商品类型 — 发注单维度。
 * 用于区分采购用途/性质，不影响业务流程。
 */
public enum ProductType {
    /**
     * 普通采购 — 标准补货/新品采购。
     */
    NORMAL("普通采购"),

    /**
     * 样品 — 试单、小批量测试用。
     */
    SAMPLE("样品"),

    /**
     * 自用 — 公司内部使用，不对外销售。
     */
    SELF_USE("自用"),

    /**
     * 配件 — 维修/配套用零部件。
     */
    PARTS("配件"),

    /**
     * 无关联 — 无明确需求来源的独立采购。
     */
    INDEPENDENT("无关联");
}
```

> **决策**：ProductType 是 **Procurement 级别**的属性，不影响 DemandType。
> - `DemandType.REPLENISHMENT` 的需求 → 创建采购时 productType = NORMAL（默认）
> - `DemandType.NEW_PURCHASE` 的需求 → 创建采购时 productType = NORMAL（默认）
> - 无需求直接采购 → productType 由用户选择，auto-generate synthetic demand

### 2.2 Procurement Entity 变更（新增字段）

在 `Procurement.java` 中新增：

```java
/**
 * 商品类型：普通/样品/自用/配件/无关联。
 * 不影响业务流程，用于分类统计。
 */
@Enumerated(EnumType.STRING)
@Column(name = "product_type", length = 16)
private ProductType productType = ProductType.NORMAL;
```

### 2.3 直接采购 — Synthetic Demand 模式

**设计原则**：直接采购复用现有 `Demand → Mapping → Procurement` 链路，不新增独立分支。

**操作语义**：
- 用户选择"直接采购"（商品类型选 SAMPLE/SELF_USE/PARTS/INDEPENDENT）
- 系统自动创建一条"空壳"需求记录（Synthetic Demand），仅生成 demandCode，其他字段全为 null
- 该空需求与采购通过 `DemandProcurementMapping` 关联，状态置为 CONFIRMED
- 前端展示时，直接采购显示 `需求: [自动生成]` 标签，不可编辑

**Synthetic Demand 特性**：

| 字段 | 值 |
|------|-----|
| demandCode | `AUTO-{procurement.productCode}-{timestamp}`（格式规则待定） |
| demandType | `NEW_PURCHASE` |
| productCode | null |
| subProductCode | null |
| quantity | null |
| destination | null |
| japanLead | null |
| status | CONFIRMED |
| linkedProcurementId | 采购 ID |
| remarks | `"[直接采购自动生成]"` |

**为什么用 synthetic demand 而非 null demandId**：
- 现有 `DemandProcurementMapping` 关联机制无需改动
- 需求采购与直接采购在数据模型上完全一致
- `ProcurementPage` 列表可通过 `demandId IS NULL / IS NOT NULL` 区分来源
- 统计查询统一，无需额外处理 null 情况

---

## 3. 数据库变更（Flyway）

```sql
-- 新增 product_type 字段（现有 ProductCategory 枚举保留不动）
ALTER TABLE procurement
  ADD COLUMN product_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL' AFTER status,
  ADD INDEX idx_procurement_product_type (product_type);

-- 为历史数据补值（默认为 NORMAL，保持向后兼容）
UPDATE procurement SET product_type = 'NORMAL' WHERE product_type IS NULL;
```

> **说明**：Synthetic Demand 不需要独立数据库字段，现有 `replenishment_demand` 表直接插入空壳记录即可。

---

## 4. API 设计

### 4.1 ProcurementCreateCmd 变更

```java
/**
 * 商品类型。
 * 不传时后端默认为 NORMAL。
 */
private ProductType productType;

/**
 * 关联需求ID。
 * —— 新建时：可不传，由后端根据 productType 自动生成 synthetic demand
 * —— 新建时：若传入有效 demandId，则建立标准关联（existing demand 链路不变）
 */
private Long demandId;
```

**校验规则**：
- `productType != NORMAL` 时，`demandId` 必须为空（触发 synthetic demand 逻辑）
- `demandId` 有效时，系统按 existing demand 链路处理（productType 仍以传入值为准）
- `demandId` 对应的 `ReplenishmentDemand.status` 必须为 `PENDING`（不允许重复关联）

### 4.2 ProcurementVO 新增字段

```java
/**
 * 商品类型。
 */
private ProductType productType;

/**
 * 关联需求单号。
 * synthetic demand 显示 "[自动生成]"，普通需求显示 demandCode。
 */
private String demandCode;

/**
 * 是否为 synthetic demand（系统自动生成）。
 */
private Boolean syntheticDemand;
```

### 4.3 ProcurementPageQuery 新增筛选

```
GET /api/v1/procurements?productType=SAMPLE&...
```

### 4.4 端点不变

现有 `POST /api/v1/procurements`、`GET /api/v1/procurements`、`PATCH /api/v1/procurements/{id}` 接口体不变，Body/Query 新增字段。

---

## 5. 前端设计

### 5.1 新建弹窗 — 商品类型选择

```
┌─────────────────────────────────────────────────────────────┐
│  新建发注单                                            [×] │
├─────────────────────────────────────────────────────────────┤
│  商品类型:                                                 │
│  [普通采购] [样品] [自用] [配件] [无关联]                    │
│                                                             │
│  选择「样品/自用/配件/无关联」时：                           │
│    → 自动生成关联需求（无需手动选择需求）                    │
│    → 系统将创建一条空需求记录并关联到本采购单                │
│                                                             │
│  [--- 下方为现有表单字段（不变）---]                        │
└─────────────────────────────────────────────────────────────┘
```

**交互逻辑**：

| 操作 | 效果 |
|------|------|
| 选择"普通采购" | 显示现有需求选择器（下拉选 PENDING 需求），保持现有流程 |
| 选择"样品/自用/配件/无关联" | 隐藏需求选择器，后端自动生成空需求并关联 |
| 选择普通采购 + 选需求后 | 自动代入 productCode/subProductCode/destination/japanLead（现有逻辑） |
| 选择非普通类型 | productCode 等字段由用户手动填写（无需求代入） |

### 5.2 列表 — 商品类型筛选

```
筛选栏新增：
  商品类型: [全部 ▼] [普通采购 ▼] [样品 ▼] [自用 ▼] [配件 ▼] [无关联 ▼]
```

### 5.3 列表 — 来源/需求列扩展

```
现有「需求」列扩展显示：
  - 关联需求: DM-20260520-001  （普通需求，可点击跳转 DemandOverview）
  - [自动生成]                  （synthetic demand，蓝色标签，不可点击）
  - —                           （无关联需求，历史数据兼容）
```

### 5.4 详情抽屉

```
商品类型: 样品
需求单号: AUTO-NY612-20260520123  [自动生成]
```

---

## 6. 业务规则

### 6.1 创建逻辑（ProcurementUseCase.create）

```
IF demandId != null:
    // 现有链路：关联已有需求
    1. 校验 demand.status == PENDING
    2. 创建 procurement
    3. 创建 DemandProcurementMapping（allocated_quantity = procurement.quantity）
    4. demand.markAsLinked(procurementId)
    RETURN

ELSE IF productType != NORMAL:
    // 直接采购链路：自动生成 synthetic demand
    1. 创建 procurement
    2. 创建 synthetic ReplenishmentDemand:
       - demandCode = "AUTO-{productCode}-{随机6位}"
       - demandType = NEW_PURCHASE
       - 其他字段 = null
       - status = CONFIRMED
       - linkedProcurementId = procurementId
       - remarks = "[直接采购自动生成]"
    3. 创建 DemandProcurementMapping（allocated_quantity = procurement.quantity, status = 进行中）
    RETURN

ELSE:
    // productType == NORMAL && demandId == null
    // → 作为普通直接采购处理（synthetic demand，productType 仍为 NORMAL）
    1. 创建 procurement
    2. 创建 synthetic demand（与上面相同逻辑）
    RETURN
```

### 6.2 删除逻辑

- `Procurement` 删除时：
  - 若存在关联 synthetic demand → 删除该 synthetic demand（`is_deleted = true`）
  - 若存在关联普通 demand → `demand.unlinkProcurement()`（状态恢复 PENDING）
  - 删除关联 `DemandProcurementMapping`（status → 已取消）

### 6.3 统计口径

| 指标 | SQL |
|------|------|
| 普通采购数 | `COUNT(*) WHERE product_type = 'NORMAL'` |
| 样品采购数 | `COUNT(*) WHERE product_type = 'SAMPLE'` |
| 直接采购总数 | `COUNT(DISTINCT p.id) FROM procurement p JOIN replenishment_demand d ON d.linked_procurement_id = p.id WHERE d.remarks LIKE '%直接采购自动生成%'` |
| 需求转化率 | `需求采购数 / 总发注数` |

---

## 7. 文件变更清单

### 后端

| 文件 | 变更类型 |
|------|----------|
| `procurement/domain/model/ProductType.java` | **新增**枚举（NORMAL/SAMPLE/SELF_USE/PARTS/INDEPENDENT） |
| `procurement/domain/model/Procurement.java` | 新增 `productType` 字段 + `@Enumerated` |
| `procurement/application/dto/ProcurementCreateCmd.java` | 新增 `productType`（默认 NORMAL）、`demandId` 字段 + 校验 |
| `procurement/application/dto/ProcurementUpdateCmd.java` | 新增 `productType` 字段（edit 时可改） |
| `procurement/application/dto/ProcurementVO.java` | 新增 `productType`、`demandCode`、`syntheticDemand` |
| `procurement/application/dto/ProcurementPageQuery.java` | 新增 `productType` 筛选 |
| `procurement/application/usecase/ProcurementUseCase.java` | 创建逻辑分支：existing demand / synthetic demand / normal direct |
| `procurement/application/assembler/ProcurementAssembler.java` | `productType`、`demandCode`、`syntheticDemand` 映射 |
| `replenishment/domain/model/ReplenishmentDemand.java` | 新增 `isSynthetic()` 领域方法 |
| `replenishment/application/service/ReplenishmentDemandService.java` | 新增 `createSyntheticDemand()` 方法 |
| `db/migration/V19__procurement_product_type.sql` | **新增** Flyway 迁移 |

### 前端

| 文件 | 变更类型 |
|------|----------|
| `api/procurement.ts` — 接口类型 | 新增 `ProductType`、`syntheticDemand`、`demandCode` |
| `pages/procurement/ProcurementPage.vue` | 商品类型单选；普通采购显示需求选择器；列表需求列扩展 |
| `locales/zh.json` | 新增 `order.productType.*` |
| `locales/ja.json` | 新增 `order.productType.*` |

### 文档

| 文件 | 变更类型 |
|------|----------|
| `SPEC-B02-发注单-步骤2.md` | 更新 Procurement Entity + API |
| `docs/database/DB-02-procurement-order.md` | 表结构 DDL 更新 |
| `docs/ui/pages/02-procurement.md` | 新建弹窗 UI 更新 |

---

## 8. 实施计划

### Phase 1（后端核心）
1. 新增 `ProductType` 枚举
2. `Procurement` entity 新增 `productType` 字段
3. `ProcurementCreateCmd` / `ProcurementUpdateCmd` / `ProcurementVO` / `ProcurementPageQuery` 新增字段
4. `ProcurementAssembler` 字段映射
5. `ReplenishmentDemand.isSynthetic()` 领域方法
6. `ReplenishmentDemandService.createSyntheticDemand()` 服务方法
7. `ProcurementUseCase.create()` 逻辑分支变更
8. Flyway 迁移脚本
9. 编译验证

### Phase 2（前端）
1. API 类型更新
2. 商品类型单选组件
3. 条件显示需求选择器（仅普通采购显示）
4. 列表需求列扩展（区分 auto-generated）
5. 商品类型筛选
6. i18n
7. TypeScript 编译通过

### Phase 3（验证）
1. 普通采购（选需求）→ 正常关联，demand.status → CONFIRMED
2. 直接采购（样品/自用/配件/无关联）→ 自动生成 synthetic demand，需求列显示"[自动生成]"
3. 删除直接采购 → synthetic demand 同步删除
4. 删除普通采购 → demand.status 恢复 PENDING
5. 按商品类型筛选正确
