# SPEC-B01-转采购功能实现

> **版本**: 1.6.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v1.6.0：批量转采购 — 子货号各自数量+目的地，批量生成 Procurement）
> **状态**: 🔲 开发中
> **对应 SPEC**: SPEC-B01-补货需求-步骤1.md §4

---

## 1. 业务规则（v1.6.0）

### 1.1 转采购流程

需求单（ReplenishmentDemand）状态为 `PENDING` 时，点击「转采购」：

```
需求单列表 → 点「转采购」
→ 跳转发注单页面（预填：主货号 + 工厂选择）
→ 用户为每个子货号指定：数量 + 目的地（已有默认值）
→ 保存发注单（每个子货号生成一条 Procurement）
→ 需求状态变为 CONVERTED
```

**前置条件：**
- 需求状态 = `PENDING`
- 至少有一个子货号明细（subProductItems 非空）
- 工厂 ID 必填

**转换后：**
- 需求单 `status` → `CONVERTED`
- 需求单 `linkedDemandItems` → `[{linkedProcurementId: X, subCode: "be"}, ...]`
- 每个子货号生成一条 Procurement（factoryId/subCode/quantity/destination 各自独立）

### 1.2 撤销转换（v1.6.0）

| 场景 | 处理方式 |
|------|---------|
| 用户转错 | 撤销转换：批量删除 N 条 Procurement，回滚 Demand → PENDING |
| 用户选错工厂 | 同上，撤销重转 |
| 用户想取消转采购 | 关闭弹窗即可，需求状态不变 |

### 1.3 业务边界

- CONVERTED 状态不可再转（领域方法校验）
- 转采购后，每个子货号独立生成 Procurement，可单独编辑/推进状态
- 撤销转换时，若任意 Procurement 已推进至终态，禁止撤销

---

## 2. 数据结构

### 转采购请求（v1.6.0）

```
POST /api/v1/demands/{id}/convert
请求体：
{
  "factoryId": 123
}

响应：
{
  "code": "ok",
  "data": {
    "demandStatus": "CONVERTED",
    "linkedProcurementIds": [101, 102, 103]  ← 批量生成的 Procurement ID 列表
  }
}
```

### 子货号明细（前端表单 → 存储 → 转采购）

```
前端：subProductItems = [
  { subCode: "be", quantity: 100, destination: "久留米" },
  { subCode: "bu", quantity: 50,  destination: "名古屋" },
  { subCode: "re", quantity: 75,  destination: "大阪" }
]

DB sub_product_code（JSON）：
[{"subCode":"be","quantity":100,"destination":"久留米"},...]
```

### 关联追溯

```
ReplenishmentDemand
  linkedDemandItems = [
    { linkedProcurementId: 101, subCode: "be" },
    { linkedProcurementId: 102, subCode: "bu" },
    { linkedProcurementId: 103, subCode: "re" }
  ]

Procurement × N
  linkedDemandItemId → 回链到需求单具体哪个子货号
```

---

## 3. 撤销转换实现

### 前端

```
POST /api/v1/demands/{id}/revert
→ 成功后 Demand.status → PENDING，linkedDemandItems 清空
→ 刷新列表
```

### 后端

`ReplenishmentDemandUseCase.revertConversion()`：
1. 检查关联的每条 Procurement 状态是否在终态
2. 删除所有关联 Procurement（逻辑删除）
3. Demand 状态回滚 PENDING，清空 linkedDemandItems
4. 事务保证原子性

---

## 4. 代码改动清单

### 后端（v1.6.0）
- [ ] 🔲 `ReplenishmentDemand` 聚合根：添加 `linkedDemandItems` 字段 + JSON 序列化
- [ ] 🔲 `ReplenishmentDemand.convertToProcurement(factoryId)`：改为批量，返回 procurementIds
- [ ] 🔲 `ReplenishmentDemand.revertConversion()`：批量撤销
- [ ] 🔲 `ReplenishmentDemandAssembler`：subProductCodes → SubProductItems 互转（含兼容旧格式）
- [ ] 🔲 `ReplenishmentDemandUseCase.convertToProcurement()`：批量创建 Procurement
- [ ] 🔲 `ReplenishmentDemandController.convert()`：请求体改为 `{factoryId}`，返回 `{linkedProcurementIds}`

### 前端（v1.6.0）
- [ ] 🔲 `DemandPage.vue` 表单：子货号明细表（el-table，动态行，子货号+数量+目的地）
- [ ] 🔲 `DemandPage.vue` 表格列：子货号列改为显示汇总（例：`be:100久留米, bu:50名古屋`）
- [ ] 🔲 `OrderPage.vue`：`onConvert()` 改为传 factoryId，接收返回的 linkedProcurementIds
- [ ] 🔲 `api/demand.ts`：`convertToProcurement` 请求体/响应体类型更新
