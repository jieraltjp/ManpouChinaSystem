# 页面规格 — 步骤1：补货需求

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **路由**: `/procurement/demand`
> **组件**: `DemandPage.vue`
> **对应后端**: `ReplenishmentDemand` 聚合根
> **依赖文档**: `SPEC-全链路业务流索引.md` §第一步

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

## 3. 表格列定义

| 列名 | 字段 | 说明 |
|------|------|------|
| 编号 | `demandCode` | 格式 `D-YYYYMMDD-NNN` |
| 类型 | `demandType` | `REPLENISHMENT`(补货) / `NEW_PURCHASE`(新品采购) |
| 主货号 | `productCode` | 主货号 |
| 子货号 | `subProductCode` | 子货号/颜色（可为空） |
| 数量 | `quantity` | 需求量 |
| 目的地 | `destination` | 发送目的地 |
| 日本担当 | `japanLead` | 日本担当 |
| 状态 | `status` | PENDING / CONVERTED / CANCELLED |
| 创建时间 | `createTime` | — |
| 操作 | — | 转采购 / 取消 / 详情 |

---

## 4. 新规录入弹窗

### 4.1 触发

点击 `[+ 新规录入]` 按钮 → 弹出表单弹窗。

### 4.2 表单字段

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 需求类型 | `el-radio-group` | ✅ | `REPLENISHMENT`(补货) / `NEW_PURCHASE`(新品采购) |
| 主货号 | `el-input` + 商品选择器 | ✅ | 来自 Product.masterCode |
| 子货号 | `el-select`（可选子货号列表） | | 根据主货号过滤颜色变体；新品采购可为空 |
| 需求量 | `el-input-number` | ✅ | 正整数 |
| 目的地 | `el-input` | | 发送给哪个日本客户 |
| 日本担当 | `el-select`（担当者列表） | | 来自系统用户表 |

### 4.3 业务规则

- 选择主货号后，若该货号有子货号（颜色变体），子货号选择器自动加载
- 新品采购：subProductCode 可为空（新品只有主货号）
- 非新品补货：subProductCode 建议填入（补货通常是已有颜色）

### 4.4 提交

- POST `/api/v1/replenishment-demands`
- 成功后刷新列表；弹窗关闭

---

## 5. 转采购（关键操作）

### 5.1 触发条件

状态 = `PENDING`，操作列点击 `[转采购]`。

### 5.2 行为

1. 弹出确认框，显示需求基本信息
2. 点击确认 → 跳转至 `/procurement/order?fromDemand={id}`
3. 发注单页面预填充以下字段（来自需求单）：
   - `productCode`
   - `subProductCode`
   - `quantity`
   - `destination`
   - `japanLead`
4. 用户补充其余字段后提交

### 5.3 后端联动

- 调用 `POST /api/v1/replenishment-demands/{id}/convert`
- 后端创建 Procurement 实体，状态推进为 `CONVERTED`

---

## 6. 状态流转

```
  PENDING ──[转采购]──▶ CONVERTED
     │
     └──[取消]──▶ CANCELLED
```

| 状态 | 颜色 | 可用操作 |
|------|------|---------|
| PENDING | `#FEF3C7` 黄色 | 转采购 / 取消 / 详情 |
| CONVERTED | `#DBEAFE` 蓝色 | 详情 |
| CANCELLED | `#F3F4F6` 灰色 | 详情 |

---

## 7. API 集成

> ⚠️ API 路径已更正（2026-04-22）：后端 Controller 为 `@RequestMapping("/api/v1/demands")`，非 `replenishment-demands`。

| 操作 | Method | Endpoint |
|------|--------|----------|
| 分页查询 | GET | `/api/v1/demands?page=&pageSize=&demandType=&productCode=&status=` |
| 详情 | GET | `/api/v1/demands/{id}` |
| 创建 | POST | `/api/v1/demands` |
| 更新 | PATCH | `/api/v1/demands/{id}` |
| 转采购 | POST | `/api/v1/demands/{id}/convert` |
| 删除 | DELETE | `/api/v1/demands/{id}` |

---

## 8. 缺口与 TODO

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 商品选择器 | P1 | 需对接 Product 列表 API；按 masterCode 搜索 |
| 担当者选择器 | P1 | 需系统用户管理表；目前 hardcode 列表 |
| 转采购预填充 | P1 | OrderPage.vue 需解析 URL query 参数 `fromDemand` |
| 取消操作 | P2 | 确认框 + DELETE API |

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
