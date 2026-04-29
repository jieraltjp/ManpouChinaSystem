# 运维手册

> **仅限 Phase 0 三服务。** 微服务体系全部上线后扩展本文档。

---

## 1. 服务拓扑

| 服务 | 端口 | 类型 | JAR / 目录 |
|---|---|---|---|
| `manpou-allinone` | `18090` | Spring Boot | `apps/manpou-allinone/target/*.jar` |
| `user-service` | `18081` | Spring Boot | `apps/user-service/target/*.jar` |
| `web` | `13000` | Vite Dev Server | `apps/web/` |

**基础设施依赖：** Docker Desktop（通过 `docker/compose.yaml` 提供 Nacos + MySQL + Redis + Kafka）

---

## 2. 日常操作

### 从服务器拉取并一键启动

```bash
git pull origin main && ./scripts/start-all.sh
```

验证启动结果：

```bash
./scripts/start-all.sh status
```

---

## 3. 脚本清单

| 脚本 | 作用 |
|---|---|
| `./scripts/start-all.sh` | 启动全部三服务（自动编译、自动装依赖） |
| `./scripts/start-all.sh manpou` | 仅启动后端 allinone |
| `./scripts/start-all.sh user` | 仅启动 user-service |
| `./scripts/start-all.sh web` | 仅启动前端 |
| `./scripts/start-all.sh status` | 查看三服务运行状态 |
| `./scripts/stop-all.sh` | 停止全部三服务 |
| `./scripts/restart-all.sh` | 重启（先停后启） |
| `./scripts/build-all.sh` | 编译所有微服务 JAR（Phase 0 不需要） |

**日志文件：** `logs/<服务名>.log`
**PID 文件：** `logs/<服务名>.pid`

---

## 4. 新机器首次配置（一次性）

### Windows（已有 Docker Desktop）

```bash
./scripts/init-config.sh dev
```

### Ubuntu 22.04 Server（新装机）

**第一步：安装基础依赖（一键）**

```bash
# 连接到服务器后，以 root 身份执行以下命令
apt update && apt install -y openjdk-21-jdk curl wget git unzip

# 安装 Node.js 20.x
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs

# 安装 Docker 和 docker-compose
apt install -y docker.io docker-compose
systemctl start docker
systemctl enable docker
```

**第二步：克隆代码**

```bash
cd /opt
git clone <你的仓库地址> ManpouChinaSystem
cd ManpouChinaSystem
```

**第三步：初始化并启动**

```bash
cd /opt/ManpouChinaSystem
./scripts/init-config.sh dev
./scripts/start-all.sh
```

> **资源注意**：完整 docker-compose 基础设施（TiDB + Kafka + Nacos + Redis + MinIO + Prometheus + Grafana + OTel）约需 4–8GB 内存。2GB 机器上请先只启动后端，Docker 容器按需启动：
> ```bash
> docker compose -f docker/compose.yaml up -d nacos redis   # 最少依赖
> ```

执行内容：
1. 检查 Docker 是否运行
2. 创建 `.env.local`（若不存在）
3. 启动 `docker/compose.yaml` 中的基础设施容器
4. 等待 Nacos 就绪
5. 推送 Nacos 配置到配置中心

---

## 5. 启动后访问地址

| 服务 | 地址 |
|---|---|
| 前端页面 | http://localhost:13000 |
| API 文档 | http://localhost:18090/swagger-ui/index.html |
| Nacos 控制台 | http://localhost:8848/nacos （nacos / nacos） |
| MinIO 控制台 | http://localhost:9001 （minioadmin / minioadmin123） |

---

## 6. 常见问题

详见 [TROUBLESHOOT.md](./TROUBLESHOOT.md)

---

## 7. 文档结构

```
docs/ops/
├── README.md        ← 本文档
├── QUICKREF.md      ← 速查卡（一行命令）
└── TROUBLESHOOT.md  ← 故障排查手册
```
