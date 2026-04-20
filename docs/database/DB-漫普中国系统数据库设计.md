# 数据库设计文档

> 数据库即单一真相来源（Single Source of Truth）。
> 所有 ORM 映射、API 契约均以此文档为准。
>
> 连接信息：`application-local.yml`（不提交仓库）

---

## §1 连接配置

| 参数 | 值 |
|------|-----|
| Host | `192.168.13.202` |
| Port | `23306` |
| Database | `manpou` |
| Username | `root` |
| Password | `manpou23306` |
| Engine | MySQL 8.x / InnoDB |
| Charset | `utf8mb4_unicode_ci` |
| Migration | Hibernate `ddl-auto: update`（开发）|

---

## §2 表清单

| # | 表名 | 行数 | 用途 | 状态 |
|---|------|------|------|------|
| 1 | `procurement` | 4 | 发注单（核心业务） | 活跃 |
| 2 | `signing_key` | 2 | JWT RS256 签名密钥 | 活跃 |
| 3 | `outbox` | 0 | 事务性发件箱（事件驱动） | 待用 |
| 4 | `saga_log` | 0 | Saga 分布式事务日志 | 待用 |
| 5 | `flyway_schema_history` | 3 | Flyway 迁移历史 | 系统 |
| 6 | `example` | 0 | 用户模块骨架占位 | 待替换 |
| 7 | `customs_example` | 0 | 报关模块骨架占位 | 待替换 |
| 8 | `finance_example` | 0 | 财务模块骨架占位 | 待替换 |
| 9 | `logistics_example` | 0 | 物流模块骨架占位 | 待替换 |
| 10 | `notification_example` | 0 | 通知模块骨架占位 | 待替换 |
| 11 | `product_example` | 0 | 商品模块骨架占位 | 待替换 |
| 12 | `warehouse_example` | 0 | 仓库模块骨架占位 | 待替换 |

> 示例表（example*）均遵循 `BaseEntity` 骨架，未来替换为真实实体。

---

## §3 命名规范

### 3.1 物理命名

| 层级 | 规则 | 示例 |
|------|------|------|
| 表名 | `snake_case`，模块前缀 | `procurement`, `signing_key` |
| 列名 | `snake_case`，全小写 | `product_code`, `create_time` |
| 主键 | `id`（单表） | `id BIGINT PK` |
| 外键 | `{referenced_table_singular}_id` | `procurement_id` |

### 3.2 Java 映射

| MySQL | Java |
|-------|------|
| `snake_case` | `camelCase` |
| `decimal(p,s)` | `BigDecimal` |
| `varchar(n)` | `String` |
| `int` | `Integer` |
| `bigint` | `Long` |
| `bit(1)` / `tinyint(1)` | `Boolean` |
| `datetime` | `LocalDateTime` |
| `date` | `LocalDate` |
| `json` | `String` / ObjectMapper |
| `enum(...)` | Java `enum` |

### 3.3 审计字段规范

所有业务表必须继承 `BaseEntity`（逻辑删除模式）：

| 列名 | 类型 | 说明 |
|------|------|------|
| `id` | `BIGINT PK` | 主键，自增 |
| `create_by` | `VARCHAR(64)` | 创建人 |
| `create_time` | `DATETIME(6)` | 创建时间（含毫秒） |
| `update_by` | `VARCHAR(64)` | 更新人 |
| `update_time` | `DATETIME(6)` | 更新时间（含毫秒） |
| `is_deleted` | `BIT(1)` | 逻辑删除标记（0=正常, 1=删除） |

---

## §4 实体-表映射

### 4.1 `procurement` — 发注单

**实体路径：** `com.manpou.allinone.procurement.domain.model.Procurement`
**聚合根：** `Procurement`

| 列名 | MySQL 类型 | Java 字段 | 说明 |
|------|-----------|-----------|------|
| `id` | `BIGINT PK` | `Long id` | 主键 |
| `create_by` | `VARCHAR(64)` | `String createBy` | 创建人 |
| `create_time` | `DATETIME(6)` | `LocalDateTime createTime` | 创建时间 |
| `update_by` | `VARCHAR(64)` | `String updateBy` | 更新人 |
| `update_time` | `DATETIME(6)` | `LocalDateTime updateTime` | 更新时间 |
| `is_deleted` | `BIT(1)` | `Boolean isDeleted` | 逻辑删除 |
| `product_code` | `VARCHAR(32)` | `String productCode` | 商品代码 |
| `quantity` | `INT` | `Integer quantity` | 订购数量 |
| `price_rmb` | `DECIMAL(12,2)` | `BigDecimal priceRmb` | 人民币单价 |
| `exchange_rate` | `DECIMAL(10,4)` | `BigDecimal exchangeRate` | CNY→JPY 汇率 |
| `tax_point` | `DECIMAL(5,4)` | `BigDecimal taxPoint` | 票点（默认 1.1） |
| `estimated_price_jpy` | `DECIMAL(14,2)` | `BigDecimal estimatedPriceJpy` | 估算批发价（计算字段） |
| `billing_method` | `VARCHAR(32)` | `String billingMethod` | 计费方式（可选） |
| `order_date` | `DATE` | `LocalDate orderDate` | 下单日 |
| `factory_ship_date` | `DATE` | `LocalDate factoryShipDate` | 厂家出货日 |
| `planned_ship_date` | `DATE` | `LocalDate plannedShipDate` | 计划出货日 |
| `product_lead` | `VARCHAR(64)` | `String productLead` | 商品担当 |
| `japan_lead` | `VARCHAR(64)` | `String japanLead` | 日本担当 |
| `china_lead` | `VARCHAR(64)` | `String chinaLead` | 中国担当 |
| `destination` | `VARCHAR(128)` | `String destination` | 发送目的地 |
| `customer_company` | `VARCHAR(128)` | `String customerCompany` | 客户公司 |
| `status` | `ENUM(...)` | `ShipmentStatus status` | 状态枚举 |
| `tax_point` | `DECIMAL(5,4)` | `BigDecimal taxPoint` | 票点 |

**索引：**

| 索引名 | 列 | 类型 |
|--------|-----|------|
| `idx_procurement_status` | `status` | MUL |
| `idx_procurement_product_code` | `product_code` | MUL |
| `idx_procurement_create_time` | `create_time` | MUL |

**状态枚举值（对应 MySQL ENUM）：**

```
ShipmentStatus: 未定, 予定, OEM, 発注待,
                 永康, 直送,
                 倉庫着, 現地検品, 検品,
                 エア便, メーカー直送, 輸出,
                 通関, 日本着,
                 会計,
                 完了, 退货
```

> ⚠️ `完了` 为终态（`isTerminal() == true`），禁止任何状态变更。

---

### 4.2 `signing_key` — JWT 签名密钥

**实体路径：** `com.manpou.allinone.domain.model.SigningKey`
**用途：** RS256 密钥对元数据管理（公钥存 DB，私钥存文件系统）

| 列名 | MySQL 类型 | Java 字段 | 说明 |
|------|-----------|-----------|------|
| `id` | `BIGINT PK` | `Long id` | 主键 |
| `kid` | `VARCHAR(64) UNIQUE` | `String kid` | 密钥 ID（写入 JWT header） |
| `public_key_pem` | `TEXT` | `String publicKeyPem` | 公钥 PEM 内容 |
| `private_key_path` | `VARCHAR(255)` | `String privateKeyPath` | 私钥文件路径 |
| `status` | `TINYINT` | `SigningKeyStatus status` | 密钥状态 |
| `create_time` | `DATETIME(3)` | `LocalDateTime createTime` | 创建时间 |

**索引：**

| 索引名 | 列 | 类型 |
|--------|-----|------|
| `idx_signing_key_status` | `status` | MUL |
| `idx_signing_key_kid` | `kid` | UNI（唯一） |

**状态映射：**

| DB 值 (`status`) | Java 枚举值 | 说明 |
|-----------------|------------|------|
| `0` | `INACTIVE` | 历史密钥（保留验签旧 Token） |
| `1` | `ACTIVE` | 当前签发密钥（仅一个） |

---

### 4.3 `outbox` — 事务性发件箱

**实体路径：** `com.manpou.allinone.domain.model.OutboxEvent`
**用途：** 确保跨库/跨服务操作的原子性（Outbox Pattern）

| 列名 | MySQL 类型 | Java 字段 | 说明 |
|------|-----------|-----------|------|
| `id` | `BIGINT PK` | `Long id` | 主键 |
| `aggregate_type` | `VARCHAR(64)` | `String aggregateType` | 聚合根类型（如 `Procurement`） |
| `aggregate_id` | `VARCHAR(64)` | `String aggregateId` | 聚合根 ID |
| `event_type` | `VARCHAR(64)` | `String eventType` | 事件类型 |
| `payload` | `JSON` | `String payload` | 事件载荷 |
| `status` | `TINYINT` | `OutboxStatus status` | 处理状态 |
| `retry_count` | `INT` | `Integer retryCount` | 已重试次数 |
| `max_retries` | `INT` | `Integer maxRetries` | 最大重试次数（默认 3） |
| `error_msg` | `TEXT` | `String errorMsg` | 最近一次错误信息 |
| `trace_id` | `VARCHAR(64)` | `String traceId` | 链路追踪 ID |
| `create_time` | `DATETIME(3)` | `LocalDateTime createTime` | 创建时间 |
| `update_time` | `DATETIME(3)` | `LocalDateTime updateTime` | 更新时间 |

**索引：**

| 索引名 | 列 | 类型 |
|--------|-----|------|
| `idx_outbox_status` | `status` | MUL |
| `idx_outbox_aggregate` | `aggregate_type, aggregate_id` | MUL |

---

### 4.4 `saga_log` — Saga 分布式事务日志

**用途：** 记录跨服务 saga 步骤执行状态，支持补偿回滚

| 列名 | MySQL 类型 | Java 字段 | 说明 |
|------|-----------|-----------|------|
| `id` | `BIGINT PK` | `Long id` | 主键 |
| `saga_id` | `VARCHAR(64)` | `String sagaId` | Saga 事务 ID |
| `step_name` | `VARCHAR(64)` | `String stepName` | 步骤名称 |
| `step_order` | `INT` | `Integer stepOrder` | 步骤顺序 |
| `status` | `TINYINT` | `SagaStepStatus status` | 步骤状态 |
| `error_msg` | `TEXT` | `String errorMsg` | 错误信息 |
| `trace_id` | `VARCHAR(64)` | `String traceId` | 链路追踪 ID |
| `create_time` | `DATETIME(3)` | `LocalDateTime createTime` | 创建时间 |
| `update_time` | `DATETIME(3)` | `LocalDateTime updateTime` | 更新时间 |

---

### 4.5 `flyway_schema_history` — Flyway 迁移历史

**用途：** 记录数据库迁移脚本执行历史（Flyway 系统表，只读）

| 列名 | MySQL 类型 | 说明 |
|------|-----------|------|
| `installed_rank` | `INT PK` | 执行顺序 |
| `version` | `VARCHAR(50)` | 版本号（可为 NULL） |
| `description` | `VARCHAR(200)` | 迁移描述 |
| `type` | `VARCHAR(20)` | 迁移类型（如 `SQL`） |
| `script` | `VARCHAR(1000)` | 脚本文件名 |
| `checksum` | `INT` | 脚本校验和（用于检测修改） |
| `installed_by` | `VARCHAR(100)` | 执行人 |
| `installed_on` | `TIMESTAMP` | 执行时间 |
| `execution_time` | `INT` | 执行耗时（毫秒） |
| `success` | `TINYINT(1)` | 是否成功（1=成功） |

> ⚠️ 当前已禁用 Flyway（`flyway.enabled: false`），改用 Hibernate `ddl-auto: update` 管理表结构。
> 如需重新启用，修复 checksum 后将 `enabled` 改为 `true`。

**现有迁移：**

| 版本 | 脚本 | 描述 | 状态 |
|------|------|------|------|
| V1 | `V1__init_schema.sql` | 初始化骨架表 | ✅ 成功 |
| V2 | `V2__outbox_table.sql` | 发件箱表 | ✅ 成功 |
| V3 | `V3__signing_key_table.sql` | 签名密钥表 | ✅ 成功 |

---

## §5 骨架占位表规范

所有 `*_example` 表均遵循统一骨架结构：

```sql
CREATE TABLE {module}_example (
    id           BIGINT PK AUTO_INCREMENT,
    create_time  DATETIME(3)  NOT NULL,
    update_time  DATETIME(3)  NOT NULL,
    create_by    VARCHAR(64)   NOT NULL,
    update_by    VARCHAR(64)   NOT NULL,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    name         VARCHAR(128)  NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 1
);
```

**骨架表清单：**

| 表名 | 目标模块 | 替换条件 |
|------|---------|---------|
| `example` | 用户认证 | 替换为真实 User/Role 表 |
| `customs_example` | 报关管理 | Phase B 货柜管理引入 |
| `finance_example` | 财务结算 | Phase B 财务结算引入 |
| `logistics_example` | 物流管理 | 待定 |
| `notification_example` | 通知管理 | 待定 |
| `product_example` | 商品目录 | Phase A2 商品目录引入 |
| `warehouse_example` | 仓库管理 | 待定 |

---

## §6 字段类型速查

| MySQL 类型 | Java 类型 | 备注 |
|-----------|-----------|------|
| `BIGINT` | `Long` | |
| `INT` | `Integer` | |
| `TINYINT(1)` / `BIT(1)` | `Boolean` | 逻辑删除/状态标志 |
| `DECIMAL(p,s)` | `BigDecimal` | 金额/汇率/价格 |
| `VARCHAR(n)` | `String` | |
| `TEXT` | `String` | PEM 公钥等长文本 |
| `JSON` | `String` / `ObjectNode` | Outbox 事件载荷 |
| `DATETIME` / `DATETIME(3/6)` | `LocalDateTime` | 审计字段 |
| `DATE` | `LocalDate` | 业务日期字段 |
| `TIMESTAMP` | `LocalDateTime` | |
| `ENUM(...)` | Java `enum` | 必须与代码同步 |

---

## §7 安全规范

| 规范 | 说明 |
|------|------|
| 生产密码 | 禁止提交至仓库（`.gitignore` 已配置） |
| 私钥文件 | `keys/private.pem` 不进数据库，仅文件系统存储 |
| 逻辑删除 | 所有业务表使用 `is_deleted` 而非物理删除 |
| JWT 密钥轮换 | `signing_key` 表保留历史密钥，支持旧 Token 验签 |

---

## §8 开发注意事项

1. **Hibernate vs Flyway**：当前使用 `ddl-auto: update`，表结构变更由 JPA 自动同步。如需正式迁移控制，切换至 Flyway 并修复 checksum。
2. **枚举变更**：MySQL `ENUM` 列增加值后，旧 Token/JWT 不受影响，但旧代码需要同步更新 Java enum。
3. **Outbox 消费者**：目前 `outbox` 表为空，待引入事件消费者后自动填充。
4. **分库分表**：当前为单库架构，未来按模块拆分时，`signing_key` / `outbox` 等共享表需独立库。
