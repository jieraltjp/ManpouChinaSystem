# 项目文档：java-service（Java 服务父 POM）

> **文档角色**：后端开发工程师视角 → 共享配置
> **对应角色文档**：`docs/role/04-后端开发工程师视角分析.md`

---

## 1. 项目定位

| 维度 | 说明 |
|------|------|
| 类型 | Maven Parent POM |
| 职责 | 统一所有 Java 微服务的依赖版本、插件配置、属性定义 |
| 位置 | `apps/java-service/pom.xml` |

---

## 2. 核心配置

### 2.1 依赖管理

```xml
<!-- Spring Boot Starter Parent 统一版本 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<!-- Spring Cloud Alibaba -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-dependencies</artifactId>
    <version>2023.0.1.2</version>
</dependency>
```

### 2.2 版本属性

| 属性 | 值 |
|------|-----|
| `java.version` | 21 |
| `maven.compiler.source` | 21 |
| `maven.compiler.target` | 21 |
| `project.build.sourceEncoding` | UTF-8 |
| `lombok.version` | 1.18.44 |
| `mapstruct.version` | 1.5.5.Final |
| `jjwt.version` | 0.12.5 |
| `spring-cloud-alibaba.version` | 2023.0.1.2 |
| `archunit.version` | 1.3.2 |

### 2.3 所有子服务依赖（统一引入）

| 依赖 | ArtifactId | 说明 |
|------|-----------|------|
| Web | `spring-boot-starter-web` | REST |
| 校验 | `spring-boot-starter-validation` | @Valid |
| JPA | `spring-boot-starter-data-jpa` | ORM |
| Redis | `spring-boot-starter-data-redis` | 缓存 |
| Security | `spring-boot-starter-security` | 安全 |
| Actuator | `spring-boot-starter-actuator` | 健康检查 |
| AOP | `spring-boot-starter-aop` | 切面 |
| MySQL | `mysql-connector-j` | JDBC |
| JJWT | `jjwt-api/impl/jackson` | JWT |
| Lombok | `lombok` | 注解 |
| MapStruct | `mapstruct` | DTO 映射 |
| 测试 | `spring-boot-starter-test` | JUnit 5 |
| 测试 | `spring-security-test` | Security 测试 |
| ArchUnit | `archunit-junit5` | 架构验证 |

---

## 3. 插件配置

| 插件 | 版本 | 配置 |
|------|------|------|
| `maven-compiler-plugin` | 3.13.0 | Java 21 + Lombok + MapStruct 注解处理器 |
| `maven-surefire-plugin` | 3.2.5 | 执行测试 |
| `spring-boot-maven-plugin` | 3.2.5 | 打包可执行 JAR |

### 注解处理器链

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.44</version>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>1.5.5.Final</version>
    </path>
</annotationProcessorPaths>
```

> **注意**：Lombok 必须在 MapStruct 之前，否则 @Builder 等注解可能无法正确处理。

---

## 4. 各服务继承关系

```
ManpouChinaSystem (root pom, 无 parent)
    └── java-service (pom.xml, Spring Boot Parent)
            ├── user-service        → artifactId: user-service
            ├── product-service     → artifactId: product-service
            ├── procurement-service  → artifactId: procurement-service
            ├── warehouse-service    → artifactId: warehouse-service
            ├── customs-service      → artifactId: customs-service
            ├── logistics-service    → artifactId: logistics-service
            ├── finance-service     → artifactId: finance-service
            └── notification-service → artifactId: notification-service
```

---

## 5. 本地构建

```bash
# 构建所有 Java 服务
cd apps/java-service
mvn clean install -DskipTests

# 或在根目录
mvn clean install -DskipTests -pl apps/user-service,apps/procurement-service,...
```

---

## 6. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范与 Action Items |
| `docs/pro/00-root-project.md` | 项目全局概览 |
| `docs/pro/02-user-service.md` | user-service 文档 |
| `docs/pro/05-procurement-service.md` | procurement-service 文档 |
