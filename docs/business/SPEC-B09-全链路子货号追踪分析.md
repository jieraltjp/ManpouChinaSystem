# SPEC-B09-EX — 全链路子货号追踪分析

> **版本**: 2.0.0
> **创建**: 2026-04-24
> **更新**: 2026-04-27（v2.0.0：整篇重构 — JSON 批量模型废除，改为 1 Demand = 1 Procurement 直接字段模型）
> **状态**: ✅ 已同步（v2.0.0 架构）
> **背景**: v2.0.0 完成 Demand subProductItems JSON → 直接字段改造，每条 Demand 对应一个子货号和一条 Procurement。全链路子货号精确追踪。

---

## 1. 业务背景（v2.0.0）

v2.0.0 核心变更：需求单（ReplenishmentDemand）从 JSON 数组 `subProductItems[]` 改为**直接字段**：
- `subProductCode` — 子货号全码（如 `ad009-be`）
- `quantity` — 需求数量
- `destination` — 目的地

转采购时 **1:1 映射**：每条 Demand 创建一个 Procurement，不再循环生成多条。

**用户意图**：逐条录入子货号需求 → 系统内部逐条独立流转 → 全链路精确到子货号

---

## 2. 全链路追踪分析（v2.0.0）

### 链路总图

```
ReplenishmentDemand（1条，含 subProductCode / quantity / destination）
  │
  │  [转采购 — ReplenishmentDemandUseCase.convertToProcurement()]
  │  1 Demand → 1 Procurement，直接字段传递
  ▼
Procurement（1条，subProductCode 直接继承自 Demand）
  │
  ├─→ [事件驱动] LogisticsInTransitEvent
  │         ▼
  │     DomesticCustomsRecord × N（有 subProductCode）✓
  │         ▼
  │     LogisticsPlan × N（手动创建，有 subProductCode）✓
  │         ▼
  │     JapanCustomsRecord × N（有 subProductCode）✓
  │         ▼
  │     [事件驱动] JapanCustomsClearedEvent
  │         ▼
  │     SalesRecord × N（有 subProductCode）✓
```

### 每步详情

| 步骤 | 实体 | subProductCode | 触发方式 | 链路完整 | 说明 |
|------|------|:---:|-----------|:---:|------|
| 1 需求单 | ReplenishmentDemand | ✅ 直接字段 | 手动录入 | ✅ | v2.0.0：直接字段，非 JSON |
| 2 发注单 | Procurement | ✅ 直接字段 | 转采购（1:1） | ✅ | `linkedProcurementId` 建立 Demand → Procurement 关联 |
| 3 验货 | QcRecord | ✅ | 手动创建 | ✅ | `procurementId` 精确追溯 |
| 4 调配计划 | LogisticsPlan | ✅ | 手动创建 | ✅ | `procurementId` 精确关联 |
| 5 国内报关 | DomesticCustomsRecord | ✅ | 物流在途事件自动创建 | ✅ | `LogisticsInTransitEventListener` 逐条创建 |
| 6 日本清关 | JapanCustomsRecord | ✅ | 手动创建 | ✅ | v1.6.1 已补全 subProductCode |
| 7 退税 | TaxRefundRecord | — | 手动创建 | ✅ | 财务维度，按采购单总额退税 |
| 8 销售记录 | SalesRecord | ✅ | 日本清关完成事件自动创建 | ✅ | 从 Procurement 继承 subProductCode |

### 步骤详解

#### 步骤2：发注单 ✅

**实体**：`Procurement`，`subProductCode` 字段
**触发**：转采购时 1:1 创建

```java
// ReplenishmentDemandUseCase.convertToProcurement()
ProcurementCreateCmd pCmd = new ProcurementCreateCmd();
pCmd.setSubProductCode(demand.getSubProductCode());   // ✓ 直接传递
pCmd.setQuantity(demand.getQuantity());
pCmd.setDestination(demand.getDestination());
procurementUseCase.create(pCmd);
```

`ReplenishmentDemand.linkedProcurementId` 在创建后填充，建立 Demand → Procurement 反向关联。

#### 步骤5：国内报关 ✅

**实体**：`DomesticCustomsRecord`，`subProductCode` 字段
**触发**：`LogisticsPlanInTransitEventListener`（TransactionalEventListener AFTER_COMMIT）

```java
public void onLogisticsInTransit(LogisticsPlanInTransitEvent evt) {
    DomesticCustomsRecord record = new DomesticCustomsRecord();
    record.setProcurementId(evt.getProcurementId());
    record.setSubProductCode(evt.getSubProductCode());  // ✓ 精确传递
    record.setProductCode(evt.getProductCode());
}
```

#### 步骤4：调配计划 ✅

**实体**：`LogisticsPlan`，`subProductCode` 字段
**触发**：手动创建（`LogisticsPlanUseCase.create()`）

#### 步骤6：日本清关 ✅（v1.6.1 已补全）

**实体**：`JapanCustomsRecord`，有 `subProductCode` 字段（v1.6.1 新增）
**触发**：手动创建（`JapanCustomsController` → `JapanCustomsUseCase.create()`）
**来源**：清关员从 Procurement 复制 subProductCode 填入

#### 步骤8：销售记录 ✅

**实体**：`SalesRecord`，`subProductCode` 字段
**触发**：`JapanCustomsClearedEventListener`（TransactionalEventListener AFTER_COMMIT）

```java
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
URL: /base/overview/procurement/{procurementId}
```

`OrderOverviewUseCase.getOverview(procurementId)` 以单条 Procurement 为锚点，聚合完整下游链路。

### 3.2 v2.0.0 影响：1 Demand = 1 Procurement

每个 Demand 转化后对应一条 Procurement，每个 Procurement 有独立 Overview 页面：

```
ReplenishmentDemand(ad009-be, 100件, 久留米)
  └── Procurement(ad009-be)  → /base/overview/procurement/101
```

**结论**：Overview 列表页每行自然对应一条子货号，无需额外聚合逻辑。

### 3.3 现状：前端 Step1 卡片适配

**字段映射**（v2.0.0）：`DemandVO` 直接暴露 `subProductCode` / `quantity` / `destination` 字段，前端 Step1 卡片直接渲染：

```
subProductCode: ad009-be
quantity: 100
destination: 久留米
```

---

## 4. 缺口修复方案

### 4.1 ✅ JapanCustomsRecord subProductCode（v1.6.1 已实现）

- `JapanCustomsRecord.java`：`subProductCode` 字段
- `JapanCustomsCreateCmd.java` / `JapanCustomsUpdateCmd.java`：新增 `subProductCode`
- `JapanCustomsAssembler.java`：copyCreate / copyUpdate / toDto 映射
- `V29__japan_customs_sub_product_code.sql`：DB migration

### 4.2 体验优化：Demand 详情页展示同需求单关联（已归档）

> 方案 A/B 均因 v2.0.0 1:1 映射而不再需要，改为 Demand 列表自然对应子货号。

---

## 5. 批量 UX 验证（v2.0.0）

| 用户操作 | 系统内部行为 | 用户感知 |
|---------|------------|---------|
| 需求单录入1个子货号 | 存1条 Demand + 1条 Procurement | 一次录入 ✓ |
| 点"转采购" | 创建1条 Procurement | 一次操作 ✓ |
| 查看 Overview | 每条 Procurement 单独一条 Overview | 自然分开 ✓ |
| 调配计划发货 | 手动创建 LogisticsPlan | 操作可选 ✓ |
| 国内报关 | 自动创建 DomesticCustomsRecord | 无感知 ✓ |
| 日本清关 | 手动创建 JapanCustomsRecord | 操作可选 ✓ |
| 销售记录 | 自动创建 SalesRecord | 无感知 ✓ |

**结论**：v2.0.0 UX 更清晰——每条子货号独立流转，无批量循环逻辑。

---

## 6. v1.3.0 架构：Overview Demand 中心化

### 双入口设计

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
4. Step1 有数据（demandCode、subProductCode、quantity、destination）
   Step2-8 全部"未开始"
   进度条：步骤1=绿色，步骤2-8=灰色
      ↓
5. 用户点击"转采购"（需选工厂）
      ↓
6. 创建 1 条 Procurement（v2.0.0 1:1）
   Demand 状态 → CONVERTED
   自动跳转到 /base/overview/procurement/:firstId
      ↓ 详情页
7. Step1=CONVERTED，Step2 有数据（发注单）
   Step3-8 按实际流程逐步填写
```

---

## 7. 结论

### 链路完整性（v2.0.0）

| 评估维度 | 结论 |
|---------|------|
| 子货号分开追踪 | ✅ 全链路所有步骤均精确到子货号（v1.6.1 JapanCustoms 已补全）|
| 1:1 UX | ✅ 每条 Demand → Procurement 自然独立，无批量循环 |
| Overview 锚点正确性 | ✅ 双入口：Demand 详情（Step1） + Procurement 详情（8步） |
| Demand 中心化 | ✅ 新建需求单立即出现在 Overview |

### 文档更新

- SPEC-B09-IMPLEMENTATION.md：v2.0.0 数据模型，API 路径已同步
- SPEC-B09-全链路子货号追踪分析.md：本文件，v2.0.0 架构已同步
