# DB-05 — 国内报关数据库设计

> **版本**: 1.3.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v1.3.0：全量实现已确认，修正为 ✅ 已实现；修正代码文件名DomesticCustomsRepository/customs.ts/CustomsPage.vue）
> **更新**: 2026-04-24（修正状态：仅聚合根+枚举已实现，UseCase/Controller/前端均未实现，修正为🔴未实现）
> **更新**: 2026-04-23（对齐 V17 迁移 + 实体实现）
> **状态**: ✅ 已实现
> **业务步号**: 05（国内报关）
> **对应业务文档**: `SPEC-B00-全链路总览.md` · `SPEC-B05-国内报关-步骤5.md`
> **对应 UI 文档**: `docs/ui/pages/05-domestic-customs.md`
> **对应后端聚合根**: `DomesticCustomsRecord`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `domestic_customs_record` | DomesticCustomsRecord | ✅ 已实现 |

---

## 1. domestic_customs_record（国内报关）

**对应**: `DomesticCustomsRecord` 聚合根

```sql
CREATE TABLE domestic_customs_record (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    customs_code           VARCHAR(32) NOT NULL COMMENT '报关单号（DC-YYYYMMDD-NNN）',
    procurement_id         BIGINT COMMENT '关联发注单 FK → procurement.id',
    logistics_plan_id      BIGINT COMMENT '关联调配计划 FK → logistics_plan.id',
    factory_id             BIGINT COMMENT '关联工厂 FK → factory.id',
    product_code           VARCHAR(32) NOT NULL COMMENT '货号',
    sub_product_code       VARCHAR(64) COMMENT '子货号',
    quantity               INT COMMENT '报关数量',
    estimated_value_cny    DECIMAL(14,2) COMMENT '预估货值（元）',
    status                 VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SUBMITTED / CLEARED / REJECTED',
    remarks                VARCHAR(512) COMMENT '备注',
    create_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by              VARCHAR(64) NOT NULL,
    update_by              VARCHAR(64) NOT NULL,
    is_deleted             TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_domestic_customs_code (customs_code),
    INDEX idx_dc_procurement_id (procurement_id),
    INDEX idx_dc_logistics_plan_id (logistics_plan_id),
    INDEX idx_dc_status (status)
);
```

---

## 字段映射

| 实体字段 | 数据库列 | 状态 |
|---------|---------|------|
| id | `id` | ✅ |
| customsCode | `customs_code` | ✅ |
| procurementId | `procurement_id` | ✅ |
| logisticsPlanId | `logistics_plan_id` | ✅ |
| factoryId | `factory_id` | ✅ |
| productCode | `product_code` | ✅ |
| subProductCode | `sub_product_code` | ✅ |
| quantity | `quantity` | ✅ |
| estimatedValueCny | `estimated_value_cny` | ✅ |
| status | `status` | ✅ |
| remarks | `remarks` | ✅ |

---

## 代码实现状态

- [x] ✅ `DomesticCustomsRecord` 聚合根实体（含 submit/clear/reject 领域方法）
- [x] ✅ `DomesticCustomsStatus` 枚举
- [x] ✅ `DomesticCustomsRepository` 领域接口（含 @Repository，无分离 JPA Adapter）
- [ ] 🔴 单元测试
- [x] ✅ `CustomsAssembler` DTO 转换器
- [x] ✅ `CustomsUseCase` 用例服务
- [x] ✅ `CustomsController` REST 控制器（`/api/v1/customs`）
- [x] ✅ `@/api/customs.ts` 前端 API 客户端
- [x] ✅ `CustomsPage.vue` 前端页面

> ⚠️ 技术债务：`DomesticCustomsRepository` 领域接口带 `@Repository`，违反铁律25。
> 无 `XxxJpaAdapter` 分离，Repository 直接继承 `JpaRepository`。
> 建议：后续重构为 `DomesticCustomsRepository`（无 @Repository）+ `DomesticCustomsJpaAdapter`。
