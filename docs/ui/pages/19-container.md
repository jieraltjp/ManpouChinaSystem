# 页面规格 — 货柜（v2.0）

> **版本**: v2.0.0
> **更新**: 2026-05-12（v2.0：扩展物流字段 + 船只关联 + 分配船只功能）
> **创建**: 2026-05-07（v1.0.0）
> **状态**: 📋 待更新（Phase 3）
> **对应业务步号**: B-12（货柜与船只管理）
> **对应后端**: `ContainerController`（扩展）→ `/api/v1/containers`
> **对应 API**: `containerApi`（`api/logistics.ts`，扩展）
> **对应路由**: `/base/container`（原 `/procurement/container`）
> **对应 DB**: DB-04 v1.5.0（container 表已有）→ DB-14（container 扩展字段，V18）
> **对应 SPEC**: `SPEC-B12 §6.3`

---

## 1. 页面定位

> **本版本变更**：在 v1.0.0 基础上新增船只关联字段（ship_id / time_slot / arrival_location / remarks）和分配船只功能。

货柜页面管理实际装柜记录。追踪货柜生命周期：创建 → 装柜完成 → 离港 → 到港。

**前置**：拼柜池（ConsolidationPool）
**后续**：步骤5（国内报关 DomesticCustomsRecord）

---

## 2. 列表页

### 2.1 筛选器

| 字段 | 控件 | 说明 |
|------|------|------|
| 货柜号 containerNo | `el-input` | 模糊搜索 |
| 状态 status | `el-select`（全部 / 待配船 / 已装柜 / 已离港 / 已到港） | 单选 |
| 船只 shipId | `el-select`（全部 / [船只下拉列表]） | 新增：分配船只筛选 |
| 新增货柜 | `el-button type="primary"` | hasPermission('container:create') |

> **v2.0 变更**：新增"船只"筛选器（下拉可选已有船只，用于过滤已分配到某船的货柜）

### 2.2 表格

| 列 | 字段 | 宽度 | 说明 |
|----|------|------|------|
| 货柜号 | containerNo | min-width=140 | — |
| 柜型 | containerType | 80px | tag 展示 |
| 总体积(m³) | totalCbm | 90px | 右对齐，4位小数 |
| 总重量(kg) | totalWeightKg | 100px | 右对齐，2位小数 |
| 调配计划数 | planCount | 80px | 居中 |
| 状态 | status | 90px | tag |
| 船名 | shipName | min-width=100 | v2.0 新增（ship_id → ship.shipName） |
| 船号 | shipNumber | min-width=100 | v2.0 新增（ship_id → ship.shipNumber） |
| 装柜日 | loadDate | 100px | — |
| 离港日 | departureDate | 100px | — |
| 到港日 | arrivalDate | 100px | — |
| 创建时间 | createTime | min-width=160 | locale 格式 |
| 操作 | — | 150px | 详情 + 编辑（hasPermission('container:update')）+ 分配船只（v2.0 新增） |

> **v2.0 变更**：
> - 新增"船名"和"船号"两列（ship → shipName / shipNumber）
> - 新增"分配船只"操作按钮（shipId 为空时显示）

### 2.3 状态机

```
CREATED（待配船） → LOADED（已装柜） → DEPARTED（已离港） → ARRIVED（已到港）
```

| 状态 | 标签颜色 | 说明 |
|------|----------|------|
| `CREATED` | info（灰） | 初始，待配船 |
| `LOADED` | success（绿） | 已分配船只，已装柜 |
| `DEPARTED` | warning（橙） | 已离港 |
| `ARRIVED` | primary（蓝） | 已到港 |

---

## 3. 新增弹窗（v1.0.0，保留）

| 字段 | 控件 | 必填 | 说明 |
|------|------|:----:|------|
| 货柜号 containerNo | `el-input` | ✅ | 唯一 |
| 柜型 containerType | `el-select` | ✅ | GP20 / GP40 / HC40 / HC45 |

> v2.0 说明：新建货柜时默认 ship_id = NULL（待配船），物流信息在"分配船只"或编辑时填写。

---

## 4. 编辑弹窗（v2.0 扩展）

> v2.0 变更：在原有字段基础上新增物流字段。

| 字段 | 控件 | 必填 | 说明 |
|------|------|:----:|------|
| 货柜号 containerNo | `el-input` | ✅ | 不可编辑（已装货物） |
| 柜型 containerType | `el-select` | ✅ | — |
| **关联船只 shipId** | `el-select`（可搜） | 否 | v2.0 新增：下拉选已有船只，选"无"=解除关联 |
| **时间段 timeSlot** | `el-input` | 否 | v2.0 新增，如 "2026-W24" |
| **到岗地点 arrivalLocation** | `el-input` | 否 | v2.0 新增，最大128字符 |
| **备注 remarks** | `el-input type="textarea"` | 否 | v2.0 新增，最大512字符 |
| 装柜日 loadDate | `el-date-picker` | 否 | — |
| 离港日 departureDate | `el-date-picker` | 否 | — |
| 到港日 arrivalDate | `el-date-picker` | 否 | — |

> **状态自动推进**：loadDate 填写 → status → LOADED；departureDate 填写 → status → DEPARTED；arrivalDate 填写 → status → ARRIVED。

---

## 5. 详情抽屉（v2.0 扩展）

```
┌──────────────────────────────────────────────────────┐
│ 货柜详情                                  [关闭]    │
├──────────────────────────────────────────────────────┤
│ 货柜号           TEMU001                            │
│ 货柜类型         GP20                               │
│ 总体积(m³)       65.3000                           │
│ 总重量(kg)       12500.00                          │
│ 调配计划数       3                                 │
│ 状态             已离港（DEPARTED）                │
├──────────────────────────────────────────────────────┤
│ ▼ 物流信息                                    [编辑] │
│ 关联船只         日章丸（V2026A）                   │
│ 出发港           上海港                            │
│ 目的港           東京港                            │
│ 装柜日期         2026-05-01                        │
│ 离港日期         2026-05-03                        │
│ 到港日期         —                                 │
│ 时间段           2026-W19                           │
│ 到岗地点         东京仓库A                         │
│ 备注             需加急清关                        │
├──────────────────────────────────────────────────────┤
│ ▼ 审计信息                                    [折叠] │
│ 创建人           admin                             │
│ 创建时间         2026-05-01 10:30:00               │
│ 更新人           manager                           │
│ 更新时间         2026-05-03 08:15:22              │
└──────────────────────────────────────────────────────┘
```

> **v2.0 变更**：新增"物流信息"区块（shipName / shipNumber / departurePort / arrivalPort / timeSlot / arrivalLocation / remarks）和审计信息折叠区块。

---

## 6. 分配船只快捷操作（v2.0 新增）

**触发条件**：货柜 `ship_id` 为 NULL（状态 = CREATED）时，操作列显示"分配船只"按钮。

**操作流程**：
1. 点击"分配船只"按钮 → 弹出简化的分配弹窗
2. 选择船只（下拉列表，显示 shipName + shipNumber）
3. 可选填写装柜日期（loadDate）
4. 点击确定 → 调用 `PUT /api/v1/containers/{id}/assign-ship`
5. 成功后自动刷新列表，状态变更为 LOADED

**分配弹窗字段**：

| 字段 | 控件 | 必填 |
|------|------|:----:|
| 选择船只 | `el-select`（可搜） | ✅ |
| 装柜日期 loadDate | `el-date-picker` | 否 |

---

## 7. API 端点（v2.0 扩展）

| 方法 | 路径 | @PreAuthorize | 说明 |
|------|------|:-------------:|------|
| `GET` | `/api/v1/containers` | `container:read` | 分页列表（含 ship 关联信息） |
| `GET` | `/api/v1/containers/{id}` | `container:read` | 详情（含船名/船号/物流字段） |
| `POST` | `/api/v1/containers` | `container:create` | 新增 |
| `PATCH` | `/api/v1/containers/{id}` | `container:update` | 编辑（含物流字段） |
| `DELETE` | `/api/v1/containers/{id}` | `container:delete` | 删除 |
| `PUT` | `/api/v1/containers/{id}/assign-ship` | `container:update` | **v2.0 新增**：分配船只 |
| `PUT` | `/api/v1/containers/{id}/unassign-ship` | `container:update` | **v2.0 新增**：解除船只关联 |
| `POST` | `/api/v1/containers/{id}/plans/{planId}` | — | 添加调配计划（已有） |

---

## 8. i18n key 前缀

`logistics.container.*`（沿用 v1.0.0）

| key | 中文 | 日文 | 变更 |
|-----|------|------|------|
| `logistics.container.title` | 货柜管理 | コンテナ管理 | — |
| `logistics.container.column.shipName` | 船名 | 船名 | v2.0 新增 |
| `logistics.container.column.shipNumber` | 船号 | 船番号 | v2.0 新增 |
| `logistics.container.column.timeSlot` | 时间段 | 時間帯 | v2.0 新增 |
| `logistics.container.column.arrivalLocation` | 到岗地点 | 到着場所 | v2.0 新增 |
| `logistics.container.column.remarks` | 备注 | 備考 | v2.0 新增 |
| `logistics.container.dialog.shipId` | 关联船只 | 船舶を選択 | v2.0 新增 |
| `logistics.container.dialog.timeSlot` | 时间段 | 時間帯 | v2.0 新增 |
| `logistics.container.dialog.arrivalLocation` | 到岗地点 | 到着場所 | v2.0 新增 |
| `logistics.container.dialog.remarks` | 备注 | 備考 | v2.0 新增 |
| `logistics.container.drawer.section.logistics` | 物流信息 | 物流情報 | v2.0 新增 |
| `logistics.container.drawer.section.audit` | 审计信息 | 監査情報 | v2.0 新增 |
| `logistics.container.action.assignShip` | 分配船只 | 船舶割当 | v2.0 新增 |
| `logistics.container.filter.ship` | 船只 | 船舶 | v2.0 新增 |
| `logistics.container.assignShip.title` | 分配船只 | 船舶割当 | v2.0 新增 |
