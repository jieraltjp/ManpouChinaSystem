# 项目文档：web（前端）

> **文档角色**：前端开发工程师视角 → 用户界面
> **对应角色文档**：`docs/role/05-前端开发工程师视角分析.md`

---

## 1. 项目定位

| 维度 | 说明 |
|------|------|
| 项目名 | `@manpou/web` |
| 端口 | 13000（开发） |
| 包名 | `@manpou/web` |
| 描述 | Vue 3 + TypeScript + Element Plus 管理后台前端 |
| 当前状态 | 脚手架 ✅，页面待开发 |

---

## 2. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 框架 |
| TypeScript | 5.4+ | 类型安全 |
| Vite | 5.3+ | 构建工具 |
| Element Plus | 2.6+ | UI 组件库 |
| Pinia | 2.1+ | 状态管理 |
| Vue Router | 4.3+ | 路由管理 |
| Axios | 1.7+ | HTTP 客户端 |
| jwt-decode | 4.0+ | Token 解析 |
| dayjs | 1.11+ | 日期格式化 |

---

## 3. 项目结构

```
apps/web/
├── src/
│   ├── api/
│   │   ├── client.ts               # Axios 实例（JWT 注入 + 401 处理）
│   │   ├── adapters/
│   │   │   └── auth.ts            # 认证 API 适配器
│   │   └── types/
│   │       └── api.ts             # (废弃) 请使用 src/types/api.ts
│   ├── components/
│   │   └── atoms/
│   │       └── Loading.vue         # 原子组件
│   ├── composables/
│   │   └── usePermission.ts        # 权限 Hook
│   ├── layouts/
│   │   └── AppLayout.vue          # 页面布局
│   ├── pages/
│   │   ├── auth/
│   │   │   └── LoginPage.vue     # 登录页
│   │   └── dashboard/
│   │       ├── DashboardPage.vue  # 仪表盘
│   │       └── ExamplesPage.vue   # 示例列表
│   ├── router/
│   │   └── index.ts              # 路由 + 守卫
│   ├── stores/
│   │   └── auth.ts               # 认证状态（Pinia）
│   ├── types/
│   │   ├── api.ts                # API 通用类型
│   │   └── user.ts               # 用户类型
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts                 # Vite 配置（proxy → 18090 Phase0 / 18080 Phase1+）
├── tsconfig.json
└── package.json
```

---

## 4. 已实现功能

| 功能 | 状态 | 说明 |
|------|------|------|
| Axios 客户端 | ✅ | JWT 注入 + 401 自动登出（中文错误提示） |
| 认证 Store | ✅ | Pinia 管理 token/userInfo |
| 路由守卫 | ✅ | 未登录跳转登录页 |
| 登录页 | ✅ | 渐变背景 + 表单验证 |
| 仪表盘 | ✅ | JWT Claims 显示 + 重新登录 |
| 示例 CRUD | ✅ | 对接 user-service 示例 API |
| 发注单管理 | ✅ | `/procurement/order`（OrderPage.vue）— 完整 CRUD + 需求带入 + 工厂选择 + 筛选 + 分页 + 详情抽屉 |
| Element Plus | ✅ | 组件库引入 |

---

## 5. 页面清单

| 页面 | 路径 | 组件 | 状态 |
|------|------|------|------|
| 登录 | `/login` | LoginPage | ✅ |
| 仪表盘 | `/dashboard` | DashboardPage | ✅ |
| 示例列表 | `/examples` | ExamplesPage | ✅ |
| 补货需求 | `/procurement/demand` | DemandPage | ✅ |
| 发注单管理 | `/procurement/order` | OrderPage | ✅ |
| 验货记录 | `/procurement/inspection` | InspectionPage | ✅ |
| 调配计划 | `/procurement/logistics` | LogisticsPage | ✅ |
| 仓储列表 | `/warehouse` | WarehouseListPage | 🔴 待实现 |
| 报关管理 | `/customs` | CustomsListPage | 🔴 待实现 |
| 物流管理 | `/logistics` | LogisticsListPage | 🔴 待实现 |
| 财务管理 | `/finance` | FinanceListPage | 🔴 待实现 |
| 商品管理 | `/product` | ProductListPage | 🔴 待实现 |
| 用户管理 | `/user` | UserListPage | 🔴 待实现 |

> 发注管理 4 个子页面：`/procurement/demand`（补货需求）、`/procurement/order`（发注单）、`/procurement/inspection`（验货记录）、`/procurement/logistics`（调配计划），均已完整实现并对接真实 API。

---

## 6. API 集成状态

### 6.1 已实现

| 接口 | 方法 | 路径 | 服务 |
|------|------|------|------|
| 登录 | POST | /api/v1/auth/login | user-service |
| 公钥获取 | GET | /api/v1/auth/public-key | user-service |
| 密钥管理 | GET/POST | /api/v1/admin/keys | user-service |
| 示例 CRUD | GET/POST/PUT/DELETE | /api/v1/examples | user-service |
| 发注单列表 | GET | /api/v1/procurements | manpou-allinone |
| 发注单详情 | GET | /api/v1/procurements/{id} | manpou-allinone |
| 创建发注单 | POST | /api/v1/procurements | manpou-allinone |
| 更新发注单 | PATCH | /api/v1/procurements/{id} | manpou-allinone |
| 删除发注单 | DELETE | /api/v1/procurements/{id} | manpou-allinone |

### 6.2 待实现（采购单）

| 接口 | 方法 | 路径 | 页面 |
|------|------|------|------|
| 采购单列表 | GET | /api/v1/procurements | procurement-list |
| 采购单详情 | GET | /api/v1/procurements/{id} | procurement-detail |
| 新建采购单 | POST | /api/v1/procurements | procurement-form |
| 提交采购单 | POST | /api/v1/procurements/{id}/submit | procurement-list |
| 审批采购单 | POST | /api/v1/procurements/{id}/approve | procurement-list |

---

## 7. Vite 代理配置

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:18090',  // Phase 0: manpou-allinone
      // Phase 1+: 改为 18080 (api-gateway)，由网关统一路由
      changeOrigin: true,
    },
  },
},
```

> **注意**：Phase 0 指向 manpou-allinone（18090），Phase 1+ 改为 18080（api-gateway）。

---

## 8. 构建与运行

```bash
# 安装依赖
cd apps/web
npm install

# 开发模式
npm run dev    # http://localhost:13000

# 类型检查
npm run type-check

# 代码检查
npm run lint

# 生产构建
npm run build
```

---

## 9. 组件规范

| 层级 | 目录 | 说明 |
|------|------|------|
| 原子组件 | `components/atoms/` | Button, Input, Loading（无业务逻辑） |
| 分子组件 | `components/molecules/` | Form, Card（组合原子组件） |
| 有机体 | `components/organisms/` | Header, Sidebar（完整功能区块） |
| 页面 | `pages/` | 路由页面（编排组件） |

---

## 10. 行动项

- [ ] **本周**：理解现有项目结构（API 客户端/Store/路由/布局）
- [ ] **本周**：理解 JWT 认证流程（登录→Token→请求拦截→401处理）
- [ ] **本周**：搭建采购单列表页面框架
- [ ] **下周二**：实现采购单列表 API 集成（GET /api/v1/procurements）
- [ ] **下周三**：实现新建采购单页面（POST）
- [ ] **持续**：所有 API 调用通过 api/adapters/ 适配
- [ ] **持续**：后端字段必须通过 Assembler 转换为前端 ViewModel

---

## 11. 前端 UI 文档

| 文档 | 说明 |
|------|------|
| [docs/ui/README.md](../ui/README.md) | 前端页面文档入口 |
| [docs/ui/ARCHITECTURE.md](../ui/ARCHITECTURE.md) | 系统架构图（Mermaid） |
| [docs/ui/pages/01-login.md](../ui/pages/01-login.md) | 登录页 |
| [docs/ui/pages/02-dashboard.md](../ui/pages/02-dashboard.md) | 仪表盘 |
| [docs/ui/pages/03-examples.md](../ui/pages/03-examples.md) | 示例列表 |
| [docs/ui/pages/04-logistics.md](../ui/pages/04-logistics.md) | 调配计划 |

---

## 12. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/05-前端开发工程师视角分析.md` | 前端开发规范与 Action Items |
| `docs/pro/00-root-project.md` | 项目全局概览 |
