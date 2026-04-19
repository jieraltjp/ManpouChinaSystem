# 项目文档：logistics-service（物流服务）

> **文档角色**：后端开发工程师视角 → 物流管理
> **对应角色文档**：`docs/role/04-后端开发工程师视角分析.md`

---

## 1. 服务定位

| 维度 | 说明 |
|------|------|
| 服务名 | logistics-service |
| 端口 | 18086 |
| 包名 | `com.manpou.logistics` |
| 描述 | 货柜管理 · 拼柜 · 装柜 · 物流轨迹 |
| 当前状态 | 脚手架 ✅，核心功能待开发 |

---

## 2. 待实现功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 货柜管理 | P0 | 货柜信息（箱型/港口/状态） |
| 拼柜计划 | P1 | 多个采购单合并一个货柜 |
| 物流轨迹 | P1 | ETD/ETA 跟踪 |
| 封柜确认 | P1 | 货柜封柜后通知报关 |

---

## 3. 数据库表（待创建）

| 版本 | 表名 | 说明 |
|------|------|------|
| V4 | `container` | 货柜表 |
| V5 | `consolidation_plan` | 拼柜计划表 |

### 货柜核心字段

```sql
CREATE TABLE container (
    id                    BIGINT      NOT NULL  AUTO_INCREMENT  PRIMARY KEY,
    container_no         VARCHAR(32) NOT NULL  UNIQUE,
    container_type       VARCHAR(10) NOT NULL,              -- 20GP/40GP/40HC/45HC
    port                 VARCHAR(20) NOT NULL,              -- QINGDAO/NINGBO/XIAMEN/SHENZHEN/SHANGHAI
    status               VARCHAR(20) NOT NULL,
    loading_date         DATE,
    etd                  DATE,                                -- 预计开船日
    eta                  DATE,                                -- 预计到港日
    actual_departure     DATETIME(3),
    actual_arrival       DATETIME(3),
    consolidation_plan_id BIGINT,
    -- 审计字段
    create_time          DATETIME(3) NOT NULL,
    update_time          DATETIME(3) NOT NULL,
    create_by            VARCHAR(64) NOT NULL,
    update_by            VARCHAR(64) NOT NULL,
    is_deleted           TINYINT     NOT NULL  DEFAULT 0,
    INDEX idx_status(status)
);
```

---

## 4. 事件驱动（Kafka）

| Topic | 事件 | 动作 |
|-------|------|------|
| `customs.events` | `DocumentsConfirmed` | 触发封柜 |
| `logistics.events` | `ContainerSealed` | 通知 customs-service |
| `logistics.events` | `ChinaExportCompleted` | 通知 finance-service |

---

## 5. 行动项

- [ ] **本周**：设计货柜表（V4__container_table.sql）
- [ ] **下周二**：实现货柜 CRUD API
- [ ] **持续**：物流轨迹追踪

---

## 6. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
