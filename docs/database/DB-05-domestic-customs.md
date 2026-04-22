# DB-05 — 国内报关数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: 🔴 占位（字段待确认）
> **业务步号**: 05（国内报关）
> **对应业务文档**: `SPEC-B01-全链路总览.md` §第五步 · `SPEC-国内报关-步骤5.md`
> **对应 UI 文档**: `docs/ui/pages/07-domestic-customs.md`
> **对应后端聚合根**: `DomesticCustomsRecord`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `domestic_customs_record` | DomesticCustomsRecord | 🔴 占位 |

---

## 1. domestic_customs_record（国内报关）🔴占位

> ⚠️ 字段为占位，待业务方提供真实报关单样本后确认。

**对应**: `DomesticCustomsRecord` 聚合根

```sql
-- TODO: 字段待业务方确认后补充完整
CREATE TABLE domestic_customs_record (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    customs_declaration_no VARCHAR(64) COMMENT '报关单号（系统生成/海关返回？）',
    procurement_id         BIGINT NOT NULL COMMENT '关联采购单 FK → procurement.id',
    logistics_plan_id      BIGINT COMMENT '关联调配计划 FK → logistics_plan.id',
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SUBMITTED / CLEARED / FAILED',
    hs_code                VARCHAR(20) COMMENT 'HS编码（来自 product.hs_code？）',
    product_code           VARCHAR(32) COMMENT '货号',
    product_name           VARCHAR(128) COMMENT '商品名称（来自 product.name_zh）',
    declaration_date       DATE COMMENT '申报日期',
    declarant             VARCHAR(64) COMMENT '申报人',
    export_port            VARCHAR(64) COMMENT '出口口岸（宁波/上海/大连/天津/其他）',
    declared_value_rmb    DECIMAL(14,2) COMMENT '申报价值（CNY）',
    gross_weight_kg       DECIMAL(10,4) COMMENT '毛重(kg)',
    quantity               INT COMMENT '数量',
    inspection_result      VARCHAR(256) COMMENT '商检结果',
    remarks                VARCHAR(512),
    create_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by              VARCHAR(64) NOT NULL,
    update_by              VARCHAR(64) NOT NULL,
    is_deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_dc_procurement (procurement_id),
    INDEX idx_dc_status (status),
    INDEX idx_dc_declaration_no (customs_declaration_no)
);
```

---

## 字段映射（占位）

| 实体字段 | 数据库列 | 状态 |
|---------|---------|------|
| id | `id` | ✅ |
| customsDeclarationNo | `customs_declaration_no` | 🔴待确认 |
| procurementId | `procurement_id` | ✅ |
| logisticsPlanId | `logistics_plan_id` | ✅ |
| status | `status` | ✅ |
| hsCode | `hs_code` | 🔴待确认 |
| productCode | `product_code` | ✅ |
| productName | `product_name` | 🔴待确认（来自Product?） |
| declarationDate | `declaration_date` | ✅ |
| declarant | `declarant` | ✅ |
| exportPort | `export_port` | 🔴待确认枚举值 |
| declaredValueRmb | `declared_value_rmb` | ✅ |
| grossWeightKg | `gross_weight_kg` | ✅ |
| quantity | `quantity` | ✅ |
| inspectionResult | `inspection_result` | 🔴待确认（商检流程是否独立？） |

---

## 代码实现状态

- [ ] 🔴 `DomesticCustomsRecord` 聚合根实体
- [ ] 🔴 `DomesticCustomsStatus` 枚举（含 `isTerminal()` + `canTransitionTo()`）
- [ ] 🔴 `DomesticCustomsRepository` 领域接口
- [ ] 🔴 `DomesticCustomsJpaRepository` JPA 适配器
- [ ] 🔴 `DomesticCustomsAssembler` DTO 转换器
- [ ] 🔴 `DomesticCustomsUseCase` 用例服务
- [ ] 🔴 `DomesticCustomsController` REST 控制器
- [ ] 🔴 `@/api/domesticCustoms.ts` 前端 API 客户端
- [ ] 🔴 `DomesticCustomsPage.vue` 页面
- [ ] 🔴 `DomesticCustomsUseCaseTest` 单元测试
