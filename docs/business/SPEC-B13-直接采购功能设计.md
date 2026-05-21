# SPEC-B13 — 直接采购功能设计

> **版本**: 1.7.0
> **更新**: 2026-05-21（v1.7.0：订货失败（退货）功能 ✅ — 联动 Demand 终态终态，单向锚点）
> **更新**: 2026-05-20（v1.6.0：商品创建弹窗与子货号新建弹窗合并为统一弹窗（主货号/子货号/名称/分类/材质/质检），移除独立 subCodeDialog ✅）
> **更新**: 2026-05-20（v1.5.0：子货号改为搜索下拉 + 新建按钮 ✅）
> **更新**: 2026-05-20（v1.4.0：提交时检测商品目录，货号不存在则弹出快速新建商品弹窗 ✅）
> **更新**: 2026-05-20（v1.3.0：前端 Phase 2 完成 ✅ — productCode 搜索下拉 + 商品类型单选 + 筛选栏）
> **更新**: 2026-05-20（v1.2.0：商品货号改为搜索下拉，选中后自动填充商品信息；Synthetic Demand 复用 demand 链路）
> **更新**: 2026-05-20（v1.1.0：直接采购复用现有 demand 链路，自动生成空需求记录；新增 ProductType 四种商品类型）
> **创建**: 2026-05-20
> **状态**: 📋 设计中
> **业务背景**: 发注单需支持：无需求直接采购（自动生成空需求记录）以及样品/自用/配件/无关采购 四种商品类型；商品货号改为搜索下拉提升录入效率；发注单标记订货失败时联动 Demand 终态。

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

1. **商品货号手工录入**：当前 `productCode` 为普通文本框，用户需记忆或复制货号，容易出错
2. 直接采购无显式入口，用户不清楚应不选需求还是有专门操作
3. 样品/自用/配件/无关联采购 无商品类型区分，无法统计
4. 直接采购在数据上没有需求记录，散单无法与需求采购统一管理

### 1.4 现有 API 支撑（已有，无需新增）

```typescript
// product.ts — 已有完整支撑
productApi.suggestMasterCodes(keyword)  // 货号模糊搜索 → MasterCodeSuggestVO[]
productApi.getByCode(masterCode)        // 获取商品详情 → ProductPageVO
productApi.suggestSubCodes(masterCode)   // 子货号搜索
```

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

### 5.1 商品货号搜索下拉（核心 UX 变更）

**现有**：`productCode` 为普通 `el-input`，手工输入。

**改为**：`el-select` + `filterable` + `remote` + `remote-method`，调用 `productApi.suggestMasterCodes()`。

```
┌─────────────────────────────────────────────────────────┐
│  商品货号                                              │
│  ┌─────────────────────────────────────────────────┐  │
│  │ 🔍 搜索货号...（输入即搜）                      │  │
│  └─────────────────────────────────────────────────┘  │
│  ↓ 展开下拉                                          │
│  ┌─────────────────────────────────────────────────┐  │
│  │ ny612         ny浴衣 collection 2色           │  │
│  │ ny007         ny休闲短裤 collection 3色       │  │
│  │ ad009         ad运动鞋 basic line             │  │
│  │ de077         de家居服 premium line           │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
│  主货号/子货号/分类 → 自动填充（选中后）               │
└─────────────────────────────────────────────────────────┘
```

**交互逻辑**：

| 步骤 | 行为 |
|------|------|
| 用户聚焦/输入 | 调用 `productApi.suggestMasterCodes(keyword)`，返回匹配列表 |
| 选择货号后 | 调用 `productApi.getByCode(masterCode)`，自动填充：<br>`category`（分类）、`material`（材质）、`requiresQc`（是否检测）、`priceRmb`（单价）、`taxPoint`（票点）；同时加载该货号的子货号下拉 |
| 手工修改已填充字段 | 允许，覆盖自动填充值 |
| 搜索无结果 | 用户仍可自由输入新货号（el-select filterable 支持手动输入）|

**子货号搜索下拉（v1.5.0 ✅）**：

```
主货号选择后 → 调用 productApi.suggestSubCodes(masterCode)
→ 填充子货号下拉列表（subCode + colorName）
→ 用户可搜索/选择已有子货号
→ 也可点击"新建"添加新的色款变体
```

| 按钮 | 效果 |
|------|------|
| 新建 | 弹出子货号快速创建弹窗：masterCode（只读自动填入）/ subCode（必填）/ colorName（可选）→ 调用 `productApi.create()` |
| 编辑 | 预留（当前仅支持新建） |

**商品不存在时快速新建（v1.4.0 ✅）**：

```
提交采购单 → productApi.getByCode(productCode) → 404 或异常
→ 弹出快速新建商品弹窗（仅 masterCode / nameZh / category）
→ 创建成功后 → 自动继续提交采购单
```

| 字段 | 说明 |
|------|------|
| masterCode | 自动代入，禁用只读 |
| nameZh | 必填，商品中文名称 |
| category | 可选，OEM / 普货 / 厂家出口 |

**下拉选项展示**：

```
label = `${masterCode}  ${nameZh || nameEn || ''}  ${colorCount > 1 ? `[${colorCount}色]` : ''}`
value = masterCode
```

### 5.2 新建弹窗 — 商品类型选择 + 需求关联

```
┌─────────────────────────────────────────────────────────────┐
│  新建发注单                                            [×] │
├─────────────────────────────────────────────────────────────┤
│  商品类型:                                                 │
│  [普通采购] [样品] [自用] [配件] [无关联]                    │
│                                                             │
│  关联需求（仅普通采购显示）:                                │
│  [下拉选择 PENDING 需求 ▼]   ← 现有需求选择器              │
│                                                             │
│  直接采购（样品/自用/配件/无关联）时：                      │
│    → 无需选择需求，系统自动生成空需求记录                    │
│    → 商品货号请使用上方搜索下拉选择                         │
│                                                             │
│  [--- 商品信息区域（productCode 改为搜索下拉）---]          │
└─────────────────────────────────────────────────────────────┘
```

**交互逻辑**：

| 操作 | 效果 |
|------|------|
| 选择"普通采购" | 显示现有需求选择器（下拉选 PENDING 需求），保持现有流程 |
| 选择"样品/自用/配件/无关联" | 隐藏需求选择器，后端自动生成空需求并关联 |
| 选择普通采购 + 选需求后 | 自动代入 productCode/subProductCode/destination/japanLead（现有逻辑） |
| 选择非普通类型 | productCode 使用搜索下拉（来自商品目录），自动填充相关信息 |

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
| `api/procurement.ts` — `CreateProcurementRequest` | 新增 `productType` |
| `pages/procurement/ProcurementPage.vue` | **productCode 改为搜索下拉**（el-select filterable remote）；商品类型单选；普通采购显示需求选择器；**合并商品创建弹窗**（主货号/子货号/名称/分类/材质/质检）；列表来源列扩展 |
| `locales/zh.json` | 新增 `order.productType.*`、`order.dialog.productCodeSearchPlaceholder`、`order.productCreateDialog.*` |
| `locales/ja.json` | 新增 `order.productType.*`、`order.dialog.productCodeSearchPlaceholder`、`order.productCreateDialog.*` |

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
1. ✅ API 类型更新（`ProductType`、新增字段）
2. ✅ **商品货号搜索下拉**（核心）：
   - `el-select` + `filterable` + `remote` + `remote-method`
   - `remote-method` 调用 `productApi.suggestMasterCodes(query)`
   - 选择后调用 `productApi.getByCode(masterCode)` 自动填充 category/material/requiresQc/priceRmb/taxPoint
   - 同时调用 `productApi.suggestSubCodes(masterCode)` 加载子货号下拉
   - 搜索无结果时用户仍可自由输入新货号
3. ✅ **子货号搜索下拉**：el-select（filterable remote）+ "新建"按钮
4. ✅ **商品快速创建弹窗（合并版）**：子货号"新建"按钮与商品不存在时触发的弹窗统一为 `productCreateDialog`
   - 字段：主货号（可留空自动生成）/ 子货号 / 商品名称（必填）/ 分类 / 材质 / 需要检测
   - 新建后：自动刷新子货号下拉并选中；或自动刷新商品下拉并自动填充商品信息
   - 移除旧的独立 `subCodeDialog`（含新建/编辑两个入口）
5. ✅ 商品类型单选组件（5选1，`el-radio-button`）
6. ✅ 条件显示需求选择器（仅普通采购 NORMAL 显示）
7. ✅ **提交时检测商品目录，货号不存在则弹出快速新建商品弹窗**
   - `onSubmit` → `productApi.getByCode(productCode)` → 404 → 弹出 `productCreateDialog`
   - 新建商品成功后继续提交采购单
8. ⏳ 列表来源列扩展（区分 auto-generated `[自动生成]` vs `DM-xxx`）— 后端未完成，暂挂
9. ✅ 商品类型筛选下拉
10. ✅ i18n 新增 key（zh.json / ja.json）
11. ✅ `npm run type-check` 通过

> ⚠️ Phase 2 第7项（列表来源列）依赖后端 `demandCode` / `syntheticDemand` 字段，待 Phase 1 后端完成后补充。

### Phase 3（验证）
1. 货号搜索下拉 → 输入 "ny" 返回匹配列表，选择后自动填充字段 + 加载子货号下拉
2. 子货号搜索下拉 → 选择主货号后显示子货号列表，可搜索/选择
3. 子货号点击"新建" → 弹出统一商品创建弹窗 → 填写后创建成功 → 刷新下拉并自动选中
4. 手工修改已填充字段 → 覆盖自动值成功
5. 输入新货号 → 提交 → 弹出快速新建商品弹窗 → 创建后继续提交采购单
6. 普通采购（选需求）→ 正常关联，demand.status → CONFIRMED
7. 直接采购（样品/自用/配件/无关联）→ 自动生成 synthetic demand，需求列显示"[自动生成]"
8. 删除直接采购 → synthetic demand 同步删除
9. 删除普通采购 → demand.status 恢复 PENDING
10. 按商品类型筛选正确

---

## 9. 订货失败（退货）功能（v1.7.0）

### 9.1 业务背景

发注单在出货后可能因质量/交期等原因被标记为「订货失败」，需要：
1. 记录失败原因（退货理由）
2. 同步告知需求侧：该需求已失败
3. 状态不可逆（终态）

### 9.2 设计原则

- **联动单向**：Procurement 标记订货失败 → Demand 自动联动变为 RETURNED（不需要用户手动操作 Demand）
- **终态**：Procurement `退货 → 完了`，Demand `RETURNED` 也是终态，不可再流转
- **显示名**：统一显示为「订货失败」（i18n key: `order.status.订货失败`）
- **退货原因**：记录在 `procurement.return_reason`，Demand 无独立退货原因字段（联动追溯）

### 9.3 状态建模

```java
// ShipmentStatus（已有 ✅）
退货   // 订货失败（Phase2 简化路径下：已下单 ↔ 已出货 ↔ 退货）

// DemandStatus（需扩展）
PENDING,     // 待确认
CONFIRMED,   // 已确认（已关联发注单）
RETURNED     // 订货失败（终态，由 Procurement 退货联动触发）
```

**FSM 变更**（`ShipmentStatus.java`）：
- `退货` 的可转换目标：`完了`（终态）
- `已出货` 可直接转换到 `退货`
- `完了` 为终态，禁止任何状态变更

### 9.4 数据库变更（Flyway）

```sql
-- 新增 return_reason 字段
ALTER TABLE procurement
  ADD COLUMN return_reason VARCHAR(512) AFTER status;

-- 新增 return_date 字段（记录退货时间）
ALTER TABLE procurement
  ADD COLUMN return_date DATETIME AFTER return_reason;
```

> `v_order_chain_v1` VIEW 无需变更，`step2_status` 保持不变（`procurement_id IS NOT NULL` → `COMPLETED`）。

### 9.5 后端变更

**Procurement Entity**（`Procurement.java`）：

```java
/**
 * 退货原因。
 * 标记为订货失败时填写，如"质量问题"、"交期延误"等。
 */
@Column(name = "return_reason", length = 512)
private String returnReason;

/**
 * 退货时间。
 */
@Column(name = "return_date")
private LocalDateTime returnDate;
```

**ProcurementUpdateCmd**（`ProcurementUpdateCmd.java`）：

```java
// 新增字段（edit 时可选填）
private String returnReason;
```

**DemandStatus 枚举**（`DemandStatus.java`）：

```java
public enum DemandStatus {
    PENDING,     // 待确认
    CONFIRMED,   // 已确认（已关联发注单）
    RETURNED     // 订货失败（终态，联动触发）
}
```

**ProcurementUseCase.update 逻辑**（`ProcurementUseCase.java`）：

```
WHEN status → 退货:
    1. 保存 returnReason + returnDate
    2. 查找该 procurement 关联的所有 demand（linkedProcurementId = this.id AND is_deleted = false）
    3. 对每条 demand: demand.status = RETURNED（调用 demandRepository.save()）
    4. 日志记录联动更新的 demandId 列表
```

**OrderChainUseCase.toDetailVO**（`OrderChainUseCase.java`）：

```java
// ProcurementVO 新增字段透传
.returnReason(v.getProcurementReturnReason())
```

### 9.6 前端 ProcurementPage 变更

**状态切换交互**：

```
点击状态 tag → 切换: 已下单 ↔ 已出货
点击状态 tag → 长按/右键 → 选择「订货失败」
→ 弹出退货原因输入弹窗（el-dialog + el-input type="textarea"）
→ 确认后调用 PATCH /api/v1/procurements/{id} { status: "退货", returnReason: "..." }
```

**退货原因输入弹窗**：

```
┌────────────────────────────────────────────┐
│  标记为订货失败                        [×] │
├────────────────────────────────────────────┤
│  退货原因:                                 │
│  ┌────────────────────────────────────┐   │
│  │ 请输入退货原因...                   │   │
│  └────────────────────────────────────┘   │
│  提示：标记后状态不可逆，将同步告知需求侧   │
│                                             │
│              [取消]        [确认]           │
└────────────────────────────────────────────┘
```

**列表状态标签**：

| 状态 | 标签类型 | 说明 |
|------|---------|------|
| 已下单 | info | 蓝色 |
| 已出货 | success | 绿色 |
| 订货失败 | danger | 红色（醒目） |

**抽屉步骤2区域新增**：

```
状态: 订货失败    ← el-tag type="danger"
退货原因: 质量问题，交期延误超30天
```

### 9.7 前端 DemandPage 变更

DemandPage 不需要独立操作入口（联动自动触发）。列表显示时：

| Demand.status | 标签 | 说明 |
|---------------|------|------|
| PENDING | info | 蓝色 |
| CONFIRMED | success | 绿色 |
| RETURNED | danger | 红色，「订货失败」 |

### 9.8 前端 OrderOverviewPage 变更

`chainStatusType` / `chainStatusLabel` 感知 `procurementStatus = '退货'`：

```typescript
function chainStatusType(row: OrderChainVO): string {
  if (row.step4Status === 'COMPLETED') return 'success'
  // ...existing logic...
  if (row.step2Status === 'COMPLETED' && row.procurementStatus === '退货') return 'danger'  // 新增
  return 'info'
}
```

抽屉步骤2区域：若 `status = '退货'` 且 `returnReason` 存在，显示红色标签 + 原因文本。

### 9.9 i18n 变更

**zh.json**：
```json
"order": {
  "status": {
    "订货失败": "订货失败"
  },
  "column": {
    "returnReason": "退货原因"
  },
  "dialog": {
    "markReturn": "标记为订货失败",
    "returnReasonPlaceholder": "请输入退货原因...",
    "returnConfirmTip": "标记后状态不可逆，将同步告知需求侧"
  }
},
"demand": {
  "status": {
    "RETURNED": "订货失败"
  }
}
```

**ja.json**：
```json
"order": {
  "status": {
    "订货失败": "注文件失敗"
  },
  "column": {
    "returnReason": "退货理由"
  },
  "dialog": {
    "markReturn": "注文件失敗としてマーク",
    "returnReasonPlaceholder": "退货理由を入力してください...",
    "returnConfirmTip": "マーク後、状態は元に戻せません。需要側に通知されます"
  }
},
"demand": {
  "status": {
    "RETURNED": "注文件失敗"
  }
}
```

### 9.10 实施清单

| 层级 | 文件 | 变更 |
|------|------|------|
| DB | `V19__procurement_return_fields.sql` | 新增 Flyway：return_reason + return_date |
| 后端 Entity | `Procurement.java` | 新增 returnReason + returnDate 字段 |
| 后端 DTO | `ProcurementUpdateCmd.java` | 新增 returnReason |
| 后端 DTO | `ProcurementPageVO.java` | 新增 returnReason + returnDate |
| 后端 DTO | `ProcurementVO.java`（orderChain） | 新增 returnReason |
| 后端 | `DemandStatus.java` | 新增 RETURNED 枚举值 |
| 后端 | `ProcurementUseCase.update()` | 标记退货时联动 Demand → RETURNED |
| 后端 | `OrderChainUseCase.toDetailVO()` | 透传 returnReason |
| 后端 | `ShipmentStatus.java` | FSM: 退货 → 完了；已出货 → 退货 |
| 前端 | `api/procurement.ts` | `UpdateProcurementRequest` / `ProcurementPageVO` 补字段 |
| 前端 | `pages/procurement/ProcurementPage.vue` | 状态 tag 逻辑扩展 + 退货原因弹窗 + 抽屉退货原因行 |
| 前端 | `pages/procurement/DemandPage.vue` | `demandStatusLabel` / `demandStatusType` 感知 RETURNED |
| 前端 | `pages/procurement/OrderOverviewPage.vue` | `chainStatusType` 感知退货 + 抽屉显示 |
| 前端 | `locales/zh.json` / `ja.json` | 订货失败/退货原因相关 i18n |
| 文档 | 本文档 v1.7.0 | ✅ |
