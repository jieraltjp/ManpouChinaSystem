# ManpouChinaSystem

> MANPOU 企业管理系统

跨境贸易发注管理系统，包含发注、仓储、报关、物流、财务等模块。

## 项目结构

```
ManpouChinaSystem/
├── apps/
│   ├── java-service/           # 父POM（仅pom.xml，无源码）
│   ├── manpou-allinone/        # 6领域合一单体（端口18090）
│   ├── user-service/            # 用户服务（端口18081）
│   ├── product-service/         # 商品服务（端口18082）
│   ├── procurement-service/     # 发注服务（端口18083）
│   ├── warehouse-service/       # 仓储服务（端口18084）
│   ├── customs-service/         # 报关服务（端口18085）
│   ├── logistics-service/       # 物流服务（端口18086）
│   ├── finance-service/         # 财务服务（端口18087）
│   ├── notification-service/   # 通知服务（端口18088）
│   ├── api-gateway/             # API网关（端口18080）
│   └── web/                     # Vue3前端（端口13000）
├── config/
│   ├── checkstyle/
│   └── nacos/
├── docker/
│   ├── compose.yaml
│   ├── otel-collector-config.yaml
│   ├── tempo-config.yaml
│   └── prometheus/
│       └── prometheus.yml
├── scripts/
│   ├── build-all.sh
│   ├── init-config.sh
│   ├── restart-all.sh
│   ├── start-all.sh
│   └── stop-all.sh
├── infra/
│   └── helm/
├── monitoring/
│   ├── alertmanager/
│   ├── grafana/
│   └── prometheus/
├── docs/
│   ├── index.md              # 文档导航
│   ├── pro/                  # 项目专属文档
│   ├── design/               # 构建指南
│   ├── role/                 # 角色视角
│   ├── ui/                   # 前端UI（含橙色主题方案）
│   └── check/                # 审计报告
├── .github/
│   └── workflows/
│       ├── ci.yaml
│       └── cd.yml
├── .gitignore
├── pom.xml
├── README.md
└── versions.toml
```

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot 3 | 3.2.5 |
| 数据库 | MySQL 8（开发）/ TiDB v8（生产） | 8.0 |
| 缓存 | Redis | 7 |
| 消息队列 | Apache Kafka | 3.8 |
| 配置中心 | Nacos | 2.3 |
| 前端 | Vue 3 + TypeScript | 3.4 |
| 构建工具 | Vite | 5.x |
| 包管理 | npm | — |

## 快速开始（Phase 0 — 三服务）

Phase 0 使用单体架构，所需服务数量最少，一键启动。

### 启动全部服务

```bash
# Windows（推荐）
scripts\restart-all.bat all

# 或分步：停止 → 启动
scripts\stop-all.bat all
scripts\start-all.bat all
```

### 单服务控制

```bash
scripts\start-all.bat manpou   # 仅启动后端（端口 18090）
scripts\start-all.bat user      # 仅启动用户服务（端口 18081）
scripts\start-all.bat web       # 仅启动前端（端口 13000）
scripts\start-all.bat status    # 查询运行状态
```

### 服务访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:13000 |
| Swagger UI | http://localhost:18090/swagger-ui/index.html |
| user-service API | http://localhost:18081 |

### 编译单个服务

```bash
cd apps/manpou-allinone
mvn package -DskipTests -q -Drevision=1.0.0

cd apps/user-service
mvn package -DskipTests -q -Drevision=1.0.0
```

## 团队成员

| 角色 | 姓名 | 职责 |
|------|------|------|
| 报关负责人 | 殷元 | 统筹通关全流程 |
| 辅料与库存协同 | 徐义超 | 辅料供应、库存管理 |
| 退税与货柜管理 | 于世荣 | 退税优化、货柜效率 |
| 方案牵头人 | 张云 | 体系升级落地 |
| 财务与系统对接 | 许文豪 | 财务凭证流对接 |
| 系统协同支持 | 陈天仪 | 业务与技术衔接 |

## 文档

| 文档 | 说明 |
|------|------|
| [01-项目规划与架构设计](docs/design/01-项目规划与架构设计.md) | 限界上下文、领域模型 |
| [02-环境准备](docs/design/02-环境准备.md) | 开发环境配置 |
| [03-后端服务生成](docs/design/03-后端服务生成.md) | 微服务生成 |
| [04-前端项目生成](docs/design/04-前端项目生成.md) | 前端配置 |
| [05-领域模块开发](docs/design/05-领域模块开发.md) | 业务模块开发 |
| [06-配置与部署](docs/design/06-配置与部署.md) | Docker、CI/CD |

---

架构委员会 | arch@manpou.com
