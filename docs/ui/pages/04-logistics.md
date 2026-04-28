# UI-07 — 调配计划页面

> **版本**: 1.2.0
> **创建**: 2026-04-22
> **更新**: 2026-04-27（v1.2.0：新增货柜号字段，列表/弹窗/抽屉三处）
> **状态**: ✅（LogisticsController + LogisticsPlanPage.vue 完整实现）
> **对应业务步号**: 04（调配计划）
> **对应业务文档**: `docs/business/SPEC-B04-调配计划-步骤4.md`
> **对应数据库文档**: `docs/database/DB-04-logistics.md`
> **对应前端组件**: `LogisticsPlanPage.vue`
> **对应路由**: `/procurement/logistics-plan`（侧边栏入口）

---

## 1. 页面概述

调配计划页面用于安排货物的运输方式（海运/空运/拼柜），生成 LogisticsPlan 记录。

**前置条件**: 步骤3（QcRecord.result = PASS）

---

## 2. 货柜号字段（v1.3.0）

### 2.1 列表页表格

新增列：`货柜号`（`containerNo`），min-width=140，show-overflow-tooltip，显示同一货柜的所有货物。

### 2.2 新增弹窗

字段位置：验货记录下拉下方（第三行首列）
- 标签：`货柜号`
- 控件：`el-input`
- placeholder：`同批次货物填入相同货柜号`
- 必填：否

### 2.3 详情抽屉

显示位置：计划编号下方
- 标签：`货柜号`
- 值：若为空显示 `-`
