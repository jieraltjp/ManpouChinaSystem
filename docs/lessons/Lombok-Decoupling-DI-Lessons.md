# Lombok + Cross-Module Compilation + Spring DI — Lessons Learned

> Date: 2026-04-24
> Project: manpou-allinone

---

## 目录

- [Lesson 1: Lombok 失效的真正根因是编译顺序](#lesson-1-lombok-失效的真正根因是编译顺序)
- [Lesson 2: 领域模型禁止直接引用其他模块的 Entity/VO](#lesson-2-领域模型禁止直接引用其他模块的-entityvo)
- [Lesson 3: JPA Repository 继承链会产生多个同名 Bean](#lesson-3-jpa-repository-继承链会产生多个同名-bean)
- [Lesson 4: BusinessException API 必须与调用方严格对齐](#lesson-4-businessexception-api-必须与调用方严格对齐)
- [Lesson 5: Test 代码必须与 Model API 同步](#lesson-5-test-代码必须与-model-api-同步)
- [Lesson 26: Maven `-q` 静默模式掩盖 repackage 失败](#lesson-26-maven--q-静默模式会吞掉-spring-bootrepackage-失败导致-jar-无法启动)
- [Lesson 27: 依赖 scope 必须与实际运行环境匹配](#lesson-27-依赖-scope-必须与实际运行环境匹配h2-test-scope-vs-runtime)
- [Lesson 28: 编译与启动必须分离，禁止在脚本中隐蔽失败](#lesson-28-编译与启动必须分离禁止在脚本中隐蔽失败)
- [Lesson 29: 删除旧注解时必须同步删除所有引用，禁止只删 import](#lesson-29-删除旧注解时必须同步删除所有引用禁止只删-import)
- [Lesson 30: Test 代码必须与 Model/API 同步更新（v1.5→v1.6 重构教训）](#lesson-30-test-代码必须与-modelapi-同步更新v15v16-重构教训)
- [Lesson 31: JSON 存储列必须用 TEXT，不能用 VARCHAR](#lesson-31-json-存储列必须用-text不能用-varchar)
- [Lesson 32: 重构实体移除字段后必须同步清理数据库旧列](#lesson-32-重构实体移除字段后必须同步清理数据库旧列)
- [Lesson 33: 锚点设计决定 Overview 可见性——Procurement 中心导致 Demand 新建后不可见](#lesson-33-锚点设计决定-overview-可见性procurement-中心导致-demand-新建后不可见)
- [Lesson 34: 前端类型定义必须与后端 VO 同步——接口变更后旧字段残留导致-undefined](#lesson-34-前端类型定义必须与后端-vo-同步接口变更后旧字段残留导致-undefined)
- [Lesson 40: TypeScript strict 编译必须通过——TS6133/TS2345 是代码腐烂信号](#lesson-40-typescript-strict-编译必须通过ts6133ts2345-是代码腐烂信号)
- [Lesson 41: 前端 API 签名变更后所有调用方必须同步——v1.6 破坏性变更教训](#lesson-41-前端-api-签名变更后所有调用方必须同步——v16-破坏性变更教训)
- [Lesson 42: Vue template v-for 的 index 参数未使用时必须加 `_` 前缀](#lesson-42-vue-template-v-for-的-index-参数未使用时必须加--前缀)
- [Lesson 43: 前端组件 Props 必须与所有调用方对齐——optional 字段不得隐式 required](#lesson-43-前端组件-props-必须与所有调用方对齐——optional-字段不得隐式-required)
- [Lesson 44: 前端对话框表格列标签必须提取为 i18n key——禁止硬编码](#lesson-44-前端对话框表格列标签必须提取为-i18n-key——禁止硬编码)
- [Lesson 45: Flyway 迁移文件版本号不得重复——冲突时立即修正](#lesson-45-flyway-迁移文件版本号不得重复——冲突时立即修正)

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
| 1 | 跨模块走 Port 接口，domain model 只用 common.enums/exception | Lombok 失效，编译顺序破坏 |
| 2 | Domain model 禁止引用其他模块 Entity | 编译耦合，业务逻辑膨胀在 domain |
| 3 | JPA Repository 继承链 → @Qualifier 显式指定 bean | Spring 启动失败 |
| 4 | BusinessException API 添加前搜索全项目确认不冲突 | 编译失败，API 歧义 |
| 5 | Model 重构后测试必须同步 | 测试编译失败，技术债累积 |

---

## Lesson 6: JPA Repository 方法名不一致导致编译失败

### 问题

```
error: conflicting: ProductRepository 中的抽象方法 findByMasterCodeAndDeletedIsFalse(String)
error: conflicting: ProductJpaRepository 中的抽象方法 findByMasterCodeAndDeletedIsFalse(String)
```

### 根因

Java 不允许同名方法不同返回类型共存于同一接口。

### 解决方案

重命名，不依赖返回类型区分：

```java
// ProductRepository — 保留 Optional 版本
Optional<Product> findByMasterCodeAndDeletedIsFalse(String masterCode);
// 另起不同名
List<Product> findAllByMasterCodeAndDeletedIsFalse(String masterCode);

// ProductJpaRepository — 移除重复声明（父接口已有）
```

### 预防

- Repository 方法命名不依赖返回类型区分同名方法
- 子接口不重复声明父接口已有方法（除非有 `@Query` 自定义实现）

---

## Lesson 7: Windows 环境脚本必须用 Git Bash 执行

### 问题

脚本在 CMD/PowerShell 报错，不识别 `./` 和 `#!/usr/bin/env bash` 语法。

### 解决方案

始终使用 Git Bash：

```bash
# ✅ 正确
./scripts/start-all.sh

# ❌ 错误：直接在 CMD 中运行 .sh
```

---

## Lesson 8: Flyway 禁用后 V4 迁移数据不会自动导入

### 问题

500 条工厂数据写在 `V4__factory_migration.sql` 中，但 Flyway 全局禁用，数据从不导入。

### 根因

```
# application.yml
flyway.enabled: false
```

Hibernate `ddl-auto: update` 只管表结构，不管数据。Flyway 禁用 = 数据迁移不执行。

### 预防

- Flyway 数据迁移（INSERT）与表结构管理（CREATE/ALTER）是两回事
- 禁用 Flyway 时，数据初始化必须走 DevTestDataInitializer
- 确认哪些是"环境基础设施数据"（Initializer），哪些是"测试数据"

---

## Lesson 9: JWT 私钥路径必须在 classpath 和文件系统双重保险

### 问题

启动报错：`RSA 私钥未找到`

### 根因

`JwtKeyManager` 只查单一路径，部署环境路径不一致。

### 解决方案

优先级：classpath > 文件系统：

```java
// 1. 先查 classpath（随 jar 打包，部署一致性高）
// 2. 再查文件系统（本地开发 / 自定义路径）
```

### 预防

- 密钥/证书类资源始终支持双路径
- 生产部署时 classpath 优先，减少环境差异

---

## Lesson 10: DTO 与 Entity 混用导致 Order 模块依赖 Procurement

### 问题

`OrderOverviewController` 返回 `Page<ProcurementPageQuery>`，Order 直接引用了 Procurement 的 DTO。

### 根因

API 返回类型用了对方模块的 DTO，模块边界模糊。

### 解决方案

每个模块定义自己的 API DTO，通过 Assembler 转换：

```java
// Order 模块自己定义
public class OrderProcurementSelectorDTO { ... }

// ProcurementAssembler 提供转换方法
public OrderProcurementSelectorDTO toOrderProcurementSelectorDto(Procurement entity) { ... }

// Controller 使用自己模块的 DTO
public Result<Page<OrderProcurementSelectorDTO>> selector(...) { ... }
```

### 预防

- Controller 的请求/响应类型必须属于自己的模块
- 定期审计 Controller 返回类型的包路径，发现跨模块引用立即拆分

---

## 总结：十条铁律

| # | 铁律 | 违反后果 |
|---|------|---------|
| 1 | 跨模块走 Port 接口，domain model 只用 common.enums/exception | Lombok 失效 |
| 2 | Domain model 禁止引用其他模块 Entity | 编译耦合 |
| 3 | JPA Repository 继承链 → @Qualifier 显式指定 bean | Spring 启动失败 |
| 4 | BusinessException API 添加前搜索全项目 | 编译失败 |
| 5 | Model 重构后测试必须同步 | 测试编译失败 |
| 6 | Repository 方法不依赖返回类型区分同名方法 | 编译失败 |
| 7 | Windows 用 Git Bash 执行 .sh 脚本 | 脚本报错 |
| 8 | Flyway 禁用时数据初始化走 DevTestDataInitializer | 数据不导入 |
| 9 | 密钥/证书资源走 classpath + 文件系统双路径 | 启动失败 |
| 10 | Controller 返回类型必须属于自己模块 | 模块边界模糊 |

---

## Lesson 11: 文档与代码同步必须持续进行，不能积累

### 问题

从 git log 看到大量重复的"审计一致性修复"提交：

```
docs: fix: 审计一致性修复 — 补全缺失字段 + 更新实现状态 + 修复过期路由引用
docs: fix: 全量文档审计 — 补全缺失字段 + 更新实现状态 + 修复过期路由引用
docs: fix: 前后端 SPEC + UI 页面文档 + 前端页面三方对齐
docs: 审计一致性修复 — entity注解/文档DTO名/重复行/字段对齐
docs: 审计一致性修复 — demand.ts类型 + SPEC-B10/DB-11状态对齐
docs: fix: 全面审计一致性修复 — 文档路径/状态/DTO/分页
```

历史审计轮次：Round 1 → Round 2 → Round 3 → Round 4，每轮都修上轮的残留问题。

### 根因

- 文档更新和代码提交不同步
- 审计只在文档写完后进行，不是持续过程
- 命名不一致（ShippingOrder vs Procurement、LogisticsPlanType vs PlanType）在开发中期才被发现

### 解决方案

**"文档即代码"规范：**

| 场景 | 规范 |
|------|------|
| 新增 API | 文档和代码同 commit |
| 重构字段 | 文档和代码同 commit，或文档先于代码 |
| 新增枚举 | 文档和代码同 commit，SPEC 枚举表同步更新 |
| 每完成一个 UseCase | 立即更新 docs/business 对应 SPEC 状态 ✅/🔴 |

**预防：**

- CI 中可加入文档校验（docstring 与 Javadoc 一致性检查）
- PR 必须包含文档更新，无文档 = 无 PR
- 定期小规模审计（每周），不要积累成大规模重构

---

## Lesson 12: i18n 必须从第一天规划，不能后期打补丁

### 问题

从 git log 看到 i18n 修复了 4 轮：

```
fix: i18n 审计 round 1 — DashboardPage 全量 i18n化
fix: i18n 审计 round 2 — zh.json 重复key + FactoryPage 全量 i18n化
fix: i18n 审计 round 3 — LogisticsPage/InspectionPage 单位符号 i18n化
fix: i18n 审计 round 4 — client.ts 错误消息 + AppLayout 语言选择器
fix: i18n 审计 round 5 — zh.json 重复key + FactoryPage 全量 i18n化
```

### 根因

- i18n 没有纳入前端骨架设计
- 硬编码中文字符串散落在 Vue 组件中，遗漏率高
- 没有 i18n linter 或 CI 检查

### 解决方案

- **第一天**：选定 i18n 方案（vue-i18n），配置 `zh.json` / `en.json`，在 `vite.config.ts` 中配置 plugin
- **组件规范**：所有用户可见文本必须是 `{{ $t('key') }}`，禁止硬编码
- **CI 检查**：`vue-i18n-extract` 或 ESLint rule 检查未翻译 key
- **占位符原则**：先写英文 key 值（如 `order.status.pending`），后配翻译

### 预防

- 前端骨架生成时，vite 模板必须包含 vue-i18n 插件
- `npm run lint` 包含 i18n 检查

---

## Lesson 13: Flyway 迁移版本号必须提前规划，避免重编号

### 问题

```
fix: Flyway 迁移文件 V3→V8，修复 V7→V8 MySQL 语法
fix: DB文档版本号同步 — DB-01 V6→V7, DB-12 V5→V6（配合Flyway迁移重编号）
```

迁移脚本版本号被改过多次，每次重编号都有风险（checksum 不一致）。

### 根因

- 没有预先规划 Flyway 版本号空间
- 迁移脚本在开发过程中随意插入，导致编号断层
- Flyway 禁用时误以为不需要规划

### 解决方案

**Flyway 版本号规划：**

```
V1__init_schema.sql              — 建表骨架（所有模块基础表）
V2__outbox_table.sql             — 事件表
V3__signing_key_table.sql        — JWT 密钥表
V10__factory_seed.sql             — 基础数据（V10起留足空间插队）
V11__product_category_seed.sql    — 商品分类
V20__shipment_status_enum.sql    — 大版本功能
```

**原则：**

- 基础设施表从 V1-V9
- 每个业务模块基础数据从 V10, V20, V30, ... 起跳（留 10 个版本空间）
- 紧急修复插队用 V10_1__fix_xxx.sql 或 V10_hotfix__xxx.sql

### 预防

- 在 `docs/database/README.md` 中维护 Flyway 版本路线图
- Flyway 启用后，修改已执行脚本前先 `flyway:repair`

---

## Lesson 14: 命名一致性必须在开发前锁定，禁止中途改名

### 问题

历史命名不一致导致的返工：

| 旧命名（错误） | 新命名（正确） | 发现时机 |
|-------------|-------------|---------|
| `ShippingOrder` | `Procurement` | 开发中期，文档审计发现 |
| `LogisticsPlanType` | `PlanType` | 开发中期 |
| `FactoryStatus` | `CooperationStatus` | 重构时 |
| `/api/v1/logistics` | `/api/v1/logistics-plans` | 第三轮审计 |
| `Vite proxy → 192.168.12.198:18090` | `Vite proxy → localhost:18080` | 实际部署时 |

### 根因

- 骨架代码中默认命名没有经过评审
- API 命名没有遵循 RESTful 规范
- 没有 API 命名规范文档

### 解决方案

**命名锁定流程：**

```
1. 写 docs/business/SPEC-*.md 时，同时定义：
   - Domain 实体名（如 Procurement 而非 ShippingOrder）
   - API 路径（如 /logistics-plans 而非 /logistics）
   - 枚举值（如 CooperationStatus 而非 FactoryStatus）

2. codegen 生成骨架后，先审命名再开发

3. 枚举改名：
   - 旧值保留（数据库兼容）
   - 新值作为替代
   - Migration: UPDATE SET status = 'SUSPENDED' WHERE status = 'INACTIVE'
```

### 预防

- 每个模块开发前，先过"命名评审"
- REST API 命名规范：资源用复数名词，嵌套路径不超过 2 层

---

## Lesson 15: 单体优先，但提前规划好未来拆分的边界

### 策略（已验证正确）

```
Phase 0: manpou-allinone（7→8领域合一，端口18090）
         └── 快速验证业务，不引入分布式复杂度

Phase B: 按 Kafka Topic 边界逐步拆分
         procurement → warehouse-service
         logistics → logistics-service
         customs → customs-service
```

### 教训

如果当初在 allinone 里把领域混在一起（无边界），后续拆分会非常痛苦。正确的做法是：

- **现在**：模块内部分层清晰（domain/application/interfaces），但部署在同一 JAR
- **未来**：每个模块抽取为独立服务，只需改 POM 依赖 + 引入 Kafka
- **禁止**：在单体阶段使用"临时全局变量"或跨模块直接调用而不走 Port

---

## Lesson 16: 前端 API 客户端类型必须与后端 DTO 严格对齐

### 问题

历史前端字段与后端不一致：

```
TestPage.vue 字段（orderNo/priority 等）← 无后端对应
DemandPage.vue 的 /replenishment-demands ← 旧 endpoint
```

### 根因

- 前端从设计稿/文档生成，后端从 JPA 实体生成，两边独立演进
- 没有类型共享机制（后端 DTO 未生成前端 TypeScript 类型）

### 解决方案

**OpenAPI 契约优先：**

```yaml
# docs/api/SPEC-B02-procurement.yml
openapi: 3.0.0
paths:
  /api/v1/procurements:
    post:
      requestBody:
        $ref: './components/schemas/ProcurementCreate.yaml'
      responseBody:
        $ref: './components/schemas/ProcurementPageQuery.yaml'
```

- 后端实现严格按 OpenAPI schema
- 前端从 OpenAPI 生成 TypeScript 类型（`openapi-typescript-codegen`）
- 双方各自生成，互不依赖源码

### 预防

- 后端 DTO 变更 → 必须更新 OpenAPI schema → 触发前端类型重新生成
- 没有 OpenAPI schema 变更 = 不允许合入

---

## Lesson 17: 环境差异（dev/staging/prod）配置必须标准化

### 问题

```
# 旧配置（混乱）
vite.config.ts proxy → 192.168.12.198:18090  # VPN 地址，本地无法访问

# 新配置（正确）
vite.config.ts proxy → localhost:18080  # API Gateway
```

### 根因

- 开发环境用了生产 VPN 内网 IP，本地无法访问
- 没有本地开发等效配置

### 解决方案

```
开发环境（localhost）：
  前端 → localhost:18080 (Gateway) → localhost:18090 (manpou-allinone)

环境配置优先级：
  1. .env.local（本地覆盖，最高优先级）
  2. .env.development（开发默认值）
  3. vite.config.ts proxy 配置（仅 dev）
  4. .env.production（生产默认值）
```

---

## Lesson 25: JPA Domain Repository 禁止加 @Repository，避免与 JpaAdapter 重复 Bean

### 问题

Spring 启动失败：
```
Error creating bean with name 'productUseCase' ...
Unsatisfied dependency expressed through constructor parameter 0:
No qualifying bean of type 'ProductRepository' available:
expected single matching bean but found 2: productRepository,productJpaRepository
```

### 根因

```java
// ProductRepository — 领域层接口，加了 @Repository
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> { ... }

// ProductJpaRepository — 基础设施层 JPA 实现，也 extends ProductRepository
@Repository
public interface ProductJpaRepository extends ProductRepository, JpaRepository<Product, Long> { }
```

Spring 扫描时，`ProductRepository`（领域接口，带 `@Repository`）和 `ProductJpaRepository`（继承领域接口）都被注册为 bean，bean ID 分别为 `productRepository` 和 `productJpaRepository`。

### 解决方案

**领域层接口不加 `@Repository`，只由 JPA 适配器注册：**

```java
// ✅ 正确：领域接口无 @Repository
public interface ProductRepository extends JpaRepository<Product, Long> { ... }

// ✅ 正确：JPA 适配器带 @Repository
@Repository
public interface ProductJpaRepository extends ProductRepository, JpaRepository<Product, Long> { }
```

### 预防

- 所有同时有 `XxxRepository`（领域接口）和 `XxxJpaAdapter`/`XxxJpaRepository`（基础设施实现）的模块，领域接口**禁止加 `@Repository`**
- Spring Boot 的 `@EnableJpaRepositories` 会自动扫描所有 `@Repository` 的接口，无论包路径
- 最佳实践：领域接口和 JPA 适配器放不同子包，用 `@EnableJpaRepositories(basePackages = ...)` 精确控制扫描范围

---

## Lesson 26: Maven `-q` 静默模式会吞掉 spring-boot:repackage 失败，导致 JAR 无法启动

### 问题

后端 `mvn package -DskipTests -q` 报告 BUILD SUCCESS，但 `java -jar target/xxx.jar` 启动失败：

```
Error: Unable to access jarfile target/manpou-allinone-1.0.0-SNAPSHOT.jar
# 或启动后立即退出，无日志
```

检查 MANIFEST.MF 发现缺失：
```
Main-Class: org.springframework.boot.loader.launch.JarLauncher   ← 缺失
Start-Class: com.manpou.allinone.ManpouAllInOneApplication       ← 缺失
```

### 根因

**两条错误叠加：**

1. **JAR 文件被进程锁住**
   前一个 Spring Boot 进程（`java -jar` 启动的）仍持有 `manpou-allinone-1.0.0-SNAPSHOT.jar` 的文件锁。`mvn package` 的 `spring-boot:repackage` 尝试覆盖该文件时失败（Windows 文件锁），但使用了 `-q`（quiet）标志，**所有输出被吞掉**。

2. **Maven 静默模式掩盖了 repackage 失败**
   `-q` 标志让 Maven 不输出任何 INFO/WARNING，repackage 失败时只返回非零退出码（脚本未检查），最终产物是没有 Spring Boot Loader 的普通 JAR。

```
mvn package -DskipTests -q
  → spring-boot:repackage 失败（文件锁）
  → 错误被 -q 吞掉
  → BUILD SUCCESS（因为 compile 成功了）
  → 产物：无 Main-Class 的普通 JAR → java -jar 失败
```

### 诊断方法

检查 JAR 是否可执行：
```bash
# 方法1：检查 MANIFEST.MF
unzip -p target/manpou-allinone-1.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF | grep "Main-Class"

# 方法2：检查 Spring Boot 特有的文件
unzip -l target/xxx.jar | grep "BOOT-INF"

# 方法3：不用 -q，跑完整日志
mvn package -DskipTests 2>&1 | grep -E "repackage|ERROR|SUCCESS"
```

### 解决方案

**立即修复（本次）：**
```bash
# 1. 杀掉持有 JAR 锁的 Java 进程
taskkill //F //IM java.exe

# 2. 不带 -q 重新打包，让错误可见
mvn package -DskipTests
```

**预防：**

| 规范 | 说明 |
|------|------|
| CI/CD 和本地打包**禁止**使用 `-q` | 掩盖 WARNING 和错误 |
| 打包后立即检查 MANIFEST.MF | `unzip -p target/xxx.jar META-INF/MANIFEST.MF \| grep Main-Class` |
| 打包前确保无旧进程锁 JAR | `taskkill //F //IM java.exe` 或等效命令 |
| 构建脚本检查 exit code | `mvn package ... && echo "OK" \|\| echo "FAILED"` |

### 相关文件

- `apps/manpou-allinone/pom.xml` — spring-boot-maven-plugin 配置
- `apps/manpou-allinone/target/manpou-allinone-1.0.0-SNAPSHOT.jar`

---

## 总结：二十七条铁律（→ 已有 44 条，详见文末）

> 以下为历史版本（Lesson 1-17），当前最新版本（含 Lesson 25-44）见文末「总结：二十七条铁律（最终版）」。

### 工程实践（代码层）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 1 | 跨模块走 Port 接口，domain model 只用 common.enums/exception | Lombok 失效 |
| 2 | Domain model 禁止引用其他模块 Entity | 编译耦合 |
| 3 | JPA Repository 继承链 → @Qualifier 显式指定 bean | Spring 启动失败 |
| 4 | BusinessException API 添加前搜索全项目 | 编译失败 |
| 5 | Model 重构后测试必须同步 | 测试编译失败 |
| 6 | Repository 方法不依赖返回类型区分同名方法 | 编译失败 |
| 22 | JPA 持久化字段必须显式标注 @Column | 列映射错误 |
| 23 | 枚举注释必须与业务语义严格对齐 | 理解歧义 |
| 29 | 删除旧注解时必须同步删除所有引用 | 编译失败 |
| 31 | JSON 存储列必须用 TEXT，不用 VARCHAR | 数据截断 |
| 32 | 实体删除字段后必须同步 DROP DB 列 | 插入失败 |

### 工程实践（环境/部署）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 7 | Windows 用 Git Bash 执行 .sh 脚本 | 脚本报错 |
| 8 | Flyway 禁用时数据初始化走 DevTestDataInitializer | 数据不导入 |
| 9 | 密钥/证书资源走 classpath + 文件系统双路径 | 启动失败 |
| 17 | 开发环境配置走 .env.local/proxy，本地必须可运行 | 环境差异 |
| 18 | private.pem 仅存于签发中心服务，禁止全量分发 | 安全漏洞 |
| 20 | 分页约定 page=0 vs page=1 必须在开发前锁定 | 前后端不对齐 |
| 26 | 打包禁止使用 `-q` + 确保无旧进程锁 JAR | repackage 失败被吞掉 |
| 27 | 运行时依赖不能是 test scope | `Cannot load driver class` |
| 28 | 编译与启动必须分离，错误必须可见 | 错误被掩盖 |

### 架构/模块化

| # | 铁律 | 违反后果 |
|---|------|---------|
| 10 | Controller 返回类型必须属于自己模块 | 模块边界模糊 |
| 15 | 单体内部分层清晰，提前规划拆分边界 | 未来拆分成本高 |
| 19 | 共享代码抽取到 common 模块 | 代码冗余 |

### 文档/命名

| # | 铁律 | 违反后果 |
|---|------|---------|
| 11 | 文档和代码同 commit，持续审计不积累 | 多轮返工 |
| 12 | i18n 从第一天规划，禁止后期打补丁 | 4轮返工 |
| 13 | Flyway 版本号提前规划，禁止重编号 | checksum 不一致 |
| 14 | 命名在开发前锁定，禁止中途改名 | 大量迁移返工 |

### 前端/契约/测试

| # | 铁律 | 违反后果 |
|---|------|---------|
| 16 | 前端类型从 OpenAPI schema 生成，禁止手动对齐 | 字段不匹配 |
| 21 | BaseEntity 用 @MappedSuperclass，所有实体继承 | 审计字段不一致 |
| 24 | 测试数据提取禁止用字符串解析（grep） | 测试脆弱 |
| 25 | 领域层 Repository 禁止加 @Repository | Spring Bean 歧义 |
| 30 | Test 代码必须与 Model/API 同步更新 | 编译失败 |

---

## Lesson 27: 依赖 scope 必须与实际运行环境匹配（H2 test scope vs runtime）

### 问题

JAR 打包成功，`java -jar` 启动时报错：
```
Caused by: java.lang.IllegalStateException: Cannot load driver class: org.h2.Driver
```

### 根因

`pom.xml` 中 H2 为 `test` scope，但 `application.yml` 默认 datasource 使用 H2：
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>   ← 不打包到 JAR
</dependency>

<!-- application.yml -->
driver-class-name: org.h2.Driver  ← 运行时需要，找不到
```

`spring-boot:repackage` 只打包 `compile` + `runtime` scope，不打包 test scope。

### 解决方案

**移除 H2，激活 MySQL local profile：**
```yaml
# application.yml
spring:
  profiles:
    active: local   # ← 激活 application-local.yml（MySQL 配置）

# application-local.yml（已在 .gitignore）
# 包含完整的 MySQL datasource + JPA 配置
```

同时从 `pom.xml` 中删除 H2 依赖。

### 预防

| 规范 | 说明 |
|------|------|
| 运行时依赖不能用 `test` scope | test scope 不参与 JAR 打包 |
| datasource profile 与实际环境匹配 | 本地开发用 MySQL → 激活 local profile |
| profile 合并后 YAML 不能有重复 key | `spring:` 只能出现一次，合并为单一块 |

---

---

## Lesson 28: 编译与启动必须分离，禁止在脚本中隐蔽失败

### 问题

`mvn clean package` 编译失败（`isScalar()` 方法找不到），但未被发现，JAR 打包表面成功，实际不可用。

### 根因

两条错误叠加：
1. Spring Cloud Alibaba Nacos 带来了旧版 Jackson，与 `manpou-common` 中声明的 Jackson 2.17.0 不兼容
2. `manpou-common` 源码调用了 Jackson 2.10+ 才有的 `JsonNode.isScalar()` 方法，编译时 classpath 有旧版 Jackson，`isScalar()` 不可见

### 本次修复

```java
// libs/manpou-common/.../NacosConfigSource.java line 153
// ❌ 旧代码（Jackson 2.10+，classpath 有旧版时报错）
return current.isScalar() ? current.asText() : current.toString();

// ✅ 新代码（兼容所有 Jackson 版本）
return !current.isContainerNode() ? current.asText() : current.toString();
```

### 正确启动流程

```bash
# 1. 确保无旧进程锁 JAR
powershell -Command "Stop-Process -Name java -Force -ErrorAction SilentlyContinue"

# 2. 全量编译（禁止 -q，让错误可见）
cd apps/manpou-allinone
mvn clean package -DskipTests

# 3. 检查 MANIFEST.MF
unzip -p target/manpou-allinone-*.jar META-INF/MANIFEST.MF | grep "Main-Class"
# 预期输出: Main-Class: org.springframework.boot.loader.launch.JarLauncher

# 4. 启动后端
java -Xms512m -Xmx1024m \
  -jar apps/manpou-allinone/target/manpou-allinone-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=local \
  --server.port=18090

# 5. 启动前端
cd apps/web && npm run dev
```

或使用脚本（Git Bash）：

```bash
# 仅启动后端
./scripts/start-all.sh manpou

# 启动全部
./scripts/start-all.sh

# 查看状态
./scripts/start-all.sh status
```

### 端口约定

| 服务 | 端口 |
|------|------|
| manpou-allinone | 18090 |
| user-service | 18081 |
| web 前端 | 13000 |
| Gateway（生产） | 18080 |

### 预防

- 编译错误必须在本对话内修复，不遗留到打包
- 禁止静默编译：`mvn package` 不带任何 `-q` / `--quiet`
- 打包后立即检查 MANIFEST.MF
- 脚本中的 `mvn ... -q` 必须替换为 `mvn ... 2>&1 | tee build.log`

---

*来源：git log（2026-04-10 ~ 04-23，104 commits）· docs/check/99-全面审计报告.md · docs/check/101~105 · docs/role/02-架构师视角.md · docs/pro/17-服务间认证.md · 2026-04-23 全量解耦重构会话 · 2026-04-23 运行时 Bean 歧义修复会话 · 2026-04-24 Jackson isScalar() 编译失败修复*

---

## Lesson 29: 删除旧注解时必须同步删除所有引用，禁止只删 import

### 问题

`ReplenishmentDemandCreateCmd.java` 中的 `@NotBlank` 注解已改为 `@NotNull`（因 `DemandType` 是枚举），但 Edit 工具误删了 `NotBlank` 的 import，导致编译失败：

```java
// ❌ 错误：使用了 @NotBlank 但没有 import
@NotBlank(message = "主货号不能为空")  // ← 编译失败，找不到符号
private DemandType demandType;

// 实际意图：DemandType 是枚举，@NotBlank 不适用，应改为 @NotNull
// 但删注解时遗漏了 import
```

### 根因

编辑时同时修改了注解和 import，但未确认两者是否匹配：
- `@NotBlank` → `@NotNull` 的修改本身正确（`DemandType` 是枚举）
- 但删除旧注解时，import 被同步删除
- 新注解 `@NotNull` 保留了 import

### 正确修复流程

1. 先确认注解是否正确（枚举用 `@NotNull`，字符串用 `@NotBlank`）
2. 修改注解后，单独检查 import 块是否仍然完整
3. 禁止在同一个 Edit 中同时删除旧注解和导入新注解——分两步做

```java
// 第1步：修改注解（不碰 import）
- @NotBlank(message = "需求类型不能为空")
+ @NotNull(message = "需求类型不能为空")

// 第2步：单独检查 import
// import jakarta.validation.constraints.NotBlank;  ← 保留（若其他字段还用）
// import jakarta.validation.constraints.NotNull;  ← 添加
```

### 预防

- 搜索所有使用该注解的文件：`grep -r "@NotBlank\|@NotNull" src/main/java`
- 删除注解前：`grep` 确认无其他引用

---

## Lesson 30: Test 代码必须与 Model/API 同步更新（v1.5→v1.6 重构教训）

### 问题

v1.5→v1.6 重构后，`ReplenishmentDemandUseCaseTest` 仍用旧 API：

```java
// 旧 API（v1.5）
d.setQuantity(50);
d.setDestination("东京");
demandUseCase.convertToProcurement(savedDemand.getId(), 1L);

// 新 API（v1.6）
// ReplenishmentDemand 不再有 quantity/destination → 用 subProductItemsRaw (JSON)
// convertToProcurement 接受 ConvertDemandCmd 而非 long
```

→ 编译失败，报 `cannot find symbol: method setQuantity(int)`

### 根因

重构时只更新了主代码，未同步更新测试代码。Test 编译未参与 CI 检查时会被漏掉。

### 本次修复

| 旧 API（v1.5） | 新 API（v1.6） |
|---|---|
| `d.setQuantity(50)` | `d.setSubProductItemsRaw(json)` |
| `d.setDestination("东京")` | 同上，用 JSON |
| `convertToProcurement(id, 1L)` | `convertToProcurement(id, ConvertDemandCmd)` |
| `getLinkedProcurementId()` | `getLinkedDemandItemsRaw()` |
| `ReplenishmentDemandUpdateCmd.setQuantity()` | 已移除该字段 |
| `CooperationStatus.ACTIVE` | `FactoryStatus.ACTIVE` → `CooperationStatus.ACTIVE` |

### 预防

- Model 重构后立即更新测试（Lesson 5 的延伸）
- `mvn test-compile` 必须在 PR 中通过
- CI 必须运行 `test-compile`，不仅 `compile`

---

## Lesson 31: JSON 存储列必须用 TEXT，不能用 VARCHAR

### 问题

`ReplenishmentDemand.subProductItemsRaw` 字段（存储 JSON 数组）定义为 `VARCHAR(2048)`：

```java
@Column(name = "sub_product_code", length = 2048)
private String subProductItemsRaw;
```

提交 8 个子货号明细时报错：

```
Data truncation: Data too long for column 'sub_product_code' at row 1
```

### 根因

JPA `length = 2048` 映射为 `VARCHAR(2048)`，限制的是**字节数**，而非字符数。

MySQL `utf8mb4` 编码下，中文字符每个占 3 字节：2048 / 3 ≈ 682 字符。
实际 JSON（8 个子货号，含中文目的地）超过此限制。

### 修复

```java
// ❌ VARCHAR(2048) — 字节限制，中文超限
@Column(name = "sub_product_code", length = 2048)

// ✅ TEXT — 无字符/字节限制
@Column(name = "sub_product_code", columnDefinition = "TEXT")
```

同时执行 DB 迁移：

```sql
ALTER TABLE replenishment_demand MODIFY COLUMN sub_product_code TEXT;
ALTER TABLE replenishment_demand MODIFY COLUMN linked_demand_items TEXT;
```

### 预防

| 字段类型 | 判定 |
|----------|------|
| 有明确上限的短字段 | `VARCHAR(n)` — 如 code、name |
| JSON / 自由文本 | `TEXT` — 无上限保证 |
| 大字段（文章/HTML） | `LONGTEXT` |

> 同理：`linkedDemandItemsRaw`（v1.6.0 JSON）也改为 TEXT

---

*来源：2026-04-24 sub_product_code VARCHAR 超限修复 · V27 migration*

---

## Lesson 32: 重构实体移除字段后必须同步清理数据库旧列

### 问题

v1.6.0 重构 `ReplenishmentDemand` 实体，移除了 `quantity`、`destination`、`linked_procurementId` 三个字段，改用 JSON 数组存储。

但数据库 `replenishment_demand` 表中旧列仍存在，且 `quantity` 定义为 `INT NOT NULL`（无默认值）。

INSERT 时 Hibernate 不填旧列，MySQL 报错：

```
SQLException: Field 'quantity' doesn't have a default value
```

### 根因

Hibernate `ddl-auto: update` **只添加新列**，不会删除已存在但实体中不再引用的列。重构时删了字段但漏了 DB 旧列。

### 本次修复

```sql
ALTER TABLE replenishment_demand DROP COLUMN quantity;
ALTER TABLE replenishment_demand DROP COLUMN destination;
ALTER TABLE replenishment_demand DROP COLUMN linked_procurement_id;
```

同步创建 `V27__demand_json_columns_text.sql` 和 `V28__demand_v1_6_schema.sql` 记录变更。

### 预防

| 重构场景 | 必做事项 |
|----------|----------|
| 新增字段 | DB migration + entity 同步 |
| 删除字段 | **DB migration 删除旧列 + entity 移除字段** |
| 重命名字段 | DB rename + entity 改名，禁止先删后加（丢数据） |
| 改列类型 | DB ALTER + entity `@Column` 更新 |

- `ddl-auto: update` 不是银弹——它只处理**新增**，不处理**删除**
- 重构前查 `DESCRIBE table_name` 确认当前 DB 结构
- Flyway 禁用时，所有表结构变更必须手动执行 ALTER

---

## 总结：二十七条铁律（更新版）

### 工程实践（代码层）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 1 | 跨模块走 Port 接口 | Lombok 失效 |
| 2 | Domain model 禁止引用其他模块 Entity | 编译耦合 |
| 3 | JPA Repository 继承链 → @Qualifier | Spring 启动失败 |
| 4 | BusinessException API 添加前搜索全项目 | 编译失败 |
| 5 | Model 重构后测试必须同步 | 测试编译失败 |
| 6 | Repository 方法不依赖返回类型区分同名方法 | 编译失败 |
| 22 | JPA 持久化字段必须显式标注 @Column | 列映射错误 |
| 23 | 枚举注释必须与业务语义严格对齐 | 理解歧义 |
| 29 | 删除旧注解时必须同步删除所有引用 | 编译失败 |
| 31 | JSON 存储列必须用 TEXT，不用 VARCHAR | 数据截断 |
| 32 | 实体删除字段后必须同步 DROP DB 列 | 插入失败 |

### 工程实践（环境/部署）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 7 | Windows 用 Git Bash 执行 .sh 脚本 | 脚本报错 |
| 8 | Flyway 禁用时数据初始化走 DevTestDataInitializer | 数据不导入 |
| 9 | 密钥资源走 classpath + 文件系统双路径 | 启动失败 |
| 17 | 开发环境配置走 .env.local/proxy | 环境差异 |
| 18 | private.pem 仅存于签发中心服务 | 安全漏洞 |
| 20 | 分页约定 page=0 vs page=1 开发前锁定 | 前后端不对齐 |
| 26 | 打包禁止 `-q` + 确保无旧进程锁 JAR | JAR 不可用 |
| 27 | 运行时依赖不能是 test scope | 启动失败 |
| 28 | 编译与启动必须分离，错误必须可见 | 错误被掩盖 |

### 架构/模块化

| # | 铁律 | 违反后果 |
|---|------|---------|
| 10 | Controller 返回类型必须属于自己模块 | 模块边界模糊 |
| 15 | 单体内部分层清晰，提前规划拆分边界 | 未来拆分成本高 |
| 19 | 共享代码抽取到 common 模块 | 代码冗余 |

### 文档/命名

| # | 铁律 | 违反后果 |
|---|------|---------|
| 11 | 文档和代码同 commit，持续审计不积累 | 多轮返工 |
| 12 | i18n 从第一天规划 | 4轮返工 |
| 13 | Flyway 版本号提前规划，禁止重编号 | checksum 不一致 |
| 14 | 命名在开发前锁定，禁止中途改名 | 大量迁移返工 |

### 前端/契约/测试

| # | 铁律 | 违反后果 |
|---|------|---------|
| 16 | 前端类型从 OpenAPI schema 生成 | 字段不匹配 |
| 21 | BaseEntity 用 @MappedSuperclass | 审计字段不一致 |
| 24 | 测试数据提取禁止用字符串解析 | 测试脆弱 |
| 25 | 领域层 Repository 禁止加 @Repository | Spring Bean 歧义 |
| 30 | Test 代码必须与 Model/API 同步更新 | 编译失败 |
| 33 | 业务链起点 = Overview 入口锚点 | Demand 新建后不可见 |
| 34 | 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步 | undefined 显示 |

---

## Lesson 33: 锚点设计决定 Overview 可见性——Procurement 中心导致 Demand 新建后不可见

### 问题

`/base/overview` 列表只展示 Procurements。用户新建 Demand 后，Demand 不会出现在 Overview 中，必须先转采购才能看到。

### 根因

Overview 以 `Procurement.id` 为锚点：
- 列表 API：`GET /orders/selector` 只查 Procurement 表
- 详情 API：`GET /orders/{procurementId}/overview` 锚点是 Procurement
- Step1 卡片只在 `procurement.linkedDemandId != null` 时显示

### 设计原则

**业务链路的起点必须是 Overview 的入口**。如果业务从 Demand 开始，Overview 必须支持 Demand 锚点。

### 修复方案

双入口架构：
```
/base/overview                          → 双 Tab（需求单 / 发注单）
/base/overview/demand/:demandId         → Demand 锚点（Step1 有数据）
/base/overview/procurement/:procurementId → Procurement 锚点（8 步全链路）
```

### 教训

> **业务链起点 = Overview 入口锚点。禁止以中间环节（如 Procurement）作为唯一的入口锚点。**

---

## Lesson 34: 前端类型定义必须与后端 VO 同步——接口变更后旧字段残留导致 undefined

### 问题

后端 `DemandVO` 从 `quantity/destination` 改为 `subProductItemsSummary`（v1.6.0），但前端 `OrderOverviewPage.vue` Step1 卡片仍引用 `overview.demand.quantity`，导致页面显示 `undefined`。

### 根因

前端 API 类型定义与后端 VO 不同步：
- 后端改了 `DemandVO`，删除了 `quantity`/`destination`
- 前端 `DemandVO` 类型定义未同步更新
- 前端模板仍用旧字段名

### 修复

变更接口时同步更新前后端类型定义：
1. 后端修改 VO → 同步更新前端 API 类型定义
2. 前端模板字段引用 → 对应更新
3. i18n key → 同步调整

### 教训

> **接口变更 = 后端 VO + 前端类型 + 前端模板 + i18n 四处同步。缺一不可。**

---

*来源：2026-04-24 sub_product_code VARCHAR 超限修复 · V27 migration · V28 schema 记录 · 旧列 DROP 修复 · 铁律表更新（18→32条）*
*新增：Lesson 33（锚点设计）· Lesson 34（前后端类型同步）· Lesson 35（文档引用必须与实现对齐）· Lesson 36（跨实体字段来源必须明确）· Lesson 37（前端状态标签必须本地化）· Lesson 38（业务逻辑校验必须在入口）*

---

## Lesson 35: 文档引用必须与实现严格对齐，禁止引用不存在的命名

### 问题

审计发现 `SPEC-B09-IMPLEMENTATION.md` 两处引用 `SalesRepository`，但全系统其他位置均为 `SalesRecordRepository`。这是 typo，文档与实现不一致。

### 根因

- 文档编写时根据上下文推断命名，未与实际代码核对
- 跨文档引用时未做 grep 验证

### 本次修复

```java
// ❌ SPEC-B09-IMPLEMENTATION.md（旧，错误）
private final SalesRepository salesRepository;

// ✅ 修正后
private final SalesRecordRepository salesRecordRepository;
```

### 预防

- 文档中引用类名/方法名时，先 `grep` 确认该名称在代码中存在
- 代码中的命名变更 → 同步更新所有引用该名称的文档
- 建议：文档中的代码片段应该可以直接复制编译

---

## Lesson 36: 聚合根引用其他实体字段时，必须在文档中注明来源

### 问题

`SPEC-B08-运营销售-步骤8.md` 中描述反馈循环时：

```
destination = this.destination（来自 Procurement）
```

但 `SalesRecord` 聚合根中根本没有 `destination` 字段，`destination` 必须通过 `procurementId` 查询 `Procurement` 获取。

### 根因

文档描述领域方法时，想当然地认为聚合根包含所有字段，没有区分"自身字段"和"引用字段"。

### 正确描述

```
destination = Procurement.destination（通过 procurementId 查询 Procurement 获取）
```

### 预防

- 聚合根字段表 + 引用字段表分开列示
- 跨实体数据访问在文档中明确标注"来自 XxxEntity"

---

## Lesson 37: 前端状态标签必须本地化，禁止直接显示枚举值

### 问题

`DemandOverviewPage.vue` 中：

```vue
<!-- ❌ 直接显示枚举值 -->
<el-tag>{{ overview.demand.status }}</el-tag>
<!-- 日语用户看到: PENDING / CONVERTED / CANCELLED -->

<!-- ✅ 显示本地化文本 -->
<el-tag>{{ demandStatusLabel(overview.demand.status) }}</el-tag>
<!-- 日语用户看到: 確認待ち / 発注済み / キャンセル済み -->
```

### 根因

- 状态枚举值在中文环境下可读，但日语环境下是乱码
- 同一字段在 `DemandPage.vue` 中已做了本地化，但在 `DemandOverviewPage.vue` 中遗漏

### 本次修复

```typescript
// DemandOverviewPage.vue
function demandStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  return t(`demand.status.${status}`)  // PENDING → 待确认 / 確認待ち
}
```

### 预防

- 前端组件中所有 `{{ someStatus }}` 文本显示必须经过 i18n 转换
- ESLint rule: `no-hardcoded-strings` 扩展到枚举值
- 新增页面时，状态字段必须使用 label 函数，不允许直接插值

---

## Lesson 38: 业务逻辑校验必须在入口处，零值/空值校验是防御性编程底线

### 问题

`ReplenishmentDemandUseCase.convertToProcurement()` 遍历 SubProductItem 列表生成 Procurement 时，未校验 `quantity` 是否为 0 或 null。

若 JSON 中有 `{subCode:"be", quantity:0}` 的异常数据，会生成数量为 0 的 Procurement。

### 根因

- 认为"前端已校验"就不在后端重复校验
- JSON 解析路径（旧数据兼容）可能产生零值

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
| 日期字段 | `!= null` 且 `>= 今天`（若业务要求）|

### 预防

- UseCase 入口处必须有完整的参数校验
- `@Valid` 注解只校验格式，不校验业务语义（如 > 0）
- 校验异常统一抛出 `BusinessException.invalidParam()`

---

## Lesson 39: 数据库 schema 文档必须与实现同步，版本号体现同步状态

### 问题

`DB-01-procurement-demand.md` 中 `replenishment_demand` 表结构为 v1.5.x 格式（含 `quantity`、`destination`、`linked_procurement_id` 列），但 v1.6.0 实体已移除这些列，改为 JSON 字段。

### 根因

- 文档和代码分属不同提交/分支，版本演进时未同步
- 文档版本号（v1.2.0）与代码版本号（v1.6.0）脱节

### 预防

| 变更类型 | 文档要求 |
|----------|---------|
| 表字段变更 | DB 文档版本 + changelog + migration 脚本三方同步 |
| 新增字段 | DB 文档 + entity + migration 同 commit |
| 删除字段 | DB migration DROP 列 + 文档同步删除 |
| 关联关系反转 | 所有引用该关系的文档全部更新 |

> **文档版本号 ≥ 代码版本号。代码到哪个版本，文档必须跟到哪个版本。**

---

## Lesson 40: TypeScript strict 编译必须通过，TS6133/TS2345 是代码腐烂信号

### 问题

`vue-tsc --noEmit` 编译失败，大量 TS 错误：

```
TS6133: 'onSubCodeChange' is declared but never used        // DemandPage.vue
TS6133: 'index' is declared but never used                 // OrderPage.vue (2处)
TS2345: Argument of type '{ currentStep: number }' is not assignable
        to parameter of type 'StepStatus[]'                   // StatusProgressBar.vue
```

### 根因

- 前端组件参数与调用方不一致（如 `StatusProgressBar` 的 `currentStep` 应为 optional）
- 旧函数被重构后遗留（`onSubCodeChange`）
- Vue `v-for` 的 `index` 参数未使用

### 本次修复

| 错误类型 | 修复方式 |
|---------|---------|
| TS6133 未使用变量 | 移除声明，或用 `_` 前缀（`v-for` 的 index）|
| TS2345 类型不匹配 | 检查组件 `defineProps` 定义与调用方是否对齐 |

```typescript
// ✅ 正确：currentStep 声明为可选
defineProps<{
  stepStatuses: StepStatus[]
  currentStep?: number  // ← optional，与3处调用方匹配
}>()

// ❌ 错误：currentStep 声明为必需
defineProps<{
  stepStatuses: StepStatus[]
  currentStep: number  // ← 3个页面调用时未传参 → TS2345
}>()
```

### 预防

- `vue-tsc --noEmit` 必须加入 CI，检查 `tsconfig.json` 中 `strict: true`
- `tsconfig.json` 包含以下规则：
  ```json
  {
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true
  }
  ```
- PR 合入前 `npm run build` 必须成功

---

## Lesson 41: 前端 API 签名变更后所有调用方必须同步，v1.6 破坏性变更教训

### 问题

v1.6.0 后端 `convertToProcurement` API 签名从：
```
convertToProcurement(id: number, factoryId: number)
```
改为：
```
convertToProcurement(id: number, cmd: ConvertDemandCmd)
```

前端 `OrderPage.vue` 仍用旧签名调用：
```typescript
// ❌ 旧调用（编译通过但语义错误）
demandApi.convertToProcurement(id, factoryId as number)

// ✅ 新调用
demandApi.convertToProcurement(id, { factoryId: factoryId as number })
```

### 根因

- 后端 API 签名变更未同步更新前端 API 客户端类型
- 前端 API 层（`api/demand.ts`）的请求类型定义与实际不匹配
- 没有前端覆盖测试（TypeScript 编译通过但运行时有误）

### 本次修复

| 旧 API（v1.5） | 新 API（v1.6） |
|---|---|
| `convertToProcurement(id, factoryId)` | `convertToProcurement(id, { factoryId })` |
| `d.subProductCodes[0]` | `d.subProductItems?.[0]?.subCode` |
| `d.destination` | `d.subProductItems?.[0]?.destination` |
| `d.quantity` | `d.subProductItems?.[0]?.quantity` |

### 预防

> **后端 API 破坏性变更 = 前端 API 类型 + 所有调用方 + 单元测试 三处同步。**

- OpenAPI schema 变更必须触发前端 codegen 重新生成
- 无 codegen 时：变更后端 API → 立即 grep 全前端 `api/*.ts` 找调用方

---

## Lesson 42: Vue template `v-for` 的 `index` 参数未使用时必须加 `_` 前缀

### 问题

```typescript
// ❌ TS6133: index is declared but never used
tableData.forEach((item, index) => {
  console.log(item)  // index 未使用
})
```

### 根因

TypeScript `noUnusedParameters: true` 规则。

### 解决方案

```typescript
// ✅ 正确：index 不需要时加 _ 前缀
tableData.forEach((item, _index) => {
  console.log(item)
})

// ✅ 或解构忽略
tableData.forEach((item) => {
  console.log(item)
})
```

### 预防

- `tsconfig.json` 开启 `noUnusedParameters: true`
- IDE 配置实时提示未使用变量（WebStorm/VSCode TypeScript 插件）

---

## Lesson 43: 前端组件 Props 必须与所有调用方对齐——optional 字段不得隐式 required

### 问题

`StatusProgressBar.vue` 组件的 `currentStep` 声明为必需，但 3 个调用页面（`DemandPage.vue`、`OrderPage.vue`、`OrderOverviewPage.vue`）均未传此参数：

```
TS2345: Argument of type '{ stepStatuses: StepStatus[] }'
is not assignable to parameter of type
'{ stepStatuses: StepStatus[]; currentStep: number }'
```

### 根因

- 组件设计时认为 `currentStep` 必需，但实际业务场景中页面不需要显示"当前步骤高亮"
- `defineProps` 定义与调用方数量不匹配时，TypeScript 无法自动推导

### 解决方案

```typescript
// ✅ 正确：非必需字段加 ?
defineProps<{
  stepStatuses: StepStatus[]
  currentStep?: number  // ← optional，调用方可以不传
}>()
```

### 预防

- 设计组件时，默认所有 props 均为 optional（`?`）
- 必须为 required 的字段才不加 `?`（如列表数据的 `items`）
- 组件添加新 prop 前，grep 所有调用方确认使用场景

---

## Lesson 44: 前端对话框表格列标签必须提取为 i18n key，禁止硬编码

### 问题

`DemandPage.vue` 中"关联采购单"对话框的表格列标签硬编码：

```vue
<!-- ❌ 硬编码中文字符串 -->
<el-table-column prop="id" label="采购单号" />
<el-table-column prop="factoryName" label="工厂名称" />
<el-table-column prop="productCode" label="货号" />
```

日语用户看到中文标签，无法理解。

### 根因

- 对话框 UI 后加，漏掉了 i18n 集成
- 硬编码比写 `{{ $t('...') }}` 更快，养成坏习惯

### 解决方案

```vue
<!-- ✅ i18n 化 -->
<el-table-column prop="id" :label="t('demand.linkedDialog.column.id')" />
<el-table-column prop="factoryName" :label="t('demand.linkedDialog.column.factoryName')" />
```

```json
// zh.json
"demand": {
  "linkedDialog": {
    "title": "关联采购单",
    "column": {
      "id": "采购单号",
      "factoryName": "工厂名称",
      "productCode": "货号"
    }
  }
}

// ja.json
"demand": {
  "linkedDialog": {
    "title": "関連発注書",
    "column": {
      "id": "発注番号",
      "factoryName": "工場名",
      "productCode": "商品番号"
    }
  }
}
```

### 预防

- ESLint rule `vue/i18n` 或 `vue/no-v-text-v-skeleton` 检查硬编码文本
- 新增组件时，所有文本显示默认用 `$t()`，不提供文本默认值

---

## Lesson 45: Flyway 迁移文件版本号不得重复，冲突时立即修正

### 问题

两个迁移文件同名 V24：

```
V24__product_field_extend.sql     — 添加 warehouse/remarks 修复
V24__product_hs_code_extend.sql  — 添加 hs_code_jp 字段
```

Flyway 按版本号排序执行，V24 冲突导致执行顺序不确定，或其中一个被忽略。

### 根因

- 多人同时开发时未协调版本号空间
- 没有统一的 Flyway 版本路线图文档
- `V10/V20/V30` 留空原则未被遵守

### 本次修复

```bash
# 重命名冲突文件至下一个可用版本
mv V24__product_hs_code_extend.sql V30__product_hs_code_extend.sql

# 更新文件内注释
-- Migration: V30__product_hs_code_extend.sql
-- Note: 原 V24 与 V24__product_field_extend.sql 重复，升至 V30
```

同步更新 SPEC-B10 中 "通过 V24 迁移完成" → "通过 V30 迁移完成"。

### 预防

| 场景 | 操作 |
|------|------|
| 新增迁移前 | `ls db/migration/` 查看已用版本号，避免重复 |
| 大版本功能前 | 预留空间：`V10__`, `V20__`, `V30__`（每个模块 10 个版本） |
| 紧急热修复 | 用 `V10_1__fix_xxx.sql` 或 `V10_hotfix__xxx.sql` |
| 发现版本冲突 | 立即修正，不要遗留（Flyway 执行后修复更复杂） |

> **Flyway 版本路线图应记录在 `docs/database/README.md` 中，每次开发前确认下一个可用版本。**

---

## 总结：二十七条铁律（最终版）

### 工程实践（代码层）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 1 | 跨模块走 Port 接口 | Lombok 失效 |
| 2 | Domain model 禁止引用其他模块 Entity | 编译耦合 |
| 3 | JPA Repository 继承链 → @Qualifier | Spring 启动失败 |
| 4 | BusinessException API 添加前搜索全项目 | 编译失败 |
| 5 | Model 重构后测试必须同步 | 测试编译失败 |
| 6 | Repository 方法不依赖返回类型区分同名方法 | 编译失败 |
| 22 | JPA 持久化字段必须显式标注 @Column | 列映射错误 |
| 23 | 枚举注释必须与业务语义严格对齐 | 理解歧义 |
| 29 | 删除旧注解时必须同步删除所有引用 | 编译失败 |
| 31 | JSON 存储列必须用 TEXT，不用 VARCHAR | 数据截断 |
| 32 | 实体删除字段后必须同步 DROP DB 列 | 插入失败 |
| 38 | 业务逻辑校验在入口处，零值/空值必须防御 | 脏数据 |

### 工程实践（环境/部署）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 7 | Windows 用 Git Bash 执行 .sh 脚本 | 脚本报错 |
| 8 | Flyway 禁用时数据初始化走 DevTestDataInitializer | 数据不导入 |
| 9 | 密钥资源走 classpath + 文件系统双路径 | 启动失败 |
| 17 | 开发环境配置走 .env.local/proxy | 环境差异 |
| 18 | private.pem 仅存于签发中心服务 | 安全漏洞 |
| 20 | 分页约定 page=0 vs page=1 开发前锁定 | 前后端不对齐 |
| 26 | 打包禁止 `-q` + 确保无旧进程锁 JAR | JAR 不可用 |
| 27 | 运行时依赖不能是 test scope | 启动失败 |
| 28 | 编译与启动必须分离，错误必须可见 | 错误被掩盖 |

### 架构/模块化

| # | 铁律 | 违反后果 |
|---|------|---------|
| 10 | Controller 返回类型必须属于自己模块 | 模块边界模糊 |
| 15 | 单体内部分层清晰，提前规划拆分边界 | 未来拆分成本高 |
| 19 | 共享代码抽取到 common 模块 | 代码冗余 |

### 文档/命名

| # | 铁律 | 违反后果 |
|---|------|---------|
| 11 | 文档和代码同 commit，持续审计不积累 | 多轮返工 |
| 12 | i18n 从第一天规划 | 4轮返工 |
| 13 | Flyway 版本号提前规划，禁止重编号 | checksum 不一致 |
| 14 | 命名在开发前锁定，禁止中途改名 | 大量迁移返工 |
| 35 | 文档引用类名/方法名必须与代码 grep 对齐 | 文档失效 |
| 36 | 跨实体字段引用在文档中注明来源 | 误解数据流 |
| 39 | DB schema 文档版本号 ≥ 代码版本号 | 文档失效 |

### 前端/契约/测试

| # | 铁律 | 违反后果 |
|---|------|---------|
| 16 | 前端类型从 OpenAPI schema 生成 | 字段不匹配 |
| 21 | BaseEntity 用 @MappedSuperclass | 审计字段不一致 |
| 24 | 测试数据提取禁止用字符串解析 | 测试脆弱 |
| 25 | 领域层 Repository 禁止加 @Repository | Spring Bean 歧义 |
| 30 | Test 代码必须与 Model/API 同步更新 | 编译失败 |
| 33 | 业务链起点 = Overview 入口锚点 | Demand 新建后不可见 |
| 34 | 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步 | undefined 显示 |
| 37 | 前端状态标签必须本地化，禁止直接显示枚举值 | 日语用户看到乱码 |
| 40 | vue-tsc --noEmit 必须通过（strict + noUnusedLocals） | TS6133/TS2345 累积 |
| 41 | 后端 API 破坏性变更 = 前端 API 类型 + 所有调用方 + 单元测试 三处同步 | 运行时错误 |
| 42 | v-for 的 index 参数未使用加 `_` 前缀 | TS6133 |
| 43 | 组件 Props 必须与所有调用方对齐——optional 字段不加 `?` 会导致 TS2345 | 编译失败 |
| 44 | 对话框/表格列标签必须提取为 i18n key，禁止硬编码 | 日语用户无法理解 |
| 45 | Flyway 迁移版本号不得重复，冲突时立即修正 | 迁移执行顺序不确定 |

---

*来源：2026-04-24 全量审计会话 · docs/business 审计（SPEC-B01/B03/B07/B08/B09/B10）· docs/database 审计（DB-01/DB-06/DB-08/DB-09/废弃旧文档）· 后端代码审计 · 前端代码审计 · lessons 文档更新（Lesson 35-45，铁律表更新至27条规则）*

