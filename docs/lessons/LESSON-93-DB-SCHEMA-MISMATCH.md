---
name: lesson-93-db-schema-mismatch
description: 数据库列与 Entity 映射不一致导致 500
type: reference
---

# Lesson 93: 数据库列与 Entity 映射不一致导致 500

## 错误信息

```
Column 'procurement_return_reason' not found.
```
或
```
Column 'procurement_code' not found.
```

## 根因

`OrderChainView.java` Entity 映射了 `v_order_chain_v1` 视图的列，但视图 SQL 与 DB 实际列名不匹配。

## 教训

| 问题 | 说明 |
|------|------|
| **procurement 表无 `procurement_code`** | V15 DDL 中没有此列，Entity 多映射了该字段 |
| **procurement 有 `return_reason`** | V22 迁移实际执行了（有数据） |
| **qc_record 有 `qc_create_time` 列名歧义** | DB 列名是 `create_time`，视图 alias 为 `qc_create_time` |

## 修复方法

手动重建视图 `v_order_chain_v1`：

- `procurement_code` → `NULL AS procurement_code`（DB 无此列）
- `procurement_return_reason` → `p.return_reason`（DB 有此列）
- `qc_create_time` → `q.create_time AS qc_create_time`

## MySQL 连接信息（192.168.13.202:23306）

- 用户：`root`
- 密码：`manpou23306`
- 数据库：`manpou`

## 正确做法

视图变更后，应同步更新 `OrderChainView.java` Entity 的 `@Column` 映射，确保与 DB 视图列名一致。日常可通过 `DESCRIBE` 表 / 视图来验证。
