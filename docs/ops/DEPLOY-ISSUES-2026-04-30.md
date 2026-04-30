# 服务器部署手册（2026-04-30）

> 192.168.13.123 Ubuntu 22.04 LXC 服务器全新部署记录。

---

## 环境概览

| 组件 | 状态 | 说明 |
|------|------|------|
| Java 21 | ✅ | `openjdk-21-jdk` via apt |
| Node.js 20 | ✅ | `nodejs` via nodesource |
| Maven 3.6.3 | ✅ | apt install maven |
| Redis | ✅ | systemctl redis-server，密码 redis123 |
| MySQL | ✅ | 远程 `192.168.13.202:23306` |
| manpou-allinone | ✅ | PID=12639，端口 18090 |
| web 前端 | ✅ | PID=12759，端口 13000 |
| user-service | ✅ | PID=16633，端口 18081（需 REDIS_PASSWORD=redis123） |
| Nacos | ⛔ | Docker 不可用（LXC 环境） |
| Kafka | ⛔ | Docker 不可用（LXC 环境） |

---

## 一、依赖安装

```bash
# Java 21
apt update && apt install -y openjdk-21-jdk

# Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && apt install -y nodejs

# Maven（包含 JDK）
apt install -y maven

# Redis（本地缓存）
apt install -y redis-server
sed -i 's/^# requirepass.*/requirepass redis123/' /etc/redis/redis.conf
systemctl restart redis-server && systemctl enable redis-server

# 验证
java -version  # openjdk version "21.0.10"
node -v        # v20.20.2
mvn -v         # Apache Maven 3.6.3
redis-cli -h localhost -p 6379 -a redis123 ping  # PONG
mysql -h 192.168.13.202 -P 23306 -u root -pmanpou23306 -e "SELECT 1"  # ok=1
```

---

## 二、代码同步

### 方式 A：通过 Git（推荐，但需要认证配置好）

服务器若能访问 Gitea：
```bash
git clone https://jiangjie:密码@192.168.12.233:3000/jiangjie/ManpouChinaSystem.git /opt/ManpouChinaSystem
```

### 方式 B：通过 tar 打包本地代码推送

> 服务器 Git 认证失败时使用此方式。

```bash
# 本地打包（不含构建产物）
tar --exclude='apps/manpou-allinone/target' \
    --exclude='apps/web/node_modules' \
    --exclude='apps/web/dist' \
    --exclude='apps/user-service/target' \
    --exclude='.git' \
    -czf - . | ssh root@192.168.13.123 "tar -xzf - -C /opt/ManpouChinaSystem"
```

**注意：必须确保以下目录全部推送成功：**
- `apps/` （含 manpou-allinone、user-service、java-service）
- `libs/` （manpou-common）
- `scripts/`、`docs/`、`config/`、`docker/`、`infra/`、`monitoring/`

推送后**必须验证**：
```bash
ssh root@192.168.13.123 'find /opt/ManpouChinaSystem -maxdepth 2 -type d | sort'
```

---

## 三、配置文件

> `application-development.yml` 是 gitignored，每次 `git pull` 后必须重建。

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

---

## 四、编译

### Maven 多模块依赖顺序

```
parent pom (apps/java-service/pom.xml)
    └── manpou-common (libs/manpou-common)
    └── manpou-allinone (apps/manpou-allinone)
    └── user-service (apps/user-service)
```

**编译步骤：**

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# 1. 安装父 POM
cd /opt/ManpouChinaSystem/apps/java-service && mvn -N install -DskipTests -q

# 2. 安装 manpou-common
cd /opt/ManpouChinaSystem/libs/manpou-common && mvn install -DskipTests -q

# 3. 编译 manpou-allinone
cd /opt/ManpouChinaSystem/apps/manpou-allinone && mvn package -DskipTests

# 4. 编译 user-service
cd /opt/ManpouChinaSystem/apps/user-service && mvn package -DskipTests
```

**验证编译产物：**
```bash
ls /opt/ManpouChinaSystem/apps/manpou-allinone/target/*.jar
ls /opt/ManpouChinaSystem/apps/user-service/target/*.jar
```

---

## 五、启动服务

### manpou-allinone（后端主服务）

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
cd /opt/ManpouChinaSystem/apps/manpou-allinone
nohup java -Xms256m -Xmx512m -jar target/manpou-allinone-1.0.0-SNAPSHOT.jar \
  --server.port=18090 \
  --spring.profiles.active=development \
  > /opt/ManpouChinaSystem/logs/manpou-allinone.log 2>&1 &
echo $!
```

### user-service（用户服务）

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
export REDIS_PASSWORD=redis123
cd /opt/ManpouChinaSystem/apps/user-service
nohup java -Xms128m -Xmx256m -jar target/user-service-1.0.0-SNAPSHOT.jar \
  --server.port=18081 \
  > /opt/ManpouChinaSystem/logs/user-service.log 2>&1 &
echo $!
```

### web 前端

```bash
cd /opt/ManpouChinaSystem/apps/web
npm install
nohup npm run dev > /opt/ManpouChinaSystem/logs/web.log 2>&1 &
echo $!
```

### 健康检查

```bash
curl http://localhost:18090/actuator/health   # manpou-allinone
curl http://localhost:18081/actuator/health   # user-service
ss -tlnp | grep -E "18090|18081|13000"       # 端口验证
```

---

## 六、Nacos / Kafka（LXC 暂不可用）

> **原因：** Proxmox LXC 容器内核限制，无法运行 Docker（需要嵌套虚拟化支持）。

如需启用，参考以下方案：

### 方案 A：启用 LXC 嵌套（推荐）

在 Proxmox Web UI：
1. 停止 LXC → 编辑 → **嵌套** → 改为 `1（启用）`
2. 重启 LXC
3. 重装 Docker：
   ```bash
   apt purge -y docker.io docker-compose
   rm -rf /var/lib/docker /etc/docker
   apt install -y docker.io docker-compose
   ```

### 方案 B：改用独立 VM

不受 LXC 限制，完全支持 Docker。

### 启动 Nacos（Docker 就绪后）

```bash
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

# 等待 60-90 秒后验证
sleep 90 && curl http://localhost:8848/nacos/v1/ns/operator/metrics
# 控制台：http://192.168.13.123:8080/next/（账号 nacos/manpou）
```

### 启动 Kafka（Docker 就绪后）

```bash
# 1. 生成 UUID（只需一次）
UUID=$(docker run --rm --privileged apache/kafka:3.8.0 \
  /opt/kafka/bin/kafka-storage.sh random-uuid)

# 2. 创建配置
docker run --rm --privileged apache/kafka:3.8.0 \
  cat /opt/kafka/config/kraft/server.properties \
  | sed 's|log.dirs=/tmp/kraft-combined-logs|log.dirs=/kafka/kraft-combined-logs|' \
  > /tmp/kafka-kraft.properties

# 3. 创建 volume 并授权
docker volume create kafka-data
docker run --rm --privileged -v kafka-data:/kafka --user root \
  apache/kafka:3.8.0 bash -c \
  'mkdir -p /kafka/kraft-combined-logs && chown 1000:1000 /kafka/kraft-combined-logs'

# 4. 格式化存储（只需一次）
docker run --rm --privileged \
  -v kafka-data:/kafka \
  -v /tmp/kafka-kraft.properties:/opt/kafka/config/kraft/server.properties:ro \
  --user root apache/kafka:3.8.0 bash -c \
  'chown appuser:appuser /kafka/kraft-combined-logs && \
   su appuser -c "/opt/kafka/bin/kafka-storage.sh format \
   -t $UUID -c /opt/kafka/config/kraft/server.properties --ignore-formatted"'

# 5. 启动
docker run -d --name kafka \
  --restart unless-stopped --privileged \
  -p 9092:9092 \
  -v kafka-data:/kafka \
  -v /tmp/kafka-kraft.properties:/opt/kafka/config/kraft/server.properties:ro \
  apache/kafka:3.8.0 \
  /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/kraft/server.properties
```

---

## 七、部署检查清单

```bash
# 1. 目录完整性
find /opt/ManpouChinaSystem -maxdepth 2 -type d | sort

# 2. Git 历史
git -C /opt/ManpouChinaSystem log --oneline -3

# 3. Maven 源码数量（期望：297）
find /opt/ManpouChinaSystem/apps/manpou-allinone/src/main/java -name "*.java" | wc -l

# 4. JAR 是否存在
ls /opt/ManpouChinaSystem/apps/manpou-allinone/target/*.jar
ls /opt/ManpouChinaSystem/apps/user-service/target/*.jar

# 5. 端口占用
ss -tlnp | grep -E "18090|18081|13000"

# 6. 健康检查
curl -s http://localhost:18090/actuator/health
curl -s http://localhost:18081/actuator/health
```

---

## 七、user-service 数据库配置

user-service 连接 MySQL 时的用户名/密码与 manpou-allinone 不同：

```yaml
# apps/user-service/src/main/resources/application.yml
datasource:
  url: jdbc:mysql://192.168.13.202:23306/user_service?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
  username: manpou
  password: manpou203
```

**数据库和用户初始化（MySQL 服务器上执行）：**
```sql
CREATE DATABASE IF NOT EXISTS user_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'manpou'@'%' IDENTIFIED BY 'manpou203';
GRANT ALL PRIVILEGES ON user_service.* TO 'manpou'@'%';
FLUSH PRIVILEGES;
```

---

## 八、故障排查

### manpou-allinone 启动失败——Unable to find main class

**原因：** `src/main/java/` 源码目录缺失。

**解决：** 重新推送 manpou-allinone 源码。

### user-service 启动失败——Unable to read meta-data for ConfigSourceAutoConfiguration

**原因：** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
引用了不存在的类 `com.manpou.user.common.config.ConfigSourceAutoConfiguration`。

**解决：** 修正为 `com.manpou.common.config.ConfigSourceAutoConfiguration`（来自 manpou-common）。

### tar --exclude 参数无效

**原因：** `--exclude` 必须在位置参数之前。

**解决：** `tar --exclude='node_modules' -C /path -czf - .`

### Maven 找不到父 POM

**原因：** `apps/java-service/pom.xml` 未推送。

**解决：** 先推送 java-service，再 `mvn -N install`。

### user-service Flyway checksum mismatch V10

**原因：** 数据库中 V10 迁移 checksum（1888470303）与本地 JAR 内文件 checksum（458859435）不一致。

**解决（方案 A）：** 手动修正数据库 checksum：
```sql
UPDATE flyway_schema_history SET checksum = 458859435 WHERE version = '10';
```

**注意：** `repair-at-start: true` 在 Flyway 校验失败后才会执行 repair，无法在同一次启动中自动修复 checksum 不匹配问题。推荐直接用 MySQL UPDATE 修正。

### user-service health DOWN —— Redis 连接失败

**原因：** user-service 启动时未传入 `REDIS_PASSWORD` 环境变量，Redis 使用默认空密码连接失败。

**解决：** 启动时指定密码：
```bash
export REDIS_PASSWORD=redis123
nohup java -Xms128m -Xmx256m -jar target/user-service-1.0.0-SNAPSHOT.jar \
  --server.port=18081 > /opt/ManpouChinaSystem/logs/user-service.log 2>&1 &
```

### SSH 密码认证（git-bash Windows）

**方法：** 使用 `SSH_ASKPASS` 环境变量指向一个可执行脚本：
```bash
# 创建 askpass.sh（必须是可执行的 bash 脚本，不能是纯文本）
cat > askpass.sh << 'EOF'
#!/bin/bash
printf 'manpou203\n'
EOF
chmod +x askpass.sh

# 使用
SSH_ASKPASS=./askpass.sh DISPLAY=dummy ssh root@192.168.13.123 "cmd"
```

纯文本文件（如 `pass.txt`）不能用作 `SSH_ASKPASS`，会被当作 shell 脚本执行导致错误。
