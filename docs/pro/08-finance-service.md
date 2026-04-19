# 项目文档：finance-service（财务服务）

> **文档角色**：后端开发工程师视角 → 财务管理
> **对应角色文档**：`docs/role/04-后端开发工程师视角分析.md`

---

## 1. 服务定位

| 维度 | 说明 |
|------|------|
| 服务名 | finance-service |
| 端口 | 18087 |
| 包名 | `com.manpou.finance` |
| 描述 | 退税管理 · 费用明细 · 发票管理 · 收款核销 |
| 当前状态 | 脚手架 ✅，核心功能待开发 |

---

## 2. 待实现功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 退税单管理 | P0 | 退税路径/税率/发票状态 |
| 费用明细 | P1 | 海运费/内陆费/保险费等 |
| 发票管理 | P2 | 发票状态跟踪（OBTAINED/PENDING/MISSING） |
| 收款核销 | P2 | 收款与采购单关联 |

---

## 3. 数据库表（待创建）

| 版本 | 表名 | 说明 |
|------|------|------|
| V4 | `tax_refund` | 退税单表 |
| V5 | `fee_detail` | 费用明细表 |

### 退税单核心字段

```sql
CREATE TABLE tax_refund (
    id                    BIGINT       NOT NULL  AUTO_INCREMENT  PRIMARY KEY,
    refund_no             VARCHAR(32)  NOT NULL  UNIQUE,
    purchase_order_id     BIGINT       NOT NULL,
    refund_path           VARCHAR(20)  NOT NULL,              -- CHAOHUI/ZHELU/FACTORY/MISC
    amount                DECIMAL(12,2),
    tax_rate              DECIMAL(5,3),
    invoice_status        VARCHAR(20)  NOT NULL,              -- OBTAINED/PENDING/MISSING
    status                VARCHAR(20)  NOT NULL,
    handler_id            BIGINT,
    handled_at            DATETIME(3),
    remark                VARCHAR(512),
    -- 审计字段
    create_time           DATETIME(3)  NOT NULL,
    update_time           DATETIME(3)  NOT NULL,
    create_by             VARCHAR(64)  NOT NULL,
    update_by             VARCHAR(64)  NOT NULL,
    is_deleted            TINYINT      NOT NULL  DEFAULT 0,
    INDEX idx_purchase_order(purchase_order_id),
    INDEX idx_status(status)
);
```

### 费用明细核心字段

```sql
CREATE TABLE fee_detail (
    id                    BIGINT       NOT NULL  AUTO_INCREMENT  PRIMARY KEY,
    purchase_order_id     BIGINT,
    fee_type              VARCHAR(30)  NOT NULL,              -- OCEAN_FREIGHT/INLAND_FREIGHT/INSURANCE...
    amount                DECIMAL(12,2),
    currency              VARCHAR(10)  NOT NULL  DEFAULT 'CNY',
    payer                 VARCHAR(64),
    claim_status          VARCHAR(20)  NOT NULL  DEFAULT 'UNCLAIMED',
    invoice_attached      TINYINT      NOT NULL  DEFAULT 0,
    -- 审计字段
    create_time           DATETIME(3)  NOT NULL,
    update_time           DATETIME(3)  NOT NULL,
    create_by             VARCHAR(64)  NOT NULL,
    update_by             VARCHAR(64)  NOT NULL,
    is_deleted            TINYINT      NOT NULL  DEFAULT 0
);
```

---

## 4. 事件驱动（Kafka）

| Topic | 事件 | 动作 |
|-------|------|------|
| `customs.events` | `DocumentsConfirmed` | 生成退税单 |
| `logistics.events` | `ChinaExportCompleted` | 更新财务状态 |

---

## 5. 行动项

- [ ] **本周**：设计退税单表（V4__tax_refund_table.sql）
- [ ] **下周二**：实现退税单 CRUD API
- [ ] **持续**：费用明细管理

---

## 6. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
