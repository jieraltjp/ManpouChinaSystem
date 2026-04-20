# 发注管理 — API 契约

> **版本**: 1.0.0
> **更新**: 2026-04-20

---

## 统一返回结构

```json
{
  "code": "ok",
  "message": "success",
  "data": { ... },
  "traceId": "uuid",
  "detail": null
}
```

**错误 code 规范**：
- `validation.*` — 参数校验失败
- `auth.*` — 认证权限错误
- `not_found` — 资源不存在
- `business.*` — 业务规则违反

---

## 1. 发注单

### 1.1 创建发注单

```
POST /api/v1/procurements
```

**请求体**：
```json
{
  "name": "张三样品订单",
  "factoryId": 1,
  "priority": "STANDARD",
  "remarks": "急单，优先排产"
}
```

**priority 枚举**：`STANDARD` | `PRODUCTION_FIRST` | `SHIP_FIRST`

**响应**：`201 Created`
```json
{
  "code": "ok",
  "data": {
    "id": 1,
    "name": "张三样品订单",
    "factoryId": 1,
    "status": "PENDING",
    "priority": "STANDARD",
    "createdAt": "2026-04-20T10:00:00+09:00"
  }
}
```

---

### 1.2 查询发注单列表

```
GET /api/v1/procurements
```

**Query 参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| status | string | 状态过滤 |
| factoryId | long | 厂家过滤 |
| page | int | 页码，默认0 |
| size | int | 页大小，默认20 |

**响应**：`200 OK`
```json
{
  "code": "ok",
  "data": {
    "content": [...],
    "totalElements": 42,
    "totalPages": 3,
    "pageNumber": 0
  }
}
```

---

### 1.3 获取发注单详情

```
GET /api/v1/procurements/{id}
```

**响应**：`200 OK`
```json
{
  "code": "ok",
  "data": {
    "id": 1,
    "name": "张三样品订单",
    "factoryId": 1,
    "factoryName": "深圳某某玩具厂",
    "status": "IN_PROGRESS",
    "priority": "PRODUCTION_FIRST",
    "shippingMode": "WAREHOUSE",
    "qcType": "ONSITE",
    "qcResult": null,
    "container": null,
    "finance": null,
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```

---

### 1.4 更新发注单状态

```
PATCH /api/v1/procurements/{id}/status
```

**请求体**：
```json
{
  "status": "SUSPENDED",
  "reason": "厂家交期延误"
}
```

**状态推进规则**：

| 当前状态 | 可转向 |
|----------|--------|
| PENDING | IN_PROGRESS, SUSPENDED |
| SUSPENDED | PENDING |
| IN_PROGRESS | QC_PENDING |
| QC_PENDING | QC_PASSED, REJECTED |
| REJECTED | PENDING |
| QC_PASSED | SHIPPING |
| SHIPPING | CLOSED |
| CLOSED | —（终态） |

**响应**：`200 OK`

---

### 1.5 删除发注单

```
DELETE /api/v1/procurements/{id}
```

- 仅 `PENDING` / `SUSPENDED` 状态可删除
- `CLOSED` 禁止删除

---

## 2. 商品信息

### 2.1 录入商品

```
POST /api/v1/procurements/{id}/items
```

**请求体**：
```json
{
  "productName": "ABS 遥控车模",
  "productCode": "CAR-2026-001",
  "unitPriceCny": 45.00,
  "exchangeRate": 21.5,
  "taxRate": 13,
  "material": "ABS塑料",
  "weightKg": 0.35,
  "lengthCm": 25,
  "widthCm": 12,
  "heightCm": 8,
  "quantity": 500,
  "remarks": "中性包装"
}
```

**字段说明**：
| 字段 | 必填 | 说明 |
|------|------|------|
| productName | ✅ | 商品名称 |
| productCode | ✅ | 商品编号 |
| unitPriceCny | ✅ | 单价(人民币) |
| exchangeRate | ✅ | 汇率(人民币→日元) |
| taxRate | ✅ | 票点(%) |
| material | | 材质 |
| weightKg | | 重量(kg) |
| lengthCm/widthCm/heightCm | | 尺寸(cm) |
| quantity | ✅ | 数量 |

**计算字段**（服务端自动）：
- `unitPriceJpy = unitPriceCny × exchangeRate`
- `taxJpy = unitPriceJpy × taxRate / 100`
- `volumeCbm = lengthCm × widthCm × heightCm / 1_000_000`

---

## 3. 验收

### 3.1 指派验收人

```
POST /api/v1/procurements/{id}/qc
```

**请求体**：
```json
{
  "qcType": "ONSITE",
  "qcUserId": 3
}
```

**qcType**：`ONSITE`(现场验收) | `REMOTE`(远程图片验收)

---

### 3.2 提交验收结果

```
POST /api/v1/procurements/{id}/qc/result
```

**请求体**：
```json
{
  "result": "PASS",
  "images": ["https://...", "https://..."],
  "remarks": "外观无瑕疵，数量相符"
}
```

**result**：`PASS` | `REJECT`

---

## 4. 运输模式

### 4.1 选择运输模式

```
POST /api/v1/procurements/{id}/shipping-mode
```

**请求体**：
```json
{
  "mode": "WAREHOUSE",
  "warehouseId": 1
}
```

**mode**：`WAREHOUSE`(自有仓库) | `POOL`(虚拟拼柜) | `DIRECT`(厂家直装)

---

## 5. 货柜管理

### 5.1 录入货柜信息

```
POST /api/v1/procurements/{id}/container
```

**请求体**：
```json
{
  "containerNo": "MSKU1234567",
  "containerType": "40HC",
  "shippingMethod": "SEA",
  "portOfLoading": "YANTIAN",
  "portOfDestination": "OSAKA",
  "estimatedShipDate": "2026-05-15",
  "cutoffDate": "2026-05-10"
}
```

**containerType**：`20GP` | `40GP` | `40HC` | `45HC`

**shippingMethod**：`SEA` | `AIR` | `LAND`

---

## 6. 财务结算

### 6.1 录入结算信息

```
POST /api/v1/procurements/{id}/finance
```

**请求体**：
```json
{
  "taxType": "EXPORT_REFUND",
  "totalCostCny": 22500.00,
  "actualPaidCny": 21000.00,
  "currency": "CNY",
  "remarks": "含运费，扣减损坏赔款"
}
```

**taxType**：`EXPORT_REFUND`(出口退税) | `NO_TAX`(不开票) | `INCLUSIVE_TAX`(含税)

---

## 7. 拼柜池

### 7.1 查询拼柜池

```
GET /api/v1/pool
```

**Query**：`destinationPort` | `status` | `minCbm`

**响应**：
```json
{
  "code": "ok",
  "data": {
    "poolId": 1,
    "destinationPort": "OSAKA",
    "totalCbm": 28.5,
    "totalBoxes": 120,
    "containerThresholdCbm": 60.0,
    "fillRate": 0.475,
    "status": "POOL_PENDING",
    "procurementIds": [3, 7, 12]
  }
}
```

### 7.2 触发拼柜（生成货柜计划）

```
POST /api/v1/pool/{id}/consolidate
```

- 仅 `POOL_READY` 状态可调用
- 自动分配集装箱号/封号

---

## 8. 错误码

| code | HTTP | 说明 |
|------|------|------|
| validation.param.required | 400 | 必填参数缺失 |
| validation.param.invalid | 400 | 参数值非法 |
| auth.unauthorized | 401 | 未认证 |
| auth.forbidden | 403 | 无权操作 |
| not_found | 404 | 发注单不存在 |
| business.status_forbidden | 422 | 状态不允许此操作 |
| business.cannot_modify_closed | 422 | 已闭环不可修改 |
| server.error | 500 | 系统异常 |
