# DB-06 — 日本清关数据库设计

> **版本**: 1.2.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v1.2.0：补充 sub_product_code 列，全链路子货号追踪完整）
> **状态**: ✅ 已实现
> **业务步号**: 06（日本清关）
> **对应业务文档**: `SPEC-B00-全链路总览.md` · `SPEC-B06-日本清关-步骤6.md`
> **对应 UI 文档**: `docs/ui/pages/06-japan-customs.md`
> **对应后端聚合根**: `JapanCustomsRecord`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `japan_customs_record` | JapanCustomsRecord | ✅ 已实现 |

---

## 1. japan_customs_record（日本清关）🔴占位

> ⚠️ 部分字段为占位，待业务方提供真实清关文件样本后确认。

**对应**: `JapanCustomsRecord` 聚合根

```sql
CREATE TABLE japan_customs_record (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    customs_entry_no      VARCHAR(32) COMMENT '入境报关号（JC-YYYYMMDD-NNN）',
    procurement_id        BIGINT COMMENT '关联采购单 FK → procurement.id（可为空）',
    domestic_customs_id  BIGINT COMMENT '关联国内报关单 FK → domestic_customs_record.id',
    logistics_plan_id     BIGINT COMMENT '关联调配计划 FK → logistics_plan.id',
    sub_product_code     VARCHAR(64) COMMENT '子货号/颜色（来自 Procurement，v1.6.1 全链路追踪）',
    status                VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / IN_PROGRESS / CLEARED / FAILED',
    arrival_date          DATE COMMENT '到达日期',
    customs_broker        VARCHAR(128) COMMENT '清关行',
    broker_phone          VARCHAR(32) COMMENT '清关行电话',
    broker_contact        VARCHAR(64) COMMENT '清关行联系人',
    import_duty_paid      DECIMAL(14,2) COMMENT '进口关税（JPY）',
    consumption_tax_paid DECIMAL(14,2) COMMENT '消费税（JPY）',
    clearance_date        DATE COMMENT '清关完成日期',
    arrival_port          VARCHAR(64) COMMENT '目的港（来自 logistics_plan）',
    declared_weight_kg   DECIMAL(10,3) COMMENT '申报重量（来自 logistics_plan）',
    declared_volume_cbm   DECIMAL(10,4) COMMENT '申报体积（来自 logistics_plan）',
    remarks               VARCHAR(512),
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by             VARCHAR(64) NOT NULL,
    update_by             VARCHAR(64) NOT NULL,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY idx_jp_customs_entry_no (customs_entry_no),
    INDEX idx_jp_procurement_id (procurement_id),
    INDEX idx_jp_domestic_customs_id (domestic_customs_id),
    INDEX idx_jp_logistics_plan_id (logistics_plan_id),
    INDEX idx_jp_status (status),
    INDEX idx_jp_create_time (create_time),
    INDEX idx_jp_is_deleted (is_deleted)
);
```

---

## 字段映射

| 实体字段 | 数据库列 | 状态 |
|---------|---------|------|
| id | `id` | ✅ |
| customsEntryNo | `customs_entry_no` | ✅ |
| procurementId | `procurement_id` | ✅（允许 NULL） |
| domesticCustomsId | `domestic_customs_id` | ✅ |
| logisticsPlanId | `logistics_plan_id` | ✅ |
| subProductCode | `sub_product_code` | ✅（v1.6.1 新增） |
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

- [x] ✅ `JapanCustomsRecord` 聚合根实体
- [x] ✅ `JapanCustomsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [x] ✅ `JapanCustomsRepository` 领域接口
- [x] ✅ `JapanCustomsJpaRepository` JPA 适配器
- [x] ✅ `JapanCustomsAssembler` DTO 转换器
- [x] ✅ `JapanCustomsUseCase` 用例服务
- [x] ✅ `JapanCustomsController` REST 控制器
- [x] ✅ `V29__japan_customs_sub_product_code.sql`（v1.6.1 新增 sub_product_code 列，全链路追踪完整）
