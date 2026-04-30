# DB-06 — 日本清关数据库设计

> **版本**: 1.4.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v1.2.0：补充 sub_product_code 列，全链路子货号追踪完整）
> **更新**: 2026-04-30（v1.4.0：**container_no 为主键字段 + product_code + factory_id 列，V44 已创建**）
> **状态**: ✅ 已实现（v1.4.0 container_no + product_code + factory_id 已实现，V44 迁移待执行）
> **业务步号**: 06（日本清关）
> **对应业务文档**: `SPEC-B00-全链路总览.md` · `SPEC-B06-日本清关-步骤6.md`
> **对应 UI 文档**: `docs/ui/pages/06-japan-customs.md`
> **对应后端聚合根**: `JapanCustomsRecord`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `japan_customs_record` | JapanCustomsRecord | 🔧 改造中（V44 待执行） |

---

## 1. japan_customs_record（日本清关）

**对应**: `JapanCustomsRecord` 聚合根

**v1.4.0 变更**：`container_no` + `product_code` 列新增（V44 迁移）

```sql
CREATE TABLE japan_customs_record (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    customs_entry_no      VARCHAR(64) COMMENT '入境报关号（JC-YYYYMMDD-NNN）',
    container_no          VARCHAR(32) DEFAULT NULL COMMENT '货柜号（v1.4.0，必填）',  -- V44 新增
    domestic_customs_id   BIGINT COMMENT '关联国内报关单 FK → domestic_customs_record.id',
    logistics_plan_id    BIGINT COMMENT '关联调配计划 FK → logistics_plan.id',
    procurement_id        BIGINT COMMENT '关联采购单 FK → procurement.id（v1.4.0 改为可选参考）',
    factory_id            BIGINT COMMENT '关联工厂（v1.4.0 新增）',                    -- V44 新增
    product_code         VARCHAR(32) COMMENT '货号（v1.4.0 新增）',                  -- V44 新增
    sub_product_code      VARCHAR(64) COMMENT '子货号/颜色（v1.6.1 全链路追踪）',
    status               VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / IN_PROGRESS / CLEARED / FAILED',
    arrival_date         DATE COMMENT '到达日期',
    customs_broker       VARCHAR(128) COMMENT '清关行',
    broker_phone         VARCHAR(32) COMMENT '清关行电话',
    broker_contact       VARCHAR(64) COMMENT '清关行联系人',
    import_duty_paid     DECIMAL(14,2) COMMENT '进口关税（JPY）',
    consumption_tax_paid DECIMAL(14,2) COMMENT '消费税（JPY）',
    clearance_date       DATE COMMENT '清关完成日期',
    arrival_port         VARCHAR(64) COMMENT '目的港（来自 logistics_plan）',
    declared_weight_kg  DECIMAL(10,3) COMMENT '申报重量（来自 logistics_plan）',
    declared_volume_cbm  DECIMAL(10,4) COMMENT '申报体积（来自 logistics_plan）',
    remarks              VARCHAR(512),
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by            VARCHAR(64) NOT NULL,
    update_by            VARCHAR(64) NOT NULL,
    is_deleted           BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_jp_customs_entry_no (customs_entry_no),
    INDEX idx_jp_container_no (container_no),          -- V44 新增
    INDEX idx_jp_procurement_id (procurement_id),
    INDEX idx_jp_domestic_customs_id (domestic_customs_id),
    INDEX idx_jp_logistics_plan_id (logistics_plan_id),
    INDEX idx_jp_status (status),
    INDEX idx_jp_create_time (create_time),
    INDEX idx_jp_is_deleted (is_deleted)
);
```

---

## 待执行迁移

| 序号 | 文件 | 内容 | 状态 |
|------|------|------|------|
| V44 | `V44__japan_customs_container_no.sql` | japan_customs_record 增加 container_no + product_code + factory_id 列 + 索引 | 🔴 待执行 |

---

## 字段映射

| 实体字段 | 数据库列 | 状态 | 说明 |
|---------|---------|------|------|
| id | `id` | ✅ | |
| customsEntryNo | `customs_entry_no` | ✅ | |
| containerNo | `container_no` | 🔧 V44 新增 | v1.4.0 必填 |
| domesticCustomsId | `domestic_customs_id` | ✅ | |
| logisticsPlanId | `logistics_plan_id` | ✅ | |
| procurementId | `procurement_id` | ✅（允许 NULL） | v1.4.0 改为可选参考 |
| factoryId | `factory_id` | 🔧 V44 新增 | v1.4.0 新增 |
| productCode | `product_code` | 🔧 V44 新增 | v1.4.0 新增 |
| subProductCode | `sub_product_code` | ✅（v1.6.1 新增） | |
| status | `status` | ✅ | |
| customsEntryNo | `customs_entry_no` | ✅ | |
| arrivalDate | `arrival_date` | ✅ | |
| customsBroker | `customs_broker` | ✅ | |
| brokerPhone | `broker_phone` | ✅ | |
| brokerContact | `broker_contact` | ✅ | |
| importDutyPaid | `import_duty_paid` | ✅ | |
| consumptionTaxPaid | `consumption_tax_paid` | ✅ | |
| clearanceDate | `clearance_date` | ✅ | |
| arrivalPort | `arrival_port` | ✅ | |
| declaredWeightKg | `declared_weight_kg` | ✅ | |
| declaredVolumeCbm | `declared_volume_cbm` | ✅ | |

---

## 代码实现状态

- [x] ✅ `JapanCustomsRecord` 聚合根实体（含 containerNo + productCode + factoryId v1.4.0）
- [x] ✅ `JapanCustomsStatus` 枚举（含 `isTerminal()`）
- [x] ✅ `JapanCustomsRepository` 领域接口
- [x] ✅ `JapanCustomsAssembler` DTO 转换器（含 containerNo 映射 v1.4.0）
- [x] ✅ `JapanCustomsUseCase` 用例服务（含 containerNo 过滤 v1.4.0）
- [x] ✅ `JapanCustomsController` REST 控制器
- [x] ✅ `@/api/japanCustoms.ts` 前端 API 客户端（含 containerNo v1.4.0）
- [x] ✅ `JapanCustomsRecordPage.vue` 前端页面（含 containerNo 筛选/列/新建 v1.4.0）
- [x] ✅ `OrderOverviewUseCase` 已集成 JapanCustomsRecord（步骤6）
- [x] ✅ DB迁移脚本 `V12__japan_customs_record_table.sql`
- [x] ✅ DB迁移脚本 `V29__japan_customs_sub_product_code.sql`（v1.6.1 新增 sub_product_code 列）
- [x] ✅ DB迁移脚本 `V44__japan_customs_container_no.sql`（v1.4.0 新增 container_no + product_code + factory_id 列 + 索引）
- [ ] 🔴 `JapanCustomsUseCaseTest` 单元测试
