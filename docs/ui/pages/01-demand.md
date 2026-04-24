# 页面规格 — 步骤1：补货需求

> **版本**: 1.6.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v1.6.0：子货号明细表单 — 每行子货号+数量+目的地各自独立）
> **路由**: `/procurement/demand`
> **组件**: `DemandPage.vue`
> **对应后端**: `ReplenishmentDemand` 聚合根
> **依赖文档**: `SPEC-B00-全链路总览.md` · `SPEC-B01-补货需求-步骤1.md`

---

## 1. 页面定位

补货需求的入口页面。非新品走补货流程，新品走新品采购流程。录入后转为发注单（Procurement），进入步骤2。

---

## 2. 布局结构

```
┌────────────────────────────────────────────────────────────────────┐
│ 页面标题：补货需求管理                               [+ 新规录入]    │
├────────────────────────────────────────────────────────────────────┤
│ 统计卡                                                               │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐     │
│ │ PENDING    │ │ CONVERTED  │ │ CANCELLED  │ │   合计     │     │
│ │ 待确认     │ │ 已转采购   │ │ 已取消     │ │            │     │
│ │    N       │ │    N       │ │    N       │ │    N       │     │
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘     │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏                                                               │
│ 类型 [全部▼]  货号 [____]  担当者 [____]  日期范围 [____至____]  │
│                                          [搜索]  [重置]            │
├────────────────────────────────────────────────────────────────────┤
│ 表格                                                               │
│ ┌──────┬────────┬──────────┬───────┬──────┬────────┬──────┐      │
│ │编号   │类型    │主货号     │子货号 │数量  │目的地  │状态  │ ... │
│ │ D-xx │补货    │odn012     │odn012 │ 100  │久留米  │PEND  │      │
│ │      │        │           │ -re   │      │       │ING   │      │
│ └──────┴────────┴──────────┴───────┴──────┴────────┴──────┘      │
│                                                            [分页]  │
└────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表格列定义（v1.6.0）

| 列名 | 字段 | 说明 |
|------|------|------|
| 编号 | `demandCode` | 格式 `D-YYYYMMDD-NNN` |
| 类型 | `demandType` | `REPLENISHMENT`(补货) / `NEW_PURCHASE`(新品采购) |
| 主货号 | `productCode` | 主货号 |
| 子货号明细 | `subProductItems` | 汇总显示：例 `be:100久留米, bu:50名古屋`（转采购后显示） |
| 日本担当 | `japanLead` | 日本担当 |
| 状态 | `status` | PENDING / CONVERTED / CANCELLED |
| 创建时间 | `createTime` | — |
| 操作 | — | 转采购 / 查看采购单 / 编辑 / 删除 |

---

## 4. 新规录入弹窗（v1.6.0）

### 4.1 触发

点击 `[+ 新规录入]` 按钮 → 弹出表单弹窗。

### 4.2 表单字段

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 需求类型 | `el-radio-group` | ✅ | `REPLENISHMENT`(补货) / `NEW_PURCHASE`(新品采购) |
| 主货号 | `el-select`（商品选择器） | ✅ | 来自 Product.masterCode，支持搜索 |
| 日本担当 | `el-select`（担当者列表） | | 来自系统用户表 |
| 子货号明细 | `el-table`（动态行） | ✅ | 核心变更：每行含子货号+数量+目的地 |
| + 添加行 | `el-button` | | 追加子货号明细行 |
| 备注 | `el-input` | | 整单备注 |

**子货号明细表结构（v1.6.0 新增）：**

| 列 | 控件 | 必填 | 说明 |
|----|------|------|------|
| 子货号 | `el-select`（可选子货号列表） | ✅ | 根据主货号过滤颜色变体 |
| 数量 | `el-input-number` | ✅ | 该子货号的需求数量 |
| 目的地 | `el-input` | ✅ | 发送给哪个日本客户（久留米/名古屋/大阪等） |
| 操作 | `el-button` | | 删除该行 |

### 4.3 业务规则

- 选择主货号后，若该货号有子货号（颜色变体），第一行自动代入全部子货号
- 用户可调整每行的数量和目的地
- 新品采购：至少填一行子货号（子货号可手动输入，不必须来自下拉）
- 非新品补货：子货号优先从下拉选择（颜色标准化）

### 4.4 提交

- POST `/api/v1/demands`
- 请求体示例：
```json
{
  "demandType": "REPLENISHMENT",
  "productCode": "ad009",
  "subProductItems": [
    { "subCode": "be", "quantity": 100, "destination": "久留米" },
    { "subCode": "bu", "quantity": 50,  "destination": "名古屋" }
  ],
  "japanLead": "田中",
  "remarks": ""
}
```
- 成功后刷新列表；弹窗关闭

---

## 5. 转采购（v1.6.0 — 批量模式）

### 5.1 触发条件

状态 = `PENDING`，操作列点击 `[转采购]`。

### 5.2 行为

1. 跳转至 `/procurement/order`（带 query 参数 `demandId`）
2. 发注单页面预填充：主货号 + 工厂选择器（空白待填）
3. **每个子货号明细行生成一条 Procurement 草稿**（数量/目的地已代入）
4. 用户选择工厂后提交（工厂 ID 传给 `/demands/{id}/convert`）

### 5.3 API 调用

```
POST /api/v1/demands/{id}/convert
请求体：{ "factoryId": 123 }
响应：{ "code": "ok", "data": { "demandStatus": "CONVERTED", "linkedProcurementIds": [101, 102] } }
```

### 5.4 查看采购单（CONVERTED）

操作列 `[查看采购单]` → 跳转 `/procurement/order?demandId={id}` → 列出该需求单生成的所有 Procurement

### 5.5 撤销转换

操作列 `[撤销转换]` → 批量删除关联的 Procurement，回滚需求状态 → PENDING

---

## 6. 状态流转

```
  PENDING ──[转采购]──▶ CONVERTED
     │
     └──[取消]──▶ CANCELLED
```

| 状态 | 颜色 | 可用操作 |
|------|------|---------|
| PENDING | `#FEF3C7` 黄色 | 转采购 / 撤销 / 编辑 / 删除 |
| CONVERTED | `#DBEAFE` 蓝色 | 查看采购单 / 撤销转换 / 详情 |
| CANCELLED | `#F3F4F6` 灰色 | 详情 |

---

## 7. API 集成

| 操作 | Method | Endpoint |
|------|--------|----------|
| 分页查询 | GET | `/api/v1/demands?page=&pageSize=&demandType=&productCode=&status=` |
| 详情 | GET | `/api/v1/demands/{id}` |
| 创建 | POST | `/api/v1/demands` |
| 更新 | PATCH | `/api/v1/demands/{id}` |
| 转采购 | POST | `/api/v1/demands/{id}/convert` → 返回 `linkedProcurementIds[]` |
| 撤销转换 | POST | `/api/v1/demands/{id}/revert` |
| 删除 | DELETE | `/api/v1/demands/{id}` |

---

## 8. 缺口与 TODO

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 子货号明细表单 | **P0** | v1.6.0 核心：el-table 动态行，子货号+数量+目的地 |
| 转采购批量 API | **P0** | 后端 convert 改为批量返回 procurementIds |
| 查看采购单列表 | P1 | CONVERTED 状态显示该需求生成的 N 条 Procurement |
| 撤销转换批量 | P1 | 遍历 linkedDemandItems 批量删除 Procurement |
| 担当者选择器 | P2 | 需系统用户管理表；目前 hardcode 列表 |

---

## 9. 组件拆分建议

| 组件 | 职责 |
|------|------|
| `DemandPage.vue` | 容器：列表 + 筛选 + 统计卡 |
| `DemandFilters.vue` | 筛选栏组件 |
| `DemandTable.vue` | 表格 + 分页 |
| `DemandFormDialog.vue` | 新规/编辑弹窗表单 |
| `DemandConvertDialog.vue` | 转采购确认弹窗 |
| `useDemand.ts` | API 调用逻辑（composable） |
