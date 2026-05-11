# 生产环境部署指南 — ManpouChinaSystem 数据库

> **版本**: 1.0.0
> **创建**: 2026-05-11
> **状态**: ✅ 可执行

---

## 1. 现状说明

### 1.1 Flyway 迁移历史

生产数据库 `flyway_schema_history` 当前已应用：**V1 ~ V14**（共18条）。

| 范围 | 说明 |
|------|------|
| V1 ~ V14 | ✅ 已通过 Flyway 应用（公司/部门/职务/用户/角色权限/签名密钥/操作日志） |
| V15 ~ V48 | ⚠️ **未通过 Flyway 应用**（JPA `ddl-auto: update` 在开发期创建了对应表） |
| V49 | 🆕 本次新增（procurement_snapshot） |

### 1.2 现有表清单（38张）

```
audit_log, cn_hs_code, company, consolidation_pool, container,
demand_procurement_mapping, department, domestic_customs_record,
example, factory, flyway_schema_history, japan_customs_record,
jp_hs_code, logistics_plan, outbox, permission, position,
procurement, procurement_snapshot, product, product_factory,
qc_image, qc_record, replenishment_demand, role,
role_permission, saga_log, sales_record, shipment_batch,
signing_key, tax_refund_record, user, user_position, user_role,
v_order_chain_v1
```

### 1.3 所有 Flyway 迁移均使用 `CREATE TABLE IF NOT EXISTS`

所有 V15 ~ V49 迁移均采用 `CREATE TABLE IF NOT EXISTS` 或 `ALTER TABLE` 写法，**幂等**：
- 表已存在 → 跳过，不报错
- 表不存在 → 创建

---

## 2. 部署方式

### 方式 A：新数据库（首次部署）✅ 推荐

```bash
# 1. 创建空数据库
mysql -h <host> -P <port> -u root -p -e "CREATE DATABASE IF NOT EXISTS manpou
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

# 2. 导入生产初始化脚本（V1~V14 已包含）
mysql -h <host> -P <port> -u root -p manpou \
  < apps/manpou-allinone/sql/production_schema.sql

# 3. 应用 Flyway 迁移（V15~V49）
# Spring Boot 启动时 Flyway 自动执行，或手动：
cd apps/manpou-allinone
mvn flyway:migrate -Dflyway.url="jdbc:mysql://<host>:<port>/manpou" \
  -Dflyway.user=root -Dflyway.password=<password>

# 4. 验证迁移
mysql -h <host> -P <port> -u root -p manpou \
  -e "SELECT version, description FROM flyway_schema_history ORDER BY installed_rank;"
```

### 方式 B：已有数据库（升级部署）✅ 推荐

已有 JPA 创建的表，无需清空。使用 `baselineOnMigrate`。

```bash
# 1. 在 application.yml 或 application-prod.yml 中配置：
flyway:
  enabled: true
  baseline-on-migrate: true        # 关键：对已有数据库基线化
  baseline-version: '14'          # 标记当前为 V14 基线
  locations: classpath:db/migration
  validate-on-migrate: true

# 2. Spring Boot 启动
# Flyway 会自动跳过 V15~V48（表已存在，CREATE IF NOT EXISTS 跳过）
# 执行 V49（新增 procurement_snapshot）
# flyway_schema_history 最终记录: V1~V14 + V15~V49（全部通过）

# 3. 验证
mysql ... -e "SELECT version, description, success FROM flyway_schema_history
  ORDER BY installed_rank DESC LIMIT 10;"
```

### 方式 C：手动 SQL 补齐

无法修改 `application.yml` 时，逐条执行缺失迁移：

```bash
# 仅手动执行 V49（其他 V15~V48 会自动跳过，因为表已存在）
mysql -h <host> -P <port> -u root -p manpou \
  < apps/manpou-allinone/src/main/resources/db/migration/V49__procurement_snapshot_table.sql

# 验证
mysql -h <host> -P <port> -u root -p manpou \
  -e "SHOW TABLES LIKE 'procurement_snapshot';"
```

---

## 3. 部署前置检查

```sql
-- 检查当前迁移状态
SELECT version, description, installed_rank, applied_on, success
FROM flyway_schema_history ORDER BY installed_rank;

-- 检查缺失的 Flyway 迁移（V15~V48）
-- 预期结果：无缺失（因为 CREATE IF NOT EXISTS 幂等）

-- 检查 procurement_snapshot 表是否存在
SHOW TABLES LIKE 'procurement_snapshot';
-- 预期（有 V49 后）：Table exists

-- 检查 user-service permission 表权限数量
SELECT COUNT(*) AS permission_count FROM permission WHERE is_deleted = 0;
-- 预期: 102 条（V8 93 + V15 8 + V16 1）
```

---

## 4. 回滚方案

| 场景 | 操作 |
|------|------|
| V49 失败 | 删除 `flyway_schema_history` 中 version='49' 的记录 |
| V49 成功但需撤销 | `DROP TABLE IF EXISTS procurement_snapshot;` |
| 基线错误 | `DELETE FROM flyway_schema_history WHERE version > '14';` |

---

## 5. 相关文件

| 文件 | 说明 |
|------|------|
| `apps/manpou-allinone/sql/production_schema.sql` | 完整数据库 Schema（38表，DDL from mysqldump） |
| `apps/manpou-allinone/sql/full_schema.sql` | 含 MySQL 头的完整导出（含注释） |
| `apps/manpou-allinone/sql/full_schema_clean.sql` | 纯 DDL 无头导出 |
| `V49__procurement_snapshot_table.sql` | 缺失的 Flyway 迁移 |
| `docs/database/DB-09-order-overview.md` | procurement_snapshot 设计文档 |

---

## 6. 注意事项

- **禁止**对已上线的列改类型或删列（Flyway DDL 规范）
- `cn_hs_code` / `jp_hs_code` / `example` 为静态数据/示例表，非业务核心
- `v_order_chain_v1` 为视图，由 `OrderOverviewUseCase` 使用，不影响业务功能
- 生产密码通过环境变量注入，不写入配置文件
