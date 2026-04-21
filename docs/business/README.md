# 发注管理 — 业务文档

> 业务分析 + API 契约 + 领域模型

---

## 实现进度

| 模块 | 文档状态 | 代码实现 | 备注 |
|------|---------|---------|------|
| 发注单（Procurement） | ✅ 完成 | ✅ CRUD+报价计算+终态校验+前端对接+完整FSM流转+工厂内嵌（v1.3.0） | manpou-allinone Phase A ✅ |
| 补货需求（ReplenishmentDemand） | ✅ 完成 | ✅ CRUD + 自动带入发注单 | manpou-allinone Phase A ✅ |
| 工厂（Factory） | ✅ 完成 | ✅ 无独立页面，内嵌于发注单表单（v1.3.0） | manpou-allinone ✅ |
| 调配计划（LogisticsPlan） | 🟡 设计中 | 🟡 骨架已生成（LogisticsStatus） | — |
| 验货记录（QcRecord） | 🟡 设计中 | 🟡 骨架已生成（QcStatus） | — |
| 商品目录（Product） | ✅ 完成 | 🔴 未实现 | — |
| 货柜管理（Container） | ✅ 完成 | 🔴 未实现 | — |
| 财务结算（FinanceRecord） | ✅ 完成 | 🔴 未实现 | — |
| 退货管理（ReturnRecord） | ✅ 完成 | 🔴 未实现 | — |
| 拼柜池（ConsolidationPool） | ✅ 完成 | 🔴 未实现 | Phase 4 |

> 🔴 = 仅骨架/未实现  🟡 = 部分实现  ✅ = 完整实现

---

## 文档索引

| 编号 | 文档 | 说明 |
|------|------|------|
| 00 | [SPEC-发注管理流程.md](./SPEC-发注管理流程.md) | 需求背景、功能范围、状态机（19态）、测试清单 |
| 01 | [API-发注管理.md](./API-发注管理.md) | REST 接口契约、请求/响应格式、错误码 |
| 02 | [DOMAIN-发注管理领域模型.md](./DOMAIN-发注管理领域模型.md) | 聚合根、值对象、枚举、仓储接口、领域服务 |
| 03 | [SPEC-调配计划流程.md](./SPEC-调配计划流程.md) | LogisticsPlan 状态机（PLANNED→BOOKED→IN_TRANSIT→DELIVERED） |
| 04 | [SPEC-验货记录流程.md](./SPEC-验货记录流程.md) | QcRecord 状态机（PASSED/FAILED）+ 与发注单联动 |
| DB | [../database/DB-发注管理数据库设计-步骤1-4.md](../database/DB-发注管理数据库设计-步骤1-4.md) | 表清单、实体-表映射（v1.3.0） |

---

## 实现规划

| Phase | 范围 | 目标 |
|--------|------|------|
| Phase 1 | ✅ 发注单 CRUD + 需求带入 + 工厂内嵌 + 报价计算 + 终态校验 + 前端对接 | ✅ 完成 |
| Phase 2 | 调配计划 + 验货记录业务实现 | 业务完整闭环 |
| Phase 3 | Product 骨架替换为真实商品目录 | 待开发 |
| Phase 4 | 微服务抽取（可选）+ Kafka 事件驱动 | 架构演进 |
