# 项目文档：notification-service（通知服务）

> **文档角色**：后端开发工程师视角 → 通知推送
> **对应角色文档**：`docs/role/04-后端开发工程师视角分析.md`

---

## 1. 服务定位

| 维度 | 说明 |
|------|------|
| 服务名 | notification-service |
| 端口 | 18088 |
| 包名 | `com.manpou.notification` |
| 描述 | 邮件/站内信/钉钉/企业微信通知推送 |
| 当前状态 | 脚手架 ✅，核心功能待开发 |

---

## 2. 待实现功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 邮件通知 | P0 | 采购单状态变更邮件 |
| 站内信 | P1 | 系统消息通知 |
| 钉钉/企微 | P2 | 即时通讯集成 |
| 通知模板 | P1 | 多语言模板管理 |
| 通知历史 | P1 | 已发送通知查询 |

---

## 3. 数据库表（待创建）

| 版本 | 表名 | 说明 |
|------|------|------|
| V4 | `notification` | 通知记录表 |
| V5 | `notification_template` | 通知模板表 |

---

## 4. 事件驱动（Kafka 消费者）

| Topic | 事件 | 动作 |
|-------|------|------|
| `procurement.events` | `PurchaseOrderApproved` | 发送邮件通知 |
| `warehouse.events` | `GoodsArrived` | 发送到货通知 |
| `customs.events` | `DocumentsConfirmed` | 发送单据完成通知 |
| `logistics.events` | `ContainerSealed` | 发送封柜通知 |
| `finance.events` | `RefundProcessed` | 发送退税通知 |

---

## 5. 行动项

- [ ] **本周**：设计通知记录表（V4__notification_table.sql）
- [ ] **下周二**：实现邮件发送服务（Spring Mail）
- [ ] **下周三**：实现 Kafka 消费者监听各服务事件
- [ ] **持续**：通知模板多语言支持

---

## 6. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
