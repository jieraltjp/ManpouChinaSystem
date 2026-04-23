# SPEC-B10 — 商品目录业务规格（商品类）

> **版本**: 1.4.0
> **更新**: 2026-04-23（v1.4.0：新增7个字段 hs_code_jp/jan_code/status/quantities/carton_qty/amount_rmb/material_ja）
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **业务步号**: 00（基础设施/商品目录）
> **对应数据库文档**: `docs/database/DB-11-product.md`
> **对应 UI 文档**: `docs/ui/pages/10-product.md`
> **前置步骤**: 无（商品目录是所有业务步的基础数据）

---

## 1. 业务定位

商品目录是整个采购链路的基础数据层。商品（Product）在步骤1被查询/选择，在步骤2被关联，在步骤5/6被用于报关，在步骤8被用于销售记录。

**设计原则**：
- 商品是主数据（Master Data），与业务流分离维护
- 一个商品可有多个颜色变体（子货号），共享主货号
- 一个商品可由多个工厂生产（多对多），通过 `product_factory` 关联表维护

---

## 2. 实体设计

### 2.1 Product（商品聚合根）

```
Product（聚合根）
├── id: Long
├── masterCode: String              # 主货号（如 odn012）
├── subCode: String                 # 子货号/色号（如 re=红色，可为空）
├── janCode: String                # JANコード（出货单位标记，来自 goods_master.JANコード）
│
├── 名称
├── nameZh: String                  # 中文名称（中国用）
├── nameEn: String                  # 英文名称（报关用）
├── nameJa: String                  # 日文名称（日本用）
├── imageUrl: String                # 商品图片 URL
│
├── 分类
├── category: ProductCategory       # OEM / ORDINARY / FACTORY_DIRECT
├── status: String                  # 商品区分（通常/予約，来自 goods_master.商品区分）
├── colorName: String               # 颜色名称
│
├── 材质
├── material: String                # 材质（中文）
├── materialJa: String              # 材质（日文，来自 DB.json）
│
├── 来源
├── origin: String                  # 原产国（来自 DB.json / factory.origin）
├── warehouse: String              # 仓库归属（来自商品分类标签）
│
├── 业务量
├── quantities: Integer             # 数量（来自 DB.json）
├── cartonQty: Integer            # 箱数（来自 DB.json）
├── unit: String                    # 计量单位（个/台/套）
├── unitPriceRmb: BigDecimal        # 含税单价(CNY)
├── amountRmb: BigDecimal         # 金额(RMB) = 单价 × 数量（来自 DB.json）
│
├── 税率
├── taxRate: BigDecimal             # 增值税率（默认 0.10）
├── taxPoint: BigDecimal            # 票点（默认 1.1）
│
├── 尺寸字段
├── lengthCm: BigDecimal            # 单品长(cm)
├── widthCm: BigDecimal             # 单品宽(cm)
├── heightCm: BigDecimal            # 单品高(cm)
├── volumeCbm: BigDecimal           # 单品体积(m³)，自动计算
│
├── 重量字段
├── netWeightKg: BigDecimal         # 净重(kg)
├── grossWeightKg: BigDecimal       # 毛重(kg)
│
├── 报关字段
├── hsCode: String                  # HS编码（中国，8-10位）
├── hsCodeJp: String              # 日本HS编码（税番，来自 goods_hs_mapping → jp_hs_code）
├── declarationElements: String     # 申报要素
│
├── 外箱字段
├── unitsPerPackage: Integer        # 段ボール入数（每箱数量）
├── packageLengthCm: BigDecimal     # 外箱长(cm)
├── packageWidthCm: BigDecimal      # 外箱宽(cm)
├── packageHeightCm: BigDecimal    # 外箱高(cm)
├── packageVolumeCbm: BigDecimal   # 外箱体积(m³)
├── packageWeightKg: BigDecimal    # 外箱毛重(kg)
│
├── 质检
├── requiresQc: Boolean             # 是否需要检测
│
├── 其他
├── remarks: String                 # 备注
├── lastUsedDate: LocalDate        # 最近使用日期
├── updateTime: LocalDateTime
└── updateBy: String
```

### 2.2 ProductFactory（商品-工厂关联）

```
ProductFactory（关联实体，非聚合根）
├── id: Long
├── productId: Long                # FK → product.id
├── factoryId: Long                # FK → factory.id
├── supplierSku: String             # 供应商货号（工厂内部编号）
├── moq: Integer                   # 最小起订量
├── leadTimeDays: Integer           # 交货周期（天）
├── unitPriceRmb: BigDecimal        # 该工厂的含税单价（各工厂可能不同）
├── isPreferred: Boolean           # 是否为首选供应商
├── createTime: LocalDateTime
└── updateTime: LocalDateTime
```

---

## 3. 枚举

```java
public enum ProductCategory {
    OEM,              // OEM定制产品
    ORDINARY,         // 普通商品
    FACTORY_DIRECT    // 工厂直供
}
```

---

## 4. 多对多关系详解

### 为什么是多对多

业务事实：
- 同一商品（masterCode=odn012）可由多个工厂生产
- 同一工厂可生产多个商品

**错误设计**（已在 DB-02 中遗留）：

```sql
-- ❌ 错误：product 表加 factory_id 单向外键
product.factory_id → factory.id
-- 导致：一个商品只能属于一个工厂，无法建模多工厂代工场景
```

**正确设计**（关联表）：

```sql
-- ✅ 正确：product_factory 关联表
product(id=1, masterCode=odn012) ←── product_factory ──→ factory(id=3)
product(id=1, masterCode=odn012) ←── product_factory ──→ factory(id=7)
```

### 表结构

```sql
CREATE TABLE product (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    master_code        VARCHAR(32)  NOT NULL,
    sub_code           VARCHAR(64),
    name_ja            VARCHAR(128) COMMENT '日文名称',
    name_en            VARCHAR(255) COMMENT '英文名称（报关用）',
    name_zh            VARCHAR(255) COMMENT '中文名称',
    image_url          VARCHAR(512) COMMENT '商品图片 URL',
    color_name         VARCHAR(64),
    material           VARCHAR(64),
    category           VARCHAR(20),
    origin             VARCHAR(100) COMMENT '原产国',
    unit               VARCHAR(50),
    length_cm          DECIMAL(8,2),
    width_cm           DECIMAL(8,2),
    height_cm          DECIMAL(8,2),
    volume_cbm         DECIMAL(10,6) COMMENT '自动计算',
    net_weight_kg     DECIMAL(10,4),
    gross_weight_kg   DECIMAL(10,4),
    unit_price_rmb     DECIMAL(12,4) COMMENT '含税单价',
    tax_point          DECIMAL(5,4) DEFAULT 1.1,
    tax_rate           DECIMAL(5,4) DEFAULT 0.10,
    hs_code            VARCHAR(20) COMMENT 'HS编码',
    declaration_elements TEXT COMMENT '申报要素',
    units_per_package  INT COMMENT '每箱数量',
    package_length_cm  DECIMAL(8,2),
    package_width_cm   DECIMAL(8,2),
    package_height_cm  DECIMAL(8,2),
    package_volume_cbm DECIMAL(10,6),
    package_weight_kg  DECIMAL(10,4),
    warehouse          VARCHAR(64),
    requires_qc       BOOLEAN,
    remarks           VARCHAR(512),
    last_used_date    DATE COMMENT '最近使用日期',
    update_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by         VARCHAR(64),
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_master_sub (master_code, sub_code),
    INDEX idx_master_code (master_code),
    INDEX idx_hs_code (hs_code)
);

CREATE TABLE product_factory (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id         BIGINT NOT NULL,
    factory_id         BIGINT NOT NULL,
    supplier_sku       VARCHAR(64) COMMENT '供应商内部货号',
    moq                INT DEFAULT 1 COMMENT '最小起订量',
    lead_time_days     INT COMMENT '交货周期(天)',
    unit_price_rmb     DECIMAL(12,4) COMMENT '该工厂含税单价',
    is_preferred       BOOLEAN DEFAULT FALSE,
    create_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_factory (product_id, factory_id),
    INDEX idx_product_id (product_id),
    INDEX idx_factory_id (factory_id)
);
```

---

## 5. 与现有数据的映射

### goods.sql → product 表字段对应

| goods.sql 字段 | product 表字段 | 说明 |
|---------------|--------------|------|
| `sku` | `master_code` + `sub_code` | `sku=in041-a` → master=`in041`, sub=`a` |
| `hs_code` | `hs_code` | 直接映射 |
| `name_en` | `name_en` | 直接映射 |
| `name_zh` | `name_zh` | 直接映射 |
| `unit_price` | `unit_price_rmb` | 直接映射（已有 782 条记录） |
| `tax_rate` | `tax_rate` | 直接映射 |
| `weight_gross` | `gross_weight_kg` | 单位换算：g→kg |
| `weight_net` | `net_weight_kg` | 单位换算：g→kg |
| `declaration_elements` | `declaration_elements` | 直接映射 |
| `box_qty` | `units_per_package` | 直接映射（部分为数量） |
| `box_desc` | `package_*_cm` | 从 `box_desc` 中解析长宽高 |
| `origin` | `origin` | 直接映射 |
| `factory_name` | → `product_factory.factory_id` | 需通过 factory.name 关联查询 |
| `buyer` | → `factory.buyer` | 移至 factory 表 |
| `remark` | `remarks` | 直接映射 |
| `last_used` | `last_used_date` | 直接映射 |

### DB-02 product 表 → 新的 product 表字段对应

| DB-02 旧字段 | 新 product 表字段 | 说明 |
|-------------|------------------|------|
| `name` | `name_ja` | 重命名，日文名称 |
| `product_category` | `category` | 重命名 |
| — | `name_en` | 新增 |
| — | `name_zh` | 新增 |
| — | `image_url` | 新增 |
| — | `origin` | 新增 |
| — | `unit` | 新增 |
| — | `net_weight_kg` | 从 `weight_kg` 拆分为净重 |
| — | `gross_weight_kg` | 新增（毛重） |
| — | `unit_price_rmb` | 新增 |
| — | `tax_point` | 新增 |
| — | `tax_rate` | 新增 |
| — | `declaration_elements` | 新增 |
| — | `package_volume_cbm` | 新增 |
| — | `package_weight_kg` | 新增 |
| `hs_code` | `hs_code` | 待新增（已在 DB-02 备注中标注） |
| `tax_point` | `tax_point` | 待新增 |

---

## 6. 与步骤1（补货需求）的集成

步骤1（`SPEC-B01`）录入补货需求时，通过两个 suggest 端点实现主/子货号自动补全：

```
补货需求录入页面（DemandPage.vue）
├── [主货号 el-select] → GET /products/suggest/master-codes?keyword={kw}
│   返回：{ masterCode, nameZh, colorCount }
├── 选中主货号后
│   └── [子货号 el-select] → GET /products/suggest/sub-codes?masterCode={code}
│       返回：{ subCode, colorName }[]
└── 用户填数量、目的地、日本担当
```

> 对应设计文档：`docs/design/FEATURE-货号自动补全与多子货号选择.md`

**ProductUseCase 用例**：

```java
// 主货号自动补全（步骤1补货需求页调用）
List<MasterCodeSuggestVO> suggestMasterCodes(String keyword);

// 子货号候选项（按主货号过滤，步骤1补货需求页调用）
List<SubCodeSuggestVO> suggestSubCodes(String masterCode);
```

**响应 DTO 字段**：

```java
// MasterCodeSuggestVO
{ masterCode: String, nameZh: String, colorCount: Integer }

// SubCodeSuggestVO
{ subCode: String, colorName: String }
```

---

## 7. 状态与约束

| 约束 | 说明 |
|------|------|
| `UNIQUE (master_code, sub_code)` | 同一主货号下子货号唯一 |
| `sub_code` 可为空 | 主货号无子货号时（如 `in041`），视为基础款 |
| `product_factory.moq >= 1` | 最小起订量至少为1 |
| `unit_price_rmb > 0` | 含税单价必须正数 |
| `tax_point` 默认 1.1 | 即发票为含税价 |

---

## 8. 代码实现状态

- [x] ✅ `Product` 聚合根实体（`manpou-allinone/.../product/domain/model/Product.java`）
- [x] ✅ `ProductFactory` 关联实体（`manpou-allinone/.../product/domain/model/ProductFactory.java`）
- [x] ✅ `ProductCategory` 枚举（`manpou-allinone/.../product/domain/model/ProductCategory.java`）
- [x] ✅ `ProductRepository` 领域接口（`manpou-allinone/.../product/domain/repository/ProductRepository.java`）
- [x] ✅ `ProductFactoryRepository` 领域接口（`manpou-allinone/.../product/domain/repository/ProductFactoryRepository.java`）
- [x] ✅ `ProductJpaRepository` JPA 持久化适配器
- [x] ✅ `ProductFactoryJpaRepository` JPA 持久化适配器
- [x] ✅ `ProductAssembler` DTO 转换器
- [x] ✅ `ProductUseCase` 用例服务（含唯一性校验）
- [x] ✅ `ProductController` REST 控制器（9个端点，含 suggest）
- [x] ✅ `/suggest/master-codes` 主货号自动补全端点
- [x] ✅ `/suggest/sub-codes` 子货号候选项端点
- [x] ✅ `MasterCodeSuggestVO` / `SubCodeSuggestVO` DTO
- [x] ✅ `ProductPage.vue` 前端商品管理页面（i18n完成，pagination bug已修复）
- [x] ✅ `product.ts` 前端 API 客户端
- [x] ✅ `V3__product_tables.sql` Flyway 迁移脚本
- [x] ✅ `V4__product_tables.sql` 表结构（factory + product + product_factory，H2 兼容）
- [x] ✅ `V5__factory_migration.sql` 工厂数据（500 条）
- [x] ✅ `V6__procurement_factory_link.sql`（占位 no-op）
- [x] ✅ `V7__demand_sub_product_extend.sql`（占位 no-op）
- [x] ✅ `V9__goods_data_migration.sql`（占位 no-op，goods_staging 数据已并入 V8）
- [x] ✅ `V10__product_data_migration.sql`（占位，真实数据由 V11 加载）
- [x] ✅ `V11__product_seed_data.sql` 商品数据 ETL（**1971 条**；含 product_factory 关联 **781 条**）
- [x] ✅ `V11__product_factory_data.sql` product_factory 关联（备选路径）

> **数据验证**：后端 H2 启动成功，`GET /api/v1/products?size=0` → `totalElements: 1971`；`GET /api/v1/factories?size=0` → `totalElements: 500`。`product_factory` 关联 781 条已由 V11__product_seed_data.sql 加载。

---

## 9. 缺口阻塞

| 项目 | 优先级 | 说明 |
|------|--------|------|
| `goods.sql` 数据清洗 | **P0** | `box_desc` 解析出尺寸（正则提取 cm/cm³） |
| `factory_name` → `factory_id` 映射 | **P0** | companies.sql 有 546 条，但 goods.sql 中有部分名称不精确 |
| `sub_code` 命名规范 | P1 | 统一颜色代码格式（re=红/bl=蓝/wt=白/...） |
| 商品图片存储方案 | P1 | OSS / MinIO / 数据库 BLOB |
| 各工厂单价差异 | P2 | `product_factory.unit_price_rmb` 是否支持不同工厂不同价 |
