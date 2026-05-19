# Lesson 88: JPQL 返回枚举字段强转为 String 导致 ClassCastException

> **发现日期**: 2026-05-19
> **项目**: ManpouChinaSystem
> **教训**: JPQL 查询枚举类型字段时，结果集元素是 Java 枚举对象，不是 String

---

## 问题

`POST /api/v1/products/batch-categories` 返回 500：

```
java.lang.ClassCastException:
  class com.manpou.allinone.product.domain.model.ProductCategory
  cannot be cast to class java.lang.String
```

---

## 根因

`Product.category` 字段定义为枚举类型：

```java
// Product.java
private ProductCategory category; // OEM / ORDINARY / FACTORY_DIRECT
```

JPQL 查询返回该字段：

```java
@Query("SELECT DISTINCT p.masterCode, p.category FROM Product p WHERE p.masterCode IN :masterCodes")
List<Object[]> findCategoryByMasterCodes(@Param("masterCodes") List<String> masterCodes);
```

使用 `Object[]` 结果集时，元素类型是 **Java 枚举对象**，不是 String。错误代码：

```java
// 错误：强转 String → ClassCastException
row -> (String) row[1]
```

---

## 正确写法

```java
row -> row[1] == null ? null : ((ProductCategory) row[1]).name()
```

`enum.name()` 返回 "ORDINARY" / "OEM" / "FACTORY_DIRECT" 等字符串。

---

## 预防

| 场景 | 处理方式 |
|------|---------|
| JPQL 返回枚举字段 | 用 `((EnumType) row[N]).name()` 转 String |
| 枚举可能为 null | 先判 null 再调 `.name()` |
| 不想暴露枚举名 | 在 VO 中用 `@JsonValue` 注解自定义序列化方法 |

## 关联 Lesson

- Lesson 51: JPQL 查询字段名 = 实体字段名，非数据库列名
- Lesson 38: 业务逻辑校验在入口处，零值/空值必须防御
