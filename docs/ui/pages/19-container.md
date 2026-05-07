# 页面规格 — 货柜

> **版本**: 1.0.0
> **创建**: 2026-05-07
> **状态**: ✅ 已实现（ContainerController + ContainerPage.vue）
> **对应业务步号**: 04（调配计划）
> **对应后端**: `POST/GET/PATCH/DELETE /api/v1/containers`
> **对应 API**: `containerApi`（`api/logistics.ts`）
> **对应路由**: `/procurement/container`
> **对应 DB**: `DB-04 v1.5.0`

---

## 1. 页面定位

货柜页面管理实际装柜记录。追踪货柜生命周期：创建 → 装柜完成 → 离港 → 到港。

**前置**: 拼柜池（ConsolidationPool）
**后续**: 步骤5（国内报关 DomesticCustomsRecord）

---

## 2. 列表页

### 2.1 筛选器

| 字段 | 控件 | 说明 |
|------|------|------|
| 货柜号 containerNo | `el-input` | 模糊搜索 |
| 状态 status | `el-select`（全部 / CREATED / LOADED / DEPARTED / ARRIVED） | 多选 |

### 2.2 表格

| 列 | 说明 | 备注 |
|----|------|------|
| 货柜号 containerNo | min-width=160 | e.g. TEMU1234567 |
| 柜型 containerType | tag 展示 | `GP20`=20尺普柜(33.2m³) / `GP40`=40尺普柜(67.7m³) / `HC40`=40尺高柜(76.4m³) / `HC45`=45尺高柜(86.0m³) |
| 总体积 totalCbm | 右对齐，4位小数 | — |
| 总重量 totalWeightKg | 右对齐，2位小数 | — |
| 调配计划数 planCount | 居中 | — |
| 状态 status | tag：`CREATED`=info / `LOADED`=success / `DEPARTED`=warning / `ARRIVED`=primary | — |
| 装柜日 loadDate | 日期 | — |
| 离港日 departureDate | 日期 | — |
| 到港日 arrivalDate | 日期 | — |
| 创建时间 createTime | locale 格式 | — |
| 操作 | 编辑 + 删除 | — |

### 2.3 状态机

```
CREATED → LOADED（装柜完成）
LOADED → DEPARTED（离港）
DEPARTED → ARRIVED（到港）
ARRIVED → 终态
```

---

## 3. 新增弹窗

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 货柜号 containerNo | `el-input` | ✅ | 唯一 |
| 柜型 containerType | `el-select` | ✅ | GP20 / GP40 / HC40 / HC45 |

---

## 4. 编辑弹窗

| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 货柜号 containerNo | `el-input` | ✅ | 不可编辑（已装货） |
| 柜型 containerType | `el-select` | ✅ | — |
| 装柜日 loadDate | `el-date-picker` | 否 | — |
| 离港日 departureDate | `el-date-picker` | 否 | — |
| 到港日 arrivalDate | `el-date-picker` | 否 | — |

---

## 5. API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/containers` | 分页列表 |
| GET | `/api/v1/containers/{id}` | 详情 |
| POST | `/api/v1/containers` | 新增 |
| PATCH | `/api/v1/containers/{id}` | 更新 |
| DELETE | `/api/v1/containers/{id}` | 删除 |
| POST | `/api/v1/containers/{id}/plans/{planId}` | 添加调配计划 |

---

## 6. i18n key 前缀

`logistics.container.*`
