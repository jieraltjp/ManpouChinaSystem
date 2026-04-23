# SPEC-B03 — 验货记录业务规格（步骤3）

> **版本**: 1.0.0
> **更新**: 2026-04-23（补充元数据字段）
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **业务步号**: 03（验货记录）
> **对应 UI 文档**: `docs/ui/pages/03-inspection.md`
> **对应数据库文档**: `docs/database/DB-03-inspection.md`
> **前置**: 步骤2（Procurement 已下单）
> **后续**: 步骤4（LogisticsPlan 调配计划）

---

## 1. 业务背景

货物到仓后进行验货，记录检品数、合格数、不良数。验货结果 PASS → 进入调配（步骤4）；FAIL → 触发退货流程。

**关键设计**：QcRecord 从内嵌值对象升级为独立聚合根，可独立查询和追踪。

---

## 2. 聚合根

### QcRecord（验货记录）

```
QcRecord（聚合根）
├── id: Long
├── qcCode: String              # Q-YYYYMMDD-NNN
├── procurementId: Long          # 关联采购单（FK）
├── sellerName: String          # 卖家名称（来自 Factory.factoryName）
├── productCode: String          # 货号
├── subProductCode: String      # 子货号
├── qcUserId: Long             # 验货负责人
├── qcType: QcType             # ONSITE(仓库验货) / REMOTE(现场异地验货)
├── qcDate: LocalDate          # 验货日期
├── result: QcResult          # PASS / FAIL
├── status: QcStatus          # PENDING → COMPLETED / RETURN_REQUESTED
├── inspectionCount: Integer    # 检品数
├── passedCount: Integer      # 合格数量
├── defectiveCount: Integer    # 不良数量（自动计算）
├── boxCount: Integer          # 箱数
├── boxLengthCm: BigDecimal    # 箱子长(cm)
├── boxWidthCm: BigDecimal     # 箱子宽(cm)
├── boxHeightCm: BigDecimal    # 箱子高(cm)
├── netWeightPerUnit: BigDecimal # 单个净重(kg)
├── grossWeight: BigDecimal    # 毛重(kg)
├── taxInclusivePrice: BigDecimal # 含税价（元）
├── material: String          # 材质
├── taxRefund: Boolean       # 是否退税
├── qcStandard: String       # 验收标准
├── remarks: String           # 备注
├── images: String            # 缺陷照片URL列表（JSON数组）
├── destination: String       # 目的地
├── quantity: Integer        # 订购数量
├── orderDate: LocalDate     # 下单日
└── 领域方法
    ├── calculateDefectiveCount()  # 不良数 = inspectionCount - passedCount
    └── updateStatus(QcStatus)      # 状态推进（含FSM校验）
```

---

## 3. 枚举

```java
public enum QcType {
    ONSITE,   // 検品 — 仓库验货
    REMOTE    // 現地検品 — 现场异地验货
}

public enum QcResult {
    PASS,  // 验货通过
    FAIL   // 验货不通过
}

public enum QcStatus {
    PENDING,          // 待验货
    COMPLETED,        // 已完成（终态）
    RETURN_REQUESTED; // 发起退货（终态）

    public boolean isTerminal() { return this == COMPLETED || this == RETURN_REQUESTED; }

    public boolean canTransitionTo(QcStatus target) {
        if (this == target) return true;
        return transitions().getOrDefault(this, Set.of()).contains(target);
    }

    private static Map<QcStatus, Set<QcStatus>> transitions() {
        return Map.of(
            PENDING,          Set.of(PENDING, COMPLETED, RETURN_REQUESTED),
            COMPLETED,        Set.of(),
            RETURN_REQUESTED, Set.of()
        );
    }
}
```

---

## 4. 状态流转

```
  PENDING ──[PASS]──▶ COMPLETED [终态]
     │
     └──[FAIL]──▶ RETURN_REQUESTED [终态]
```

---

## 5. 自动计算

| 字段 | 公式 | 实现位置 |
|------|------|---------|
| defectiveCount | `inspectionCount - passedCount` | `QcRecord.calculateDefectiveCount()` |
| result | `defectiveCount == 0 ? PASS : FAIL` | 由验货员手动判断 |

---

## 6. API 设计

```
GET    /api/v1/qc-records?page=&pageSize=&procurementId=&productCode=&result=&status=
GET    /api/v1/qc-records/{id}
POST   /api/v1/qc-records
PATCH  /api/v1/qc-records/{id}
DELETE /api/v1/qc-records/{id}
```

---

## 7. 代码实现状态

- [x] ✅ `QcRecord` 聚合根实体（含 `calculateDefectiveCount()`）
- [x] ✅ `QcStatus` 枚举（含 `isTerminal()` + `canTransitionTo()` + FSM map）
- [x] ✅ `QcResult` 枚举
- [x] ✅ `QcType` 枚举
- [x] ✅ `QcRecordRepository` 领域接口 + JPA 适配器
- [x] ✅ `QcRecordUseCase` 用例服务
- [x] ✅ `QcRecordAssembler` DTO 转换器
- [x] ✅ `QcRecordController` REST 控制器
- [x] ✅ `QcRecordUseCaseTest` 单元测试（10个用例，全部通过）
- [x] ✅ `@/api/inspection.ts` 前端 API 客户端
- [x] ✅ `InspectionPage.vue` 页面（已对接真实 API）
