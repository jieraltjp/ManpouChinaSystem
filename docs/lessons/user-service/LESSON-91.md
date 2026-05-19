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

**1. 创建 Flyway 迁移脚本** `user-service/src/main/resources/db/migration/V3__add_language_timezone.sql`：

```sql
ALTER TABLE `user` ADD COLUMN `language` VARCHAR(10) DEFAULT 'zh' COMMENT '界面语言 zh/ja';
ALTER TABLE `user` ADD COLUMN `timezone` VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '界面时区';
```

**2. 启用 Flyway** `application.yml`：

```yaml
flyway:
  enabled: true
  baseline-on-migrate: true  # V1/V2 已有表结构，基线化后只执行 V3
```

---

## 预防

| 场景 | 检查项 |
|------|--------|
| 新增 Entity 字段 | 确认目标服务 ddl-auto 设置；若为 none，手动建列或启用 Flyway 迁移 |
| user-service 新字段 | 始终通过 Flyway 迁移脚本添加，禁止 AdminController 一次性修复 |
