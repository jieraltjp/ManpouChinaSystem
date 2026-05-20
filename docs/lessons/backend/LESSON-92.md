# Lesson 92: JPA `findByMasterCode` 返回多条记录导致 NonUniqueResultException 500

> **发现日期**: 2026-05-20
> **项目**: ManpouChinaSystem / manpou-allinone
> **教训**: Spring Data JPA 的 `findByXxx` 方法返回 `Optional<T>`，底层调用 `getSingleResult()`，多条结果直接抛异常

---

## 问题

`/procurement/demand` 页面点击"生成需求"，`POST /api/v1/demands` 返回 **500 Internal Server Error**。

日志：
```
org.hibernate.NonUniqueResultException: Query did not return a unique result: 8 results were returned
```

---

## 根因

`ProductQueryPortImpl.findByMasterCode()` 调用：

```java
return productRepository.findByMasterCodeAndDeletedIsFalse(masterCode);
```

`findByMasterCodeAndDeletedIsFalse` 返回 `Optional<Product>`，底层 Hibernate 调用 `Query.getSingleResult()`。当数据库中有多条 `master_code` 相同的商品时（如 8 条），抛出 `NonUniqueResultException`。

数据库中实际有 8 条商品的 `master_code` 重复（其中 7 条是无效/重复数据）。

---

## 修复

改用 `findByMasterCodeAndSubCodeIsNullAndDeletedIsFalse`（只查 master-level 商品）：

```java
// ProductQueryPortImpl.java
@Override
public Optional<Product> findByMasterCode(String masterCode) {
    // 用 master-level 查询（sub_code IS NULL），避免 master_code 重复导致 NonUniqueResultException
    return productRepository.findByMasterCodeAndSubCodeIsNullAndDeletedIsFalse(masterCode);
}
```

---

## 预防

| 场景 | 检查项 |
|------|--------|
| `findByXxx` 返回 `Optional<T>` | 确认查询条件能保证唯一性，或改用 `findAllByXxx` 返回 `List` |
| 商品表 master_code | 建唯一索引，定期清理重复数据 |
