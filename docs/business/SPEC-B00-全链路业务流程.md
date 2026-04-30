# SPEC-B00 — 全链路业务流程（完整版）

> **版本**: 1.0.0
> **创建**: 2026-04-30
> **状态**: ✅ 分析完成
> **关联**: SPEC-B01 · SPEC-B02 · SPEC-B03 · SPEC-B04 · SPEC-B05 · SPEC-B06 · SPEC-B07 · SPEC-B08 · SPEC-B10 · SPEC-B11

---

## 1. 全链路总图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          商品（Product / ProductFactory）                    │
│  masterCode + subCode ←── OEM / ORDINARY / FACTORY_DIRECT              │
│  工厂关联（多对多）←── 新品先在商品模块建品，老品直接用                 │
└─────────────────────────────────────────────────────────────────────────┘

                              ┌──────────────────┐
                              │   步骤1 补货需求   │  ReplenishmentDemand
                              │   Demand         │  REPLENISHMENT / NEW_PURCHASE
                              └──────┬───────────┘
                                     │ 1:N
                    ┌────────────────┼────────────────┐
                    ▼                ▼                ▼
             Demand → Procurement   Demand → Procurement   Demand → Procurement
             (老品)              (老品·多采购)       (新品·需先建商品)
                    │                │                │
                    └────────┬───────┴────────┬───────┘
                             │                │
                    ┌────────▼────────────────▼──────────────────┐
                    │           步骤2 下单采购 Procurement          │
                    │  OEM / 普货 / 厂家直销 · 老工厂 / 新工厂      │
                    │  单需求 → 多采购单（DemandProcurementMapping）│
                    └────────┬──────────────────────────────────┘
                             │ 1:N
                    ┌────────▼──────────────────────────────────┐
                    │       步骤2.5 出货批次 ShipmentBatch       │
                    │  单采购 → 多批次出货 · 手动登记           │
                    │  SUM可超额（遗留库存）                     │
                    └────────┬──────────────────────────────────┘
                             │ 1:N（关联）
┌─────────────────────────────▼─────────────────────────────┐
│                  步骤3 验货 QcRecord                       │
│  关联出货批次 · PASS/FAIL · inspectionCount / passedCount│
└────────────────────┬──────────────────────────────────────┘
                     │
     ┌───────────────┼───────────────────────────────┐
     │               │                               │
     ▼               ▼                               ▼
┌─────────┐  ┌────────────┐  ┌─────────────────────────┐
│  PASS   │  │   FAIL     │  │  PASS + volume≤0.5m³   │
│  合格品 │  │  不良品     │  │  → エア便（空运）        │
└────┬────┘  └────────────┘  └──────────┬──────────────┘
     │                                        │
     ▼                                        ▼
┌─────────────────────────────────┐  ┌─────────────────────────┐
│          步骤4 调配 LogisticsPlan  │  │  ステップ4 厂家直销     │
│                                  │  │  メーカー直送          │
│  ┌─────────────────────────────┐ │  │  QC后直送日本          │
│  │  路径A 永康仓               │ │  └──────────┬──────────┘
│  │  → 入永康仓                 │ │             │
│  │  → 配货/出库                │ │             │ 直接去日本
│  └──────────┬──────────────────┘ │             ▼
│             │                    │     ┌───────────────────┐
│             ▼                    │     │ 步骤6 日本清关     │
│  ┌──────────────────────────┐  │     │ JapanCustomsRecord│
│  │  路径B 拼柜               │  │     └──────────┬────────┘
│  │  ConsolidationPool        │  │                │
│  │  → 多批货物汇入          │  │                ▼
│  │  → 凑满一个柜（≈70m³）   │  │     ┌───────────────────┐
│  │  → 装柜 Container        │  │     │ 步骤7 退税          │
│  │  → 出口                   │  │     │ TaxRefundRecord    │
│  └──────────┬───────────────┘  │     │ billingType决定   │
│             │                  │     │ 退税金额           │
│             ▼                  │     └──────────┬────────┘
│     ┌──────────────┐           │                │
│     │ 步骤5 国内报关 │          │                ▼
│     │ DomesticCustoms│          │     ┌───────────────────┐
│     │ containerNo    │          │     │ 步骤8 上架运营      │
│     └───────┬───────┘           │     │ SalesRecord       │
│             │  CLEARED         │     │ listingDate      │
│             ▼                   │     │ Amazon/Mercari   │
│     ┌──────────────┐           │     │ 低库存→触发步骤1  │
│     │ 步骤6 日本清关 │          │     └───────────────────┘
│     │ JapanCustoms  │          │
│     └──────────────┘           │
└─────────────────────────────────┘
```

---

## 2. 十一步骤详解

### 步骤0：商品 & 工厂（先行准备）

```
目的：建立商品主数据库和工厂关系
触发：新品引入时（NEW_PURCHASE）
```

| 子步骤 | 操作 | 实体 |
|--------|------|------|
| 0.1 | 新品在商品模块建品（主货号+子货号），老品直接用 | Product |
| 0.2 | 建立商品-工厂关联（哪些工厂能生产此商品） | ProductFactory |
| 0.3 | 新工厂在工厂模块建档，老工厂直接选 | Factory |

**Product → ProductCategory**：

| 分类 | 含义 | 后续流程影响 |
|------|------|-------------|
| `OEM` | 定制产品，需单独开模 | 走永康仓路径 |
| `ORDINARY` | 普通商品，有库存 | 可走拼柜路径 |
| `FACTORY_DIRECT` | 厂家直供 | QC后直送日本，绕过永康仓和拼柜 |

**ProductFactory**（多对多）：

```
Product (masterCode+subCode)
  └── ProductFactory（per factory）
        ├── supplierSku（供应商内部货号）
        ├── unitPriceRmb（此工厂对此商品的含税单价）
        ├── moq（最小起订量）
        ├── leadTimeDays（交货周期）
        └── isPreferred（首选供应商）
```

---

### 步骤1：补货需求（Demand）

```
目的：记录「需要补什么货、补多少」
触发：运营人员手动创建 / 低库存自动触发（步骤8反馈）
```

**DemandType**：

| 类型 | 含义 | 后续流程 |
|------|------|----------|
| `REPLENISHMENT` | 老品补货 | 商品和工厂关系已存在，直接建需求 |
| `NEW_PURCHASE` | 新品采购 | 需先完成步骤0（建商品+工厂） |

**DemandStatus FSM**：

```
PENDING（待确认） ──[关联采购单]──▶ CONFIRMED（已确认）
       │                                  │
       └──[取消/删除]──▶ CANCELLED（已取消）
```

**核心字段**：`productCode` + `subProductCode` → 关联 Product；`quantity` → 需求量；`destination` → 目的地（久留米/名古屋/大阪等）

**关键设计**：Demand 不生成 Procurement，由 Procurement 主动关联 Demand（反向关联）。`DemandProcurementMapping` 支持一需求对多采购（1:N）。

---

### 步骤2：下单采购（Procurement）

```
目的：向工厂下采购单
触发：Demand 确认后（或运营直接建采购单）
```

**ProductCategory → 发注路径**：

| 分类 | 发注路径 | 特点 |
|------|---------|------|
| `OEM` | 永康路径 | 厂家→永康仓→配货→报关 |
| `ORDINARY` | 永康路径 或 拼柜路径 | 看货物量和体积 |
| `FACTORY_DIRECT` | 直送路径 | 厂家→QC→直送日本，绕过永康仓 |

**BillingType（报关类型）**：

| 类型 | 含义 | 退税影响 |
|------|------|----------|
| `ZHE_LU_KAI_PIAO` | 浙鲁开票 | 有退税（Zhejiang/Shandong invoice） |
| `CHAO_HUI_TUI_SHUI` | 超慧退税 | 有退税（特殊渠道） |
| `NO_REFUND` | 不退税 | 无退税 |
| `OTHER` | 其他 | 待定 |

**ShipmentStatus FSM（核心段）**：

```
未定
  │
  ├──[建单]──▶ 発注待
  │              │
  │              ├─[永康路径]──▶ 永康
  │              ├─[直送路径]──▶ 直送
  │              └─[OEM路径]──▶ OEM
  │
  └──[Phase2]────────────────▶ 已下单 ←→ 已出货
                                   │
                   ┌───────────────┼───────────────┐
                   ▼               ▼               ▼
               倉庫着           倉庫着           倉庫着
              （永康路径）    （直送路径）      （Phase2）

Phase2（简化路径）：
  已下单 ←→ 已出货 → 倉庫着
```

**关键设计**：

- 一需求单 → 多采购单（`DemandProcurementMapping`，SPEC-B11）
- 采购单关联工厂（老工厂选或新建）
- `estimatedPriceJpy` 自动计算：`priceRmb / taxPoint × 1.02 × 1.2 × exchangeRate × 1.05`

---

### 步骤2.5：出货批次（ShipmentBatch）— SPEC-B11

```
目的：记录厂家每次发货事件
触发：手动登记，在验货之前
```

**关键规则（SPEC-B11 Q1-Q7）**：

| 规则 | 内容 |
|------|------|
| Q1 | 手动登记，系统不自动创建 |
| Q2 | SUM(ShipmentBatch) 可 > Procurement.quantity（遗留库存） |
| Q3 | 出货→验货强顺序，每批出货必须关联验货记录 |
| Q4 | 数量独立：SUM(passedCount) ≤ SUM(shipmentQuantity) |
| Q5 | Demand↔Procurement 关联须 subProductCode 一致 |

**ShipmentBatchStatus FSM**：

```
待验货 ──[关联验货记录]──▶ 验货中 ──[全部 COMPLETED]──▶ 已验货
      │                              │
      │                              └──[新增迟到货物]──▶ 验货中（回归）
      └──[人工取消]──────▶ 已取消
```

**数量链**：

```
Demand.quantity
  = SUM(Procurement.quantity)          [采购总量]
  = SUM(ShipmentBatch.shipmentQty)   [出货总量，可超额]
  ≥ SUM(QcRecord.inspectionCount)    [验货总量]
  = SUM(QcRecord.passedCount)        [合格总量]
  ≥ SUM(SalesRecord.initialStock)    [上架库存]
```

---

### 步骤3：验货（QcRecord）

```
目的：记录验货结果，区分合格品和不良品
触发：货物到达仓库/现场后，验货员录入
```

**QcType**：

| 类型 | 含义 | 结果 |
|------|------|------|
| `ONSITE`（検品） | 仓库验货 | PASS→按体积走空运/海运；FAIL→退货 |
| `REMOTE`（現地検品） | 现场异地验货 | PASS→直送日本（メーカー直送） |

**QcStatus FSM**：

```
PENDING（待验货）──[录入结果]──▶ COMPLETED（已完成）  [终态]
                              └──▶ RETURN_REQUESTED（发起退货）[终态]
```

**关键数量字段**：

```
inspectionCount（抽检数量）
  │
  ├── passedCount（合格数量）
  └── defectiveCount（不合格数量）= inspectionCount - passedCount
```

**触发后续（当前断裂，需修复）**：

```
QcRecord COMPLETED
  │
  ├── PASS + volume≤0.5m³ → エア便（空运）
  ├── PASS + volume>0.5m³ → 輸出（海运）
  └── REMOTE QC → メーカー直送（直送日本）
```

---

### 步骤4：调配（LogisticsPlan）— 三条路径

```
目的：货物调配出库，决定运输方式
触发：验货 PASS 后，手动创建调配计划
```

**PlanType**：

| 类型 | 含义 | 路径 |
|------|------|------|
| `SEA` | 整柜海运 | 永康仓出库→报关→海运 |
| `AIR` | 空运 | 永康仓出库→报关→空运 |
| `CONSOLIDATION` | 拼柜（LCL） | 多批汇入池→凑柜→报关→海运 |

**三条路径详解**：

```
路径A：永康仓路径（OEM / 大件普货）
────────────────────────────────────────
ShipmentBatch → 入永康仓（实际到仓日）
  → 用户在 LogisticsPlan 选 PlanType=SEA/AIR
  → 货物出库
  → 报关（步骤5 DomesticCustoms）
  → 运输到日本

路径B：拼柜路径（多批小件合并）
────────────────────────────────────────
多个 ShipmentBatch → 加入 ConsolidationPool（拼柜池）
  → 池内汇总 volume/weight
  → 凑满一个柜（约70m³）→ Container 实体
  → 装柜（ConsolidationPool.status → LOADED）
  → 报关（步骤5 DomesticCustoms，containerNo 关联）
  → 运输到日本

路径C：厂家直销（QC后直送）
────────────────────────────────────────
ShipmentBatch → QcRecord（REMOTE类型）
  → Procurement.status → メーカー直送
  → 跳过步骤4的 LogisticsPlan（不经过永康仓）
  → 直接进入步骤6日本清关
```

**Container（占位实体）**：

| 字段 | 说明 |
|------|------|
| containerNo | 柜号（装柜后才有） |
| containerType | 20GP/40GP/40HC/45HC |
| totalCbm | 总体积（约70m³满柜） |
| totalWeightKg | 总重量 |

**ConsolidationPool（占位实体）**：

| 字段 | 说明 |
|------|------|
| poolCode | 池编号 |
| destinationPort | 目的港 |
| totalCbm | 当前累计体积 |
| containerThreshold | 触发装柜的体积阈值（如70m³） |
| status | POOL_PENDING → POOL_READY → CONTAINER_PLANNED → LOADED |

**LogisticsStatus FSM**：

```
PLANNED（计划中）──[订舱]──▶ BOOKED（已订舱）
       │                            │
       │                   ──[发货在途]──▶ IN_TRANSIT（在途）
       │                                         │
       └──[取消]──▶ （终态）                      └──[到达]──▶ DELIVERED（已到达）
```

---

### 步骤5：国内报关（DomesticCustoms）

```
目的：出口报关，取得货物出口许可
触发：调配计划出库后，手动创建（或自动触发）
```

**前置条件**：

| 路径 | 前置实体 | 关联字段 |
|------|---------|---------|
| 永康仓路径 | LogisticsPlan | containerNo |
| 拼柜路径 | Container | containerNo |
| 直送路径 | 无（QC后直送） | 不经过此步骤 |

**DomesticCustomsStatus FSM**：

```
PENDING（待提交）──[提交]──▶ SUBMITTED（已提交）
                              │          │
                              │    ──[放行]──▶ CLEARED（已清关）[终态]
                              └──[驳回]──▶ REJECTED（已驳回，可重新编辑）
                                         │
                                         └──[重新编辑提交]──▶ SUBMITTED
```

**关键字段**：`containerNo`（货柜号，用于关联调配计划）；`quantity`（申报数量）；`estimatedValueCny`（申报金额）

---

### 步骤6：日本清关（JapanCustoms）

```
目的：货物进口日本，缴纳关税和消费税
触发：DomesticCustoms CLEARED 后自动创建（当前正常）
```

**JapanCustomsStatus FSM**：

```
PENDING（待清关）──[开始]──▶ IN_PROGRESS（清关中）
                                    │
                          ──[完成]──▶ CLEARED（已清关）[终态]
                          ──[失败]──▶ FAILED（清关失败）[终态]
```

**触发后续**：
- CLEARED → 自动创建 `SalesRecord`（步骤8上架）
- CLEARED → 计算关税+消费税（待退税步骤7）

**费用字段**：`importDutyPaid`（进口关税 JPY）；`consumptionTaxPaid`（消费税 JPY）

---

### 步骤7：退税（TaxRefund）

```
目的：申请出口退税（增值税退还）
触发：国内报关 CLEARED 后手动创建（当前正常）
```

**前置条件**：`Procurement.billingType ≠ NO_REFUND`

**TaxRefundStatus FSM**：

```
APPLYING（申请中）──[完成退税]──▶ COMPLETED（已完成）[终态]
        │
        └──[不退税]──▶ NO_REFUND（不退税）[终态]
```

**退税金额公式**：

```
estimatedRefundRmb = priceRmb × quantity × (taxPoint - 1)

示例：含税单价25元 × 200件 × (1.1 - 1) = 500元退税
```

**BillingType 决定是否可退税**：

| BillingType | 退税 | 处理 |
|------------|------|------|
| `ZHE_LU_KAI_PIAO` | ✅ | 计算 estimatedRefundRmb |
| `CHAO_HUI_TUI_SHUI` | ✅ | 同上 |
| `NO_REFUND` | ❌ | 直接 NO_REFUND |
| `OTHER` | ❓ | 待确认 |

---

### 步骤8：上架运营（SalesRecord）

```
目的：商品上架销售，追踪库存和退货
触发：JapanCustoms CLEARED 后自动创建（当前正常）
```

**SalesChannel（销售渠道）**：

| 渠道 | 含义 |
|------|------|
| `AMAZON` | Amazon 平台 |
| `MERCALI` | Mercari 平台 |
| `SELF_SITE` | 自建站 |
| `OTHER` | 其他渠道 |

**SalesStatus FSM**：

```
LISTED（已上架）──[库存低]──▶ LOW_STOCK
       │                          │
       │                   ──[售罄]──▶ OUT_OF_STOCK
       │                          │
       └──[下架]──▶ DISCONTINUED（已下架）[终态]
```

**库存追踪**：

```
initialStock（初始上架量）= SUM(QcRecord.passedCount)  ← 关键！不是采购量
currentStock = initialStock - 销量 + 退货量
lowStock = currentStock < safetyThreshold
```

**反馈循环（已实现）**：

```
currentStock < safetyThreshold
  │
  └──→ SalesLowStockEventListener
        │
        └──→ ReplenishmentDemand（自动创建步骤1）
              │
              └──→ 新循环开始...
```

**退货率触发**：

| 退货率 | 触发动作 |
|--------|---------|
| > 5% | 通知 QC 团队升级验货标准 |
| > 10% | 标记工厂质量问题，降级供应商 |

---

### 步骤9-10：运营数据 & 商品返回

```
目的：数据汇总和反馈优化
触发：持续进行，人工分析
```

**运营数据维度**：

| 数据维度 | 来源 | 用途 |
|---------|------|------|
| 销量趋势 | SalesRecord.salesQuantity | 预测补货量 |
| 退货率 | SalesRecord.returnRate | 供应商评级 |
| 库存周转 | currentStock / 日均销量 | 调整采购量 |
| 利润率 | 销售价 - 采购价 - 关税 - 运费 | 选品决策 |

**商品返回（退货）**：

```
客户退货 → 更新 SalesRecord.returnedQuantity
       → 退货率上升
       → 触发 >5%/10% 阈值
       → 供应商质量标记
       → 可能触发新品替代方案
```

---

## 3. 完整实体关系图

```
Product (masterCode + subCode, OEM/ORDINARY/FACTORY_DIRECT)
  └── ProductFactory（product ↔ factory 多对多）

ReplenishmentDemand (需求入口)
  │
  └── DemandProcurementMapping（N:1）
           │
           ▼
Procurement（多采购单←同一需求）
  │
  ├── Factory（老工厂/新工厂）
  │
  └── ShipmentBatch（N批次出货）
        │
        └── QcRecord（关联到批次）
              │
              ├── PASS → LogisticsPlan（调配）
              │         │
              │         ├─[SEA/AIR]──▶ DomesticCustoms
              │         │                    │
              │         │                    ▼
              │         │              JapanCustoms
              │         │                    │
              │         │                    ▼
              │         │              TaxRefund
              │         │                    │
              │         │                    ▼
              │         │              SalesRecord
              │         │                    │
              │         │                    ▼
              │         │              低库存 → ReplenishmentDemand（新循环）
              │         │
              │         └─[CONSOLIDATION]──▶ ConsolidationPool（拼柜池）
              │                                       │
              │                                       ▼
              │                                 Container（装柜）
              │                                       │
              │                                       ▼
              │                                 DomesticCustoms（containerNo关联）
              │
              └── FAIL → 不良品处理（人工，不影响批次状态）
```

---

## 4. 事件链全图（4条，当前2条正常，2条修复中）

```
Chain 1: 库存低 → 补货需求  ✅ 正常
────────────────────────────────────────
SalesRecord.isLowStock()
  → SalesLowStockEventListener
  → ReplenishmentDemand.autoCreate()

Chain 2: 清关完成 → 上架销售  ✅ 正常
────────────────────────────────────────
JapanCustomsUseCase.complete(CLEARED)
  → JapanCustomsClearedEvent
  → JapanCustomsClearedEventListener
  → SalesRecord.autoCreate()

Chain 3: 验货完成 → 发注单推进  🔧 修复中
────────────────────────────────────────
QcRecordUseCase.update(COMPLETED)
  → ✅ ApplicationEventPublisher 已注入（2026-04-29）
  → ✅ PASS → QcRecordCompletedEvent 已发布
  → ✅ ProcurementQcPassedEventListener 已触发（监听条件含 已出货）
  → ✅ Procurement.status 自动推进至 エア便/輸出/メーカー直送

Chain 4: 物流在途 → 国内报关  🔧 修复中
────────────────────────────────────────
LogisticsPlanUseCase.update(IN_TRANSIT)
  → ✅ ApplicationEventPublisher 已注入（2026-04-29）
  → ✅ LogisticsPlanInTransitEvent 已发布
  → ✅ LogisticsInTransitEventListener 已触发
  → ✅ DomesticCustomsRecord 自动创建（幂等）
```

---

## 5. 各步骤状态机汇总

| 步骤 | 实体 | 状态枚举 | 终态 |
|------|------|---------|------|
| 1 补货需求 | ReplenishmentDemand | PENDING / CONFIRMED / CANCELLED | CANCELLED |
| 2 下单采购 | Procurement | 19态 FSM（未定→完了） | 完了 |
| 2.5 出货批次 | ShipmentBatch | 待验货 / 验货中 / 已验货 / 已取消 | 已验货 / 已取消 |
| 3 验货 | QcRecord | PENDING / COMPLETED / RETURN_REQUESTED | COMPLETED |
| 4 调配 | LogisticsPlan | PLANNED / BOOKED / IN_TRANSIT / DELIVERED | DELIVERED |
| 4 拼柜池 | ConsolidationPool | POOL_PENDING / POOL_READY / CONTAINER_PLANNED / LOADED | LOADED |
| 4 货柜 | Container | —（占位） | — |
| 5 国内报关 | DomesticCustomsRecord | PENDING / SUBMITTED / CLEARED / REJECTED | CLEARED |
| 6 日本清关 | JapanCustomsRecord | PENDING / IN_PROGRESS / CLEARED / FAILED | CLEARED / FAILED |
| 7 退税 | TaxRefundRecord | APPLYING / COMPLETED / NO_REFUND | COMPLETED / NO_REFUND |
| 8 运营销售 | SalesRecord | LISTED / LOW_STOCK / OUT_OF_STOCK / DISCONTINUED | DISCONTINUED |

---

## 6. 实现状态矩阵

| 步骤 | Entity | CRUD | 事件链 | 前端 | Spec |
|------|--------|------|--------|------|------|
| 0 商品 | Product + ProductFactory | ✅ | — | ✅ | SPEC-B10 |
| 1 补货 | ReplenishmentDemand | ✅ | — | ✅ | SPEC-B01 v2.2.0 |
| 2 采购 | Procurement + Factory | ✅ | — | ✅ | SPEC-B02 |
| 2.5 出货批次 | ShipmentBatch | 🔴 | — | 🔴 | SPEC-B11 |
| 3 验货 | QcRecord | ✅ | 🔧修复中 | ✅ | SPEC-B03 |
| 4 调配 | LogisticsPlan | ✅ | 🔧修复中 | ✅ | SPEC-B04 |
| 4 拼柜 | ConsolidationPool | 🔴 | — | 🔴 | SPEC-B04 |
| 4 货柜 | Container | 🔴 | — | 🔴 | SPEC-B04 |
| 5 国内报关 | DomesticCustomsRecord | ✅ | — | 🔧改造中 | SPEC-B05 v1.4.0 |
| 6 日本清关 | JapanCustomsRecord | ✅ | ✅ | 🔧改造中 | SPEC-B06 v1.3.0 |
| 7 退税 | TaxRefundRecord | ✅ | — | ✅ | SPEC-B07 |
| 8 运营销售 | SalesRecord | ✅ | ✅ | ✅ | SPEC-B08 |

**图例**：✅ 已实现 · 🔴 占位/未实现 · 🔧改造中

---

## 7. 当前关键问题清单

| # | 问题 | 影响 | 优先级 |
|---|------|------|--------|
| 1 | ~~Chain 3：验货完成不推进发注单状态~~ | ✅ 已修复（2026-04-29） | — |
| 2 | ~~Chain 4：物流在途不自动创建报关~~ | ✅ 已修复（2026-04-29） | — |
| 3 | SalesRecord.initialStock = procurement.quantity | 初始库存错误（应为 passedCount 聚合） | P0 |
| 4 | ShipmentBatch + DemandProcurementMapping 未实现 | 出货批次追踪缺失 | P1 |
| 5 | DomesticCustoms 新建表单 procurementId 必填，containerNo 可选 | 报关维度错误，应以货柜为主 | **P0（2026-04-30）** |
| 6 | JapanCustomsRecord 缺少 containerNo 字段和货柜级入口 | 清关无法按货柜维度追踪 | **P1（2026-04-30）** |
| 7 | LogisticsPlanPage 缺少"创建报关"批量按钮 | 无法从物流计划直接发起报关 | **P1（2026-04-30）** |
| 5 | ConsolidationPool + Container 未实现 | 拼柜路径无法执行 | P1 |
| 6 | OrderOverview 步骤5-8 未展示 | 运营看不到全链路 | P2 |
| 7 | LogisticsPlan.quantity 来源错误 | 调配数量非实际验货量 | P2 |
| 8 | ReplenishmentDemand 无自动触发来源 | 低库存触发链路存在，但新品需求仍需手动 | P3 |
