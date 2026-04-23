# 调配计划 — 规格文档

> **版本**: 1.0.0
> **创建**: 2026-04-21
> **状态**: 设计中
> **依据**: `SPEC-B04-调配计划-步骤4.md` §2
> **前置依赖**: Procurement ✅ 已实现 · QcRecord 🔴 验货记录（可并行）

---

## 1. 业务背景

调配计划（LogisticsPlan）是业务流第四步。验货完成后，安排货物的运输方式（海运/空运/拼柜），生成调配记录。调配类型决定了后续报关和运输的流程差异。

**三种调配路径：**

| 类型 | 代码 | 触发条件 | 说明 |
|------|------|----------|------|
| 海运 | `SEA` | 体积/重量超标 | 検品后体积重量超出空运标准 |
| 空运 | `AIR` | 体积/重量达标 | 軽量の صغيرة货，优先空运 |
| 拼柜 | `CONSOLIDATION` | 多张小单合并 | 不同采购单合并入同一货柜，共享舱位 |

**装箱优先级规则：**
- 同一工厂、同一天出货的货物优先拼柜
- 拼柜池（ConsolidationPool）管理待拼货物
- 拼柜装满后自动生成 Container（货柜）记录

---

## 2. 实体定义

### 2.1 LogisticsPlan 聚合根

| 字段 | 来源 | 必填 | 说明 |
|------|------|------|------|
| `id` | 系统 | — | 主键 |
| `planCode` | 系统生成 | — | 格式：`L-YYYYMMDD-NNN`（如 L-20260421-001） |
| `procurementId` | 用户选择 | | 关联采购单（FK → procurement.id）；拼柜时可为空 |
| `factoryId` | 自动带入 | | 工厂ID（来自 Procurement） |
| `productCode` | 自动带入 | ✅ | 主货号（来自 Procurement） |
| `subProductCode` | 自动带入 | | 子货号（来自 Procurement） |
| `cargoLengthCm` | 用户输入 | | 货物长度(cm) |
| `cargoWidthCm` | 用户输入 | | 货物宽度(cm) |
| `cargoHeightCm` | 用户输入 | | 货物高度(cm) |
| `cargoVolumeCbm` | 用户计算 | | 体积(m³) = 长×宽×高 / 1,000,000 |
| `cargoWeightKg` | 用户输入 | | 货物重量(kg) |
| `quantity` | 自动带入 | | 数量（来自 Procurement） |
| `requiresQc` | 自动带入 | | 是否需要检测（来自 Procurement） |
| `planType` | 用户选择 | ✅ | 调配类型：`SEA`（海运）/ `AIR`（空运）/ `CONSOLIDATION`（拼柜） |
| `estimatedShipDate` | 用户输入 | | 预计发货日 |
| `actualShipDate` | 用户输入 | | 实际发货日 |
| `containerId` | 装柜后赋值 | | 货柜编号（装柜后关联 container.id） |
| `poolId` | 加入拼柜池 | | 所属拼柜池ID（consolidation_pool.id） |
| `status` | 系统 | ✅ | 状态：`PLANNED` → `BOOKED` → `IN_TRANSIT` → `DELIVERED` |
| `remarks` | 用户输入 | | 备注 |

### 2.2 Container 货柜（第四步）

| 字段 | 来源 | 必填 | 说明 |
|------|------|------|------|
| `id` | 系统 | — | 主键 |
| `containerCode` | 系统生成 | — | 格式：`C-YYYYMMDD-NNN` |
| `containerNo` | 用户输入 | ✅ | 货柜号（船公司提供，如 TEMU1234567） |
| `containerType` | 用户选择 | ✅ | `20GP` / `40GP` / `40HC` / `45HC` |
| `sealNo` | 用户输入 | | 封条号 |
| `planType` | 用户选择 | ✅ | 调配类型：`SEA` / `AIR` |
| `estimatedDepartureDate` | 用户输入 | | 预计开船日 |
| `actualDepartureDate` | 用户输入 | | 实际开船日 |
| `estimatedArrivalDate` | 用户输入 | | 预计到港日 |
| `actualArrivalDate` | 用户输入 | | 实际到港日 |
| `departurePort` | 用户输入 | | 起运港（如 宁波/上海） |
| `arrivalPort` | 用户输入 | | 目的港（如 東京/大阪） |
| `status` | 系统 | | `LOADING` → `DEPARTED` → `ARRIVED` → `DELIVERED` |
| `remarks` | 用户输入 | | 备注 |

### 2.3 ConsolidationPool 拼柜池（第四步）

| 字段 | 来源 | 必填 | 说明 |
|------|------|------|------|
| `id` | 系统 | — | 主键 |
| `poolCode` | 系统生成 | — | 格式：`P-YYYYMMDD-NNN` |
| `destination` | 用户输入 | ✅ | 目的地（同一目的港的货物合并） |
| `departurePort` | 用户输入 | | 起运港 |
| `arrivalPort` | 用户输入 | | 目的港 |
| `totalWeight` | 自动计算 | | 总重量(kg) = SUM(货物重量) |
| `totalVolume` | 自动计算 | | 总体积(m³) = SUM(货物体积) |
| `planType` | 用户选择 | | 默认 CONSOLIDATION |
| `containerId` | 装柜后赋值 | | 分配货柜ID |
| `status` | 系统 | | `PENDING` → `LOADING` → `LOADED` → `DEPARTED` → `ARRIVED` |
| `remarks` | 用户输入 | | 备注 |

---

## 3. 状态机

### 3.1 LogisticsPlan 状态

```
PLANNED（已创建）
    ↓ 订舱
BOOKED（已订舱）
    ↓ 装柜 + 开船
IN_TRANSIT（运输中）
    ↓ 到港 + 清关
DELIVERED（已送达）[终态]
```

| 当前状态 | 触发动作 | 下一状态 |
|----------|----------|----------|
| PLANNED | 订舱 | BOOKED |
| BOOKED | 实际发货 | IN_TRANSIT |
| IN_TRANSIT | 实际到货 | DELIVERED |

### 3.2 Container 状态

```
LOADING（装箱中）
    ↓ 开船
DEPARTED（已开船）
    ↓ 到港
ARRIVED（已到港）
    ↓ 送达
DELIVERED（已送达）[终态]
```

---

## 4. 编号生成规则

与 ReplenishmentDemand/QcRecord 相同：
- 调配：`L-YYYYMMDD-NNN`
- 货柜：`C-YYYYMMDD-NNN`
- 拼柜池：`P-YYYYMMDD-NNN`

---

## 5. 数据联动规则

| 事件 | 自动带入 |
|------|----------|
| 选择采购单 | factoryId、productCode、subProductCode、quantity、requiresQc |
| 选择调配类型 | 更新 status → PLANNED |
| 装柜完成 | containerId 赋值，status → IN_TRANSIT |
| 拼柜合并 | 加入 consolidationPool.totalWeight / totalVolume |

---

## 6. API 设计（后端）

### 6.1 LogisticsPlan
```
GET  /api/v1/logistics-plans?page=0&pageSize=20&planType=&productCode=&status=
GET  /api/v1/logistics-plans/{id}
POST /api/v1/logistics-plans
PATCH /api/v1/logistics-plans/{id}
DELETE /api/v1/logistics-plans/{id}
```

### 6.2 Container
```
GET  /api/v1/containers?page=0&pageSize=20&status=
POST /api/v1/containers
PATCH /api/v1/containers/{id}
```

### 6.3 ConsolidationPool
```
GET  /api/v1/consolidation-pools?page=0&pageSize=20&status=
POST /api/v1/consolidation-pools
POST /api/v1/consolidation-pools/{poolId}/items   # 添加货物到拼柜池
DELETE /api/v1/consolidation-pools/{poolId}/items/{itemId}
```

---

## 7. 前端页面（LogisticsPage.vue）

### 7.1 列表页

- **路由**: `/procurement/logistics`
- **统计卡**: 调配中(PLANNED) / 已订舱(BOOKED) / 运输中(IN_TRANSIT) / 已送达(DELIVERED)
- **筛选**: 调配类型、货号、工厂名、状态
- **表格列**: 编号、货号、工厂名、调配类型、货物尺寸、重量、验货、状态、操作
- **操作**: 详情（抽屉）

### 7.2 新增调配弹窗

关联采购单选择 → 自动带入字段 → 用户补充调配类型和日期信息。

---

## 8. 代码实现清单

> 🔴 待实现  ✅ 已实现

- [ ] 🔴 `logistics_plan` 数据库表（含 BaseEntity 审计字段）
- [ ] 🔴 `container` 数据库表
- [ ] 🔴 `consolidation_pool` 数据库表
- [ ] 🔴 `consolidation_pool_item` 数据库表（拼柜池明细）
- [x] ✅ `LogisticsPlan` 实体（domain/model/LogisticsPlan.java）
- [ ] 🔴 `Container` 实体
- [ ] 🔴 `ConsolidationPool` 实体 + `ConsolidationPoolItem`
- [x] ✅ `LogisticsStatus` 枚举（PLANNED / BOOKED / IN_TRANSIT / DELIVERED）
- [ ] 🔴 `ContainerStatus` 枚举
- [x] ✅ `PlanType` 枚举（SEA / AIR / CONSOLIDATION）
- [x] ✅ Repository 接口（LogisticsPlanRepository.java）
- [x] ✅ Assembler 转换器（LogisticsPlanAssembler.java）
- [x] ✅ UseCase（LogisticsPlanUseCase.java）
- [x] ✅ Controller（LogisticsController.java）
- [x] ✅ `@/api/logistics.ts` 前端 API 客户端
- [x] ✅ `LogisticsPage.vue` 列表页（接真实 API，含 factoryId 列）
- [x] ✅ 新增调配弹窗
- [x] ✅ 调配详情抽屉（含 factoryId）
- [x] ✅ SPEC 进度更新至 `SPEC-B04-调配计划-步骤4.md`
- [x] ✅ 单元测试（LogisticsPlanUseCaseTest，12 个测试用例）
