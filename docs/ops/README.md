# 运维手册

> **仅限 Phase 0 三服务。** 微服务体系全部上线后扩展本文档。

---

## 1. 服务拓扑

| 服务 | 端口 | 类型 | 说明 |
|---|---|---|---|
| `manpou-allinone` | `18090` | Spring Boot | 后端主服务（JAR: `apps/manpou-allinone/target/*.jar`） |
| `web` | `13000` | Vite Dev Server | 前端（`apps/web/`） |
| `user-service` | `18081` | Spring Boot | 用户服务（当前未启用） |

**基础设施（Docker，Phase B）：**

| 服务 | 端口 | 说明 |
|---|---|---|
| Redis | `6379` | Docker，密码 `redis123` |
| Kafka | `9092` | Docker，KRaft 模式 |
| Nacos | `8848`（API）/ `8080`（控制台 /next/） | Docker，注册/配置中心 |

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
| `./scripts/build-all.sh` | 编译所有微服务 JAR |

> **Profile 自动选择：** `start-all.sh` 会自动检测 OS：
> - Windows (git-bash): 使用 `local` profile（H2 内存数据库）
> - Linux (Ubuntu Server): 使用 `development` profile（连接远程 MySQL `192.168.13.202:23306`）
>
> 可通过环境变量覆盖：`SPRING_PROFILES_ACTIVE=local ./scripts/start-all.sh`

**日志文件：** `logs/<服务名>.log`
**PID 文件：** `logs/<服务名>.pid`

---

## 4. 新机器首次配置（一次性）

### Windows（已有 Docker Desktop）

```bash
./scripts/init-config.sh dev
```

### Ubuntu Server（新装机）

#### 阶段 A：立即可用（无需容器基础设施）

适用于：Proxmox LXC 容器（Docker/Podman 受内核限制）。

**第一步：安装基础依赖**

```bash
# 以 root 身份执行
apt update && apt install -y openjdk-21-jdk curl wget git unzip

# Node.js 20.x
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs

# Redis（本地缓存）
apt install -y redis-server
sed -i 's/^# requirepass.*/requirepass redis123/' /etc/redis/redis.conf
systemctl restart redis-server
systemctl enable redis-server
```

**第二步：创建 application-development.yml（gitignored，git pull 后需手动重建）**

```bash
mkdir -p /opt/ManpouChinaSystem/apps/manpou-allinone/src/main/resources/
cat > /opt/ManpouChinaSystem/apps/manpou-allinone/src/main/resources/application-development.yml << 'YAML'
spring:
  application:
    name: manpou-allinone
  datasource:
    url: jdbc:mysql://192.168.13.202:23306/manpou?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&connectionAttributes=none
    username: root
    password: manpou23306
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
  flyway:
    enabled: false
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
logging:
  level:
    root: INFO
    com.manpou.allinone: INFO
  org.hibernate.SQL: WARN
  org.hibernate.tool.schema: INFO
YAML
```

**第三步：获取代码（内网无 git 访问时，用 rsync / scp / tar+ssh 推送）**

```bash
# 先在远程创建目录
ssh root@<服务器IP> "mkdir -p /opt/ManpouChinaSystem"

# 打包推送（排除构建产物）
tar --exclude='.git' \
    --exclude='apps/manpou-allinone/target' \
    --exclude='apps/web/node_modules' \
    --exclude='apps/web/dist' \
    -czf - . \
    | ssh root@<服务器IP> "tar -xzf - -C /opt/ManpouChinaSystem"
```

**第四步：编译后端**

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=\$JAVA_HOME/bin:\$PATH
cd /opt/ManpouChinaSystem

# 先编译依赖模块
cd libs/manpou-common && mvn install -DskipTests -q && cd ..

# 编译主服务
cd apps/manpou-allinone && mvn package -DskipTests
```

**第五步：启动**

```bash
# 开放防火墙
ufw allow 13000/tcp
ufw allow 18090/tcp

# 后端（development profile，连接远程 MySQL 192.168.13.202:23306）
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=\$JAVA_HOME/bin:\$PATH
export REDIS_PASSWORD=redis123
cd /opt/ManpouChinaSystem/apps/manpou-allinone
nohup java -Xms256m -Xmx512m -jar target/manpou-allinone-1.0.0-SNAPSHOT.jar \
  --server.port=18090 \
  --spring.profiles.active=development \
  > /opt/ManpouChinaSystem/logs/manpou-allinone.log 2>&1 &

# 前端（首次需 npm install）
cd /opt/ManpouChinaSystem/apps/web
npm install
nohup npm run dev > /opt/ManpouChinaSystem/logs/web.log 2>&1 &
```

---

#### 阶段 B：启用容器基础设施（Proxmox 嵌套 或 VM）

适用于：Proxmox LXC 已开启嵌套功能、或使用独立 VM。

**前提条件：** LXC 嵌套已启用，或为独立 VM（而非容器）。

> **LXC 环境特别说明：** 所有 `docker run` 命令必须加 `--privileged`，否则 AppArmor 会阻止容器启动。

```bash
# 安装 Docker
apt install -y docker.io docker-compose
systemctl enable docker
systemctl start docker

# 安装完成后验证（hello-world 必须成功）
docker run --privileged hello-world
```

**创建 application-development.yml（gitignored，git pull 后需手动重建）：**
（同阶段 A 第二步，完整 YAML 内容略——参考 QUICKREF.md 或 docs/ops/README.md 中阶段 A 的完整配置块）

**启动基础设施（一次性初始化）：**

```bash
# Redis
docker run -d --name redis \
  --restart unless-stopped --privileged \
  -p 6379:6379 \
  redis:latest redis-server --requirepass redis123

# Kafka（KRaft，首次需初始化）
UUID=$(docker run --rm --privileged apache/kafka:3.8.0 \
  /opt/kafka/bin/kafka-storage.sh random-uuid)
docker run --rm --privileged apache/kafka:3.8.0 \
  cat /opt/kafka/config/kraft/server.properties \
  | sed 's|log.dirs=/tmp/kraft-combined-logs|log.dirs=/kafka/kraft-combined-logs|' \
  > /tmp/kafka-kraft.properties
docker volume create kafka-data
docker run --rm --privileged -v kafka-data:/kafka --user root \
  apache/kafka:3.8.0 bash -c \
  'mkdir -p /kafka/kraft-combined-logs && chown 1000:1000 /kafka/kraft-combined-logs'
docker run --rm --privileged -v kafka-data:/kafka \
  -v /tmp/kafka-kraft.properties:/opt/kafka/config/kraft/server.properties:ro \
  --user root apache/kafka:3.8.0 bash -c \
  'chown appuser:appuser /kafka/kraft-combined-logs && \
   su appuser -c "/opt/kafka/bin/kafka-storage.sh format \
   -t $UUID -c /opt/kafka/config/kraft/server.properties --ignore-formatted"'
docker run -d --name kafka \
  --restart unless-stopped --privileged \
  -p 9092:9092 \
  -v kafka-data:/kafka \
  -v /tmp/kafka-kraft.properties:/opt/kafka/config/kraft/server.properties:ro \
  apache/kafka:3.8.0 \
  /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/kraft/server.properties

# Nacos（JWT secret 必须 >=32 字节 base64）
SECRET=$(dd if=/dev/urandom bs=1 count=32 2>/dev/null | base64 -w0)
docker run -d --name nacos \
  --restart unless-stopped --privileged \
  -p 8848:8848 -p 9848:9848 -p 8080:8080 \
  -e MODE=standalone -e NACOS_AUTH_ENABLE=false \
  -e NACOS_AUTH_TOKEN="$SECRET" \
  -e NACOS_AUTH_PLUGIN_NACOS_TOKEN_SECRET_KEY="$SECRET" \
  -e NACOS_AUTH_PLUGIN_NACOS_TOKEN_EXPIRE_SECONDS=18000 \
  -e NACOS_AUTH_IDENTITY_KEY=nacos123 \
  -e NACOS_AUTH_IDENTITY_VALUE=nacos456 \
  nacos/nacos-server:latest

# Nacos 控制台（首次登录后修改密码）：
# http://192.168.13.123:8080/next/
# 账号：nacos / manpou

# 验证 Nacos 就绪（可能需要 60-90 秒）
sleep 60 && curl http://localhost:8848/nacos/v1/ns/operator/metrics
```

**日常启停：**

```bash
# 启动后端 + 前端
export REDIS_PASSWORD=redis123
cd /opt/ManpouChinaSystem/apps/manpou-allinone
nohup java -Xms256m -Xmx512m -jar target/manpou-allinone-1.0.0-SNAPSHOT.jar \
  --server.port=18090 > /opt/ManpouChinaSystem/logs/manpou-allinone.log 2>&1 &

cd /opt/ManpouChinaSystem/apps/web
nohup npm run dev > /opt/ManpouChinaSystem/logs/web.log 2>&1 &
```

> **如何启用 LXC 嵌套**（Proxmox Web UI）：
> 停止 LXC → 编辑 → 选项 → 嵌套 → 改为 `1（启用）` → 重启容器。
> 然后重装 Docker（清理之前损坏的配置）。

---

**Profile 说明**

| Profile | 用途 | 数据库 | Redis |
|---------|------|--------|-------|
| `local` | Windows 本地开发 | H2 内存 / 本地 MySQL | 本地 |
| `development` | Ubuntu 开发服务器 | 远程 MySQL `192.168.13.202:23306` | Docker `localhost:6379` |

**配置文件：**

- `application.yml` — 默认配置，包含 Redis、Nacos（均 disabled）、日志格式等公共配置
- `application-development.yml` — Ubuntu Server 专用，覆盖数据库连接（远程 MySQL）、禁用 Redis auto-config（Nacos 暂未启用）

> Nacos 服务注册（`spring.cloud.nacos.discovery.enabled=true`）目前与 Phase 0 单体架构存在 auto-configuration 耦合问题，将在 Phase B 微服务拆分时一并解决。Phase B 启用时需同时配置 Nacos Config 初始化。详见 TROUBLESHOOT.md。

---

## 5. 启动后访问地址

| 服务 | Windows 本地 | Ubuntu 服务器 |
|---|---|---|
| 前端页面 | http://localhost:13000 | http://192.168.13.123:13000 |
| 后端 API | http://localhost:18090 | http://192.168.13.123:18090 |
| Swagger 文档 | http://localhost:18090/swagger-ui/index.html | http://192.168.13.123:18090/swagger-ui/index.html |
| Nacos（API） | http://localhost:8848/nacos | http://192.168.13.123:8848/nacos |
| Nacos（控制台 /next/） | - | http://192.168.13.123:8080/next/（账号：nacos / manpou） |

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
