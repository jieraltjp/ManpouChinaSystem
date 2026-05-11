# SQL 目录说明

> 生成时间: 2026-05-11（INTJ 审计）

---

## 文件清单

| 文件 | 说明 |
|------|------|
| `production_schema.sql` | 生产环境数据库初始化脚本（38张表 DDL，来源 mysqldump） |
| `full_schema.sql` | 含 MySQL 版本头/注释的完整导出 |
| `full_schema_clean.sql` | 纯 DDL，无 MySQL 头/注释 |

---

## 使用方式

### 1. 生产环境首次部署（新数据库）

```bash
# 导入完整 Schema
mysql -h <host> -P <port> -u root -p manpou \
  < apps/manpou-allinone/sql/production_schema.sql

# Spring Boot 启动后 Flyway 自动执行 V15~V49
```

### 2. 升级已有数据库（JPA ddl-auto → Flyway）

```bash
# 方式 A：在 application-prod.yml 中配置
flyway:
  baseline-on-migrate: true
  baseline-version: '14'

# 方式 B：手动执行 V49
mysql -h <host> -P <port> -u root -p manpou \
  < apps/manpou-allinone/src/main/resources/db/migration/V49__procurement_snapshot_table.sql
```

详见: `docs/pro/PRODUCTION-DEPLOY.md`

---

## 当前迁移状态

- **Flyway 已应用**: V1 ~ V14（18条记录）
- **Flyway 跳过**: V15 ~ V48（表由 JPA ddl-auto 创建，幂等 CREATE IF NOT EXISTS 安全）
- **本次新增**: V49（procurement_snapshot）

---

## 表统计

```
38 张业务表 + 3 张视图 = 41 个对象
```

详见: `docs/database/DB-*.md`
