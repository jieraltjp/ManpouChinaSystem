# 前端 UI 文档

> ManpouChinaSystem 前端页面文档 — 截图、设计说明、组件对应

---

## 文档索引

| 编号 | 文档 | 页面路径 | 说明 |
|------|------|---------|------|
| — | [ARCHITECTURE](ARCHITECTURE.md) | — | 系统架构图（前端视角） |
| — | [05-UI改版方案-橙色商业主题](05-UI改版方案-橙色商业主题.md) | — | 橙色商业主题设计系统 |
| 01 | [pages/01-login.md](pages/01-login.md) | `/login` | MANPOU 登录 |
| 02 | [pages/02-dashboard.md](pages/02-dashboard.md) | `/dashboard` | JWT 状态 + 快捷入口 |
| 03 | [pages/03-examples.md](pages/03-examples.md) | `/examples` | 基础 CRUD 演示 |
| 04 | [pages/04-procurement.md](pages/04-procurement.md) | `/procurement/order` | 发注单管理 |
| 05 | [pages/05-demand.md](pages/05-demand.md) | `/procurement/demand` | 补货需求（步骤1） |
| 06 | [pages/06-inspection.md](pages/06-inspection.md) | `/procurement/inspection` | 验货记录（步骤3） |
| 07 | [pages/07-domestic-customs.md](pages/07-domestic-customs.md) | `/customs/domestic` | 国内报关（步骤5）⚡占位 |
| 08 | [pages/08-japan-customs.md](pages/08-japan-customs.md) | `/customs/japan` | 日本清关（步骤6）⚡占位 |
| 09 | [pages/09-tax-refund.md](pages/09-tax-refund.md) | `/finance/tax-refund` | 退税（步骤7）⚡占位 |
| 10 | [pages/10-sales.md](pages/10-sales.md) | `/sales/operations` | 运营销售（步骤8）⚡占位 |
| 11 | [pages/11-order-overview.md](pages/11-order-overview.md) | `/procurement/overview` | **订单总览**（核心视图） |
| — | [06-发注管理UI设计-步骤1-4](06-发注管理UI设计-步骤1-4.md) | — | 发注管理全链路UI设计（历史文档） |

### 路由总览

| 路由 | 组件 | 步骤 | 状态 |
|------|------|------|------|
| `/procurement/demand` | DemandPage.vue | 步骤1 | ✅ 已实现 |
| `/procurement/order` | OrderPage.vue | 步骤2 | ✅ 已实现 |
| `/procurement/inspection` | InspectionPage.vue | 步骤3 | ✅ 已实现 |
| `/procurement/logistics` | LogisticsPage.vue | 步骤4 | ✅ 已实现 |
| `/customs/domestic` | DomesticCustomsPage.vue | 步骤5 | ⚡占位 |
| `/customs/japan` | JapanCustomsPage.vue | 步骤6 | ⚡占位 |
| `/finance/tax-refund` | TaxRefundPage.vue | 步骤7 | ⚡占位 |
| `/sales/operations` | SalesOperationsPage.vue | 步骤8 | ⚡占位 |
| `/procurement/overview` | OrderOverviewPage.vue | 核心视图 | 🟡设计中 |

---

## 截图存放

截图统一放在 `docs/ui/screenshots/`，文件名与页面文档对应：

| 页面 | 截图文件 |
|------|---------|
| 登录页 | `01-login.png` |
| 仪表盘 | `02-dashboard.png` |
| 示例列表 | `03-examples.png` |
| 采购单管理 | `04-procurement.png` |
| 侧边栏（展开） | `layout-sidebar-expanded.png` |
| 侧边栏（收起） | `layout-sidebar-collapsed.png` |

> **截图方法**：启动前端 `cd apps/web && npm run dev`，浏览器打开对应路径，按 `F12` 截取或用系统截图工具。

---

## 路由速查

```
/login                       → LoginPage.vue         → 无需认证
/dashboard                   → DashboardPage.vue     → 需要认证
/examples                    → ExamplesPage.vue      → 需要认证
/procurement/demand          → DemandPage.vue        → 需要认证（补货需求）
/procurement/order           → OrderPage.vue         → 需要认证（发注单）
/procurement/inspection      → InspectionPage.vue    → 需要认证（验货记录）
/procurement/logistics       → LogisticsPage.vue     → 需要认证（调配计划）
```

---

## 布局结构

```
┌────────────┬─────────────────────────────────┐
│            │  [折叠按钮]     [用户名] [退出] │
│  MANPOU   ├─────────────────────────────────┤
│  ────────  │                                 │
│  仪表盘    │         <RouterView />           │
│  示例列表  │                                 │
│  补货需求  │                                 │
│  发注单    │                                 │
│  验货记录  │                                 │
│  调配计划  │                                 │
└────────────┴─────────────────────────────────┘
侧边栏 220px   顶栏 60px   内容区 #f5f7fa
```

---

*相关文档：[docs/pro/11-web-frontend.md](../pro/11-web-frontend.md)*
