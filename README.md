# ManpouChinaSystem

> 满铺中国发注管理体系

跨境贸易发注管理系统，包含发注、仓储、报关、物流、财务等模块。

## 项目结构

```
ManpouChinaSystem/
├── apps/                           # 微服务
│   ├── java-service/              # 父POM
│   ├── user-service/              # 用户服务 (认证/权限)
│   ├── product-service/           # 商品服务 (徐义超)
│   ├── procurement-service/       # 发注服务 (张云)
│   ├── warehouse-service/         # 仓储服务 (永康)
│   ├── customs-service/           # 报关服务 (殷元)
│   ├── logistics-service/         # 物流服务 (于世荣)
│   ├── finance-service/           # 财务服务 (许文豪)
│   ├── notification-service/      # 通知服务 (陈天仪)
│   └── web/                       # 前端应用
├── config/                         # 配置文件
│   ├── checkstyle/              # 代码规范
│   └── nacos/                   # Nacos配置
├── docker/                        # Docker配置
│   └── compose.yaml             # 本地开发环境
├── scripts/                        # 运维脚本
│   └── build-all.sh            # 全量构建脚本
├── infra/                         # 基础设施
│   └── helm/                   # K8s Helm Charts
├── docs/                          # 文档
│   └── *.md                    # 详细文档
├── .github/                       # CI/CD
│   └── workflows/
│       └── ci.yaml             # GitHub Actions
└── README.md
```

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot 3 | 3.2.5 |
| 数据库 | MySQL 8 | 8.0 |
| 缓存 | Redis | 7 |
| 消息队列 | Apache Kafka | 3.8 |
| 配置中心 | Nacos | 2.3 |
| 前端 | Vue 3 + TypeScript | 3.4 |
| 构建工具 | Vite | 5.x |
| 包管理 | pnpm | 8.x |

## 快速开始

### 1. 启动基础设施

```bash
cd docker
docker compose up -d
```

### 2. 编译服务

```bash
# 编译单个服务
cd apps/user-service
./mvnw clean package -DskipTests

# 编译所有服务
./scripts/build-all.sh
```

### 3. 启动服务

```bash
cd apps/user-service
./mvnw spring-boot:run
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
| [01-项目规划与架构设计](docs/desigin/01-项目规划与架构设计.md) | 限界上下文、领域模型 |
| [02-环境准备](docs/desigin/02-环境准备.md) | 开发环境配置 |
| [03-后端服务生成](docs/desigin/03-后端服务生成.md) | 微服务生成 |
| [04-前端项目生成](docs/desigin/04-前端项目生成.md) | 前端配置 |
| [05-领域模块开发](docs/desigin/05-领域模块开发.md) | 业务模块开发 |
| [06-配置与部署](docs/desigin/06-配置与部署.md) | Docker、CI/CD |

---

架构委员会 | arch@manpou.com
