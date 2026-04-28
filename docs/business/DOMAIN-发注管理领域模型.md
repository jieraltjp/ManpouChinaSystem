# 发注管理 — 领域模型

> **版本**: 1.6.0
> **更新**: 2026-04-28（DemandStatus 新增 CONFIRMED）
> **依据**: 业务流分析（8步全链路） + `SPEC-B02-发注单-步骤2.md`

> **代码实现进度**:
> Procurement ✅ 已实现（v1.3.0） · Product ✅ 已实现（完整字段，含 hsCode/taxPoint/多语言名称） · ReplenishmentDemand ✅
> · Factory ✅（v1.4.0，详见 DB-10-factory.md） · QcRecord ✅ · LogisticsPlan ✅ · DomesticCustoms 🔴 待定 · JapanCustoms 🔴 待定

---

## 变更记录（v1.2.0 → v1.3.0）

| 变更类型 | 实体 | 说明 |
|----------|------|------|
| **新增** | `ReplenishmentDemand` | 补货需求单聚合根，解决"未定"状态双重语义问题 |
| **新增** | `Factory` | 工厂聚合根（无独立页面，内嵌于发注单），解决工厂信息内联在 Procurement 的问题 |
| **扩展** | `Product` | 新增 masterCode/subCode（主/子货号）+ colorName + material + productCategory（待开发） |
| **扩展** | `Procurement` | v1.3.0 新增 factoryId + subProductCode + material + requiresQc + billingType + customsRemarks + instructionManual + actualShipDate |
| **升级** | `QcRecord` | 从值对象升级为独立聚合根，补全箱数/尺寸/序列号/净重/毛重/含税价/验收标准 |
| **新增** | `BillingType` | 替换 billingMethod，定义浙鲁开票/超慧退税/不退税/其他枚举 |
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
├── status: DemandStatus            # PENDING(待确认) | CONFIRMED(已确认) | CONVERTED(已转采购) | CANCELLED(取消)
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
├── factoryCode: String             # 工厂编号（F-YYYYMMDD-NNN）
├── factoryName: String             # 工厂名称
├── category: FactoryCategory        # 分类（TOOLS/TEXTILE/PLASTIC/...）
├── province: String                # 省
├── city: String                    # 市
├── county: String                  # 县/区
├── roughLocation: String           # 详细地址（粗略）
├── longitude: BigDecimal           # 经度
├── latitude: BigDecimal            # 纬度
├── contactName: String             # 联系人
├── contactPhone: String             # 手机号
├── contactWechat: String           # 微信号
├── contactQq: String              # QQ号
├── cooperationStatus: CooperationStatus  # 合作状态
├── paymentTerms: PaymentTerms      # 账期
├── notes: String                   # 备注
├── createBy: String
├── createTime: LocalDateTime
├── updateTime: LocalDateTime
│
└── 领域方法
    └── linkProductCode(code)       # 关联货号
```

**CooperationStatus 枚举：**

```java
public enum CooperationStatus {
    ACTIVE,     // 合作中
    SUSPENDED,  // 已暂停
    ELIMINATED, // 已淘汰
    POTENTIAL   // 潜在合作
}
```

**FactoryCategory 枚举：**

```java
public enum FactoryCategory {
    TOOLS, TEXTILE, PLASTIC, ELECTRONICS,
    FURNITURE, AUTO_PARTS, SPORTS, PET,
    MEDICAL, CRAFTS, CHEMICAL, OTHER
}
```

**PaymentTerms 枚举：**

```java
public enum PaymentTerms {
    CASH,   // 现结
    NET_30, // 月结30天
    NET_60, // 月结60天
    NET_90, // 月结90天
    CREDIT  // 信用账期
}
```

---

### 1.3 Procurement（出货单 — 核心聚合根）

> 对应业务流第二步下单 + 第三步验收前段。一次发注 = 一条出货单记录。

```
Procurement（聚合根）
├── id: Long
├── factoryId: Long                  # 关联工厂ID（v1.3.0 新增）
├── productCode: String              # 主货号
├── subProductCode: String           # 子货号/枝番（颜色，如 odn012-re）（v1.3.0 新增）
├── material: String                 # 材质（v1.3.0 新增）
├── requiresQc: Boolean              # 是否需要检测（v1.3.0 新增）
├── quantity: Integer                # 订购数量
├── priceRmb: BigDecimal             # 人民币单价
├── exchangeRate: BigDecimal         # CNY→JPY 汇率
├── taxPoint: BigDecimal             # 票点（默认 1.1）
├── billingType: BillingType         # 报关类型（v1.3.0 替换 billingMethod）
├── estimatedPriceJpy: BigDecimal    # 估算批发价 JPY（前端计算存储）
├── customsRemarks: String           # 报关备注（v1.3.0 新增）
├── instructionManual: String         # 说明书（v1.3.0 新增）
├── orderDate: LocalDate             # 下单日
├── factoryShipDate: LocalDate        # 厂家出货日
├── plannedShipDate: LocalDate       # 预计出货日（交货期）
├── actualShipDate: LocalDate        # 实际出货日（v1.3.0 新增）
├── productLead: String              # 商品担当
├── japanLead: String                # 日本担当
├── chinaLead: String                # 中国担当
├── destination: String               # 发送目的地
├── customerCompany: String           # 客户公司
├── status: ShipmentStatus            # 状态
├── createBy: String
├── createTime: LocalDateTime
├── updateTime: LocalDateTime
│
└── 领域方法（状态推进）
    └── moveTo(status)               # 推进状态（由操作员在页面触发，完了为终态）
```

---

### 1.4 Product（商品目录 — 聚合根）

> 对应业务流所有步骤的商品基础信息。支持主/子货号结构（颜色变体）。

```
Product（聚合根）
├── id: Long
├── masterCode: String               # 主货号（如 odn012）
├── subCode: String                 # 子货号/色号（如 re=红色，可为空）
├── nameJa: String                  # 日文名称（日本用）
├── nameEn: String                 # 英文名称（报关用）
├── nameZh: String                 # 中文名称（中国用）
├── imageUrl: String               # 商品图片 URL
├── colorName: String              # 颜色名称
├── material: String               # 材质
├── category: ProductCategory      # OEM / ORDINARY / FACTORY_DIRECT
├── origin: String                 # 原产国
├── unit: String                   # 计量单位（个/台/套）
├── lengthCm: BigDecimal          # 单品长(cm)
├── widthCm: BigDecimal           # 单品宽(cm)
├── heightCm: BigDecimal           # 单品高(cm)
├── volumeCbm: BigDecimal          # 单品体积(m³)（自动计算）
├── netWeightKg: BigDecimal        # 净重(kg)
├── grossWeightKg: BigDecimal      # 毛重(kg)
├── unitPriceRmb: BigDecimal       # 含税单价(CNY)
├── taxPoint: BigDecimal          # 票点（默认 1.1）
├── taxRate: BigDecimal           # 增值税率（默认 0.1）
├── hsCode: String                # HS编码（8-10位）
├── declarationElements: String    # 申报要素
├── unitsPerPackage: Integer      # 段ボール入数（每箱数量）
├── packageLengthCm: BigDecimal  # 外箱长(cm)
├── packageWidthCm: BigDecimal    # 外箱宽(cm)
├── packageHeightCm: BigDecimal   # 外箱高(cm)
├── packageVolumeCbm: BigDecimal  # 外箱体积(m³)
├── packageWeightKg: BigDecimal   # 外箱毛重(kg)
├── warehouse: String             # 仓库归属
├── requiresQc: Boolean          # 是否需要检测
├── remarks: String               # 备注
├── lastUsedDate: LocalDate      # 最近使用日期
├── createBy / createTime / updateTime
│
└── 领域方法
    ├── calculateVolume()              # 计算单品体积
    ├── calculatePackageVolume()       # 计算外箱体积
    └── getFullCode()                 # 返回 masterCode-subCode 完整货号
```

**唯一键**: `(masterCode, subCode)` — 复合唯一

**ProductCategory 枚举：**

```java
public enum ProductCategory {
    OEM,           // OEM定制产品（批量采购）
    ORDINARY,      // 普货
    FACTORY_DIRECT // 厂家出口
}
```

### 1.5 ProductFactory（商品-工厂关联 — 关联实体）

> 多对多关系，记录每个工厂生产的商品及其特定属性（供应商货号/MOQ/单价）。

```
ProductFactory（关联实体）
├── id: Long
├── productId: Long              # FK → product.id
├── factoryId: Long              # FK → factory.id
├── supplierSku: String          # 供应商内部货号
├── moq: Integer               # 最小起订量
├── leadTimeDays: Integer       # 交货周期(天)
├── unitPriceRmb: BigDecimal   # 该工厂的含税单价
├── isPreferred: Boolean        # 是否首选供应商
├── createTime / updateTime
│
└── 唯一键
    └── (productId, factoryId)
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
├── boxLengthCm: BigDecimal           # 箱子长(cm)
├── boxWidthCm: BigDecimal            # 箱子宽(cm)
├── boxHeightCm: BigDecimal           # 箱子高(cm)
├── netWeightPerUnit: BigDecimal     # 单个净重（新增）
├── grossWeight: BigDecimal          # 毛重（新增）
├── taxInclusivePrice: BigDecimal   # 含税价（新增）
├── material: String                # 材质（新增）
├── taxRefund: Boolean              # 是否退税（新增）
├── qcStandard: String             # 验收标准（新增）
├── images: String                # 缺陷照片URL列表（JSON数组）
├── remarks: String                # 备注
├── qcDate: LocalDate               # 验货日期（新增）
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
└── 领域方法
    ├── calculateDefectiveCount()  # 不良数 = inspectionCount - passedCount
    ├── pass()                      # 验货通过
    └── fail(reason)                 # 验货不通过，触发退货
```

---

### 1.6 LogisticsPlan（调配计划 — 聚合根）

> 对应业务流第四步。整合货柜管理与拼柜逻辑。

```
LogisticsPlan（聚合根）
├── id: Long
├── planCode: String                 # 流水号（如 L-20260421-001）
├── procurementId: Long              # 关联采购单ID（可为空，拼柜时）
├── factoryId: Long                 # 关联工厂ID
├── productCode: String              # 货号
├── subProductCode: String           # 子货号/颜色
├── planType: PlanType             # SEA(海运) | AIR(空运) | CONSOLIDATION(拼柜)
├── status: LogisticsStatus         # PLANNED | BOOKED | IN_TRANSIT | DELIVERED
├── cargoLengthCm: BigDecimal        # 长(cm)
├── cargoWidthCm: BigDecimal         # 宽(cm)
├── cargoHeightCm: BigDecimal        # 高(cm)
├── cargoVolumeCbm: BigDecimal       # 体积(m³)
├── cargoWeightKg: BigDecimal        # 重量(kg)
├── quantity: Integer               # 数量
├── requiresQc: Boolean             # 是否需要检测
├── containerId: Long               # 货柜ID（装柜后赋值）
├── poolId: Long                   # 拼柜池ID（拼柜后赋值）
├── estimatedShipDate: LocalDate
├── actualShipDate: LocalDate
├── remarks: String
├── createBy: String
├── createTime: LocalDateTime
├── updateTime: LocalDateTime
│
└── 领域方法
    ├── calculateVolume()           # 计算体积 = 长×宽×高 / 1_000_000
    ├── updateStatus(newStatus)     # 推进状态（终态 DELIVERED 后禁止变更）
    └── isTerminal()                 # 检查是否为终态
```

**PlanType 枚举：**

```java
public enum PlanType {
    SEA,             // 海运
    AIR,             // 空运
    CONSOLIDATION    // 拼柜
}
```

**LogisticsStatus 枚举：**

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
public interface FactoryRepository {
    Optional<Factory> findById(Long id);
    Optional<Factory> findByIdAndDeletedIsFalse(Long id);
    Optional<Factory> findByFactoryNameAndDeletedIsFalse(String factoryName);
    Factory save(Factory entity);
    void deleteById(Long id);
    List<Factory> findAllByDeletedIsFalse();
    Page<Factory> findAllByDeletedIsFalse(Pageable pageable);
    Page<Factory> findByCooperationStatusAndDeletedIsFalse(CooperationStatus status, Pageable pageable);
    Page<Factory> findByFactoryNameAndDeletedIsFalse(String factoryName, Pageable pageable);
    boolean existsByDeletedIsFalse();
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
public interface ProductRepository {
    Optional<Product> findById(Long id);
    Optional<Product> findByIdAndDeletedIsFalse(Long id);
    Optional<Product> findByMasterCodeAndDeletedIsFalse(String masterCode);
    Optional<Product> findByMasterCodeAndSubCodeAndDeletedIsFalse(String masterCode, String subCode);
    Product save(Product entity);
    void deleteById(Long id);
    List<Product> findAllByDeletedIsFalse();
    Page<Product> findAllByDeletedIsFalse(Pageable pageable);
    Page<Product> findByMasterCodeAndDeletedIsFalse(String masterCode, Pageable pageable);
    Page<Product> findByNameZhContainingAndDeletedIsFalse(String keyword, Pageable pageable);
    Page<Product> findByHsCodeAndDeletedIsFalse(String hsCode, Pageable pageable);
}

> **实现**: `ProductJpaRepository`（`infrastructure/persistence/jpa/`）同时继承本接口和 `JpaRepository<Product, Long>`。

### ProductFactoryRepository

```java
public interface ProductFactoryRepository {
    List<ProductFactory> findByProductIdAndDeletedIsFalse(Long productId);
    List<ProductFactory> findByFactoryIdAndDeletedIsFalse(Long factoryId);
    Optional<ProductFactory> findByProductIdAndFactoryId(Long productId, Long factoryId);
}

### ProductFactoryRepository

```java
public interface ProductFactoryRepository {
    List<ProductFactory> findByProductIdAndDeletedIsFalse(Long productId);
    List<ProductFactory> findByFactoryIdAndDeletedIsFalse(Long factoryId);
    Optional<ProductFactory> findByProductIdAndFactoryId(Long productId, Long factoryId);
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
    List<LogisticsPlan> findByPlanType(PlanType type);
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
├── status: ShipmentStatus           # 19种状态，含完了终态
└── 验货/调配/货柜/财务 → QcRecord / LogisticsPlan / Container / FinanceRecord（业务关联，非外键约束）

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
