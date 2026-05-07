# 前端 UI 文档

> ManpouChinaSystem 前端页面文档 — 截图、设计说明、组件对应

---

## 编号规则

| 编号 | 含义 |
|------|------|
| UI-01 ~ UI-03 | 系统页面（登录/仪表盘/示例），无业务步号 |
| UI-04 ~ UI-12 | 业务步号 01 ~ 09（与业务步号一一对应） |

---

## 文档索引

| 编号 | 文档 | 页面路径 | 步号 | 状态 |
|------|------|---------|------|------|
| UI-01 | [pages/01-login.md](pages/01-login.md) | `/login` | — | ✅ |
| UI-02 | [pages/02-dashboard.md](pages/02-dashboard.md) | `/dashboard` | — | ✅ |
| UI-03 | [pages/03-examples.md](pages/03-examples.md) | `/examples` | — | ✅ |
| UI-04 | [pages/01-demand.md](pages/01-demand.md) | `/procurement/demand` | 01 | ✅ |
| UI-05 | [pages/02-procurement.md](pages/02-procurement.md) | `/procurement/procurement` | 02 | ✅ |
| UI-06 | [pages/03-inspection.md](pages/03-inspection.md) | `/procurement/inspection` | 03 | ✅ |
| UI-07 | [pages/04-logistics.md](pages/04-logistics.md) | `/procurement/logistics` | 04 | ✅ |
| UI-08 | [pages/05-domestic-customs.md](pages/05-domestic-customs.md) | `/procurement/customs` | 05 | ✅ |
| UI-09 | [pages/06-japan-customs.md](pages/06-japan-customs.md) | `/procurement/japan-customs` | 06 | ✅ |
| UI-10 | [pages/07-tax-refund.md](pages/07-tax-refund.md) | `/finance/tax-refund` | 07 | ✅ |
| UI-11 | [pages/08-sales.md](pages/08-sales.md) | `/sales/operations` | 08 | ✅ |
| UI-12 | [pages/09-order-overview.md](pages/09-order-overview.md) | `/base/overview` | 09 | ✅ |
| UI-13 | [pages/10-product.md](pages/10-product.md) | `/base/product` | — | ✅ |
| UI-14 | [pages/11-factory.md](pages/11-factory.md) | `/base/factory` | — | ✅ |
| UI-15 | [pages/12-demand-overview.md](pages/12-demand-overview.md) | `/base/overview/demand/:id` | — | ✅ |
| UI-16 | [pages/13-procurement-overview.md](pages/13-procurement-overview.md) | `/base/overview/order/:id` | — | ✅ |
| UI-17 | [pages/14-user-management.md](pages/14-user-management.md) | `/system/user` | B-11 | ✅ Phase 3 完成 |
| UI-18 | [pages/15-role-management.md](pages/15-role-management.md) | `/system/role` | B-11 | ✅ Phase 3 完成 |
| UI-19 | [pages/16-audit-log.md](pages/16-audit-log.md) | `/system/audit-log` | B-11 | 📋 待开发 |
| UI-20 | [pages/17-profile.md](pages/17-profile.md) | `/profile` | B-11 | 📋 待开发 |
| — | [05-UI改版方案-橙色商业主题.md](05-UI改版方案-橙色商业主题.md) | — | — | ✅ |
| — | [ARCHITECTURE.md](ARCHITECTURE.md) | — | — | ✅ |

---

## 侧边栏路由（与 router/index.ts 一一对应）

| 步号 | 路由 | 组件 | 页面文档 |
|------|------|------|---------|
| — | `/login` | LoginPage.vue | UI-01 |
| — | `/dashboard` | DashboardPage.vue | UI-02 |
| 01 | `/procurement/demand` | DemandPage.vue | UI-04 |
| 02 | `/procurement/procurement` | ProcurementPage.vue | UI-05 |
| 03 | `/procurement/qc-record` | QcRecordPage.vue | UI-06 |
| 04 | `/procurement/logistics-plan` | LogisticsPlanPage.vue | UI-07 ✅ |
| 05 | `/procurement/domestic-customs` | DomesticCustomsPage.vue | UI-08 ✅ |
| 06 | `/procurement/japan-customs` | JapanCustomsRecordPage.vue | UI-09 ✅ |
| — | `/finance/tax-refund-record` | TaxRefundRecordPage.vue | UI-10 ✅ |
| — | `/sales/sales-record` | SalesRecordPage.vue | UI-11 ✅ |
| — | `/base/factory` | FactoryPage.vue | — |
| — | `/base/product` | ProductPage.vue | — |
| 09 | `/base/overview` | OrderOverviewPage.vue | UI-12 ✅ |
| B-11 | `/system/user` | UserPage.vue | UI-17 ✅ |
| B-11 | `/system/role` | RolePage.vue | UI-18 ✅ |
| B-11 | `/system/audit-log` | AuditLogPage.vue | UI-19 📋 |
| B-11 | `/profile` | ProfilePage.vue | UI-20 📋 |

---

## 业务步号映射（三目录严格对应）

| 步号 | 业务文档 | UI文档 | 数据库文档 |
|------|---------|--------|-----------|
| 01 | B-01 补货需求 | UI-04 01-demand | DB-01 procurement-demand |
| 02 | B-02 发注单+工厂 | UI-05 02-procurement | DB-02 procurement-order |
| 03 | B-03 验货记录 | UI-06 03-inspection | DB-03 inspection |
| 04 | B-04 调配计划 | UI-07 04-logistics | DB-04 logistics |
| 05 | B-05 国内报关 | UI-08 05-domestic-customs | DB-05 domestic-customs |
| 06 | B-06 日本清关 | UI-09 06-japan-customs | DB-06 japan-customs |
| 07 | B-07 退税 | UI-10 07-tax-refund | DB-07 tax-refund |
| 08 | B-08 运营销售 | UI-11 08-sales | DB-08 sales |
| 09 | B-09 订单总览 | UI-12 09-order-overview | DB-09 order-overview |
