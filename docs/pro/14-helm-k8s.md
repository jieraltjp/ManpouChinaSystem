# 项目文档：Helm K8s 部署

> **文档角色**：运维工程师 + 架构师视角
> **对应目录**：`infra/helm/`
> **用途**：生产 Kubernetes 集群部署

---

## 1. 概述

| 维度 | 说明 |
|------|------|
| Chart 类型 | Helm v3 |
| Kubernetes 版本 | 1.27+ |
| 生产状态 | 脚手架 ✅，待接入集群 |

---

## 2. Chart 结构

```
infra/helm/
├── Chart.yaml                    # 根 Chart（元数据）
└── java-service/
    ├── Chart.yaml               # java-service Chart
    ├── values.yaml              # 默认配置
    ├── values-prod.yaml         # 生产环境覆盖
    └── templates/
        ├── _helpers.tpl        # 模板辅助函数
        ├── deployment.yaml      # Deployment（多副本 + 就绪探针）
        ├── service.yaml         # ClusterIP Service
        ├── ingress.yaml         # Ingress（可选 TLS）
        ├── configmap.yaml       # 非敏感配置
        ├── secret.yaml          # 敏感数据（密钥、密码）
        └── pod-disruption-budget.yaml # Pod 中断预算
```

---

## 3. 核心模板说明

### 3.1 Deployment

```yaml
# 核心配置
replicaCount: 2                        # 默认 2 副本
podDisruptionBudget.minAvailable: 1    # 滚动更新时至少保留 1 个
readinessProbe:
  path: /actuator/health/readiness
  initialDelaySeconds: 30
livenessProbe:
  path: /actuator/health/liveness
  initialDelaySeconds: 60
resources:
  requests: { cpu: 250m, memory: 512Mi }
  limits:   { cpu: 1000m, memory: 1Gi }
```

### 3.2 安全上下文

```yaml
podSecurityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  seccompProfile.type: RuntimeDefault

securityContext:
  allowPrivilegeEscalation: false
  capabilities.drop: ["ALL"]
```

### 3.3 Ingress（可选）

```yaml
# values.yaml 中启用
ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
    - host: api.manpou.com
      paths: [{ path: /, pathType: Prefix }]
  tls:
    - secretName: api-manpou-tls
      hosts: [api.manpou.com]
```

---

## 4. 部署命令

```bash
# 安装（默认 values.yaml）
helm upgrade --install java-service ./infra/helm/java-service \
  --namespace manpou --create-namespace

# 安装（生产环境）
helm upgrade --install java-service ./infra/helm/java-service \
  --namespace manpou \
  --values ./infra/helm/java-service/values-prod.yaml \
  --set image.tag=v1.0.0 \
  --timeout 5m

# 查看部署状态
helm list -n manpou

# 回滚
helm rollback java-service -n manpou

# 卸载
helm uninstall java-service -n manpou
```

---

## 5. 生产环境变量（values-prod.yaml 关键覆盖）

```yaml
replicaCount: 3                        # 生产 3 副本
image:
  repository: ghcr.io/manpou/java-service
  pullPolicy: Always
  tag: "latest"
resources:
  requests: { cpu: 500m, memory: 1Gi }
  limits:   { cpu: 2000m, memory: 2Gi }
spring:
  profiles:
    active: prod                      # 启用生产配置
  datasource:
    url: jdbc:mysql://mysql.manpou.svc:3306/manpou
  redis:
    host: redis.manpou.svc
spring.cloud.nacos.discovery.enabled: true
spring.cloud.nacos.config.enabled: true
```

---

## 6. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/pro/13-docker-compose.md` | 本地开发环境（Docker Compose） |
| `docs/pro/15-monitoring.md` | 监控告警配置 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
