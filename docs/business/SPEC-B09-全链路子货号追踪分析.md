# SPEC-B09-EX — 全链路子货号追踪分析

> **版本**: 1.0.0
> **创建**: 2026-04-24
> **状态**: 🟡 建设中
> **背景**: v1.6.0 批量子货号改造完成后，系统需要确认：全链路（需求单→发注单→国内报关→日本清关→调配计划→销售记录）是否按子货号分开追踪；Overview 页面锚点设计是否满足 v1.6.0 需求。

---

## 1. 业务背景

v1.6.0 核心变更：需求单（ReplenishmentDemand）从单 quantity/destination 改为 JSON 数组存储多个子货号明细（SubProductItem），每个 SubProductItem 独立数量和目的地。转采购时循环生成多个 Procurement。

**用户意图**：批量录入多个子货号 → 系统内部每条子货号独立流转 → 全链路可追溯

---

## 2. 全链路追踪分析

### 链路总图

```
ReplenishmentDemand
  subProductItemsRaw: JSON数组 [{"subCode":"be","quantity":100,"destination":"久留米"},{"subCode":"bu","quantity":50,"destination":"名古屋"}]
  │
  │  [转采购循环 — ReplenishmentDemandUseCase.convertToProcurement()]
  │  for each SubProductItem → new Procurement()
  ▼
Procurement × N（每个 SubProductItem 一个 Procurement）
  subProductCode ✓
  linkedDemandId ✓
  │
  ├─→ [事件驱动] LogisticsInTransitEvent
  │         ▼
  │     DomesticCustomsRecord × N（有 subProductCode）✓
  │         ▼
  │     LogisticsPlan × N（手动创建，有 subProductCode）✓
  │         ▼
  │     JapanCustomsRecord × N（手动创建，无 subProductCode）✗ 缺口
  │         ▼
  │     [事件驱动] JapanCustomsClearedEvent
  │         ▼
  │     SalesRecord × N（有 subProductCode，从 Procurement 继承）✓
  │
  └─→ SalesRecord × N（从 Procurement 取 subProductCode）✓
```

### 每步详情

| 步骤 | 实体 | subProductCode | 触发方式 | 链路完整 | 缺口说明 |
|------|------|:---:|-----------|:---:|---------|
| 1 需求单 | ReplenishmentDemand | JSON数组 | 手动批量录入 | ✅ | v1.6.0 核心：JSON 存多条子货号明细 |
| 2 发注单 | Procurement | ✅ | 转采购循环（逐条创建） | ✅ | 循环中 `linkedDemandId` 建立双向关联 |
| 3 验货 | QcRecord | ✅ | 手动创建 | ✅ | `procurementId` 精确追溯到单条 Procurement |
| 4 调配计划 | LogisticsPlan | ✅ | 手动创建 | ✅ | 单独创建，procurementId 精确关联 |
| 5 国内报关 | DomesticCustomsRecord | ✅ | 物流在途事件自动创建 | ✅ | `LogisticsInTransitEventListener` 逐条创建 |
| **6 日本清关** | **JapanCustomsRecord** | **✗** | **手动创建** | **⚠️** | **缺少 subProductCode，通过 procurementId 间接可查** |
| 7 退税 | TaxRefundRecord | — | 手动创建 | ✅ | 财务维度，按采购单总额退税，不区分子货号 |
| 8 销售记录 | SalesRecord | ✅ | 日本清关完成事件自动创建 | ✅ | `JapanCustomsClearedEventListener` 从 Procurement 取 subProductCode |

### 步骤详解

#### 步骤2：发注单 ✅

**实体**：`Procurement`，`subProductCode` 字段
**触发**：转采购时循环创建

```java
// ReplenishmentDemandUseCase.convertToProcurement()
for (int i = 0; i < subItems.size(); i++) {
    SubProductItem item = subItems.get(i);
    ProcurementCreateCmd pCmd = new ProcurementCreateCmd();
    pCmd.setSubProductCode(item.getSubCode());
    pCmd.setQuantity(item.getQuantity());
    pCmd.setDestination(item.getDestination());
    pCmd.setLinkedDemandId(demandId);
    pCmd.setLinkedDemandItemId((long) i);  // 关联到具体 SubProductItem
    procurementUseCase.create(pCmd);
}
```

同时 `linkedDemandItemsRaw`（Demand）填充 JSON：`[{"linkedProcurementId":101,"subCode":"be"},...]`，建立双向可查。

#### 步骤5：国内报关 ✅

**实体**：`DomesticCustomsRecord`，`subProductCode` 字段
**触发**：`LogisticsInTransitEventListener`（TransactionalEventListener AFTER_COMMIT）

```java
public void onLogisticsInTransit(LogisticsPlanInTransitEvent evt) {
    DomesticCustomsRecord record = new DomesticCustomsRecord();
    record.setProcurementId(evt.getProcurementId());
    record.setSubProductCode(evt.getSubProductCode());  // ✓ 子货号精确传递
    record.setProductCode(evt.getProductCode());
    // ...
}
```

每条 LogisticsPlanInTransitEvent 对应一条 DomesticCustomsRecord，subProductCode 精确传递。

#### 步骤4：调配计划 ✅

**实体**：`LogisticsPlan`，`subProductCode` 字段
**触发**：手动创建（`LogisticsPlanUseCase.create()`）
**设计合理性**：物流计划按出货批次，可以多件商品合一个 LogisticsPlan，但表本身有 subProductCode，下游仍可追溯。

#### 步骤6：日本清关 ⚠️ 缺口

**实体**：`JapanCustomsRecord`，**无** `subProductCode` 字段
**触发**：手动创建（`JapanCustomsController` → `JapanCustomsUseCase.create()`）
**当前链路**：`JapanCustomsRecord.procurementId` → `Procurement.subProductCode`（间接追溯）

**业务合理性**：日本清关行按提单/货件维度处理，不按商品 SKU 区分——这一层确实可以不分。但下游 `SalesRecord` 需要精确的 subProductCode。

**缺口后果**：
- `JapanCustomsClearedEvent` 只传 `procurementId`，`JapanCustomsClearedEventListener` 从 Procurement 查 subProductCode，绕过了缺口
- 但 `JapanCustomsRecord` 表本身不记录子货号，清关员页面无法显示

#### 步骤8：销售记录 ✅

**实体**：`SalesRecord`，`subProductCode` 字段
**触发**：`JapanCustomsClearedEventListener`（TransactionalEventListener AFTER_COMMIT）

```java
// JapanCustomsClearedEventListener.createSalesRecordFromProcurement()
record.setProcurementId(procurement.getId());
record.setProductCode(procurement.getProductCode());
record.setSubProductCode(procurement.getSubProductCode());  // ✓ 从 Procurement 继承
record.setStatus(SalesStatus.LISTED);
```

---

## 3. Overview 页面架构分析

### 3.1 锚点设计

```
Overview 锚点 = Procurement.id
URL: /base/overview/{procurementId}
```

`OrderOverviewUseCase.getOverview(procurementId)` 以单条 Procurement 为锚点，聚合该 Procurement 的完整下游链路。

### 3.2 v1.6.0 影响：一个 Demand → 多个 Procurement

```
ReplenishmentDemand
  └── Procurement(be, 100件, 久留米)     → /base/overview/101
  └── Procurement(bu, 50件, 名古屋)     → /base/overview/102
  └── Procurement(re, 75件, 大阪)       → /base/overview/103
```

**结论**：每个 Procurement 的 Overview 页面独立、完整；但 Overview 列表页无法一眼看到"同需求单下的所有子货号"。

### 3.3 现状：前端 Bug 已修复

**Bug**：Step1 卡片前端模板引用旧字段 `overview.demand.quantity` 和 `overview.demand.destination`（v1.5 字段），后端 `DemandVO` 早已改为 `subProductItemsSummary`（格式 `"be:100久留米, bu:50名古屋"`）。

**修复**：
- `api/orderOverview.ts`：前端 `DemandVO` 类型 `quantity`/`destination` → `subProductItemsSummary`
- `OrderOverviewPage.vue`：Step1 卡片模板对应更新
- `locales/zh.json` + `locales/ja.json`：`quantity`/`destination` → `subProductItems`

---

## 4. 缺口修复方案

### 4.1 缺口：日本清关补充 subProductCode

**优先级**：P2（业务链路实际通过 Procurement 可查，仅展示层缺失）
**理由**：
1. 日本清关行的业务操作确实按货件/提单维度，不需要精确到 SKU
2. 财务（退税）和运营（销售）都通过 `procurementId` 精确追溯

**如需修复**：

```sql
-- DB migration
ALTER TABLE japan_customs_record ADD COLUMN sub_product_code VARCHAR(64);
```

```java
// JapanCustomsRecord.java 新增字段
@Column(name = "sub_product_code", length = 64)
private String subProductCode;

// JapanCustomsUseCase.create() 传入
// JapanCustomsAssembler.toJapanCustomsVO() 填充
```

**不修复的影响**：极小——`JapanCustomsClearedEventListener` 直接从 Procurement 查 subProductCode，销售记录数据完全正确。缺口仅在清关员查看日本清关页面时看不到子货号字段。

### 4.2 体验优化：Overview 列表页展示关联子货号（待定）

**需求**：在 Overview 列表页（`/base/overview` 选择器模式），将同 Demand 的多个 Procurement 关联展示。

**方案 A**：在列表页增加"关联需求单"列，显示 Demand 编号 + 子货号摘要

```
需求单编号        子货号明细           状态
D-20260424-001   be:100, bu:50, re:75   进行中
  └─ P-101 (be) — 久留米 — 步骤3
  └─ P-102 (bu) — 名古屋 — 步骤5
  └─ P-103 (re) — 大阪   — 步骤2
```

**方案 B**：保持现状（每个 Procurement 单独一行），在详情页顶部显示"同需求单的其他子货号"折叠区

---

## 5. 批量 UX 验证

| 用户操作 | 系统内部行为 | 用户感知 |
|---------|------------|---------|
| 需求单录入3个子货号 | 存1条 Demand，subProductItemsRaw=JSON数组 | 一次录入 ✓ |
| 点"转采购" | 循环创建3条 Procurement | 一次操作 ✓ |
| 查看 Overview | 每个 Procurement 单独一条 Overview | 自然分开 ✓ |
| 调配计划发货 | 可选：3条 LogisticsPlan 或1条含3件的批次 | 操作灵活 ✓ |
| 国内报关 | 自动创建3条 DomesticCustomsRecord | 无感知 ✓ |
| 日本清关 | 手动创建3条 JapanCustomsRecord（或按发货批次合并）| 操作可选 ✓ |
| 销售记录 | 自动创建3条 SalesRecord | 无感知 ✓ |

**结论**：批量 UX 设计合理——用户批量操作，系统内部每条子货号独立流转，互不干扰。

---

## 6. v1.3.0 架构变更：Overview Demand 中心化

### 变更原因

**问题**：原 Overview 是 Procurement 中心——列表只展示 Procurements，新建 Demand 不会出现在 Overview 中。

**设计**：Overview 从 Procurement 中心 → Demand 中心，支持双入口。

```
/base/overview                          → 双 Tab 列表（需求单 / 发注单）
/base/overview/demand/:demandId           → Demand 详情（Step1 有数据，Step2-8 未开始）
/base/overview/procurement/:procurementId → Procurement 详情（8 步全链路）
```

### 新建需求单后的完整数据流

```
1. 用户在 DemandPage 新建需求单
      ↓
2. Demand 立即出现在 /base/overview → 需求单 Tab（状态=PENDING）
      ↓
3. 用户点击"总览" → 跳转到 /base/overview/demand/:id
      ↓ 详情页
4. Step1 有数据（demandCode、productCode、subProductItemsSummary）
   Step2-8 全部"未开始"
   进度条：步骤1=绿色，步骤2-8=灰色
      ↓
5. 用户点击"转采购"（需选工厂）
      ↓
6. 创建 N 条 Procurement（N = 子货号数量）
   Demand 状态 → CONVERTED
   自动跳转到 /base/overview/procurement/:firstId
      ↓ 详情页
7. Step1=CONVERTED，Step2 有数据（发注单）
   Step3-8 按实际流程逐步填写
```

---

## 7. 数据修复：历史旧数据兼容

v1.5 历史数据的 `sub_product_code` 格式为 `["be","bu","re"]`（JSON 字符串数组），v1.6.0 的 `ReplenishmentDemandAssembler.parseSubProductItems()` 需要兼容：

```java
// ReplenishmentDemandAssembler.parseSubProductItems(raw)
if (raw == null) return Collections.emptyList();
raw = raw.trim();

// 旧格式：["be","bu"] → JSON数组，非对象数组
if (raw.startsWith("[")) {
    try {
        var node = objectMapper.readTree(raw);
        if (node.isArray()) {
            List<SubProductItem> items = new ArrayList<>();
            for (var elem : node) {
                if (elem.isTextual()) {
                    // 旧格式元素：字符串 "be" → 转为 SubProductItem(subCode="be")
                    items.add(new SubProductItem(elem.asText(), null, null));
                } else if (elem.isObject()) {
                    // 新格式元素：对象 {"subCode":"be","quantity":100,"destination":"久留米"}
                    items.add(parseObjectElement(elem));
                }
            }
            return items;
        }
    } catch (JsonProcessingException e) { /* fall through */ }
}
// 纯字符串旧格式：be,bu,re
return parseCommaSeparated(raw);
```

---

## 7. 结论

### 链路完整性

| 评估维度 | 结论 |
|---------|------|
| 子货号分开追踪 | ✅ 除日本清关外的所有步骤均精确到子货号 |
| 批量 UX | ✅ 用户批量录入，系统内部逐条分开 |
| Overview 锚点正确性 | ✅ 双入口：Demand 详情（Step1） + Procurement 详情（8步） |
| v1.6.0 兼容性 | ✅ 修复了前端 Step1 卡片显示 bug |
| v1.3.0 Demand 中心化 | ✅ 新建需求单立即出现在 Overview |
| 缺口可接受性 | ⚠️ JapanCustomsRecord 缺少 subProductCode（影响小，可延后修复）|

### 立即可修复项

1. **前端 Bug**（已修复）：Step1 卡片 `quantity`/`destination` → `subProductItemsSummary`
2. **JapanCustomsRecord 补充 subProductCode**（P2，可选）：需 ALTER TABLE + 后端字段 + Assembler 填充

### 文档更新

- SPEC-B09-IMPLEMENTATION.md：v1.3.0 双入口架构，Phase 5 已完成
- SPEC-B09-全链路子货号追踪分析.md：本文件，新增 v1.3.0 架构变更章节
- SPEC-B01：补充转换链路说明（转采购循环 + linkedDemandItemsRaw 双向关联）
