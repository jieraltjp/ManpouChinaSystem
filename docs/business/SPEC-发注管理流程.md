# 发注管理 — 规格文档

> **版本**: 1.3.0
> **创建**: 2026-04-20
> **更新**: 2026-04-21
> **状态**: 已审计
> **依据**: 业务流分析（6步重构） + `docs/发注管理体系升级.pdf`

> ⚠️ **代码实现进度**: ReplenishmentDemand ✅ · Procurement ✅（v1.3.0 含 field sourcing + 工厂快速新建/编辑）
> · Product 🔴 需扩展货号结构 · Factory ✅（无独立页面，内嵌于发注单页面）· QcRecord 🔴 升级为聚合根
> · LogisticsPlan 🔴 新增 · DomesticCustoms 🔴 待定 · JapanCustoms 🔴 待定

---

## 1. 需求背景

面向中日贸易场景的采购发注全链路管理系统，覆盖从「需求录入」到「日本清关完成/会计结算」的全流程数字化。

**新业务主流程（6步）：**

| 步骤 | 名称 | 说明 |
|------|------|------|
| 第一步 | 补货/采购需求录入 | 非新品=补货，新品=采购，生成 ReplenishmentDemand |
| 第二步 | 下单 | 采购根据需求信息向工厂下单，生成 Procurement |
| 第三步 | 验收 | 货物到仓后验货，生成 QcRecord |
| 第四步 | 调配 | 安排海运/空运/拼柜，生成 LogisticsPlan |
| 第五步 | 国内报关 | 国内出口报关（TBD） |
| 第六步 | 日本清关 | 日本进口清关（TBD） |

**价格计算公式（前端实时计算，后端存结果）：**
```
批发价 JPY = (人民币单价 ÷ 票点 × 1.02 × 1.2) × 汇率 × 1.05
```

---

## 2. 商品类型

| 类型 | 代码 | 说明 |
|------|------|------|
| OEM定制产品 | `OEM` | 批量采购，需现场验货 |
| 普货 | `ORDINARY` | 普通贸易货物 |
| 厂家出口 | `FACTORY_DIRECT` | 厂家直送，不经仓库 |

**货号结构：**

```
主货号（MasterCode）：odn012
└── 子货号（SubCode）：odn012-re（黑色）、odn012-wh（白色）、odn012-bk（黑色原版）
```

---

## 3. 字段清单（逐步骤）

### 3.1 第一步：补货/采购需求录入（ReplenishmentDemand）

| 字段 | 来源 | 必填 | 说明 |
|------|------|------|------|
| demandType | 用户选择 | ✅ | REPLENISHMENT(补货) / NEW_PURCHASE(新品采购) |
| productCode | 用户输入 | ✅ | 主货号 |
| subProductCode | 用户输入 | | 子货号（颜色变体） |
| quantity | 用户输入 | ✅ | 需求量 |
| destination | 用户输入 | | 目的地 |
| japanLead | 用户输入 | | 日本担当 |
| status | 系统 | | default=PENDING |

### 3.2 第二步：下单（Procurement）

> **工厂集成**（v1.3.0）：Factory 无独立页面，完全内嵌于发注单页面。发注表单内工厂选择器提供"新建"和"编辑"两个按钮，均在同一弹窗内完成工厂的创建/编辑/状态切换。

| 字段 | 来源 | 必填 | 说明 |
|------|------|------|------|
| factoryId | 用户选择 | ✅ | 工厂（关联 Factory）；选择器内嵌新建/编辑按钮 |
| productCode | 自动带入 | ✅ | 主货号（来自 ReplenishmentDemand） |
| subProductCode | 用户输入 | | 子货号/枝番（颜色） |
| material | 用户输入 | | 材质 |
| requiresQc | 用户选择 | ✅ | 是否需要检测 |
| chinaLead | 用户输入 | ✅ | 中国担当 |
| priceRmb | 用户输入 | ✅ | 商品单价（元） |
| exchangeRate | 用户输入 | ✅ | CNY→JPY 汇率（默认 21.5） |
| taxPoint | 用户输入 | | 票点（默认 1.1） |
| billingType | 用户选择 | ✅ | 报关类型（浙鲁开票/超慧退税/不退税） |
| estimatedPriceJpy | 自动计算 | | 估算批发价 JPY（公式计算） |
| customsRemarks | 用户输入 | | 报关备注 |
| instructionManual | 用户输入 | | 说明书 |
| orderDate | 用户输入 | | 下单日 |
| factoryShipDate | 用户输入 | | 厂家出货日 |
| plannedShipDate | 用户输入 | | 预计出货日（交货期） |
| actualShipDate | 用户输入 | | 实际出货日 |
| productLead | 用户输入 | | 商品担当 |
| japanLead | 自动带入 | | 日本担当（来自 ReplenishmentDemand） |
| destination | 自动带入 | | 目的地（来自 ReplenishmentDemand） |
| customerCompany | 用户输入 | | 客户公司 |
| status | 系统 | | default=未定 → 转为 発注待 |

### 3.3 第三步：验收（QcRecord）

| 字段 | 来源 | 必填 | 说明 |
|------|------|------|------|
| qcCode | 系统生成 | | 验收编号（流水号） |
| procurementId | 自动关联 | ✅ | 关联采购单 |
| sellerName | 用户输入 | | 卖家名称 |
| productCode | 自动带入 | | 货号（来自 Procurement） |
| qcUserId | 用户选择 | | 开单人/验货负责人 |
| orderDate | 自动带入 | | 下单日（来自 Procurement） |
| taxRefund | 用户选择 | | 是否退税 |
| destination | 自动带入 | | 目的地 |
| quantity | 自动带入 | | 订购数量 |
| inspectionCount | 用户输入 | | 检品数 |
| boxCount | 用户输入 | | 箱数 |
| boxDimensions | 用户输入 | | 箱子尺寸（长×宽×高 cm） |
| serialNumbers | 用户输入 | | 序列号列表 |
| netWeightPerUnit | 用户输入 | | 单个净重(kg) |
| grossWeight | 用户输入 | | 毛重(kg) |
| taxInclusivePrice | 用户输入 | | 含税价 |
| material | 自动带入 | | 材质（来自 Procurement） |
| qcDate | 用户输入 | | 验货日期 |
| qcStandard | 用户输入 | | 验收标准 |
| result | 验货结果 | | PASS / FAIL |
| passedCount | 用户输入 | | 合格数量 |
| defectiveCount | 用户输入 | | 不良数量 |
| remarks | 用户输入 | | 备注 |

### 3.4 第四步：调配（LogisticsPlan）

| 字段 | 来源 | 必填 | 说明 |
|------|------|------|------|
| procurementId | 用户选择 | | 关联采购单（拼柜时可为空） |
| factoryId | 自动带入 | | 工厂（来自 Procurement） |
| productCode | 自动带入 | | 货号（来自 Procurement） |
| cargoSize | 用户输入 | | 货物尺寸 |
| cargoWeight | 用户输入 | | 货物重量(kg) |
| requiresQc | 自动带入 | | 是否需要检测（来自 Procurement） |
| planType | 用户选择 | ✅ | SEA(海运) / AIR(空运) / CONSOLIDATION(拼柜) |
| estimatedShipDate | 用户输入 | | 预计发货日 |
| actualShipDate | 用户输入 | | 实际发货日 |
| containerId | 装柜后赋值 | | 货柜编号 |
| status | 系统 | | PLANNED → BOOKED → IN_TRANSIT → DELIVERED |

### 3.5 第五步：国内报关（DomesticCustomsRecord）

| 字段 | 状态 | 说明 |
|------|------|------|
| 所有字段 | **待定** | 等待业务确认后补充 |

预期包含：报关单号、HS编码、申报日期、申报人、货物价值、出口口岸等。

### 3.6 第六步：日本清关（JapanCustomsRecord）

| 字段 | 状态 | 说明 |
|------|------|------|
| 所有字段 | **待定** | 等待业务确认后补充 |

预期包含：入境报关号、到达日期、清关行、清关行电话等。

---

## 4. 状态流转图（v1.3.0）

```
┌──────────────────────────────────────────────────────────────────┐
│                    永康路径（普通货物）                              │
└──────────────────────────────────────────────────────────────────┘
                              →
  ReplenishmentDemand(CONVERTED) → 発注待 → 永康 → 倉庫着 → 検品
      → エア便/輸出 → 国内通関 → 通関 → 日本着 → 日本通関完了 → 会計 → 完了

┌──────────────────────────────────────────────────────────────────┐
│                    OEM 路径                                         │
└──────────────────────────────────────────────────────────────────┘
                              →
  ReplenishmentDemand(CONVERTED) → 発注待 → OEM → 倉庫着 → 現地検品
      → メーカー直送 → 日本着 → 日本通関完了 → 会計 → 完了

┌──────────────────────────────────────────────────────────────────┐
│                    空运路径                                         │
└──────────────────────────────────────────────────────────────────┘
                              →
  ReplenishmentDemand(CONVERTED) → 発注待 → 直送 → 倉庫着 → 検品
      → エア便 → 国内通関 → 通関 → 日本着 → 日本通関完了 → 会計 → 完了

┌──────────────────────────────────────────────────────────────────┐
│                    状态说明                                         │
└──────────────────────────────────────────────────────────────────┘

  未定     — 需求录入后初始状态（已转为 ReplenishmentDemand）
  予定     — 预计发注
  発注待   — 已录入商品，等待下单
  OEM      — OEM定制产品路径
  永康     — 1688下单后货物发往永康仓
  直送     — 厂家直接发货（不经永康仓）
  倉庫着   — 货物到达仓库
  現地検品 — 现场验货（异地）
  検品     — 仓库验货
  エア便   — 空运（尺寸/重量达标的轻量货）
  メーカー直送 — 厂家直送
  国内通関 — 国内报关（新增）
  輸出     — 已出口
  通関     — 日本报关
  日本着   — 已到日本
  日本通関完了 — 日本清关完成（新增）
  会計     — 财务结算
  完了     — 全流程结束（终态 — 禁止任何变更）
  退货     — 退货管理
```

---

## 5. 状态推进规则

| 当前状态 | 触发动作 | 下一状态 |
|----------|----------|----------|
| （ReplenishmentDemand PENDING） | 转采购 | 発注待 |
| 未定 | 预计发注 | 予定 |
| 未定 | 下单 | 発注待 |
| 未定 | OEM下单 | OEM |
| 未定 | 重置 | 未定 |
| 発注待 | 永康仓发货 | 永康 |
| 発注待 | 厂家直送（不经永康） | 直送 |
| 永康 | 到达仓库 | 倉庫着 |
| 直送 | 到达仓库 | 倉庫着 |
| 倉庫着 | 仓库验货 | 検品 |
| 倉庫着 | 现场异地验货 | 現地検品 |
| 現地検品 | 确认发货 | メーカー直送 |
| 検品 | 体积/重量达标 | エア便 |
| 検品 | 超体积/重量，需海运 | 輸出 |
| 検品 | 退回复检 | 倉庫着 |
| エア便 | 国内报关 | 国内通関 |
| 輸出 | 国内报关 | 国内通関 |
| 国内通関 | 日本报关 | 通関 |
| 通関 | 清关完成 | 日本着 |
| 日本着 | 清关完成 | 日本通関完了 |
| 日本通関完了 | 财务结算 | 会計 |
| 会計 | 完成 | 完了 |

---

## 6. 核心业务规则

| 规则 | 说明 |
|------|------|
| 完了终态 | `完了` 后禁止任何状态变更 |
| 需求先于采购 | 必须先录入 ReplenishmentDemand，再转为 Procurement |
| 退货独立 | 退货与原单独立处理 |
| 空运判定 | 尺寸+重量达标自动推荐走 エア便 路径 |
| 报关前置 | 国内通関必须在日本通関之前完成 |
| 报价计算 | `(priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05` |
| 货号结构 | Product 唯一键 = masterCode + subCode，支持颜色变体 |
| 工厂引用 | Procurement 引用 FactoryId，不内联工厂信息 |

---

## 7. 对应微服务归属（Phase 0 → manpou-allinone）

> ⚠️ Phase 0：所有领域（含发注/仓储/报关/物流/财务等）合一部署在 **manpou-allinone**（端口 18090）。
> 下表为 Phase 1+ 拆分后的目标归属，仅供参考。

| 表/功能 | Phase 0 | Phase 1+ 目标服务 |
|---------|---------|------------------|
| Procurement（发注单主表） | manpou-allinone:18090 | procurement-service |
| ReplenishmentDemand（补货需求单） | manpou-allinone:18090 | procurement-service |
| Factory（工厂管理） | manpou-allinone:18090 | procurement-service |
| Product（商品目录） | manpou-allinone:18090 | product-service |
| QcRecord（验收记录） | manpou-allinone:18090 | customs-service |
| LogisticsPlan（调配计划） | manpou-allinone:18090 | logistics-service |
| ConsolidationPool（拼柜池） | manpou-allinone:18090 | logistics-service |
| Container（货柜） | manpou-allinone:18090 | logistics-service |
| FinanceRecord（财务） | manpou-allinone:18090 | finance-service |
| DomesticCustomsRecord（国内报关） | customs-service |
| JapanCustomsRecord（日本清关） | customs-service |

---

## 8. 测试清单

> ✅ = 已实现（代码验证通过）  🔴 = 待实现

- [ ] 🔴 新规 ReplenishmentDemand：补货类型 + 新品采购类型区分
- [ ] 🔴 补货需求转采购：CONVERTED 状态推进
- [ ] 🔴 Product 主/子货号结构：颜色变体查询
- [x] ✅ OEM路径完整流转（canTransitionTo FSM 支持）
- [x] ✅ 永康路径完整流转（含国内通関/日本通関完了，FSM 支持）
- [x] ✅ 空运路径完整流转（FSM 支持）
- [x] ✅ 直送路径完整流转（FSM 支持）
- [ ] 🔴 QcRecord 独立聚合根：PASS/FAIL 流程
- [ ] 🔴 LogisticsPlan 调配：海运/空运/拼柜
- [x] ✅ 退货状态触发（退货 state exists in ShipmentStatus）
- [x] ✅ 报价公式计算验证（calculateEstimatedPriceJpy 已实现）
- [x] ✅ 完了后禁止修改（isTerminal + BusinessException）
- [x] ✅ FSM 无效转换拒绝（canTransitionTo 校验）
- [ ] 🔴 担当者权限隔离
- [ ] 🔴 第五步国内报关 DomesticCustomsRecord（字段待定）
- [ ] 🔴 第六步日本清关 JapanCustomsRecord（字段待定）
