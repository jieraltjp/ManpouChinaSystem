# UI-07 — 调配计划页面

> **版本**: 1.1.0
> **创建**: 2026-04-22
> **更新**: 2026-04-22
> **状态**: ✅（LogisticsController + LogisticsPage.vue 完整实现）
> **对应业务步号**: 04（调配计划）
> **对应业务文档**: `docs/business/SPEC-B04-调配计划-步骤4.md`
> **对应数据库文档**: `docs/database/DB-04-logistics.md`
> **对应前端组件**: `LogisticsPage.vue`
> **对应路由**: `/procurement/logistics`（侧边栏入口）

---

## 1. 页面概述

调配计划页面用于安排货物的运输方式（海运/空运/拼柜），生成 LogisticsPlan 记录。

**前置条件**: 步骤3（QcRecord.result = PASS）

---

## 2. 状态占位

> 当前 `LogisticsPage.vue` 已实现并对接真实 API。
> 本文档待补全：完整 UI 规格、字段说明、交互细节。
