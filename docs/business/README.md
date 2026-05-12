# 业务文档 — README

> **版本**: 1.2.0
> **更新**: 2026-05-07（新增审计报告；B-API/B-DOMAIN 版本更新）

---

## 编号规则

| 编号 | 含义 |
|------|------|
| B-00 | 全链路总览（主入口） |
| B-01 ~ B-09 | 业务步号 01~09（与 UI 步号/数据库编号一一对应） |
| B-API | API 契约 |
| B-DOMAIN | 领域模型 |
| B-PLAN | 调配计划详细规格 |
| B-INSPECT | 验货记录详细规格 |

---

## 文档索引

| 编号 | 文档 | 说明 |
|------|------|------|
| **B-00** | **[SPEC-B00-全链路总览.md](./SPEC-B00-全链路总览.md)** | **主入口**：9步全链路字段地图 + 循环反馈 + 缺口总表 |
| B-01 | [SPEC-B01-补货需求-步骤1.md](./SPEC-B01-补货需求-步骤1.md) | 补货需求聚合根 ✅ |
| B-02 | [SPEC-B02-发注单-步骤2.md](./SPEC-B02-发注单-步骤2.md) | 发注单+工厂聚合根 ✅ |
| B-03 | [SPEC-B03-验货记录-步骤3.md](./SPEC-B03-验货记录-步骤3.md) | 验货记录聚合根 ✅ |
| B-04 | [SPEC-B04-调配计划-步骤4.md](./SPEC-B04-调配计划-步骤4.md) | 调配计划+货柜+拼柜 ✅（v1.3.0） |
| B-05 | [SPEC-B05-国内报关-步骤5.md](./SPEC-B05-国内报关-步骤5.md) | DomesticCustomsRecord ✅（v1.4.0） |
| B-06 | [SPEC-B06-日本清关-步骤6.md](./SPEC-B06-日本清关-步骤6.md) | JapanCustomsRecord ✅（v1.4.0） |
| B-07 | [SPEC-B07-退税-步骤7.md](./SPEC-B07-退税-步骤7.md) | TaxRefundRecord ✅（v1.2.0） |
| B-08 | [SPEC-B08-运营销售-步骤8.md](./SPEC-B08-运营销售-步骤8.md) | SalesRecord ✅（v1.3.0） |
| B-09 | [SPEC-B09-订单总览-API设计.md](./SPEC-B09-订单总览-API设计.md) | **核心视图**：聚合接口设计 ✅ |
| B-10 | [SPEC-B10-商品目录-产品管理.md](./SPEC-B10-商品目录-产品管理.md) | Product + product_factory 多对多设计 🟡 |
| B-11 | [SPEC-B11-用户中心与权限体系.md](./SPEC-B11-用户中心与权限体系.md) | 用户管理+角色权限+操作日志+个人中心 📋 |
| B-12 | [SPEC-B12-货柜与船只管理.md](./SPEC-B12-货柜与船只管理.md) | 货柜管理+船只管理+柜-船关联+状态机 📋 |
| B-API | [API-发注管理.md](./API-发注管理.md) | REST 接口契约 |
| B-DOMAIN | [DOMAIN-发注管理领域模型.md](./DOMAIN-发注管理领域模型.md) | 聚合根、值对象、枚举、仓储接口 |
| B-PLAN | [SPEC-B04-调配计划-步骤4.md](./SPEC-B04-调配计划-步骤4.md) | LogisticsPlan 状态机（PLANNED→BOOKED→IN_TRANSIT→DELIVERED） |
| B-INSPECT | [SPEC-B03-验货记录-步骤3.md](./SPEC-B03-验货记录-步骤3.md) | QcRecord 状态机（COMPLETED/RETURN_REQUESTED） |
| 审计报告 | [../audit/BUSINESS-LOGIC-AUDIT-2026-05-07.md](../audit/BUSINESS-LOGIC-AUDIT-2026-05-07.md) | FSM/DB-Entity/计算字段/字段必填五层对齐审计 |

---

## 业务步号映射

| 步号 | 业务文档 | UI文档 | 数据库文档 |
|------|---------|--------|-----------|
| 00 | B-10 商品目录 | 10-product.md | DB-11 product + product_factory |
| 01 | B-01 补货需求 | 01-demand.md | DB-01 procurement-demand |
| 02 | B-02 发注单+工厂 | 02-procurement.md | DB-02 procurement-order |
| 03 | B-03 验货记录 | 03-inspection.md | DB-03 inspection |
| 04 | B-04 调配计划 | 04-logistics.md | DB-04 logistics |
| 05 | B-05 国内报关 | 05-domestic-customs.md | DB-05 domestic-customs |
| 06 | B-06 日本清关 | 06-japan-customs.md | DB-06 japan-customs |
| 07 | B-07 退税 | 07-tax-refund.md | DB-07 tax-refund |
| 08 | B-08 运营销售 | 08-sales.md | DB-08 sales |
| 09 | B-09 订单总览 | 09-order-overview.md | DB-09 order-overview |
| B-11 | B-11 用户中心与权限体系 | UI-17~20 | V4~V9 |
| B-12 | B-12 货柜与船只管理 | UI-19（container v2.0）+ UI-21（ship） | DB-14 ship+container 扩展 |
