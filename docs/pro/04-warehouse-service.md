# 项目文档：warehouse-service（仓储服务）

> **文档角色**：后端开发工程师视角 → 仓储管理
> **对应角色文档**：`docs/role/04-后端开发工程师视角分析.md`

---

## 1. 服务定位

| 维度 | 说明 |
|------|------|
| 服务名 | warehouse-service |
| 端口 | 18084 |
| 包名 | `com.manpou.warehouse` |
| 描述 | 到货登记 · 质检 · 货物照片 · 库存管理 |
| 当前状态 | 脚手架 ✅，核心功能待开发 |

---

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 3.2.5 | 应用框架 |
| Spring Data JPA | ORM |
| Spring Security | 认证/鉴权 |
| H2（开发）/ MySQL 8（生产） | 数据库 |
| Flyway | 数据库迁移 |

---

## 3. 项目结构

```
src/main/java/com/manpou/warehouse/
├── WarehouseServiceApplication.java   # 启动类
├── interfaces/controller/            # REST 入口（待实现）
├── application/dto/                  # DTO（待实现）
├── application/usecase/             # 用例（待实现）
├── domain/model/
│   ├── ArrivalRecord.java           # 到货记录聚合根（待实现）
│   ├── CargoPhoto.java              # 货物照片实体（待实现）
│   └── WarehouseStatus.java         # 状态枚举（待实现）
├── infrastructure/config/            # 配置
└── common/                          # 通用组件

src/main/resources/
├── application.yml                   # 18084 端口
└── db/migration/
    ├── V1__init_schema.sql
    ├── V2__outbox_table.sql
    └── V3__signing_key_table.sql
```

---

## 4. 待实现功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 到货登记 | P0 | 采购单到货后登记（AR+yyyyMMdd+序号） |
| 质检评分 | P0 | 货物质检 + 评分 |
| 货物照片 | P1 | 到货/加工/入库照片（关联 MinIO） |
| 库存查询 | P1 | 按工厂代码分库查询 |
| 预警机制 | P2 | 货物滞留预警（priority=URGENT 红色） |

---

## 5. 数据库表（待创建）

| 版本 | 表名 | 说明 |
|------|------|------|
| V4 | `arrival_record` | 到货记录 |
| V5 | `cargo_photo` | 货物照片 |

### 到货记录核心字段

```sql
CREATE TABLE arrival_record (
    id                  BIGINT      NOT NULL  AUTO_INCREMENT  PRIMARY KEY,
    arrival_no          VARCHAR(32) NOT NULL  UNIQUE,    -- AR+yyyyMMdd+序号
    purchase_order_id   BIGINT      NOT NULL,
    warehouse_code      VARCHAR(32) NOT NULL,             -- Yongkang
    arrival_date        DATE,
    status              VARCHAR(20) NOT NULL,
    priority            VARCHAR(20) NOT NULL  DEFAULT 'NONE', -- URGENT=红色预警
    inspector_id        BIGINT,
    inspector_name      VARCHAR(64),
    inspection_date    DATETIME(3),
    remark              VARCHAR(512),
    -- 审计字段
    create_time         DATETIME(3) NOT NULL,
    update_time         DATETIME(3) NOT NULL,
    create_by           VARCHAR(64) NOT NULL,
    update_by           VARCHAR(64) NOT NULL,
    is_deleted          TINYINT     NOT NULL  DEFAULT 0,
    INDEX idx_purchase_order(purchase_order_id),
    INDEX idx_inspector(inspector_id)
);
```

---

## 6. 事件驱动（Kafka 消费者）

| Topic | 事件 | 动作 |
|-------|------|------|
| `procurement.events` | `ProcurementApproved` | 创建到货记录 |
| `procurement.events` | `ProcurementShipped` | 更新物流状态 |

---

## 7. 行动项

- [ ] **本周**：设计到货记录表（V4__arrival_record_table.sql）
- [ ] **下周二**：实现到货登记 API
- [ ] **下周三**：实现 Kafka 消费者（监听 procurement.events）
- [ ] **持续**：货物照片对接 MinIO 存储

---

## 8. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
