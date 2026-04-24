# 页面规格 — 步骤1：补货需求

> **版本**: 2.0.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v2.0.0：每条 Demand = 一个子货号；表单简化为直接字段）
> **路由**: `/procurement/demand`
> **组件**: `DemandPage.vue`
> **对应后端**: `ReplenishmentDemand` 聚合根
> **对应文档**: `SPEC-B01-补货需求-步骤1.md` v2.0.0

---

## 1. 页面定位

补货需求的入口页面。**每条需求单 = 一个子货号**（商品唯一标识 = 主货号 + 子货号）。
非新品走补货流程，新品走新品采购流程。录入后转为发注单（Procurement），进入步骤2。

---

## 2. 核心设计原则

- **商品唯一标识 = 主货号 + 子货号**，如 `ad009-be`
- **每条 ReplenishmentDemand = 一个子货号**（不在 JSON 数组里存多个子货号）
- 转采购：**1 Demand → 1 Procurement**（一对一）

---

## 3. 布局结构

```
┌────────────────────────────────────────────────────────────────────┐
│ 页面标题：补货需求管理                               [+ 新规录入]    │
├────────────────────────────────────────────────────────────────────┤
│ 统计卡                                                               │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐                     │
│ │ PENDING    │ │ CONVERTED  │ │ CANCELLED  │                     │
│ │ 待确认     │ │ 已转采购   │ │ 已取消     │                     │
│ │    N       │ │    N       │ │    N       │                     │
│ └────────────┘ └────────────┘ └────────────┘                     │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏                                                               │
│ 类型 [全部▼]  货号 [____]  状态 [全部▼]                          │
│                                          [搜索]  [重置]            │
├────────────────────────────────────────────────────────────────────┤
│ 表格（v2.0.0：每行 = 一个子货号）                                │
│ ┌──────┬────────┬──────────┬───────────┬──────┬────────┬───────┐  │
│ │编号   │类型    │子货号     │数量       │目的地 │日本担当│状态   │  │
│ │ D-xx │补货    │odn012-be  │ 100台     │久留米  │田中   │PEND  │  │
│ │ D-xx │补货    │odn012-bu  │  50台     │名古屋  │田中   │CONV  │  │
│ │ D-xx │新品    │odn012-re  │  75台     │大阪    │山本   │PEND  │  │
│ └──────┴────────┴───────────┴──────────┴──────┴────────┴───────┘  │
│                                                            [分页]  │
└────────────────────────────────────────────────────────────────────┘
```

---

## 4. 表格列定义（v2.0.0）

| 列名 | 字段 | 说明 |
|------|------|------|
| 编号 | `demandCode` | 格式 `D-YYYYMMDD-NNN` |
| 类型 | `demandType` | `REPLENISHMENT`(补货) / `NEW_PURCHASE`(新品采购) |
| 子货号 | `subProductCode` | 子货号全码，如 `odn012-be` |
| 数量 | `quantity` | 该子货号的需求数量 |
| 目的地 | `destination` | 出货目的地，如久留米/名古屋 |
| 日本担当 | `japanLead` | 日本担当 |
| 状态 | `status` | PENDING / CONVERTED / CANCELLED |
| 创建时间 | `createTime` | — |
| 操作 | — | 转采购 / 查看采购单 / 编辑 / 删除 |

---

## 5. 新规录入弹窗（v2.0.0）

### 5.1 触发

点击 `[+ 新规录入]` 按钮 → 弹出表单弹窗。

### 5.2 表单字段

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 需求类型 | `el-radio-group` | ✅ | `REPLENISHMENT`(补货) / `NEW_PURCHASE`(新品采购) |
| 主货号 | `el-select`（商品选择器） | ✅ | 来自 Product.masterCode，支持搜索 |
| 子货号 | `el-select`（可选子货号列表） | ✅ | 根据主货号过滤颜色变体，自动代入第一个 |
| 数量 | `el-input-number` | ✅ | 该子货号的需求数量 |
| 目的地 | `el-input` | | 发送给哪个日本客户（久留米/名古屋/大阪等） |
| 日本担当 | `el-input` | | 自由输入 |
| 备注 | `el-input` | | 整单备注 |

### 5.3 业务规则

- 选择主货号后，自动加载该主货号下所有子货号（颜色变体），默认代入第一个
- 选完主货号后，用户手动选择子货号（一个）
- 每条 Demand 独立一条子货号记录

### 5.4 提交

- POST `/api/v1/demands`
- 请求体示例：
```json
{
  "demandType": "REPLENISHMENT",
  "productCode": "ad009",
  "subProductCode": "ad009-be",
  "quantity": 100,
  "destination": "久留米",
  "japanLead": "田中",
  "remarks": ""
}
```
- 成功后刷新列表；弹窗关闭

---

## 6. 转采购（v2.0.0 — 1:1 模式）

### 6.1 触发条件

状态 = `PENDING`，操作列点击 `[转采购]`。

### 6.2 行为

1. 弹出工厂选择弹窗
2. 选择工厂后确认
3. **1 Demand → 1 Procurement**（该子货号独立生成一条 Procurement）
4. 需求单状态 → CONVERTED
5. `linkedProcurementId` 记录对应的 Procurement.id

### 6.3 API 调用

```
POST /api/v1/demands/{id}/convert
请求体：{ "factoryId": 123 }
响应：{ "code": "ok", "data": { "demandStatus": "CONVERTED", "linkedProcurementId": 101 } }
```

### 6.4 查看采购单（CONVERTED）

操作列 `[查看采购单]` → 弹窗显示对应的 Procurement 明细

### 6.5 撤销转换

操作列 `[撤销转换]` → 删除关联的 Procurement，回滚需求状态 → PENDING

---

## 7. 状态流转

```
  PENDING ──[转采购]──▶ CONVERTED
     │
     └──[取消]──▶ CANCELLED
```

---

## 8. API 集成

| 操作 | Method | Endpoint |
|------|--------|----------|
| 分页查询 | GET | `/api/v1/demands?page=&pageSize=&demandType=&productCode=&status=` |
| 详情 | GET | `/api/v1/demands/{id}` |
| 创建 | POST | `/api/v1/demands` |
| 更新 | PATCH | `/api/v1/demands/{id}` |
| 转采购（1:1） | POST | `/api/v1/demands/{id}/convert` → 返回 `linkedProcurementId` |
| 撤销转换 | POST | `/api/v1/demands/{id}/revert` |
| 查看采购单 | GET | `/api/v1/demands/{id}/procurement` |
| 删除 | DELETE | `/api/v1/demands/{id}` |

---

## 9. 代码实现状态

| 项目 | 状态 |
|------|------|
| `DemandPage.vue` 表单（v2.0.0：直接字段） | ✅ |
| `DemandPage.vue` 表格（v2.0.0：子货号/数量/目的地列） | ✅ |
| `DemandPage.vue` 转采购弹窗（1:1 预览） | ✅ |
| `DemandPage.vue` 查看采购单弹窗（单条） | ✅ |
| `demand.ts` API 客户端 | ✅ |
| `OrderOverviewPage.vue` 需求单 Tab（v2.0.0 列） | ✅ |
| `orderOverview.ts` DemandVO / DemandSelectorVO | ✅ |
| `OrderOverviewAssembler` / `OrderOverviewUseCase` | ✅ |
| 数据库迁移脚本（Vxx） | 🔲 待创建 |
