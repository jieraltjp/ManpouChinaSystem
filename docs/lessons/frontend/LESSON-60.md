# Lesson 60: Spring Data JPA nativeQuery + Pageable ORDER BY 陷阱

## 问题

`/api/v1/orders/chain` 返回 500：

```
Unknown column 'v.demandCreateTime' in 'order clause'
```

## 根因

当 `Pageable` 有排序时，Spring Data JPA 会在原生 SQL 查询后 **追加** ORDER BY 子句：

```sql
-- 我们的 SQL（nativeQuery = true）
SELECT * FROM v_order_chain_v1 v WHERE ...

-- Spring Data 实际追加的
... order by v.demandCreateTime desc limit ?
                  ^^^^^^^^^^^^^^
                  实体属性名，非数据库列名
```

在原生查询（`nativeQuery = true`）中，Spring Data **不做实体属性→列名的映射**，直接使用实体属性名拼接 SQL。VIEW 列名是 `demand_create_time`，但实体属性是 `demandCreateTime`，所以报错。

## 预防方案

### 方案 A：在 UseCase 层 in-memory 排序（推荐）

```java
// Repository：SQL 中不加 ORDER BY
@Query(value = """
    SELECT * FROM v_order_chain_v1 v
    WHERE ...
    """, nativeQuery = true)
Page<OrderChainView> findChainList(String demandStatus, String keyword, Pageable pageable);

// UseCase：用 Sort.unsorted() 禁止 SQL 层排序，改为 in-memory 排序
PageRequest sortFreeRequest = PageRequest.of(
    pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
Page<OrderChainView> page = repo.findChainList(demandStatus, keyword, sortFreeRequest);

List<OrderChainVO> sorted = page.getContent().stream()
    .sorted(Comparator.comparing(OrderChainView::getDemandCreateTime).reversed())
    .map(this::toChainVO)
    .toList();

return new PageImpl<>(sorted, pageable, page.getTotalElements());
```

### 方案 B：SQL 中显式写 ORDER BY + 覆盖 Sort

若 SQL 中已写了 `ORDER BY v.demand_create_time DESC`，同时 Pageable 有默认 Sort，两个 ORDER BY 冲突。

**禁止** 在 SQL 和 Pageable 同时写 ORDER BY。

### 方案 C：避免使用 Pageable 排序

Controller 层传入 `Sort.unsorted()`，排序完全由 UseCase/Service 层 in-memory 处理。

## 为什么方案 A 最佳

- 原生查询的 ORDER BY 天然是 DB 列名，无法引用实体属性映射
- In-memory 排序利用 JPA 实体 getter 的映射（`getDemandCreateTime()` → JVM 内存对象），无歧义
- 分页 offset/limit 由 SQL 完成，排序由 JVM 完成，性能可接受（数据量 < 10000 时无感知）

## 触发条件

- `nativeQuery = true`
- `Pageable` 参数有排序（默认或显式）
- 实体属性名（驼峰）与数据库列名（下划线）不一致

## 违反后果

排序字段报错 500，API 不可用。

## 相关 Lesson

- Lesson 51：JPQL 查询字段名 = 实体字段名（非数据库列名）— 同类问题不同触发
