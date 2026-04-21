# 测试数据集

> 用于手动测试、API 契约验证、端到端业务流测试。

---

## 1. Factory 工厂

```json
[
  {
    "factoryCode": "F-TEST-001",
    "factoryName": "杭州测试服饰厂",
    "contactName": "张工",
    "contactPhone": "138-0000-0001",
    "contactWechat": "zhang_test",
    "location": "浙江省杭州市余杭区",
    "status": "ACTIVE"
  },
  {
    "factoryCode": "F-TEST-002",
    "factoryName": "宁波玩具制造厂",
    "contactName": "李经理",
    "contactPhone": "138-0000-0002",
    "location": "浙江省宁波市北仑区",
    "status": "ACTIVE"
  },
  {
    "factoryCode": "F-TEST-003",
    "factoryName": "义乌小商品加工厂",
    "contactName": "王老板",
    "contactPhone": "138-0000-0003",
    "location": "浙江省义乌市",
    "status": "INACTIVE"
  }
]
```

---

## 2. Procurement 发注单

> 注意：`taxPoint` 字段须使用 `1.1` 格式（H2 列宽限制）。

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

```json
[
  {
    "procurementId": 1,
    "productCode": "odn-test-001",
    "subProductCode": "red",
    "sellerName": "杭州测试服饰厂",
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
    "procurementId": 2,
    "productCode": "odn-test-002",
    "sellerName": "杭州测试服饰厂",
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
    "procurementId": 3,
    "productCode": "odn-test-003",
    "sellerName": "宁波玩具制造厂",
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

```json
[
  {
    "procurementId": 1,
    "factoryId": 1,
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

## 5. Container 货柜（待实现）

```json
[
  {
    "containerCode": "C-20260428-001",
    "containerNo": "TEMU1234567",
    "containerType": "40GP",
    "sealNo": "SEAL-2026-001",
    "planType": "SEA",
    "estimatedDepartureDate": "2026-04-28",
    "actualDepartureDate": "2026-04-28",
    "estimatedArrivalDate": "2026-05-15",
    "departurePort": "宁波",
    "arrivalPort": "东京",
    "status": "LOADING"
  }
]
```

---

## 6. ConsolidationPool 拼柜池（待实现）

```json
[
  {
    "poolCode": "P-20260428-001",
    "destination": "久留米",
    "departurePort": "宁波",
    "arrivalPort": "门司",
    "planType": "CONSOLIDATION",
    "status": "LOADING",
    "totalWeight": 320.5,
    "totalVolume": 2.4,
    "remarks": "久留米方向拼柜"
  }
]
```
