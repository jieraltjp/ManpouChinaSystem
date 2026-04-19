# 项目文档：配置中心与代码规范

> **文档角色**：后端开发工程师 + 架构师视角
> **对应目录**：`config/` + `config/nacos/`
> **用途**：Checkstyle 规范 + Nacos 配置模板

---

## 1. Checkstyle 代码规范

文件：`config/checkstyle/checkstyle.xml`

### 1.1 规范要点

| 检查项 | 规则 |
|--------|------|
| 命名规范 | 类名 `UpperCamelCase`，方法名 `lowerCamelCase`，常量 `UPPER_SNAKE_CASE` |
| 行长度 | 单行不超过 120 字符 |
| 缩进 | 4 空格（禁止 Tab） |
| 空行 | 方法之间 1 空行，类之间 2 空行 |
| Javadoc | 公开类/方法必须有 Javadoc |
| `this.` | 强制使用（消除歧义） |
| 魔法数 | 禁止硬编码数字，必须使用常量 |
| `final` | 类字段和方法参数强制 `final` |
| 导包 | 禁止通配符导入（`import foo.*`） |
| 块结构 | `else`/`catch` 必须使用 `{}` |

### 1.2 Maven 集成

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <configuration>
        <configLocation>config/checkstyle/checkstyle.xml</configLocation>
    </configuration>
</plugin>
```

### 1.3 使用命令

```bash
# 检查所有模块
mvn checkstyle:check

# 生成报告
mvn checkstyle:checkstyle

# IDE 集成（IDEA）
# Settings → Tools → Checkstyle → + 选择 config/checkstyle/checkstyle.xml
```

---

## 2. Nacos 配置中心

目录：`config/nacos/`

### 2.1 配置文件清单

| 文件 | data-id | 说明 |
|------|---------|------|
| `shared-common.yml` | `shared-common` | 所有服务共享配置 |
| `dev/app-service.yml` | `app-service.yml` | 服务通用配置 |
| `dev/datasource.yml` | `datasource.yml` | 数据库连接池配置 |
| `dev/redis.yml` | `redis.yml` | Redis 连接配置 |
| `dev/kafka.yml` | `kafka.yml` | Kafka 消费者配置 |

### 2.2 共享配置（shared-common）

```yaml
# config/nacos/shared-common.yml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss.SSS
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false

logging:
  level:
    root: INFO
    com.manpou: DEBUG
  pattern:
    console: '{"time":"%d{yyyy-MM-dd HH:mm:ss.SSS}","level":"%level","msg":"%msg"}%n'

management:
  endpoints:
    web.exposure.include: health,info,metrics
  endpoint:
    health.show-details: when_authorized
```

### 2.3 接入步骤

```bash
# 1. 启动 Nacos
docker compose -f docker/compose.yaml up -d nacos

# 2. 初始化配置
./scripts/init-config.sh

# 3. 服务启用 Nacos（取消禁用）
# 各服务 application.yml 中：
spring:
  cloud:
    nacos:
      config:
        enabled: true          # 改为 true
      discovery:
        enabled: true          # 改为 true
    alibaba:
      cloud:
        enabled: true           # 改为 true

# 4. 重启服务
```

---

## 3. 环境隔离

| 环境 | Nacos Namespace | 说明 |
|------|----------------|------|
| `dev` | `dev` | 本地开发 |
| `staging` | `staging` | 预发布 |
| `prod` | `prod` | 生产 |

---

## 4. 文件清单

```
config/
├── checkstyle/
│   └── checkstyle.xml     # 代码规范检查配置
└── nacos/
    ├── shared-common.yml   # 共享配置模板
    └── dev/
        ├── app-service.yml # 服务通用配置
        ├── datasource.yml  # 数据库连接
        ├── redis.yml      # Redis 连接
        └── kafka.yml       # Kafka 配置
```

---

## 5. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/pro/00-root-project.md` | 项目全局概览 |
| `docs/pro/12-docker-compose.md` | Nacos 启动方式 |
| `docs/pro/15-ci-cd.md` | CI 中的 Checkstyle 检查 |
| `docs/role/04-后端开发工程师视角分析.md` | 开发规范 |
