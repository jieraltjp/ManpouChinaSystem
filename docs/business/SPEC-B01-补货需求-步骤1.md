# SPEC-B01 — 补货需求业务规格（步骤1）

> **版本**: 1.6.0
> **更新**: 2026-04-24（v1.6.0：子货号明细表单 — 每个子货号独立数量+目的地；转采购改为批量模式）
> **更新**: 2026-04-22（v1.1.0：新增商品自动补全 API 与 DemandPage 前端改造）
> **创建**: 2026-04-22
> **状态**: 🔲 开发中
> **业务步号**: 01（补货需求/新品采购）
> **对应 UI 文档**: `docs/ui/pages/01-demand.md`
> **对应数据库文档**: `docs/database/DB-01-procurement-demand.md`

---

## 1. 业务背景

补货需求是整个采购链路的入口。非新品走补货流程，新品走新品采购流程。录入后转为发注单（Procurement），进入步骤2。

**两种意图必须分离**：
- `REPLENISHMENT`（补货）：基于历史销售数据补货，通常有子货号
- `NEW_PURCHASE`（新品采购）：全新商品立项，无子货号

---

## 2. 聚合根

### SubProductItem（子货号明细）

```
SubProductItem（值对象，嵌入 JSON）
├── subCode: String          # 子货号（如 be / bu / re）
├── quantity: Integer        # 该子货号的需求数量（各颜色可不同）
└── destination: String     # 目的地（各颜色可不同，如久留米/名古屋）
```

> **设计背景（v1.6.0）**：同一主货号下多个子货号（颜色），数量和目的地可能各不相同，不能共用一个 quantity/destination。例：
> - ad009-be（黑色）→ 数量100，目的地=久留米
> - ad009-bu（蓝色）→ 数量50，目的地=名古屋
> - ad009-re（红色）→ 数量75，目的地=大阪

### ReplenishmentDemand（补货需求单）

```
ReplenishmentDemand（聚合根）
├── id: Long
├── demandCode: String           # D-YYYYMMDD-NNN
├── demandType: DemandType        # REPLENISHMENT(补货) / NEW_PURCHASE(新品采购)
├── productCode: String           # 主货号
├── subProductItems: List<SubProductItem> # 子货号明细（JSON 数组；v1.6.0 升级）
├── japanLead: String           # 日本担当（整单共用）
├── status: DemandStatus        # PENDING → CONVERTED → CANCELLED
├── linkedDemandItems: List<LinkedDemandItem> # 关联的 Procurement 明细（CONVERTED时填充）
└── 领域方法
    ├── convertToProcurement()   # 转为发注单，批量生成 Procurement（每条子货号一个）
    └── cancel()                 # 取消需求
```

**与旧版兼容**：`sub_product_code` 字段（varchar）存储格式：
- 旧数据（v1.5.x）：`["be","bu","re"]` → 转为 `[{subCode:"be"},{subCode:"bu"},...]`，数量取原 quantity，destination 取原 destination
- 新数据（v1.6.0）：`[{"subCode":"be","quantity":100,"destination":"久留米"},...]`

---

## 3. 枚举

```java
public enum DemandType {
    REPLENISHMENT,   // 非新品 — 补货
    NEW_PURCHASE      // 新品 — 采购
}

public enum DemandStatus {
    PENDING,     // 待确认（录入后默认）
    CONVERTED,  // 已转采购（生成 Procurement 后推进至此）
    CANCELLED   // 已取消
}
```

### LinkedDemandItem（关联明细）

```java
// 记录 CONVERTED 后，每个子货号对应的 Procurement ID
public class LinkedDemandItem {
    Long linkedProcurementId; // Procurement.id
    String subCode;            // 对应哪个子货号
}
```

---

## 4. 状态流转

```
  PENDING ──[转采购]──▶ CONVERTED
     │
     └──[取消]──▶ CANCELLED
```

### 转采购：批量生成逻辑（v1.6.0）

`convertToProcurement(factoryId, procurementIds)` 的语义升级：

- **每个 SubProductItem 生成一条 Procurement 记录**（数量、目的地各自独立）
- **一条 ReplenishmentDemand 对应多条 Procurement**（通过 `linkedDemandItems` 追踪）
- 撤销转换：遍历 `linkedDemandItems`，删除所有关联 Procurement，回滚 Demand 状态

---

## 5. API 设计

### 5.1 补货需求 API

```
GET    /api/v1/demands?page=&pageSize=&demandType=&productCode=&status=
GET    /api/v1/demands/{id}
POST   /api/v1/demands
PATCH  /api/v1/demands/{id}
POST   /api/v1/demands/{id}/convert    # 转采购（批量生成 N 条 Procurement）
POST   /api/v1/demands/{id}/revert     # 撤销转换（批量删除 N 条 Procurement）
DELETE /api/v1/demands/{id}
```

> **POST /demands/{id}/convert 请求体变更**（v1.6.0）：
> ```json
> {
>   "factoryId": 123,
>   // 不再传 procurementId，改为返回批量生成的 ID 列表
> }
> ```
> 响应：
> ```json
> {
>   "code": "ok",
>   "data": {
>     "demandStatus": "CONVERTED",
>     "linkedProcurementIds": [101, 102, 103]
>   }
> }
> ```

### 5.2 商品自动补全 API（补货页主/子货号选择）

```
GET    /api/v1/products/suggest/master-codes?keyword={keyword}   # 主货号下拉补全
GET    /api/v1/products/suggest/sub-codes?masterCode={code}    # 子货号候选项
```

> 对应设计文档：`docs/design/FEATURE-货号自动补全与多子货号选择.md`

---

## 6. 代码实现状态

- [x] ✅ `ReplenishmentDemand` 聚合根实体（v1.6.0：subProductItems + linkedDemandItems）
- [x] ✅ `SubProductItem` 值对象（v1.6.0 新增）
- [x] ✅ `LinkedDemandItem` 关联明细（v1.6.0 新增）
- [x] ✅ `DemandType` 枚举
- [x] ✅ `DemandStatus` 枚举
- [x] ✅ `ReplenishmentDemandRepository` 领域接口
- [x] ✅ `ReplenishmentDemandUseCase` 用例服务
- [x] ✅ `ReplenishmentDemandController` REST 控制器
- [x] ✅ `@/api/demand.ts` 前端 API 客户端
- [x] ✅ `DemandPage.vue` 页面骨架
- [x] ✅ 主货号自动补全（el-select remote + 300ms 防抖）
- [x] ✅ 子货号多选（el-select multiple + filterable + allow-create）
- [x] ✅ `ProductController` suggest 端点（/suggest/master-codes + /suggest/sub-codes）
- [x] ✅ `ReplenishmentDemandAssembler` JSON 序列化/反序列化（v1.6.0，含旧数据兼容升格式）
- [x] ✅ `ReplenishmentDemandUseCase.convertToProcurement()` 批量生成 N 条 Procurement（v1.6.0）
- [x] ✅ `ReplenishmentDemandUseCase.revertConversion()` 批量撤销（v1.6.0）
- [ ] 🔲 `DemandPage.vue` 子货号明细表单（v1.6.0：el-table 动态行，每行子货号+数量+目的地各自独立）
- [ ] 🔲 `DemandPage.vue` 转采购改为弹窗选择工厂（v1.6.0：POST /demands/{id}/convert + factoryId）
- [ ] 🔲 `DemandPage.vue` 表格列显示 subProductItems 含数量/目的地（v1.6.0）
- [ ] 🔲 `demand.ts` API 类型对齐 v1.6.0（CreateDemandRequest.subProductItems[]）
