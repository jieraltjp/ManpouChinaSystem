# 项目文档：监控告警体系

> **文档角色**：运维工程师 + 架构师视角
> **对应目录**：`monitoring/` + `docker/`
> **用途**：Prometheus 指标采集 + Grafana 可视化 + Alertmanager 告警

---

## 1. 组件架构

```
                    ┌──────────────┐
                    │ Prometheus   │  ← metrics scraper (:9090)
                    └──────┬───────┘
                           │ scrape
         ┌─────────────────┼─────────────────┐
         │                 │                 │
  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
  │Java Services │  COS(腾讯云)  │  Kafka      │
  │:18081-18088  │  │             │  │             │
  └─────────────┘  └─────────────┘  └─────────────┘

         ┌─────────────────┬─────────────────┐
         │                 │                 │
  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
  │ Grafana     │  │ Alertmanager│  │ OTel        │
  │ (:3000)     │  │             │  │ Collector   │
  └─────────────┘  └──────┬──────┘  └──────┬──────┘
         │                 │                 │
         │          ┌──────▼──────┐  ┌──────▼──────┐
         └─────────►│   告警通知   │  │   Tempo     │
                    │ (Email/Slack)│  │ (链路存储)  │
                    └─────────────┘  └─────────────┘
```

---

## 2. 监控内容

### 2.1 Java 服务指标（JMX Exporter / Micrometer）

| 指标 | 说明 |
|------|------|
| `http_server_requests_seconds` | HTTP 请求延迟分布（p50/p95/p99） |
| `jvm_memory_used_bytes` | JVM 堆内存使用 |
| `jvm_gc_pause_seconds` | GC 暂停时间 |
| `hikaricp_connections_active` | 数据库连接池活跃数 |
| `kafka_consumer_lag` | Kafka 消费延迟 |
| `process_cpu_usage` | 进程 CPU 使用率 |

### 2.2 基础设施指标

| 组件 | 关键指标 |
|------|---------|
| MySQL | 连接数 / 查询 QPS / 慢查询 |
| Redis | 内存使用 / 命中率 / 连接数 |
| Kafka | Topic lag / 消费延迟 / ISR 副本 |
| 腾讯云 COS | 存储使用 / 请求延迟（腾讯云控制台） |

---

## 3. Prometheus 告警规则

文件：`monitoring/prometheus/alerts.yml`

| 告警名称 | 条件 | 等级 |
|---------|------|------|
| `ServiceDown` | `up == 0` 持续 1 分钟 | critical |
| `HighErrorRate` | `rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05` | warning |
| `HighLatency` | `histogram_quantile(0.95, ...) > 2` | warning |
| `JVMHeapUsageHigh` | `jvm_memory_used_bytes / jvm_memory_max_bytes > 0.85` | warning |
| `KafkaConsumerLagHigh` | `kafka_consumer_lag > 10000` | warning |
| `DatabaseConnectionPoolExhausted` | `hikaricp_connections_active / hikaricp_connections_max > 0.9` | critical |

---

## 4. Alertmanager 配置

文件：`monitoring/alertmanager/alertmanager.yml`

```yaml
global:
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alert@manpou.com'

route:
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default'

receivers:
  - name: 'default'
    email_configs:
      - to: 'oncall@manpou.com'
        send_resolved: true
  - name: 'slack'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/xxx'
        channel: '#alerts'
```

---

## 5. Grafana 仪表盘

| 仪表盘 | 文件 | 内容 |
|--------|------|------|
| 微服务总览 | `micro-service.json` | 请求量 / 延迟 / 错误率 / JVM |
| 基础设施 | `infra.json` | MySQL / Redis / Kafka / COS（腾讯云） |

导入命令：
```bash
# 通过 API 导入
curl -X POST http://admin:admin@localhost:3001/api/dashboards/import \
  -H 'Content-Type: application/json' \
  -d @monitoring/grafana/dashboards/micro-service.json
```

---

## 6. 启动监控栈

```bash
# docker/compose.yaml 已包含所有监控组件
docker compose -f docker/compose.yaml up -d prometheus grafana alertmanager

# 验证 Prometheus 目标
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets'

# 查看 Grafana
open http://localhost:3001
```

---

## 7. 文件清单

```
monitoring/
├── alertmanager/
│   └── alertmanager.yml      # 告警路由 + 通知渠道
├── prometheus/
│   └── alerts.yml           # 告警规则（PrometheusRule）
├── grafana/
│   ├── datasources/
│   │   └── prometheus.yaml  # Prometheus 数据源配置
│   └── dashboards/
│       ├── dashboard.yml     # 仪表盘注册
│       ├── micro-service.json # 微服务仪表盘
│       └── infra.json        # 基础设施仪表盘
```

---

## 8. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/pro/12-docker-compose.md` | 监控栈启动方式 |
| `docs/pro/13-helm-k8s.md` | K8s 部署中的 Prometheus Operator |
| `docs/pro/00-root-project.md` | 项目全局概览 |
