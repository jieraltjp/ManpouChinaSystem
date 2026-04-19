# 项目概览：ManpouChinaSystem（满铺中国系统）

> **文档角色**：架构师视角 → 项目全局
> **对应角色文档**：`docs/role/02-架构师视角分析.md`

---

## 1. 项目定位

| 维度 | 说明 |
|------|------|
| 项目类型 | Maven 多模块 + 前端 Monorepo |
| 定位 | 面向全球市场的发注·仓储·报关·物流·财务一体化管理系统 |
| 技术栈 | Spring Boot 3 + Vue 3 + MySQL/Redis/Kafka |
| Java 版本 | JDK 21 |
| 前端版本 | Node 20+ |

---

## 2. 模块结构

```
ManpouChinaSystem/
├── apps/
│   ├── java-service/         # [Parent POM] 所有 Java 服务的父 pom
│   ├── user-service/         # 用户认证 + 权限管理（端口 18081）
│   ├── product-service/      # 商品管理（端口 18082）
│   ├── procurement-service/   # 发注管理（端口 18083）
│   ├── warehouse-service/     # 仓储管理（端口 18084）
│   ├── customs-service/       # 报关管理（端口 18085）
│   ├── logistics-service/     # 物流管理（端口 18086）
│   ├── finance-service/       # 财务管理（端口 18087）
│   ├── notification-service/  # 通知服务（端口 18088）
│   ├── api-gateway/         # API 网关（端口 18080）
│   └── web/                  # 前端（端口 3000）
├── scaffolds/
│   └── api-gateway/          # API 网关脚手架（已迁移至 apps/）
├── infra/
│   └── helm/                 # K8s Helm 部署模板
├── docker/                   # Docker Compose 本地开发环境
├── monitoring/               # Prometheus + Grafana + Alertmanager
├── config/
│   ├── checkstyle/          # 代码规范
│   └── nacos/              # Nacos 配置模板
├── scripts/                  # 构建与初始化脚本
├── .github/workflows/       # CI/CD 流水线
└── docs/
    ├── pro/                 # 各项目专属文档（本目录，12-17 基础设施/服务）
    ├── ui/                  # 前端 UI 文档 + 架构图
    ├── desigin/            # 构建指南（Step 1-6）
    └── role/               # 角色视角分析（6 角色）
```

---

## 3. 技术栈矩阵

| 层级 | 技术 | 版本 |
|------|------|------|
| Java 运行时 | JDK | 21 |
| Java 框架 | Spring Boot | 3.2.5 |
| Java 微服务生态 | Spring Cloud Alibaba | 2023.0.1.2 |
| ORM | Spring Data JPA + Hibernate | - |
| 数据库 | MySQL 8.x（开发）/ TiDB v8（生产） | 8.0 |
| 数据库迁移 | Flyway | 10.10.0 |
| 前端框架 | Vue | 3.4+ |
| 前端构建 | Vite | 5.3+ |
| 前端语言 | TypeScript | 5.4+ |
| 状态管理 | Pinia | 2.1+ |
| UI 组件库 | Element Plus | 2.8+ |
| 缓存 | Redis | 7 |
| 消息队列 | Kafka | 3.8 |
| 对象存储 | MinIO | latest |
| 服务注册/配置 | Nacos | - |
| 链路追踪 | OTel + Grafana Tempo | - |

---

## 4. 端口分配

| 服务 | 端口 | 上下文路径 |
|------|------|-----------|
| user-service | 18081 | /api/v1 |
| product-service | 18082 | /api/v1 |
| procurement-service | 18083 | /api/v1 |
| warehouse-service | 18084 | /api/v1 |
| customs-service | 18085 | /api/v1 |
| logistics-service | 18086 | /api/v1 |
| finance-service | 18087 | /api/v1 |
| notification-service | 18088 | /api/v1 |
| web（前端） | 3000 | / |
| Nacos | 8848 | /nacos |
| Kafka | 9092 | - |
| Kafka UI | 8080 | - |

---

## 5. 构建与运行

### 5.1 全量构建

```bash
# 根目录执行
mvn clean install -DskipTests   # Java 全量构建
cd apps/web && npm install && npm run build   # 前端构建
```

### 5.2 单服务运行

```bash
# 后端（procurement-service 示例）
cd apps/procurement-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 前端
cd apps/web
npm run dev
```

### 5.3 Docker 运行

```bash
docker-compose -f docker-compose.yml up -d
```

---

## 6. 认证机制

- **算法**：RS256（非对称签名）
- **Token 有效期**：15 分钟（access token）
- **密钥管理**：JWT 签名密钥表（signing_key 表）+ 自动轮换
- **登录入口**：`POST /api/v1/auth/login`（user-service）

---

## 7. 测试覆盖

| 层级 | 状态 |
|------|------|
| ArchUnit 架构测试 | ✅ 8 个服务全部通过 |
| 单元测试 | 🔴 暂无 |
| 集成测试 | 🔴 暂无 |
| E2E 测试 | 🔴 暂无 |

---

## 8. 已知风险

| ID | 风险 | 等级 | 建议 |
|----|------|------|------|
| AR-001 | API Gateway 脚手架已生成（apps/api-gateway），待启动验证 | 🟡 中 | 启动 + 集成前端代理 |
| AR-002 | 无服务间认证 | 🔴 高 | mTLS / JWT 内部传递 |
| AR-003 | Kafka 消费者未实现 | 🔴 高 | 实现消费逻辑 |
| AR-004 | Nacos 配置当前禁用 | 🟡 中 | 开发完成后接入 |
| AR-005 | MinIO 未集成 | 🟡 中 | P1 接入 |

---

## 9. 快速入口

| 资源 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| Swagger UI（procurement） | http://localhost:18083/swagger-ui/index.html |
| Swagger UI（user） | http://localhost:18081/swagger-ui/index.html |
| Nacos（需 Docker） | http://localhost:8848/nacos |
| Kafka UI（需 Docker） | http://localhost:8080 |
| Grafana（需 Docker） | http://localhost:3001 |
| Prometheus（需 Docker） | http://localhost:9090 |

---

## 10. 项目专属文档（docs/pro/）

| 编号 | 文档 | 对应目录 |
|------|------|---------|
| 00 | 项目概览 | — |
| 01 | Java 父 POM | `apps/java-service/` |
| 02 | user-service | `apps/user-service/` |
| 03 | product-service | `apps/product-service/` |
| 04 | warehouse-service | `apps/warehouse-service/` |
| 05 | procurement-service | `apps/procurement-service/` |
| 06 | customs-service | `apps/customs-service/` |
| 07 | logistics-service | `apps/logistics-service/` |
| 08 | finance-service | `apps/finance-service/` |
| 09 | notification-service | `apps/notification-service/` |
| 10 | web 前端 + UI 文档 | `apps/web/` |
| 11 | API Gateway | `apps/api-gateway/` |
| 12 | Docker Compose | `docker/` |
| 13 | Helm K8s | `infra/helm/` |
| 14 | 监控告警 | `monitoring/` |
| 15 | CI/CD | `.github/workflows/` + `scripts/` |
| 16 | 配置中心 | `config/nacos/` + `config/checkstyle/` |
| 17 | 可插拔基础设施 | Docker 可选接入，代码不依赖容器存在 |

---

## 11. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/02-架构师视角分析.md` | 架构决策、约束、风险 |
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/role/05-前端开发工程师视角分析.md` | 前端开发规范 |
| `docs/role/06-测试工程师视角分析.md` | 测试策略与用例 |
| `docs/ui/README.md` | 前端 UI 文档入口 |
| `docs/ui/ARCHITECTURE.md` | 系统架构图（Mermaid） |
| `docs/pro/05-procurement-service.md` | 发注服务文档 |
