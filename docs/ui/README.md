# 前端 UI 文档

> ManpouChinaSystem 前端页面文档 — 截图、设计说明、组件对应

---

## 文档索引

| 文档 | 页面路径 | 说明 |
|------|---------|------|
| [ARCHITECTURE](ARCHITECTURE.md) | — | 系统架构图（前端视角） |
| [05-UI改版方案-橙色商业主题](05-UI改版方案-橙色商业主题.md) | — | 橙色商业主题设计系统（CSS 变量 + 配色 + 组件规范） |
| [01-登录页](pages/01-login.md) | `/login` | MANPOU 登录 |
| [02-仪表盘](pages/02-dashboard.md) | `/dashboard` | JWT 状态 + 快捷入口 |
| [03-示例列表](pages/03-examples.md) | `/examples` | 基础 CRUD 演示 |
| [04-采购单管理](pages/04-procurement.md) | `/procurement/order` | 发注单管理（OrderPage.vue） |
| [06-发注管理UI设计-步骤1-4](06-发注管理UI设计-步骤1-4.md) | — | 发注管理全链路UI设计（补货/发注单/工厂/验货/调配） |

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
/login          → LoginPage.vue          → 无需认证
/dashboard      → DashboardPage.vue      → 需要认证
/examples       → ExamplesPage.vue       → 需要认证
/procurement/order → OrderPage.vue      → 需要认证（发注单管理）
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
│  采购单管理│                                 │
└────────────┴─────────────────────────────────┘
侧边栏 220px   顶栏 60px   内容区 #f5f7fa
```

---

*相关文档：[docs/pro/11-web-frontend.md](../pro/11-web-frontend.md)*
