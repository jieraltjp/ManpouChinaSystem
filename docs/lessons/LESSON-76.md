# Lesson 76: allinone 重启流程——JAR 锁定的正确处理

> **发现日期**: 2026-05-08
> **项目**: ManpouChinaSystem
> **教训**: 所有inone 代码变更后必须重启，重启前必须先停进程再删 JAR

---

## 问题

allinone JWT filter 修复代码已提交，但重启 `restart-all.bat` 后权限仍不生效。

**表面症状**：jiangjie (VIEWER) 仍可调用 DELETE /api/v1/procurements/1（无 `procurement:delete` 权限）

**根因**：restart-all.bat 的 `start-all.bat` 第 76 行使用 `-q` 静默打包，且未确保旧进程被杀死，导致启动的仍是旧 JAR。

---

## 根因分析（两条）

### 根因 1：JAR 被进程锁定，`-q` 掩盖 repackage 失败

`start-all.bat` 打包命令：
```bat
call mvn package -DskipTests -q -Drevision=1.0.0
```

`mvn spring-boot:repackage` 需要：
1. 创建新 thin JAR：`manpou-allinone-1.0.0-SNAPSHOT.jar`
2. **重命名旧 thin JAR** 为 `.original`
3. 创建新 fat JAR 替换原文件名

若进程持有 JAR 文件句柄，步骤 2 失败。但 `-q` 把错误吞掉，只报 BUILD SUCCESS → 启动的是旧 fat JAR（编译成功后遗留的），新代码从未进入。

### 根因 2：PowerShell PID 查找 + 杀进程不彻底

`start-all.bat` 杀进程：
```bat
for /f ... ('powershell ... Get-NetTCPConnection -LocalPort 18090 ...') do (
    powershell ... Stop-Process -Id %%i -Force
)
```

若端口处于 TIME_WAIT 状态，`OwningProcess = 0`，循环退出但进程实际未死。且杀完后未等待 Windows 文件句柄释放（通常需 2-5 秒）。

---

## 正确重启流程（手动操作）

```bash
# 1. 找到 allinone PID（不能用端口号，因为 TIME_WAIT 时端口空）
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { \$_.CommandLine -like '*manpou-allinone*' } | Select-Object ProcessId,CommandLine"

# 2. 杀掉进程
powershell -NoProfile -Command "Stop-Process -Id <PID> -Force"

# 3. 等 Windows 释放文件句柄（必须）
sleep 5

# 4. 删除可能被锁的 JAR
powershell -NoProfile -Command "Remove-Item target/manpou-allinone-1.0.0-SNAPSHOT.jar -Force"

# 5. 重新打包（禁止 -q）
cd apps/manpou-allinone && mvn package -DskipTests

# 6. 验证 MANIFEST.MF 有 Main-Class
unzip -p target/manpou-allinone-1.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF | grep Main-Class

# 7. 启动
java -Xms512m -Xmx1024m -jar target/manpou-allinone-1.0.0-SNAPSHOT.jar \
  --server.port=18090 --spring.profiles.active=local &

# 8. 验证
sleep 15 && curl http://localhost:18090/actuator/health
```

---

## 干跑验证（代码变更后必须执行）

```bash
# 获取 jiangjie token（VIEWER 角色，无写权限）
TOKEN=$(curl -s -X POST http://localhost:18081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jiangjie","password":"123456"}' | jq -r '.data.accessToken')

# 无写权限 → 403
curl -w "\nHTTP: %{http_code}" -X DELETE \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:18090/api/v1/procurements/999
# 期望: HTTP 403 auth.forbidden

# 有读权限 → 200
curl -w "\nHTTP: %{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:18090/api/v1/demands?page=0&size=1"
# 期望: HTTP 200
```

---

## 预防

| 规范 | 做法 |
|------|------|
| 脚本禁止 `-q` | 去掉 `-q`，让 repackage 错误可见 |
| 重启前确认进程死透 | 用 `Get-CimInstance` 双重验证，不依赖端口 |
| 打包前等句柄释放 | `sleep 5` 后再 rm，避免 Device busy |
| 代码变更后必须干跑 | 用 jiangjie token 测 403 + 200 组合 |
| 写权限注解 | `@PreAuthorize("hasAuthority('module:delete')")` |
