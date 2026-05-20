# SPEC-B13 — 直接采购功能设计

> **版本**: 1.0.0（初稿）
> **创建**: 2026-05-20
> **状态**: 📋 设计中
> **业务背景**: 用户在发注单页面创建采购时，存在两种来源：① 从已有补货需求关联（`FROM_DEMAND`）② 无补货直接下单（`DIRECT`）。当前系统虽支持不选需求创建采购，但 UI 无明确区分，不便于统计和管理。

---

## 1. 现状分析

### 1.1 当前能力

| 维度 | 现状 |
|------|------|
| 后端支持 | ✅ `ProcurementCreateCmd` 无 `demandId` 必填字段，`ProcurementUseCase.create()` 不依赖 demand，可直接创建 |
| 前端支持 | ⚠️ `ProcurementPage.vue` 需求选择器为 `clearable`（可选），但无"直接采购"显式入口 |
| 数据区分 | ❌ `Procurement` 实体无来源类型字段，无法区分"直接采购"和"关联需求采购" |
| 列表筛选 | ❌ 发注列表无来源类型筛选项 |

### 1.2 问题

1. **语义不明确**：用户不选需求时，采购属于"无主"状态，没有明确标识
2. **数据无法统计**：无法从数据层面区分"直接采购"与"需求转化采购"的数量/金额
3. **操作入口不清晰**：需要"直接采购"的用户不知道应该不选需求，还是要有一个明确的操作入口

---

## 2. 业务建模

### 2.1 ProcurementType 枚举（新增）

```java
public enum ProcurementType {
    /**
     * 直接采购：无补货需求，直接创建采购单。
     * 通常用于试单、新品、不依赖库存预测的场景。
     */
    DIRECT,

    /**
     * 需求采购：由补货需求（ReplenishmentDemand）转化而来。
     * 关联 demand_id，可追溯需求来源。
     */
    FROM_DEMAND
}
```

### 2.2 Procurement Entity 变更（新增字段）

在 `Procurement.java` 中新增：

```java
/**
 * 采购类型。
 * DIRECT        = 无需求直接采购
 * FROM_DEMAND  = 由补货需求转化
 */
private ProcurementType procurementType;  // 默认 DIRECT

/**
 * 关联的补货需求ID（FROM_DEMAND 时必填）。
 * 不做外键约束，业务层保证一致性。
 */
private Long demandId;
```

### 2.3 数据库变更（Flyway）

```sql
-- 新增采购类型和需求ID字段
ALTER TABLE procurement
  ADD COLUMN procurement_type VARCHAR(16) NOT NULL DEFAULT 'DIRECT' AFTER status,
  ADD COLUMN demand_id BIGINT DEFAULT NULL AFTER procurement_type,
  ADD INDEX idx_procurement_demand_id (demand_id),
  ADD INDEX idx_procurement_type (procurement_type);

-- 为历史数据补值（默认为 DIRECT，保持向后兼容）
UPDATE procurement SET procurement_type = 'DIRECT' WHERE procurement_type IS NULL;
```

> 迁移版本号待定（需确认 V18 是否已占用）

---

## 3. API 设计

### 3.1 ProcurementCreateCmd 新增字段

```java
/**
 * 采购类型。
 * 不传时后端默认为 DIRECT。
 */
private ProcurementType procurementType;

/**
 * 关联需求ID。
 * procurementType == FROM_DEMAND 时必填。
 */
private Long demandId;
```

**校验规则**：
- `procurementType == FROM_DEMAND` 时，`demandId` 必填
- `procurementType == DIRECT` 时，`demandId` 必须为空
- `demandId` 对应的 `ReplenishmentDemand.status` 必须为 `PENDING`（不允许重复关联）

### 3.2 ProcurementUpdateCmd 新增字段

```java
private ProcurementType procurementType;
// demandId 在 update 时不允许修改（防止业务逻辑混乱）
```

### 3.3 ProcurementVO（列表/响应）新增字段

```java
private ProcurementType procurementType;  // 列表展示用
private String demandCode;                 // 关联需求的单号（FROM_DEMAND 时有值）
```

### 3.4 ProcurementController 变更

现有端点不变，请求体新增字段：

```
POST   /api/v1/procurements
  Body: {
    ...,          // 现有字段
    "procurementType": "FROM_DEMAND",  // 可选，默认 "DIRECT"
    "demandId": 123                     // FROM_DEMAND 时必填
  }

GET    /api/v1/procurements
  Query: ..., &procurementType=DIRECT   // 新增筛选参数
```

---

## 4. 前端设计

### 4.1 新建弹窗 — 采购类型选择

```
┌─────────────────────────────────────────────────────┐
│  新建发注单                                      [×] │
├─────────────────────────────────────────────────────┤
│  采购类型:  ○ 直接采购   ● 从需求关联              │
│           ───────────────────────────────           │
│  关联需求: [下拉选择需求 ▼]  (仅"从需求关联"显示)  │
│                                                     │
│  关联需求说明:  选择后将自动代入：商品信息/目的地/担当 │
│                                                     │
│  [--- 下方为现有表单字段（不变）---]               │
└─────────────────────────────────────────────────────┘
```

**交互逻辑**：

| 操作 | 效果 |
|------|------|
| 选择"直接采购" | 清空 `selectedDemandId`，隐藏需求选择器 |
| 选择"从需求关联" | 显示需求选择器，必须选择一个 PENDING 状态的需求 |
| 选择需求后 | 自动代入：`productCode`、`subProductCode`、`destination`、`japanLead`（现有逻辑） |
| 编辑已有采购 | 显示类型（只读），demandId 不允许修改 |

### 4.2 列表筛选 — 新增类型筛选项

```
筛选栏新增：
  采购类型: [全部 ▼] [直接采购 ▼] [从需求关联 ▼]
```

### 4.3 列表列 — 新增来源标识

```
在表格中新增一列"来源"：
  - DIRECT      → 显示标签 [直接采购]（蓝色）
  - FROM_DEMAND → 显示标签 [需求: DM-xxx]（绿色，点击跳转 DemandOverview）
```

### 4.4 详情抽屉

```
来源类型: 直接采购 / 从需求关联
需求单号: DM-20260520-001  （仅 FROM_DEMAND 有值）
```

---

## 5. 业务规则

### 5.1 创建规则

| 场景 | procurementType | demandId | 需求状态 |
|------|----------------|----------|----------|
| 直接创建采购 | DIRECT | null | — |
| 从 PENDING 需求关联 | FROM_DEMAND | 有效 ID | PENDING |
| 从已 CONFIRMED 需求关联 | — | — | ❌ 不允许（需求已关联其他采购） |

### 5.2 状态联动

- `Procurement` 创建并 `procurementType=FROM_DEMAND` 时：
  - 同时创建 `DemandProcurementMapping` 记录（`demand_id`、`procurement_id`、`allocated_quantity = procurement.quantity`、`status = 进行中`）
  - 调用 `demand.markAsLinked(procurementId)` 将需求状态置为 `CONFIRMED`
- `Procurement` 删除时：
  - 若 `procurementType=FROM_DEMAND`，则 `demand.unlinkProcurement()`（状态恢复 PENDING）
  - 删除关联的 `DemandProcurementMapping`（status → 已取消）

### 5.3 统计口径

| 指标 | 口径 |
|------|------|
| 直接采购数 | `COUNT(*) WHERE procurement_type = 'DIRECT'` |
| 需求采购数 | `COUNT(*) WHERE procurement_type = 'FROM_DEMAND'` |
| 需求转化率 | `FROM_DEMAND / TOTAL` |

---

## 6. 文件变更清单

### 后端

| 文件 | 变更类型 |
|------|----------|
| `procurement/domain/model/Procurement.java` | 新增 `procurementType`、`demandId` 字段 |
| `procurement/domain/model/ShipmentStatus.java` | 不变 |
| `procurement/application/dto/ProcurementType.java` | **新增**枚举类 |
| `procurement/application/dto/ProcurementCreateCmd.java` | 新增 `procurementType`、`demandId` 字段 + 校验 |
| `procurement/application/dto/ProcurementUpdateCmd.java` | 新增 `procurementType` 字段 |
| `procurement/application/dto/ProcurementVO.java` | 新增 `procurementType`、`demandCode` 字段 |
| `procurement/application/dto/ProcurementPageVO.java` | 不变 |
| `procurement/application/dto/ProcurementPageQuery.java` | 新增 `procurementType` 筛选字段 |
| `procurement/application/usecase/ProcurementUseCase.java` | 创建时校验 + 关联需求逻辑 |
| `procurement/application/assembler/ProcurementAssembler.java` | 新增字段映射 |
| `procurement/interfaces/controller/ProcurementController.java` | 新增 `procurementType` 参数透传 |
| `procurement/interfaces/controller/ProcurementController.java` | GET 列表新增 `procurementType` 查询参数 |
| `replenishment/application/usecase/ReplenishmentDemandUseCase.java` | 新增 `unlinkProcurement()` 方法 |
| `replenishment/domain/model/ReplenishmentDemand.java` | 新增 `unlinkProcurement()` 领域方法 |
| `replenishment/application/dto/DemandProcurementMappingService.java` | 新增 `cancelByProcurementId()` 方法 |
| `db/migration/V19__procurement_type_and_demand_id.sql` | **新增** Flyway 迁移 |

### 前端

| 文件 | 变更类型 |
|------|----------|
| `api/procurement.ts` — `ProcurementQuery` | 新增 `procurementType` |
| `api/procurement.ts` — `ProcurementCreateCmd` | 新增 `procurementType`、`demandId` |
| `api/procurement.ts` — `ProcurementVO` | 新增 `procurementType`、`demandCode` |
| `pages/procurement/ProcurementPage.vue` | 弹窗：采购类型单选 + 条件显示需求选择器；表格：来源列；筛选：类型筛选 |
| `locales/zh.json` | 新增 `order.procurementType.direct` / `order.procurementType.fromDemand` 等 |
| `locales/ja.json` | 同上 |

### 文档

| 文件 | 变更类型 |
|------|----------|
| `SPEC-B02-发注单-步骤2.md` | 更新 Procurement Entity（新增字段） + API 变更 |
| `docs/database/DB-02-procurement-order.md` | 更新表结构 DDL |
| `docs/ui/pages/02-procurement.md` | 更新新建弹窗 UI + 列表列 |

---

## 7. 实施计划

### Phase 1（后端核心）
1. 新增 `ProcurementType` 枚举
2. `Procurement` Entity 新增字段
3. `ProcurementCreateCmd` / `ProcurementUpdateCmd` / `ProcurementVO` 新增字段 + 校验
4. `ProcurementPageQuery` 新增筛选字段
5. `ProcurementAssembler` 字段映射
6. `ProcurementUseCase` 创建逻辑变更（FROM_DEMAND 时关联 demand + mapping）
7. `ReplenishmentDemand.unlinkProcurement()` 领域方法
8. `DemandProcurementMappingService.cancelByProcurementId()`
9. Flyway 迁移脚本
10. 编译验证

### Phase 2（前端）
1. API 类型更新
2. 弹窗采购类型选择（单选 + 条件显示）
3. 列表新增来源列
4. 筛选栏新增类型筛选
5. 详情抽屉来源信息
6. i18n 补充
7. TypeScript 编译通过

### Phase 3（验证）
1. 直接采购创建 → `procurement_type=DIRECT`, `demand_id=NULL`
2. 从需求关联采购 → `procurement_type=FROM_DEMAND`, `demand_id=有效`, `demand.status=CONFIRMED`
3. 删除 FROM_DEMAND 采购 → 需求恢复 PENDING
4. 列表按类型筛选正常
5. 统计数字准确

---

## 8. 风险与决策点

| 问题 | 建议 |
|------|------|
| 历史数据 `procurement_type` 默认 `DIRECT` 是否合理？ | ✅ 是——历史采购均无需求关联，按 DIRECT 处理符合业务认知 |
| `demandId` 是否做外键约束？ | ❌ 否——`demand_procurement_mapping` 已有 N:1 追踪，外键会导致需求删除困难；业务层校验足够 |
| 编辑时是否允许修改采购类型？ | ❌ 否——类型一旦确定不允许变更（防止业务逻辑混乱） |
| 一个需求能否关联多个采购？ | ✅ 是——`DemandProcurementMapping` 支持 N:1；数量分配由 `allocated_quantity` 追踪 |
