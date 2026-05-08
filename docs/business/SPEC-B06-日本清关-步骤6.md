# 日本清关 — 业务规格（步骤6）

> **版本**: 1.4.1
> **创建**: 2026-04-22
> **更新**: 2026-05-07（v1.4.1：补POST /batch/PUT/DELETE端点）
> **更新**: 2026-04-30（v1.4.0：**containerNo 为主键 + 与 DomesticCustomsRecord 联动 + 货柜级批量创建**）
> **状态**: ✅ 已实现（v1.4.0 containerNo 全链路实现完成，V44 待执行）
> **对应前端**: `JapanCustomsRecordPage.vue`（`apps/web/src/pages/customs/JapanCustomsRecordPage.vue`）
> **前置**: DomesticCustomsRecord.status = CLEARED
> **后续**: TaxRefundRecord（步骤7）— ✅ 已实现

---

## 1. 业务背景

货物到达日本港口后，向日本海关办理进口清关手续，缴纳进口关税和消费税，获取放行通知后货物方可提取，进入日本仓库或直接配送。

**货柜级维度（v1.4.0 新增）**：

日本清关是**按货柜号**进行的，与国内报关（步骤5）一一对应：

```
货柜 TRLU1234567
  ├── DomesticCustomsRecord-A: CLEARED（国内报关已放行）
  └── JapanCustomsRecord-A: 待清关（containerNo=TRLU1234567）

国内报关 CLEARED 后 → 自动/手动创建日本清关记录（同一货柜号）
```

> ⚠️ v1.3.0 之前：`procurementId` 作为主关联字段，**无** `containerNo`
> ⚠️ v1.4.0：`containerNo` 为主键字段，`procurementId` 降为可选参考

**预期流程**：
```
DomesticCustomsRecord.status = CLEARED
    │
    └── 自动/手动创建 JapanCustomsRecord（**按 containerNo**）
            │
            ├── 录入清关资料（入境报关号/到达日期/清关行）
            ├── 开始清关
            ├── 缴纳税费（关税/消费税）
            └── 放行
```

---

## 2. 聚合根定义

### 2.1 JapanCustomsRecord

```
JapanCustomsRecord（聚合根）
├── id: Long
├── customsEntryNo: String          # 入境报关号（JC-YYYYMMDD-NNN）
├── containerNo: String             # 货柜号（**v1.4.0 新增必填**）
├── domesticCustomsId: Long        # 关联国内报关单（FK → domestic_customs_record.id）
├── logisticsPlanId: Long          # 关联调配计划（FK → logistics_plan.id，可选）
├── procurementId: Long            # 关联采购单（**v1.4.0 改为可选参考字段**）
├── factoryId: Long               # 关联工厂
├── productCode: String           # 货号（**v1.4.0 新增**）
├── subProductCode: String        # 子货号/颜色（来自 LogisticsPlan）
├── status: JapanCustomsStatus   # PENDING / IN_PROGRESS / CLEARED / FAILED
├── customsEntryNo: String         # 入境报关号（已在 DB）
├── arrivalDate: LocalDate        # 到达日期
├── customsBroker: String          # 清关行
├── brokerPhone: String           # 清关行电话
├── brokerContact: String         # 清关行联系人
├── importDutyPaid: BigDecimal   # 进口关税（JPY）
├── consumptionTaxPaid: BigDecimal # 消费税（JPY）
├── clearanceDate: LocalDate       # 清关完成日期
├── arrivalPort: String           # 目的港（来自 LogisticsPlan）
├── declaredWeightKg: BigDecimal  # 申报重量（来自 LogisticsPlan）
├── declaredVolumeCbm: BigDecimal # 申报体积（来自 LogisticsPlan）
├── remarks: String               # 备注
├── createBy: String
├── createTime: LocalDateTime
├── updateTime: LocalDateTime
│
└── 领域方法
    ├── startClearance()         # 开始清关 → status = IN_PROGRESS
    ├── complete(importDuty, consumptionTax)  # 放行 → status = CLEARED
    ├── fail(reason)             # 失败 → status = FAILED
    └── isTerminal()             # CLEARED / FAILED 为终态
```

---

## 3. 状态枚举

```java
public enum JapanCustomsStatus {
    PENDING,       // 待清关
    IN_PROGRESS,   // 清关中
    CLEARED,       // 已放行（终态）
    FAILED         // 清关失败（终态）
}
```

### 状态流转

```
  PENDING ──[开始]──▶ IN_PROGRESS ──[完成]──▶ CLEARED [终态]
                                           └──[失败]──▶ FAILED [终态]
```

---

## 4. 触发规则（v1.4.0）

**规则**：DomesticCustomsRecord.status = CLEARED 时，自动创建 JapanCustomsRecord（status = PENDING，**按 containerNo 关联**）。

**操作路径**：

```
DomesticCustomsPage（国内报关）
  → 某条 DomesticCustomsRecord 状态变为 CLEARED
  → 自动/手动创建 JapanCustomsRecord（containerNo 相同）
  → JapanCustomsRecordPage 显示该货柜的清关状态
```

---

## 5. API 设计

### JapanCustomsController

```
GET    /api/v1/japan-customs?page=&pageSize=&containerNo=&domesticCustomsId=&status=
GET    /api/v1/japan-customs/{id}
POST   /api/v1/japan-customs                              # 创建（**containerNo 必填**）
POST   /api/v1/japan-customs/batch                       # 批量创建（v1.4.0新增）
PUT    /api/v1/japan-customs/{id}                        # 编辑（v1.4.0新增）
PATCH  /api/v1/japan-customs/{id}
PATCH  /api/v1/japan-customs/{id}/start                  # 开始清关
PATCH  /api/v1/japan-customs/{id}/complete               # 完成清关（含缴纳税费）
PATCH  /api/v1/japan-customs/{id}/fail                   # 清关失败
DELETE /api/v1/japan-customs/{id}                        # 删除（v1.4.0新增）
```

> ⚠️ **v1.4.0 API 变更**：
> - `GET` 新增 `containerNo` 筛选参数
> - `GET` 新增 `domesticCustomsId` 筛选参数
> - `POST` body 中 `containerNo` 必填，`procurementId` 可选

---

## 6. 前端改造清单（v1.4.0）

### 6.1 JapanCustomsRecordPage.vue

| 改造项 | 当前 | 目标 |
|--------|------|------|
| 列表页主筛选 | procurementId | containerNo |
| containerNo 列 | 无 | 有（展示 + 可点击跳转 DomesticCustomsPage） |
| domesticCustomsId 列 | 无 | 有（展示国内报关状态） |

---

## 7. 缺口阻塞（字段待确认）

| 字段 | 现状 | 阻塞原因 |
|------|------|---------|
| customsEntryNo | 占位 | 入境报关号由谁提供？系统生成还是海关返回？ |
| 税费计算 | 占位 | 进口关税/消费税是否有标准计算公式？ |
| 清关费用记账 | 占位 | 税费是否需要生成 FinanceRecord？ |
| customsBroker 管理 | 占位 | 清关行是固定合作方还是有选择列表？ |

---

## 8. 代码实现清单

- [x] ✅ `JapanCustomsRecord` 聚合根实体（含 containerNo + productCode + factoryId v1.4.0）
- [x] ✅ `JapanCustomsStatus` 枚举（含 `isTerminal()`）
- [x] ✅ `JapanCustomsRepository` 领域接口（直接继承 JpaRepository）
- [x] ✅ `JapanCustomsAssembler` DTO ↔ Entity 转换器（含 containerNo 映射 v1.4.0）
- [x] ✅ `JapanCustomsUseCase` 用例服务（含 containerNo 过滤 v1.4.0）
- [x] ✅ `JapanCustomsController` REST 控制器
- [x] ✅ `@/api/japanCustoms.ts` 前端 API 客户端（含 containerNo v1.4.0）
- [x] ✅ `JapanCustomsRecordPage.vue` 前端页面（含 containerNo 筛选/列/新建 v1.4.0）
- [x] ✅ `OrderOverviewUseCase` 已集成 JapanCustomsRecord（步骤6）
- [x] ✅ DB迁移脚本 `V12__japan_customs_record_table.sql`
- [x] ✅ DB迁移脚本 `V44__japan_customs_container_no.sql`（v1.4.0：container_no + product_code + factory_id 列 + 索引）
- [ ] 🔴 `JapanCustomsUseCaseTest` 单元测试
