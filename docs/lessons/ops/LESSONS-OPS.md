# 工程教训 — 运维/部署（Build / Deploy / Environment）

> 项目：ManpouChinaSystem
> 覆盖范围：Maven 打包 / 进程管理 / 环境配置 / 密钥管理
> Lesson 编号：7–9, 17–18, 20, 26–28, 76（共 10 条）

---

## 目录

- [Lesson 7: Windows 环境脚本必须用 Git Bash 执行](#lesson-7-windows-环境脚本必须用-git-bash-执行)
- [Lesson 8: Flyway 禁用后 V4 迁移数据不会自动导入](#lesson-8-flyway-禁用后-v4-迁移数据不会自动导入)
- [Lesson 9: JWT 私钥路径必须在 classpath 和文件系统双重保险](#lesson-9-jwt-私钥路径必须在-classpath-和文件系统双重保险)
- [Lesson 17: 环境差异（dev/staging/prod）配置必须标准化](#lesson-17-环境差异devstagingprod-配置必须标准化)
- [Lesson 18: private.pem 仅存于签发中心服务，禁止全量分发](#lesson-18-privatepem-仅存于签发中心服务禁止全量分发)
- [Lesson 20: 分页约定 page=0 vs page=1 必须在开发前锁定](#lesson-20-分页约定-page0-vs-page1-必须在开发前锁定)
- [Lesson 26: Maven `-q` 静默模式会吞掉 spring-boot:repackage 失败，导致 JAR 无法启动](#lesson-26-maven--q-静默模式会吞掉-spring-bootrepackage-失败导致-jar-无法启动)
- [Lesson 27: 依赖 scope 必须与实际运行环境匹配（H2 test scope vs runtime）](#lesson-27-依赖-scope-必须与实际运行环境匹配h2-test-scope-vs-runtime)
- [Lesson 28: 编译与启动必须分离，禁止在脚本中隐蔽失败](#lesson-28-编译与启动必须分离禁止在脚本中隐蔽失败)
- [Lesson 76: allinone 重启流程——JAR 锁定的正确处理](#lesson-76-allinone-重启流程jar-锁定的正确处理)

---

## Lesson 7: Windows 环境脚本必须用 Git Bash 执行

### 问题

脚本在 CMD/PowerShell 报错，不识别 `./` 和 `#!/usr/bin/env bash` 语法。

### 解决方案

始终使用 Git Bash：
```bash
# ✅ 正确
./scripts/start-all.sh

# ❌ 错误：直接在 CMD 中运行 .sh
```

---

## Lesson 8: Flyway 禁用后 V4 迁移数据不会自动导入

### 问题

500 条工厂数据写在 `V4__factory_migration.sql` 中，但 Flyway 全局禁用，数据从不导入。

### 根因

```
# application.yml
flyway.enabled: false
```

Hibernate `ddl-auto: update` 只管表结构，不管数据。

### 预防

- Flyway 数据迁移（INSERT）与表结构管理（CREATE/ALTER）是两回事
- 禁用 Flyway 时，数据初始化必须走 `DevTestDataInitializer`

---

## Lesson 9: JWT 私钥路径必须在 classpath 和文件系统双重保险

### 问题

启动报错：`RSA 私钥未找到`

### 解决方案

优先级：`classpath` > 文件系统：

```java
// 1. 先查 classpath（随 jar 打包，部署一致性高）
// 2. 再查文件系统（本地开发 / 自定义路径）
```

### 预防

- 密钥/证书类资源始终支持双路径
- 生产部署时 classpath 优先

---

## Lesson 17: 环境差异（dev/staging/prod）配置必须标准化

### 问题

```
# 旧配置（混乱）
vite.config.ts proxy → 192.168.12.198:18090  # VPN 地址，本地无法访问

# 新配置（正确）
vite.config.ts proxy → localhost:18080  # API Gateway
```

### 解决方案

```
开发环境（localhost）：
  前端 → localhost:18080 (Gateway) → localhost:18090 (manpou-allinone)

环境配置优先级：
  1. .env.local（本地覆盖，最高优先级）
  2. .env.development（开发默认值）
  3. vite.config.ts proxy 配置（仅 dev）
  4. .env.production（生产默认值）
```

---

## Lesson 18: private.pem 仅存于签发中心服务，禁止全量分发

### 问题

`private.pem` 被全量分发到所有服务，JWT 签名密钥泄露风险。

### 解决方案

JWT 私钥只存于签发中心服务（如 `user-service`），其他服务只持有公钥。

### 预防

- 密钥分发走 Secret Manager，禁止文件系统复制
- 生产环境禁止将私钥提交到代码仓库

---

## Lesson 20: 分页约定 page=0 vs page-1 必须在开发前锁定

### 问题

前端用 page=1，后端用 page=0（Spring Data 的 Pageable 默认从 0 开始），第一页数据永远是第二页。

### 解决方案

在 `SPEC-*.md` 中明确锁定分页约定：
```
GET /api/v1/demands?page=0&pageSize=20   ← 统一用 page=0
```

前端在调用前做转换：
```typescript
// 前端用 0-based
const res = await demandApi.list({ page: page - 1, pageSize: 20 })
```

### 预防

- 分页约定在开发前锁定，禁止中途变更
- 单元测试覆盖第一页和第二页的数据边界

---

## Lesson 26: Maven `-q` 静默模式会吞掉 spring-boot:repackage 失败，导致 JAR 无法启动

### 问题

`mvn package -DskipTests -q` 报告 BUILD SUCCESS，但 `java -jar` 启动失败：

```
Error: Unable to access jarfile target/manpou-allinone-1.0.0-SNAPSHOT.jar
```

MANIFEST.MF 缺失 `Main-Class` 和 `Start-Class`。

### 根因

**两条错误叠加：**

1. JAR 文件被前一个 `java -jar` 进程锁住，`spring-boot:repackage` 覆盖失败
2. `-q` 标志把所有输出吞掉，repackage 失败被掩盖

```
mvn package -DskipTests -q
  → spring-boot:repackage 失败（文件锁）
  → 错误被 -q 吞掉
  → BUILD SUCCESS（因为 compile 成功了）
  → 产物：无 Main-Class 的普通 JAR → java -jar 失败
```

### 诊断方法

```bash
# 检查 MANIFEST.MF
unzip -p target/manpou-allinone-*.jar META-INF/MANIFEST.MF | grep "Main-Class"

# 检查 Spring Boot 特有文件
unzip -l target/xxx.jar | grep "BOOT-INF"

# 完整日志
mvn package -DskipTests 2>&1 | grep -E "repackage|ERROR|SUCCESS"
```

### 解决方案

```bash
# 1. 杀掉持有 JAR 锁的 Java 进程
taskkill //F //IM java.exe

# 2. 不带 -q 重新打包
mvn package -DskipTests
```

### 预防

| 规范 | 说明 |
|------|------|
| 禁止使用 `-q` | 掩盖 WARNING 和错误 |
| 打包后立即检查 MANIFEST.MF | `unzip -p target/xxx.jar META-INF/MANIFEST.MF \| grep Main-Class` |
| 打包前确保无旧进程锁 JAR | `taskkill //F //IM java.exe` |
| 构建脚本检查 exit code | `mvn package ... && echo "OK" \|\| echo "FAILED"` |

---

## Lesson 27: 依赖 scope 必须与实际运行环境匹配（H2 test scope vs runtime）

### 问题

JAR 打包成功，`java -jar` 启动时报错：

```
Caused by: java.lang.IllegalStateException: Cannot load driver class: org.h2.Driver
```

### 根因

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>   ← 不打包到 JAR
</dependency>

<!-- application.yml -->
driver-class-name: org.h2.Driver  ← 运行时需要，找不到
```

### 解决方案

移除 H2，激活 MySQL local profile：
```yaml
spring:
  profiles:
    active: local   # ← 激活 application-local.yml（MySQL 配置）
```

### 实施记录（2026-05-01）

| 操作 | 文件 | 说明 |
|------|------|------|
| MySQL | `application.yml` | `jdbc:h2:mem` → `jdbc:mysql://192.168.13.202:23306/manpou` |
| MySQL | `application-local.yml` | 新增 datasource / JPA / logging 配置 |
| MySQL | `pom.xml` | `mysql-connector-java` scope: test → compile |
| H2 | `java-service/pom.xml` | 移除 H2 依赖 |
| H2 | `manpou-allinone/pom.xml` | H2 依赖已移除 |

**状态**：✅ 已解决（commit 1df2cc6）

### 预防

- 运行时依赖不能用 `test` scope
- `spring-boot:repackage` 只打包 `compile` + `runtime` scope

---

## Lesson 28: 编译与启动必须分离，禁止在脚本中隐蔽失败

### 问题

`mvn clean package` 编译失败（`isScalar()` 方法找不到），但未被发现，JAR 打包表面成功，实际不可用。

### 根因

1. Spring Cloud Alibaba Nacos 带来了旧版 Jackson
2. `manpou-common` 源码调用了 Jackson 2.10+ 才有的 `JsonNode.isScalar()` 方法，编译时 classpath 有旧版 Jackson

### 本次修复

```java
// ❌ 旧代码
return current.isScalar() ? current.asText() : current.toString();

// ✅ 新代码（兼容所有 Jackson 版本）
return !current.isContainerNode() ? current.asText() : current.toString();
```

### 正确启动流程

```bash
# 1. 确保无旧进程锁 JAR
powershell -Command "Stop-Process -Name java -Force -ErrorAction SilentlyContinue"

# 2. 全量编译（禁止 -q）
cd apps/manpou-allinone
mvn clean package -DskipTests

# 3. 检查 MANIFEST.MF
unzip -p target/manpou-allinone-*.jar META-INF/MANIFEST.MF | grep "Main-Class"

# 4. 启动
java -Xms512m -Xmx1024m \
  -jar apps/manpou-allinone/target/manpou-allinone-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=local \
  --server.port=18090
```

### 端口约定

| 服务 | 端口 |
|------|------|
| manpou-allinone | 18090 |
| user-service | 18081 |
| web 前端 | 13000 |
| Gateway（生产） | 18080 |

### 预防

- 禁止静默编译：`mvn package` 不带任何 `-q`
- 脚本中的 `mvn ... -q` 必须替换为 `mvn ... 2>&1 | tee build.log`

---

## Lesson 76: allinone 重启流程——JAR 锁定的正确处理

### 问题

JWT filter / Controller 注解修复已提交，但 restart-all.bat 后权限仍不生效。

**根因**：`start-all.bat` 第 76 行 `mvn package -q` 静默打包，若进程持有 JAR 句柄，`spring-boot:repackage` 重命名失败被吞掉。启动的仍是旧 JAR。

### 正确流程

```bash
# 1. 找到进程（不用端口，TIME_WAIT 时端口空）
powershell -Command "Get-CimInstance Win32_Process | Where-Object { \$_.CommandLine -like '*manpou-allinone*' }"

# 2. 杀进程
powershell -Command "Stop-Process -Id <PID> -Force"

# 3. 等句柄释放（必须！）
sleep 5

# 4. 删 JAR
powershell -Command "Remove-Item target/manpou-allinone-*.jar -Force"

# 5. 打包（禁止 -q）
cd apps/manpou-allinone && mvn package -DskipTests

# 6. 启动
java -Xms512m -Xmx1024m -jar target/manpou-allinone-1.0.0-SNAPSHOT.jar \
  --server.port=18090 --spring.profiles.active=local &
```

### 干跑验证（代码变更后必须）

```bash
TOKEN=$(curl -s -X POST http://localhost:18081/api/v1/auth/login \
  -d '{"username":"jiangjie","password":"123456"}' | jq -r '.data.accessToken')

# 无权限 → 403
curl -w "\n%{http_code}" -X DELETE \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:18090/api/v1/procurements/999
# 期望: {"code":"auth.forbidden"...} HTTP 403
```

---

## 铁律总结表（运维/部署）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 7 | Windows 用 Git Bash 执行 .sh 脚本 | 脚本报错 |
| 8 | Flyway 禁用时数据初始化走 DevTestDataInitializer | 数据不导入 |
| 9 | 密钥资源走 classpath + 文件系统双路径 | 启动失败 |
| 17 | 开发环境配置走 .env.local/proxy，本地必须可运行 | 环境差异 |
| 18 | private.pem 仅存于签发中心服务，禁止全量分发 | 安全漏洞 |
| 20 | 分页约定 page=0 vs page=1 开发前锁定 | 前后端不对齐 |
| 26 | 打包禁止 `-q` + 确保无旧进程锁 JAR | JAR 不可用 |
| 76 | 代码变更后重启 allinone 必须：先停进程→等5秒→删JAR→重打包→干跑验证 | 旧 JAR 不含新代码，权限验证形同虚设 |
| 27 | 运行时依赖不能是 test scope | 启动失败 |
| 28 | 编译与启动必须分离，错误必须可见 | 错误被掩盖 |
