# 国内报关 — 业务规格（步骤5）

> **版本**: 1.5.1
> **创建**: 2026-04-22
> **更新**: 2026-05-07（v1.5.1：补POST /batch批量创建端点）
> **更新**: 2026-04-30（v1.4.0：containerNo 改为必填主键 + 批量创建对话框 + CustomsBatchCreateCmd）
> **状态**: ✅ v1.4.0 实施完成
> **对应前端**: `DomesticCustomsPage.vue`（`apps/web/src/pages/customs/DomesticCustomsPage.vue`）
> **前置**: LogisticsPlan 已编排货柜号（containerNo）
> **后续**: JapanCustomsRecord（步骤6）— ✅ 已实现

---

## 1. 业务背景

国内出口报关是跨境贸易的必要环节。货物离港前，向中国海关提交出口申报，获取放行通知后货物方可装船出运。

**货柜级聚合（v1.3.0 新增，v1.4.0 强化）**：

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

**v1.4.0 核心变更**：

| 字段 | v1.3.0（错误） | v1.4.0（正确） |
|------|---------------|---------------|
| 新建入口 | 采购单号（procurementId）为主 | 货柜号（containerNo）为主 |
| containerNo | 可选 | **必填** |
| procurementId | 必填 | **可选**（作为参考，不主导流程） |

> 报关业务对象是**货柜**，不是采购单。采购单只是货物来源参考。

---

## 2. 聚合根定义

### 2.1 DomesticCustomsRecord

```
DomesticCustomsRecord（聚合根）
├── id: Long
├── customsCode: String              # 报关单号（DC-YYYYMMDD-NNN，系统生成）
├── containerNo: String              # 货柜号（v1.3.0，**v1.4.0 改为必填**）
├── procurementId: Long             # 关联采购单（**v1.4.0 改为可选参考字段**）
├── logisticsPlanId: Long           # 关联调配计划（FK → logistics_plan.id，可选）
├── factoryId: Long                 # 关联工厂（FK → factory.id）
├── productCode: String             # 货号
├── subProductCode: String          # 子货号
├── quantity: Integer               # 报关数量
├── estimatedValueCny: BigDecimal   # 预估货值（元）
├── status: DomesticCustomsStatus   # PENDING / SUBMITTED / CLEARED / REJECTED
├── remarks: String                  # 备注
├── createBy: String
├── createTime: LocalDateTime
├── updateTime: LocalDateTime
│
└── 领域方法
    ├── submit()                    # 提交海关 → status = SUBMITTED
    ├── clear()                     # 放行 → status = CLEARED
    ├── reject(reason)              # 驳回 → status = REJECTED
    └── isTerminal()                # CLEARED 为终态
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

## 4. 触发规则（v1.4.0）

**操作路径（正确流程）**：

```
LogisticsPlanPage
  → 按货柜号筛选（containerNo）
  → 选中某货柜号下的所有计划 → 点击"创建报关"
  → 跳转 /procurement/domestic-customs?containerNo=TRLU1234567
  → DomesticCustomsPage 自动填入货柜号
  → 用户按商品+工厂分别创建报关单（同一货柜号可创建多条）
```

**为什么不自动创建**：
- 1货柜 = N 个 DomesticCustomsRecord（按商品+工厂分组），自动触发只能创建1条
- 实际业务由操作员按商品/工厂维度拆分报关
- 简化设计避免一次自动创建大量无意义记录

**LogisticsPlanPage 改造要点（v1.4.0）**：

| 改造项 | 说明 |
|--------|------|
| 按货柜号分组展示 | 表格加 containerNo 列，支持按货柜号筛选 |
| 货柜级批量"创建报关" | 选中同 containerNo 的多条记录 → 跳转报关页面 |
| 显示已有报关状态 | 某货柜已报关 → 显示 CLEARED 标签，防止重复创建 |

---

## 5. API 设计

### CustomsController

```
GET    /api/v1/customs?page=&pageSize=&containerNo=&procurementId=&status=
GET    /api/v1/customs/{id}
POST   /api/v1/customs                              # 创建（**containerNo 必填，procurementId 可选**）
POST   /api/v1/customs/batch                       # 批量创建（按货柜号聚合，v1.4.0新增）
PUT    /api/v1/customs/{id}                         # 编辑
PATCH  /api/v1/customs/{id}/submit                  # 提交
PATCH  /api/v1/customs/{id}/clear                   # 放行
PATCH  /api/v1/customs/{id}/reject                  # 驳回
DELETE /api/v1/customs/{id}
```

> ⚠️ **v1.4.0 API 变更**：POST body 中 `containerNo` 改为必填，`procurementId` 改为可选。

---

## 6. 前端改造清单（v1.4.0 → v1.5.0）

### 6.1 DomesticCustomsPage.vue

| 改造项 | 当前 | 目标 |
|--------|------|------|
| 新建弹窗 procurementId | 必填 el-input-number | 可选 el-input-number |
| 新建弹窗 containerNo | 可选 el-input | **必填 remote-select（第一位）** |
| 新建入口文案 | "新规报关" | **"按货柜批量创建"** |
| 新建模式 | 单条表单 | **批量对话框（选货柜号→表格展示 LogisticsPlan→多选→批量提交）** |
| 列表页筛选 | procurementId 优先 | containerNo 优先 |
| URL 参数 | ?containerNo= | ✅ 已支持 |

### 6.2 LogisticsPlanPage.vue

| 改造项 | 当前 | 目标 |
|--------|------|------|
| "创建报关"按钮 | 有 | ✅ |
| containerNo 列 | 有 | ✅ 已支持 |
| 按货柜号筛选 | 有（el-select + remote） | ✅ |
| 已有报关状态提示 | 无 | 待实施 |

---

## 7. 缺口阻塞（字段待确认）

| 字段 | 现状 | 阻塞原因 |
|------|------|---------|
| hsCode | 占位 | 是否与 Product.hsCode 关联？ |
| exportPort 枚举 | 占位 | 具体枚举值待确认（宁波/上海/大连/天津/其他） |
| declaredValueRmb | 占位 | 由用户填入还是从采购单价计算？ |
| 商检流程 | 无 | 部分商品需先商检再报关 |

---

## 8. 代码实现清单

- [x] ✅ `DomesticCustomsRecord` 聚合根实体（含 `containerNo` 字段 v1.3.0）
- [x] ✅ `DomesticCustomsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [x] ✅ `DomesticCustomsRepository` 领域接口（直接继承 JpaRepository）
- [x] ✅ `CustomsAssembler` DTO ↔ Entity 转换器
- [x] ✅ `CustomsUseCase` 用例服务
- [x] ✅ `CustomsController` REST 控制器
- [x] ✅ `@/api/customs.ts` 前端 API 客户端
- [x] ✅ `DomesticCustomsPage.vue` 前端页面（`apps/web/src/pages/customs/DomesticCustomsPage.vue`）
- [x] ✅ `LogisticsPlanPage.vue` 有 containerNo 列（v1.3.0）
- [x] ✅ LogisticsPlanPage 增加"创建报关"按钮（v1.4.0）
- [x] ✅ DomesticCustomsPage 批量创建表单（v1.4.0）：选货柜号 → 表格展示 LogisticsPlan → 多选 → 批量提交
- [x] ✅ `CustomsCreateCmd.containerNo` 增加 `@NotBlank` 校验（v1.4.0）
- [x] ✅ `CustomsBatchCreateCmd` + `POST /batch` 批量创建接口（v1.4.0）
- [ ] 🔴 `CustomsUseCaseTest` 单元测试
- [x] ✅ DB迁移脚本 `V17__domestic_customs_record_table.sql`
- [x] ✅ DB迁移脚本 `V36__domestic_customs_container_no.sql`（`container_no` 列已存在）
