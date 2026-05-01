# API 契约测试文档

> 基于 REST Assured + JUnit 5，验证 API 响应结构与状态码。
> ⚠️ 响应/请求字段必须与 `application/dto/` 下实际的 Cmd/Query 类完全对齐。

---

## 1. QcRecord 验货记录 API

### 1.1 GET /api/v1/qc-records

**期望响应**（`QcRecordPageQuery.java`）:
```json
{
  "code": "ok",
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "qcCode": "Q-20260421-001",
        "procurementId": 1,
        "shipmentBatchId": 1,
        "sellerName": "杭州测试服饰厂",
        "factoryId": 1,
        "factoryName": "杭州测试服饰厂",
        "productCode": "odn-test-001",
        "subProductCode": "red",
        "qcUserId": null,
        "qcType": "ONSITE",
        "qcDate": "2026-04-15",
        "result": "PASS",
        "status": "COMPLETED",
        "inspectionCount": 100,
        "passedCount": 97,
        "defectiveCount": 3,
        "boxCount": 10,
        "boxLengthCm": 40.00,
        "boxWidthCm": 30.00,
        "boxHeightCm": 20.00,
        "netWeightPerUnit": 0.50,
        "grossWeight": 55.00,
        "taxInclusivePrice": 6000.00,
        "material": "棉",
        "taxRefund": true,
        "qcStandard": "外观无破损",
        "remarks": "抽检",
        "images": null,
        "destination": "东京",
        "quantity": 100,
        "orderDate": "2026-04-01",
        "createBy": "admin",
        "createTime": "2026-04-21T10:00:00",
        "updateTime": null
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "pageNumber": 0
  }
}
```

**HTTP 状态码**: 200
**认证**: Bearer Token

### 1.2 POST /api/v1/qc-records

> `QcRecordCreateCmd.java` — `shipmentBatchId` 和 `productCode` 为必填字段（`@NotNull`）

**请求体**:
```json
{
  "shipmentBatchId": 1,
  "procurementId": 1,
  "sellerName": "杭州测试服饰厂",
  "productCode": "odn-test-001",
  "subProductCode": "red",
  "qcUserId": null,
  "qcType": "ONSITE",
  "qcDate": "2026-04-15",
  "result": "PASS",
  "status": "COMPLETED",
  "inspectionCount": 100,
  "passedCount": 97,
  "boxCount": 10,
  "boxLengthCm": 40,
  "boxWidthCm": 30,
  "boxHeightCm": 20,
  "netWeightPerUnit": 0.5,
  "grossWeight": 55,
  "taxInclusivePrice": 6000,
  "material": "棉",
  "taxRefund": true,
  "qcStandard": "外观无破损",
  "remarks": "抽检"
}
```

**期望响应**:
```json
{
  "code": "ok",
  "message": "success",
  "data": 1
}
```

**HTTP 状态码**: 200

**异常: shipmentBatchId 为空**:
```json
// 请求体缺少 shipmentBatchId
```
→ 期望: 400，message 包含 "关联出货批次不能为空"

**异常: productCode 为空**:
```json
{
  "shipmentBatchId": 1
  // 缺少 productCode
}
```
→ 期望: 400，message 包含 "货号不能为空"

### 1.3 PATCH /api/v1/qc-records/{id}

**请求体** (状态流转):
```json
{
  "status": "COMPLETED",
  "result": "PASS",
  "passedCount": 97
}
```

**期望响应**: `{"code":"ok","message":"success","data":null}`
**HTTP 状态码**: 200

**异常: 验货数量校验**:
```json
{
  "passedCount": 150  // 超过 inspectionCount
}
```
→ 期望: 400 或 UseCase 抛出业务异常

### 1.4 DELETE /api/v1/qc-records/{id}

**期望响应**: `{"code":"ok","message":"success","data":null}`
**HTTP 状态码**: 200

---

## 2. LogisticsPlan 调配计划 API

### 2.1 GET /api/v1/logistics-plans

**期望响应**（`LogisticsPlanPageQuery.java`）:
```json
{
  "code": "ok",
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "planCode": "L-20260421-001",
        "containerNo": null,
        "qcRecordId": 1,
        "qcCode": "Q-20260421-001",
        "procurementId": 1,
        "factoryId": 1,
        "factoryName": "杭州测试服饰厂",
        "productCode": "odn-test-001",
        "subProductCode": "red",
        "planType": "SEA",
        "status": "PLANNED",
        "cargoLengthCm": 50.00,
        "cargoWidthCm": 40.00,
        "cargoHeightCm": 30.00,
        "cargoWeightKg": 25.50,
        "cargoVolumeCbm": 0.06000,
        "quantity": 100,
        "requiresQc": true,
        "containerId": null,
        "poolId": null,
        "estimatedShipDate": "2026-04-25",
        "actualShipDate": null,
        "remarks": "海运货物",
        "createBy": "admin",
        "createTime": "2026-04-21T10:00:00",
        "updateTime": null
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "pageNumber": 0
  }
}
```

**HTTP 状态码**: 200

### 2.2 POST /api/v1/logistics-plans

**请求体**:
```json
{
  "procurementId": 1,
  "factoryId": 1,
  "productCode": "odn-test-001",
  "planType": "SEA",
  "cargoLengthCm": 50,
  "cargoWidthCm": 40,
  "cargoHeightCm": 30,
  "cargoWeightKg": 25.5,
  "quantity": 100,
  "requiresQc": true,
  "estimatedShipDate": "2026-04-25"
}
```

**期望响应**:
```json
{
  "code": "ok",
  "message": "success",
  "data": 1
}
```

### 2.3 PATCH /api/v1/logistics-plans/{id}

**状态流转**:
```
PLANNED → BOOKED → IN_TRANSIT → DELIVERED
```

**请求体**:
```json
{
  "status": "BOOKED"
}
```

**期望响应**: `{"code":"ok","message":"success","data":null}`

**异常: 已终态禁止修改**:
→ 期望: 业务异常 "调配计划已完成，禁止修改"

**异常: 删除已运输记录**:
→ 期望: 业务异常 "已完成/运输中的调配计划禁止删除"

### 2.4 DELETE /api/v1/logistics-plans/{id}

**期望响应**: `{"code":"ok","message":"success","data":null}`
**限制**: `DELIVERED` / `IN_TRANSIT` 状态禁止删除

---

## 3. 业务流端到端测试脚本

```bash
#!/bin/bash
# manpou-allinone 端口
BASE="http://localhost:18090/api/v1"

# 获取测试令牌（admin/admin123）
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

H="Authorization: Bearer $TOKEN"

# Step 1: 创建验货记录（shipmentBatchId 必填）
QC_ID=$(curl -s -X POST $BASE/qc-records $H \
  -H "Content-Type: application/json" \
  -d '{"shipmentBatchId":1,"procurementId":1,"productCode":"odn-test-001","qcType":"ONSITE","qcDate":"2026-04-15","result":"PASS","status":"COMPLETED","inspectionCount":100,"passedCount":97}' \
  | grep -o '"data":[0-9]*' | cut -d':' -f2)
echo "Created QC record: $QC_ID"

# Step 2: 创建调配计划（factoryId 必填）
LP_ID=$(curl -s -X POST $BASE/logistics-plans $H \
  -H "Content-Type: application/json" \
  -d '{"procurementId":1,"factoryId":1,"productCode":"odn-test-001","planType":"SEA","cargoLengthCm":50,"cargoWidthCm":40,"cargoHeightCm":30,"cargoWeightKg":25.5}' \
  | grep -o '"data":[0-9]*' | cut -d':' -f2)
echo "Created LogisticsPlan: $LP_ID"

# Step 3: 状态流转
curl -s -X PATCH $BASE/logistics-plans/$LP_ID $H \
  -H "Content-Type: application/json" \
  -d '{"status":"BOOKED"}' | grep -o '"code":"[^"]*"'

curl -s -X PATCH $BASE/logistics-plans/$LP_ID $H \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_TRANSIT"}' | grep -o '"code":"[^"]*"'
```

---

## 4. 状态机测试矩阵

### QcRecord

| 当前状态 | 目标状态 | 结果 |
|---------|---------|------|
| PENDING | COMPLETED | ✅ |
| PENDING | RETURN_REQUESTED | ✅ |
| PENDING | PENDING | ✅ |
| COMPLETED | PENDING | ❌ |
| RETURN_REQUESTED | COMPLETED | ✅ |
| RETURN_REQUESTED | PENDING | ✅ |
| COMPLETED | RETURN_REQUESTED | ✅ |

### LogisticsPlan

| 当前状态 | 目标状态 | 结果 |
|---------|---------|------|
| PLANNED | BOOKED | ✅ |
| BOOKED | IN_TRANSIT | ✅ |
| IN_TRANSIT | DELIVERED | ✅ |
| DELIVERED | BOOKED | ❌ 禁止 |
| DELIVERED | IN_TRANSIT | ❌ 禁止 |
| PLANNED | DELIVERED | ❌ 禁止 |

### Container

| 当前状态 | 目标状态 | 结果 |
|---------|---------|------|
| CREATED | LOADED | ✅ |
| CREATED | DEPARTED | ❌ 禁止（须先 LOADED） |
| LOADED | DEPARTED | ✅ |
| LOADED | CREATED | ❌ 禁止 |
| DEPARTED | ARRIVED | ✅ |
| ARRIVED | (终态) | - |

### ConsolidationPool

| 当前状态 | 目标状态 | 结果 |
|---------|---------|------|
| OPEN | PENDING | ✅ (封池) |
| OPEN | LOADED | ✅ (直接装柜) |
| PENDING | LOADED | ✅ |
| PENDING | OPEN | ❌ 禁止 |
| LOADED | SHIPPED | ✅ |
| LOADED | OPEN | ❌ 禁止 |
| SHIPPED | (终态) | - |
