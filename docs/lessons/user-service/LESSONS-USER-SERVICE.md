# 工程教训 — user-service 实施（SPEC-B11 Phase 1）

> 项目：ManpouChinaSystem
> 覆盖范围：user-service JWT 跨服务架构 / Flyway + JPA schema 对齐 / MySQL TINYINT 类型映射
> Lesson 编号：62–71（共 10 条）

---

## 目录

- [Lesson 62: BaseEntity 与 Flyway 建表必须同步——JPA 实体有 BaseEntity 父类，但 SQL 迁移缺少审计列](#lesson-62-baseentity-与-flyway-建表必须同步jpa-实体有-baseentity-父类但-sql-迁移缺少审计列)
- [Lesson 63: JPA TINYINT → Java 类型映射须用 columnDefinition——MySQL TINYINT ≠ INTEGER](#lesson-63-jpa-tinyint-→-java-类型映射须用-columndefinitionmysql-tinyint-≠-integer)
- [Lesson 64: Flyway 部分失败的迁移修复流程——repair + 重新运行](#lesson-64-flyway-部分失败的迁移修复流程repair--重新运行)
- [Lesson 65: Flyway 迁移必须幂等设计——INSERT 用 ON DUPLICATE KEY UPDATE](#lesson-65-flyway-迁移必须幂等设计insert-用-on-duplicate-key-update)
- [Lesson 66: BCrypt 密码哈希必须实际验证——不能用记忆的 hash](#lesson-66-bcrypt-密码哈希必须实际验证不能用记忆的-hash)
- [Lesson 67: mvn spring-boot:run 启动失败时，先用 mvn compile 诊断——不能用 -q 静默](#lesson-67-mvn-spring-bootrun-启动失败时先用-mvn-compile-诊断不能用--q-静默)

---

## Lesson 62: BaseEntity 与 Flyway 建表必须同步——JPA 实体有 BaseEntity 父类，但 SQL 迁移缺少审计列

### 问题

JPA schema-validation 报错：
```
Schema-validation: missing column [create_by] in table [permission]
Schema-validation: missing column [create_by] in table [role]
```

但 `BaseEntity.java` 已经定义了这些字段（`create_by`, `update_by`, `is_deleted`）。

### 根因

SQL 迁移创建的表只定义了业务字段，没有包含 `BaseEntity` 的审计列：
- `permission` 表缺少：`create_by`, `update_by`, `update_time`
- `role` 表缺少：`create_by`, `update_by`

### 本次修复

1. 直接用 JDBC ALTER TABLE 添加缺失列（绕过 Flyway）：
```java
conn.createStatement().executeUpdate(
    "ALTER TABLE permission ADD COLUMN create_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'");
conn.createStatement().executeUpdate(
    "ALTER TABLE role ADD COLUMN create_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM'");
```

2. 创建 Flyway 迁移记录变更（确保新环境不重复问题）：
```sql
-- 补充 BaseEntity 审计列
ALTER TABLE permission
    ADD COLUMN create_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN update_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3);

ALTER TABLE role
    ADD COLUMN create_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN update_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM';
```

### 预防

| 场景 | 必做事项 |
|------|---------|
| 新建 Flyway 迁移 | 先确认 BaseEntity 是否有审计列要求 |
| 新建 JPA Entity extends BaseEntity | Flyway 迁移必须包含 create_by, update_by, update_time, is_deleted |
| BaseEntity 变更（新增字段） | 必须新建 Vx__add_xxx_columns.sql |

> **BaseEntity 字段变更 = Flyway ALTER 迁移 + 文档同步更新**

---

## Lesson 63: JPA TINYINT → Java 类型映射须用 columnDefinition——MySQL TINYINT ≠ INTEGER

### 问题

JPA schema-validation 报错：
```
wrong column type encountered in column [is_deleted] in table [permission];
found [tinyint (Types#TINYINT)], but expecting [bit (Types#BOOLEAN)]

wrong column type encountered in column [status] in table [permission];
found [tinyint (Types#TINYINT)], but expecting [integer (Types#INTEGER)]
```

### 根因

Hibernate 5.x/6.x 对 Java `Boolean` 默认映射到 `BIT(1)`，对 Java `Integer` 默认映射到 `INT(11)`。MySQL 用 `TINYINT` 时，类型不匹配。

### 修复

```java
// BaseEntity.java
@Column(name = "is_deleted", nullable = false, columnDefinition = "TINYINT(1)")
private Boolean isDeleted = false;

// Permission.java / Role.java / User.java
@Column(name = "status", columnDefinition = "TINYINT")
private Integer status = 1;

@Column(name = "is_editable", columnDefinition = "TINYINT")
private Integer isEditable = 1;
```

### MySQL TINYINT 映射速查

| Java 类型 | MySQL 列类型 | `@Column` 配置 |
|-----------|-------------|----------------|
| `Boolean` | `TINYINT(1)` | `columnDefinition = "TINYINT(1)"` |
| `Integer` | `TINYINT` | `columnDefinition = "TINYINT"` |
| `Integer` | `INT` | 默认即可 |

### 预防

- 新建实体字段时，确认 Java 类型与 MySQL 列类型的 Hibernate 默认映射
- 不确定时查 Hibernate 文档或实际测试
- 生产 MySQL 用 `TINYINT` 时显式写 `columnDefinition`

---

## Lesson 64: Flyway 部分失败的迁移修复流程——repair + 重新运行

### 问题

迁移因 SQL 错误部分执行后：
```
Migration V__xxx.sql failed
Please remove any half-completed changes then run repair to fix the schema history.
```

再次启动时 Flyway 拒绝运行，困在"半失败"状态。

### 根因

Flyway 的 `flyway_schema_history` 表记录了该版本的 `success=0`。后续启动时 Flyway validate 发现 failed migration，要求先 repair。

### 正确流程

```bash
# 1. 先用 Maven repair 清除 failed 标记
mvn flyway:repair \
  -Dflyway.url="jdbc:mysql://host/db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
  -Dflyway.user=root \
  -Dflyway.password=xxx

# 2. 手动修复数据库（如需要 ALTER TABLE）
# 用 JDBC 或 mysql CLI 直接执行 DDL

# 3. 重启应用
mvn spring-boot:run
```

### 紧急手动修复（当 Maven repair 不可用时）

用 Java/JDBC 直接操作 `flyway_schema_history`：
```java
Connection conn = DriverManager.getConnection(url, user, pass);
conn.createStatement().executeUpdate(
    "DELETE FROM flyway_schema_history WHERE version = '10' AND success = 0");
```

### 预防

| 规范 | 说明 |
|------|------|
| 迁移写完后本地测试 | 先用 H2/MySQL 跑一遍 |
| 幂等设计 | INSERT 用 `ON DUPLICATE KEY UPDATE` |
| 每次修复后本地验证 | 重新启动确认无报错 |

---

## Lesson 65: Flyway 迁移必须幂等设计——INSERT 用 ON DUPLICATE KEY UPDATE

### 问题

迁移第一次失败后修复重跑，再次失败（主键冲突）：
```
Duplicate entry '1' for key 'PRIMARY'
```

即使修复了 SQL 错误，INSERT 也会因为主键存在而失败。

### 根因

Flyway 重跑时，成功的 DDL（如 ALTER TABLE）已执行，但 INSERT 语句按 Flyway 的 repeatable/版本语义重新执行。

### 修复

所有数据 seed INSERT 必须带幂等保护：
```sql
-- ❌ 会重复报错
INSERT INTO user (id, username, password_hash, ...) VALUES (1, 'admin', '...');

-- ✅ 幂等：已存在则更新
INSERT INTO user (id, username, password_hash, ...)
VALUES (1, 'admin', '...')
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    registration_status = VALUES(registration_status);

-- ✅ 幂等：选择性插入（已有则跳过）
INSERT INTO role (id, role_code, role_name_cn, ...)
VALUES (1, 'ADMIN', '系统管理员', ...)
ON DUPLICATE KEY UPDATE role_name_cn = VALUES(role_name_cn);

-- ✅ 幂等：子查询关联（防止 user 不存在时引用失败）
INSERT INTO user_role (user_id, role_id)
SELECT u.id, 1 FROM user u WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE role_id = 1;
```

### 预防

- 所有 Flyway INSERT 语句必须使用 `ON DUPLICATE KEY UPDATE`
- UPDATE 语句也建议先 DELETE 再 INSERT（更安全）
- 幂等性是 Flyway 迁移的第一要务

---

## Lesson 66: BCrypt 密码哈希必须实际验证——不能用记忆的 hash

### 问题

用"记忆的" BCrypt hash 创建 admin 用户，登录时报 `密码错误`：
```
BusinessException: 密码错误
```

### 根因

BCrypt hash 每次生成结果不同（因为随机 salt）。"记忆的" hash 是别人生成的，不匹配 `admin123`。

### 正确做法

**用实际运行中的 BCryptPasswordEncoder 生成：**
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hash = encoder.encode("admin123");
System.out.println(hash); // 每次不同
System.out.println(encoder.matches("admin123", hash)); // true
```

**或用 Spring Boot 测试：**
```java
@Autowired
private PasswordEncoder passwordEncoder;

@Test
void encodePassword() {
    String hash = passwordEncoder.encode("admin123");
    System.out.println("Hash: " + hash);
    assertTrue(passwordEncoder.matches("admin123", hash));
}
```

### 根因：BCrypt 每次结果不同

```
$2a$12$LQv3c1yqBWVHxkd0LHAkCO...  ← hash A（不匹配 admin123）
$2a$12$t7mRpfsCDNFgj6LET1Y47eH...  ← hash B（匹配 admin123）
```

### 预防

- 永远不要"记住" BCrypt hash，用时现生成
- CI/CD 中用 PropertiesLauncher 或测试类动态生成
- 生成后立即写入 Flyway seed 并 commit

---

## Lesson 67: mvn spring-boot:run 启动失败时，先用 mvn compile 诊断——不能用 -q 静默

### 问题

`mvn spring-boot:run -pl apps/manpou-allinone -q` 后台运行，失败时只看到 exit code，无错误信息。

### 根因

`-q` (quiet) 模式吞掉了所有编译和启动输出。后台 `&` 时 parent shell 无法看到 child 的输出（除非重定向）。

### 正确诊断流程

```bash
# 1. 先确认当前目录正确
pwd

# 2. 全量编译（禁止 -q），看编译是否成功
cd D:/Programme/java/ManpouChinaSystem
mvn compile -pl apps/manpou-allinone -am 2>&1 | tail -20

# 3. 编译成功后单独启动（重定向到文件）
mvn spring-boot:run -pl apps/manpou-allinone 2>&1 | tee start.log &
sleep 30
tail -30 start.log
```

### 正确后台启动流程

```bash
# ✅ 正确：分离编译和启动，检查输出
cd D:/Programme/java/ManpouChinaSystem
mvn compile -pl apps/manpou-allinone -am
mvn spring-boot:run -pl apps/manpou-allinone 2>&1 | tee allinone.log &
sleep 40
grep -E "Started|ERROR|failed" allinone.log | head -10

# ❌ 错误：-q 静默模式
mvn spring-boot:run -pl apps/manpou-allinone -q &

# ❌ 错误：不在项目根目录
mvn spring-boot:run -pl apps/manpou-allinone &
```

### 预防

| 规范 | 说明 |
|------|------|
| 编译禁止 `-q` | 让所有 WARNING 和 ERROR 可见 |
| 启动输出重定向 | `2>&1 \| tee service.log` |
| 编译和启动分离 | 先 compile 确认，再 spring-boot:run |
| 用 scripts/start-all.bat | 项目已有启动脚本，统一入口 |

---

---

## Lesson 68: Maven annotationProcessorPaths Lombok 版本必须与 classpath 一致

### 问题

user-service 编译报错：`cannot find symbol: method getStatus()`，`cannot find symbol: method setCreateTime(...)` 等——Lombok 未生成 getter/setter。

### 根因

`mvn compile -X` 显示：
```
compile classpath: ...lombok-1.18.46.jar...
annotationProcessorPaths: ...lombok-1.18.32.jar...
```
annotation processor 用 1.18.32，运行时 classpath 用 1.18.46。版本不匹配导致 Lombok 处理器生成的代码与运行时不一致。

### 修复

user-service/pom.xml 中 annotation processor 版本与 classpath 对齐：
```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.46</version>  <!-- 与 classpath 一致 -->
    </path>
    ...
</annotationProcessorPaths>
```

### 排查方法

```bash
# 1. 查看 classpath Lombok 版本
mvn dependency:tree -pl apps/user-service | grep lombok

# 2. 对比 annotation processor 版本
mvn compile -X 2>&1 | grep "lombok.*jar"
```

### 预防

- annotation processor 永远显式指定版本，不用 `${lombok.version}`（父 POM 版本可能与 classpath 不同）
- 每引入新模块，先检查 Lombok 版本一致性

---

## Lesson 69: BaseEntity 的 setter 须 public——子类 Service 在不同包无法访问 protected

### 问题

编译报错：`setCreateTime(...) has protected access in BaseEntity`

### 根因

`BaseEntity` 字段是 `private`，只有 `@Getter`（Lombok 生成 getter），没有 setter。但子类 `RoleService` / `UserService` 在 `application.service` 包，与 `BaseEntity`（`domain.model` 包）不在同一包，`protected` 仍无法访问。

### 修复

BaseEntity 中添加 public setter 方法：
```java
// BaseEntity.java
public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
public void setCreateBy(String createBy) { this.createBy = createBy; }
public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
```

### 预防

- `@Access(AccessType.FIELD)` + Lombok `@Getter` 的 entity，如需 Service 层注入字段，BaseEntity 必须提供 public setter
- 或改用 `@Access(AccessType.PROPERTY)` + `@Setter`，由 Lombok 生成

---

## Lesson 70: UserRepository 使用 Specification 查询必须显式继承 JpaSpecificationExecutor

### 问题

编译报错：`no suitable method found for findAll((root,cq,cb)->..., PageRequest)`

### 根因

`UserRepository extends JpaRepository<User, Long>` —— `JpaRepository` 不包含 `findAll(Specification, Pageable)` 方法，该方法在 `JpaSpecificationExecutor` 接口中。

虽然 `JpaRepository` 间接继承了 `JpaSpecificationExecutor`，但 Spring Data JPA 的处理器需要**直接声明**继承才能正确生成实现类。

### 修复

```java
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    // ...
}
```

### 预防

- 任何需要 `Specification` / `Example` / `QueryByExampleExecutor` 查询的 Repository，必须显式继承对应接口
- 不要依赖接口间的间接继承关系

---

## Lesson 71: 同一 .java 文件内的多个 top-level public 类导致 JDK 编译异常

### 问题

RoleDTOs.java / UserDTOs.java 合并多个 public 类，javac 报错：`cannot find symbol: RoleCreateCmd`，但类定义明明存在。

### 根因

Windows 环境下 JDK 21 对同一文件中多个 top-level public 类的处理存在不确定性。根本原因不明，但拆分为独立文件后问题消失。

### 修复

每个 public 类一个 .java 文件（标准 Java 实践）：
```
application/dto/
├── RoleCreateCmd.java
├── RoleUpdateCmd.java
├── RolePermissionsCmd.java
├── PermissionVO.java
├── RoleVO.java
├── RoleSimpleVO.java
├── PermissionTreeVO.java
└── ...
```

### 预防

- **一个 public 类 = 一个 .java 文件**（Java 语言规范要求，但不是所有编译器严格遵守）
- 禁止同一文件多个 public 类（包括内部类 public static class 也要检查）

---

## 铁律总结表（user-service 实施）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 62 | BaseEntity 字段变更必须同步 Flyway ALTER 迁移 | JPA schema-validation 失败 |
| 63 | MySQL TINYINT 列对应 Java Integer/Boolean 须用 columnDefinition | 类型不匹配启动失败 |
| 64 | Flyway 部分失败后先 repair 再重跑 | 困在"半失败"状态无法启动 |
| 65 | Flyway INSERT 必须幂等（ON DUPLICATE KEY UPDATE） | 重跑失败或数据重复 |
| 66 | BCrypt hash 不用记忆，用时现生成并验证 | 密码错误无法登录 |
| 67 | 编译和启动分离，不用 -q 静默模式 | 错误被掩盖，无法诊断 |
| 68 | annotationProcessorPaths Lombok 版本必须与 classpath 一致 | Lombok 不生成代码 |
| 69 | BaseEntity setter 须 public（跨包访问） | protected 无法被 service 包访问 |
| 70 | Repository 使用 Specification 必须显式继承 JpaSpecificationExecutor | 方法不存在编译失败 |
| 71 | 一个 public 类一个 .java 文件 | JDK 编译异常 |
