# 页面规格 — 步骤5：国内报关

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **更新**: 2026-04-22 — 前端页面已实现
> **路由**: `/procurement/domestic-customs`
> **组件**: `DomesticCustomsPage.vue`（`apps/web/src/pages/customs/DomesticCustomsPage.vue`）
> **对应后端**: `DomesticCustomsRecord` 聚合根（✅ 已实现）
> **依赖文档**: `SPEC-B00-全链路总览.md` · `SPEC-B05-国内报关-步骤5.md`
> **前置步骤**: 步骤4（调配计划已发货 LogisticsPlan.status = IN_TRANSIT）
> **后续步骤**: 步骤6（日本清关 JapanCustomsRecord — 待实现）

---

## 1. 页面定位

国内出口报关管理。对应业务流第五步。货物离港后，提交出口报关资料。

> ⚠️ **占位页面** — 字段待业务方确认后补充完整设计。当前仅定义页面骨架。

---

## 2. 布局结构（骨架）

```
┌────────────────────────────────────────────────────────────────────┐
│ 页面标题：国内报关                                   [+ 新规报关]  │
├────────────────────────────────────────────────────────────────────┤
│ 统计卡                                                               │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│ │ PENDING    │ │ SUBMITTED  │ │ CLEARED    │ │   合计     │       │
│ │ 待申报     │ │ 已提交     │ │ 已放行     │ │            │       │
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘       │
├────────────────────────────────────────────────────────────────────┤
│ 筛选栏                                                               │
│ 报关单号 [________]  采购单号 [________]  状态 [全部▼]  日期范围   │
│                                                [搜索]  [重置]       │
├────────────────────────────────────────────────────────────────────┤
│ 表格（占位字段，待确认）                                             │
│ ┌────────┬──────────┬────────┬────────┬────────┬────────┐         │
│ │报关单号 │采购单号  │货号    │HS编码  │申报日期│状态    │ 操作  │
│ └────────┴──────────┴────────┴────────┴────────┴────────┘         │
└────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表格列定义（待确认字段）

> ⚠️ 以下字段来源于 `SPEC-B05-国内报关-步骤5.md`，**待业务方提供真实报关单样本后确认**。

| 列名 | 字段 | 来源 | 说明 |
|------|------|------|------|
| 报关单号 | `customsDeclarationNo` | 系统/海关返回 | — |
| 采购单号 | `procurementId` | 关联 | — |
| 货号 | `productCode` | 来自 Procurement | — |
| HS编码 | `hsCode` | 用户输入/自动查询 | — |
| 申报日期 | `declarationDate` | 用户输入 | — |
| 申报人 | `declarant` | 用户输入 | — |
| 出口口岸 | `exportPort` | 用户输入 | — |
| 申报价值(CNY) | `declaredValueRmb` | 用户输入 | — |
| 状态 | `status` | 系统 | PENDING / SUBMITTED / CLEARED / FAILED |
| 操作 | — | — | 详情 / 编辑 |

> **前端表格布局**：列宽用 `min-width`，不写 `table-layout="fixed"`，操作列不写 `fixed="right"`。详见 [docs/ui/ARCHITECTURE.md §8](../ARCHITECTURE.md#8-element-plus-表格布局规范)。

---

## 4. 新规报关弹窗（骨架）

### 4.1 触发

点击 `[+ 新规报关]` → 弹出表单弹窗。

### 4.2 表单字段（占位，待确认）

**关联信息（自动代入）**：

| 字段 | 来源 | 说明 |
|------|------|------|
| 关联采购单 | 用户选择 | 选择 LogisticsPlan 或 Procurement |
| 货号 | 自动代入 | — |
| 商品名称 | 自动代入 | 来自 Product.nameZh |
| 起运港 | 自动代入 | 来自 Container.departurePort（Container 实体实现后补充） |

**用户填入（待确认）**：

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 报关单号 | `el-input` | | 系统生成 or 海关返回？ |
| HS编码 | `el-input` + HS查询 | | 可按 productCode 自动查询 Product.hsCode |
| 申报日期 | `el-date-picker` | ✅ | — |
| 申报人 | `el-input` | ✅ | — |
| 出口口岸 | `el-select` | ✅ | 宁波/上海/其他 |
| 申报价值(CNY) | `el-input-number` | | — |
| 商品规格 | `el-input` | | — |
| 数量 | `el-input-number` | | — |
| 毛重(kg) | `el-input-number` | | — |
| 备注 | `el-input`（textarea） | | — |

---

## 5. 状态流转（骨架）

```
  PENDING ──[提交]──▶ SUBMITTED ──[放行]──▶ CLEARED
                                        └──[驳回]──▶ FAILED
```

| 状态 | 颜色 | 说明 |
|------|------|------|
| PENDING | 黄色 | 待申报 |
| SUBMITTED | 蓝色 | 已提交海关 |
| CLEARED | 绿色 | 已放行 |
| FAILED | 红色 | 申报失败/被驳回 |

---

## 6. 自动触发规则

建议：**LogisticsPlan.status = IN_TRANSIT 时，自动创建 DomesticCustomsRecord（status = PENDING）**。

---

## 7. API 集成（骨架）

| 操作 | Method | Endpoint | 状态 |
|------|--------|----------|------|
| 分页查询 | GET | `/api/v1/customs?page=&pageSize=&procurementId=&status=` | ✅已实现 |
| 详情 | GET | `/api/v1/customs/{id}` | ✅已实现 |
| 创建 | POST | `/api/v1/customs` | ✅已实现 |
| 更新 | PUT | `/api/v1/customs/{id}` | ✅已实现 |
| 提交 | PATCH | `/api/v1/customs/{id}/submit` | ✅已实现 |
| 放行 | PATCH | `/api/v1/customs/{id}/clear` | ✅已实现 |
| 驳回 | PATCH | `/api/v1/customs/{id}/reject` | ✅已实现 |
| 删除 | DELETE | `/api/v1/customs/{id}` | ✅已实现 |

---

## 8. 缺口阻塞

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 报关单字段确认 | **P0** | 需提供真实报关单样本 |
| HS编码查询 | P1 | Product 表需新增 hsCode 字段 |
| 自动触发逻辑 | P1 | IN_TRANSIT → 自动创建 DomesticCustomsRecord |
| 出口口岸枚举 | P1 | 宁波/上海/大连/天津/其他 |

---

## 9. 组件拆分建议

| 组件 | 职责 |
|------|------|
| `DomesticCustomsPage.vue` | 容器：列表 + 筛选 + 统计卡 |
| `DomesticCustomsTable.vue` | 表格 + 分页 |
| `DomesticCustomsFormDialog.vue` | 新规/编辑表单 |
| `DomesticCustomsDetailDrawer.vue` | 详情抽屉 |
| `HsCodeSelect.vue` | HS编码搜索选择器（待实现） |
| `useDomesticCustoms.ts` | API 调用（composable） |
