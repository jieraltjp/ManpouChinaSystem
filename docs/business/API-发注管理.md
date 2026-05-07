# 发注管理 — API 契约

> **版本**: 1.5.1
> **更新**: 2026-05-07（v1.5.1：修正 estimatedPriceJpy 注释；补 Container 字段必填表；补 Phase2 状态说明）
> **依据**: `SPEC-B02-发注单-步骤2.md` + `DOMAIN-发注管理领域模型.md`

> **代码实现进度**: 发注单 CRUD ✅ 已实现 · 完整状态流转校验 ✅ 已实现 · 商品目录 ✅ 已实现（masterCode/subCode）· 验货记录 ✅ 已实现 · 货柜 ✅ 已实现 · 国内报关 ✅ 已实现 · 日本清关 ✅ 已实现 · 财务 🔴 待开发（FinanceRecord 聚合根）· 通知 🔴 Example 存根 · 退货管理 🔴 待开发 · 空运推荐 🔴 待开发

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
  "factoryId": 1,
  "productCode": "de077",
  "subProductCode": "re",
  "material": "plastic",
  "requiresQc": true,
  "quantity": 500,
  "priceRmb": 45.00,
  "exchangeRate": 21.5,
  "taxPoint": 1.1,
  "billingType": "ZHE_LU_KAI_PIAO",
  "customsRemarks": "浙鲁开票",
  "instructionManual": "中日英三语",
  "orderDate": "2026-04-20",
  "factoryShipDate": "2026-05-01",
  "plannedShipDate": "2026-05-10",
  "actualShipDate": "2026-05-08",
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
| factoryId | 工厂 | | 关联工厂（FK → factory.id） |
| productCode | 货号 | ✅ | 主货号（关联 Product 表） |
| subProductCode | 子货号 | | 枝番/颜色（re/wh/bk） |
| material | 材质 | | 材质 |
| requiresQc | 必要检测 | | 是否需要检测 |
| quantity | 数量 | ✅ | 订购数量 |
| priceRmb | 人民币价格 | ✅ | 人民币单价 |
| exchangeRate | 汇率 | ✅ | CNY→JPY 汇率（默认 21.5） |
| taxPoint | 票点 | | 默认 1.1 |
| billingType | 报关类型 | | ZHE_LU_KAI_PIAO/CHAO_HUI_TUI_SHUI/NO_REFUND/OTHER |
| customsRemarks | 报关备注 | | 报关备注 |
| instructionManual | 说明书 | | 说明书内容 |
| orderDate | 下单日 | | 1688 下单日期 |
| factoryShipDate | 厂家出货日 | | 厂家发货日期 |
| plannedShipDate | 计划出货日 | | 计划发货日期 |
| actualShipDate | 实际出货日 | | 实际发货日期 |
| productLead | 商品担当 | | 负责人 |
| japanLead | 日本担当 | | 日本侧负责人 |
| chinaLead | 中国担当 | | 中国侧负责人 |
| destination | 发送目的地 | | 目的地 |
| customerCompany | 客户公司 | | |
| status | 状态 | | default: 未定 |

**计算字段**（后端在创建/更新时自动计算并存储，前端仅用于表单实时预览）：
> ⚠️ 文档旧版写"前端实时计算"有歧义——实际是后端计算存储，frontend `previewPriceJpy` 仅作表单预览用。
```
estimatedPriceJpy = (priceRmb / taxPoint * 1.02 * 1.2) * exchangeRate * 1.05
```

**响应**：`201 Created`
```json
{
  "code": "ok",
  "message": "发注单创建成功",
  "data": 1
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
| status | string | 状态过滤（**Phase2**：已下单/已出货；**标准19态**：未定/予定/OEM/発注待/永康/直送/倉庫着/現地検品/検品/エア便/メーカー直送/輸出/国内通関/通関/日本着/日本通関完了/会計/完了/退货） |
| productCode | string | 商品代码 |
| customerCompany | string | 客户公司 |
| page | int | 页码，默认 0（0-indexed） |
| pageSize | int | 页大小，默认 20 |

**响应**：`200 OK`
```json
{
  "code": "ok",
  "data": {
    "content": [
      {
        "id": 1,
        "factoryId": 1,
        "productCode": "de077",
        "subProductCode": "re",
        "material": "plastic",
        "requiresQc": true,
        "quantity": 500,
        "priceRmb": 45.00,
        "exchangeRate": 21.5,
        "taxPoint": 1.1,
        "billingType": "ZHE_LU_KAI_PIAO",
        "estimatedPriceJpy": 11321.25,
        "customsRemarks": "浙鲁开票",
        "instructionManual": "中日英三语",
        "orderDate": "2026-04-20",
        "factoryShipDate": "2026-05-01",
        "plannedShipDate": "2026-05-10",
        "actualShipDate": "2026-05-08",
        "productLead": "王琳琳",
        "japanLead": "张云",
        "chinaLead": "田中",
        "destination": "名古屋倉庫",
        "customerCompany": "永康株式会社",
        "status": "倉庫着",
        "createBy": "admin",
        "createTime": "2026-04-20T10:00:00+09:00",
        "updateTime": "2026-04-20T10:00:00+09:00"
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
    "factoryId": 1,
    "productCode": "de077",
    "subProductCode": "re",
    "material": "plastic",
    "requiresQc": true,
    "quantity": 500,
    "priceRmb": 45.00,
    "exchangeRate": 21.5,
    "taxPoint": 1.1,
    "billingType": "ZHE_LU_KAI_PIAO",
    "estimatedPriceJpy": 11321.25,
    "customsRemarks": "浙鲁开票",
    "instructionManual": "中日英三语",
    "orderDate": "2026-04-20",
    "factoryShipDate": "2026-05-01",
    "plannedShipDate": "2026-05-10",
    "actualShipDate": "2026-05-08",
    "productLead": "王琳琳",
    "japanLead": "张云",
    "chinaLead": "田中",
    "destination": "名古屋倉庫",
    "customerCompany": "永康株式会社",
    "status": "未定",
    "createBy": "admin",
    "createTime": "2026-04-20T10:00:00+09:00",
    "updateTime": "2026-04-20T10:00:00+09:00"
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

**状态推进规则**（与 `ShipmentStatus.java` FSM 完全一致，详见 `DOMAIN-发注管理领域模型.md`）：

| 当前状态 | 可转向 |
|----------|--------|
| 未定 | 未定 / 予定 / 発注待 / OEM |
| 予定 | 未定 / 予定 / 発注待 / OEM |
| OEM | 未定 / 予定 / 発注待 / OEM |
| 発注待 | 未定 / 予定 / 永康 / 直送 / OEM |
| 永康 | 未定 / 倉庫着 |
| 直送 | 未定 / 倉庫着 |
| 倉庫着 | 未定 / 検品 / 現地検品 |
| 検品 | 未定 / エア便 / 輸出 / 倉庫着 |
| 現地検品 | 未定 / メーカー直送 / 倉庫着 |
| エア便 | 未定 / 国内通関 |
| 輸出 | 未定 / 国内通関 |
| メーカー直送 | 未定 / 日本着 |
| 国内通関 | 未定 / 通関 |
| 通関 | 未定 / 日本着 |
| 日本着 | 未定 / 日本通関完了 |
| 日本通関完了 | 未定 / 会計 |
| 会計 | 未定 / 完了 |
| 完了 | —（终态，禁止任何变更） |
| 退货 | 未定 / 完了 |


---

### 1.5 删除出货单

```
DELETE /api/v1/procurements/{id}
```

- 仅 `未定`/`発注待` 状态可删除
- `完了` 禁止删除

---

## 2. 商品目录（Product）

### 2.1 创建/更新商品目录

```
POST   /api/v1/products          — 新规商品
PATCH  /api/v1/products/{id}     — 部分更新商品
GET    /api/v1/products/code/{masterCode}   — 按 masterCode 查询
GET    /api/v1/products/suggest/master-codes — 货号自动补全
GET    /api/v1/products/suggest/sub-codes    — 子货号自动补全
```

**请求体**（ProductCreateCmd / ProductUpdateCmd）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| masterCode | String | ✅ | 主货号（唯一键，UNIQUE master_code+sub_code） |
| subCode | String | | 子货号/颜色（如 a/b/m/L） |
| nameZh | String | | 中文名称 |
| nameEn | String | | 英文名称 |
| nameJa | String | | 日文名称（来自 DB.xlsx） |
| unitPriceRmb | Decimal | | 含税单价（元） |
| taxRate | Decimal | | 税率，默认 0.1（10%） |
| grossWeightKg | Decimal | | 毛重 kg（DB.xlsx 原始数据） |
| netWeightKg | Decimal | | 净重 kg |
| hsCode | String | | HS 编码（goods.sql 有，DB.xlsx 无） |
| declarationElements | String | | 申报要素 |
| unitsPerPackage | Integer | | 每箱数量 |
| origin | String | | 原产国，默认"中国" |
| unit | String | | 单位，默认 PCS |
| remarks | String | | 备注 |
| lastUsedDate | Date | | 最近使用日期 |
| category | Enum | | OEM / ORDINARY / FACTORY_DIRECT |

> ⚠️ **数据库字段映射**：`goods.sql` 来源有完整字段；`DB.xlsx` 来源仅含 `name_ja`、`unit_price`、`weight`，`hs_code`/`origin`/`factory` 均无。

---

### 2.2 查询商品目录

```
GET /api/v1/products?warehouse=名古屋&remarks=箱规不固定
```

---

### 2.3 按商品代码获取尺寸

> ⚠️ 此端点不存在。尺寸字段已内嵌在 `GET /api/v1/products/code/{masterCode}` 的完整响应中。

```
GET /api/v1/products/code/{masterCode}
```

返回字段包含：`lengthCm`, `widthCm`, `heightCm`, `volumeCbm`, `netWeightKg`, `grossWeightKg`, `unitsPerPackage`, `packageLengthCm`, `packageWidthCm`, `packageHeightCm`, `packageVolumeCbm`, `warehouse`。

---

## 3. 验货（検品 / 現地検品）

> ⚠️ **已迁移**：验货记录为独立模块，不在发注单下嵌套。
> 验货 API 详见 `SPEC-B03-验货记录-步骤3.md`。
>
> | 操作 | Endpoint |
> |------|----------|
> | 创建验货记录 | `POST /api/v1/qc-records` |
> | 更新验货记录 | `PATCH /api/v1/qc-records/{id}` |
> | 查询验货记录 | `GET /api/v1/qc-records?procurementId={id}` |

---

## 4. 货柜管理 ✅

> Container 聚合根已实现（v1.5.0），API 路径见 Controller。

```
POST /api/v1/containers
```

**请求体字段**：

| 字段 | 必填 | 说明 |
|------|------|------|
| containerNo | ✅ | 集装箱号 |
| containerType | ✅ | 箱型（GP20/GP40/HC40/HC45） |
| sealNo | | 封条号 |
| portOfLoading | | 装货港代码 |
| portOfDestination | | 目的港代码 |
| procurementIds | | 关联发注单 ID 列表 |

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

**⚠️ 缺口**：前端 `ContainerPage.vue` 当前仅实现基础字段，`sealNo`/`portOfLoading`/`portOfDestination`/`procurementIds` 尚未接入表单（详情见 `docs/ui/pages/19-container.md` §缺口分析）。

---

## 5. 财务结算（会計）🔴

> 🔴 **待开发**：FinanceRecord 聚合根未实现，API 路径待定。

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

## 6. 退货管理 🔴

> 🔴 **待开发**：退货记录聚合根未实现，API 路径待定。

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
| resource.not-found | 404 | 资源不存在 |
| business.invalid_status_transition | 422 | 状态「A」不允许跳转至「B」 |
| business.cannot_modify_closed | 422 | 完了（终态）后禁止任何变更 |
| business.air_recommended | 200 | 空运推荐（warn级别提示） |
| system.internal-error | 500 | 系统异常 |

---

## 8. Phase 2 扩展域（Example 存根）

以下域已实现 API 骨架，但领域模型为 `Example` 存根（仅 `name`/`status`），实际字段待 Phase 2-8 实现：

| 域 | 路径 | 状态 | 说明 |
|---|------|------|------|
| 仓库 | `/api/v1/warehouse` | 🟡 存根 | WarehouseExample（name/status） |
| 通知 | `/api/v1/notifications` | 🟡 存根 | NotificationExample（name/status） |
| 财务 | `/api/v1/finance` | 🟡 存根 | FinanceExample（name/status） |
| 报关 | `/api/v1/customs` | 🟡 存根 | CustomsExample（name/status） |

前端适配器：`web/src/api/{warehouse,notification,finance,customs}.ts`

完整领域模型实现计划：
- Phase 5/6（报关）：DomesticCustoms / JapanCustoms（含 submit/clear/reject 等状态机）
- Phase 7（财务）：FinanceRecord（含 taxType, totalCostRmb, actualPaidRmb, currency）
- Phase 7（退税）：TaxRefund（含 refundAmount, noRefund 标记）
- Phase 8（销售）：SalesRecord（含 stock decrement/increment）

---

## 9. 待开发端点

| 端点 | 说明 | 阶段 |
|------|------|------|
| `POST /api/v1/procurements/{id}/finance` | 财务结算关联 | Phase 7 |
| `POST /api/v1/procurements/{id}/returns` | 退货管理 | Phase 8 |
| `空运推荐`（自动） | 尺寸/重量达标自动推荐空运 | 待定 |
