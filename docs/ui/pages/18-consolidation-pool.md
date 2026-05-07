# 页面规格 — 拼柜池

> **版本**: 1.0.0
> **创建**: 2026-05-07
> **状态**: ✅ 已实现（ConsolidationPoolController + ConsolidationPoolPage.vue）
> **对应业务步号**: 04（调配计划）
> **对应后端**: `POST/GET/PATCH/DELETE /api/v1/consolidation-pools`
> **对应 API**: `consolidationPoolApi`（`api/logistics.ts`）
> **对应路由**: `/procurement/consolidation-pool`
> **对应 DB**: `DB-04 v1.5.0`

---

## 1. 页面定位

拼柜池页面将多个发注单（Procurement）的调配计划合并到同一目的港，等待装柜。汇总体积/重量达到阈值（默认 70m³）时触发装柜建议。

**前置**: 步骤2（发注单已创建）
**后续**: 步骤4 → Container（货柜）

---

## 2. 列表页

### 2.1 筛选器

| 字段 | 控件 | 说明 |
|------|------|------|
| 目的港 destinationPort | `el-input` | 模糊搜索 |
| 状态 status | `el-select`（全部 / OPEN / PENDING / LOADED / SHIPPED） | 多选 |

### 2.2 表格

| 列 | 说明 | 备注 |
|----|------|------|
| 池编号 poolCode | min-width=160 | e.g. CP-20260430-001 |
| 目的港 destinationPort | min-width=140 | — |
| 总体积 totalCbm | 右对齐，4位小数 + "m³" | — |
| 总重量 totalWeightKg | 右对齐，2位小数 + "kg" | — |
| 调配计划数 planCount | 居中 | — |
| 装柜阈值 containerThresholdCbm | 默认 70m³ | — |
| 状态 status | tag：`OPEN`=success / `PENDING`=warning / `LOADED`=primary / `SHIPPED`=info | — |
| 创建时间 createTime | locale 格式 | — |
| 操作 | 编辑 + 创建货柜 + 删除 | — |

### 2.3 状态机

```
OPEN → PENDING（手动关闭）/ LOADED（自动装柜）/ OPEN
PENDING → LOADED（装柜完成）
LOADED → SHIPPED（离港）
SHIPPED → 终态
```

---

## 3. 新增弹窗

| 字段 | 控件 | 必填 | 默认值 |
|------|------|------|--------|
| 目的港 destinationPort | `el-input`（max 64） | ✅ | — |
| 装柜阈值 containerThresholdCbm | `el-input-number` | 否 | 70 m³ |

---

## 4. 编辑弹窗

同新增表单。

---

## 5. API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/consolidation-pools` | 分页列表 |
| GET | `/api/v1/consolidation-pools/{id}` | 详情 |
| POST | `/api/v1/consolidation-pools` | 新增 |
| PATCH | `/api/v1/consolidation-pools/{id}` | 更新 |
| DELETE | `/api/v1/consolidation-pools/{id}` | 删除 |
| POST | `/api/v1/consolidation-pools/{id}/plans/{planId}` | 添加调配计划 |
| DELETE | `/api/v1/consolidation-pools/{id}/plans/{planId}` | 移除调配计划 |

---

## 6. i18n key 前缀

`logistics.pool.*`
