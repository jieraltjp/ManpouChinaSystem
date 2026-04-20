# 项目文档：发注管理（procurement 模块）

> **注意**：发注管理已从旧版 procurement-service 迁移至 `manpou-allinone` 单体。
> 旧版 procurement-service（端口 18083）仅骨架，核心业务实现在 manpou-allinone（端口 18090）。
> 本文档描述 manpou-allinone 中的 procurement 模块。

---

## 1. 模块定位

| 维度 | 说明 |
|------|------|
| 模块名 | procurement |
| 所在 jar | manpou-allinone |
| 端口 | 18090 |
| 包名 | `com.manpou.allinone.procurement` |
| 描述 | 发注单（Procurement）全生命周期管理，对应 Excel 出货单弹窗 |
| 当前状态 | ✅ CRUD + 报价计算 + 终态校验已完成 |
| 下一步 | 完整状态流转校验 + 验货/货柜/财务 |

---

## 2. 项目结构

```
src/main/java/com/manpou/allinone/
└── procurement/
    ├── domain/
    │   ├── model/
    │   │   ├── BaseEntity.java         # 审计基类（createTime/updateTime）
    │   │   ├── Procurement.java        # 发注单聚合根（✅ 已实现）
    │   │   └── ShipmentStatus.java     # 状态枚举（未定→完了）
    │   └── repository/
    │       └── ProcurementRepository.java
    ├── application/
    │   ├── dto/
    │   │   ├── ProcurementCreateCmd.java
    │   │   ├── ProcurementUpdateCmd.java
    │   │   ├── ProcurementQuery.java
    │   │   └── ProcurementPageQuery.java
    │   ├── assembler/
    │   │   └── ProcurementAssembler.java
    │   └── usecase/
    │       └── ProcurementUseCase.java  # 业务编排层
    └── interfaces/
        └── controller/
            └── ProcurementController.java  # /api/v1/procurements

src/main/resources/
└── application.yml                    # 18090 端口，H2 内存数据库
```

---

## 3. 发注单实体（Procurement）

对应 Excel 出货单弹窗，一次发注 = 一条记录。

**核心字段**：

| 字段 | DB 列名 | 说明 |
|------|---------|------|
| id | id | 主键 |
| productCode | product_code | 商品代码（关联 Product.productCode） |
| quantity | quantity | 订购数量 |
| priceRmb | price_rmb | 人民币单价 |
| exchangeRate | exchange_rate | CNY→JPY 汇率 |
| taxPoint | tax_point | 票点（默认 1.1） |
| billingMethod | billing_method | 计费方式 |
| estimatedPriceJpy | estimated_price_jpy | 估算批发价 JPY（自动计算） |
| orderDate | order_date | 下单日（1688下单日期） |
| factoryShipDate | factory_ship_date | 厂家出货日 |
| plannedShipDate | planned_ship_date | 计划出货日 |
| productLead | product_lead | 商品担当 |
| japanLead | japan_lead | 日本担当 |
| chinaLead | china_lead | 中国担当 |
| destination | destination | 发送目的地 |
| customerCompany | customer_company | 客户公司 |
| status | status | 状态（默认：未定） |

**计算公式**（后端自动计算并存储）：
```
estimatedPriceJpy = (priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05
```

---

## 4. 状态机

### 状态枚举（ShipmentStatus）

```
未定 → 発注待 → 永康/直送 → 倉庫着 → 検品/現地検品 → ...
  → エア便/輸出 → 通関 → 日本着 → 会計 → 完了
```

| 状态 | 说明 | 终态 |
|------|------|------|
| 未定 | 还未下单，仅记录需求 | |
| 発注待 | 已录入商品，等待下单 | |
| 永康 | 货物发往永康仓 | |
| 直送 | 厂家直接发货（不经永康仓） | |
| OEM | OEM 定制产品路径 | |
| 倉庫着 | 货物到达仓库 | |
| 現地検品 | 现场异地验货 | |
| 検品 | 仓库验货 | |
| エア便 | 空运 | |
| メーカー直送 | 厂家直送 | |
| 輸出 | 已出口 | |
| 通関 | 已报关 | |
| 日本着 | 已到日本 | |
| 会計 | 财务结算 | |
| 完了 | 全流程结束 | ✅ |
| 退货 | 退货（独立处理） | |

**路径说明**：
- 永康路径：未定 → 発注待 → 永康 → 倉庫着 → 検品 → エア便/輸出 → 通関 → 日本着 → 会計 → 完了
- OEM 路径：未定 → 発注待 → OEM → 倉庫着 → 現地検品 → メーカー直送 → 完了

---

## 5. API 契约

> 详见 `docs/business/API-发注管理.md`

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/v1/procurements` | 分页查询列表 | ✅ |
| GET | `/api/v1/procurements/{id}` | 获取详情 | ✅ |
| POST | `/api/v1/procurements` | 创建发注单 | ✅ |
| PATCH | `/api/v1/procurements/{id}` | 部分更新（含状态推进） | ✅ |
| DELETE | `/api/v1/procurements/{id}` | 删除（仅未定/発注待） | ✅ |

---

## 6. 领域方法

```java
// 计算报价
procurement.calculateEstimatedPriceJpy();

// 更新状态（终态禁止修改）
procurement.updateStatus(ShipmentStatus.発注待);

// 重置为未定
procurement.resetToUndecided();
```

> ⚠️ 完整状态流转校验（validateTransition）待实现，当前仅检查终态。

---

## 7. 数据库

- **开发**：H2 内存（`jdbc:h2:mem:allinone`）
- **表**：`procurement`
- **迁移**：JPA `ddl-auto: update` 管理开发期 schema

---

## 8. 行动项

- [ ] **Phase A**：完整状态流转校验（validateTransition）
- [ ] **Phase B**：验货（QC）功能
- [ ] **Phase B**：货柜管理
- [ ] **Phase B**：财务结算
- [ ] **Phase B**：退货管理

---

## 9. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/business/README.md` | 业务文档总览 + 实现进度 |
| `docs/business/API-发注管理.md` | API 契约 |
| `docs/business/DOMAIN-发注管理领域模型.md` | 领域模型 |
| `docs/business/SPEC-发注管理流程.md` | 需求规格 + 状态机规则 |
| `docs/pro/19-manpou-allinone.md` | manpou-allinone 整体文档 |
| `apps/manpou-allinone/` | 代码实现 |
