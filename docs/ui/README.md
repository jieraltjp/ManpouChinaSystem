# 前端 UI 文档

> ManpouChinaSystem 前端页面文档 — 截图、设计说明、组件对应

---

## 编号规则

| 编号 | 含义 |
|------|------|
| UI-01 ~ UI-03 | 系统页面（登录/仪表盘/示例），无业务步号 |
| UI-04 ~ UI-09 | 业务步号 02 ~ 09（与业务步号一一对应） |

---

## 文档索引

| 编号 | 文档 | 页面路径 | 步号 | 状态 |
|------|------|---------|------|------|
| UI-01 | [pages/01-login.md](pages/01-login.md) | `/login` | — | ✅ |
| UI-02 | [pages/02-dashboard.md](pages/02-dashboard.md) | `/dashboard` | — | ✅ |
| UI-03 | [pages/03-examples.md](pages/03-examples.md) | `/examples` | — | ✅ |
| UI-04 | [pages/02-procurement.md](pages/02-procurement.md) | `/procurement/order` | 02 | ✅ |
| UI-05 | [pages/04-logistics.md](pages/04-logistics.md) | `/procurement/logistics` | 04 | ⚡占位 |
| UI-06 | [pages/01-demand.md](pages/01-demand.md) | `/procurement/demand` | 01 | ✅ |
| UI-07 | [pages/03-inspection.md](pages/03-inspection.md) | `/procurement/inspection` | 03 | ✅ |
| UI-08 | [pages/05-domestic-customs.md](pages/05-domestic-customs.md) | `/customs/domestic` | 05 | ⚡占位 |
| UI-09 | [pages/06-japan-customs.md](pages/06-japan-customs.md) | `/customs/japan` | 06 | ⚡占位 |
| UI-10 | [pages/07-tax-refund.md](pages/07-tax-refund.md) | `/finance/tax-refund` | 07 | ⚡占位 |
| UI-11 | [pages/08-sales.md](pages/08-sales.md) | `/sales/operations` | 08 | ⚡占位 |
| UI-12 | [pages/09-order-overview.md](pages/09-order-overview.md) | `/procurement/overview` | 09 | 🟡设计中 |
| — | [05-UI改版方案-橙色商业主题.md](05-UI改版方案-橙色商业主题.md) | — | — | ✅ |
| — | [ARCHITECTURE.md](ARCHITECTURE.md) | — | — | ✅ |

---

## 侧边栏路由

| 步号 | 路由 | 组件 | 页面文档 |
|------|------|------|---------|
| — | `/login` | LoginPage.vue | UI-01 |
| — | `/dashboard` | DashboardPage.vue | UI-02 |
| 01 | `/procurement/demand` | DemandPage.vue | UI-06 |
| 02 | `/procurement/order` | OrderPage.vue | UI-04 |
| 03 | `/procurement/inspection` | InspectionPage.vue | UI-07 |
| 04 | `/procurement/logistics` | LogisticsPage.vue | UI-05 |
| 05 | `/customs/domestic` | DomesticCustomsPage.vue | UI-08 ⚡占位 |
| 06 | `/customs/japan` | JapanCustomsPage.vue | UI-09 ⚡占位 |
| 07 | `/finance/tax-refund` | TaxRefundPage.vue | UI-10 ⚡占位 |
| 08 | `/sales/operations` | SalesOperationsPage.vue | UI-11 ⚡占位 |
| 09 | `/procurement/overview` | OrderOverviewPage.vue | UI-12 🟡设计中 |

---

## 业务步号映射（三目录严格对应）

| 步号 | 业务文档 | UI文档 | 数据库文档 |
|------|---------|--------|-----------|
| 01 | B-01 补货需求 | UI-06 01-demand | DB-01 procurement-demand |
| 02 | B-02 发注单+工厂 | UI-04 02-procurement | DB-02 procurement-order |
| 03 | B-03 验货记录 | UI-07 03-inspection | DB-03 inspection |
| 04 | B-04 调配计划 | UI-05 04-logistics | DB-04 logistics |
| 05 | B-05 国内报关 | UI-08 05-domestic-customs | DB-05 domestic-customs |
| 06 | B-06 日本清关 | UI-09 06-japan-customs | DB-06 japan-customs |
| 07 | B-07 退税 | UI-10 07-tax-refund | DB-07 tax-refund |
| 08 | B-08 运营销售 | UI-11 08-sales | DB-08 sales |
| 09 | B-09 订单总览 | UI-12 09-order-overview | DB-09 order-overview |
