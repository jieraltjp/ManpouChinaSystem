# 发注管理 — API 契约

> **版本**: 1.1.0
> **更新**: 2026-04-20
> **依据**: `docs/发注管理体系升级.pdf` + `docs/新発注管理-設計図.xlsx`

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

---

## 1. 发注单（Procurement / 出货单）

### 1.1 创建出货单

```
POST /api/v1/procurements
```

**请求体**（对应 Excel 出货单弹窗）：
```json
{
  "skuNumber": "de077",
  "quantity": 500,
  "priceRmb": 45.00,
  "exchangeRate": 21.5,
  "taxPoint": 1.1,
  "billingMethod": "METHOD_A",
  "orderDate": "2026-04-20",
  "factoryShipDate": "2026-05-01",
  "plannedShipDate": "2026-05-10",
  "productLead": "王琳琳",
  "japanLead": "张云",
  "chinaLead": "田中",
  "destination": "名古屋倉庫",
  "customerCompany": "永康株式会社",
  "status": "未定"
}
```

**字段说明**：

| 字段 | Excel 名称 | 必填 | 说明 |
|------|-----------|------|------|
| skuNumber | 货号 | ✅ | 商品代码（关联 Product 表） |
| quantity | 数量 | ✅ | 订购数量 |
| priceRmb | 人民币价格 | ✅ | 人民币单价 |
| exchangeRate | 汇率 | ✅ | CNY→JPY 汇率 |
| taxPoint | 票点 | ✅ | 默认 1.1 |
| billingMethod | 计费方式 | ✅ | 计算方式 |
| orderDate | 下单日 | | 1688 下单日期 |
| factoryShipDate | 厂家出货日 | | 厂家发货日期 |
| plannedShipDate | 计划出货日 | | 计划发货日期 |
| productLead | 商品担当 | | 负责人 |
| japanLead | 日本担当 | | 日本侧负责人 |
| chinaLead | 中国担当 | | 中国侧负责人 |
| destination | 发送目的地 | | 目的地 |
| customerCompany | 客户公司 | | |
| status | 状态 | | default: 未定 |

**计算字段**（前端实时计算，后端存结果）：
```
estimatedPriceJpy = (priceRmb / taxPoint * 1.02 * 1.2) * exchangeRate * 1.05
```

**响应**：`201 Created`
```json
{
  "code": "ok",
  "data": {
    "id": 1,
    "skuNumber": "de077",
    "estimatedPriceJpy": 11321.25,
    "status": "未定",
    "createdAt": "2026-04-20T10:00:00+09:00"
  }
}
```

---

### 1.2 查询出货单列表

```
GET /api/v1/procurements
```

**Query 参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| status | string | 状态过滤（未定/発注待/永康/倉庫着/検品/エア便/輸出/通関/日本着/会計/完了/退货） |
| skuNumber | string | 商品代码 |
| customerCompany | string | 客户公司 |
| page | int | 页码，默认 0 |
| size | int | 页大小，默认 20 |

**响应**：`200 OK`
```json
{
  "code": "ok",
  "data": {
    "content": [
      {
        "id": 1,
        "skuNumber": "de077",
        "quantity": 500,
        "estimatedPriceJpy": 11321.25,
        "status": "倉庫着",
        "productLead": "王琳琳",
        "plannedShipDate": "2026-05-10",
        "createdAt": "2026-04-20T10:00:00+09:00"
      }
    ],
    "totalElements": 42,
    "totalPages": 3,
    "pageNumber": 0
  }
}
```

---

### 1.3 获取出货单详情

```
GET /api/v1/procurements/{id}
```

**响应**：`200 OK`
```json
{
  "code": "ok",
  "data": {
    "id": 1,
    "skuNumber": "de077",
    "quantity": 500,
    "priceRmb": 45.00,
    "exchangeRate": 21.5,
    "taxPoint": 1.1,
    "estimatedPriceJpy": 11321.25,
    "billingMethod": "METHOD_A",
    "orderDate": "2026-04-20",
    "factoryShipDate": "2026-05-01",
    "plannedShipDate": "2026-05-10",
    "productLead": "王琳琳",
    "japanLead": "张云",
    "chinaLead": "田中",
    "destination": "名古屋倉庫",
    "customerCompany": "永康株式会社",
    "status": "未定",
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```

---

### 1.4 更新出货单

```
PATCH /api/v1/procurements/{id}
```

**请求体**（可部分更新）：
```json
{
  "status": "発注待",
  "factoryShipDate": "2026-05-05"
}
```

**状态推进规则**（见 SPEC）：

| 当前状态 | 可转向 |
|----------|--------|
| 未定/予定 | 発注待 / OEM |
| 発注待 | 永康 / 直送 / 倉庫着 |
| 永康 | 倉庫着 |
| 直送 | 倉庫着 |
| 倉庫着 | 検品 / 現地検品 |
| 現地検品 | メーカー直送 |
| 検品 | エア便 / 輸出 |
| エア便 | 通関 / 日本着 |
| 輸出 | 通関 |
| 通関 | 日本着 |
| 日本着 | 会計 |
| 会計 | 完了(済) |
| 完了(済) | —（终态） |

---

### 1.5 删除出货单

```
DELETE /api/v1/procurements/{id}
```

- 仅 `未定`/`予定`/`発注待` 状态可删除
- `完了(済)` 禁止删除

---

## 2. 商品目录（Product）

### 2.1 创建/更新商品目录

```
POST   /api/v1/products      — 新规商品
PUT    /api/v1/products/{id} — 更新商品
```

**请求体**（对应 Excel 商品管理表）：

| 字段 | Excel 名称 | 必填 | 说明 |
|------|-----------|------|------|
| productCode | 商品コード | ✅ | 唯一键 |
| name | 名称 | ✅ | 日文名称 |
| heightCm | 高(cm) | | |
| widthCm | 宽(cm) | | |
| depthCm | 深(cm) | | |
| weightKg | 重量(kg) | | |
| unitsPerPackage | 段ボール入数 | | 每包数量 |
| packageHeightCm | 包装高 | | |
| packageWidthCm | 包装宽 | | |
| packageDepthCm | 包装深 | | |
| packageWeightKg | 包装重量 | | |
| remarks | 备注 | | 箱规不固定/整托不固定 |
| warehouse | 倉庫 | | 名古屋/久留米/永康 |

**计算字段**（自动）：
```
dimensionSum = heightCm + widthCm + depthCm
```

---

### 2.2 查询商品目录

```
GET /api/v1/products?warehouse=名古屋&remarks=箱规不固定
```

---

### 2.3 按商品代码获取尺寸

```
GET /api/v1/products/{code}/dimensions
```

返回：`{ heightCm, widthCm, depthCm, weightKg, unitsPerPackage, packageDimensions, warehouse }`

---

## 3. 验货（検品 / 現地検品）

### 3.1 提交验货结果

```
POST /api/v1/procurements/{id}/qc
```

**请求体**：
```json
{
  "qcType": "ONSITE",
  "qcUserId": 3,
  "result": "PASS",
  "passedCount": 498,
  "defectiveCount": 2,
  "images": ["https://cdn.example.com/qc/defect1.jpg"],
  "remarks": "外箱轻微破损"
}
```

**qcType**：`ONSITE`(検品/仓库验货) | `REMOTE`(現地検品/现场异地)

---

## 4. 货柜管理

### 4.1 录入货柜

```
POST /api/v1/containers
```

**请求体**：
```json
{
  "containerNo": "MSKU1234567",
  "containerType": "40HC",
  "sealNo": "SEAL2026001",
  "portOfLoading": "YANTIAN",
  "portOfDestination": "NAGOYA",
  "procurementIds": [1, 3, 7]
}
```

---

## 5. 财务结算（会計）

### 5.1 财务结算

```
POST /api/v1/procurements/{id}/finance
```

**请求体**：
```json
{
  "taxType": "EXPORT_REFUND",
  "totalCostRmb": 22500.00,
  "actualPaidRmb": 21000.00,
  "currency": "CNY",
  "remarks": "含运费，扣减损坏赔款"
}
```

---

## 6. 退货管理

### 6.1 创建退货记录

```
POST /api/v1/procurements/{id}/returns
```

**请求体**：
```json
{
  "reason": "不良品",
  "quantity": 2,
  "refundAmount": 90.00,
  "1688OrderId": "1688-order-12345"
}
```

---

## 7. 错误码

| code | HTTP | 说明 |
|------|------|------|
| validation.required | 400 | 必填参数缺失 |
| validation.param.invalid | 400 | 参数值非法 |
| auth.unauthorized | 401 | 未认证 |
| auth.forbidden | 403 | 无权操作 |
| not_found | 404 | 资源不存在 |
| business.status_forbidden | 422 | 状态不允许此操作 |
| business.cannot_modify_closed | 422 | 完了(済)后不可修改 |
| business.air_recommended | 200 | 空运推荐（warn级别提示） |
| server.error | 500 | 系统异常 |
