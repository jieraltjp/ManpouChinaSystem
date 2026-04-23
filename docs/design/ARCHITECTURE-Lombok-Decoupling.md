# manpou-allinone Lombok 编译失败修复方案

## 1. 问题根因

### 1.1 现象

`mvn clean compile` 失败，报错：

```
找不到符号: 方法 builder()        // @Builder 不生成
找不到符号: 变量 log             // @Slf4j 不生成
找不到符号: 方法 getXxx()        // @Getter 不生成
找不到符号: 方法 setXxx()        // @Setter 不生成
```

### 1.2 根本原因：application 层跨模块直接引用 domain model

当模块 A 的 `application` 层直接引用模块 B 的 `domain` 类时，javac 编译顺序不确定。Lombok 的字节码注入依赖于 **在同一个编译批次中看到所有源码**，一旦编译顺序错乱：

1. 编译 A 类时，需要 B 类的字节码
2. B 类此时还在编译中，Lombok 尚未生成 getter/setter/builder 等方法
3. 编译失败

**验证结论：**

| 模块组合 | 编译结果 |
|---------|---------|
| common + domain | SUCCESS |
| common + domain + customs | SUCCESS |
| common + domain + customs + factory（移除跨模块引用后） | SUCCESS |
| common + domain + customs + factory + logistics | FAIL |
| 全部模块 | FAIL |

**common 模块本身没有跨模块引用，但在全部编译时会报错**，说明是某个其他模块的编译失败"级联污染"了 Lombok processor。

### 1.3 受污染报错的模块

确认由跨模块引用导致的 Lombok 失败：

- `common/result/Result.java` — `@Builder` builder() 不生成
- `common/filter/TraceFilter.java` — `@Slf4j` log 不生成
- `customs/application/KeyManagementService.java` — assembler builder() 不生成
- `customs/application/assembler/CustomsAssembler.java` — 多字段 getter 不生成
- `product/domain/repository/ProductRepository.java` — 父接口方法找不到

---

## 2. 循环依赖链路

```
factory      →  procurement (existsActiveByFactoryId)
logistics    →  factory (FactoryRepository)
order        →  factory + logistics + customs + procurement
qc           →  factory
product      →  factory
finance      →  procurement
infrastructure →  procurement
procurement  →  factory + product + qc
interfaces   →  product
```

---

## 3. 修复方案：依赖倒置 + Port 接口

### 3.1 核心原则

**application 层不允许直接引用其他模块的 domain model 或 repository。**

依赖关系：

```
模块 A application  →  模块 B domain (WRONG)
模块 A application  →  common.port.XxxPort  →  模块 B domain (CORRECT)
```

### 3.2 通用 Port 接口定义

在 `common.port` 包下定义跨模块通信接口：

```java
// common/port/ProcurementPort.java
public interface ProcurementPort {
    boolean existsActiveByFactoryId(Long factoryId);
    // 其他需要跨模块查询的方法
}

// common/port/ProductQueryPort.java
public interface ProductQueryPort {
    Product findByMasterCode(String masterCode);
    List<Product> findByFactoryId(Long factoryId);
}

// common/port/FactoryQueryPort.java
public interface FactoryQueryPort {
    Factory findById(Long id);
}

// common/port/LogisticsQueryPort.java
public interface LogisticsQueryPort {
    LogisticsPlan findById(Long id);
}

// common/port/CustomsQueryPort.java
public interface CustomsQueryPort {
    DomesticCustomsRecord findById(Long id);
}

// common/port/QcQueryPort.java
public interface QcQueryPort {
    QcRecord findById(Long id);
}
```

### 3.3 各模块解耦步骤

#### Step 1: Factory 模块

**问题：** `FactoryUseCase.delete()` 引用 `ProcurementRepository`

**修改：**

```java
// 移除
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
private final ProcurementRepository procurementRepository;

// 改为
import com.manpou.allinone.common.port.ProcurementPort;
private final ProcurementPort procurementPort;

// delete() 方法中
if (procurementPort.existsActiveByFactoryId(id)) { ... }
```

**Infrastructure 实现：**

```java
// infrastructure/port/ProcurementPortImpl.java
@Component
@RequiredArgsConstructor
public class ProcurementPortImpl implements ProcurementPort {
    private final ProcurementRepository procurementRepository;

    @Override
    public boolean existsActiveByFactoryId(Long factoryId) {
        return procurementRepository.existsActiveByFactoryId(factoryId);
    }
}
```

#### Step 2: Logistics 模块

**问题：** `LogisticsPlanAssembler` 引用 `FactoryRepository`

**修改：** 移除直接引用，改为通过 `FactoryQueryPort` 或传参

#### Step 3: Procurement 模块

**问题：** `ProcurementAssembler` 引用 `Factory`; `ProcurementQcPassedEventListener` 引用 `ProductRepository`, `QcRecordRepository`

**修改：**
- `ProcurementAssembler` 改为接收 `FactoryDTO` 而非 `Factory` 实体
- `ProcurementQcPassedEventListener` 改为通过 `ProductQueryPort`, `QcQueryPort` 接口查询

#### Step 4: Order 模块

**问题：** 引用 factory + logistics + customs + procurement

**修改：** 全部改为 Port 接口

#### Step 5: QC 模块

**问题：** 引用 factory

#### Step 6: Product 模块

**问题：** `ProductAssembler` 引用 `Factory`; `ProductFactoryVO` 引用 `Factory.CooperationStatus`

**修改：** 改为 DTO 传参

#### Step 7: Finance 模块

**问题：** 引用 procurement

#### Step 8: Infrastructure 模块

**问题：** `DevTestDataInitializer` 引用 procurement

**修改：** 暂时注释或删除该初始化器（dev only）

#### Step 9: Interfaces 模块

**问题：** `KeyManagementController` 引用 product

---

## 4. 执行顺序

按依赖关系从底层到上层修改：

```
Step 1: Factory        (移除 procurement 引用)
Step 2: Logistics      (移除 factory 引用)
Step 3: Procurement    (移除 factory/product/qc 引用)
Step 4: QC             (移除 factory 引用)
Step 5: Product        (移除 factory 引用)
Step 6: Finance        (移除 procurement 引用)
Step 7: Order          (移除所有跨模块引用)
Step 8: Infrastructure (移除/注释 dev initializer)
Step 9: Interfaces     (移除 product 引用)
```

---

## 5. 编译验证

每完成一个模块的解耦，立即执行：

```bash
cd apps/manpou-allinone
mvn clean compile
```

验证 `BUILD SUCCESS` 后再进行下一个模块。

---

## 6. Port 接口清单

| Port 接口 | 调用方 | 提供方 | 方法 |
|---------|-------|-------|------|
| `ProcurementPort` | Factory | Procurement | `existsActiveByFactoryId` |
| `FactoryQueryPort` | Logistics | Factory | `findById`, `findByFactoryName` |
| `ProductQueryPort` | Procurement, Order | Product | `findByMasterCode`, `findByFactoryId` |
| `QcQueryPort` | Procurement, Order | QC | `findById` |
| `LogisticsQueryPort` | Order | Logistics | `findById`, `findByProcurementId` |
| `CustomsQueryPort` | Order | Customs | `findById` |

---

## 7. 注意事项

1. **Port 接口放在 `common.port` 包下**，因为它需要被多个模块共享
2. **Port 实现放在调用方的 `infrastructure` 层**，避免循环
3. **DTO 传参代替实体传参**，减少跨模块边界传递的复杂度
4. **事件机制解耦**（可选优化）：用 Spring Event 替代直接方法调用，完全异步化
