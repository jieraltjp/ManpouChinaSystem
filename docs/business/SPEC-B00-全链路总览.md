# 全链路业务流索引 — MANPOU 中国系统

> **版本**: 1.5.0
> **创建**: 2026-04-22
> **更新**: 2026-05-07（v1.5.0：Container / ConsolidationPool 已实现；与全链路业务流程同步）
> **状态**: ✅ 已实现（全链路8步 CRUD + 事件驱动 + 反馈循环）
> **依据**: 用户业务描述（8步全链路 + 商品数据库 + 循环反馈）

---

## 文档目的

作为 `docs/business/` 的主入口索引，按业务步骤顺序列出：
- **需要什么**（本步骤消耗哪些上游数据）
- **填入什么**（本步骤产生哪些下游数据）
- **与现有文档的对应关系**
- **缺口（待确认 / 未实现）**

循环反馈：`第八步（运营销售） → 验证 → 第一步（补货需求）` 独立标注。

---

## 业务流全景图

```
┌─────────────────────────────────────────────────────────────────────┐
│                     正向流程（线性）                                   │
└─────────────────────────────────────────────────────────────────────┘

  第一步          第二步           第三步           第四步
 ┌────────┐    ┌──────────┐   ┌────────┐    ┌──────────┐
 │补货/采购│───▶│ 下单采购  │───▶│ 验收    │───▶│ 调配(海空运)│
 │需求录入 │    │+工厂管理  │   │        │    │ +拼柜/货柜 │
 └────────┘    └──────────┘   └────────┘    └──────────┘
                                   │                │
                          QcRecord ▼         LogisticsPlan ▼
                                   │                ▼
                          第五步          ┌────────────────┐
                        ┌────────────┐   │ 第五步│第六步│  │
                        │ 国内报关     │───▶│日本清关│    │  │
                        └────────────┘      └────────────────┘
                               │                  │
                               ▼                  ▼
                        DomesticCustoms     JapanCustoms
                          Record              Record
                               │                  │
                               └────────┬────────┘
                                        ▼
                               ┌────────────────┐
                               │  第七步 退税    │
                               │  (日本到达触发) │
                               └────────────────┘
                                        │
                                        ▼
                               ┌────────────────┐
                               │  第八步 运营    │
                               │  (日本销售)     │
                               └────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                     反馈循环（第八步 → 第一步）                        │
└─────────────────────────────────────────────────────────────────────┘

   第八步(运营) ──销量验证──▶ 第一步(补货需求)
                                     ▲
                                     │ 再补货
                                     │
   第八步(运营) ──滞销预警──▶ 第一步(补货需求)
                                     ▲
                                     │ 暂停补货
                                     │
   第八步(运营) ──新品需求──▶ 第一步(新品采购)
                                     ▲
                                     │ 新品立项
```

---

## 第一步：补货/采购需求录入

**对应文档**: `SPEC-B01-补货需求-步骤1.md` · `DOMAIN-发注管理领域模型.md` §1.1
**对应代码**: `ReplenishmentDemand` 聚合根 ✅ 已实现

### 需要什么（输入）

| 字段 | 来源 | 必填 | 说明 |
|------|------|------|------|
| demandType | 用户选择 | ✅ | `REPLENISHMENT`(补货) 或 `NEW_PURCHASE`(新品采购) |
| productCode | 用户输入 / Product库选择 | ✅ | 主货号 |
| subProductCode | 用户输入 | | 子货号（颜色，如 re/wh/bk）；新品无子货号时可为空 |
| quantity | 用户输入 | ✅ | 需求量 |
| destination | 用户输入 | | 目的地（发给哪个日本客户） |
| japanLead | 用户输入 | | 日本担当 |

### 填入什么（输出 → 流向第二步）

| 字段 | 目标 | 说明 |
|------|------|------|
| productCode | Procurement.productCode | 自动带入 |
| destination | Procurement.destination | 自动带入 |
| japanLead | Procurement.japanLead | 自动带入 |
| id (linked) | Procurement.replenishmentDemandId | 关联回溯 |

### 缺口分析

| 问题 | 现状 | 建议 |
|------|------|------|
| 新品采购无子货号 | subProductCode 可为空，但 Product 库无新品立项入口 | 新品采购后应自动在 Product 创建主货号记录 |
| 补货参考历史 | 仅靠用户记忆，无历史采购数据参照 | 可从 Procurement 历史记录中带出上次采购价/数量 |
| 反馈循环接口 | 步骤8运营数据未反向流入 | 需要销售数据 API 作为步骤1的输入 |

---

## 第二步：下单（采购 + 工厂管理）

**对应文档**: `SPEC-B02-发注单-步骤2.md` · `DOMAIN-发注管理领域模型.md` §1.2 §1.3
**对应代码**: `Procurement` 聚合根 ✅ · `Factory` 聚合根 ✅（内嵌表单）

### 需要什么（输入）

**来自第一步**（自动代入）:

| 字段 | 来源 | 说明 |
|------|------|------|
| productCode | ReplenishmentDemand.productCode | 主货号 |
| japanLead | ReplenishmentDemand.japanLead | 日本担当 |
| destination | ReplenishmentDemand.destination | 目的地 |

**来自用户**（第二步新增）:

| 字段 | 必填 | 说明 |
|------|------|------|
| factoryId | ✅ | 工厂（关联 Factory）；支持新建/编辑 |
| subProductCode | | 子货号/枝番（颜色，如 re/wh/bk） |
| material | | 材质 |
| requiresQc | ✅ | 是否需要检测 |
| chinaLead | ✅ | 中国担当 |
| priceRmb | ✅ | 商品单价（元） |
| exchangeRate | ✅ | CNY→JPY 汇率（默认 21.5） |
| taxPoint | | 票点（默认 1.1） |
| billingType | ✅ | 报关类型（浙鲁开票/超慧退税/不退税/其他） |
| customsRemarks | | 报关备注 |
| instructionManual | | 说明书 |
| orderDate | | 下单日 |
| factoryShipDate | | 厂家出货日 |
| plannedShipDate | | 预计出货日（交货期） |
| actualShipDate | | 实际出货日 |
| productLead | | 商品担当 |
| customerCompany | | 客户公司 |

**来自 Factory 库**（选择后代入）:

| 字段 | 来源 | 说明 |
|------|------|------|
| factoryName | Factory.factoryName | 工厂名称 |
| factoryLocation | Factory.province + Factory.city | 工厂位置（省/市） |
| factoryRoughLocation | Factory.roughLocation | 粗略位置（工业区/园区） |
| contactName | Factory.contactName | 联系人名称 |
| contactPhone | Factory.contactPhone | 联系人电话 |

### 填入什么（输出 → 流向第三步）

| 字段 | 目标 | 说明 |
|------|------|------|
| procurementId | QcRecord.procurementId | 关联验货单 |
| productCode | QcRecord.productCode | 自动带入 |
| subProductCode | QcRecord.subProductCode | 自动带入 |
| quantity | QcRecord.quantity | 自动带入 |
| orderDate | QcRecord.orderDate | 自动带入 |
| destination | QcRecord.destination | 自动带入 |
| material | QcRecord.material | 自动带入 |
| requiresQc | LogisticsPlan.requiresQc | 自动带入 |
| productCode | LogisticsPlan.productCode | 自动带入 |
| factoryId | LogisticsPlan.factoryId | 自动带入 |
| billingType | 报关凭证 | 用于国内/日本报关 |

### 缺口分析

| 问题 | 现状 | 建议 |
|------|------|------|
| Factory 库字段不完整 | 现有 Factory 无联系人微信/QQ、无经纬度 | 需扩展 Factory 实体字段（见 §商品数据库章节） |
| billingType 枚举值 | 现有只有 ZHE_LU_KAI_PIAO / CHAO_HUI_TUI_SHUI / NO_REFUND / OTHER | 用户提"浙鲁开票/超慧退税/其他"需映射至枚举 |
| instructionManual | 作为 TEXT 字段存储 | 考虑是否需要附件上传能力 |
| 商品名称未关联 Product | Procurement.productCode 为字符串，未关联 Product 实体 | 应通过 productCode 关联 Product 表获取 nameZh / nameEn |

---

## 第三步：验收

**对应文档**: `SPEC-B03-验货记录-步骤3.md` · `DOMAIN-发注管理领域模型.md` §1.5
**对应代码**: `QcRecord` 聚合根 ✅ 已实现

### 需要什么（输入）

**来自第二步**（自动代入）:

| 字段 | 来源 | 说明 |
|------|------|------|
| procurementId | Procurement.id | 关联采购单 |
| productCode | Procurement.productCode | 货号 |
| subProductCode | Procurement.subProductCode | 子货号 |
| quantity | Procurement.quantity | 订购数量 |
| orderDate | Procurement.orderDate | 下单日 |
| destination | Procurement.destination | 目的地 |
| material | Procurement.material | 材质 |
| priceRmb | Procurement.priceRmb | 参考含税价计算 |

**来自用户**（验货现场填入）:

| 字段 | 必填 | 说明 |
|------|------|------|
| qcCode | — | 系统生成，格式 `Q-YYYYMMDD-NNN` |
| sellerName | | 卖家名称（来自 Factory.factoryName 的代入？） |
| qcUserId | | 开单人 / 验货负责人（用户选择） |
| taxRefund | | 是否退税（来自 Procurement.billingType？如有退税则 true） |
| inspectionCount | | 检品数 |
| passedCount | | 合格数量 |
| defectiveCount | | 不良数量（可由 inspectionCount - passedCount 自动计算） |
| boxCount | | 箱数 |
| boxLengthCm / boxWidthCm / boxHeightCm | | 箱子尺寸 |
| netWeightPerUnit | | 单个净重(kg) |
| grossWeight | | 毛重(kg) |
| taxInclusivePrice | | 含税价 |
| qcDate | | 验货日期 |
| qcStandard | | 验收标准 |
| remarks | | 备注 |

**自动计算**:

| 字段 | 计算规则 | 说明 |
|------|------|------|
| defectiveCount | `inspectionCount - passedCount` | 已在 `QcRecord.calculateDefectiveCount()` 实现 |
| result | `defectiveCount == 0 ? PASS : FAIL` | 由 passedCount 决定 |

### 填入什么（输出 → 流向第四步）

| 字段 | 目标 | 说明 |
|------|------|------|
| procurementId | LogisticsPlan.procurementId | 关联调配计划 |
| productCode | LogisticsPlan.productCode | 自动带入 |
| boxLengthCm/boxWidthCm/boxHeightCm | LogisticsPlan.cargoLengthCm/widthCm/heightCm | 箱子尺寸作为货物尺寸参考 |
| result | 质量判定 | PASS → 进入调配；FAIL → 触发退货流程 |

### 缺口分析

| 问题 | 现状 | 建议 |
|------|------|------|
| sellerName 来源 | 用户输入，未与 Factory 联动 | 应默认代入 Factory.factoryName，用户可修改 |
| taxRefund 来源 | 用户手动选择 | 可从 Procurement.billingType 推断（超慧退税=需要退税）|
| qcUserId 选择器 | 无用户管理页面 | 需要系统用户/员工表 |
| 验货结果与步骤4联动 | QcRecord 已独立，但 PASS 后是否自动触发 LogisticsPlan 创建？ | 需要确认业务流程：自动生成调配计划 or 手动创建 |
| 退货触发 | result=FAIL 可触发退货（QcStatus.RETURN_REQUESTED） | 退货 Record 与原单独立，字段待确认 |

---

## 第四步：调配（海运 / 空运 / 拼柜）

**对应文档**: `SPEC-B04-调配计划-步骤4.md` · `DOMAIN-发注管理领域模型.md` §1.6
**对应代码**: `LogisticsPlan` ✅ · `Container` ✅ 已实现 · `ConsolidationPool` ✅ 已实现

### 需要什么（输入）

**来自第二步**（自动代入）:

| 字段 | 来源 | 说明 |
|------|------|------|
| productCode | Procurement.productCode | 货号 |
| factoryId | Procurement.factoryId | 关联工厂 |
| requiresQc | Procurement.requiresQc | 是否需要检测 |

**来自第三步**（可选带入）:

| 字段 | 来源 | 说明 |
|------|------|------|
| boxLengthCm/boxWidthCm/boxHeightCm | QcRecord.boxLengthCm/boxWidthCm/boxHeightCm | 箱子尺寸作为货物尺寸 |
| grossWeight | QcRecord.grossWeight | 毛重作为货物重量参考 |

**来自用户**（调配现场填入）:

| 字段 | 必填 | 说明 |
|------|------|------|
| planCode | — | 系统生成，格式 `L-YYYYMMDD-NNN` |
| procurementId | | 关联采购单（拼柜时可为空） |
| planType | ✅ | SEA(海运) / AIR(空运) / CONSOLIDATION(拼柜) |
| cargoLengthCm | | 货物长度(cm) |
| cargoWidthCm | | 货物宽度(cm) |
| cargoHeightCm | | 货物高度(cm) |
| cargoWeightKg | | 货物重量(kg) |
| quantity | | 数量（来自 Procurement.quantity） |
| estimatedShipDate | | 预计发货日 |
| actualShipDate | | 实际发货日 |
| containerId | 装柜后 | 货柜编号（装柜后赋值） |
| poolId | 拼柜后 | 拼柜池ID |
| remarks | | 备注 |

**自动计算**:

| 字段 | 计算规则 | 说明 |
|------|------|------|
| cargoVolumeCbm | `长×宽×高 / 1,000,000` | 已在 `LogisticsPlan.calculateVolume()` 实现 |

### 填入什么（输出 → 流向第五步）

| 字段 | 目标 | 说明 |
|------|------|------|
| containerId | Container.containerId | 装柜完成后关联 |
| departurePort | 国内报关单 | 起运港（宁波/上海/其他） |
| arrivalPort | 日本清关单 | 目的港 |
| cargoWeightKg | 日本清关单 | 申报重量 |
| cargoVolumeCbm | 日本清关单 | 申报体积 |

### 缺口分析

| 问题 | 现状 | 建议 |
|------|------|------|
| Container 实体 | ✅ 已实现 | `Container` 聚合根（v1.5.0） |
| ConsolidationPool 实体 | ✅ 已实现 | `ConsolidationPool` 聚合根（v1.5.0） |
| 起运港/目的港字段 | LogisticsPlan 无此字段 | 需要添加 departurePort / arrivalPort |
| 工厂联系人信息 | 从 Factory 带入 factoryId，但联系人字段不在 LogisticsPlan | 用户信息已通过 factoryId 关联查询 |
| 空运/海运判定 | 用户手动选择 planType | 可根据 cargoVolumeCbm + cargoWeightKg 自动推荐 |
| 拼柜规则 | 无自动化规则 | 可按工厂/发货日/目的港自动建议可拼柜的 Procurement |

---

## 第五步：国内报关

**对应文档**: `SPEC-B05-国内报关-步骤5.md` · `DB-05-domestic-customs.md`
**对应代码**: `DomesticCustomsRecord` ✅ 已实现

### 需要什么（输入）

| 字段 | 来源 | 说明 |
|------|------|------|
| procurementId | LogisticsPlan.procurementId | 关联采购单 |
| departurePort | LogisticsPlan 或用户 | 起运港 |
| productCode | LogisticsPlan.productCode | 货号（用于 HS 编码查询） |
| billingType | Procurement.billingType | 报关类型，影响申报方式 |
| cargoWeightKg | LogisticsPlan.cargoWeightKg | 货物重量 |
| cargoVolumeCbm | LogisticsPlan.cargoVolumeCbm | 货物体积 |
| customsRemarks | Procurement.customsRemarks | 报关备注 |

**用户填入**（待确认字段）:

| 字段 | 说明 |
|------|------|
| customsDeclarationNo | 报关单号（系统生成？海关返回？） |
| hsCode | HS 编码（根据 productCode 自动查询 Product 表？） |
| declarationDate | 申报日期 |
| declarant | 申报人 |
| exportPort | 出口口岸 |
| declaredValueRmb | 申报价值（CNY） |
| inspectionResult | 商检结果 |
| remarks | 备注 |

### 缺口分析

| 问题 | 现状 | 建议 |
|------|------|------|
| 所有字段 | 🔴 实体已实现，字段值待业务样本确认 | **需要业务方提供真实报关单样本** |
| HS 编码管理 | ✅ Product 表已有 hsCode 字段 | — |
| 与步骤4联动 | ✅ LogisticsPlan → DomesticCustomsRecord 手动创建 | 自动触发待实现 |
| 商检前置 | 部分商品需先商检再报关 | 需增加商检状态字段 |

---

## 第六步：日本清关

**对应文档**: `SPEC-B06-日本清关-步骤6.md` · `DB-06-japan-customs.md`
**对应代码**: `JapanCustomsRecord` ✅ 已实现（含自动创建 SalesRecord）

### 需要什么（输入）

| 字段 | 来源 | 说明 |
|------|------|------|
| procurementId | DomesticCustomsRecord 或 LogisticsPlan | 关联采购单 |
| arrivalPort | LogisticsPlan 或用户 | 目的港 |
| productCode | LogisticsPlan.productCode | 货号 |
| cargoWeightKg | LogisticsPlan.cargoWeightKg | 申报重量 |
| cargoVolumeCbm | LogisticsPlan.cargoVolumeCbm | 申报体积 |
| billingType | Procurement.billingType | 影响清关方式 |

**用户填入**（待确认字段）:

| 字段 | 说明 |
|------|------|
| customsEntryNo | 入境报关号 |
| arrivalDate | 到达日期 |
| customsBroker | 清关行 |
| brokerPhone | 清关行电话 |
| brokerContact | 清关行联系人 |
| importDutyPaid | 进口关税（已付） |
| consumptionTaxPaid | 消费税（已付） |
| clearanceDate | 清关完成日期 |
| remarks | 备注 |

### 缺口分析

| 问题 | 现状 | 建议 |
|------|------|------|
| 所有字段 | 🔴 实体已实现，字段值待业务样本确认 | **需要业务方提供真实清关文件样本** |
| 清关费用记账 | 无财务模块（设计决策：不做财务模块） | — |
| 与步骤7联动 | ✅ TaxRefundRecord 存在（手动创建，非自动触发） | — |

---

## 第七步：退税

**对应文档**: `SPEC-B07-退税-步骤7.md` · `DB-07-tax-refund.md`
**对应代码**: `TaxRefundRecord` ✅ 已实现（手动创建）

### 需要什么（输入）

| 字段 | 来源 | 说明 |
|------|------|------|
| procurementId | JapanCustomsRecord.procurementId | 关联采购单 |
| billingType | Procurement.billingType | 超慧退税=需要退税 |
| priceRmb | Procurement.priceRmb | 采购价 |
| quantity | Procurement.quantity | 采购数量 |
| exchangeRate | Procurement.exchangeRate | 退税计算用汇率 |
| taxPoint | Procurement.taxPoint | 票点 |
| customsEntryNo | JapanCustomsRecord | 报关单号 |

**用户填入**（待确认字段）:

| 字段 | 说明 |
|------|------|
| taxRefundAmountRmb | 退税金额（CNY） |
| taxRefundDate | 退税日期 |
| taxRefundBank | 退税银行账户 |
| taxRefundStatus | 退税状态（申请中/已退税/不退税） |
| remarks | 备注 |

### 缺口分析

| 问题 | 现状 | 建议 |
|------|------|------|
| 退税计算公式 | ✅ priceRmb × (taxPoint - 1) × quantity | 理论退税额公式已实现 |
| 退税状态管理 | ✅ APPLYING / COMPLETED / NO_REFUND | TaxRefundRecord.status 枚举已实现 |
| 与步骤8联动 | 无触发 | 运营销售由日本清关自动触发，非退税触发（设计决策） |

---

## 第八步：运营（销售）

**对应文档**: `SPEC-B08-运营销售-步骤8.md` · `DB-08-sales.md`
**对应代码**: `SalesRecord` ✅ 已实现（含 JapanCustomsClearedEvent 事件驱动自动创建 + 低库存触发补货反馈）

### 需要什么（输入）

| 字段 | 来源 | 说明 |
|------|------|------|
| productCode | Procurement.productCode | 货号 |
| subProductCode | Procurement.subProductCode | 子货号 |
| estimatedPriceJpy | Procurement.estimatedPriceJpy | 估算批发价（参考定价） |
| arrivalDate | JapanCustomsRecord.arrivalDate | 到货日期 |
| quantity | Procurement.quantity | 到货数量 |

**用户填入**（待确认字段）:

| 字段 | 说明 |
|------|------|
| listingDate | 上架日期 |
| sellingPriceJpy | 实际销售价（JPY） |
| salesChannel | 销售渠道（Amazon / メルカリ / 自社サイト / 其他） |
| initialStock | 初始库存 |
| currentStock | 当前库存（实时更新） |
| salesQuantity | 累计销量 |
| returnedQuantity | 退货数量 |

### 填入什么（输出 → 反馈循环）

| 字段 | 目标 | 说明 |
|------|------|------|
| salesQuantity | ReplenishmentDemand.quantity（参考） | 销量作为补货量参考 |
| currentStock | 库存预警 | 低于阈值时触发步骤1补货提醒 |
| returnedQuantity | 质量分析 | 退货率高时反馈至步骤3验货标准 |

### 缺口分析

| 问题 | 现状 | 建议 |
|------|------|------|
| 基础功能 | ✅ SalesRecord 聚合根已实现 | SalesRecord（v1.3.0）+ JapanCustomsClearedEvent 驱动 |
| 低库存反馈 | ✅ 低库存触发补货反馈 | 低库存自动生成 ReplenishmentDemand |
| 销售数据来源 | 待定 | 需要确认：平台 API 接入 or 人工维护 |
| 实时库存 | 待定 | 需要库存管理模块（当前无库存表） |

---

## 反馈循环分析

### 循环A — 补货闭环（正向销售驱动）

```
运营销售数据 → 实际销量统计 → 自动/手动生成补货需求
```

| 来源字段 | 目标字段 | 规则 |
|------|------|------|
| salesQuantity | ReplenishmentDemand.quantity | 建议补货量 = 过去N天平均销量 × 安全库存系数 |
| currentStock | 触发条件 | 库存 ≤ 阈值 → 自动提示补货 |
| returnedQuantity | 补货优先级 | 退货率 > X% → 质量复盘 |

### 循环B — 新品立项（市场反馈驱动）

```
市场/客户需求 → 新品信息登记 → 新品采购需求 → 完整采购流程
```

| 来源 | 说明 |
|------|------|
| 客户询价记录 | 新品意向 → ReplenishmentDemand(demandType=NEW_PURCHASE) |
| 竞争对手分析 | 新品货号规划 → Product 表新增主货号 |
| 销售预测 | 基于爆款分析 → 提前备货 |

### 循环C — 质量改进（退货驱动）

```
运营退货数据 → 退货率统计 → 验货标准升级 → 步骤3验货更严格
```

| 来源 | 目标 | 说明 |
|------|------|------|
| returnedQuantity | QcRecord.qcStandard | 退货原因分析后更新验货标准 |
| 退货率 | Factory 评级 | 高退货率 → Factory.status = INACTIVE |

---

## 商品数据库

### 主货号表（Product）

**对应文档**: `DOMAIN-发注管理领域模型.md` §1.4
**对应代码**: `Product` 🔴 部分字段存在，结构不完整

| 字段 | 类型 | 说明 |
|------|------|------|
| masterCode | String | 主货号（如 `odn012`），唯一键之一 |
| subCode | String | 子货号（如 `re`=红色，`wh`=白色，`bk`=黑色），可为空 |
| nameJa | String | 商品名称（日文） |
| nameZh | String | 商品名称（中文） |
| nameEn | String | 商品名称（英文，报关用） |
| colorName | String | 颜色名称（日文：黒/白/赤） |
| material | String | 材质 |
| productCategory | Enum | OEM / ORDINARY / FACTORY_DIRECT |
| lengthCm / widthCm / heightCm | BigDecimal | 单品尺寸(cm) |
| weightKg | BigDecimal | 单品净重(kg) |
| packageHeightCm / packageWidthCm / packageDepthCm | BigDecimal | 外箱尺寸 |
| packageWeightKg | BigDecimal | 外箱毛重 |
| unitsPerPackage | Integer | 段ボール入数（每箱数量） |
| hsCode | String | HS 编码（报关用，**新增**） |
| taxPoint | BigDecimal | 票点（默认 1.1，**新增**） |
| warehouse | String | 仓库归属（名古屋/久留米/永康） |
| requiresQc | Boolean | 是否需要检测 |
| imageUrls | JSON | 商品图片URL列表 |
| remarks | String | 备注（箱规不固定/整托不固定等） |

**唯一键**: `(masterCode, subCode)` — 复合唯一
**缺口**: hsCode / taxPoint 字段在现有 Product 实体中不存在，需新增

### 工厂表（Factory）

**对应文档**: `DB-10-factory.md` · `DOMAIN-发注管理领域模型.md` §1.2
**对应代码**: `Factory` ✅ 已实现（v1.4.0 字段完整）

| 字段 | 现状 | 说明 |
|------|------|------|
| factoryCode | ✅ 已有（F-YYYYMMDD-NNN） | — |
| factoryName | ✅ 已有 | — |
| province / city / county | ✅ 已有 | location 已拆分为省/市/县三级 |
| roughLocation | ✅ 已有 | 详细地址 |
| longitude / latitude | ✅ 已有 | 经纬度 |
| contactName | ✅ 已有 | — |
| contactPhone | ✅ 已有 | — |
| contactWechat | ✅ 已有 | — |
| contactQq | ✅ 已有 | — |
| category | ✅ 已有（FactoryCategory） | 12种行业分类 |
| cooperationStatus | ✅ 已有（CooperationStatus） | 替代原 status |
| paymentTerms | ✅ 已有（PaymentTerms） | CASH/NET_30/60/90/CREDIT |

---

## 缺口总表

| 优先级 | 步骤 | 缺口 | 影响 |
|--------|------|------|------|
| P0 | — | 现有缺口已全部清理（见各步缺口分析） | — |
| P1 | 2 | Factory 微信/QQ/经纬度已实现 | ✅ v1.4.0 |
| P1 | 3 | sellerName 从 Factory 自动代入 | ✅ |
| P1 | 3 | 用户管理（Phase 3）已完成 | ✅ |
| P2 | 1 | 无历史采购价参照 | 补货无定价参考 |
| P2 | 8 | 库存管理模块缺失 | 实时库存/库存预警功能无法实现 |

---

## 与现有文档对照

| 现有文档 | 覆盖范围 | 缺失内容 |
|---------|---------|---------|
| SPEC-B01~B04 | 步骤1-4 完整字段清单 | 步骤5-8（占位） |
| SPEC-B05~B08 | 步骤5-8 占位字段清单 | 真实业务样本 |
| SPEC-B09 | 订单总览 API 设计 | 前端实现 |
| DOMAIN-发注管理领域模型.md | 聚合根/枚举/仓储接口 | 步骤5-8 领域模型 |
| SPEC-B04-调配计划-步骤4.md | 步骤4 详细设计（参考） | Container 字段 |
| SPEC-B03-验货记录-步骤3.md | 步骤3 详细设计（参考） | sellerName 来源 |

---

## 下一步行动

1. **代码实现**（已完成的 P0/P1/P2 项）
   - [x] ✅ Product 表 hsCode / taxPoint（V30 MySQL 直接迁移）
   - [x] ✅ Factory 表 wechat / qq / longitude / latitude（全字段已实现）
   - [x] ✅ DomesticCustomsRecord 聚合根（国内报关 v1.3.0）
   - [x] ✅ JapanCustomsRecord 聚合根（日本清关 v1.2.0）
   - [x] ✅ TaxRefundRecord 聚合根（退税 v1.1.0）
   - [x] ✅ SalesRecord 聚合根（运营销售 v1.2.0）
   - [x] ✅ Container / ConsolidationPool（v1.5.0 已实现）

2. **业务方确认**（仍在阻塞中）
   - [ ] 提供真实国内报关单样本 → 补充 HS 编码/申报价值/出口口岸等字段
   - [ ] 确认运营销售数据来源（平台 API / 人工维护）

3. **反馈循环设计**
   - [ ] 确定库存预警阈值机制
   - [ ] 设计 ReplenishmentDemand 自动生成规则
   - [ ] 退货率 → Factory 评级联动
