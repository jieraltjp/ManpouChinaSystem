# 生产环境部署指南 — ManpouChinaSystem 数据库

> **版本**: 2.0.0
> **创建**: 2026-05-11
> **更新**: 2026-05-11（迁移体系重新设计）
> **状态**: ✅ 可执行

---

## 1. 迁移体系设计

### 1.1 背景

历史 49 个迁移文件（V1~V49）杂乱无章，部分缺乏幂等性（V45/V46 无 `IF NOT EXISTS`），且与生产实际 schema 不一致。生产未部署，重新设计迁移体系。

### 1.2 新迁移结构

| 版本 | 文件 | 说明 |
|------|------|------|
| **V15** | `V15__baseline_schema.sql` | 43 张表 DDL + 全部种子数据（幂等） |
| **V16** | `V16__procurement_snapshot.sql` | 发注单快照表（幂等兜底） |

**已删除**：V1~V49 共 35 个旧迁移文件（历史包袱，V15 已包含全部表结构）。

### 1.3 幂等性保证

- 所有 DDL：`CREATE TABLE IF NOT EXISTS`
- 种子数据：`INSERT IGNORE INTO`（主键/UNIQUE 冲突时静默跳过）
- `flyway_schema_history` 已有 V1~V14，生产部署时通过 `baseline-version='14'` 跳过

---

## 2. 部署步骤

### 2.1 全新部署（推荐）

生产数据库为空时，按顺序执行：

```bash
# 1. 创建数据库
mysql -h <host> -P <port> -u root -p -e \
  "CREATE DATABASE IF NOT EXISTS manpou
   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

# 2. 导入完整 schema（38 张表 + 种子数据，一次完成）
mysql -h <host> -P <port> -u root -p manpou \
  < apps/manpou-allinone/sql/production_schema.sql

# 3.（可选）导入静态基础数据（中国 HS 编码 / 日本 HS 编码）
# 如果 production_schema.sql 已包含，跳过
# mysql -h <host> -P <port> -u root -p manpou < apps/manpou-allinone/sql/cn_hs_code_data.sql
# mysql -h <host> -P <port> -u root -p manpou < apps/manpou-allinone/sql/jp_hs_code_data.sql

# 4. 启动 manpou-allinone（激活 production profile）
export SPRING_PROFILES_ACTIVE=production
# 或启动参数: --spring.profiles.active=production
# application-production.yml 中 spring.profiles.active=production 会覆盖 application.yml 的 local 默认值
# Flyway 检测到 baseline='14'，自动跳过 V1~V14
# 执行 V15（幂等：表已存在则跳过）→ V16
```

### 2.2 已有数据库升级

生产数据库已有 JPA 创建的表，使用 `baseline-on-migrate`：

```bash
# 启动时指定 production profile，application-production.yml 已配置：
# export SPRING_PROFILES_ACTIVE=production
# Flyway 配置（application-production.yml）：
spring:
  profiles:
    active: production        # 覆盖 application.yml 的 local 默认值
  jpa:
    hibernate:
      ddl-auto: validate       # 不靠 ddl-auto，靠 Flyway
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: '14'    # 已有 V1~V14 记录，跳过
    locations: classpath:db/migration
    validate-on-migrate: true
```

`flyway_schema_history` 最终记录：`V1~V14（baseline） + V15 + V16`

### 2.3 验证迁移结果

```sql
SELECT version, description, installed_rank, applied_on, success
FROM flyway_schema_history ORDER BY installed_rank;

-- 预期：V1~V14（baseline） + V15 + V16，全部 success=1
```

---

## 3. 大表静态数据

| 表 | 行数 | 文件 |
|----|------|------|
| `cn_hs_code` | ~12000 | `sql/cn_hs_code_data.sql`（从 production_schema.sql 提取） |
| `jp_hs_code` | ~9700 | `sql/jp_hs_code_data.sql`（从 production_schema.sql 提取） |
| `factory` | ~500 | 已在 production_schema.sql 中 |
| `product` | ~5000 | 已在 production_schema.sql 中 |

如需分离导入：将上述文件解压后单独执行。

---

## 4. 回滚方案

| 场景 | 操作 |
|------|------|
| V16 失败 | 删除 `flyway_schema_history` 中 version='16' 的记录 |
| V16 成功但需撤销 | `DROP TABLE IF EXISTS procurement_snapshot;` |
| V15 失败 | `DROP DATABASE manpou` 后重新执行部署步骤 |
| 基线错误 | `DELETE FROM flyway_schema_history WHERE version > '14';` |

---

## 5. 相关文件

| 文件 | 说明 |
|------|------|
| `apps/manpou-allinone/sql/production_schema.sql` | 完整数据库 Schema（38 表，mysqldump 2026-05-11） |
| `V15__baseline_schema.sql` | 43 张表 DDL + 种子数据（生产基准） |
| `V16__procurement_snapshot.sql` | 发注单快照表（幂等兜底） |
| `application-production.yml` | 生产配置（Flyway 启用，baseline='14'） |
| `docs/business/DB-09-order-overview.md` | procurement_snapshot 设计文档 |

---

## 6. user-service 迁移处理

user-service 的 16 个 Flyway 迁移文件已全部删除，依赖 allinone 的同一数据库（manpou），无需独立迁移。user-service 的 `flyway.enabled=false`（保持 JPA ddl-auto 用于开发）。

---

## 7. 注意事项

- **禁止**对已上线的列改类型或删列（Flyway DDL 规范）
- `cn_hs_code` / `jp_hs_code` / `example` 为静态数据/示例表，非业务核心
- 生产密码通过环境变量注入，不写入配置文件
- 大表数据（cn_hs_code 12030行 / jp_hs_code 9694行）建议单独导入，避免拖慢主 schema 导入
