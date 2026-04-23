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
| 脚本无法执行 | 用 **Git Bash** 或 **WSL**，不要用 CMD / PowerShell 直接运行 |
| `mvn` 命令找不到 | 使用项目自带的 `./mvnw` 包装器 |
| 文件路径问题 | 确保项目路径无中文、无空格 |

---

## 健康检查（一键验证）

> 仅 Spring Boot 服务（18090 / 18081）支持 `/actuator/health`。
> 18081 和 13000 若无 actuator，请用端口可用性判断。

```bash
#!/usr/bin/env bash
is_port_used() {
    powershell -Command "Get-NetTCPConnection -LocalPort $1 -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty LocalPort" 2>/dev/null | grep -q "$1"
}

for port in 18090 18081 13000; do
    if is_port_used "$port"; then
        echo "✓ :${port} 端口已占用"
    else
        echo "✗ :${port} 未启动"
    fi
done
```
