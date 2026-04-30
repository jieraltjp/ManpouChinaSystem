# 速查卡

## 拉取 + 启动（一行搞定）

```bash
git pull origin main && ./scripts/start-all.sh
```

---

## 启停

| 操作 | 命令 |
|---|---|
| 启动全部 | `./scripts/start-all.sh` |
| 停止全部 | `./scripts/stop-all.sh` |
| 重启全部 | `./scripts/restart-all.sh` |
| 查看状态 | `./scripts/start-all.sh status` |
| 只启后端 | `./scripts/start-all.sh manpou` |
| 只启前端 | `./scripts/start-all.sh web` |

---

## 端口

| 服务 | 端口 |
|---|---|
| manpou-allinone | 18090 |
| user-service | 18081 |
| web 前端 | 13000 |
| Nacos 注册中心 | 8848（API）、8080（控制台 /next/） |
| Redis | 6379 |
| Kafka | 9092 |

**Nacos 控制台：** http://192.168.13.123:8080/next/  
**默认账号：** `nacos` / `manpou`（生产环境请修改）

---

## 日志

```bash
# 后端日志
tail -f logs/manpou-allinone.log

# 前端日志
tail -f logs/web.log
```

---

## Docker（Phase B）

> **LXC 环境必须加 `--privileged`**，否则容器无法启动（AppArmor 限制）。

### 启动基础设施（一次性步骤）

```bash
# 启动 Redis
docker run -d --name redis \
  --restart unless-stopped --privileged \
  -p 6379:6379 \
  redis:latest redis-server --requirepass redis123

# Kafka（KRaft 模式，首次需初始化 storage）
# 1. 生成 cluster UUID
UUID=$(docker run --rm --privileged apache/kafka:3.8.0 \
  /opt/kafka/bin/kafka-storage.sh random-uuid)

# 2. 提取并修改 server.properties
docker run --rm --privileged apache/kafka:3.8.0 \
  cat /opt/kafka/config/kraft/server.properties \
  | sed 's|log.dirs=/tmp/kraft-combined-logs|log.dirs=/kafka/kraft-combined-logs|' \
  > /tmp/kafka-kraft.properties

# 3. 初始化存储（只需一次，保留 UUID）
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

# 4. 启动 Kafka
docker run -d --name kafka \
  --restart unless-stopped --privileged \
  -p 9092:9092 \
  -v kafka-data:/kafka \
  -v /tmp/kafka-kraft.properties:/opt/kafka/config/kraft/server.properties:ro \
  apache/kafka:3.8.0 \
  /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/kraft/server.properties

# Nacos（控制台: http://host:8080/next/，账号 nacos/manpou）
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
```

### 日常操作

```bash
# 查看运行中的容器
docker ps

# 重启基础设施（保留数据）
docker restart redis kafka nacos

# 完全重建（需重新初始化 Kafka storage 时用）
docker stop redis kafka nacos && docker rm redis kafka nacos
# 然后重新执行上方 "启动基础设施" 步骤
```

### 验证

```bash
# Nacos 就绪（返回 {"status":"UP"}）
curl http://localhost:8848/nacos/v1/ns/operator/metrics

# Kafka 连接（需先进入容器）
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# 创建 Kafka topic
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic procurement-orders --partitions 1 --replication-factor 1

# Nacos 控制台登录（用户名 nacos，密码 manpou）
# http://192.168.13.123:8080/next/
```

---

## 编译

```bash
./scripts/build-all.sh
```

---

## 首次配置（新机器）

### Windows

```bash
./scripts/init-config.sh dev
./scripts/start-all.sh
```

### Ubuntu Server（阶段 A：无需容器基础设施）

```bash
# 安装依赖
apt update && apt install -y openjdk-21-jdk curl wget git unzip
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
apt install -y redis-server
sed -i 's/^# requirepass.*/requirepass redis123/' /etc/redis/redis.conf
systemctl restart redis-server

# 编译
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd /opt/ManpouChinaSystem
cd libs/manpou-common && mvn install -DskipTests -q && cd ..
cd apps/manpou-allinone && mvn package -DskipTests

# 启动后端
export REDIS_PASSWORD=redis123
cd /opt/ManpouChinaSystem/apps/manpou-allinone
nohup java -Xms256m -Xmx512m -jar target/manpou-allinone-1.0.0-SNAPSHOT.jar \
  --server.port=18090 --spring.profiles.active=development \
  > /opt/ManpouChinaSystem/logs/manpou-allinone.log 2>&1 &

# 启动前端
cd /opt/ManpouChinaSystem/apps/web
npm install
nohup npm run dev > /opt/ManpouChinaSystem/logs/web.log 2>&1 &

# 开放防火墙
ufw allow 13000/tcp
ufw allow 18090/tcp
```

### Ubuntu Server（阶段 B：启用容器基础设施）

```bash
# 先启用 LXC 嵌套，或使用独立 VM
apt install -y docker.io docker-compose
systemctl start docker

cd /opt/ManpouChinaSystem
./scripts/init-config.sh dev
./scripts/start-all.sh
```
