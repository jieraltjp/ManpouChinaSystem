# 工程教训 — 数据库（DB / Flyway / Schema）

> 项目：ManpouChinaSystem
> 覆盖范围：MySQL Schema / Flyway 迁移 / DB 文档
> Lesson 编号：8, 13, 31, 32, 39, 45, 51, 59（共 8 条）

> 注：Lesson 8（Flyway 禁用数据不导入）和 Lesson 31（JSON TEXT）在 [LESSONS-OPS.md](./LESSONS-OPS.md) 和 [LESSONS-BACKEND.md](./LESSONS-BACKEND.md) 中也有详细描述，此处专注 DB 层。

---

## 目录

- [Lesson 13: Flyway 迁移版本号必须提前规划，避免重编号](#lesson-13-flyway-迁移版本号必须提前规划避免重编号)
- [Lesson 31: JSON 存储列必须用 TEXT，不能用 VARCHAR](#lesson-31-json-存储列必须用-text不能用-varchar)
- [Lesson 32: 重构实体移除字段后必须同步清理数据库旧列](#lesson-32-重构实体移除字段后必须同步清理数据库旧列)
- [Lesson 39: 数据库 schema 文档必须与实现同步，版本号体现同步状态](#lesson-39-数据库-schema-文档必须与实现同步版本号体现同步状态)
- [Lesson 45: Flyway 迁移文件版本号不得重复，冲突时立即修正](#lesson-45-flyway-迁移文件版本号不得重复冲突时立即修正)
- [Lesson 51: JPQL 查询字段名 = 实体字段名，非数据库列名](#lesson-51-jpql-查询字段名-实体字段名非数据库列名)
- [Lesson 59: Flyway 禁用项目新增 DB 列类型变更——@PostConstruct 幂等迁移组件](#lesson-59-flyway-禁用项目新增-db-列类型变更postconstruct-幂等迁移组件)

---

## Lesson 13: Flyway 迁移版本号必须提前规划，避免重编号

### 问题

```
fix: Flyway 迁移文件 V3→V8，修复 V7→V8 MySQL 语法
fix: DB文档版本号同步 — DB-01 V6→V7, DB-12 V5→V6（配合Flyway迁移重编号）
```

迁移脚本版本号被改过多次，每次重编号都有风险（checksum 不一致）。

### 根因

- 没有预先规划 Flyway 版本号空间
- 迁移脚本在开发过程中随意插入
- Flyway 禁用时误以为不需要规划

### 解决方案

**Flyway 版本号规划：**

```
V1__init_schema.sql              — 建表骨架（所有模块基础表）
V2__outbox_table.sql             — 事件表
V3__signing_key_table.sql        — JWT 密钥表
V10__factory_seed.sql            — 基础数据（V10起留足空间插队）
V20__shipment_status_enum.sql    — 大版本功能
V30__procurement_extend.sql      — 新模块
```

**原则：**
- 基础设施表从 V1-V9
- 每个业务模块基础数据从 V10, V20, V30 起跳（留 10 个版本空间）
- 紧急修复：`V10_1__fix_xxx.sql` 或 `V10_hotfix__xxx.sql`

### 预防

- 在 `docs/database/README.md` 中维护 Flyway 版本路线图
- Flyway 启用后，修改已执行脚本前先 `flyway:repair`

---

## Lesson 31: JSON 存储列必须用 TEXT，不能用 VARCHAR

### 问题

`VARCHAR(2048)` 在 `utf8mb4` 下只能存 ~682 个中文字符，8 个子货号的 JSON 超限：

```
Data truncation: Data too long for column 'sub_product_code' at row 1
```

### 根因

JPA `length = 2048` 映射为 `VARCHAR(2048)`，限制的是**字节数**而非字符数。

### 修复

```java
// ❌ VARCHAR(2048) — 字节限制，中文超限
@Column(name = "sub_product_code", length = 2048)

// ✅ TEXT — 无字符/字节限制
@Column(name = "sub_product_code", columnDefinition = "TEXT")
```

同时执行 DB 迁移：
```sql
ALTER TABLE replenishment_demand MODIFY COLUMN sub_product_code TEXT;
```

### 字段类型判定

| 字段类型 | 判定 |
|----------|------|
| 有明确上限的短字段 | `VARCHAR(n)` — 如 code、name |
| JSON / 自由文本 | `TEXT` — 无上限保证 |
| 大字段（文章/HTML） | `LONGTEXT` |

---

## Lesson 32: 重构实体移除字段后必须同步清理数据库旧列

### 问题

v1.6.0 删除 `quantity`/`destination`/`linked_procurement_id` 字段，但 DB 中旧列仍存在且定义为 `INT NOT NULL`，INSERT 报错：

```
SQLException: Field 'quantity' doesn't have a default value
```

### 根因

Hibernate `ddl-auto: update` **只添加新列**，不删除已存在但实体中不再引用的列。

### 本次修复

```sql
ALTER TABLE replenishment_demand DROP COLUMN quantity;
ALTER TABLE replenishment_demand DROP COLUMN destination;
ALTER TABLE replenishment_demand DROP COLUMN linked_procurement_id;
```

同步创建 `V27__demand_json_columns_text.sql` 和 `V28__demand_v1_6_schema.sql` 记录变更。

### 预防

| 重构场景 | 必做事项 |
|----------|---------|
| 新增字段 | DB migration + entity 同步 |
| 删除字段 | **DB migration 删除旧列 + entity 移除字段** |
| 重命名字段 | DB rename + entity 改名，禁止先删后加（丢数据）|
| 改列类型 | DB ALTER + entity `@Column` 更新 |

> `ddl-auto: update` 不是银弹——它只处理**新增**，不处理**删除**

---

## Lesson 39: 数据库 schema 文档必须与实现同步，版本号体现同步状态

### 问题

`DB-01-procurement-demand.md` 表结构为 v1.5.x 格式（含 `quantity`、`destination` 列），但 v1.6.0 实体已移除这些列，改为 JSON 字段。

### 根因

- 文档和代码分属不同提交/分支，版本演进时未同步
- 文档版本号（v1.2.0）与代码版本号（v1.6.0）脱节

### 预防

| 变更类型 | 文档要求 |
|----------|---------|
| 表字段变更 | DB 文档版本 + changelog + migration 脚本三方同步 |
| 新增字段 | DB 文档 + entity + migration 同 commit |
| 删除字段 | DB migration DROP 列 + 文档同步删除 |
| 关联关系反转 | 所有引用该关系的文档全部更新 |

> **文档版本号 ≥ 代码版本号。代码到哪个版本，文档必须跟到哪个版本。**

---

## Lesson 45: Flyway 迁移文件版本号不得重复，冲突时立即修正

### 问题

两个迁移文件同名 V24：

```
V24__product_field_extend.sql     — 添加 warehouse/remarks 修复
V24__product_hs_code_extend.sql  — 添加 hs_code_jp 字段
```

Flyway 按版本号排序执行，V24 冲突导致执行顺序不确定，或其中一个被忽略。

### 根因

- 多人同时开发时未协调版本号空间
- 没有统一的 Flyway 版本路线图文档
- `V10/V20/V30` 留空原则未被遵守

### 本次修复

```bash
# 重命名冲突文件至下一个可用版本
mv V24__product_hs_code_extend.sql V30__product_hs_code_extend.sql

# 更新文件内注释
-- Migration: V30__product_hs_code_extend.sql
-- Note: 原 V24 与 V24__product_field_extend.sql 重复，升至 V30
```

同步更新 SPEC-B10 中 "通过 V24 迁移完成" → "通过 V30 迁移完成"。

### 预防

| 场景 | 操作 |
|------|------|
| 新增迁移前 | `ls db/migration/` 查看已用版本号 |
| 大版本功能前 | 预留空间：`V10__`, `V20__`, `V30__` |
| 紧急热修复 | `V10_1__fix_xxx.sql` 或 `V10_hotfix__xxx.sql` |
| 发现版本冲突 | 立即修正，不要遗留 |

> **Flyway 版本路线图应记录在 `docs/database/README.md` 中**

---

## Lesson 51: JPQL 查询字段名 = 实体字段名，非数据库列名

### 问题

JPQL 查询报错：`could not resolve property: is_deleted`

```java
// ❌ 错误：用了数据库列名
@Query("SELECT DISTINCT d.destination FROM ReplenishmentDemand d WHERE d.isDeleted = false ...")

// ✅ 正确：用了实体字段名
@Query("SELECT DISTINCT d.destination FROM ReplenishmentDemand d WHERE d.deleted = false ...")
```

### 根因

JPQL（Hibernate Query Language）查询的是 **JPA 实体属性**，不是数据库列名。

`BaseEntity` 中：
```java
// 数据库列名：is_deleted
// 实体字段名：deleted
@Column(name = "is_deleted", nullable = false)
private Boolean deleted = false;
```

### 修复对照

| 错误写法 | 正确写法 | 说明 |
|---------|---------|------|
| `d.isDeleted = false` | `d.deleted = false` | BaseEntity 字段名是 `deleted` |
| `d.ADDRESS` | `d.address` | JPQL 字段名大小写不敏感，但遵循驼峰 |
| `d.ProductCode` | `d.productCode` | JPA 默认列名映射：驼峰 → 下划线 |

### 预防

- 写 JPQL 前先看实体类的字段定义，不是数据库表结构
- 实体字段命名规范：`Boolean deleted`（非 `isDeleted`）
- 软删除查询统一模式：`WHERE d.deleted = false`

---

## Lesson 59: Flyway 禁用项目新增 DB 列类型变更——@PostConstruct 幂等迁移组件

### 问题

后端 `DemandStatus` 枚举新增 `CONFIRMED` 值，前端 toggle-confirm 接口调用时 500：

```
Data truncated for column 'status' at row 1
[update replenishment_demand set ... status=? where id=?]
```

### 根因

项目使用 Hibernate `ddl-auto: update` 管理表结构，**Flyway 被禁用**：

```yaml
# application-local.yml
flyway:
  enabled: false
```

V38 迁移文件存在但从未执行。数据库 `status` 列仍是旧 ENUM 类型：

```sql
status ENUM('PENDING','CONVERTED','CANCELLED') NOT NULL DEFAULT 'PENDING'
```

代码中写入 `'CONFIRMED'` 超出 ENUM 范围，MySQL 拒绝。

### 修复方案

创建 `@PostConstruct` 幂等迁移组件，在启动时检测并 ALTER：

```java
@Component
public class DemandStatusMigrationConfig {

    @Autowired
    private JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        // 幂等检测：已是 VARCHAR 则跳过
        String colType = jdbc.queryForObject(
            "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'replenishment_demand' AND COLUMN_NAME = 'status'",
            String.class);

        if ("varchar(32)".equalsIgnoreCase(colType)) {
            System.out.println("[DemandStatusMigration] status 已是 VARCHAR(32)，跳过");
            return;
        }

        System.out.println("[DemandStatusMigration] 将 status 从 " + colType + " 改为 VARCHAR(32)");
        jdbc.execute("ALTER TABLE replenishment_demand " +
            "MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING' " +
            "COMMENT '状态：PENDING=待确认，CONFIRMED=已确认，CONVERTED=已转采购，CANCELLED=已取消'");
    }
}
```

### 关键设计原则

| 原则 | 说明 |
|------|------|
| 幂等 | 启动时检测列类型，已变更则跳过 |
| 单一职责 | 仅做一件事：修复 ENUM → VARCHAR |
| 可移除 | Flyway 正式启用后，删除此组件 |

### 预防

- 新增枚举值前先查 DB 列类型：`SHOW COLUMNS FROM table WHERE Field = 'column'`
- 迁移文件配合 JPA 实体变更一起提交，不能假设 Flyway 会自动执行
- Flyway 禁用项目建议统一用 @PostConstruct 迁移模式，防止人工遗漏

---

## 铁律总结表（数据库）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 13 | Flyway 版本号提前规划，禁止重编号 | checksum 不一致 |
| 31 | JSON 存储列必须用 TEXT，不用 VARCHAR | 数据截断 |
| 32 | 实体删除字段后必须同步 DROP DB 列 | 插入失败 |
| 39 | DB schema 文档版本号 ≥ 代码版本号 | 文档失效 |
| 45 | Flyway 迁移版本号不得重复，冲突时立即修正 | 迁移执行顺序不确定 |
| 51 | JPQL 查询字段名 = 实体字段名，非数据库列名 | 查询报错 |
| 59 | Flyway 禁用项目新增枚举值须同步 ALTER DB 列类型，V38 类迁移组件幂等设计 | Data truncated 500 错误 |
