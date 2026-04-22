# DB-01 — 补货需求数据库设计

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **业务步号**: 01（补货需求）
> **对应业务文档**: `SPEC-B01-全链路总览.md` §第一步
> **对应 UI 文档**: `docs/ui/pages/01-demand.md`
> **对应后端聚合根**: `ReplenishmentDemand`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `replenishment_demand` | ReplenishmentDemand | ✅ 已实现 |

---

## 1. replenishment_demand（补货需求单）

**对应**: `ReplenishmentDemand` 聚合根

```sql
CREATE TABLE replenishment_demand (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    demand_code     VARCHAR(32)  NOT NULL UNIQUE COMMENT '需求编号 D-YYYYMMDD-NNN',
    demand_type     VARCHAR(20)  NOT NULL COMMENT 'REPLENISHMENT(补货) / NEW_PURCHASE(新品采购)',
    product_code    VARCHAR(32)  NOT NULL COMMENT '主货号',
    sub_product_code VARCHAR(64) COMMENT '子货号/颜色（如 re=红色）',
    quantity        INT          NOT NULL COMMENT '需求量',
    destination     VARCHAR(128) COMMENT '目的地',
    japan_lead      VARCHAR(64)  COMMENT '日本担当',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / CONVERTED / CANCELLED',
    linked_procurement_id BIGINT COMMENT '关联的采购单ID（status=CONVERTED 时赋值）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       VARCHAR(64)  NOT NULL,
    update_by       VARCHAR(64)  NOT NULL,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    INDEX idx_demand_product (product_code),
    INDEX idx_demand_status (status),
    INDEX idx_demand_type (demand_type)
);
```

---

## 字段映射

| 实体字段 | 数据库列 | 类型 | 说明 |
|---------|---------|------|------|
| id | `id` | BIGINT | 主键 |
| demandCode | `demand_code` | VARCHAR(32) | 格式 `D-YYYYMMDD-NNN` |
| demandType | `demand_type` | VARCHAR(20) | REPLENISHMENT / NEW_PURCHASE |
| productCode | `product_code` | VARCHAR(32) | 主货号 |
| subProductCode | `sub_product_code` | VARCHAR(64) | 子货号 |
| quantity | `quantity` | INT | 需求量 |
| destination | `destination` | VARCHAR(128) | 目的地 |
| japanLead | `japan_lead` | VARCHAR(64) | 日本担当 |
| status | `status` | VARCHAR(20) | PENDING / CONVERTED / CANCELLED |
| linkedProcurementId | `linked_procurement_id` | BIGINT | 关联采购单 |
| createTime | `create_time` | DATETIME | — |
| updateTime | `update_time` | DATETIME | — |
| createBy | `create_by` | VARCHAR(64) | — |
| updateBy | `update_by` | VARCHAR(64) | — |
| isDeleted | `is_deleted` | BOOLEAN | 逻辑删除 |

---

## 代码实现状态

- [x] ✅ `ReplenishmentDemand` 聚合根实体
- [x] ✅ `DemandType` 枚举
- [x] ✅ `DemandStatus` 枚举
- [x] ✅ `ReplenishmentDemandRepository` 领域接口
- [x] ✅ `ReplenishmentDemandUseCase` 用例服务
- [x] ✅ `ReplenishmentDemandController` REST 控制器
- [x] ✅ `ReplenishmentDemandAssembler` DTO 转换器
- [ ] 🔴 `ReplenishmentDemandUseCaseTest` 单元测试（待补充）
