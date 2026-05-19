# 工程教训 — 后端（Java / Spring / JPA / DDD）

> 项目：ManpouChinaSystem
> 覆盖范围：Java 编译 / Spring DI / JPA / DDD 分层 / 业务校验
> Lesson 编号：1–6, 10, 25, 29–34, 38, 88（共 18 条）

---

## 目录

- [Lesson 1: Lombok 失效的真正根因是编译顺序](#lesson-1-lombok-失效的真正根因是编译顺序)
- [Lesson 2: 领域模型禁止直接引用其他模块的 Entity/VO](#lesson-2-领域模型禁止直接引用其他模块的-entityvo)
- [Lesson 3: JPA Repository 继承链会产生多个同名 Bean](#lesson-3-jpa-repository-继承链会产生多个同名-bean)
- [Lesson 4: BusinessException API 必须与调用方严格对齐](#lesson-4-businessexception-api-必须与调用方严格对齐)
- [Lesson 5: Test 代码必须与 Model API 同步](#lesson-5-test-代码必须与-model-api-同步)
- [Lesson 6: JPA Repository 方法名不一致导致编译失败](#lesson-6-jpa-repository-方法名不一致导致编译失败)
- [Lesson 10: DTO 与 Entity 混用导致 Order 模块依赖 Procurement](#lesson-10-dto-与-entity-混用导致-order-模块依赖-procurement)
- [Lesson 25: JPA Domain Repository 禁止加 @Repository，避免与 JpaAdapter 重复 Bean](#lesson-25-jpa-domain-repository-禁止加-repository避免与-jpaadapter-重复-bean)
- [Lesson 29: 删除旧注解时必须同步删除所有引用，禁止只删 import](#lesson-29-删除旧注解时必须同步删除所有引用禁止只删-import)
- [Lesson 30: Test 代码必须与 Model/API 同步更新（v1.5→v1.6 重构教训）](#lesson-30-test-代码必须与-modelapi-同步更新v15v16-重构教训)
- [Lesson 31: JSON 存储列必须用 TEXT，不能用 VARCHAR](#lesson-31-json-存储列必须用-text不能用-varchar)
- [Lesson 32: 重构实体移除字段后必须同步清理数据库旧列](#lesson-32-重构实体移除字段后必须同步清理数据库旧列)
- [Lesson 33: 锚点设计决定 Overview 可见性——Procurement 中心导致 Demand 新建后不可见](#lesson-33-锚点设计决定-overview-可见性procurement-中心导致-demand-新建后不可见)
- [Lesson 34: 前端类型定义必须与后端 VO 同步——接口变更后旧字段残留导致-undefined](#lesson-34-前端类型定义必须与后端-vo-同步接口变更后旧字段残留导致-undefined)
- [Lesson 38: 业务逻辑校验必须在入口处，零值/空值校验是防御性编程底线](#lesson-38-业务逻辑校验必须在入口处零值空值校验是防御性编程底线)

---

## Lesson 1: Lombok 失效的真正根因是编译顺序

### 问题

`mvn clean compile` 大量报错：

```
找不到符号: 方法 getXxx()     // Lombok @Getter 未生成
找不到符号: 方法 builder()    // Lombok @Builder 未生成
找不到符号: 方法 conflict()   // Lombok @Slf4j 未生成
找不到符号: 变量 log          // Lombok @Slf4j 未生成
```

### 根因

**不是 Lombok 配置错误，是 javac 编译顺序破坏。**

当模块 A 的 `application` 层直接引用模块 B 的 domain 类时，javac 按字母序编译，product 模块被编译时 factory 模块的 Lombok 还未处理完，导致 product 模块看到的 factory 类是"半成品"——字段存在但无 getter/setter。

### 解决方案：依赖倒置（Port 接口）

```
模块 A 的 application 层  ──依赖──►  common.port.XxxQueryPort  (接口)
                                          ▲
模块 B 的 infrastructure 层  ──依赖──►  B 的 domain 类
```

### 预防

- 任何跨模块的 `application → domain` 直接引用都要过 Port 接口
- domain model 只引用 `common.enums` / `common.exception` 等纯数据类

---

## Lesson 2: 领域模型禁止直接引用其他模块的 Entity/VO

### 问题

`Procurement.suggestNextStatus(QcRecord, Product)` — 在 `Procurement` entity 里调用其他模块的 entity，触发跨模块依赖。

### 解决方案

状态判断逻辑放在 application 层的 `ProcurementQcPassedEventListener` 里，通过 Port 获取数据后自行判断：

```java
// ✅ 正确：application 层组合多个 Port
private ShipmentStatus resolveSuggestedStatus(ShipmentStatus current, QcRecord qcRecord, Product product) {
    if (current == ShipmentStatus.現地検品) {
        return ShipmentStatus.メーカー直送;
    }
    if (qcRecord.getQcType() == QcType.ONSITE) {
        if (product != null && product.getVolumeCbm() != null
                && product.getVolumeCbm().compareTo(new BigDecimal("0.5")) <= 0) {
            return ShipmentStatus.エア便;
        }
        return ShipmentStatus.輸出;
    }
    return ShipmentStatus.輸出;
}
```

### 预防

- `grep -r "import com.manpou.allinone\.[a-z]+\.domain\.model" src/main/java/` — 定期扫描
- domain model 只 import `common.enums`、`common.exception`、`jakarta.persistence`

---

## Lesson 3: JPA Repository 继承链会产生多个同名 Bean

### 问题

运行时启动失败：找到 2 个 `productRepository` bean，无法注入。

### 根因

```java
// ProductRepository — 领域层接口（契约）
public interface ProductRepository extends JpaRepository<Product, Long> { ... }

// ProductJpaRepository — 基础设施层 JPA 实现
@Repository
public interface ProductJpaRepository extends ProductRepository, JpaRepository<Product, Long> { }
```

Spring 把 `ProductRepository` 和 `ProductJpaRepository` 都注册为 bean，ID 分别是 `productRepository` 和 `productJpaRepository`。

### 解决方案

```java
@Component
public class ProductQueryPortImpl implements ProductQueryPort {
    public ProductQueryPortImpl(
            @Qualifier("productJpaRepository") ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

### 预防

- 如果 `XxxJpaRepository extends XxxRepository`，始终用 `@Qualifier("xxxJpaRepository")` 注入

---

## Lesson 4: BusinessException API 必须与调用方严格对齐

### 问题

编译报错：`找不到符号: 方法 conflict(java.lang.String, java.lang.String)`

### 根因

`BusinessException.conflict(String message)` 只接受一个参数，但调用方用了两个参数。

### 解决方案

保留两个入口，职责明确：
- 构造函数 `new BusinessException(code, message)` — 直接构造，code 语义完整
- 工厂方法 `BusinessException.conflict(message)` — 自动填 code

### 预防

- 添加新工厂方法前：`grep -r "BusinessException\." src/`
- 所有业务异常走 `BusinessException`，禁止裸 `throw new RuntimeException()`

---

## Lesson 5: Test 代码必须与 Model API 同步

### 问题

Test 编译报错：`找不到符号: 方法 setLocation(java.lang.String)`

### 根因

`Factory` 从 `location`/`status` 改为 `province`/`city`/`cooperationStatus`，测试代码未同步。

### 解决方案

| 测试错误 | 修复 |
|---------|------|
| `factory.setLocation()` | → `factory.setProvince()` + `factory.setCity()` |
| `FactoryStatus.ACTIVE` | → `CooperationStatus.ACTIVE` |
| `findByIdAndIsDeletedFalse()` | → `findByIdAndDeletedIsFalse()` |

### 预防

- Model 重构后立即搜索所有 test 文件
- `mvn test-compile` 必须在 PR 中通过

---

## Lesson 6: JPA Repository 方法名不一致导致编译失败

### 问题

```
error: conflicting: ProductRepository 中的抽象方法 findByMasterCodeAndDeletedIsFalse(String)
```

### 根因

Java 不允许同名方法不同返回类型共存于同一接口。

### 解决方案

重命名，不依赖返回类型区分：
```java
Optional<Product> findByMasterCodeAndDeletedIsFalse(String masterCode);
List<Product> findAllByMasterCodeAndDeletedIsFalse(String masterCode);
```

### 预防

- Repository 方法命名不依赖返回类型区分同名方法
- 子接口不重复声明父接口已有方法（除非有 `@Query`）

---

## Lesson 10: DTO 与 Entity 混用导致 Order 模块依赖 Procurement

### 问题

`OrderOverviewController` 返回 `Page<ProcurementPageQuery>`，Order 直接引用了 Procurement 的 DTO。

### 解决方案

每个模块定义自己的 API DTO，通过 Assembler 转换：
```java
// Order 模块自己定义
public class OrderProcurementSelectorDTO { ... }

// Controller 使用自己模块的 DTO
public Result<Page<OrderProcurementSelectorDTO>> selector(...) { ... }
```

### 预防

- Controller 的请求/响应类型必须属于自己的模块
- 定期审计 Controller 返回类型的包路径

---

## Lesson 25: JPA Domain Repository 禁止加 @Repository

### 问题

Spring 启动失败：`expected single matching bean but found 2: productRepository, productJpaRepository`

### 根因

领域层接口 `ProductRepository` 加了 `@Repository`，JPA 适配器 `ProductJpaRepository` 也 extends 它，两个都被注册为 bean。

### 解决方案

```java
// ✅ 正确：领域接口无 @Repository
public interface ProductRepository extends JpaRepository<Product, Long> { ... }

// ✅ 正确：JPA 适配器带 @Repository
@Repository
public interface ProductJpaRepository extends ProductRepository, JpaRepository<Product, Long> { }
```

### 预防

- 领域层接口**禁止加 `@Repository`**
- 用 `@EnableJpaRepositories(basePackages = ...)` 精确控制扫描范围

---

## Lesson 29: 删除旧注解时必须同步删除所有引用，禁止只删 import

### 问题

`@NotBlank` 改为 `@NotNull` 后编译失败：`找不到符号: @NotBlank`（import 删了但注解引用还在）。

### 正确修复流程

1. 先确认注解是否正确（枚举用 `@NotNull`，字符串用 `@NotBlank`）
2. 修改注解后，**单独检查 import 块**是否仍然完整
3. 禁止在同一个 Edit 中同时删除旧注解和导入新注解——分两步做

### 预防

- 删除注解前：`grep -r "@NotBlank\|@NotNull" src/main/java`
- 确认无其他引用后再删

---

## Lesson 30: Test 代码必须与 Model/API 同步更新（v1.5→v1.6 重构教训）

### 问题

v1.5→v1.6 重构后，`ReplenishmentDemandUseCaseTest` 仍用旧 API → 编译失败。

### 本次修复

| 旧 API（v1.5） | 新 API（v1.6） |
|---|---|
| `d.setQuantity(50)` | `d.setSubProductItemsRaw(json)` |
| `convertToProcurement(id, 1L)` | `convertToProcurement(id, ConvertDemandCmd)` |

### 预防

- `mvn test-compile` 必须在 PR 中通过
- CI 必须运行 `test-compile`，不仅 `compile`

---

## Lesson 31: JSON 存储列必须用 TEXT，不能用 VARCHAR

### 问题

`VARCHAR(2048)` 在 `utf8mb4` 下只能存 ~682 个中文字符，8 个子货号的 JSON 超限：

```
Data truncation: Data too long for column 'sub_product_code' at row 1
```

### 解决方案

```java
// ❌ VARCHAR(2048) — 字节限制，中文超限
@Column(name = "sub_product_code", length = 2048)

// ✅ TEXT — 无字符/字节限制
@Column(name = "sub_product_code", columnDefinition = "TEXT")
```

### 判定

| 字段类型 | 判定 |
|----------|------|
| 有明确上限的短字段 | `VARCHAR(n)` — 如 code、name |
| JSON / 自由文本 | `TEXT` — 无上限保证 |
| 大字段（文章/HTML） | `LONGTEXT` |

---

## Lesson 32: 重构实体移除字段后必须同步清理数据库旧列

### 问题

v1.6.0 删除 `quantity`/`destination` 字段，DB 中旧列仍存在且定义为 `INT NOT NULL`，INSERT 报错：

```
SQLException: Field 'quantity' doesn't have a default value
```

### 根因

Hibernate `ddl-auto: update` **只添加新列**，不删除已存在但实体中不再引用的列。

### 预防

| 重构场景 | 必做事项 |
|----------|---------|
| 新增字段 | DB migration + entity 同步 |
| 删除字段 | **DB migration 删除旧列 + entity 移除字段** |
| 重命名字段 | DB rename + entity 改名，禁止先删后加 |
| 改列类型 | DB ALTER + entity `@Column` 更新 |

> `ddl-auto: update` 不是银弹——它只处理**新增**，不处理**删除**

---

## Lesson 33: 锚点设计决定 Overview 可见性——Procurement 中心导致 Demand 新建后不可见

### 问题

`/base/overview` 只展示 Procurements。用户新建 Demand 后必须先转采购才能在 Overview 看到。

### 设计原则

> **业务链起点 = Overview 入口锚点。禁止以中间环节作为唯一入口锚点。**

### 修复方案

双入口架构：
```
/base/overview                          → 双 Tab（需求单 / 发注单）
/base/overview/demand/:demandId         → Demand 锚点
/base/overview/procurement/:procurementId → Procurement 锚点
```

---

## Lesson 34: 前端类型定义必须与后端 VO 同步——接口变更后旧字段残留导致 undefined

### 问题

后端 `DemandVO` 从 `quantity/destination` 改为 `subProductItemsSummary`，前端 `OrderOverviewPage.vue` Step1 卡片仍引用 `overview.demand.quantity` → 显示 `undefined`。

### 根因

前端 API 类型定义与后端 VO 不同步。

### 修复

变更接口时同步更新：
1. 后端修改 VO → 同步更新前端 API 类型定义
2. 前端模板字段引用 → 对应更新
3. i18n key → 同步调整

### 教训

> **接口变更 = 后端 VO + 前端类型 + 前端模板 + i18n 四处同步。缺一不可。**

---

## Lesson 38: 业务逻辑校验必须在入口处，零值/空值校验是防御性编程底线

### 问题

`ReplenishmentDemandUseCase.convertToProcurement()` 未校验 `quantity` 是否为 0 或 null，生成数量为 0 的 Procurement。

### 本次修复

```java
// ✅ 入口校验
if (item.getQuantity() == null || item.getQuantity() <= 0) {
    throw BusinessException.invalidParam(
            String.format("子货号 [%s] 数量无效（%s），无法转采购",
                    item.getSubCode(), item.getQuantity()));
}
```

### 防御性校验清单

| 场景 | 校验规则 |
|------|---------|
| 数量字段 | `> 0` |
| ID 字段 | `> 0`（不为 null） |
| 枚举字段 | `!= null` |
| 金额字段 | `>= 0` |

### 预防

- `@Valid` 注解只校验格式，不校验业务语义
- 校验异常统一抛出 `BusinessException.invalidParam()`

---

## 铁律总结表（后端）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 1 | 跨模块走 Port 接口，domain model 只用 common.enums/exception | Lombok 失效 |
| 2 | Domain model 禁止引用其他模块 Entity | 编译耦合 |
| 3 | JPA Repository 继承链 → @Qualifier | Spring 启动失败 |
| 4 | BusinessException API 添加前搜索全项目 | 编译失败 |
| 5 | Model 重构后测试必须同步 | 测试编译失败 |
| 6 | Repository 方法不依赖返回类型区分同名方法 | 编译失败 |
| 10 | Controller 返回类型必须属于自己模块 | 模块边界模糊 |
| 25 | 领域层 Repository 禁止加 @Repository | Spring Bean 歧义 |
| 29 | 删除旧注解时必须同步删除所有引用 | 编译失败 |
| 31 | JSON 存储列必须用 TEXT，不用 VARCHAR | 数据截断 |
| 32 | 实体删除字段后必须同步 DROP DB 列 | 插入失败 |
| 33 | 业务链起点 = Overview 入口锚点 | Demand 新建后不可见 |
| 34 | 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步 | undefined 显示 |
| 38 | 业务逻辑校验在入口处，零值/空值必须防御 | 脏数据 |
