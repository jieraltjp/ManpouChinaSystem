# 测试数据集

> 用于手动测试、API 契约验证、端到端业务流测试。
>
> ⚠️ 所有 JSON 数据必须与实体字段完全对齐，参考 `apps/manpou-allinone/src/main/java/com/manpou/allinone/{domain}/domain/model/` 下的 JPA 实体。

---

## 1. Factory 工厂

> 实体：`Factory.java`，必填字段：`factoryCode`、`factoryName`、`category`、`province`、`city`、`county`、`cooperationStatus`、`paymentTerms`

```json
[
  {
    "factoryCode": "F-20260501-001",
    "factoryName": "杭州测试服饰厂",
    "category": "APPAREL",
    "province": "浙江省",
    "city": "杭州市",
    "county": "余杭区",
    "roughLocation": "文一西路1000号",
    "contactName": "张工",
    "contactPhone": "138-0000-0001",
    "contactWechat": "zhang_test",
    "contactQq": "123456",
    "cooperationStatus": "ACTIVE",
    "paymentTerms": "NET_30",
    "notes": "测试用工厂"
  },
  {
    "factoryCode": "F-20260501-002",
    "factoryName": "宁波玩具制造厂",
    "category": "TOYS",
    "province": "浙江省",
    "city": "宁波市",
    "county": "北仑区",
    "roughLocation": "开发区工业园1号",
    "contactName": "李经理",
    "contactPhone": "138-0000-0002",
    "contactWechat": "li_toys",
    "cooperationStatus": "ACTIVE",
    "paymentTerms": "NET_30"
  },
  {
    "factoryCode": "F-20260501-003",
    "factoryName": "义乌小商品加工厂",
    "category": "GENERAL",
    "province": "浙江省",
    "city": "金华市",
    "county": "义乌市",
    "roughLocation": "国际商贸城旁",
    "contactName": "王老板",
    "contactPhone": "138-0000-0003",
    "cooperationStatus": "INACTIVE",
    "paymentTerms": "NET_15"
  }
]
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `category` | FactoryCategory 枚举 | `APPAREL` / `TOYS` / `GENERAL` / `ELECTRONICS` / `FOOD` / `OTHER` |
| `cooperationStatus` | CooperationStatus 枚举 | `POTENTIAL` / `ACTIVE` / `INACTIVE` / `BLACKLISTED` |
| `paymentTerms` | PaymentTerms 枚举 | `NET_15` / `NET_30` / `NET_60` / `PREPAID` / `COD` |

---

## 2. Procurement 发注单

> 实体：`Procurement.java`（v1.5.0）
> 注意：`taxPoint` 字段须使用 `"1.1000"` 格式（H2 列宽限制）。

```json
[
  {
    "factoryId": 1,
    "productCode": "odn-test-001",
    "subProductCode": "red",
    "quantity": 100,
    "priceRmb": 30.00,
    "exchangeRate": 21.5,
    "taxPoint": "1.1000",
    "billingType": "ZHE_LU_KAI_PIAO",
    "status": "未定",
    "orderDate": "2026-04-01",
    "plannedShipDate": "2026-04-20",
    "destination": "东京",
    "requiresQc": true,
    "material": "棉",
    "chinaLead": "李华"
  },
  {
    "factoryId": 1,
    "productCode": "odn-test-002",
    "quantity": 50,
    "priceRmb": 45.00,
    "exchangeRate": 21.5,
    "taxPoint": "1.1000",
    "billingType": "CHAO_HUI_TUI_SHUI",
    "status": "未定",
    "orderDate": "2026-04-05",
    "plannedShipDate": "2026-04-25",
    "destination": "大阪",
    "requiresQc": false
  },
  {
    "factoryId": 2,
    "productCode": "odn-test-003",
    "quantity": 200,
    "priceRmb": 15.00,
    "exchangeRate": 21.5,
    "taxPoint": "1.1000",
    "billingType": "NO_REFUND",
    "status": "未定",
    "orderDate": "2026-04-10",
    "plannedShipDate": "2026-04-30",
    "destination": "久留米",
    "requiresQc": true
  }
]
```

---

## 3. QcRecord 验货记录

> 实体：`QcRecord.java`（v1.5.0，V43 新增 shipmentBatchId 必填）
> 关联字段：`shipmentBatchId`（必填）、`procurementId`（可选，审计追溯用）

```json
[
  {
    "shipmentBatchId": 1,
    "procurementId": 1,
    "sellerName": "杭州测试服饰厂",
    "productCode": "odn-test-001",
    "subProductCode": "red",
    "qcType": "ONSITE",
    "qcDate": "2026-04-15",
    "result": "PASS",
    "status": "COMPLETED",
    "inspectionCount": 100,
    "passedCount": 97,
    "defectiveCount": 3,
    "boxCount": 10,
    "boxLengthCm": 40,
    "boxWidthCm": 30,
    "boxHeightCm": 20,
    "netWeightPerUnit": 0.5,
    "grossWeight": 55,
    "taxInclusivePrice": 6000,
    "material": "棉",
    "taxRefund": true,
    "qcStandard": "外观无破损，尺寸偏差≤2mm",
    "remarks": "抽检比例 5%",
    "destination": "东京",
    "quantity": 100,
    "orderDate": "2026-04-01"
  },
  {
    "shipmentBatchId": 2,
    "procurementId": 2,
    "sellerName": "杭州测试服饰厂",
    "productCode": "odn-test-002",
    "qcType": "REMOTE",
    "qcDate": "2026-04-16",
    "result": "FAIL",
    "status": "RETURN_REQUESTED",
    "inspectionCount": 50,
    "passedCount": 35,
    "defectiveCount": 15,
    "boxCount": 5,
    "qcStandard": "外观无破损",
    "remarks": "15件存在色差超标",
    "destination": "大阪"
  },
  {
    "shipmentBatchId": 3,
    "procurementId": 3,
    "sellerName": "宁波玩具制造厂",
    "productCode": "odn-test-003",
    "qcType": "ONSITE",
    "qcDate": "2026-04-18",
    "result": "PASS",
    "status": "PENDING",
    "inspectionCount": 0,
    "passedCount": 0,
    "boxCount": 20,
    "qcStandard": "玩具安全标准 GB 6675",
    "destination": "久留米"
  }
]
```

---

## 4. LogisticsPlan 调配计划

> 实体：`LogisticsPlan.java`（v1.3.0+ 新增 qcRecordId / containerNo）

```json
[
  {
    "procurementId": 1,
    "factoryId": 1,
    "qcRecordId": 1,
    "productCode": "odn-test-001",
    "subProductCode": "red",
    "planType": "SEA",
    "status": "PLANNED",
    "cargoLengthCm": 50,
    "cargoWidthCm": 40,
    "cargoHeightCm": 30,
    "cargoWeightKg": 25.5,
    "quantity": 100,
    "requiresQc": true,
    "estimatedShipDate": "2026-04-25",
    "remarks": "海运货物，注意防潮"
  },
  {
    "procurementId": 2,
    "factoryId": 1,
    "qcRecordId": 2,
    "productCode": "odn-test-002",
    "planType": "AIR",
    "status": "BOOKED",
    "cargoLengthCm": 30,
    "cargoWidthCm": 20,
    "cargoHeightCm": 15,
    "cargoWeightKg": 5.0,
    "quantity": 50,
    "requiresQc": false,
    "estimatedShipDate": "2026-04-20",
    "actualShipDate": "2026-04-19",
    "remarks": "急单，空运优先"
  },
  {
    "procurementId": 3,
    "factoryId": 2,
    "qcRecordId": 3,
    "productCode": "odn-test-003",
    "planType": "CONSOLIDATION",
    "status": "IN_TRANSIT",
    "cargoLengthCm": 60,
    "cargoWidthCm": 50,
    "cargoHeightCm": 40,
    "cargoWeightKg": 120.0,
    "quantity": 200,
    "requiresQc": true,
    "estimatedShipDate": "2026-04-28",
    "actualShipDate": "2026-04-28",
    "containerId": 1,
    "remarks": "拼柜货物，同目的港合并"
  }
]
```

---

## 5. Container 货柜

> 实体：`Container.java`（v1.5.0，SPEC-B00 Issue #8）
> ⚠️ 字段与旧文档完全不同，已按实际实体重写。

```json
[
  {
    "containerNo": "TEMU1234567",
    "containerType": "GP40",
    "totalCbm": 0,
    "totalWeightKg": 0,
    "planCount": 0,
    "poolId": 1,
    "status": "CREATED",
    "loadDate": null,
    "departureDate": null,
    "arrivalDate": null
  },
  {
    "containerNo": "TEMU7654321",
    "containerType": "HC40",
    "totalCbm": 65.5000,
    "totalWeightKg": 12500.0000,
    "planCount": 5,
    "poolId": 1,
    "status": "LOADED",
    "loadDate": "2026-04-28",
    "departureDate": "2026-04-28",
    "arrivalDate": null
  }
]
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `containerType` | ContainerType 枚举 | `GP20` / `GP40` / `HC40` / `RF40` |
| `status` | ContainerStatus 枚举 | `CREATED` → `LOADED` → `DEPARTED` → `ARRIVED` |
| `totalCbm` | BigDecimal | 当前已装体积 |
| `totalWeightKg` | BigDecimal | 当前已装重量 |
| `planCount` | Integer | 已装调配计划数量 |

---

## 6. ConsolidationPool 拼柜池

> 实体：`ConsolidationPool.java`（v1.5.0，SPEC-B00 Issue #8）
> ⚠️ 字段与旧文档完全不同，已按实际实体重写。

```json
[
  {
    "poolCode": "CP-20260501-001",
    "destinationPort": "久留米",
    "totalCbm": 0,
    "totalWeightKg": 0,
    "planCount": 0,
    "containerThresholdCbm": 70,
    "status": "OPEN"
  },
  {
    "poolCode": "CP-20260501-002",
    "destinationPort": "东京",
    "totalCbm": 45.5000,
    "totalWeightKg": 8500.0000,
    "planCount": 3,
    "containerThresholdCbm": 70,
    "status": "OPEN"
  }
]
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `destinationPort` | String | 目的港（非 `destination`） |
| `totalCbm` | BigDecimal | 当前池内货物总体积（非 `totalVolume`） |
| `totalWeightKg` | BigDecimal | 当前池内货物总重量（非 `totalWeight`） |
| `containerThresholdCbm` | BigDecimal | 触发装柜的体积阈值（默认 70） |
| `status` | ConsolidationPoolStatus 枚举 | `OPEN` → `PENDING` → `LOADED` → `SHIPPED` |

### 关键领域方法（测试重点）

| 方法 | 说明 |
|------|------|
| `isReadyToLoad()` | `totalCbm >= containerThresholdCbm` 时可触发装柜 |
| `addPlan(volume, weight)` | 状态 OPEN 时添加货物，更新体积/重量/计数 |
| `removePlan(volume, weight)` | 非 LOADED/SHIPPED 时可移除 |
| `closeToPending()` | 封池，仅 OPEN → PENDING |
| `markLoaded()` | 装柜完成 |
| `markShipped()` | 出港 |
