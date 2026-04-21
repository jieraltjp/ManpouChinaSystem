# 发注管理 — 领域模型

> **版本**: 1.3.0
> **更新**: 2026-04-21
> **依据**: 业务流分析（6步重构） + `docs/发注管理体系升级.pdf`

> **代码实现进度**:
> Procurement ✅ 已实现（需扩展字段） · Product 🔴 需扩展货号结构
> · ReplenishmentDemand 🔴 新增 · Factory 🔴 新增 · QcRecord 🔴 升级为聚合根
> · LogisticsPlan 🔴 新增 · DomesticCustoms 🔴 待定 · JapanCustoms 🔴 待定

---

## 变更记录（v1.2.0 → v1.3.0）

| 变更类型 | 实体 | 说明 |
|----------|------|------|
| **新增** | `ReplenishmentDemand` | 补货需求单聚合根，解决"未定"状态双重语义问题 |
| **新增** | `Factory` | 工厂聚合根，解决工厂信息内联在 Procurement 的问题 |
| **扩展** | `Product` | 新增 masterCode/subCode（主/子货号）+ colorName + material + productCategory |
| **扩展** | `Procurement` | 新增 factoryId + subProductCode + material + billingType + customsRemarks + instructionManual + actualShipDate |
| **升级** | `QcRecord` | 从值对象升级为独立聚合根，补全箱数/尺寸/序列号/净重/毛重/含税价/验收标准 |
| **新增** | `BillingType` | 替换 billingMethod，定义浙鲁开票/超慧退税/不退税等枚举 |
| **新增** | `ProductCategory` | OEM/普货/厂家出口 三种商品类型 |
| **新增** | `BoxDimension` | 箱子尺寸值对象（长/宽/高） |
| **新增** | `DomesticCustomsRecord` | 国内报关记录（字段待定） |
| **新增** | `JapanCustomsRecord` | 日本清关记录（字段待定） |
| **新增** | `LogisticsPlan` | 调配计划聚合根（整合 Container 和拼柜逻辑） |

---

## 1. 聚合根

### 1.1 ReplenishmentDemand（补货需求单 — 核心入口）

> 对应业务流第一步。新品采购与非新品补货是不同意图，必须分离。

```
ReplenishmentDemand（聚合根）
├── id: Long
├── demandType: DemandType           # REPLENISHMENT(补货) | NEW_PURCHASE(新品采购)
├── productCode: String              # 主货号（如 odn012）
├── subProductCode: String           # 子货号/枝番（如 odn012-re 黑色）
├── quantity: Integer               # 需求量
├── destination: String             # 目的地
├── japanLead: String               # 日本担当
├── status: DemandStatus            # PENDING(待确认) | CONVERTED(已转采购) | CANCELLED(取消)
├── linkedProcurementId: Long        # 关联的采购单ID（status=CONVERTED 时赋值）
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
└── 领域方法
    ├── convertToProcurement()       # 转为采购单，生成 Procurement 记录
    └── cancel()                     # 取消需求
```

**DemandType 枚举：**

```java
public enum DemandType {
    REPLENISHMENT,   // 非新品 — 补货
    NEW_PURCHASE      // 新品 — 采购
}
```

**DemandStatus 枚举：**

```java
public enum DemandStatus {
    PENDING,      // 待确认（录入后默认状态）
    CONVERTED,   // 已转采购（生成 Procurement 后推进至此）
    CANCELLED    // 已取消
}
```

---

### 1.2 Factory（工厂 — 独立实体）

> 对应业务流第二步。工厂信息独立管理，不再内联在 Procurement 中。

```
Factory（聚合根）
├── id: Long
├── factoryName: String              # 工厂名称
├── location: String                 # 工厂位置（省/市）
├── roughLocation: String            # 粗略位置（工业区/园区/镇）
├── contactName: String              # 联系人名称
├── contactPhone: String             # 联系人电话
├── status: FactoryStatus            # ACTIVE | INACTIVE
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
└── 领域方法
    └── linkProductCode(code)        # 关联该厂生产的货号
```

**FactoryStatus 枚举：**

```java
public enum FactoryStatus {
    ACTIVE,     // 正常合作
    INACTIVE    // 已停止合作
}
```

---

### 1.3 Procurement（出货单 — 核心聚合根）

> 对应业务流第二步下单 + 第三步验收前段。一次发注 = 一条出货单记录。

```
Procurement（聚合根）
├── id: Long
├── factoryId: Long                  # 关联工厂ID（新增）
├── productCode: String              # 主货号
├── subProductCode: String           # 子货号/枝番（颜色，如 odn012-re）（新增）
├── quantity: Integer                # 订购数量
├── priceRmb: BigDecimal             # 人民币单价
├── exchangeRate: BigDecimal         # CNY→JPY 汇率
├── taxPoint: BigDecimal             # 票点（默认 1.1）
├── billingType: BillingType         # 报关类型（替换 billingMethod）（新增）
├── orderDate: LocalDate             # 下单日
├── plannedShipDate: LocalDate       # 预计出货日（交货期）
├── actualShipDate: LocalDate        # 实际出货日（新增）
├── factoryShipDate: LocalDate        # 厂家出货日
├── material: String                 # 材质（新增）
├── productLead: String              # 商品担当
├── japanLead: String                # 日本担当
├── chinaLead: String                # 中国担当
├── destination: String               # 发送目的地
├── customerCompany: String           # 客户公司
├── instructionManual: String         # 说明书（新增）
├── customsRemarks: String           # 报关备注（新增）
├── status: ShipmentStatus            # 状态
├── qcType: QcType                   # 验货方式
├── qcResult: QcResult               # 验货结果
├── qcRecordId: Long                 # 验收记录ID（升级为聚合根引用）（变更）
├── logisticsPlanId: Long            # 调配计划ID（新增）
├── containerId: Long                # 货柜ID
├── financeId: Long                  # 财务结算ID
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
├── 计算属性（只读，非持久化）
│   └── estimatedPriceJpy = (priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05
│
└── 领域方法（状态推进）
    ├── submitQc(type)               # 提交验货
    ├── approveQc()                  # 验货通过
    ├── rejectQc()                   # 验货不通过（触发退货路径）
    ├── assignLogistics(plan)        # 分配调配计划
    ├── loadToContainer()            # 装柜
    ├── settle()                     # 财务结算
    └── close()                      # 终态（完了后禁止任何变更）
```

---

### 1.4 Product（商品目录 — 聚合根）

> 对应业务流所有步骤的商品基础信息。新增主/子货号结构支持颜色变体。

```
Product（聚合根）
├── id: Long
├── masterCode: String               # 主货号（唯一键，如 odn012）（新增）
├── subCode: String                  # 子货号/枝番（如 re=红色, wh=白色, bk=黑色）（新增）
├── name: String                     # 日文名称
├── nameZh: String                  # 中文名称
├── nameEn: String                  # 英文名称
├── colorName: String               # 颜色名称（日文：黒/白/赤）（新增）
├── material: String                 # 材质（新增）
├── productCategory: ProductCategory # 商品类型（新增）
├── heightCm: BigDecimal             # 高(cm)
├── widthCm: BigDecimal              # 宽(cm)
├── depthCm: BigDecimal              # 深(cm)
├── weightKg: BigDecimal            # 单个净重(kg)
├── unitsPerPackage: Integer         # 段ボール入数（每包数量）
├── packageHeightCm: BigDecimal
├── packageWidthCm: BigDecimal
├── packageDepthCm: BigDecimal
├── packageWeightKg: BigDecimal
├── remarks: String                  # 备注（箱规不固定 / 整托不固定）
├── warehouse: String               # 仓库（名古屋/久留米/永康）
├── requiresQc: Boolean             # 是否需要检测（新增）
├── updatedBy: String
├── updatedAt: LocalDateTime
│
├── 计算属性（只读）
│   ├── dimensionSum = heightCm + widthCm + depthCm
│   └── packageDimensions = packageHeightCm × packageWidthCm × packageDepthCm
│
└── 唯一键
    └── masterCode + subCode（复合唯一键）
```

**ProductCategory 枚举（新增）：**

```java
public enum ProductCategory {
    OEM,           // OEM定制产品（批量采购）
    ORDINARY,      // 普货
    FACTORY_DIRECT // 厂家出口
}
```

---

### 1.5 QcRecord（验收记录 — 聚合根）

> 对应业务流第三步。从内嵌值对象升级为独立聚合根，支持完整验收流程追踪。

```
QcRecord（聚合根）
├── id: Long
├── procurementId: Long              # 关联采购单ID
├── qcCode: String                  # 验收编号（业务流水号）（新增）
├── sellerName: String              # 卖家名称（新增）
├── qcUserId: Long                 # 验货负责人/开单人（新增）
├── result: QcResult                # PASS | FAIL
├── passedCount: Integer            # 合格数量
├── defectiveCount: Integer          # 不良数量
├── inspectionCount: Integer         # 检品数（新增）
├── boxCount: Integer               # 箱数（新增）
├── boxDimensions: BoxDimension      # 箱子尺寸（新增）
├── serialNumbers: List<String>     # 序列号列表（新增）
├── netWeightPerUnit: BigDecimal     # 单个净重（新增）
├── grossWeight: BigDecimal          # 毛重（新增）
├── taxInclusivePrice: BigDecimal   # 含税价（新增）
├── material: String                # 材质（新增）
├── taxRefund: Boolean              # 是否退税（新增）
├── qcStandard: String             # 验收标准（新增）
├── images: List<String>           # 缺陷照片URL列表
├── remarks: String                # 备注
├── qcDate: LocalDate               # 验货日期（新增）
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
└── 领域方法
    ├── pass(inspectionCount, passedCount)  # 验货通过
    ├── fail(reason)                         # 验货不通过，触发退货
    └── calculateDefectRate()                # 计算不良率 = defectiveCount / (passedCount + defectiveCount)
```

---

### 1.6 LogisticsPlan（调配计划 — 聚合根）

> 对应业务流第四步。整合货柜管理与拼柜逻辑。

```
LogisticsPlan（聚合根）
├── id: Long
├── procurementId: Long              # 关联采购单ID（可为空，拼柜时）
├── planType: LogisticsPlanType     # SEA(海运) | AIR(空运) | CONSOLIDATION(拼柜)
├── factoryId: Long                 # 关联工厂ID（新增）
├── productCode: String              # 货号（新增）
├── cargoSize: String               # 货物尺寸（新增）
├── cargoWeight: BigDecimal          # 货物重量（新增）
├── requiresQc: Boolean             # 货物是否需要检测（新增）
├── status: LogisticsStatus         # PLANNED | BOOKED | IN_TRANSIT | DELIVERED
├── containerId: Long               # 关联货柜ID（装柜后赋值）
├── poolId: Long                   # 关联拼柜池ID（拼柜后赋值）
├── estimatedShipDate: LocalDate
├── actualShipDate: LocalDate
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
└── 领域方法
    ├── bookContainer(type)         # 订舱
    ├── assignToPool(poolId)        # 划入拼柜池
    └── loadToContainer(containerId) # 装柜
```

**LogisticsPlanType 枚举（新增）：**

```java
public enum LogisticsPlanType {
    SEA,             // 海运
    AIR,             // 空运
    CONSOLIDATION    // 拼柜
}
```

**LogisticsStatus 枚举（新增）：**

```java
public enum LogisticsStatus {
    PLANNED,      // 调配计划已创建
    BOOKED,       // 舱位已预订
    IN_TRANSIT,   // 运输中
    DELIVERED     // 已送达
}
```

---

### 1.7 ConsolidationPool（拼柜池）

> 对应业务流第四步拼柜。多个出货单合并装柜的虚拟容器。

```
ConsolidationPool（聚合根）
├── id: Long
├── destinationPort: String          # 目的港
├── status: PoolStatus
├── containerId: Long               # 实际货柜（装柜后赋值）
├── containerThresholdCbm: BigDecimal
├── totalCbm: BigDecimal
├── totalBoxes: Integer
├── estimatedConsolidationDate: LocalDate
├── createdBy: String
├── createdAt: LocalDateTime
│
└── 领域方法
    ├── add(procurementId)           # 出货单加入拼柜池
    ├── remove(procurementId)        # 移出
    ├── calculateFillRate()           # 填充率 = totalCbm / thresholdCbm
    ├── isReady()                     # 填充率 ≥ 1 可触发装柜
    └── consolidate(containerId)      # 触发装柜 → 绑定 Container
```

---

## 2. 值对象

### BoxDimension（箱子尺寸 — 新增）

```
BoxDimension（值对象）
├── lengthCm: BigDecimal    # 长(cm)
├── widthCm: BigDecimal     # 宽(cm)
├── heightCm: BigDecimal    # 高(cm)
│
└── 计算属性
    └── volumeCbm = lengthCm × widthCm × heightCm / 1_000_000
```

### Container（货柜信息）

```
Container（值对象）
├── id: Long
├── containerNo: String              # 货柜编号
├── containerType: ContainerType     # 40HC / 20GP / 40GP / 45HC
├── sealNo: String                   # 封号
├── portOfLoading: String             # 起运港
├── portOfDestination: String         # 目的港
├── totalCbm: BigDecimal             # 总体积
├── totalWeightKg: BigDecimal         # 总重量（新增）
├── estimatedShipDate: LocalDate
├── actualShipDate: LocalDate
├── createdBy: String
├── createdAt: LocalDateTime
```

### FinanceRecord（财务结算）

```
FinanceRecord（值对象）
├── id: Long
├── procurementId: Long
├── taxType: TaxType               # 税务类型
├── totalCostRmb: BigDecimal        # 实际总成本（CNY）
├── actualPaidRmb: BigDecimal       # 实付金额（CNY）
├── currency: String                # CNY
├── remarks: String
└── settledAt: LocalDateTime
```

### ReturnRecord（退货记录）

```
ReturnRecord（值对象）
├── id: Long
├── procurementId: Long
├── reason: String                 # 退货原因
├── quantity: Integer             # 退货数量
├── refundAmount: BigDecimal       # 退款金额
├── 1688OrderId: String
└── returnDate: LocalDateTime
```

---

## 3. 枚举

### ShipmentStatus（出货单状态）

```java
public enum ShipmentStatus {
    未定,           // 还未下单（已转为 ReplenishmentDemand，此状态仅作历史兼容）
    予定,           // 预计发注
    OEM,            // OEM定制产品路径
    発注待,         // 已录入商品，等待下单
    永康,           // 1688下单后货物发往永康仓
    直送,           // 1688下单后厂家直接发货
    倉庫着,         // 货物到达仓库
    現地検品,       // 现场异地验货
    検品,           // 仓库验货
    エア便,         // 空运
    メーカー直送,   // 厂家直送
    輸出,           // 已出口
    国内通関,       // 国内报关（新增）
    通関,           // 日本报关
    日本着,         // 已到日本
    日本通関完了,   // 日本清关完成（新增）
    会計,           // 财务结算
    完了,           // 全流程结束（终态）
    退货;           // 退货

    // 永康路径：未定 → 発注待 → 永康 → 倉庫着 → 検品 → エア便/輸出 → 国内通関 → 通関 → 日本着 → 日本通関完了 → 会計 → 完了
    // OEM 路径：未定 → 発注待 → OEM → 倉庫着 → 現地検品 → メーカー直送 → 日本着 → 日本通関完了 → 会計 → 完了
    // 空运路径：未定 → 発注待 → 直送 → 倉庫着 → 検品 → エア便 → 国内通関 → 通関 → 日本着 → 日本通関完了 → 会計 → 完了
}
```

### BillingType（报关类型 — 新增，替换 billingMethod）

```java
public enum BillingType {
    ZHE_LU_KAI_PIAO,     // 浙鲁开票
    CHAO_HUI_TUI_SHUI,   // 超慧退税
    NO_REFUND,           // 不退税
    OTHER                // 其他
}
```

### QcType（验货方式）

```java
public enum QcType {
    ONSITE,   // 検品 — 仓库验货
    REMOTE    // 現地検品 — 现场异地验货
}
```

### QcResult（验货结果）

```java
public enum QcResult {
    PASS,     // 验货通过
    FAIL      // 验货不通过（触发退货）
}
```

### ContainerType（柜型）

```java
public enum ContainerType {
    GP20(33, 67.7),
    GP40(37, 67.7),
    HC40(39, 67.7),
    HC45(42, 67.3);

    private final double tareKg;
    private final double payloadM3;
    public double maxCbm() { return payloadM3; }
}
```

### TaxType（财务类型）

```java
public enum TaxType {
    EXPORT_REFUND,    // 出口退税
    NO_REFUND         // 不退税
}
```

### PoolStatus（拼柜池状态）

```java
public enum PoolStatus {
    POOL_PENDING,        // 待拼箱
    POOL_READY,          // 可安排（已凑满一柜）
    CONTAINER_PLANNED,    // 货柜计划已生成
    LOADED               // 已装柜
}
```

---

## 4. 新增聚合根（待定字段）

### DomesticCustomsRecord（国内报关 — 新增，字段待定）

```
DomesticCustomsRecord（聚合根）
├── id: Long
├── procurementId: Long
├── status: DomesticCustomsStatus   # PENDING | SUBMITTED | CLEARED | FAILED
├── customsDeclarationNo: String     # 报关单号（待定）
├── declarationDate: LocalDate      # 申报日期（待定）
├── declarer: String                # 申报人（待定）
├── remarks: String
├── createdBy: String
├── createdAt: LocalDateTime
└── updatedAt: LocalDateTime

（其他字段待业务确认后补充）
```

### JapanCustomsRecord（日本清关 — 新增，字段待定）

```
JapanCustomsRecord（聚合根）
├── id: Long
├── procurementId: Long
├── status: JapanCustomsStatus      # PENDING | IN_PROGRESS | CLEARED | FAILED
├── customsEntryNo: String          # 入境报关号（待定）
├── arrivalDate: LocalDate          # 到达日期（待定）
├── customsBroker: String            # 清关行（待定）
├── brokerPhone: String             # 清关行电话（待定）
├── remarks: String
├── createdBy: String
├── createdAt: LocalDateTime
└── updatedAt: LocalDateTime

（其他字段待业务确认后补充）
```

---

## 5. 仓储接口

### ReplenishmentDemandRepository

```java
public interface ReplenishmentDemandRepository extends JpaRepository<ReplenishmentDemand, Long> {
    List<ReplenishmentDemand> findByStatus(DemandStatus status);
    List<ReplenishmentDemand> findByDemandType(DemandType type);
    Optional<ReplenishmentDemand> findByLinkedProcurementId(Long procurementId);
}
```

### FactoryRepository

```java
public interface FactoryRepository extends JpaRepository<Factory, Long> {
    Optional<Factory> findByFactoryName(String factoryName);
    List<Factory> findByStatus(FactoryStatus status);
}
```

### ProcurementRepository

```java
public interface ProcurementRepository extends JpaRepository<Procurement, Long> {
    Page<Procurement> findByStatus(ShipmentStatus status, Pageable pageable);
    Page<Procurement> findByProductCode(String productCode, Pageable pageable);
    Page<Procurement> findByFactoryId(Long factoryId, Pageable pageable);
    List<Procurement> findByIdIn(List<Long> ids);
}
```

### ProductRepository

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByMasterCodeAndSubCode(String masterCode, String subCode);
    List<Product> findByMasterCode(String masterCode);           // 查所有颜色变体
    List<Product> findByWarehouse(String warehouse);
    List<Product> findByProductCategory(ProductCategory category);
    List<Product> findByRequiresQc(Boolean requiresQc);
}
```

### QcRecordRepository

```java
public interface QcRecordRepository extends JpaRepository<QcRecord, Long> {
    Optional<QcRecord> findByProcurementId(Long procurementId);
    List<QcRecord> findByResult(QcResult result);
}
```

### LogisticsPlanRepository

```java
public interface LogisticsPlanRepository extends JpaRepository<LogisticsPlan, Long> {
    List<LogisticsPlan> findByStatus(LogisticsStatus status);
    List<LogisticsPlan> findByProcurementId(Long procurementId);
    List<LogisticsPlan> findByPlanType(LogisticsPlanType type);
}
```

### ConsolidationPoolRepository

```java
public interface ConsolidationPoolRepository extends JpaRepository<ConsolidationPool, Long> {
    List<ConsolidationPool> findByDestinationPortAndStatus(String port, PoolStatus status);
    Optional<ConsolidationPool> findByStatus(PoolStatus ready);
}
```

---

## 6. 领域服务

### ProcurementDomainService

```java
@Service
public class ProcurementDomainService {

    public void validateTransition(ShipmentStatus current, ShipmentStatus next) {
        if (current == 完了) {
            throw new BusinessException("business.cannot_modify_closed");
        }
    }

    public void onQcPassed(Procurement order, QcType type) {
        if (type == REMOTE) {
            order.moveTo(メーカー直送);
        } else {
            order.moveTo(推荐空运(order) ? エア便 : 輸出);
        }
    }

    public boolean 推荐空运(Procurement order) {
        // 读取 Product 尺寸信息，判定是否达标
    }
}
```

### ReplenishmentDemandDomainService

```java
@Service
public class ReplenishmentDemandDomainService {

    public Procurement convertToProcurement(ReplenishmentDemand demand, ProcurementCommand command) {
        if (demand.getStatus() != PENDING) {
            throw new BusinessException("demand.already_processed");
        }
        demand.convertToProcurement();
        // 生成 Procurement 记录，填充第二步下单所需字段
    }
}
```

### PriceCalculationService

```java
@Service
public class PriceCalculationService {

    /**
     * 批发价 JPY = (人民币单价 ÷ 票点 × 1.02 × 1.2) × 汇率 × 1.05
     */
    public BigDecimal calculateEstimatedPriceJpy(
            BigDecimal priceRmb,
            BigDecimal taxPoint,
            BigDecimal exchangeRate) {
        return priceRmb
            .divide(taxPoint, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("1.02"))
            .multiply(new BigDecimal("1.2"))
            .multiply(exchangeRate)
            .multiply(new BigDecimal("1.05"));
    }
}
```

---

## 7. 业务规则

| 规则 | 说明 |
|------|------|
| 完了终态 | 状态为 `完了` 后，禁止任何状态变更 |
| 退货独立 | 退货记录与原出货单独立处理，不影响原单状态 |
| 需求先于采购 | 补货/新品采购必须通过 ReplenishmentDemand 录入，再转为 Procurement |
| 主/子货号 | Product 唯一键为 masterCode + subCode，主货号下可有多条颜色变体 |
| 工厂引用 | Procurement 引用 FactoryId，不内联工厂信息 |
| 验收独立 | QcRecord 为聚合根，可独立查询和追踪 |
| 调配绑定 | LogisticsPlan 关联 Procurement 和 Factory，承载第四步调配信息 |
| 空运推荐 | 尺寸+重量达标自动推荐走 エア便 路径 |
| 报关前置 | 国内通関必须在日本通関之前完成 |
| 报价计算 | `(priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05` |

---

## 8. 实体关系图（v1.3.0）

```
ReplenishmentDemand
├── status: DemandStatus
└── linkedProcurementId → Procurement

Factory
├── id
└── 被 Procurement 引用（factoryId）

Procurement
├── factoryId → Factory
├── productCode → Product.masterCode
├── subProductCode → Product.subCode
├── qcRecordId → QcRecord
├── logisticsPlanId → LogisticsPlan
├── containerId → Container
└── financeId → FinanceRecord

Product
├── masterCode + subCode（复合唯一键）
├── productCategory: ProductCategory
└── warehouse → 仓库归属

QcRecord
└── procurementId → Procurement

LogisticsPlan
├── procurementId → Procurement
├── factoryId → Factory
├── containerId → Container
└── poolId → ConsolidationPool

ConsolidationPool
└── containerId → Container

DomesticCustomsRecord
└── procurementId → Procurement

JapanCustomsRecord
└── procurementId → Procurement
```

---

## 9. 待确认事项（字段 TBD）

以下字段等待业务方确认后补充至对应实体：

| 步骤 | 实体 | 待确认字段 |
|------|------|-----------|
| 第五步 国内报关 | `DomesticCustomsRecord` | 报关单号、HS编码、申报人、货物价值、出口口岸等全部字段 |
| 第六步 日本清关 | `JapanCustomsRecord` | 入境报关号、到港日期、清关行、清关费用等全部字段 |
| 第三步 验收 | `QcRecord` | 验收标准的具体判定规则（是否拍照留存等） |
| 第四步 调配 | `LogisticsPlan` | 海运/空运的具体航线选项 |
