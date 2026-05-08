# DB-03 — 验货记录数据库设计

> **版本**: 1.2.0
> **创建**: 2026-04-22
> **更新**: 2026-05-08（v1.2.0：索引名称与 V15 实际迁移对齐，移除不存在的 idx_qc_result/is_deleted，新增 idx_qc_product_code/create_time）
> **状态**: ✅ 已实现
> **业务步号**: 03（验货记录）
> **对应业务文档**: `SPEC-B00-全链路总览.md` §第三步
> **对应 UI 文档**: `docs/ui/pages/03-inspection.md`
> **对应后端聚合根**: `QcRecord`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `qc_record` | QcRecord | ✅ 已实现 |

---

## 1. qc_record（验货记录）

**对应**: `QcRecord` 聚合根

```sql
CREATE TABLE qc_record (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    qc_code             VARCHAR(32)  NOT NULL UNIQUE COMMENT '验货编号 Q-YYYYMMDD-NNN',
    procurement_id      BIGINT      NOT NULL COMMENT '关联采购单 FK → procurement.id',
    seller_name         VARCHAR(128) COMMENT '卖家名称（来自 factory.factory_name）',
    product_code        VARCHAR(32)  NOT NULL COMMENT '货号',
    sub_product_code    VARCHAR(64) COMMENT '子货号/颜色',
    qc_user_id         BIGINT COMMENT '验货负责人 FK → system_user.id',
    qc_type            VARCHAR(16) COMMENT 'ONSITE(仓库验货) / REMOTE(现场异地验货)',
    qc_date            DATE COMMENT '验货日期',
    result             VARCHAR(16)  NOT NULL DEFAULT 'PASS' COMMENT 'PASS / FAIL',
    status             VARCHAR(24)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / COMPLETED / RETURN_REQUESTED',
    inspection_count   INT COMMENT '检品数',
    passed_count       INT COMMENT '合格数量',
    defective_count     INT COMMENT '不良数量（自动计算）',
    box_count          INT COMMENT '箱数',
    box_length_cm      DECIMAL(8,2) COMMENT '箱子长(cm)',
    box_width_cm       DECIMAL(8,2) COMMENT '箱子宽(cm)',
    box_height_cm      DECIMAL(8,2) COMMENT '箱子高(cm)',
    net_weight_per_unit DECIMAL(10,4) COMMENT '单个净重(kg)',
    gross_weight       DECIMAL(10,4) COMMENT '毛重(kg)',
    tax_inclusive_price DECIMAL(14,2) COMMENT '含税价（元）',
    material           VARCHAR(64) COMMENT '材质',
    qc_standard        VARCHAR(512) COMMENT '验收标准',
    remarks            VARCHAR(512) COMMENT '备注',
    images             JSON COMMENT '缺陷照片URL列表',
    destination        VARCHAR(128) COMMENT '目的地',
    quantity           INT COMMENT '订购数量',
    order_date         DATE COMMENT '下单日',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by          VARCHAR(64)  NOT NULL,
    update_by          VARCHAR(64)  NOT NULL,
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_qc_code (qc_code),
    INDEX idx_qc_procurement_id (procurement_id),
    INDEX idx_qc_product_code (product_code),
    INDEX idx_qc_status (status),
    INDEX idx_qc_date (qc_date),
    INDEX idx_qc_create_time (create_time)
);
```

---

## 字段映射

| 实体字段 | 数据库列 | 说明 |
|---------|---------|------|
| id | `id` | 主键 |
| qcCode | `qc_code` | 格式 `Q-YYYYMMDD-NNN` |
| procurementId | `procurement_id` | FK → procurement.id |
| sellerName | `seller_name` | 来自 factory.factory_name |
| productCode | `product_code` | 货号 |
| subProductCode | `sub_product_code` | 子货号 |
| qcUserId | `qc_user_id` | 验货负责人 |
| qcType | `qc_type` | ONSITE / REMOTE |
| qcDate | `qc_date` | 验货日期 |
| result | `result` | PASS / FAIL |
| status | `status` | PENDING / COMPLETED / RETURN_REQUESTED |
| inspectionCount | `inspection_count` | 检品数 |
| passedCount | `passed_count` | 合格数量 |
| defectiveCount | `defective_count` | 自动计算 |
| boxCount | `box_count` | 箱数 |
| boxLengthCm | `box_length_cm` | 箱子长 |
| boxWidthCm | `box_width_cm` | 箱子宽 |
| boxHeightCm | `box_height_cm` | 箱子高 |
| netWeightPerUnit | `net_weight_per_unit` | 单个净重 |
| grossWeight | `gross_weight` | 毛重 |
| taxInclusivePrice | `tax_inclusive_price` | 含税价 |
| material | `material` | 材质 |
| qcStandard | `qc_standard` | 验收标准 |
| remarks | `remarks` | 备注 |
| images | `images` | JSON 数组 |
| destination | `destination` | 目的地 |
| quantity | `quantity` | 订购数量 |
| orderDate | `order_date` | 下单日 |
| createTime / updateTime | `create_time / update_time` | 审计字段 |
| isDeleted | `is_deleted` | 逻辑删除 |

---

## 代码实现状态

- [x] ✅ `QcRecord` 聚合根实体（含 `calculateDefectiveCount()`）
- [x] ✅ `QcStatus` 枚举（含 `isTerminal()` + `canTransitionTo()` + FSM map）
- [x] ✅ `QcResult` 枚举
- [x] ✅ `QcType` 枚举
- [x] ✅ `QcRecordRepository` 领域接口
- [x] ✅ `QcRecordJpaRepository` JPA 适配器
- [x] ✅ `QcRecordUseCase` 用例服务
- [x] ✅ `QcRecordController` REST 控制器
- [x] ✅ `QcRecordAssembler` DTO 转换器
- [x] ✅ `QcRecordUseCaseTest` 单元测试（10 个用例，全部通过）
- [x] ✅ `@/api/inspection.ts` 前端 API 客户端
- [x] ✅ `QcRecordPage.vue` 页面（已对接真实 API）
