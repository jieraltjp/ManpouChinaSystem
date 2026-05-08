# SPEC-B02 — 发注单业务规格（步骤2）

> **版本**: 1.12.0
> **更新**: 2026-05-08（v1.12.0：Procurement.linkedDemandId/linkedDemandItemId标记⚠️——Entity不存在，N:1关联改为DemandProcurementMapping表实现）
> **更新**: 2026-05-07（v1.11.0：ReplenishmentDemand修正为v2.2.0模型；移除/convert改/link；Factory.linkProductCode注⚠️）
> **更新**: 2026-04-24（v1.6.0：补货需求转采购改为批量模式；一个 ReplenishmentDemand 批量生成多条 Procurement）
> **更新**: 2026-04-23（补充元数据字段）
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **业务步号**: 02（发注单+工厂管理）
> **对应 UI 文档**: `docs/ui/pages/02-procurement.md`
> **对应数据库文档**: `docs/database/DB-02-procurement-order.md`

---

## 1. 业务背景

发注单（Procurement）是采购流程的核心实体。以补货需求（ReplenishmentDemand）为入口，录入工厂信息、商品信息、价格信息后完成下单。工厂（Factory）无独立页面，完全内嵌于发注单表单中。

**流程**：ReplenishmentDemand → Procurement → 下单

---

## 2. 聚合根

### 2.1 ReplenishmentDemand（补货需求单）

> ⚠️ **v2.2.0 重大变更**：字段与 v1.x 完全重构。以下为 v2.2.0 最新结构。
> **入口**：步骤1已创建，此处引用。（v2.2.0：批量转采购改为通过 DemandProcurementMapping 关联）

```
ReplenishmentDemand（聚合根）— v2.2.0
├── id: Long
├── demandCode: String           # D-YYYYMMDD-NNN
├── demandType: DemandType        # REPLENISHMENT / NEW_PURCHASE
├── productCode: String           # 主货号
├── subProductCode: String        # 子货号/枝番
├── quantity: Integer             # 数量
├── destination: String          # 目的地
├── japanLead: String            # 日本担当
├── status: DemandStatus ⚠️      # ⚠️ v2.2.0: PENDING → CONFIRMED（原CONVERTED/CANCELLED已移除）
├── linkedProcurementId: Long    # 关联采购单ID（CONFIRMED时赋值）
├── imageUrl: String             # 商品图片
├── remarks: String
└── 领域方法
    ├── markAsLinked(procurementId) ⚠️  # ⚠️ v2.2.0替代convertToProcurement()
    └── unlinkProcurement() ⚠️          # ⚠️ v2.2.0替代cancel()
```

### 2.2 Factory（工厂）

> **内嵌**：无独立页面，工厂选择器内嵌于发注单表单，支持新建/编辑。
>
> **v1.8.0 变更**：移除了 `FactorySynergyPort`（跨模块强耦合），`cooperation_status` 以 DB 值为准，
> 由管理员在工厂管理页面手动维护 ACTIVE/POTENTIAL/SUSPENDED/ELIMINATED 四态。

```
Factory（聚合根）
├── id: Long
├── factoryCode: String         # F-YYYYMMDD-NNN
├── factoryName: String         # 工厂名称
├── category: FactoryCategory    # 分类（TOOLS/TEXTILE/PLASTIC/...）
├── province: String             # 省
├── city: String                 # 市
├── county: String              # 县/区
├── roughLocation: String       # 详细地址（粗略）
├── longitude: BigDecimal       # 经度
├── latitude: BigDecimal        # 纬度
├── contactName: String         # 联系人
├── contactPhone: String        # 手机号
├── contactWechat: String       # 微信号
├── contactQq: String          # QQ号
├── cooperationStatus: CooperationStatus  # 合作状态（DB直传，管理员手动维护）
├── paymentTerms: PaymentTerms  # 账期
├── notes: String              # 备注
└── 领域方法
    └── linkProductCode(code) ⚠️  # ⚠️ Entity未实现此方法（审计v2.0.0 §SPEC）
```

### 2.3 Procurement（发注单）

> **核心**：对应业务流第二步下单。

```
Procurement（聚合根）
├── id: Long
├── factoryId: Long             # 关联工厂
├── productCode: String          # 主货号
├── subProductCode: String       # 子货号/枝番
├── material: String            # 材质
├── requiresQc: Boolean         # 是否需要检测
├── quantity: Integer           # 订购数量
├── priceRmb: BigDecimal       # 人民币单价
├── exchangeRate: BigDecimal   # CNY→JPY 汇率
├── taxPoint: BigDecimal       # 票点（默认 1.1）
├── billingType: BillingType    # 报关类型
├── estimatedPriceJpy: BigDecimal  # 估算批发价（自动计算）
├── customsRemarks: String      # 报关备注
├── instructionManual: String    # 说明书
├── orderDate: LocalDate       # 下单日
├── factoryShipDate: LocalDate  # 厂家出货日
├── plannedShipDate: LocalDate # 预计出货日（交货期）
├── actualShipDate: LocalDate  # 实际出货日
├── productLead: String        # 商品担当
├── japanLead: String          # 日本担当
├── chinaLead: String          # 中国担当
├── destination: String        # 发送目的地（转采购时从 SubProductItem 代入）
├── customerCompany: String    # 客户公司
├── linkedDemandId: Long ⚠️ # ⚠️ Entity中不存在；N:1关联改为 DemandProcurementMapping 表实现
├── linkedDemandItemId: Long ⚠️ # ⚠️ Entity中不存在；N:1关联改为 DemandProcurementMapping 表实现
├── leadTimeDays: Integer     # 交货期天数（30/45/60），默认值 30
├── cartonNotes: String       # 纸箱备注（v1.9.0 新增）
├── afterSalesDeadline: LocalDate  # 售后截止日（v1.10.0 新增）
├── status: ShipmentStatus     # 19态（含完了终态）
└── 领域方法
    ├── calculateEstimatedPriceJpy()  # 估算批发价
    ├── updateStatus(ShipmentStatus)   # 状态推进（含FSM校验）
    └── resetToUndecided()            # 重置为未定
```

---

## 3. 状态枚举

### 3.1 DemandStatus（需求状态）

```java
public enum DemandStatus {
    PENDING,     // 待确认（录入后默认，或取消关联后）
    CONFIRMED    // 已确认（已关联发注单，由发注单页面关联时写入 linkedProcurementId）
}
```

### 3.2 CooperationStatus（合作状态）

```java
public enum CooperationStatus {
    ACTIVE,     // 合作中
    SUSPENDED,  // 已暂停
    ELIMINATED, // 已淘汰
    POTENTIAL   // 潜在合作
}
```

**FactoryCategory（分类）:**

```java
public enum FactoryCategory {
    TOOLS, TEXTILE, PLASTIC, ELECTRONICS,
    FURNITURE, AUTO_PARTS, SPORTS, PET,
    MEDICAL, CRAFTS, CHEMICAL, OTHER
}
```

**PaymentTerms（账期）:**

```java
public enum PaymentTerms {
    CASH,   // 现结
    NET_30, // 月结30天
    NET_60, // 月结60天
    NET_90, // 月结90天
    CREDIT  // 信用账期
}
```

### 3.3 BillingType（报关类型）

```java
public enum BillingType {
    ZHE_LU_KAI_PIAO,     // 浙鲁开票
    CHAO_HUI_TUI_SHUI,   // 超慧退税
    NO_REFUND,           // 不退税
    OTHER                // 其他
}
```

### 3.4 ShipmentStatus（发注单状态）

19态，详见 `DOMAIN-发注管理领域模型.md` §3。

---

## 4. 价格计算公式

```
估算批发价 JPY = (priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05
```

| 常量 | 值 | 说明 |
|------|-----|------|
| CONSOLIDATION_FEE | 1.02 | 集拼费 |
| PROFIT_MARGIN | 1.2 | 利润率 |
| EXCHANGE_BUFFER | 1.05 | 汇率缓冲 |

---

## 5. API 设计

### ReplenishmentDemandController

```
GET    /api/v1/demands?page=&pageSize=&demandType=&productCode=&status=
GET    /api/v1/demands/{id}
POST   /api/v1/demands
PATCH  /api/v1/demands/{id}
DELETE /api/v1/demands/{id}
POST   /api/v1/demands/{id}/link          # v2.2.0：关联采购单 → status=CONFIRMED
POST   /api/v1/demands/{id}/unlink         # v2.2.0：取消关联 → status=PENDING
GET    /api/v1/demands/{id}/procurement    # 获取关联的采购单列表
GET    /api/v1/demands/suggest/destinations # 目的港自动补全
GET    /api/v1/demands/suggest/japan-leads  # 日本担当自动补全
```

> ⚠️ **v2.2.0 变更**：`/convert` 端点已移除，改用 `/link`+`DemandProcurementMapping` 表实现多对多关联。

### FactoryController

```
GET    /api/v1/factories?page=&pageSize=&factoryName=&status=
GET    /api/v1/factories/{id}
POST   /api/v1/factories
PATCH  /api/v1/factories/{id}
DELETE /api/v1/factories/{id}
```

### ProcurementController

```
GET    /api/v1/procurements?page=&pageSize=&status=&productCode=&customerCompany=
GET    /api/v1/procurements/{id}
POST   /api/v1/procurements
PATCH  /api/v1/procurements/{id}
DELETE /api/v1/procurements/{id}
```

---

## 6. 代码实现状态

- [x] ✅ `ReplenishmentDemand` 聚合根实体
- [x] ✅ `DemandType` 枚举
- [x] ✅ `DemandStatus` 枚举
- [x] ✅ `ReplenishmentDemandRepository` 领域接口
- [x] ✅ `ReplenishmentDemandUseCase` 用例服务
- [x] ✅ `ReplenishmentDemandController` REST 控制器
- [x] ✅ `Factory` 聚合根实体（内嵌表单）
- [x] ✅ `FactoryRepository` 领域接口 + JPA 适配器
- [x] ✅ `FactoryUseCase` 用例服务
- [x] ✅ `FactoryAssembler` DTO 转换器
- [x] ✅ `Procurement` 聚合根实体（含常量 + FSM + 报价计算）
- [x] ✅ `ShipmentStatus` 枚举（含 `isTerminal()` + `canTransitionTo()` + 完整FSM map）
- [x] ✅ `BillingType` 枚举
- [x] ✅ `ProcurementRepository` 领域接口 + JPA 适配器
- [x] ✅ `ProcurementUseCase` 用例服务
- [x] ✅ `ProcurementAssembler` DTO 转换器
- [x] ✅ `ProcurementController` REST 控制器
- [x] ✅ `ProcurementUseCaseTest` 单元测试（14个用例，全部通过）
- [x] ✅ `FactoryUseCaseTest` 单元测试（8个用例，全部通过）
- [x] ✅ ~~`FactorySynergyPort`~~ （v1.8.0 已移除，强耦合，已改用 DB 直传）
- [x] ✅ `@/api/order.ts` 前端 API 客户端
- [x] ✅ `ProcurementPage.vue` 页面（已对接真实 API）
- [x] ✅ 工厂内嵌选择器（新建/编辑）
