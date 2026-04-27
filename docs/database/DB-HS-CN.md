# DB-HS-CN — 中国 HS 编码数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-27
> **状态**: ✅ 已实现（外部参考表，仅供查询）
> **对应业务文档**: `SPEC-B05-国内报关-步骤5.md`
> **数据来源**: `esagf_oem.cn_hs_code`（原始导入）
> **说明**: 独立查询表，不参与业务流程聚合根管理

---

## 表清单

| 序号 | 表名 | 用途 | 行数 | 状态 |
|------|------|------|------|------|
| 1 | `cn_hs_code` | 中国 HS 编码 | 12030 | ✅ 查询用 |

---

## 1. cn_hs_code（中国 HS 编码）

**用途**: 报关时查询中国 HS 编码对应的税率、监管条件、申报要素等。

> **性质**: 外部参考表（Read-only），由 `esagf_oem` 数据库导入，供独立查询，不参与业务流程聚合根管理。

```sql
CREATE TABLE cn_hs_code (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    编码               TEXT         COMMENT 'HS 编码（8-10位）',
    名称               TEXT         COMMENT '商品名称',
    备注               TEXT         COMMENT '备注',
    第一法定单位        TEXT         COMMENT '第一法定计量单位',
    第二法定单位        TEXT         COMMENT '第二法定计量单位',
    监管条件           TEXT         COMMENT '监管条件',
    普通税率           TEXT         COMMENT '普通税率（%）',
    优惠税率           TEXT         COMMENT '优惠税率（%）',
    出口税率           TEXT         COMMENT '出口税率（%）',
    消费税率           TEXT         COMMENT '消费税率（%）',
    增值税率           TEXT         COMMENT '增值税率（%）',
    申报要素           TEXT         COMMENT '申报要素（如：品牌|型号|用途）',
    import_batch       VARCHAR(32)  COMMENT '导入批次',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_hs_code (编码(12))   -- 前缀索引，兼容 8-12 位 HS 编码查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT UNSIGNED | 主键 |
| `编码` | TEXT | HS 编码，8-10位，前缀索引支持模糊查询 |
| `名称` | TEXT | 商品名称 |
| `备注` | TEXT | 备注信息 |
| `第一法定单位` | TEXT | 第一法定计量单位（如：千克、台、件） |
| `第二法定单位` | TEXT | 第二法定计量单位 |
| `监管条件` | TEXT | 监管条件（如：A、B、E 等监管代码） |
| `普通税率` | TEXT | 普通税率，百分比格式 |
| `优惠税率` | TEXT | 优惠税率（协定的优惠税率） |
| `出口税率` | TEXT | 出口退税率 |
| `消费税率` | TEXT | 消费税率 |
| `增值税率` | TEXT | 增值税率 |
| `申报要素` | TEXT | 申报要素，多个要素以 `|` 分隔 |
| `import_batch` | VARCHAR(32) | 导入批次号，便于追踪数据来源 |
| `created_at` | TIMESTAMP | 创建时间 |

---

## 索引设计

| 索引名 | 列 | 类型 | 说明 |
|--------|-----|------|------|
| `PRIMARY` | `id` | 主键 | BIGINT UNSIGNED 自增 |
| `idx_hs_code` | `编码(12)` | 前缀索引 | 对 TEXT 列建 12 字符前缀索引，支持 8-12 位 HS 编码精确/模糊查询 |

---

## 与 product 表的关联

```sql
-- product.hs_code → cn_hs_code.编码
-- 报关时查询对应 HS 编码的税率和申报要素
SELECT c.普通税率, c.优惠税率, c.申报要素, c.监管条件
FROM cn_hs_code c
WHERE c.编码 = '8471300000';
```

---

## 数据来源

- **原始表**: `esagf_oem.cn_hs_code`
- **导入方式**: 直接 SQL 导入（`docs/database/sql/`）
- **刷新策略**: 定期从源头同步，不参与 Flyway 版本管理

---

## 已知问题

| 问题 | 严重度 | 说明 |
|------|--------|------|
| 所有字段为 TEXT 类型 | 🟡 P1 | 无法建精确索引，建议后续拆分为 VARCHAR 类型并建完整索引 |
| 无软删除字段 | 🟡 P1 | 外部参考表通常无需软删除，但与本系统审计规范不一致 |
| 无审计字段 | 🟡 P1 | 无 create_by/update_by/update_time，与业务表审计规范不一致 |
| 无业务关联外键 | ✅ 正常 | 外部参考表，无需 FK 约束 |
