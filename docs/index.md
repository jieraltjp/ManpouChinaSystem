# ManpouChinaSystem 文档导航

> **维护原则**：文档在哪里（文件系统）= 文档是什么（内容职责），路径即语义
> **最后更新**：2026-05-13（v1.0.0：新增 110-audit-2026-05-13.md）

---

## 总览

```
docs/
├── index.md                    ← 本文件 · 全局导航索引
├── pro/                         ← 项目专属文档（每个服务一篇）
├── design/                      ← 构建指南（Step 1-7 · 过程文档）
├── role/                        ← 角色视角分析（6 角色 · 受众文档）
├── ui/                          ← 前端 UI 文档 + 架构图
├── business/                    ← 业务 SPEC + API 文档
├── database/                    ← 数据库设计文档（表结构/ER 图）
├── lessons/                     ← 工程教训库（Lesson 1-87）
├── permission/                  ← 权限体系与 AuditLog 文档
├── ops/                         ← 运维/故障排查文档
├── test/                        ← 测试策略与用例文档
└── check/                       ← 审计报告（历史积累）
```

---

## pro/ — 项目专属文档（每个服务/模块一篇）

> **定位**：告诉开发者"这个项目是什么"，对应 `apps/` 下的每一个代码目录
> **编号约定**：01-19 = 服务，20 = 部署，17-18 = 基础设施

| 编号 | 文件 | 对应代码目录 | 核心内容 |
|------|------|------------|---------|
| 00 | `00-root-project.md` | — | 项目全局概览、端口分配、技术栈矩阵 |
| 01 | `01-java-service-parent.md` | `apps/java-service/` | Maven 父 POM、依赖版本、插件配置 |
| 02 | `02-user-service.md` | `apps/user-service/` | 用户认证、JWT RS256、权限管理 |
| 03 | `03-product-service.md` | `apps/product-service/` | 商品管理 |
| 04 | `04-warehouse-service.md` | `apps/warehouse-service/` | 仓储管理 |
| 05 | `05-procurement-service.md` | `apps/procurement-service/` | 发注管理（核心业务） |
| 06 | `06-customs-service.md` | `apps/customs-service/` | 报关管理 |
| 07 | `07-logistics-service.md` | `apps/logistics-service/` | 物流管理 |
| 08 | `08-finance-service.md` | `apps/finance-service/` | 财务管理 |
| 09 | `09-notification-service.md` | `apps/notification-service/` | 通知服务 |
| 10 | `10-api-gateway.md` | `apps/api-gateway/` | Spring Cloud Gateway、路由规则、JWT 验签 |
| 11 | `11-web-frontend.md` | `apps/web/` | Vue 3 前端、路由、状态管理 |
| 12 | `12-docker-compose.md` | `docker/` | 本地开发环境、基础设施容器化 |
| 13 | `13-helm-k8s.md` | `infra/helm/` | K8s Helm 部署模板 |
| 14 | `14-monitoring.md` | `monitoring/` | Prometheus + Grafana + Alertmanager |
| 15 | `15-ci-cd.md` | `.github/workflows/` + `scripts/` | CI/CD 流水线、自动化脚本 |
| 16 | `16-config-center.md` | `config/` | Checkstyle + Nacos 配置模板 |
| 17 | `17-服务间认证.md` | — | JWT Token 透明传递、RS256 验签机制 |
| 18 | `18-可插拔基础设施.md` | — | Docker/Redis/Kafka/Nacos 可选接入原则 |
| 19 | `19-manpou-allinone.md` | `apps/manpou-allinone/` | 12 领域合一单体、Phase 0 策略 |
| 20 | `20-ubuntu-deploy-dev.md` | — | Ubuntu 轻量部署、Systemd 服务、Phase 0 三服务 |
| — | `PRODUCTION-DEPLOY.md` | — | 生产部署指南（迁移体系、秘钥管理、监控） |

---

## design/ — 构建指南

> **定位**：告诉开发者"如何从零构建这个项目"，6 步过程文档
> **受众**：首次克隆项目的开发者

| 文件 | 阶段 | 核心内容 |
|------|------|---------|
| `README.md` | 入口 | 快速开始、技术栈索引、服务端口表 |
| `01-项目规划与架构设计.md` | Step 1 | 限界上下文、领域模型、项目规划 |
| `02-环境准备.md` | Step 2 | JDK 21、Maven、Node.js、IDE 配置 |
| `03-后端服务生成.md` | Step 3 | 微服务脚手架生成（codegen） |
| `04-前端项目生成.md` | Step 4 | Vue 3 + Vite + Element Plus 配置 |
| `05-领域模块开发.md` | Step 5 | 业务模块详细开发指南 |
| `06-配置与部署.md` | Step 6 | Docker Compose、CI/CD、Helm 部署 |
| `07-Element-Plus-表格布局规范.md` | 规范 | el-table/el-dialog 紧凑设计规范 |
| `ARCHITECTURE-Lombok-Decoupling.md` | 架构 | Lombok 与 Spring DI 解耦教训 |
| `FEATURE-货号自动补全与多子货号选择.md` | 特性 | 商品子货号自动补全交互设计 |

---

## role/ — 角色视角分析

> **定位**：告诉每个角色"你需要关心什么"，6 角色的专属视角
> **受众**：产品经理、架构师、后端/前端/测试工程师、数据库工程师

| 编号 | 文件 | 角色 | 核心关注点 |
|------|------|------|-----------|
| 01 | `01-产品经理视角分析.md` | 产品经理 | 业务需求、功能模块、优先级 |
| 02 | `02-架构师视角分析.md` | 架构师 | 技术选型、架构决策、风险评估 |
| 03 | `03-数据库工程师视角分析.md` | DBA | 表结构设计、索引策略、数据迁移 |
| 04 | `04-后端开发工程师视角分析.md` | 后端工程师 | 开发规范、API 设计、测试策略 |
| 05 | `05-前端开发工程师视角分析.md` | 前端工程师 | Vue 3 规范、组件设计、API 契约 |
| 06 | `06-测试工程师视角分析.md` | 测试工程师 | 测试用例、覆盖率目标、自动化测试 |

---

## ui/ — 前端 UI 文档

> **定位**：告诉开发者"前端页面长什么样、如何交互"
> **受众**：前端工程师、UI/UX 设计师

| 文件/目录 | 内容 |
|---------|------|
| `README.md` | UI 文档入口、页面列表 |
| `ARCHITECTURE.md` | 前端架构图（Mermaid）、组件分层 |
| `pages/01-login.md` | 登录页设计：字段、校验、OAuth 入口 |
| `pages/02-dashboard.md` | 仪表盘设计：图表、统计数据、快捷入口 |
| `pages/03-examples.md` | 示例列表页设计：CRUD 表格、分页、筛选 |
| `pages/02-procurement.md` | 发注单页面设计：流程状态、多步骤表单 |
| `screenshots/` | 页面截图存放目录 |

---

## check/ — 审计报告

> **定位**：自动生成的健康检查报告，供架构委员会 review
> **生成频率**：每次 `/codegen` 执行后自动更新

| 文件 | 周期 | 核心内容 |
|------|------|---------|
| `98-项目全貌与演进路线图.md` | 季度 | 价值分层模型、Phase 0→4 演进路线 |
| `99-全面审计报告.md` | 审计后 | 代码与文档对应核查、INTJ 量化分析 |
| `100-服务启动审计报告.md` | 审计后 | 服务启动验证、冒烟测试结果 |
| `101-docs-audit-20260420.md` | 审计后 | 文档一致性审计（v1 口径修正） |
| `103-docs-audit-20260422b.md` | 审计后 | 文档审计第二批次 |
| `104-docs-audit-20260422c.md` | 审计后 | 文档审计第三批次 |
| `105-db-audit-20260422.md` | 审计后 | 数据库迁移审计 |
| `106-db-audit-20260427.md` | 审计后 | DB 审计（dist 时间差/重复 key） |
| `107-ui-page-audit-20260427.md` | 审计后 | UI 页面审计 |
| `108-DB-AUDIT-20260427.md` | 审计后 | DB 全量审计 |
| `109-full-audit-20260501.md` | 审计后 | 全量代码审计（i18n/样式/权限/性能） |
| `110-audit-2026-05-13.md` | 审计后 | **本次全量审计**：权限100%覆盖、前端规范清理、文档一致性修正 |

---

## 快速定位

```
想了解某个服务的细节？
  → docs/pro/{编号}-{服务名}.md

想知道如何从零构建？
  → docs/design/README.md → 按 Step 1-6 顺序阅读

我是 {角色}，需要关注什么？
  → docs/role/{编号}-{角色}视角分析.md

前端页面怎么设计？
  → docs/ui/pages/{编号}-{页面}.md

文档结构是否一致？
  → docs/check/99-全面审计报告.md
```

---

## 文档变更守则

1. **新增服务文档** → 在 `docs/pro/` 按编号顺序插入，更新本索引
2. **移动文档位置** → 必须同步更新所有跨文档引用
3. **修改文档编号** → 先搜索所有 `docs/` 目录的内部链接，再执行移动
4. **新增角色视角** → 在 `docs/role/` 末尾追加，更新本索引 role/ 表格
5. **文档移动/重命名后** → 必须同步更新 `docs/index.md` 导航索引
6. **CI 中应包含断链检测 step** → 文档链接校验，发现断链应阻断合并
