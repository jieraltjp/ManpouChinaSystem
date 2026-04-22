# DB-06 — 日本清关数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: 🔴 占位（字段待确认）
> **业务步号**: 06（日本清关）
> **对应业务文档**: `SPEC-B01-全链路总览.md` §第六步 · `SPEC-日本清关-步骤6.md`
> **对应 UI 文档**: `docs/ui/pages/08-japan-customs.md`
> **对应后端聚合根**: `JapanCustomsRecord`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `japan_customs_record` | JapanCustomsRecord | 🔴 占位 |

---

## 1. japan_customs_record（日本清关）🔴占位

> ⚠️ 字段为占位，待业务方提供真实清关文件样本后确认。

**对应**: `JapanCustomsRecord` 聚合根

```sql
-- TODO: 字段待业务方确认后补充完整
CREATE TABLE japan_customs_record (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    customs_entry_no      VARCHAR(64) COMMENT '入境报关号',
    procurement_id        BIGINT NOT NULL COMMENT '关联采购单 FK → procurement.id',
    domestic_customs_id  BIGINT COMMENT '关联国内报关单 FK → domestic_customs_record.id',
    logistics_plan_id     BIGINT COMMENT '关联调配计划 FK → logistics_plan.id',
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / IN_PROGRESS / CLEARED / FAILED',
    arrival_date          DATE COMMENT '到达日期',
    customs_broker        VARCHAR(128) COMMENT '清关行',
    broker_phone          VARCHAR(32) COMMENT '清关行电话',
    broker_contact        VARCHAR(64) COMMENT '清关行联系人',
    import_duty_paid      BIGINT COMMENT '进口关税（JPY）',
    consumption_tax_paid BIGINT COMMENT '消费税（JPY）',
    clearance_date        DATE COMMENT '清关完成日期',
    arrival_port          VARCHAR(64) COMMENT '目的港（来自 logistics_plan）',
    declared_weight_kg   DECIMAL(10,4) COMMENT '申报重量（来自 logistics_plan）',
    declared_volume_cbm   DECIMAL(10,6) COMMENT '申报体积（来自 logistics_plan）',
    remarks               VARCHAR(512),
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by             VARCHAR(64) NOT NULL,
    update_by             VARCHAR(64) NOT NULL,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_jc_procurement (procurement_id),
    INDEX idx_jc_status (status),
    INDEX idx_jc_entry_no (customs_entry_no)
);
```

---

## 字段映射（占位）

| 实体字段 | 数据库列 | 状态 |
|---------|---------|------|
| id | `id` | ✅ |
| customsEntryNo | `customs_entry_no` | 🔴待确认 |
| procurementId | `procurement_id` | ✅ |
| domesticCustomsId | `domestic_customs_id` | ✅ |
| logisticsPlanId | `logistics_plan_id` | ✅ |
| status | `status` | ✅ |
| arrivalDate | `arrival_date` | ✅ |
| customsBroker | `customs_broker` | ✅ |
| brokerPhone | `broker_phone` | ✅ |
| brokerContact | `broker_contact` | ✅ |
| importDutyPaid | `import_duty_paid` | ✅ |
| consumptionTaxPaid | `consumption_tax_paid` | ✅ |
| clearanceDate | `clearance_date` | ✅ |
| arrivalPort | `arrival_port` | ✅ |
| declaredWeightKg | `declared_weight_kg` | ✅ |
| declaredVolumeCbm | `declared_volume_cbm` | ✅ |

---

## 代码实现状态

- [ ] 🔴 `JapanCustomsRecord` 聚合根实体
- [ ] 🔴 `JapanCustomsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [ ] 🔴 `JapanCustomsRepository` 领域接口
- [ ] 🔴 `JapanCustomsJpaRepository` JPA 适配器
- [ ] 🔴 `JapanCustomsAssembler` DTO 转换器
- [ ] 🔴 `JapanCustomsUseCase` 用例服务
- [ ] 🔴 `JapanCustomsController` REST 控制器
- [ ] 🔴 `@/api/japanCustoms.ts` 前端 API 客户端
- [ ] 🔴 `JapanCustomsPage.vue` 页面
- [ ] 🔴 `JapanCustomsUseCaseTest` 单元测试
