# ManpouChinaSystem 构建指南

> 满铺中国零售管理系统 — 从零开始构建指南

---

## 文档索引

| 文档 | 说明 | 状态 |
|------|------|------|
| [01-项目规划与架构设计](01-项目规划与架构设计.md) | 项目概述、限界上下文、领域模型设计 | ✅ |
| [02-环境准备](02-环境准备.md) | 开发环境配置、IDE 设置 | ✅ |
| [03-后端服务生成](03-后端服务生成.md) | 使用 codegen 生成微服务 | ✅ |
| [04-前端项目生成](04-前端项目生成.md) | Vue 3 前端项目配置 | ✅ |
| [05-领域模块开发](05-领域模块开发.md) | 业务模块详细开发 | ✅ |
| [06-配置与部署](06-配置与部署.md) | Docker Compose、CI/CD 配置 | ✅ |

---

## 快速开始

### 1. 克隆脚手架

```bash
cd D:/Programme/java/ManpouChinaSystem
git clone https://your-git-server/arch-scaffolds.git scaffolds
```

### 2. 安装依赖

```bash
# 前端
cd apps/web && npm install
```

### 3. 启动服务

```bash
# Docker Compose 启动基础设施（可选）
docker compose -f docker/compose.yaml up -d

# 启动后端服务（user-service 必须最先启动）
cd apps/user-service && mvn spring-boot:run
cd apps/procurement-service && mvn spring-boot:run
# ... 其他服务

# 启动前端
cd apps/web && npm run dev
```

---

## 项目架构

```
ManpouChinaSystem
├── apps/
│   ├── java-service/           # Maven 父 POM
│   ├── user-service/           # 用户认证 (18081)
│   ├── product-service/        # 商品管理 (18082)
│   ├── procurement-service/    # 发注管理 (18083)
│   ├── warehouse-service/      # 仓储管理 (18084)
│   ├── customs-service/        # 报关管理 (18085)
│   ├── logistics-service/      # 物流管理 (18086)
│   ├── finance-service/        # 财务管理 (18087)
│   ├── notification-service/  # 通知服务 (18088)
│   └── web/                   # Vue 3 前端 (3000)
├── docs/
│   ├── desigin/               # 构建指南
│   ├── pro/                   # 项目专属文档（每服务一篇）
│   └── role/                  # 角色视角文档（6 角色）
├── config/nacos/              # Nacos 配置模板
├── docker/                    # Docker Compose
├── infra/helm/                 # K8s Helm 模板
├── monitoring/                 # 监控告警
└── scaffolds/                 # 脚手架（codegen / api-gateway）
```

---

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| Java | JDK | 21 |
| 数据库 | MySQL 8 / H2（开发） | 8.0 |
| ORM | Spring Data JPA | — |
| 消息队列 | Apache Kafka | 3.8 |
| 配置中心 | Nacos（可选） | 2.3 |
| 前端框架 | Vue 3 + TypeScript | 3.4 |
| 构建工具 | Vite | 5.x |
| 状态管理 | Pinia | — |
| UI 组件库 | Element Plus | — |
| 包管理 | npm | — |

---

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| user-service | 18081 | JWT 认证（最先启动） |
| product-service | 18082 | 商品管理 |
| procurement-service | 18083 | 发注管理核心 |
| warehouse-service | 18084 | 仓储管理 |
| customs-service | 18085 | 报关管理 |
| logistics-service | 18086 | 物流管理 |
| finance-service | 18087 | 财务管理 |
| notification-service | 18088 | 通知推送 |
| web（前端） | 3000 | Vite dev server |

---

## 联系方式

架构委员会 | arch@manpou.com
