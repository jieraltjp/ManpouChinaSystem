# 国内报关 — 业务规格（步骤5）

> **版本**: 1.3.0
> **创建**: 2026-04-22
> **更新**: 2026-04-27（v1.3.0：方案C——货柜级报关，containerNo 字段 + LogisticsPlanPage 发起流程）
> **更新**: 2026-04-24（v1.2.0：全量实现已确认，与 DB-05 v1.3.0 状态对齐）
> **更新**: 2026-04-22 — 步骤6已实现，同步更新后续状态
> **状态**: ✅ 已实现（B05 报关单核心 CRUD + 生命周期流转 + v1.3.0 货柜级聚合）
> **对应前端**: `DomesticCustomsPage.vue`（`apps/web/src/pages/customs/DomesticCustomsPage.vue`）· `docs/ui/pages/05-domestic-customs.md`
> **前置**: LogisticsPlan 已编排货柜号（containerNo）
> **后续**: JapanCustomsRecord（步骤6）— ✅ 已实现

---

## 1. 业务背景

国内出口报关是跨境贸易的必要环节。货物离港前，向中国海关提交出口申报，获取放行通知后货物方可装船出运。

**货柜级聚合（v1.3.0 新增）**：

实际业务中，**一个货柜号下可能有多个 LogisticsPlan（不同商品/工厂）**，但对应同一份出口报关资料。报关维度为**货柜号**：

```
货柜 TRLU1234567
  ├── LogisticsPlan-A: 商品X / 工厂甲 / 50件
  ├── LogisticsPlan-B: 商品X / 工厂乙 / 30件
  └── LogisticsPlan-C: 商品Y / 工厂甲 / 20件
        ↓ 操作员从 LogisticsPlanPage 点击"创建报关"
  DomesticCustomsRecord(containerNo=TRLU1234567, productCode=商品X, factoryId=工厂甲)
  DomesticCustomsRecord(containerNo=TRLU1234567, productCode=商品Y, factoryId=工厂甲)
```

> ⚠️ 当前 Phase 1 按**商品+工厂**分组报关。未来可扩展为全明细关联表（方案A）。

---

## 2. 聚合根定义

### 2.1 DomesticCustomsRecord

```
DomesticCustomsRecord（聚合根）
├── id: Long
├── customsCode: String              # 报关单号（DC-YYYYMMDD-NNN，系统生成）
├── containerNo: String              # 货柜号（v1.3.0，来自 LogisticsPlan.containerNo）
├── procurementId: Long              # 关联采购单（FK → procurement.id，可选）
├── logisticsPlanId: Long            # 关联调配计划（FK → logistics_plan.id，可选；v1.3.0 保留字段）
├── factoryId: Long                  # 关联工厂（FK → factory.id）
├── productCode: String              # 货号
├── subProductCode: String           # 子货号
├── quantity: Integer                # 报关数量
├── estimatedValueCny: BigDecimal   # 预估货值（元）
├── status: DomesticCustomsStatus   # PENDING / SUBMITTED / CLEARED / REJECTED
├── remarks: String                  # 备注
├── createBy: String
├── createTime: LocalDateTime
├── updateTime: LocalDateTime
│
└── 领域方法
    ├── submit()                     # 提交海关 → status = SUBMITTED
    ├── clear()                      # 放行 → status = CLEARED
    ├── reject(reason)               # 驳回 → status = REJECTED
    └── isTerminal()                 # CLEARED 为终态
```

---

## 3. 状态枚举

```java
public enum DomesticCustomsStatus {
    PENDING,      // 待申报
    SUBMITTED,    // 已提交海关
    CLEARED,      // 已放行（终态）
    REJECTED      // 被驳回（可修正后重新提交）
}
```

### 状态流转

```
  PENDING ──[提交]──▶ SUBMITTED ──[放行]──▶ CLEARED [终态]
                                    └──[驳回]──▶ REJECTED [可重新提交]
  REJECTED ──[重新编辑提交]──▶ SUBMITTED
```

---

## 4. 触发规则（v1.3.0 修正）

**规则**：由用户在 LogisticsPlanPage 手动发起，不自动创建。

**操作路径**：
```
LogisticsPlanPage → 选中某货柜号下的计划 → 点击"创建报关"
    → 跳转 /procurement/domestic-customs?containerNo=TRLU1234567
    → DomesticCustomsPage 自动填入货柜号 + 显示该货柜下已有报关记录
    → 用户按商品+工厂分别创建报关单
```

**为什么不自动创建（方案B简化理由）**：
- 1货柜 = N 个 DomesticCustomsRecord（按商品+工厂分组），自动触发只能创建1条
- 实际业务由操作员按商品/工厂维度拆分报关
- 简化设计避免一次自动创建大量无意义记录

---

## 5. API 设计

### CustomsController

```
GET    /api/v1/customs?page=&pageSize=&containerNo=&procurementId=&status=
GET    /api/v1/customs/{id}
POST   /api/v1/customs                              # 创建（containerNo 必填）
PUT    /api/v1/customs/{id}                          # 编辑
PATCH  /api/v1/customs/{id}/submit                    # 提交
PATCH  /api/v1/customs/{id}/clear                    # 放行
PATCH  /api/v1/customs/{id}/reject                    # 驳回
DELETE /api/v1/customs/{id}
```

> v1.3.0 新增 `containerNo` 筛选参数，列表页按货柜号筛选。

---

## 6. 缺口阻塞（字段待确认）

| 字段 | 现状 | 阻塞原因 |
|------|------|---------|
| hsCode | 占位 | 是否与 Product.hsCode 关联？ |
| exportPort 枚举 | 占位 | 具体枚举值待确认（宁波/上海/大连/天津/其他） |
| declaredValueRmb | 占位 | 由用户填入还是从采购单价计算？ |
| 商检流程 | 无 | 部分商品需先商检再报关 |

---

## 7. 代码实现清单

- [x] ✅ `DomesticCustomsRecord` 聚合根实体（含 `containerNo` 字段 v1.3.0）
- [x] ✅ `DomesticCustomsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [x] ✅ `DomesticCustomsRepository` 领域接口（直接继承 JpaRepository）
- [x] ✅ `CustomsAssembler` DTO ↔ Entity 转换器
- [x] ✅ `CustomsUseCase` 用例服务
- [x] ✅ `CustomsController` REST 控制器
- [x] ✅ `@/api/customs.ts` 前端 API 客户端
- [x] ✅ `DomesticCustomsPage.vue` 前端页面（`apps/web/src/pages/customs/DomesticCustomsPage.vue`）
- [x] ✅ `LogisticsPlanPage.vue` 增加"创建报关"按钮（v1.3.0）
- [ ] 🔴 `CustomsUseCaseTest` 单元测试
- [x] ✅ DB迁移脚本 `V17__domestic_customs_record_table.sql`
- [ ] 🔴 V36 迁移：`domestic_customs_record` 增加 `container_no` 字段
