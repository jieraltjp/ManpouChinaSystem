# DB-01 — 补货需求数据库设计

> **版本**: 2.0.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v2.0.0：每条 Demand = 一个子货号，删 JSON 列，新增 quantity/destination/sub_product_code）
> **状态**: ✅ 已实现（代码 + 迁移脚本 V31）
> **业务步号**: 01（补货需求）
> **对应业务文档**: `SPEC-B01-补货需求-步骤1.md` v2.0.0
> **对应 UI 文档**: `docs/ui/pages/01-demand.md`
> **对应后端聚合根**: `ReplenishmentDemand`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `replenishment_demand` | ReplenishmentDemand | 🔲 待迁移 |

---

## 1. replenishment_demand（补货需求单，v2.0.0）

**对应**: `ReplenishmentDemand` 聚合根（v2.0.0）

```sql
CREATE TABLE replenishment_demand (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    demand_code           VARCHAR(32)  NOT NULL UNIQUE COMMENT '需求编号 D-YYYYMMDD-NNN',
    demand_type           VARCHAR(32)  NOT NULL COMMENT 'REPLENISHMENT(补货) / NEW_PURCHASE(新品采购)',
    product_code          VARCHAR(32)  NOT NULL COMMENT '主货号',
    sub_product_code      VARCHAR(64)  COMMENT '子货号全码（v2.0.0，如 ad009-be，商品唯一标识）',
    quantity              INT          COMMENT '需求数量（v2.0.0）',
    destination           VARCHAR(128) COMMENT '目的地（v2.0.0）',
    japan_lead            VARCHAR(64)  COMMENT '日本担当',
    status                VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / CONVERTED / CANCELLED',
    linked_procurement_id BIGINT       COMMENT '关联的 Procurement.id（v2.0.0，CONVERTED 时填充）',
    remarks               VARCHAR(512) COMMENT '备注',
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by             VARCHAR(64)  NOT NULL,
    update_by             VARCHAR(64)  NOT NULL,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE KEY demand_code (demand_code),
    INDEX idx_demand_product_code (product_code),
    INDEX idx_demand_status (status),
    INDEX idx_demand_type (demand_type),
    INDEX idx_demand_is_deleted (is_deleted)
);
```

> **v2.0.0 重大变更**：
> - 删除 `sub_product_code` 的 TEXT 类型（JSON 数组格式已废弃）
> - 删除 `linked_demand_items`（JSON 数组格式已废弃）
> - 新增 `sub_product_code`（VARCHAR 64，子货号全码，如 ad009-be）
> - 新增 `quantity`（INT，需求数量）
> - 新增 `destination`（VARCHAR 128，目的地）
> - 新增 `linked_procurement_id`（BIGINT，关联 Procurement.id，替代 linked_demand_items）
>
> **数据迁移策略**：旧数据（v1.6.0 JSON 数组格式）需执行迁移脚本，将每条 JSON 数组展开为多条记录。

---

## 字段映射

| 实体字段 | 数据库列 | 类型 | 说明 |
|---------|---------|------|------|
| id | `id` | BIGINT | 主键 |
| demandCode | `demand_code` | VARCHAR(32) | 格式 `D-YYYYMMDD-NNN` |
| demandType | `demand_type` | VARCHAR(32) | REPLENISHMENT / NEW_PURCHASE |
| productCode | `product_code` | VARCHAR(32) | 主货号 |
| subProductCode | `sub_product_code` | VARCHAR(64) | v2.0.0 子货号全码 |
| quantity | `quantity` | INT | v2.0.0 需求数量 |
| destination | `destination` | VARCHAR(128) | v2.0.0 目的地 |
| japanLead | `japan_lead` | VARCHAR(64) | 日本担当 |
| status | `status` | VARCHAR(32) | PENDING / CONVERTED / CANCELLED |
| linkedProcurementId | `linked_procurement_id` | BIGINT | v2.0.0 关联 Procurement.id |
| remarks | `remarks` | VARCHAR(512) | 备注 |
| createTime | `create_time` | DATETIME | — |
| updateTime | `update_time` | DATETIME | — |
| createBy | `create_by` | VARCHAR(64) | — |
| updateBy | `update_by` | VARCHAR(64) | — |
| isDeleted | `is_deleted` | BOOLEAN | 逻辑删除 |

---

## 代码实现状态

- [x] ✅ `ReplenishmentDemand` 聚合根实体（v2.0.0）
- [x] ✅ `DemandType` 枚举
- [x] ✅ `DemandStatus` 枚举
- [x] ✅ `ReplenishmentDemandRepository` 领域接口
- [x] ✅ `ReplenishmentDemandUseCase` 用例服务（v2.0.0：1:1 转采购）
- [x] ✅ `ReplenishmentDemandController` REST 控制器
- [x] ✅ `ReplenishmentDemandAssembler` DTO 转换器（v2.0.0：直接字段映射）
- [x] ✅ `ReplenishmentDemandUseCaseTest` 单元测试（v2.0.0）
- [x] ✅ `V31__demand_v2_schema.sql` 迁移脚本（删除 JSON 列，新增直接字段）
