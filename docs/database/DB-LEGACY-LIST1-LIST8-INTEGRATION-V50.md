# 旧系统数据导入 — V50+ 数据库直接写入方案

> 日期：2026-05-24
> 源文件：`d:\Programme\database\20260524\list8.sql`、`list1.sql`
> 目标：V51/V52/V53 Flyway 迁移脚本
>
> 方式：**源数据入临时表 → INSERT...SELECT 直接写入目标表**

---

## 一、迁移文件清单

| 文件 | 内容 |
|------|------|
| `V51__legacy_fields_and_constraints.sql` | ALTER TABLE + legacy 字段 + **legacy_id_mapping 表** + NOT NULL 放宽 |
| `V52__legacy_import_list1.sql` | 创建 `legacy_import_list1` 表 + INSERT...SELECT → procurement |
| `V53__legacy_import_list8.sql` | 创建 `legacy_import_list8` 表 + Factory + INSERT...SELECT → logistics_plan |

---

## 二、执行顺序

```
1. 手动加载源数据到临时表
   (sed 替换 INSERT INTO 表名后导入，见各文件注释)

2. Flyway 自动执行：
   V51: ALTER TABLE + 字段扩展 + 约束放宽
   V52: INSERT...SELECT list1 → procurement
   V53: INSERT...SELECT list8 → logistics_plan
```

---

## 三、源数据加载命令

### list1（71,797 行）

```bash
# 1. V51 执行后，创建临时表
# （Flyway V52 会自动 CREATE TABLE legacy_import_list1）

# 2. 用 sed 替换原始 SQL 表名后导入
# 注意：MySQL strict 模式下 '0000-00-00' date 被拒绝，需同步替换为 NULL
sed -e "s/INSERT INTO \`list1\`/INSERT INTO legacy_import_list1/g" \
    -e "s/'0000-00-00'/NULL/g" \
    "d:/Programme/database/20260524/list1.sql" \
    | mysql -u root -p manpou

# 3. 验证
mysql -u root -p manpou -e "SELECT COUNT(*) FROM legacy_import_list1;"
# 预期：71688（包含表头行，实际数据 71797 行）
```

### list8（3,661 行）

```bash
sed -e "s/INSERT INTO \`list8\`/INSERT INTO legacy_import_list8/g" \
    -e "s/'0000-00-00'/NULL/g" \
    "d:/Programme/database/20260524/list8.sql" \
    | mysql -u root -p manpou

mysql -u root -p manpou -e "SELECT COUNT(*) FROM legacy_import_list8;"
# 预期：3661
```

---

## 四、V51：Schema 扩展

文件：`V51__legacy_fields_and_constraints.sql`

### 4.0 Legacy ID 映射表（新增）

用于记录每个原始记录是否成功导入及对应的目标主键：

```sql
CREATE TABLE legacy_id_mapping (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_table   VARCHAR(32) NOT NULL COMMENT 'list1 或 list8',
    legacy_id      BIGINT     NOT NULL COMMENT '原始表主键',
    target_table   VARCHAR(32) NOT NULL COMMENT 'procurement / logistics_plan',
    target_id      BIGINT     COMMENT '新系统主键（可NULL）',
    product_code   VARCHAR(64) COMMENT '货号',
    import_status  VARCHAR(32) NOT NULL DEFAULT 'imported'
                   COMMENT 'imported=已导入 / skipped=跳过 / duplicate=重复',
    skip_reason    VARCHAR(255) COMMENT '跳过原因',
    create_time    DATETIME(3) NOT NULL DEFAULT NOW(3),

    UNIQUE KEY uk_source_legacy_id (source_table, legacy_id),
    INDEX idx_target (target_table, target_id),
    INDEX idx_product_code (product_code)
);
```

### 4.1 Procurement 扩展

```sql
-- legacy 溯源字段
legacy_list1_id, legacy_order_group, legacy_item_name,
legacy_img, legacy_inspect_qty, legacy_fba_stock,
legacy_container_no, legacy_updater, legacy_updatetime,
legacy_is_legacy

-- NOT NULL 约束放宽（legacy 数据中 price_rmb/exchange_rate/tax_point 大量为 0/空）
price_rmb     DECIMAL(12,4)  -- 取消 NOT NULL
exchange_rate DECIMAL(10,4)  -- 取消 NOT NULL
tax_point     DECIMAL(5,4)   -- 取消 NOT NULL
```

### 4.2 LogisticsPlan 扩展

```sql
-- legacy 溯源字段
legacy_list8_id, legacy_pieces, legacy_destination,
legacy_warehouse, legacy_location, legacy_material, legacy_kensa,
legacy_show_flag, legacy_status,
legacy_unit_ch, legacy_rate,
legacy_updater, legacy_updatetime,
legacy_is_legacy

-- cargo_weight_kg 取消 NOT NULL
```

### 4.3 Factory 扩展

```sql
legacy_source, legacy_souko, legacy_location, legacy_is_warehouse
```

---

## 五、V52：list1 → Procurement

文件：`V52__legacy_import_list1.sql`

### 5.1 字段映射

| list1 字段 | procurement 字段 | 表达式 |
|-----------|----------------|--------|
| `code` | `product_code` | `UPPER(TRIM(code))` |
| `sub-code` | `sub_product_code` | `NULLIF(TRIM(`sub-code`), '')` |
| `order-count` | `quantity` | 直接 |
| `unit-ch` | `price_rmb` | `NULLIF(unit_ch, 0)` **（kaitsuke 100% NULL）** |
| `rate` | `exchange_rate` | `NULLIF(rate, 0)` |
| `hyoten` | `tax_point` | `NULLIF(hyoten, 0)` |
| `arrival-depo` | `destination` | `NULLIF(TRIM(arrival_depo), '')` |
| `yoyaku-hasoubi` | `planned_ship_date` | `CASE WHEN != '0000-00-00'` |
| `departure` | `actual_ship_date` | `CASE WHEN != '0000-00-00'` |
| `material-ch/material` | `material` | `COALESCE(NULLIF(TRIM(material_ch),''), NULLIF(TRIM(material),''))` |
| `note` | `customs_remarks` | `NULLIF(TRIM(note), '')` |
| 全部记录 | `status='完了'` | `'完了'` |
| `updater` | `legacy_updater` | `TRIM(updater)`（**不映射 japanLead**） |
| `ID` | `legacy_list1_id` | 直接 |
| `order-group` | `legacy_order_group` | `TRIM(order_group)` |
| `item-name` | `legacy_item_name` | `TRIM(item-name)` |
| `container` | `legacy_container_no` | `NULLIF(TRIM(container), '')` |

### 5.2 幂等条件

排除 code 与现有 procurement 重复：
```sql
AND UPPER(TRIM(src.code)) NOT IN (
    SELECT UPPER(product_code) FROM procurement
    WHERE is_deleted = FALSE AND legacy_is_legacy = FALSE
)
```

---

## 六、V53：list8 → LogisticsPlan + Factory

文件：`V53__legacy_import_list8.sql`

### 6.1 Factory 创建逻辑

**类型A**：location 为城市名 → `legacy_is_warehouse = TRUE`
**类型B**：souko 含公司名（有限公司/株式会社）→ Factory

souko 90%+ 为空字符串，降级用 location。

### 6.2 字段映射

| list8 字段 | logistics_plan 字段 | 表达式 |
|-----------|-------------------|--------|
| `code` | `product_code` | `UPPER(TRIM(code))` |
| `num` | `quantity` | 直接（装箱数量） |
| `pieces` | `legacy_pieces` | 直接（件数） |
| `length` | `cargo_length_cm` | `NULLIF(length, 0)` |
| `weight2>weight` | `net_weight_kg` | weight（净重） |
| `weight2>weight` | `gross_weight_kg` | weight2（毛重） |
| 其他情况 | `cargo_weight_kg` | weight（总重） |
| `date1` | `actual_ship_date` | `CASE WHEN != '0000-00-00'` |
| `other` | `remarks` | `NULLIF(TRIM(other), '')` |
| showFlag=0+`完成` | `status='DELIVERED'` | CASE 表达式 |
| 其他 | `status='PACKED'` | |
| `destination` | `legacy_destination` | `TRIM(destination)` |
| `souko` | `legacy_warehouse` | `TRIM(souko)` |
| `location` | `legacy_location` | `TRIM(location)` |
| `unit_ch` | `legacy_unit_ch` | `NULLIF(unit_ch, 0)`（参考值，80%为0） |
| `updateuser` | `legacy_updater` | `TRIM(updateuser)` |
| `ID` | `plan_code` | `CONCAT('L-LEGACY-', ID)` |

### 6.3 关联逻辑

```sql
-- factory_id: 优先 souko 匹配，其次 location 匹配
-- procurement_id: 通过 product_code 匹配
```

---

## 七、关键修正（对比之前版本）

| # | 之前版本（错误） | 修正后（正确） |
|---|----------------|-------------|
| 1 | list8.`weight` → `net_weight_kg` | weight→`cargo_weight_kg`；weight2→`gross_weight_kg`；weight→`net_weight_kg` |
| 2 | list1.`updater` → `japanLead` | **不映射**，存入 `legacy_updater` |
| 3 | list8.`unit_ch` 作为采购价 | 存 `legacy_unit_ch`（参考值，80%为0） |
| 4 | `cargo_weight_kg` 在 weight2>weight 时=null | 改为 `cargo_weight_kg = weight`（净重） |
| 5 | 跳过临时表 | 保留临时表（INSERT...SELECT 需要源表） |
| 6 | SELECT FROM source SQL | 先加载入临时表，再 INSERT...SELECT |

---

## 八、执行验证

```sql
-- =============================================
-- legacy_id_mapping 对账（最核心）
-- =============================================
-- list1 导入结果
SELECT
    source_table,
    import_status,
    COUNT(*) AS cnt
FROM legacy_id_mapping
WHERE source_table IN ('list1', 'list8')
GROUP BY source_table, import_status;

-- 预期 list1:
-- source_table | import_status | cnt
-- list1        | imported      | ~71000
-- list1        | skipped       | ~1000（已存在code）

-- 预期 list8:
-- list8        | imported      | ~3600
-- list8        | skipped       | ~0~50（已存在code）

-- 核对：legacy_id_mapping 数量 vs 源表数量
SELECT
    'list1' AS source,
    COUNT(*) AS source_rows,
    (SELECT COUNT(*) FROM legacy_id_mapping WHERE source_table = 'list1') AS mapped_rows
FROM legacy_import_list1
UNION ALL
SELECT
    'list8' AS source,
    COUNT(*) AS source_rows,
    (SELECT COUNT(*) FROM legacy_id_mapping WHERE source_table = 'list8') AS mapped_rows
FROM legacy_import_list8;

-- =============================================
-- procurement legacy 统计
-- =============================================
SELECT
    COUNT(*)                                       AS total,
    SUM(legacy_is_legacy = 1)                      AS legacy_count,
    SUM(price_rmb IS NOT NULL)                     AS has_price,
    SUM(destination IS NOT NULL)                   AS has_destination,
    SUM(container_id IS NOT NULL)                  AS has_container
FROM procurement WHERE legacy_is_legacy = 1;

-- =============================================
-- logistics_plan legacy 统计
-- =============================================
SELECT
    COUNT(*)                                       AS total,
    SUM(legacy_is_legacy = 1)                      AS legacy_count,
    SUM(net_weight_kg IS NOT NULL)                AS has_net_weight,
    SUM(factory_id IS NOT NULL)                   AS has_factory,
    SUM(procurement_id IS NOT NULL)               AS has_procurement
FROM logistics_plan WHERE legacy_is_legacy = 1;

-- =============================================
-- factory legacy 统计
-- =============================================
SELECT
    COUNT(*)                                       AS total,
    SUM(legacy_is_legacy = 1)                      AS legacy_count
FROM factory WHERE legacy_source = 'list8';

-- =============================================
-- OrderChain 视图：legacy 数据应为全链路 COMPLETED
-- =============================================
SELECT status, COUNT(*) FROM procurement
WHERE legacy_is_legacy = 1 GROUP BY status;
-- 预期：status='完了'
```
