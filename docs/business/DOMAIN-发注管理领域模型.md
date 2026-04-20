# 发注管理 — 领域模型

> **版本**: 1.0.0
> **更新**: 2026-04-20

---

## 1. 聚合根

### Procurement（发注单聚合根）

```
Procurement（聚合根）
├── id: Long
├── name: String(128)
├── factoryId: Long
├── status: ProcurementStatus(枚举)
├── priority: Priority(枚举)
├── shippingMode: ShippingMode(枚举)
├── qcType: QcType(枚举)
├── qcResult: QcResult(枚举, nullable)
├── containerId: Long(nullable)
├── financeId: Long(nullable)
├── createdBy: String(64)
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
├── isDeleted: Boolean
│
├── items: List<ProcurementItem>  (内聚值对象集合)
└── 状态转换方法（领域规则）
    ├── suspend(reason)
    ├── resume()
    ├── submitQc()
    ├── approveQc()
    ├── rejectQc()
    ├── selectShippingMode()
    ├── ship()
    └── close()
```

---

## 2. 值对象

### ProcurementItem（发注商品）

```
ProcurementItem（值对象）
├── id: Long
├── productName: String(128)
├── productCode: String(64)
├── unitPriceCny: BigDecimal(10,2)
├── exchangeRate: BigDecimal(8,4)
├── taxRate: BigDecimal(5,2)
├── material: String(64)
├── weightKg: BigDecimal(8,3)
├── lengthCm: Integer
├── widthCm: Integer
├── heightCm: Integer
├── quantity: Integer
├── remarks: String(256)
│
└── 计算属性（只读）
    ├── unitPriceJpy = unitPriceCny × exchangeRate
    ├── taxJpy = unitPriceJpy × taxRate
    ├── volumeCbm = length × width × height / 1_000_000
    ├── subtotalCny = unitPriceCny × quantity
    └── subtotalJpy = unitPriceJpy × quantity
```

### Container（货柜信息）

```
Container（值对象）
├── id: Long
├── containerNo: String(20)     # 箱号
├── sealNo: String(20)           # 封号
├── containerType: ContainerType  # 20GP/40GP/40HC/45HC
├── shippingMethod: ShippingMethod # SEA/AIR/LAND
├── portOfLoading: String(32)    # 起运港
├── portOfDestination: String(32)# 目的港
├── estimatedShipDate: LocalDate
├── cutoffDate: LocalDate
└── totalCbm: BigDecimal
```

### Finance（财务结算）

```
Finance（值对象）
├── id: Long
├── taxType: TaxType
├── totalCostCny: BigDecimal(12,2)
├── actualPaidCny: BigDecimal(12,2)
├── currency: String(3)          # CNY/JPY
├── paymentStatus: PaymentStatus
└── remarks: String(256)
```

---

## 3. 枚举

### ProcurementStatus（发注单状态）

```java
public enum ProcurementStatus {
    PENDING(0),       // 待执行
    SUSPENDED(1),     // 已挂起
    IN_PROGRESS(2),  // 执行中
    QC_PENDING(3),    // 待验收
    REJECTED(4),      // 返工
    QC_PASSED(5),    // 验收通过
    SHIPPING(6),     // 已发货
    CLOSED(7);       // 流程闭环（终态）

    private final int order;
    // 禁止逆向流转：CLOSED 后不可修改
}
```

### Priority（优先级）

```java
public enum Priority {
    STANDARD(0),         // 标准
    PRODUCTION_FIRST(1), // 优先排产
    SHIP_FIRST(2)         // 优先发货
}
```

### ShippingMode（运输模式）

```java
public enum ShippingMode {
    WAREHOUSE,   // 自有仓库
    POOL,        // 虚拟拼柜
    DIRECT       // 厂家直装
}
```

### QcType（验收方式）

```java
public enum QcType {
    ONSITE,  // 现场验收
    REMOTE   // 远程图片验收
}
```

### QcResult（验收结果）

```java
public enum QcResult {
    PASS,
    REJECT
}
```

### ContainerType

```java
public enum ContainerType {
    GP20(33, 67.7),  // 20GP: 自重/TARE, 最大载重/PAYLOAD
    GP40(37, 67.7),
    HC40(39, 67.7),  // 40HC: 高箱
    HC45(42, 67.3);  // 45HC

    private final double tareKg;      // 自重(T)
    private final double payloadM3;  // 最大容积(m³)

    public double maxCbm() { return payloadM3; }
}
```

---

## 4. 拼柜池聚合

### ConsolidationPool（拼柜池聚合根）

```
ConsolidationPool
├── id: Long
├── destinationPort: String(32)
├── status: PoolStatus
├── containerThresholdCbm: BigDecimal
├── totalCbm: BigDecimal
├── totalBoxes: Integer
├── estimatedConsolidationDate: LocalDate(nullable)
├── containerId: Long(nullable)
│
└── 领域方法
    ├── add(Procurement)   // 加入拼柜池
    ├── remove(Procurement)// 移出
    ├── calculateFillRate() // 填充率
    ├── isReady()          // 是否满足一柜
    └── consolidate()      // 触发装柜
```

### PoolStatus

```java
public enum PoolStatus {
    POOL_PENDING,      // 待拼箱
    POOL_READY,        // 可安排（已凑够）
    CONTAINER_PLANNED, // 货柜计划已生成
    SHIPPED            // 已装柜
}
```

---

## 5. 仓储接口

### ProcurementRepository

```java
public interface ProcurementRepository
    extends JpaRepository<Procurement, Long> {

    Page<Procurement> findByFactoryId(Long factoryId, Pageable pageable);
    Page<Procurement> findByStatus(ProcurementStatus status, Pageable pageable);
    List<Procurement> findByIdInAndStatus(List<Long> ids, ProcurementStatus status);
}
```

### ConsolidationPoolRepository

```java
public interface ConsolidationPoolRepository
    extends JpaRepository<ConsolidationPool, Long> {

    List<ConsolidationPool> findByDestinationPortAndStatus(
        String port, PoolStatus status);

    Optional<ConsolidationPool> findByStatus(PoolStatus ready);
}
```

---

## 6. 领域服务

### ProcurementDomainService

**职责**：状态转换规则校验 + 触发副作用

```java
@Service
public class ProcurementDomainService {

    // 校验状态转换合法性
    public void validateTransition(Procurement p, ProcurementStatus next) {
        // CLOSED 后禁止任何操作
        // REJECTED 只允许 → PENDING
        // ...
    }

    // 验收通过后：根据 shippingMode 路由
    public void onQcPassed(Procurement p) {
        switch (p.getShippingMode()) {
            case WAREHOUSE -> p.moveToWarehouse();
            case POOL     -> p.addToConsolidationPool();
            case DIRECT   -> p.prepareDirectShip();
        }
    }
}
```

---

## 7. 关键业务规则

| 规则 | 说明 |
|------|------|
| CLOSED 终态不可逆 | 一旦闭环，禁止任何状态变更 |
| 返工只能回到 PENDING | 重新录入商品后重新提交验收 |
| 拼柜池按目的港隔离 | 相同目的港才能合箱 |
| 填充率 = totalCbm / containerThresholdCbm ≥ 1 才可触发合箱 |
| 财务结算在货柜录入后 | finance 必须在 container 之后 |
| 流程闭环 = 已报关 或 已日本签收 | 手动触发终态 |
