# Lesson 91: user-service ddl-auto:none 导致新 Entity 字段无法写入数据库

> **发现日期**: 2026-05-19
> **项目**: ManpouChinaSystem / user-service
> **教训**: `ddl-auto: none` + Flyway disabled 时，新增 Entity 字段不会自动创建 DB 列，导致 500

---

## 问题

`/system/profile` 页面点击头像上传，`PUT /api/v1/users/me` 返回 **500 Internal Server Error**。

控制台：
```
PUT http://192.168.12.198:13000/api/v1/users/me 500
```

---

## 根因

`User` 实体新增了 `language` 和 `timezone` 字段：

```java
@Column(name = "language")
private String language = "zh";

@Column(name = "timezone")
private String timezone = "CST";
```

但 user-service 的 `application.yml` 配置：

```yaml
jpa:
  hibernate:
    ddl-auto: none
flyway:
  enabled: false
```

`ddl-auto: none` + Flyway disabled → Hibernate 不会自动创建列，`language`/`timezone` 列在 DB 中不存在 → JPA 写入时报 SQL 异常 → 500。

---

## 修复

**最终方案：`ApplicationRunner` 启动时执行 DDL**（优于 Flyway，避免迁移文件管理复杂性）：

`UserServiceApplication.java` 添加静态内部类 `DatabaseInitializer`：

```java
@Slf4j
@Component
@RequiredArgsConstructor
static class DatabaseInitializer {
    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        addColumnIfNotExists("language", "VARCHAR(10) DEFAULT 'zh' COMMENT '界面语言'");
        addColumnIfNotExists("timezone", "VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '界面时区'");
        alterColumnIfTooSmall("avatar_url", "MEDIUMTEXT DEFAULT NULL COMMENT '头像Base64或URL'");
    }

    private void addColumnIfNotExists(String column, String definition) {
        try {
            jdbc.execute("ALTER TABLE `user` ADD COLUMN " + column + " " + definition);
            log.info("Added column: {}", column);
        } catch (Exception e) {
            log.debug("Column {} already exists or error: {}", column, e.getMessage());
        }
    }
}
```

`application.yml`：Flyway 禁用，改由 ApplicationRunner 管理列：
```yaml
flyway:
  enabled: false
```

---

## 预防

| 场景 | 检查项 |
|------|--------|
| 新增 Entity 字段 | 确认目标服务 ddl-auto 设置；若为 none，在 DatabaseInitializer 中添加 ALTER TABLE |
| user-service 新字段 | 在 DatabaseInitializer.init() 中添加 `addColumnIfNotExists` 调用 |
