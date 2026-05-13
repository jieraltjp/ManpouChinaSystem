# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 项目概览

**ManpouChinaSystem** — 跨境贸易发注管理系统（满铺中国），包含发注、仓储、报关、物流、财务等模块。Phase 0 采用单体架构（manpou-allinone），未来按 Kafka Topic 边界拆分为微服务。

**技术栈**：Java 21 / Spring Boot 3 / JPA / MySQL 8 / Vue 3 + TypeScript / Kafka / Redis / Nacos

---

## Maven 模块结构

```
ManpouChinaSystem/
├── apps/
│   ├── java-service/          # 父 POM（artifactId: parent），所有服务的父级
│   ├── manpou-allinone/      # 单体应用（端口 18090），当前主力服务
│   ├── user-service/         # 用户服务（端口 18081）
│   ├── product-service/       # 商品服务（端口 18082）⚠ 有 pom 无源码
│   ├── api-gateway/           # API 网关（端口 18080）⚠ 有 pom 无源码
│   └── web/                  # Vue 3 前端（端口 13000）
├── libs/
│   └── manpou-common/        # 共享库（Result/BaseEntity/ConfigSource/JWT 工具）
└── config/
    └── checkstyle/           # CheckStyle 规则
```

**模块继承关系**：
- 所有 `apps/*-service` 的 parent 为 `apps/java-service/pom.xml`（artifactId: `parent`）
- `manpou-common` 是独立 library，需先 `mvn install -pl libs/manpou-common` 再编译依赖它的服务

**前端 Vite Proxy 配置**（`apps/web/vite.config.ts`）：
- `/api/v1/auth`、`/api/v1/users`、`/api/v1/roles`、`/api/v1/permissions`、`/api/v1/audit-logs` → `http://localhost:18081`（user-service）
- `/api` → `http://localhost:18090`（manpou-allinone）

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
cd apps/manpou-allinone && mvn test -Dtest=ProcurementUseCaseTest

# 安装 manpou-common（共享库变更后必须执行）
cd libs/manpou-common && mvn install
```

### user-service

```bash
cd apps/user-service && mvn spring-boot:run
```

### 前端

```bash
cd apps/web

# 开发服务器（端口 13000）
npm run dev

# 生产构建
npm run build

# TypeScript 类型检查（必须通过）
npm run type-check

# ESLint
npm run lint
```

### 脚本（Windows）

```bash
# 一键启动全部服务
scripts\start-all.bat all

# 重启
scripts\restart-all.bat all

# 查询状态
scripts\start-all.bat status
```

---

## 架构

### Phase 0 — 单体优先

当前主力是 `apps/manpou-allinone`（端口 18090），包含 12 个领域模块：

```
procurement（发注） · factory（工厂） · qc（验货） · logistics（物流）
replenishment（补货需求） · product（商品） · customs（报关） · finance（财务）
notification（通知） · sales（销售） · warehouse（仓储） · order（订单/概览）
```

### manpou-allinone DDD 分层结构

每个领域模块内部遵循六边形架构：

```
interfaces/controller/   ← HTTP 适配器（Spring MVC）
application/usecase/     ← 用例（编排领域逻辑）
application/dto/         ← Command / Query / VO
application/assembler/   ← MapStruct Assembler（Entity ↔ DTO 互转）
domain/model/           ← 实体、值对象、领域服务
domain/repository/      ← 仓储接口（Port）
domain/event/           ← 领域事件
infrastructure/persistence/jpa/  ← JPA 实现（Adapter）
```

**六边形原则**：domain 层只引用 `libs/manpou-common` 下的枚举和异常，禁止跨领域直接调用。

### user-service DDD 结构

```
interfaces/controller/   ← REST API
application/service/     ← Service（编排领域逻辑）
application/dto/         ← Command / Query / VO
domain/model/           ← 实体（User/Role/Permission/AuditLog）
domain/repository/      ← JPA Repository
domain/port/            ← Port 接口
infrastructure/security/ ← JWT 认证/鉴权
infrastructure/aspect/  ← 幂等性切面
```

### 前端架构

```
apps/web/src/
├── api/           ← Axios 客户端，按领域拆分（/api/v1/xxx）
├── pages/         ← 路由页面，按领域目录组织
├── layouts/       ← 布局组件（AppLayout.vue 硬编码菜单栏）
├── locales/       ← zh.json / ja.json（双语 i18n）
├── stores/        ← Pinia 状态（当前仅 auth）
├── composables/   ← Vue Composables（usePermission, useAvatarCompress, useOrderOverview）
└── router/        ← 路由定义 + 认证守卫
```

---

## 关键设计

### API 响应格式

统一使用 `Result<T>` 包装：

```json
{ "code": 200, "data": { ... }, "message": "ok" }
{ "code": 400, "data": null, "message": "业务异常描述" }
```

前端访问：`data?.content ?? []`

### JWT RS256 认证

- 登录：`POST /api/v1/auth/login` → 返回 JWT Access Token（user-service 签发）
- 公钥：`GET /api/v1/auth/public-key` → 前端验签用
- Token 注入：所有 API 请求自动在 `Authorization: Bearer <token>` 头部注入
- 401 响应：前端自动跳转登录页
- **TTL**：user-service 签发 86400s（1 天）；allinone 消费验证（verifier endpoint 轮询公钥，刷新间隔 300s）

### 权限控制三角

| 层 | 机制 |
|----|------|
| 前端 | `hasPermission('xxx:read')` composable 守卫 |
| 后端 | `@PreAuthorize` 在 Controller 层 |
| 数据库 | `permission` 表 78 条权限记录（user-service V15） |

### 数据库迁移

| 服务 | 数据库 | Flyway |
|------|--------|--------|
| manpou-allinone | `manpou`（JPA `ddl-auto: update` 开发，生产 Flyway） | V15~V18（baseline 32 表 + 业务表） |
| user-service | `user_service`（`ddl-auto: none`，生产 Flyway） | V2（用户头像字段扩展） |

- allinone Flyway 脚本：`V15__baseline_schema.sql`（基线 32 表 + 种子数据）→ V16（发注快照）→ V17/V18（业务扩展）
- user-service 独立数据库，独立 Flyway，baseline-on-migrate: true
- **禁止 Flyway 版本号重编号**

### i18n 规范

- `zh.json` / `ja.json` 均为大型 JSON 文件，用专用编辑器防止重复 key
- 前端标签必须使用 `$t()`，禁止直接显示枚举原始值
- 新增 key 时同步在 zh 和 ja 两侧添加

### 测试策略

- 单元测试：`mvn test`（跳过 Testcontainers）
- 集成测试：`mvn test -Dtest=*IntegrationTest`（需要 Docker）
- 测试框架：JUnit 5 + Mockito + AssertJ + Testcontainers

### 质量门禁

- CheckStyle：编码规范（配置文件在 `config/checkstyle/`）
- ArchUnit：架构验证（DDD 分层约束）
- OWASP Dependency Check：`mvn dependency-check:check`

---

## 服务端口

| 服务 | 端口 | 数据库 | 说明 |
|------|------|--------|------|
| manpou-allinone | 18090 | `manpou` | 单体后端（当前主力） |
| user-service | 18081 | `user_service` | 用户/角色/权限/AuditLog |
| api-gateway | 18080 | — | API 网关（⚠ 有 pom 无源码） |
| web | 13000 | — | Vue 3 前端 |
| MySQL（Docker） | 23306 | — | 开发数据库 |

---

## 项目规范（铁律）

**写代码前必查** `docs/lessons/README.md` 是否有相关 lesson。

### 后端铁律

| # | 规则 |
|---|------|
| 1 | 跨模块走 Port 接口，domain model 只引用 common.enums/exception |
| 2 | 领域模型禁止直接引用其他模块 Entity/VO |
| 3 | JPA Repository 继承链需显式 `@Qualifier` 指定 bean |
| 4 | BusinessException 添加前搜索全项目确认 code 不冲突 |
| 25 | JPA Domain Repository 禁止加 `@Repository` |
| 34 | 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步修改 |
| 38 | 业务逻辑入口处校验，零值/空值必须防御 |
| 76 | allinone 代码变更后：先停进程→等5秒→删JAR→重打包→启动验证 |
| 83 | `@PreAuthorize` 必须加在 Controller 层，内部方法调用绕过 AOP 代理 |
| 85 | SpEL `#_return` 仅支持 `Result<T>` 单值，不支持 `ResponseEntity/Result<List>` |

### 前端铁律

| # | 规则 |
|---|------|
| 40 | `vue-tsc --noEmit` 必须通过（strict + noUnusedLocals） |
| 41 | 前端 API 签名变更后所有调用方必须同步 |
| 42 | v-for 的 index 参数未使用时必须加 `_` 前缀 |
| 44 | i18n JSON 大型文件用专用编辑器，防重复 key |
| 50 | API 响应必须防御性取值 `data?.content ?? []` |
| 52 | 样式修复后立即 commit，禁止与代码修复分开提交 |
| 54 | 多文件样式修复必须用 grep 全局扫描受影响文件 |

### 安全铁律

| # | 规则 |
|---|------|
| 75 | JWT 新增 claim 必须两端同步；新增 Controller 必须加 `@PreAuthorize` |
| 78 | 权限三角（前端/后端/DB）必须完全一致 |
| 79 | JWT 密钥来源必须统一——env var > classpath > DB，禁止混用 |
| 81 | allinone 跨服务写 AuditLog 时需设置 `secret` 校验，且要写对数据库 |
| 82 | `UserContext.getUsername()` 接口必须实现，内部服务调用传 operatorName |

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
| SPEC-B11 用户中心 | `docs/business/SPEC-B11-用户中心与权限体系.md` |
| SPEC-B12 船只与货柜 | `docs/business/SPEC-B12-船只与货柜.md` |
| 权限与日志 | `docs/permission/README.md` |

---

## 注意事项

- **AppLayout.vue 菜单是硬编码的**，非动态生成，新增页面必须同时修改 `AppLayout.vue` 和 `router/index.ts`
- `libs/manpou-common` 变更后需先 `mvn install -pl libs/manpou-common` 再编译依赖它的服务
- Lombok + MapStruct 共用 `@Mapper(componentModel = "spring")`，编译时生成实现类
- `@Service` Bean 名称冲突：所有领域模块的 `KeyManagementService` 必须显式命名 `@Service("xxxKeyManagementService")`
- 前端 `npm run build` 后需检查产物时间戳，确认是最新构建再部署
- Git 提交前先运行 `mvn compile` 和 `npm run type-check`，确保无编译错误
