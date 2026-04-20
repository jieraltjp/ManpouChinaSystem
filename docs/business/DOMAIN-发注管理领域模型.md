# 发注管理 — 领域模型

> **版本**: 1.1.0
> **更新**: 2026-04-20
> **依据**: `docs/发注管理体系升级.pdf` + `docs/新発注管理-設計図.xlsx`

---

## 1. 聚合根

### 1.1 ShippingOrder（出货单 — 核心聚合根）

> 对应 Excel 出货单弹窗。一次发注 = 一条出货单记录。

```
ShippingOrder（聚合根）
├── id: Long
├── skuNumber: String          # 货号（关联 Product.productCode）
├── quantity: Integer         # 订购数量
├── priceRmb: BigDecimal      # 人民币单价
├── exchangeRate: BigDecimal  # CNY→JPY 汇率
├── taxPoint: BigDecimal      # 票点（默认 1.1）
├── billingMethod: String     # 计费方式（METHOD_A 等）
├── orderDate: LocalDate      # 下单日（1688下单日期）
├── factoryShipDate: LocalDate # 厂家出货日
├── plannedShipDate: LocalDate # 计划出货日
├── productLead: String        # 商品担当
├── japanLead: String         # 日本担当
├── chinaLead: String         # 中国担当
├── destination: String       # 发送目的地
├── customerCompany: String   # 客户公司
├── status: ShipmentStatus    # 状态（见 枚举章节）
├── qcType: QcType           # 验货方式（nullable）
├── qcResult: QcResult       # 验货结果（nullable）
├── containerId: Long         # 货柜ID（nullable，发货后赋值）
├── financeId: Long           # 财务结算ID（nullable，结算后赋值）
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
│
├── 计算属性（只读，非持久化）
│   └── estimatedPriceJpy = (priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05
│
└── 领域方法（状态推进）
    ├── moveTo(下一状态)   — 状态机规则校验
    ├── submitQc(type)     — 提交验货
    ├── approveQc()        — 验货通过
    ├── rejectQc()         — 验货不通过（触发退货路径）
    ├── loadToContainer()  — 装柜
    ├── settle()            — 财务结算
    └── close()            — 终态（完了(済)后禁止任何变更）
```

### 1.2 Product（商品目录 — 聚合根）

> 对应 Excel 商品管理表。维护商品代码、尺寸、重量、包装规格、仓库归属。

```
Product（聚合根）
├── id: Long
├── productCode: String        # 商品代码（唯一键，如 de077）
├── name: String              # 日文名称
├── heightCm: BigDecimal      # 高(cm)
├── widthCm: BigDecimal       # 宽(cm)
├── depthCm: BigDecimal       # 深(cm)
├── weightKg: BigDecimal      # 重量(kg)
├── unitsPerPackage: Integer  # 段ボール入数（每包数量）
├── packageHeightCm: BigDecimal
├── packageWidthCm: BigDecimal
├── packageDepthCm: BigDecimal
├── packageWeightKg: BigDecimal
├── remarks: String          # 备注（箱规不固定 / 整托不固定）
├── warehouse: String        # 仓库（名古屋/久留米/永康）
├── updatedBy: String
├── updatedAt: LocalDateTime
│
└── 计算属性（只读）
    ├── dimensionSum = heightCm + widthCm + depthCm
    └── packageDimensions = packageHeightCm × packageWidthCm × packageDepthCm
```

---

## 2. 值对象

### Container（货柜信息）

> 对应 Excel「货柜编号」区域。

```
Container（值对象）
├── id: Long
├── containerNo: String      # 货柜编号（箱号）
├── containerType: ContainerType  # 40HC / 20GP / 40GP / 45HC
├── sealNo: String           # 封号
├── portOfLoading: String     # 起运港（如 YANTIAN）
├── portOfDestination: String # 目的港（如 NAGOYA）
├── procurementIds: List<Long> # 关联的出货单ID列表
├── totalCbm: BigDecimal     # 总体积
├── estimatedShipDate: LocalDate
├── createdBy: String
├── createdAt: LocalDateTime
```

### QcRecord（验货记录）

> 对应 Excel「検品」和「現地検品」功能。

```
QcRecord（值对象）
├── id: Long
├── procurementId: Long
├── qcType: QcType           # ONSITE=検品 / REMOTE=現地検品
├── qcUserId: Long           # 验货人
├── result: QcResult         # PASS / FAIL
├── passedCount: Integer     # 合格数量
├── defectiveCount: Integer  # 不良数量
├── images: List<String>    # 缺陷照片URL列表
├── remarks: String         # 备注（如：外箱轻微破损）
└── qcDate: LocalDateTime
```

### FinanceRecord（财务结算）

> 对应 Excel「会計」功能。

```
FinanceRecord（值对象）
├── id: Long
├── procurementId: Long
├── taxType: TaxType         # EXPORT_REFUND 等
├── totalCostRmb: BigDecimal # 实际总成本（CNY）
├── actualPaidRmb: BigDecimal # 实付金额（CNY）
├── currency: String         # CNY
├── remarks: String
└── settledAt: LocalDateTime
```

### ReturnRecord（退货记录）

> 对应 Excel「返品」功能。

```
ReturnRecord（值对象）
├── id: Long
├── procurementId: Long
├── reason: String           # 退货原因（如：不良品）
├── quantity: Integer       # 退货数量
├── refundAmount: BigDecimal # 退款金额
├── 1688OrderId: String     # 关联的1688订单ID
└── returnDate: LocalDateTime
```

---

## 3. 枚举

### ShipmentStatus（出货单状态）

> 对应 Excel basic status 流转图。终态为 `完了(済)`，此后禁止任何变更。

```java
public enum ShipmentStatus {
    未定,       // 还未下单，仅记录需求
    予定,       // 预计发注
    OEM,        // OEM 定制产品路径
    発注待,     // 已录入商品，等待下单
    永康,       // 1688下单后货物发往永康仓
    直送,       // 1688下单后厂家直接发货（不经永康仓）
    倉庫着,     // 货物到达仓库
    現地検品,   // 现场异地验货
    検品,       // 仓库验货
    エア便,     // 空运（尺寸/重量达标的轻量货）
    メーカー直送, // 厂家直送
    輸出,       // 已出口
    通関,       // 已报关
    日本着,     // 已到日本
    会計,       // 财务结算
    完了,       // 全流程结束（终态 — 禁止任何变更）
    退货;       // 退货（独立处理，不影响原单状态）

    // 永康路径：未定 → 発注待 → 永康 → 倉庫着 → 検品 → エア便 → 輸出 → 通関 → 日本着 → 会計 → 完了
    // OEM 路径：未定 → 発注待 → OEM → 倉庫着 → 現地検品 → メーカー直送 → 完了
    // 终态校验：status == 完了 时，所有状态推进方法抛出 BusinessException
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
    GP20(33, 67.7),   // 20GP: 自重(TARE) / 最大载重(PAYLOAD)
    GP40(37, 67.7),   // 40GP
    HC40(39, 67.7),   // 40HC 高箱
    HC45(42, 67.3);   // 45HC

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

---

## 4. 拼柜池聚合

### ConsolidationPool（拼柜池）

> 对应 Excel「货物发送整理」。多个出货单合并装柜的虚拟容器。

```
ConsolidationPool（聚合根）
├── id: Long
├── destinationPort: String       # 目的港
├── status: PoolStatus
├── containerId: Long             # 实际货柜（nullable，装柜后赋值）
├── containerThresholdCbm: BigDecimal
├── totalCbm: BigDecimal
├── totalBoxes: Integer
├── estimatedConsolidationDate: LocalDate
├── createdBy: String
├── createdAt: LocalDateTime
│
└── 领域方法
    ├── add(ShippingOrder)         // 出货单加入拼柜池
    ├── remove(ShippingOrder)      // 移出
    ├── calculateFillRate()        // 填充率 = totalCbm / thresholdCbm
    ├── isReady()                  // 填充率 ≥ 1 可触发装柜
    └── consolidate()              // 触发装柜 → 绑定 Container
```

### PoolStatus

```java
public enum PoolStatus {
    POOL_PENDING,      // 待拼箱
    POOL_READY,         // 可安排（已凑满一柜）
    CONTAINER_PLANNED,  // 货柜计划已生成
    LOADED              // 已装柜
}
```

---

## 5. 仓储接口

### ShippingOrderRepository

```java
public interface ShippingOrderRepository extends JpaRepository<ShippingOrder, Long> {

    Page<ShippingOrder> findByStatus(ShipmentStatus status, Pageable pageable);
    Page<ShippingOrder> findBySkuNumber(String skuNumber, Pageable pageable);
    Page<ShippingOrder> findByCustomerCompany(String customerCompany, Pageable pageable);
    List<ShippingOrder> findByIdIn(List<Long> ids);
}
```

### ProductRepository

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductCode(String productCode);
    List<Product> findByWarehouse(String warehouse);
    List<Product> findByRemarksContaining(String keyword);
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

### ShippingOrderDomainService

**职责**：状态转换规则校验 + 触发副作用

```java
@Service
public class ShippingOrderDomainService {

    // 永康路径状态推进
    public void validateTransition(ShipmentStatus current, ShipmentStatus next) {
        // 完了(済) 后禁止任何状态变更
        if (current == 完了) {
            throw new BusinessException("business.cannot_modify_closed");
        }
        // 校验允许的下一状态...
    }

    // 验货完成后路由
    public void onQcPassed(ShippingOrder order, QcType type) {
        if (type == REMOTE) {
            order.moveTo(メーカー直送);
        } else {
            // 体积/重量判定 → エア便 或 輸出
            order.moveTo(推荐空运(order) ? エア便 : 輸出);
        }
    }

    // 空运推荐判定（尺寸+重量达标）
    public boolean 推荐空运(ShippingOrder order) {
        // 读取 Product 尺寸信息，判定是否达标
    }
}
```

### PriceCalculationService

**职责**：批发价 JPY 计算

```java
@Service
public class PriceCalculationService {

    /**
     * 批发价 JPY = (人民币单价 ÷ 票点 × 1.02 × 1.2) × 汇率 × 1.05
     *
     * 参数说明：
     * - taxPoint：票点（默认 1.1，即含10%增值税）
     * - 1.02：中国国内流通费率
     * - 1.2：利润率系数
     * - 1.05：跨境费用系数
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

## 7. 关键业务规则

| 规则 | 说明 |
|------|------|
| 完了终态 | 状态为 `完了` 后，禁止任何状态变更（抛出 BusinessException） |
| 退货独立 | 退货记录与原出货单独立处理，不影响原单状态 |
| 空运推荐 | 尺寸+重量达标自动推荐走 エア便 路径 |
| 永康路径 | 1688下单 → 永康仓 → 倉庫着 → 検品 → 発送路径 |
| 厂家直送 | 紧急/大批量货不经仓库，厂家直送 |
| OEM路径 | 新规 → OEM → 倉庫着 → 現地検品 → メーカー直送 → 完了 |
| 报价计算 | `(priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05` |
| 最低必要条件 | 商品代码、担当（馬さん/張雲さん确认项）必须填写才能推进至 発注待 |
