# DB-HS-JP — 日本 HS 税番数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-27
> **状态**: ✅ 已实现（外部参考表，仅供查询）
> **对应业务文档**: `SPEC-B06-日本清关-步骤6.md`
> **数据来源**: `esagf_oem.jp_hs_code`（原始导入）
> **说明**: 独立查询表，不参与业务流程聚合根管理

---

## 表清单

| 序号 | 表名 | 用途 | 行数 | 状态 |
|------|------|------|------|------|
| 1 | `jp_hs_code` | 日本 HS 税番 | 9694 | ✅ 查询用 |

---

## 1. jp_hs_code（日本 HS 税番）

**用途**: 清关时查询日本 HS 税番对应的各类税率（基本/暂定/WTO协定/RCEP等）。

> **性质**: 外部参考表（Read-only），由 `esagf_oem` 数据库导入，供独立查询，不参与业务流程聚合根管理。

```sql
CREATE TABLE jp_hs_code (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    -- 基本信息
    番号               TEXT         COMMENT '税番（HS 8位编码）',
    品名               TEXT         COMMENT '品名',
    単位1              TEXT         COMMENT '第一计量单位',
    単位2              TEXT         COMMENT '第二计量单位',

    -- 税率列（按贸易协定分类，共 19 列）
    基本関税率         TEXT         COMMENT '基本関税率（%）',
    暫定関税率         TEXT         COMMENT '暫定関税率（%）',
    WTO協定関税率      TEXT         COMMENT 'WTO协定関税率（%）',
    特恵関税率         TEXT         COMMENT '特恵関税率（%）',
    特別特恵関税率     TEXT         COMMENT '特別特恵関税率（%）',
    Singapore関税率    TEXT         COMMENT '新加坡协定関税率（%）',
    Mexico関税率       TEXT         COMMENT '墨西哥协定関税率（%）',
    Malaysia関税率     TEXT         COMMENT '马来西亚协定関税率（%）',
    Chile関税率        TEXT         COMMENT '智利协定関税率（%）',
    Thailand関税率     TEXT         COMMENT '泰国协定関税率（%）',
    Indonesia関税率    TEXT         COMMENT '印度尼西亚协定関税率（%）',
    Brunei関税率       TEXT         COMMENT '文莱协定関税率（%）',
    ASEAN関税率        TEXT         COMMENT 'ASEAN 协定関税率（%）',
    Philippines関税率  TEXT         COMMENT '菲律宾协定関税率（%）',
    Switzerland関税率 TEXT         COMMENT '瑞士协定関税率（%）',
    VietNam関税率      TEXT         COMMENT '越南协定関税率（%）',
    India関税率        TEXT         COMMENT '印度协定関税率（%）',
    Peru関税率         TEXT         COMMENT '秘鲁协定関税率（%）',
    RCEP日ANZASEAN税率 TEXT         COMMENT 'RCEP 日本/ANZ/ASEAN 协定関税率（%）',
    RCEP中国税率       TEXT         COMMENT 'RCEP 中国协定関税率（%）',
    RCEP韓国税率       TEXT         COMMENT 'RCEP 韩国协定関税率（%）',

    -- 元数据
    import_batch       VARCHAR(32)  COMMENT '导入批次',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_hs_code (番号(12))  -- 前缀索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT UNSIGNED | 主键 |
| `番号` | TEXT | 税番（8位 HS 编码），前缀索引支持查询 |
| `品名` | TEXT | 商品名称 |
| `単位1` | TEXT | 第一计量单位 |
| `単位2` | TEXT | 第二计量单位 |
| `基本関税率` | TEXT | 基本进口关税率（%） |
| `暫定関税率` | TEXT | 暂定关税率（%） |
| `WTO協定関税率` | TEXT | WTO 协定关税率（%） |
| `特恵関税率` | TEXT | 特惠关税率（%） |
| `特別特恵関税率` | TEXT | 特别特惠关税率（%） |
| `Singapore/Mexico/Malaysia/...関税率` | TEXT | 各双边/多边贸易协定的优惠关税率 |
| `RCEP日ANZASEAN税率` | TEXT | RCEP 协定下日本/澳大利亚·新西兰/ASEAN 优惠税率 |
| `RCEP中国税率` | TEXT | RCEP 中国优惠税率 |
| `RCEP韓国税率` | TEXT | RCEP 韩国优惠税率 |
| `import_batch` | VARCHAR(32) | 导入批次号 |
| `created_at` | TIMESTAMP | 创建时间 |

---

## 索引设计

| 索引名 | 列 | 类型 | 说明 |
|--------|-----|------|------|
| `PRIMARY` | `id` | 主键 | BIGINT UNSIGNED 自增 |
| `idx_hs_code` | `番号(12)` | 前缀索引 | 对 TEXT 列建 12 字符前缀索引，支持税番精确/模糊查询 |

---

## 税率优先级（清关时取值顺序）

```
基本関税率（默认）
    ↓
暫定関税率（存在时优先）
    ↓
WTO協定関税率（存在时优先）
    ↓
特恵関税率 / 特別特恵関税率（如适用FTA）
    ↓
RCEP日ANZASEAN税率（适用 RCEP 成员国）
    ↓
RCEP中国税率 / RCEP韓国税率（如适用）
    ↓
各双边协定税率（Singapore / Mexico / Malaysia / ...）
```

---

## 与 product / japan_customs_record 的关联

```sql
-- product.hs_code_jp → jp_hs_code.番号
-- 日本清关时查询适用税率
SELECT
    j.番号,
    j.品名,
    j.基本関税率,
    j.暫定関税率,
    j.WTO协定関税率,
    j.RCEP中国税率
FROM jp_hs_code j
WHERE j.番号 = '847130000';
```

---

## 数据来源

- **原始表**: `esagf_oem.jp_hs_code`
- **导入方式**: 直接 SQL 导入（`docs/database/sql/`）
- **刷新策略**: 定期从源头同步，不参与 Flyway 版本管理

---

## 已知问题

| 问题 | 严重度 | 说明 |
|------|--------|------|
| 所有字段为 TEXT 类型 | 🟡 P1 | 无法建精确索引，建议后续拆分为 VARCHAR 并建完整索引 |
| 无软删除字段 | 🟡 P1 | 外部参考表通常无需软删除，与本系统审计规范不一致 |
| 无审计字段 | 🟡 P1 | 无 create_by/update_by/update_time，与业务表审计规范不一致 |
| 无业务关联外键 | ✅ 正常 | 外部参考表，无需 FK 约束 |
| 税率列为 TEXT 而非数值 | 🟡 P1 | 需转换为 DECIMAL 才可参与计算 |

---

## E-R 图

```
┌─────────────────────────────┐
│         product             │
│  hs_code_jp (日本 HS 税番)  │
└──────────────┬──────────────┘
               │  1:0..1
               ▼
┌─────────────────────────────┐
│       jp_hs_code            │  外部查询表（Read-only）
│  番号 / 品名 / 19类税率     │
└─────────────────────────────┘

┌──────────────────────────────┐
│  japan_customs_record         │
│  import_duty_paid (JPY)      │
│  consumption_tax_paid (JPY) │
└──────────────┬───────────────┘
               │  查询 jp_hs_code 取适用税率
               ▼
        关税计算 = 商品价格 × 适用税率
```
