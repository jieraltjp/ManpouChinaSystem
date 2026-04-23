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

## 总结：十七条铁律

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

### 工程实践（环境/部署）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 7 | Windows 用 Git Bash 执行 .sh 脚本 | 脚本报错 |
| 8 | Flyway 禁用时数据初始化走 DevTestDataInitializer | 数据不导入 |
| 9 | 密钥/证书资源走 classpath + 文件系统双路径 | 启动失败 |
| 17 | 开发环境配置走 .env.local/proxy，本地必须可运行 | 环境差异 |
| 18 | private.pem 仅存于签发中心服务，禁止全量分发 | 安全漏洞 |
| 20 | 分页约定 page=0 vs page=1 必须在开发前锁定 | 前后端不对齐 |

### 架构/模块化

| # | 铁律 | 违反后果 |
|---|------|---------|
| 10 | Controller 返回类型必须属于自己模块 | 模块边界模糊 |
| 15 | 单体内部分层清晰，提前规划拆分边界 | 未来拆分成本高 |
| 19 | 共享代码抽取到 common 模块，禁止服务内联重复 | 代码冗余 |

### 文档/命名

| # | 铁律 | 违反后果 |
|---|------|---------|
| 11 | 文档和代码同 commit，持续审计不积累 | 多轮返工，版本 drift |
| 12 | i18n 从第一天规划，禁止后期打补丁 | 4轮返工，key 遗漏 |
| 13 | Flyway 版本号提前规划，留足空间禁止重编号 | checksum 不一致 |
| 14 | 命名在开发前锁定，禁止中途改名 | 大量迁移返工 |

### 前端/契约/测试

| # | 铁律 | 违反后果 |
|---|------|---------|
| 16 | 前端类型从 OpenAPI schema 生成，禁止手动对齐 | 字段不匹配 |
| 21 | BaseEntity 用 @MappedSuperclass，所有实体继承 | 审计字段不一致 |
| 24 | 测试数据提取禁止用字符串解析（grep），用专业 API 库 | 测试脆弱 |

---

*来源：git log（2026-04-10 ~ 04-23，104 commits）· docs/check/99-全面审计报告.md · docs/check/101~105 · docs/role/02-架构师视角.md · docs/pro/17-服务间认证.md · 2026-04-23 全量解耦重构会话*
