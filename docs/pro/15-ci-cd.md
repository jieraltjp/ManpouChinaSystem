# 项目文档：CI/CD 流水线

> **文档角色**：运维工程师 + 后端开发工程师视角
> **对应目录**：`.github/workflows/` + `scripts/`
> **用途**：GitHub Actions 自动构建 + 部署

---

## 1. CI/CD 架构

```
代码提交 (main)
      ↓
GitHub Actions CI (.github/workflows/ci.yaml)
      ↓ 检查：编译 + 单元测试 + ArchUnit + Checkstyle
      ↓
代码审查 + Merge
      ↓
GitHub Actions CD (.github/workflows/cd.yml)
      ↓ 构建：Docker 镜像 + 推送 GHCR
      ↓
Helm 部署 → Kubernetes 集群
```

---

## 2. CI 流水线（ci.yaml）

触发条件：`push`（main, develop）+ `pull_request`（main）

### 阶段

| 阶段 | 任务 | 说明 |
|------|------|------|
| `build` | Maven 编译 | `mvn clean compile -DskipTests` |
| `test` | 单元测试 | `mvn test` |
| `arch-test` | 架构测试 | `mvn test -Dtest=ArchUnitTest` |
| `checkstyle` | 代码风格 | Checkstyle 检查 |
| `sonar` | 代码质量 | SonarQube 扫描（可选） |

### 环境变量

| 变量 | 说明 |
|------|------|
| `JAVA_VERSION` | JDK 21 |
| `MAVEN_OPTS` | `-Xmx1024m` |

---

## 3. CD 流水线（cd.yml）

触发条件：`workflow_run`（CI Build 通过后，main 分支）

### 阶段

| 阶段 | 任务 | 说明 |
|------|------|------|
| `build-image` | 构建镜像 | `docker build -t ghcr.io/manpou/<service>:<sha> .` |
| `push-image` | 推送镜像 | 推送到 GHCR（GitHub Container Registry） |
| `deploy-staging` | 部署预发 | Helm 部署到 staging namespace |
| `smoke-test` | 冒烟测试 | `curl` 健康检查 |
| `deploy-prod` | 部署生产 | 手动确认后部署（需 GitHub Environments approval） |

---

## 4. 环境变量（Secrets）

在 GitHub `Settings → Secrets and variables → Actions` 中配置：

| Secret | 说明 |
|--------|------|
| `DOCKER_REGISTRY_TOKEN` | GHCR 登录凭证 |
| `KUBECONFIG` | Kubernetes 配置（base64 编码） |
| `SONAR_TOKEN` | SonarQube 访问令牌 |
| `SLACK_WEBHOOK` | 部署通知 Webhook |

---

## 5. 构建脚本

### 5.1 全量构建脚本

文件：`scripts/build-all.sh`

```bash
#!/bin/bash
set -e

# 1. Java 全量构建
mvn clean install -DskipTests -T 1C

# 2. 前端构建
cd apps/web && npm install && npm run build && cd ../..

# 3. Docker 镜像构建（可选）
docker build -t manpou/java-service:$1 apps/java-service
```

### 5.2 Nacos 配置初始化

文件：`scripts/init-config.sh`

```bash
#!/bin/bash
NACOS_ADDR="http://localhost:8848"
NACOS_USER="nacos"
NACOS_PASS="nacos"

for file in config/nacos/dev/*.yml; do
  name=$(basename "$file" .yml)
  curl -X POST "${NACOS_ADDR}/nacos/v1/cs/configs" \
    -d "dataId=${name}&group=DEFAULT_GROUP&type=yaml&content=$(cat $file)"
done
```

---

## 6. 版本号策略

```
镜像标签 = ${GITHUB_SHA:0:7}
Helm Chart 版本 = 语义化版本 (SemVer)
App 版本 = ${revision} (来自 pom.xml)
```

---

## 7. 分支策略

| 分支 | 触发 | 动作 |
|------|------|------|
| `feature/*` | push | CI（编译 + 测试） |
| `main` | push | CI + CD（staging 部署） |
| `release/*` | push | CD（prod 部署，需审批） |
| tag `v*` | push tag | CD（正式发布） |

---

## 8. 文件清单

```
.github/workflows/
├── ci.yaml    # CI：编译 + 测试 + Checkstyle + Sonar
└── cd.yml    # CD：镜像构建 + Helm 部署

scripts/
├── build-all.sh     # 全量构建脚本
└── init-config.sh   # Nacos 配置初始化
```

---

## 9. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/pro/13-helm-k8s.md` | Helm 部署配置 |
| `docs/pro/14-monitoring.md` | 监控告警 |
| `docs/pro/12-docker-compose.md` | Docker 开发环境 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
