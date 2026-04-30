# 故障排查手册

---

## 服务启动失败

### 症状：端口被占用（port already in use）

**原因：** 上次启动的进程未正常关闭。

**排查：**
```bash
# Windows PowerShell
powershell -Command "Get-NetTCPConnection -LocalPort 18090 | Format-List"
```

**解决：** 重新执行停止再启动：
```bash
./scripts/stop-all.sh
./scripts/start-all.sh
```
脚本会自动检测并 kill 占用端口的进程。

---

### 症状：提示 JAR 未找到，编译也失败

**原因：** Maven 离线模式 / 网络不通 / `pom.xml` 解析错误。

**解决：**
```bash
cd apps/manpou-allinone
mvn package -DskipTests
```
确认编译成功后，再运行 `./scripts/start-all.sh`。

---

### 症状：前端 npm ERR

**原因：** `node_modules` 损坏或 package-lock.json 冲突。

**解决：**
```bash
cd apps/web
rm -rf node_modules package-lock.json
npm install
```

---

## Docker 基础设施异常

### 症状：Nacos 无法访问（localhost:8848）

**排查：**
```bash
docker ps --filter "name=nacos"
```

**解决：**
```bash
docker compose -f docker/compose.yaml up -d
# 等待 15 秒后验证：
curl -sf http://localhost:8848/nacos/v1/console/health/readiness
```

---

### 症状：数据库连接失败

**排查：**
```bash
docker ps --filter "name=mysql"
docker logs $(docker ps --filter "name=mysql" -q) 2>&1 | grep -i "access denied\|connection refused\|unknown host"
```

**解决：**
```bash
# 重启 MySQL 容器
docker compose -f docker/compose.yaml restart mysql

# 若仍有问题，完全重建（不删除命名卷，需清数据时加 -v）：
docker compose -f docker/compose.yaml up -d --build mysql
```

---

## Git 拉取冲突

### 症状：`Merge conflict` 或 `git pull` 失败

**原因：** 本地脚本被修改后与远程冲突。

**解决（放弃本地脚本修改）：**
```bash
git stash          # 暂存本地修改（如果有重要改动先备份）
git pull origin main
git stash drop
```

**解决（保留本地脚本修改）：**
```bash
git stash push -m "local script changes"
git pull origin main
git stash pop      # 手动解决冲突后保留
```

---

## PID 文件问题

### 症状：`PID file not found`

**原因：** 服务是通过 IDE 或其他方式启动的，不在脚本管控范围内。

**解决：**
```bash
./scripts/stop-all.sh   # 按端口强制 kill，清理残留 PID 文件
./scripts/start-all.sh
```

---

## Windows 特殊问题

| 症状 | 解决方法 |
|---|---|
| 脚本无法执行 | Windows 用 **Git Bash** 或 **WSL**，不要用 CMD / PowerShell 直接运行 |
| `mvn` 命令找不到 | 使用项目自带的 `./mvnw` 包装器 |
| 文件路径问题 | 确保项目路径无中文、无空格 |

---

## Proxmox LXC 特殊问题

### 症状：Docker/Podman 安装后无法运行容器

**原因：** Proxmox LXC 容器内核禁止 `mount /proc`，容器运行时无法启动任何容器。

**表现：**
```
Error: error mounting "proc" to rootfs at "/proc": permission denied
AppArmor: Permission denied (running apparmor_parser)
```

**解决路径 A：启用 LXC 嵌套（推荐）**

1. 在 Proxmox Web UI 中停止目标 LXC 容器
2. 编辑 → 选项 → **嵌套** → 改为 `1（启用）`
3. 重启 LXC
4. 重装 Docker：

```bash
# 清理旧配置
apt purge -y docker.io docker-compose podman
rm -rf /var/lib/docker /etc/docker /var/run/docker* /var/lib/containerd

# 重装
apt install -y docker.io docker-compose
systemctl start docker
docker run hello-world   # 验证
```

**解决路径 B：改为独立 VM**

在 Proxmox 上创建完整虚拟机（不是 LXC），不受容器嵌套限制。

**解决路径 C：仅用阶段 A 方式开发**

不依赖容器基础设施，后端直连远程 MySQL（`development` profile），本地只装 Redis。Phase 0 单体无需 Nacos。

---

### 症状：Docker 容器拉不起来（`--privileged` 仍报 permission denied）

**原因：** AppArmor 的 `docker-default` profile 阻止了 `--privileged` 容器启动。

**表现：** `apparmor_parser: Unable to replace "docker-default". Permission denied`

**解决：** 所有 Docker 容器必须加 `--privileged` 标志（这是 LXC + AppArmor 的 workaround）：

```bash
docker run --privileged ...
```

如果仍失败，先卸载 AppArmor profiles：

```bash
aa-teardown
# 或
rm /etc/apparmor.d/docker-default
touch /etc/apparmor.d/abi/3.0
systemctl restart docker
```

---

### 症状：Kafka 容器反复重启（`No readable meta.properties files found`）

**原因：** Kafka KRaft 模式首次启动必须先格式化存储目录（生成 `meta.properties`），否则启动失败。

**解决：**

```bash
# 1. 生成 KRaft cluster UUID
UUID=$(docker run --rm --privileged apache/kafka:3.8.0 \
  /opt/kafka/bin/kafka-storage.sh random-uuid)

# 2. 创建自定义 server.properties（使用持久化 volume）
docker run --rm --privileged apache/kafka:3.8.0 \
  cat /opt/kafka/config/kraft/server.properties \
  | sed 's|log.dirs=/tmp/kraft-combined-logs|log.dirs=/kafka/kraft-combined-logs|' \
  > /tmp/kafka-kraft.properties

# 3. 创建目录并授权
docker run --rm --privileged \
  -v kafka-data:/kafka \
  --user root \
  apache/kafka:3.8.0 \
  bash -c 'mkdir -p /kafka/kraft-combined-logs && chown 1000:1000 /kafka/kraft-combined-logs'

# 4. 格式化存储
docker run --rm --privileged \
  -v kafka-data:/kafka \
  -v /tmp/kafka-kraft.properties:/opt/kafka/config/kraft/server.properties:ro \
  --user root \
  apache/kafka:3.8.0 \
  bash -c 'chown appuser:appuser /kafka/kraft-combined-logs && \
    su appuser -c "/opt/kafka/bin/kafka-storage.sh format \
    -t $UUID -c /opt/kafka/config/kraft/server.properties --ignore-formatted"'

# 5. 启动 Kafka（使用持久化 volume）
docker run -d --name kafka \
  --restart unless-stopped \
  --privileged \
  -p 9092:9092 \
  -v kafka-data:/kafka \
  -v /tmp/kafka-kraft.properties:/opt/kafka/config/kraft/server.properties:ro \
  apache/kafka:3.8.0 \
  /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/kraft/server.properties
```

**注意：** Kafka 数据存储在 Docker volume `kafka-data`，容器重建后需重新执行步骤 1-4（保留 UUID 只需一次）。

---

### 症状：Nacos 容器无法启动（`NACOS_AUTH_TOKEN must be set` 或 JWT secret 过短）

**原因：** Nacos Docker entrypoint 要求 `NACOS_AUTH_TOKEN` 必须是 **base64 编码且解码后 ≥ 32 字节**的字符串。

**解决：** 生成正确长度的 JWT secret 并设置所有必需环境变量。Nacos 3.x 控制台路径为 `/next/`：

```bash
# 生成 32 字节随机 secret（base64 后 44 字符）
SECRET=$(dd if=/dev/urandom bs=1 count=32 2>/dev/null | base64 -w0)

docker run -d --name nacos \
  --restart unless-stopped \
  --privileged \
  -p 8848:8848 -p 9848:9848 -p 8080:8080 \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=false \
  -e NACOS_AUTH_TOKEN="$SECRET" \
  -e NACOS_AUTH_PLUGIN_NACOS_TOKEN_SECRET_KEY="$SECRET" \
  -e NACOS_AUTH_PLUGIN_NACOS_TOKEN_EXPIRE_SECONDS=18000 \
  -e NACOS_AUTH_IDENTITY_KEY=nacos123 \
  -e NACOS_AUTH_IDENTITY_VALUE=nacos456 \
  nacos/nacos-server:latest

# 控制台入口：http://host:8080/next/
# 默认账号：nacos / manpou（生产环境请修改）
# 注意：Nacos 3.x 控制台需要带 /next/ 前缀
```

---

### 症状：Maven 编译失败——git-commit-id 插件报错

**原因：** `.git` 目录不存在（tar 打包时未包含）。

**解决：**

```bash
# 在 pom.xml 中临时禁用该插件
# apps/java-service/pom.xml 中找到 <pluginManagement> → <plugins> 中的
# git-commit-id-maven-plugin，加入：
<configuration>
    <skip>true</skip>
</configuration>
```

---

### 症状：后端启动成功但数据库连不上

**原因：** `development` profile 指向远程 MySQL `192.168.13.202:23306`，该地址需网络可达。

**排查：**

```bash
# 从服务器测试 MySQL 连通性
apt install -y mysql-client
mysql -h 192.168.13.202 -P 23306 -u root -pmanpou23306 -e "SELECT 1"

# 从服务器测试 Redis 连通性
apt install -y redis-tools
redis-cli -h localhost -p 6379 -a redis123 ping
```

**解决：** 确认网络可达后，检查 MySQL 是否允许该 IP 连接。

---

## 健康检查（一键验证）

> 仅 Spring Boot 服务（18090 / 18081）支持 `/actuator/health`。
> 18081 和 13000 若无 actuator，请用端口可用性判断。

```bash
#!/usr/bin/env bash
is_port_used() {
    if command -v ss &>/dev/null; then
        ss -tlnp 2>/dev/null | grep -q ":${1} "
    elif command -v netstat &>/dev/null; then
        netstat -tlnp 2>/dev/null | grep -q ":${1} "
    else
        lsof -i ":${1}" &>/dev/null
    fi
}

for port in 18090 18081 13000; do
    if is_port_used "$port"; then
        echo "✓ :${port} 端口已占用"
    else
        echo "✗ :${port} 未启动"
    fi
done
```
