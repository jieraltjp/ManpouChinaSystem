# 项目文档：customs-service（报关服务）

> **文档角色**：后端开发工程师视角 → 报关管理
> **对应角色文档**：`docs/role/04-后端开发工程师视角分析.md`

---

## 1. 服务定位

| 维度 | 说明 |
|------|------|
| 服务名 | customs-service |
| 端口 | 18085 |
| 包名 | `com.manpou.customs` |
| 描述 | 出口单据生成 · INVOICE · PACKING_LIST · ATTACHED_SHEET |
| 当前状态 | 脚手架 ✅，核心功能待开发 |

---

## 2. 待实现功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| INVOICE 生成 | P0 | 出口发票（PDF 生成） |
| PACKING_LIST 生成 | P0 | 装箱单 |
| ATTACHED_SHEET 生成 | P1 | 附页（面单） |
| 单据确认 | P1 | 确认/驳回单据 |
| 云端备份 | P2 | 单据 PDF 上传 MinIO |

---

## 3. 数据库表（待创建）

| 版本 | 表名 | 说明 |
|------|------|------|
| V4 | `export_document` | 出口单据表 |

### 出口单据核心字段

```sql
CREATE TABLE export_document (
    id                     BIGINT      NOT NULL  AUTO_INCREMENT  PRIMARY KEY,
    document_no            VARCHAR(32) NOT NULL  UNIQUE,
    purchase_order_id      BIGINT      NOT NULL,
    document_type          VARCHAR(20) NOT NULL,          -- INVOICE/PACKING_LIST/ATTACHED_SHEET
    status                 VARCHAR(20) NOT NULL,
    generated_by           VARCHAR(64),
    generated_at           DATETIME(3),
    confirmed_by           VARCHAR(64),
    confirmed_at           DATETIME(3),
    file_url               VARCHAR(512),                 -- PDF 存储路径
    cloud_backup_status    VARCHAR(20)  DEFAULT 'PENDING',
    -- 审计字段
    create_time            DATETIME(3) NOT NULL,
    update_time            DATETIME(3) NOT NULL,
    create_by              VARCHAR(64) NOT NULL,
    update_by              VARCHAR(64) NOT NULL,
    is_deleted             TINYINT     NOT NULL  DEFAULT 0,
    INDEX idx_purchase_order(purchase_order_id)
);
```

---

## 4. 事件驱动（Kafka）

| Topic | 事件 | 动作 |
|-------|------|------|
| `warehouse.events` | `InspectionCompleted` | 生成出口单据 |
| `procurement.events` | `PurchaseOrderApproved` | 预生成 INVOICE |

---

## 5. 行动项

- [ ] **本周**：设计出口单据表（V4__export_document_table.sql）
- [ ] **下周二**：实现 INVOICE 生成 API
- [ ] **下周三**：集成 PDF 生成（iText / OpenPDF）
- [ ] **持续**：单据 PDF 对接 MinIO

---

## 6. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
