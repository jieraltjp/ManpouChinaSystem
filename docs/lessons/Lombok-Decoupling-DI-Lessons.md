# Lombok + Cross-Module Compilation + Spring DI — Lessons Learned

> Date: 2026-04-23
> Project: manpou-allinone

---

## 目录

- [Lesson 1: Lombok 失效的真正根因是编译顺序](#lesson-1-lombok-失效的真正根因是编译顺序)
- [Lesson 2: 领域模型禁止直接引用其他模块的 Entity/VO](#lesson-2-领域模型禁止直接引用其他模块的-entityvo)
- [Lesson 3: JPA Repository 继承链会产生多个同名 Bean](#lesson-3-jpa-repository-继承链会产生多个同名-bean)
- [Lesson 4: BusinessException API 必须与调用方严格对齐](#lesson-4-businessexception-api-必须与调用方严格对齐)
- [Lesson 5: Test 代码必须与 Model API 同步](#lesson-5-test-代码必须与-model-api-同步)

---

## Lesson 1: Lombok 失效的真正根因是编译顺序

### 问题

`mvn clean compile` 大量报错：

```
找不到符号: 方法 getXxx()     // Lombok @Getter 未生成
找不到符号: 方法 builder()    // Lombok @Builder 未生成
找不到符号: 方法 conflict()   // Lombok @Slf4j 未生成
找不到符号: 变量 log          // Lombok @Slf4j 未生成
找不到符号: 符号 getKeyword() // @Data 未生成
```

### 根因

**不是 Lombok 配置错误，是 javac 编译顺序破坏。**

当模块 A 的 `application` 层直接引用模块 B 的 domain 类（如 `Product`、`QcRecord`）时：

```
product/application/usecase/ProductUseCase.java  ──imports──►  product/domain/model/Product.java
logistics/application/.../LogisticsPlanAssembler.java ──imports──►  factory/domain/model/Factory.java
procurement/application/.../ProcurementQcPassedEventListener.java ──imports──►  qc/domain/model/QcRecord.java
```

javac 按字母序/依赖顺序编译，product 模块被编译时，factory 模块的 Lombok 还未处理完，导致 product 模块看到的 factory 类是"半成品"——字段存在但无 getter/setter。

**Lombok 是字节码增强注解处理器**，在第一次扫描时不生成方法，在增量编译时更依赖类文件的完整状态。跨模块直接引用打破了这一前提。

### 解决方案：依赖倒置（Port 接口）

```
模块 A 的 application 层  ──依赖──►  common.port.XxxQueryPort  (接口)
                                          ▲
                                          │
                                          │ implements
                                          │
模块 B 的 infrastructure 层  ──依赖──►  B 的 domain 类
```

所有跨模块引用都通过 `common.port` 下的接口通信。高层模块不感知底层实现。

### 预防

- 任何跨模块的 `application → domain` 直接引用都要过 Port 接口
- domain model 只能引用 `common.enums` / `common.exception` 等纯数据类，绝不能引用其他模块的 entity
- 业务逻辑（尤其是事件监听器）若需要多个模块的数据，逻辑写在 application 层，domain entity 保持纯净

### 相关文件

- `docs/ARCHITECTURE-Lombok-Decoupling.md` — 解耦架构文档

---

## Lesson 2: 领域模型禁止直接引用其他模块的 Entity/VO

### 问题

`Procurement.suggestNextStatus(QcRecord, Product)` — 最初想在 `Procurement` entity 里调用其他模块的 entity，触发跨模块依赖。

### 根因

DDD 分层原则：领域模型是最内层，不能依赖外层（application、infrastructure）。Entity 直接 import 其他模块的 entity 意味着：

- 编译顺序耦合（Lesson 1 的根因）
- 违反单一职责（Entity 承担了"知道其他模块存在"的职责）
- 难以独立测试

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

```java
// ❌ 错误：domain model 引用其他模块 entity
public ShipmentStatus suggestNextStatus(QcRecord qcRecord, Product product) {
    // 违反：Procurement 知道 QcRecord 和 Product 的存在
}
```

### 预防

- `grep -r "import com.manpou.allinone\.[a-z]+\.domain\.model" src/main/java/com/manpou/allinone/X/` — 定期扫描 domain 层的不当 import
- domain model 只 import `common.enums`、`common.exception`、`jakarta.persistence`、其他 module 的 domain enums

---

## Lesson 3: JPA Repository 继承链会产生多个同名 Bean

### 问题

运行时启动失败：

```
Parameter 0 of constructor in ProductQueryPortImpl required a single bean,
but 2 were found:
  - productJpaRepository: defined in ProductJpaRepository
  - productRepository:    defined in ProductRepository

Action: Consider marking one of the beans as @Primary,
or using @Qualifier to identify the bean that should be consumed.
```

### 根因

```java
// ProductRepository.java — 领域层接口（契约）
public interface ProductRepository extends JpaRepository<Product, Long> { ... }

// ProductJpaRepository.java — 基础设施层 JPA 实现
@Repository
public interface ProductJpaRepository extends ProductRepository, JpaRepository<Product, Long> { }
```

Spring JPA 扫描时，把 `ProductRepository` 和 `ProductJpaRepository` 都注册为 bean（ID 分别为 `productRepository` 和 `productJpaRepository`）。`ProductQueryPortImpl` 注入 `ProductRepository`，Spring 找到两个候选，无法抉择。

### 解决方案

在 PortImpl 上用 `@Qualifier` 显式指定用哪个：

```java
@Component
public class ProductQueryPortImpl implements ProductQueryPort {
    private final ProductRepository productRepository;

    public ProductQueryPortImpl(
            @Qualifier("productJpaRepository") ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

### 预防

- 未来新建 PortImpl 时，如果涉及 JPA Repository，先确认该 Repository 是否存在继承链
- 如果 `XxxJpaRepository extends XxxRepository`，始终用 `@Qualifier("xxxJpaRepository")` 注入
- 最佳实践：`XxxRepository`（domain 接口）+ `XxxJpaAdapter`（infrastructure 实现），而不是让 JPA 接口直接继承 domain 接口

---

## Lesson 4: BusinessException API 必须与调用方严格对齐

### 问题

编译报错：

```
找不到符号: 方法 conflict(java.lang.String, java.lang.String)
  原因: 实际参数 (java.lang.String, java.lang.String)
  预期: 商务异常冲突
```

调用方：`ProductUseCase` 用 `BusinessException.conflict("product.duplicate", msg)`，但 `BusinessException.conflict(String message)` 只接受一个参数。

### 根因

历史 API 演化不一致：

```java
// BusinessException.java — 旧工厂方法（已存在）
public static BusinessException conflict(String message) {
    return new BusinessException("resource.conflict", message);
}

// 某次新增：双参数版本（从未被调用）
public static BusinessException conflict(String code, String message) {
    return new BusinessException(code, message);
}

// 调用方（错误）
throw BusinessException.conflict("product.duplicate", msg);  // 实际调用的是单参数版本，参数名误用
```

### 解决方案

保留两个入口，职责明确：

- 构造函数 `new BusinessException(code, message)` — 直接构造，code 语义完整
- 工厂方法 `BusinessException.conflict(message)` — 自动填 code，前端友好
- 业务代码直接 `new BusinessException("domain.error-code", "消息")` 是合理的，不强制走工厂方法

```java
public BusinessException(String code, String message) { ... }
public static BusinessException conflict(String message) { ... }  // 保留
// 删除了从未被调用的 conflict(String, String) 工厂方法
```

### 预防

- 添加新工厂方法前，用 `grep -r "BusinessException\." src/` 确认是否已存在类似工厂
- 统一规范：所有业务异常走 `BusinessException` 构造函数或工厂方法，禁止裸 `throw new RuntimeException()`
- 接口文档与实现严格对齐

---

## Lesson 5: Test 代码必须与 Model API 同步

### 问题

Test 编译报错：

```
找不到符号: 方法 setLocation(java.lang.String)
  位置: 类型 com.manpou.allinone.factory.domain.model.Factory 的变量 factory

找不到符号: 方法 findByIdAndIsDeletedFalse(java.lang.Long)
  位置: 类型 ...Repository 的变量 ...
```

### 根因

Model 重构后（`Factory` 从 `location`/`status` 改为 `province`/`city`/`cooperationStatus`），测试代码未同步更新，仍用旧 API。

### 解决方案

| 测试错误 | 修复 |
|---------|------|
| `factory.setLocation()` | → `factory.setProvince()` + `factory.setCity()` |
| `factory.setStatus(FactoryStatus.ACTIVE)` | → `factory.setCooperationStatus(CooperationStatus.ACTIVE)` |
| `FactoryStatus.INACTIVE` | → `CooperationStatus.SUSPENDED` |
| `findByIdAndIsDeletedFalse()` 拼写错误 | → `findByIdAndDeletedIsFalse()` |

### 预防

- Model 重构后立即搜索所有 test 文件：`grep -r "setLocation\|setStatus\|findByIdAndIsDeletedFalse" src/test/`
- CI 中 `mvn test-compile` 必须通过，未同步测试 = 技术债务
- 重构规范：改 Model 字段 → 改测试 → 改业务代码（测试先行）

---

## 总结：五条铁律

| # | 铁律 | 违反后果 |
|---|------|---------|
| 1 | 跨模块依赖走 Port 接口，domain model 只用 common.enums/exception | Lombok 失效，编译顺序破坏 |
| 2 | 领域模型禁止 import 其他模块的 entity/VO | 编译耦合，业务逻辑膨胀在 domain |
| 3 | JPA Repository 继承链 → 用 @Qualifier 显式指定 bean | Spring 启动失败 |
| 4 | BusinessException API 添加前必须搜索全项目确认不冲突 | 编译失败，API 歧义 |
| 5 | Model 重构后测试必须同步 | 测试编译失败，技术债累积 |
