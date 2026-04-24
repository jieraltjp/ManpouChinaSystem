# DB-01 — 补货需求数据库设计

> **版本**: 1.3.0
> **创建**: 2026-04-22
> **更新**: 2026-04-24（v1.3.0：同步 v1.6.0 schema — sub_product_code 改为 TEXT JSON 数组；quantity/destination/linked_procurement_id 已移除；新增 linked_demand_items TEXT）
> **状态**: ✅ 已实现
> **业务步号**: 01（补货需求）
> **对应业务文档**: `SPEC-B00-全链路总览.md` §第一步
> **对应 UI 文档**: `docs/ui/pages/01-demand.md`
> **对应后端聚合根**: `ReplenishmentDemand`
> **对应设计文档**: `docs/design/FEATURE-货号自动补全与多子货号选择.md`

---

## 表清单

| 序号 | 表名 | 聚合根 | 状态 |
|------|------|--------|------|
| 1 | `replenishment_demand` | ReplenishmentDemand | ✅ 已实现 |

---

## 1. replenishment_demand（补货需求单）

**对应**: `ReplenishmentDemand` 聚合根（v1.6.0）

```sql
CREATE TABLE replenishment_demand (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    demand_code     VARCHAR(32)  NOT NULL UNIQUE COMMENT '需求编号 D-YYYYMMDD-NNN',
    demand_type     VARCHAR(32)  NOT NULL COMMENT 'REPLENISHMENT(补货) / NEW_PURCHASE(新品采购)',
    product_code    VARCHAR(32)  NOT NULL COMMENT '主货号',
    sub_product_code TEXT COMMENT '子货号明细（v1.6.0，JSON数组）如：[{"subCode":"be","quantity":100,"destination":"久留米"},...]',
    japan_lead     VARCHAR(64)  COMMENT '日本担当',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / CONVERTED / CANCELLED',
    linked_demand_items TEXT COMMENT '关联发注表明细（v1.6.0，JSON数组）如：[{"linkedProcurementId":101,"subCode":"be"},...]',
    remarks         VARCHAR(512) COMMENT '备注',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       VARCHAR(64)  NOT NULL,
    update_by       VARCHAR(64)  NOT NULL,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE KEY demand_code (demand_code),
    INDEX idx_demand_product_code (product_code),
    INDEX idx_demand_status (status),
    INDEX idx_demand_type (demand_type),
    INDEX idx_demand_is_deleted (is_deleted)
);
```

> **v1.6.0 重大变更**：`quantity` / `destination` / `linked_procurement_id` 列已删除。
> 子货号数量/目的地信息迁移至 `sub_product_code` JSON 数组，每条子货号独立数量和目的地。
> 关联关系迁移至 `linked_demand_items` JSON 数组，追踪每条子货号对应的 Procurement。
> 旧数据（v1.5.x）`sub_product_code` 格式为 `["be","bu"]` 时，Assembler 层兼容解析。

---

## 字段映射

| 实体字段 | 数据库列 | 类型 | 说明 |
|---------|---------|------|------|
| id | `id` | BIGINT | 主键 |
| demandCode | `demand_code` | VARCHAR(32) | 格式 `D-YYYYMMDD-NNN` |
| demandType | `demand_type` | VARCHAR(32) | REPLENISHMENT / NEW_PURCHASE |
| productCode | `product_code` | VARCHAR(32) | 主货号 |
| subProductItemsRaw | `sub_product_code` | TEXT | v1.6.0 JSON 数组；旧数据兼容解析 |
| japanLead | `japan_lead` | VARCHAR(64) | 日本担当 |
| status | `status` | VARCHAR(32) | PENDING / CONVERTED / CANCELLED |
| linkedDemandItemsRaw | `linked_demand_items` | TEXT | v1.6.0 JSON 数组 |
| remarks | `remarks` | VARCHAR(512) | 备注 |
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
- [x] ✅ `V7__demand_sub_product_extend.sql` 迁移脚本（sub_product_code VARCHAR(64) → VARCHAR(512)）
- [x] ✅ `ReplenishmentDemandUseCaseTest` 单元测试（v1.6.0 已补充）
- [x] ✅ `V27__demand_json_columns_text.sql`（sub_product_code → TEXT）
- [x] ✅ `V28__demand_v1_6_schema.sql`（v1.6.0 最终正确结构文档）
