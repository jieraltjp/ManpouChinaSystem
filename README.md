# ManpouChinaSystem

> 跨境贸易发注管理系统（满铺中国）
>
> 更新时间：2026-05-24

包含发注、仓储、报关、物流、财务等模块的跨境贸易管理平台。

---

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端 | Java 21 + Spring Boot 3 | 3.2.5 |
| 数据库 | MySQL 8（开发）/ TiDB v8（生产） | 8.0 |
| 缓存 | Redis | 7 |
| 消息队列 | Apache Kafka | 3.8 |
| 配置中心 | Nacos | 2.3 |
| 前端 | Vue 3 + TypeScript | 3.4 |
| 构建工具 | Vite | 5.x |

---

## 项目结构

```
ManpouChinaSystem/
├── apps/
│   ├── java-service/          # 父 POM（artifactId: parent）
│   ├── manpou-allinone/      # 单体应用（端口 18090）← 当前主力
│   ├── user-service/         # 用户服务（端口 18081）
│   ├── product-service/       # ⚠ 有 pom，无源码
│   ├── api-gateway/           # ⚠ 有 pom，无源码
│   └── web/                  # Vue 3 前端（端口 13000）
├── libs/
│   └── manpou-common/        # 共享库（Result/BaseEntity/ConfigSource/JWT）
├── config/checkstyle/
├── docker/
├── docs/
│   ├── pro/                  # 项目专属文档（SPEC-B01~B13）
│   ├── ui/pages/             # 前端 UI 页面设计（01~18）
│   ├── lessons/              # 工程教训库（93 条 lesson）
│   ├── database/
│   ├── business/
│   └── permission/
└── scripts/
```

---

## Phase 0 — 单体架构（当前）

主力服务是 `manpou-allinone`（端口 18090），包含 12 个领域模块：

```
procurement（发注） · factory（工厂） · qc（验货） · logistics（物流）
replenishment（补货） · product（商品） · customs（报关） · finance（财务）
notification（通知） · sales（销售） · warehouse（仓储） · order（概览）
```

### manpou-allinone DDD 分层

每个领域模块遵循六边形架构：

```
interfaces/controller/   ← HTTP 适配器（Spring MVC）
application/usecase/     ← 用例（编排领域逻辑）
application/dto/         ← Command / Query / VO
application/assembler/   ← MapStruct Assembler
domain/model/           ← 实体、值对象、领域服务
domain/repository/      ← 仓储接口（Port）
infrastructure/persistence/jpa/  ← JPA 实现（Adapter）
```

---

## 服务端口

| 服务 | 端口 | 数据库 |
|------|------|--------|
| manpou-allinone | 18090 | `manpou` |
| user-service | 18081 | `user_service` |
| api-gateway | 18080 | — |
| web | 13000 | — |
| MySQL（Docker） | 23306 | — |

---

## 快速开始

### 启动全部服务（Windows）

```bash
scripts\restart-all.bat all
```

### 单服务控制

```bash
scripts\start-all.bat manpou   # 后端（18090）
scripts\start-all.bat user      # 用户服务（18081）
scripts\start-all.bat web       # 前端（13000）
scripts\start-all.bat status    # 查询状态
```

### 编译

```bash
# allinone
cd apps/manpou-allinone && mvn package -DskipTests

# user-service
cd apps/user-service && mvn package -DskipTests

# 前端
cd apps/web && npm run build

# manpou-common（共享库变更后必须先装）
cd libs/manpou-common && mvn install
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:13000 |
| Swagger | http://localhost:18090/swagger-ui/index.html |
| user-service | http://localhost:18081 |

---

## API 响应格式

```json
{ "code": 200, "data": { "content": [...] }, "message": "ok" }
```

前端访问：`data?.content ?? []`

### JWT 认证

- 登录：`POST /api/v1/auth/login` → JWT Access Token（user-service 签发，RS256）
- 公钥：`GET /api/v1/auth/public-key` → 前端验签
- TTL：1 天（user-service 签发，allinone 消费验证）

### 权限三角

| 层 | 机制 |
|----|------|
| 前端 | `hasPermission('xxx:read')` composable 守卫 |
| 后端 | `@PreAuthorize` 在 Controller 层 |
| 数据库 | `permission` 表 ~120 条权限记录 |

---

## 团队成员

| 角色 | 姓名 | 职责 |
|------|------|------|
| 报关负责人 | 殷元 | 统筹通关全流程 |
| 辅料与库存协同 | 徐义超 | 辅料供应、库存管理 |
| 退税与货柜管理 | 于世荣 | 退税优化、货柜效率 |
| 方案牵头人 | 张云 | 体系升级落地 |
| 财务与系统对接 | 许文豪 | 财务凭证流对接 |
| 系统协同支持 | 陈天仪 | 业务与技术衔接 |

---

## 文档导航

| 文档 | 说明 |
|------|------|
| [SPEC-B11 用户中心与权限体系](docs/business/SPEC-B11-用户中心与权限体系.md) | JWT/权限/审计日志 |
| [SPEC-B12 船只与货柜](docs/business/SPEC-B12-船只与货柜.md) | Ship/Container 模块 |
| [工程教训库](docs/lessons/README.md) | 93 条 lesson，铁律索引 |
| [UI 页面设计](docs/ui/README.md) | 18 个页面设计文档 |
| [前端文档](docs/ui/README.md) | 架构/i18n/组件规范 |

---

## 工程教训（铁律速查）

> 完整索引：[docs/lessons/README.md](docs/lessons/README.md)

| # | 场景 | 关键规则 |
|---|------|---------|
| 76 | allinone 重启流程 | 先停进程→等5秒→删JAR→重打包→启动验证 |
| 52 | dist 构建无效 | `npm run build` 后确认产物时间戳 |
| 50 | API 响应崩溃 | `data?.content ?? []` 防御性访问 |
| 44 | 日语用户乱码 | 对话框/表格列标签必须提取为 i18n key |
| 33 | Demand 新建不可见 | 业务链起点 = Overview 入口锚点 |
| 75 | 权限三角不一致 | 前端/后端/DB 必须完全一致 |
| 83 | @PreAuthorize 失效 | 必须加在 Controller 层，内部调用绕过 AOP |
| 79 | allinone 401 | JWT 密钥来源必须统一，禁止混用 |
| 89 | COS 图片 404 | TLS 握手失败先 curl -v 验证 |
| 85 | 审计日志 resourceId 为 null | `#_return` SpEL 仅支持 `Result<T>` 单值 |

---

## 开发规范

### 后端铁律

- 跨模块走 Port 接口，domain model 只用 `common.enums/exception`
- JPA Repository 继承链 → `@Qualifier` 显式指定 bean
- 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步修改
- 业务逻辑校验在入口处，零值/空值必须防御
- `@PreAuthorize` 必须加在 Controller 层

### 前端铁律

- `vue-tsc --noEmit` 必须通过（strict + noUnusedLocals）
- 后端 API 破坏性变更 = API 类型 + 所有调用方 + 测试 三处同步
- v-for 的 index 参数未使用加 `_` 前缀
- 多文件样式修复必须 grep 全局扫描
- 样式修复后立即 commit，禁止与代码修复分开提交
- i18n JSON 用专用编辑器，防重复 key

### 数据库铁律

- Flyway 版本号提前规划，禁止重编号
- 实体删除字段后必须同步 DROP DB 列
- nativeQuery + Pageable ORDER BY → Spring 追加实体属性名（非列名）
