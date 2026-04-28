# DB-13 — 订单快照表设计（procurement_snapshot）

> **版本**: 1.0.0
> **创建**: 2026-04-28
> **状态**: 🔄 设计中
> **关联文档**: `DB-09-order-overview.md` · `DB-02-procurement-order.md` · `DB-10-factory.md` · `DB-11-product.md`

---

## 1. 问题背景

**为什么需要快照？**

`v_order_chain` 视图需要展示"下单时刻"的工厂和商品信息（工厂名称、商品名称、商品分类）。

但 `factory` 表和 `product` 表的实时数据会随业务变化：

| 场景 | 变化前 | 变化后 | 视图结果 |
|------|--------|--------|----------|
| 工厂改名 | "义乌XX家具厂" | "义乌YY家具厂" | 历史订单工厂名全部变成 YY |
| 商品名改 | "木制椅子" | "木制餐椅" | 历史订单商品名全部变成 餐椅 |
| 商品分类改 | OEM | ORDINARY | 历史订单分类全部变成 ORDINARY |

因此，**工厂和商品在采购下单时刻的信息必须快照**，与实时表分离。

---

## 2. 设计方案

### 2.1 核心思路

在 `order` 模块新建 `procurement_snapshot` 表（采购快照），在 **创建发注单时自动填入当前工厂+商品信息**，同时 **允许事后修改**。

`v_order_chain` 视图 LEFT JOIN `procurement_snapshot` 而非实时 `factory` / `product` 表。

### 2.2 数据聚合关系

```
replenishment_demand（锚点）
  └── linked_procurement_id → procurement.id
                              └── procurement_snapshot.procurement_id（1:1，快照表）
                                                    ├── factory_snapshot 字段（工厂下单时信息）
                                                    └── product_snapshot 字段（商品下单时信息）
```

### 2.3 procurement_snapshot 表结构

```sql
CREATE TABLE procurement_snapshot (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    procurement_id       BIGINT NOT NULL UNIQUE COMMENT '对应发注单 FK → procurement.id',

    -- ====== 工厂快照（下单时刻）======
    factory_id            BIGINT COMMENT '工厂 ID（实时关联，可变化）',
    factory_code          VARCHAR(32) COMMENT '工厂编号（下单时刻）',
    factory_name          VARCHAR(128) COMMENT '工厂名称（下单时刻）',
    factory_province      VARCHAR(64) COMMENT '工厂省份（下单时刻）',
    factory_city          VARCHAR(64) COMMENT '工厂城市（下单时刻）',
    factory_contact_name  VARCHAR(64) COMMENT '工厂联系人（下单时刻）',
    factory_contact_phone VARCHAR(32) COMMENT '工厂电话（下单时刻）',

    -- ====== 商品快照（下单时刻）======
    product_name_zh      VARCHAR(255) COMMENT '商品中文名（下单时刻）',
    product_name_ja       VARCHAR(128) COMMENT '商品日文名（下单时刻）',
    product_category      VARCHAR(32) COMMENT '商品分类（下单时刻：OEM/ORDINARY/FACTORY_DIRECT）',

    -- ====== 元数据 ======
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '发注单快照——记录下单时刻的工厂和商品信息，保证历史订单数据不变';
```

### 2.4 索引

```sql
CREATE UNIQUE INDEX idx_snapshot_proc ON procurement_snapshot(procurement_id);
CREATE INDEX idx_snapshot_factory ON procurement_snapshot(factory_id);
```

---

## 3. 快照填充时机与规则

### 3.1 自动填入（创建发注单时）

在 `ProcurementUseCase.create()` 中：

```java
// 1. 查询当前工厂信息
Factory factory = factoryRepository.findById(cmd.getFactoryId());
// 2. 查询当前商品信息
Product product = productRepository.findByMasterCode(cmd.getProductCode());

// 3. 创建快照
ProcurementSnapshot snapshot = ProcurementSnapshot.builder()
    .procurementId(procurement.getId())
    .factoryId(factory.getId())
    .factoryCode(factory.getFactoryCode())
    .factoryName(factory.getFactoryName())
    .factoryProvince(factory.getProvince())
    .factoryCity(factory.getCity())
    .factoryContactName(factory.getContactName())
    .factoryContactPhone(factory.getContactPhone())
    .productNameZh(product.getNameZh())
    .productNameJa(product.getNameJa())
    .productCategory(product.getCategory() != null ? product.getCategory().name() : null)
    .build();
snapshotRepository.save(snapshot);
```

### 3.2 允许修改

用户可在发注单编辑页面修改工厂/商品快照字段（名称、地址等），修改后更新 `procurement_snapshot`，不影响 `factory` / `product` 实时表。

### 3.3 查询展示

`ProcurementService` 提供 `getSnapshot(Long procurementId)` 接口，返回快照数据供前端展示。

---

## 4. VIEW 改造（DB-09）

`v_order_chain_v1` 视图改造：移除 `factory` / `product` JOIN，改为 LEFT JOIN `procurement_snapshot`。

```sql
-- 改造前（实时表 JOIN，会随变化而变化）
LEFT JOIN factory f ON f.id = p.factory_id
LEFT JOIN product prd ON prd.master_code = d.product_code ...

-- 改造后（快照表，固定不变）
LEFT JOIN procurement_snapshot sn ON sn.procurement_id = p.id
```

### 改造后 v_order_chain_v1 字段映射

| 旧来源 | 新来源 |
|--------|--------|
| `f.factory_code` | `sn.factory_code` |
| `f.factory_name` | `sn.factory_name` |
| `f.province` | `sn.factory_province` |
| `f.city` | `sn.factory_city` |
| `f.contact_name` | `sn.factory_contact_name` |
| `f.contact_phone` | `sn.factory_contact_phone` |
| `prd.name_zh` | `sn.product_name_zh` |
| `prd.name_ja` | `sn.product_name_ja` |
| `prd.category` | `sn.product_category` |

---

## 5. 实现计划

| # | 项目 | 状态 | 说明 |
|---|------|------|------|
| 1 | `procurement_snapshot` 建表 SQL | 🔲 待建 | |
| 2 | `ProcurementSnapshot` JPA Entity | 🔲 待建 | order/domain/model |
| 3 | `ProcurementSnapshotRepository` | 🔲 待建 | |
| 4 | `ProcurementUseCase` 创建时自动填充快照 | 🔲 待改 | |
| 5 | `ProcurementUpdateCmd` / Assembler 支持编辑快照字段 | 🔲 待改 | |
| 6 | `v_order_chain_v1` 视图改造（替换为快照表 JOIN） | 🔲 待改 | |
| 7 | 前端发注单编辑页支持修改快照字段 | 🔲 待改 | |
| 8 | DB-09 文档同步更新 | 🔲 待改 | |

---

## 6. 已有数据迁移

已有 `procurement` 记录需要回填 `procurement_snapshot`：

```sql
INSERT INTO procurement_snapshot (procurement_id, factory_id, factory_code, factory_name,
  factory_province, factory_city, factory_contact_name, factory_contact_phone,
  product_name_zh, product_name_ja, product_category)
SELECT
  p.id,
  f.id,
  f.factory_code,
  f.factory_name,
  f.province,
  f.city,
  f.contact_name,
  f.contact_phone,
  prd.name_zh,
  prd.name_ja,
  prd.category
FROM procurement p
LEFT JOIN factory f ON f.id = p.factory_id AND f.is_deleted = FALSE
LEFT JOIN product prd ON prd.master_code = p.product_code AND prd.sub_code IS NULL AND prd.is_deleted = FALSE
WHERE NOT EXISTS (
  SELECT 1 FROM procurement_snapshot sn WHERE sn.procurement_id = p.id
);
```
