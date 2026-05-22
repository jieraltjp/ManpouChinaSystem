# 公网服务器部署指南（8.136.139.224）

> 适用于 8.136.139.224 首次部署和后续更新。

---

## 1. 服务器当前状态

| 项目 | 值 |
|---|---|
| OS | Ubuntu 22.04 LTS |
| Java | OpenJDK 21.0.10 (`/usr/lib/jvm/java-1.21.0-openjdk-amd64`) |
| Maven | 3.6.3 |
| MySQL | 8.0.46（本地，端口 3306） |
| 部署目录 | `/opt/manpou/` |

**运行中的服务：**

| 服务 | 端口 | 状态 | 配置文件 |
|---|---|---|---|
| manpou-allinone | 18090 | 运行中 | `/opt/manpou/conf/allinone.yml` |
| user-service | 18081 | 运行中 | `/opt/manpou/conf/user-service.yml` |
| MySQL | 3306 | 运行中 | - |

---

## 2. 完整部署流程

### 2.1 服务器初始化（首次）

#### MySQL 8 安装（替换 MariaDB）

如果服务器上已有 MariaDB，必须先卸载再安装 MySQL 8：

```bash
# 停止现有 MySQL/MariaDB
systemctl stop mysql 2>/dev/null || systemctl stop mariadb 2>/dev/null
systemctl disable mysql 2>/dev/null || systemctl disable mariadb 2>/dev/null

# 完全卸载 MariaDB（否则与 MySQL 8 包冲突）
apt-get purge -y mariadb-server mariadb-client mariadb-common galera-4 \
  dirmngr aptirm '?name(?mariadb)' '?name(?mysql*)' 2>/dev/null
rm -rf /var/lib/mysql /etc/mysql

# 添加 MySQL 8 APT 源
wget https://repo.mysql.com/mysql-apt-config_0.8.29-1_all.deb
dpkg -i mysql-apt-config_0.8.29-1_all.deb
# 选择 MySQL 8.0（不用 interactive prompt，手动创建源文件）
echo "deb [trusted=yes] http://repo.mysql.com/apt/ubuntu/ jammy mysql-8.0" \
  > /etc/apt/sources.list.d/mysql.list
apt-get update -y

# 安装 MySQL 8（无交互）
DEBIAN_FRONTEND=noninteractive apt-get install -y mysql-community-server

# 初始化（如果 data directory 为空）
# mysqld --initialize-insecure --user=mysql --datadir=/var/lib/mysql
# systemctl start mysql

# 设置 root 密码
mysql -uroot -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'manpou23306'; FLUSH PRIVILEGES;"
```

**注意：如果 MySQL 8 无法启动（"Tablespace flags invalid" 错误），说明 `/var/lib/mysql` 中有 MariaDB 的旧数据文件：**

```bash
mv /var/lib/mysql /var/lib/mysql_old_maria
mkdir /var/lib/mysql
chown mysql:mysql /var/lib/mysql
mysqld --initialize-insecure --user=mysql --datadir=/var/lib/mysql
systemctl start mysql
mysql -uroot -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'manpou23306'; FLUSH PRIVILEGES;"
```

#### 创建数据库

```bash
mysql -uroot -pmanpou23306 -e "
CREATE DATABASE IF NOT EXISTS manpou CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS user_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
"
```

### 2.2 数据库迁移

**方式 A：直接导入（如果能访问源数据库）**

```bash
# 从源 MySQL 导出
mysqldump -h192.168.13.202 -P23306 -uroot -pmanpou23306 --databases manpou > /tmp/manpou_full.sql

# 导入到公网服务器
mysql -uroot -pmanpou23306 < /tmp/manpou_full.sql
```

**方式 B：通过 GitHub 中转（大文件/JAR 传输）**

```bash
# 在本地开发机推送到 GitHub（注意先移除敏感文件）
git filter-repo --invert-paths --path apps/web/.env --invert-paths --path apps/manpou-allinone/src/main/resources/application-local.yml
git push --force origin main

# 在服务器克隆
cd /opt && git clone https://github.com/jieraltjp/ManpouChinaSystem.git manpou-build
```

### 2.3 在服务器编译

```bash
export JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
cd /opt/manpou-build

# 编译共享库
cd libs/manpou-common && mvn install -DskipTests -q && cd ..

# 编译 allinone
cd apps/manpou-allinone && mvn package -DskipTests

# 编译 user-service
cd ../user-service && mvn package -DskipTests

# 复制 JAR 到部署目录
mkdir -p /opt/manpou/apps/manpou-allinone/target
mkdir -p /opt/manpou/apps/user-service/target
cp /opt/manpou-build/apps/manpou-allinone/target/manpou-allinone-1.0.0-SNAPSHOT.jar \
   /opt/manpou/apps/manpou-allinone/target/manpou-allinone.jar
cp /opt/manpou-build/apps/user-service/target/user-service-1.0.0-SNAPSHOT.jar \
   /opt/manpou/apps/user-service/target/user-service.jar
```

### 2.4 写入配置文件（重要）

**使用 heredoc 写法（避免 shell 变量展开问题）：**

```bash
# allinone.yml
cat << 'ENDFILE' > /opt/manpou/conf/allinone.yml
spring:
  application:
    name: manpou-allinone
  datasource:
    url: jdbc:mysql://localhost:3306/manpou?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&connectionAttributes=none
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
ENDFILE

# user-service.yml
cat << 'ENDFILE' > /opt/manpou/conf/user-service.yml
server:
  port: 18081
spring:
  application:
    name: user-service
  datasource:
    url: jdbc:mysql://localhost:3306/manpou?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
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
    open-in-view: false
    hibernate:
      ddl-auto: none
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
  data:
    redis:
      host: localhost
      port: 6379
      password: redis123
      database: 0
      timeout: 5000
jwt:
  access-token-ttl-seconds: 86400
  key:
    algorithm: RS256
    kid: e57043d2
    private: ${JWT_PRIVATE_KEY:}
    public: ${JWT_PUBLIC_KEY:}
logging:
  level:
    root: INFO
    com.manpou.user: INFO
app:
  audit-log:
    secret: ${AUDIT_LOG_SECRET:CHANGE-ME-AUDIT-SECRET}
  config:
    source: properties
ENDFILE
```

### 2.5 写入启动脚本（使用 heredoc）

```bash
# start-allinone.sh
cat << 'ENDFILE' > /opt/manpou/scripts/start-allinone.sh
#!/bin/bash
JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64
PATH=$JAVA_HOME/bin:$PATH
LOG=/opt/manpou/logs/manpou-allinone.log
PIDFILE=/opt/manpou/logs/manpou-allinone.pid
mkdir -p /opt/manpou/logs
if [ -f "$PIDFILE" ]; then
  PID=$(cat "$PIDFILE")
  if kill -0 $PID 2>/dev/null; then
    echo "Stopping manpou-allinone (PID $PID)..."
    kill $PID
    sleep 3
  fi
fi
cd /opt/manpou/apps/manpou-allinone
nohup java -Xms256m -Xmx512m -jar target/manpou-allinone.jar \
  --server.port=18090 \
  --spring.config.location=/opt/manpou/conf/allinone.yml \
  --spring.cloud.nacos.config.enabled=false \
  --spring.cloud.nacos.discovery.enabled=false \
  --spring.cloud.alibaba.cloud.enabled=false \
  > "$LOG" 2>&1 &
echo $! > "$PIDFILE"
echo "manpou-allinone started (PID $(cat $PIDFILE))"
ENDFILE

# start-userservice.sh
cat << 'ENDFILE' > /opt/manpou/scripts/start-userservice.sh
#!/bin/bash
JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64
PATH=$JAVA_HOME/bin:$PATH
LOG=/opt/manpou/logs/user-service.log
PIDFILE=/opt/manpou/logs/user-service.pid
mkdir -p /opt/manpou/logs
if [ -f "$PIDFILE" ]; then
  PID=$(cat "$PIDFILE")
  if kill -0 $PID 2>/dev/null; then
    echo "Stopping user-service (PID $PID)..."
    kill $PID
    sleep 3
  fi
fi
cd /opt/manpou/apps/user-service
nohup java -Xms128m -Xmx256m -jar target/user-service.jar \
  --server.port=18081 \
  --spring.config.location=/opt/manpou/conf/user-service.yml \
  --spring.cloud.nacos.config.enabled=false \
  --spring.cloud.nacos.discovery.enabled=false \
  --spring.cloud.alibaba.cloud.enabled=false \
  > "$LOG" 2>&1 &
echo $! > "$PIDFILE"
echo "user-service started (PID $(cat $PIDFILE))"
ENDFILE

chmod +x /opt/manpou/scripts/start-allinone.sh
chmod +x /opt/manpou/scripts/start-userservice.sh
```

### 2.6 启动服务

```bash
# 防火墙开放端口（首次）
ufw allow 18090/tcp comment 'manpou-allinone'
ufw allow 18081/tcp comment 'user-service'
ufw allow 13000/tcp comment 'web-frontend'

# 启动
bash /opt/manpou/scripts/start-allinone.sh
bash /opt/manpou/scripts/start-userservice.sh

# 等待 40 秒
sleep 40

# 验证
curl -s http://localhost:18090/actuator/health
curl -s http://localhost:18081/actuator/health
curl -s -X POST http://localhost:18081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

---

## 3. 更新部署（代码更新后）

### 3.1 推送代码到 GitHub

```bash
# 在本地开发机
git add . && git commit -m "update" && git push origin main
```

### 3.2 在服务器重新编译并重启

```bash
# SSH 到服务器
ssh root@8.136.139.224

# 停止服务
bash /opt/manpou/scripts/start-allinone.sh stop  # 或手动 kill
pkill -f manpou-allinone
pkill -f user-service
sleep 3

# 重新拉取代码
cd /opt/manpou-build && git pull origin main

# 重新编译
export JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64
cd /opt/manpou-build/libs/manpou-common && mvn install -DskipTests -q && cd ..
cd /opt/manpou-build/apps/manpou-allinone && mvn package -DskipTests
cd ../user-service && mvn package -DskipTests

# 复制新 JAR
cp /opt/manpou-build/apps/manpou-allinone/target/manpou-allinone-1.0.0-SNAPSHOT.jar \
   /opt/manpou/apps/manpou-allinone/target/manpou-allinone.jar
cp /opt/manpou-build/apps/user-service/target/user-service-1.0.0-SNAPSHOT.jar \
   /opt/manpou/apps/user-service/target/user-service.jar

# 重启
bash /opt/manpou/scripts/start-allinone.sh
bash /opt/manpou/scripts/start-userservice.sh

# 验证
sleep 40 && curl -s http://localhost:18090/actuator/health
```

---

## 4. 关键经验教训（从部署中总结）

### 4.1 shell 变量展开问题

**问题**：用 Python 的 `repr()` 写启动脚本时，`$LOG`、`$PIDFILE` 等变量被 shell 展开为空字符串。

**解决**：使用 `cat << 'ENDFILE'` heredoc（单引号 ENDFILE 分隔符）防止任何变量展开。

### 4.2 Nacos spring.config.import 错误

**症状**：`Application failed to start - No spring.config.import property has been defined`

**原因**：JAR 内部 `application.yml` 有 `spring.config.import=nacos:` 设置，但 Nacos 服务不存在。

**解决**：
1. 不在外部配置文件使用 `spring.config.import` 属性
2. 通过 CLI 参数禁用 Nacos：
   ```bash
   --spring.cloud.nacos.config.enabled=false
   --spring.cloud.nacos.discovery.enabled=false
   --spring.cloud.alibaba.cloud.enabled=false
   ```

### 4.3 spring.config.location 行为

**关键**：当 `--spring.config.location` 指向**文件**（不是目录）时，该文件**替换**默认配置源（而非追加）。JAR 内部的 `application.yml` 不会被加载。

**解决**：确保外部配置文件包含**所有必要的属性**（datasource、jpa 等）。

### 4.4 profile 默认值

JAR 内部 `application.yml` 默认 profile 是 `local`，**不是** `development`。即使传了 `--spring.config.location`，如果不显式设置 profile，JAR 内部 `profiles.active: local` 仍生效。

### 4.5 GitHub secret scanning 阻断 push

**问题**：推送到 GitHub 时，AKID/Secret 等凭据被检测并阻断。

**解决**：
```bash
git filter-repo --invert-paths --path <文件路径>
git push --force origin main
```

### 4.6 SFTP 大文件上传失败

**问题**：JAR（91MB）通过 SFTP 上传时连接重置。

**解决**：改用 GitHub 中转 —— 推送到 GitHub private repo → 服务器 `git clone`。

---

## 5. 架构说明

```
公网服务器 8.136.139.224
├── MySQL 8 (localhost:3306)
│   ├── manpou 数据库（allinone 数据）
│   └── user_service 数据库（user-service 数据）
├── manpou-allinone (18090)
│   ├── 业务逻辑（发注/仓储/报关等）
│   └── 消费 user-service JWT
└── user-service (18081)
    ├── 用户/角色/权限 CRUD
    ├── JWT 签发（RS256，kid=e57043d2）
    └── AuditLog
```

**前后端通信：**

- 前端 → Vite Proxy → `/api/v1/auth` → user-service (18081)
- 前端 → Vite Proxy → `/api` → allinone (18090)
- allinone → JWT 验证 → 轮询 user-service `/api/v1/auth/keys/active/public-key`
