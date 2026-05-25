# Lesson 96: legacy-procurement overdue SQL 性能优化与日期格式解析

> **发现日期**: 2026-05-25
> **项目**: ManpouChinaSystem / manpou-allinone
> **教训**: 遗留数据接口慢 → 全量加载 → Java 过滤 → 改 SQL 层过滤；日期字符串格式混用 → 统一 SQL STR_TO_DATE 解析

---

## 问题

`/api/v1/legacy-procurements/overdue` 接口返回超期记录，耗时 13+ 秒。

**根因链路**：
1. 初期实现：Java 全量加载 `findByDeletedFalse()`（全表 7 万条）→ 流式过滤 → 返回 3.6 万条 25MB
2. 改用 SQL 过滤后：原生 SQL `STR_TO_DATE` 无法解析混合格式（`M/d` vs `MM.dd`）→ 所有记录都判定为 overdue
3. 数据量：返回 36,300 条，25MB，耗时 ~1.3s（SQL 1s + 网络传输）

---

## 分析方法

```sql
-- 查 DB 列名（不能用下划线，要用反引号）
mysql> DESCRIBE `legacy_import_list1`;

-- 查 arrival-depo 实际数据格式
SELECT `arrival-depo`, COUNT(*) as cnt
FROM `legacy_import_list1`
WHERE is_deleted=0 AND `arrival-depo` IS NOT NULL AND `arrival-depo`!=''
GROUP BY `arrival-depo`
ORDER BY LENGTH(`arrival-depo`), `arrival-depo`
LIMIT 30;
```

**发现**：表中有两种日期格式混用：
- `MM.dd`：如 `4.15`、`12.27`（24286 条）
- `M/d`：如 `10/24`、`6/23`（111 条）
- 非日期数字：如 `0`、`1`、`90`（33 条）

**overdue 判定逻辑**：先看 `arrival-depo`（有格式日期），没有或解析失败再看 `yoyaku-hasoubi`。

---

## 最终 SQL

```java
@Query(value = """
        SELECT * FROM `legacy_import_list1`
        WHERE `is_deleted` = 0
          AND `ID` > 50000
          AND (
            (
              `arrival-depo` IS NOT NULL
              AND `arrival-depo` != ''
              AND (
                -- MM.dd 格式: 形如 4.15, 12.27
                (LOCATE('/', `arrival-depo`) = 0 AND LOCATE('.', `arrival-depo`) > 0
                 AND STR_TO_DATE(CONCAT('2026/', `arrival-depo`), '%Y/%c.%d') IS NOT NULL
                 AND STR_TO_DATE(CONCAT('2026/', `arrival-depo`), '%Y/%c.%d') < CURDATE())
                OR
                -- M/d 格式: 形如 10/24, 6/23
                (LOCATE('/', `arrival-depo`) > 0
                 AND STR_TO_DATE(CONCAT('2026/', `arrival-depo`), '%Y/%c/%d') IS NOT NULL
                 AND STR_TO_DATE(CONCAT('2026/', `arrival-depo`), '%Y/%c/%d') < CURDATE())
              )
            )
            OR (`yoyaku-hasoubi` IS NOT NULL AND `yoyaku-hasoubi` < CURDATE())
          )
        """, nativeQuery = true)
List<LegacyProcurement> findOverdueExcludeDeleted();
```

**关键点**：
- `CONCAT('2026/', arrival-depo)` 拼接前缀，用 `%c`（不补零）和 `%d` 解析
- `IS NOT NULL` 过滤掉解析失败的结果（`STR_TO_DATE` 返回 NULL）
- 列名含连字符（如 `arrival-depo`）用反引号包裹
- JPA 默认转下划线 `legacyId → legacy_id`，实际列名是 `ID`（大写），需查 `DESCRIBE` 确认

---

## 性能对比

| 方案 | 数据量 | 耗时 |
|------|--------|------|
| Java 全量过滤 | 25MB / 36300 条 | ~13s |
| SQL 过滤（无格式判断） | 25MB / 36300 条 | ~1.3s |
| SQL 过滤 + ID > 50000 | 12MB / 16545 条 | **0.6s** |

---

## 如何避免

1. **遗留数据接口必须用 SQL 层过滤**，禁止 `findAll()` 全量加载到 Java
2. **日期格式混用字段**：先在 DB 层分析实际数据分布，再决定 SQL 解析逻辑
3. **列名含连字符**：原生 SQL 用反引号，HQL/JPQL 用 `@Column(name = "xxx")` 显式映射
4. **JPA Entity 列名映射**：`legacyId` 无 `@Column` → JPA 默认转 `legacy_id`，实际 DB 是 `ID`（大写），需查 `DESCRIBE` 确认
5. **先写 SQL 验证，再写 Java**：`mysql -e "SELECT ..."` 直接测，避免反复重启服务
