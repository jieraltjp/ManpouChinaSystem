# 项目文档：Docker Compose（本地开发环境）

> **文档角色**：架构师 + 运维工程师视角
> **对应目录**：`docker/`
> **用途**：本地开发环境基础设施（MySQL / Redis / Kafka / Nacos / MinIO）

---

## 1. 概述

| 维度 | 说明 |
|------|------|
| 文件 | `docker/compose.yaml` |
| 环境 | 本地开发（不适用于生产） |
| 当前状态 | 配置完成 ✅，本地开发不使用 |

> **重要**：本地开发各服务独立运行（`mvn spring-boot:run`），无需启动 Docker。Docker/K8s 基础设施已预留空间，后续接入。

---

## 2. 组件清单

| 服务 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| **MySQL** | mysql:8.0 | 3306 | 业务数据持久化 |
| **Redis** | redis:7 | 6379 | 缓存 + 会话 |
| **Kafka** | bitnami/kafka:3.8 | 9092 (ext:29092) | 消息队列 |
| **Zookeeper** | confluentinc/cp-zookeeper:7.6 | 2181 | Kafka 依赖 |
| **Nacos** | nacos/nacos-server:v2.3 | 8848 (ext:8848) | 配置中心 + 注册中心 |
| **MinIO** | minio/minio | 9000 (ext:9001) | 对象存储（文件/照片） |
| **Prometheus** | prom/prometheus:v2.52 | 9090 | 指标采集 |
| **Grafana** | grafana/grafana:11 | 3001 | 可视化监控 |
| **OTel Collector** | otel/opentelemetry-collector:0.107 | 4317 (grpc) / 4318 (http) | 链路追踪采集 |
| **Tempo** | grafana/tempo:2 | 3100 | 链路存储 |
| **Kafka UI** | provectuslabs/kafka-ui:latest | 8080 | Kafka 管理界面 |

---

## 3. 启动命令

```bash
# 启动所有基础设施
docker compose -f docker/compose.yaml up -d

# 查看状态
docker compose -f docker/compose.yaml ps

# 查看日志
docker compose -f docker/compose.yaml logs -f

# 停止所有服务
docker compose -f docker/compose.yaml down

# 停止并清除数据卷
docker compose -f docker/compose.yaml down -v
```

---

## 4. 初始化数据

### 4.1 MySQL 初始化

数据库启动时自动执行 `docker/init.sql`（需创建）：
```sql
CREATE DATABASE IF NOT EXISTS manpou CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE manpou;
CREATE TABLE IF NOT EXISTS `user` (...);
```

### 4.2 Nacos 配置

初始化脚本位于 `scripts/init-config.sh`，将 `config/nacos/` 下的配置推送到 Nacos。

---

## 5. 各组件访问地址

| 服务 | 地址 | 默认凭证 |
|------|------|---------|
| 前端（通过网关） | http://localhost:13000 | — |
| Nacos | http://localhost:8848/nacos | nacos/nacos |
| Kafka UI | http://localhost:8080 | — |
| MinIO Console | http://localhost:9001 | minioadmin/minioadmin |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3001 | admin/admin |
| Jaeger/Tempo | http://localhost:16686 | — |

---

## 6. 环境变量覆盖

```bash
# 使用 .env 文件覆盖默认值
echo "MYSQL_ROOT_PASSWORD=secret" > docker/.env
docker compose -f docker/compose.yaml up -d
```

---

## 7. 文件清单

```
docker/
├── compose.yaml              # 主配置文件
├── otel-collector-config.yaml # OTel 采集器配置
├── tempo-config.yaml        # Grafana Tempo 配置
└── prometheus/
    └── prometheus.yml       # Prometheus 抓取配置
```

---

## 8. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/pro/13-helm-k8s.md` | K8s 生产部署（Helm） |
| `docs/pro/14-monitoring.md` | 监控告警配置 |
| `docs/pro/16-config-center.md` | Nacos 配置中心 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
