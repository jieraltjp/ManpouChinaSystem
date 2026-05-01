# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 项目概览

**ManpouChinaSystem** — 跨境贸易发注管理系统（满铺中国），包含发注、仓储、报关、物流、财务等模块。Phase 0 采用单体架构（manpou-allinone），未来按 Kafka Topic 边界拆分为微服务。

**技术栈**：Java 21 / Spring Boot 3 / JPA / MySQL 8 / Vue 3 + TypeScript / Kafka / Redis / Nacos

---

## 常用命令

### 后端（manpou-allinone — 当前主力服务）

```bash
# 编译
cd apps/manpou-allinone && mvn compile

# 打包（跳过测试）
cd apps/manpou-allinone && mvn package -DskipTests

# 启动（开发端口 18090）
cd apps/manpou-allinone && mvn spring-boot:run

# 运行单个测试
mvn test -Dtest=ProcurementUseCaseTest

# 构建依赖项（首次或依赖变更后）
cd apps/manpou-allinone && mvn dependency:resolve

# 全量编译（父 pom）
mvn compile -pl libs/manpou-common,apps/manpou-allinone -am
```

### 前端

```bash
cd apps/web

# 开发服务器（端口 13000）
npm run dev

# 构建（生产环境）
npm run build

# TypeScript 类型检查（必须通过）
npm run type-check

# ESLint 检查
npm run lint
```

### 脚本（Windows）

```bash
# 一键启动全部服务
scripts\start-all.bat all

# 重启（停止 + 启动）
scripts\restart-all.bat all

# 查询状态
scripts\start-all.bat status
```

---

## 架构

### Phase 0 — 单体优先

当前主力是 `apps/manpou-allinone`（端口 18090），包含 8 个领域模块：

```
procurement（发注） · factory（工厂） · qc（验货） · logistics（物流）
replenishment（补货需求） · product（商品） · customs（报关） · finance（财务）
```

各独立微服务（user-service 等）已存在但当前开发集中在 allinone。

### DDD 分层结构（每个领域模块一致）

```
interfaces/controller/   ← HTTP 适配器（Spring MVC）
application/usecase/    ← 用例（编排领域逻辑）
application/dto/        ← 数据传输对象（Command / Query / VO）
domain/model/           ← 实体、值对象、领域服务
domain/repository/      ← 仓储接口（Port）
infrastructure/persistence/jpa/  ← JPA 实现（Adapter）
```

**六边形原则**：domain 层只引用 `common/` 下的枚举和异常，禁止跨领域直接调用。

### 前端架构

```
apps/web/src/
├── api/           ← 按领域拆分的 Axios 客户端（/api/v1/xxx）
├── pages/         ← 路由页面，按领域目录组织
├── locales/       ← zh.json / ja.json（双语 i18n）
├── stores/        ← Pinia 状态（当前仅 auth）
├── composables/   ← Vue Composables（useOrderOverview, usePermission）
└── router/        ← 路由定义 + 认证守卫
```

### API 响应格式

统一使用 `Result<T>` 包装：
```json
{ "code": 200, "data": { ... }, "message": "ok" }
{ "code": 400, "data": null, "message": "业务异常描述" }
```

前端访问响应数据时应防御性取值：`data?.content ?? []`

---

## 关键设计

### JWT RS256 认证

- 登录：`POST /api/v1/auth/login` → 返回 JWT Access Token
- 公钥：`GET /api/v1/auth/public-key` → 前端验签用
- Token 注入：所有 API 请求自动在 `Authorization: Bearer <token>` 头部注入
- 401 响应：前端自动跳转登录页

### W3C Trace Context 全链路追踪

- 请求头 `traceparent` 透传（或自动生成 traceId）
- 日志 MDC 中打印 `traceId`
- 响应头返回 `X-Trace-Id`

### 数据库迁移

- 开发期：JPA `ddl-auto: update`（H2 内存数据库）
- 生产：Flyway 迁移脚本（`db/migration/V*.sql`）
- **禁止 Flyway 版本号重编号**，冲突时立即修正

### i18n 规范

- `zh.json` / `ja.json` 均为 50KB 级大型文件，用专用编辑器打开防止重复 key
- 前端标签必须使用 `$t()`，禁止直接显示枚举原始值
- 新增 key 时同步在 zh 和 ja 两侧添加

---

## 项目规范（铁律）

**写代码前必查** `docs/lessons/` 是否有相关 lesson。

### 后端铁律

| # | 规则 |
|---|------|
| 1 | 跨模块走 Port 接口，domain model 只引用 common.enums/exception |
| 3 | JPA Repository 继承链需显式 `@Qualifier` 指定 bean |
| 4 | BusinessException 添加前搜索全项目确认 code 不冲突 |
| 34 | 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步修改 |
| 38 | 业务逻辑入口处校验，零值/空值必须防御 |

### 前端铁律

| # | 规则 |
|---|------|
| 40 | `vue-tsc --noEmit` 必须通过（strict + noUnusedLocals） |
| 44 | i18n JSON 大型文件用专用编辑器，防重复 key |
| 52 | 样式修复后立即 commit，禁止与代码修复分开提交 |
| 54 | 多文件样式修复必须用 grep 全局扫描受影响文件 |

---

## 文档导航

| 需求 | 文档 |
|------|------|
| 项目全局概览 | `docs/pro/00-root-project.md` |
| manpou-allinone 架构 | `docs/pro/19-manpou-allinone.md` |
| 领域模型设计 | `docs/business/DOMAIN-发注管理领域模型.md` |
| 数据库设计 | `docs/database/DB-发注管理数据库设计-步骤1-4.md` |
| API 文档 | `docs/business/API-发注管理.md` |
| 前端 UI 文档 | `docs/ui/README.md` |
| 测试策略 | `docs/test/TEST-STRATEGY.md` |
| 经验教训库 | `docs/lessons/README.md` |
| CI/CD 流程 | `docs/pro/15-ci-cd.md` |

---

## 服务端口

| 服务 | 端口 |
|------|------|
| manpou-allinone（后端单体） | 18090 |
| user-service | 18081 |
| api-gateway | 18080 |
| web（前端） | 13000 |
| MySQL（Docker） | 23306 |

---

## 注意事项

- Lombok + MapStruct 共用 `@Mapper(componentModel = "spring")`，编译时生成实现类
- 所有领域模块的 `KeyManagementService` 必须用 `@Service("xxxKeyManagementService")` 显式命名，否则 Spring Bean 冲突
- 前端 `npm run build` 后需检查产物时间戳，确认是最新构建再部署
- Git 提交前先运行 `mvn compile` 和 `npm run type-check`，确保无编译错误
