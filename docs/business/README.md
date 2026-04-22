# 发注管理 — 业务文档

> 业务分析 + API 契约 + 领域模型

---

## 实现进度

| 模块 | 文档状态 | 代码实现 | 备注 |
|------|---------|---------|------|
| 发注单（Procurement） | ✅ 完成 | ✅ CRUD+报价计算+终态校验+前端对接+完整FSM流转+工厂内嵌（v1.3.0） | manpou-allinone Phase A ✅ |
| 补货需求（ReplenishmentDemand） | ✅ 完成 | ✅ CRUD + 自动带入发注单 | manpou-allinone Phase A ✅ |
| 工厂（Factory） | 🟡 完成 | ✅ 基础字段；🔴 缺微信/QQ/经纬度（见全链路索引） | manpou-allinone ✅ |
| 调配计划（LogisticsPlan） | ✅ 完成 | ✅ 实体+Repository+UseCase+Controller+前端页面（v1.3.0） | — |
| 验货记录（QcRecord） | ✅ 完成 | ✅ 实体+Repository+UseCase+Controller+前端页面（v1.3.0） | — |
| 商品目录（Product） | 🟡 完成 | ✅ 基础字段；🔴 缺 hsCode / taxPoint（见全链路索引） | — |
| 货柜管理（Container） | 🔴 待开发 | 字段已定义，代码未实现 | Phase 2 |
| 拼柜池（ConsolidationPool） | 🔴 待开发 | 字段已定义，代码未实现 | Phase 2 |
| 国内报关（DomesticCustomsRecord） | 🔴 待定 | 字段全部待业务确认（见全链路索引 §第五步） | Phase 3 |
| 日本清关（JapanCustomsRecord） | 🔴 待定 | 字段全部待业务确认（见全链路索引 §第六步） | Phase 3 |
| 退税（TaxRefundRecord） | 🔴 待定 | 触发时机和字段待确认（见全链路索引 §第七步） | Phase 3 |
| 运营销售（SalesRecord） | 🔴 待定 | 字段和触发机制待确认（见全链路索引 §第八步+反馈循环） | Phase 4 |
| 财务结算（FinanceRecord） | 🔴 待定 | — | Phase 3 |

> 🔴 = 仅骨架/未实现  🟡 = 部分实现  ✅ = 完整实现

---

## 文档索引

| 编号 | 文档 | 说明 |
|------|------|------|
| **00** | **[SPEC-全链路业务流索引.md](./SPEC-全链路业务流索引.md)** | **主入口**：8步全链路字段地图 + 循环反馈 + 商品数据库 + 缺口总表 |
| 01 | [SPEC-发注管理流程.md](./SPEC-发注管理流程.md) | 需求背景、功能范围、状态机（19态）、测试清单 |
| 02 | [API-发注管理.md](./API-发注管理.md) | REST 接口契约、请求/响应格式、错误码 |
| 03 | [DOMAIN-发注管理领域模型.md](./DOMAIN-发注管理领域模型.md) | 聚合根、值对象、枚举、仓储接口、领域服务 |
| 04 | [SPEC-调配计划流程.md](./SPEC-调配计划流程.md) | LogisticsPlan 状态机（PLANNED→BOOKED→IN_TRANSIT→DELIVERED） |
| 05 | [SPEC-验货记录流程.md](./SPEC-验货记录流程.md) | QcRecord 状态机（PASSED/FAILED）+ 与发注单联动 |
| 06 | [SPEC-国内报关-步骤5.md](./SPEC-国内报关-步骤5.md) | DomesticCustomsRecord 聚合根 ⚡占位 |
| 07 | [SPEC-日本清关-步骤6.md](./SPEC-日本清关-步骤6.md) | JapanCustomsRecord 聚合根 ⚡占位 |
| 08 | [SPEC-退税-步骤7.md](./SPEC-退税-步骤7.md) | TaxRefundRecord 聚合根 ⚡占位 |
| 09 | [SPEC-运营销售-步骤8.md](./SPEC-运营销售-步骤8.md) | SalesRecord 聚合根 + 反馈循环 ⚡占位 |
| 10 | [SPEC-订单总览-API设计.md](./SPEC-订单总览-API设计.md) | **核心视图**：GET /api/v1/orders/{id}/overview 聚合接口设计 |
| DB | [../database/DB-发注管理数据库设计-步骤1-4.md](../database/DB-发注管理数据库设计-步骤1-4.md) | 表清单、实体-表映射（v1.3.0） |

---

## 实现规划

| Phase | 范围 | 目标 |
|--------|------|------|
| Phase 1 | ✅ 发注单 CRUD + 需求带入 + 工厂内嵌 + 报价计算 + 终态校验 + 前端对接 | ✅ 完成 |
| Phase 2 | 调配计划 + 验货记录业务实现 | 业务完整闭环 |
| Phase 3 | Product 骨架替换为真实商品目录 | 待开发 |
| Phase 4 | 微服务抽取（可选）+ Kafka 事件驱动 | 架构演进 |
