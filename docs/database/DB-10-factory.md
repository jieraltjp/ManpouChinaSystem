# DB-10 — 工厂/厂家数据库设计

> **版本**: 1.1.0
> **创建**: 2026-04-22
> **更新**: 2026-04-23（v1.1.0：factory_name 加 UNIQUE KEY uk_factory_name，业务唯一标识）
> **状态**: ✅ 已实现
> **业务步号**: 02（工厂信息 — 发注前置基础数据）
> **对应 UI**: `docs/ui/pages/02-procurement.md` §工厂信息
> **对应实体**: `com.manpou.allinone.factory.Factory`

---

## §1 业务背景

工厂（厂家）是发注管理的**前置基础数据**。发注单（procurement）在录入阶段必须关联到具体的工厂，且工厂信息决定后续物流、报关、结算等多个环节。

现有 `companies` 表（545 条记录，来自 `huogui` 遗留数据库）存在以下问题：
- 无联系方式字段（微信/QQ/手机号分散）
- 无合作状态分类
- 无账期字段
- 无省/市/县拆分（地址粒度粗）
- 无分类字段（无法区分五金/纺织/塑料等）
- 约 160 条记录无省市县和经纬度信息

---

## §2 业务模型

### 2.1 工厂分类（category）

| 分类 | category_code | 说明 |
|------|-------------|------|
| 五金工具 | `TOOLS` | 永康、宁波工具类厂家 |
| 纺织服装 | `TEXTILE` | 服装、箱包、面料 |
| 塑料制品 | `PLASTIC` | 容器、配件、玩具原料 |
| 电子电器 | `ELECTRONICS` | 电子设备、家电配件 |
| 家具家居 | `FURNITURE` | 家具、家居用品 |
| 汽车配件 | `AUTO_PARTS` | 汽车零部件、维修工具 |
| 运动户外 | `SPORTS` | 户外装备、体育器材 |
| 宠物用品 | `PET` | 宠物笼具、玩具 |
| 医疗器械 | `MEDICAL` | 医疗设备、护理器械 |
| 工艺礼品 | `CRAFTS` | 工艺品、礼品 |
| 化工材料 | `CHEMICAL` | 化工原料、涂料 |
| 其他 | `OTHER` | 未分类 |

### 2.2 合作状态（cooperation_status）

| 状态 | 值 | 说明 |
|------|---|------|
| 合作中 | `ACTIVE` | 正常下单 |
| 已暂停 | `SUSPENDED` | 暂停合作，待评估 |
| 已淘汰 | `ELIMINATED` | 淘汰厂家，不再合作 |
| 潜在合作 | `POTENTIAL` | 潜在供应商，尚未合作 |

### 2.3 账期（payment_terms）

| 账期 | payment_code | 说明 |
|------|-------------|------|
| 现结 | `CASH` | 当次结清 |
| 月结30天 | `NET_30` | 当月货款次月结算 |
| 月结60天 | `NET_60` | 两个月账期 |
| 月结90天 | `NET_90` | 季度结算 |
| 信用账期 | `CREDIT` | 按订单结算 |

---

## §3 数据迁移策略

### 3.1 迁移路径

```
companies (545条)  ──迁移──▶  factory (新表)
```

| companies 字段 | → | factory 字段 | 转换规则 |
|----------------|---|-------------|---------|
| `id` | → | `id` | 直接迁移 |
| `name` | → | `factory_name` | 直接迁移 |
| `province` | → | `province` | 直接迁移（160条为空的待补录） |
| `city` | → | `city` | 直接迁移 |
| `district` | → | `county` | 列重命名 |
| `address` | → | `rough_location` | 列重命名，含义对齐 |
| `longitude` | → | `longitude` | 直接迁移（0值待补录） |
| `latitude` | → | `latitude` | 直接迁移（0值待补录） |
| `created_at` | → | `create_time` | 列重命名 |
| `updated_at` | → | `update_by` | 旧数据置空 |
| `is_deleted` | → | `is_deleted` | 直接迁移 |
| — | → | `factory_code` | 格式：`F-YYYYMMDD-NNN`，按 create_time 升序分配 |
| — | → | `contact_name` | 空，待补录 |
| — | → | `contact_phone` | 空，待补录 |
| — | → | `contact_wechat` | 空，待补录 |
| — | → | `contact_qq` | 空，待补录 |
| — | → | `payment_terms` | 空，默认 NET_30 |
| — | → | `cooperation_status` | 空，默认 POTENTIAL |
| — | → | `category` | 空，根据 name 关键词智能推断 |

### 3.2 分类智能推断规则

根据公司名称关键词推断 `category`：

| 关键词 | 推断分类 |
|--------|---------|
| 五金/工具/工具有限 | `TOOLS` |
| 纺织/服饰/服装/箱包/帽业/布业 | `TEXTILE` |
| 塑料/塑胶/塑料/橡塑 | `PLASTIC` |
| 电子/电器/光电/电气/LED/科技 | `ELECTRONICS` |
| 家具/家居/木业/竹木 | `FURNITURE` |
| 汽车/汽配/车业/汽保 | `AUTO_PARTS` |
| 户外/体育/运动/健身/玩具 | `SPORTS` |
| 宠物/猫狗/笼具 | `PET` |
| 医疗/器械/护理/康养 | `MEDICAL` |
| 工艺/礼品/竹木/编织 | `CRAFTS` |
| 化工/涂料/颜料/涂层 | `CHEMICAL` |
| 其他无法匹配 | `OTHER` |

---

## §4 数据库设计

> **数据库**: MySQL 8.x / InnoDB / `utf8mb4_unicode_ci`

### 4.1 完整建表语句

```sql
-- ============================================================
-- factory — 工厂/厂家信息表
-- 对应实体: com.manpou.allinone.factory.Factory
-- 数据来源: companies (huogui 遗留数据迁移)
-- ============================================================

CREATE TABLE factory (
    -- ===== 主键 & 审计 =====
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    create_time     DATETIME(6)  NOT NULL  DEFAULT CURRENT_TIMESTAMP(6)  COMMENT '创建时间',
    update_time     DATETIME(6)  NOT NULL  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)  COMMENT '更新时间',
    create_by       VARCHAR(64)  NOT NULL  DEFAULT ''  COMMENT '创建人',
    update_by       VARCHAR(64)  NOT NULL  DEFAULT ''  COMMENT '更新人',
    is_deleted      BIT(1)      NOT NULL  DEFAULT b'0'  COMMENT '逻辑删除: 0=正常, 1=删除',

    -- ===== 基础信息 =====
    factory_code    VARCHAR(32)  NOT NULL  UNIQUE  COMMENT '工厂编号: F-YYYYMMDD-NNN',
    factory_name   VARCHAR(128) NOT NULL            COMMENT '工厂名称',
    category       VARCHAR(32)  NOT NULL  DEFAULT 'OTHER'  COMMENT '分类: TOOLS/TEXTILE/PLASTIC/ELECTRONICS/FURNITURE/AUTO_PARTS/SPORTS/PET/MEDICAL/CRAFTS/CHEMICAL/OTHER',

    -- ===== 地理信息 =====
    province        VARCHAR(64)  NOT NULL  DEFAULT ''  COMMENT '省',
    city            VARCHAR(64)  NOT NULL  DEFAULT ''  COMMENT '市',
    county          VARCHAR(64)  NOT NULL  DEFAULT ''  COMMENT '县/区',
    rough_location VARCHAR(500)           DEFAULT NULL  COMMENT '详细地址（粗略）',
    longitude       DECIMAL(11,8)         DEFAULT NULL  COMMENT '经度',
    latitude        DECIMAL(11,8)         DEFAULT NULL  COMMENT '纬度',

    -- ===== 联系方式 =====
    contact_name   VARCHAR(64)            DEFAULT NULL  COMMENT '联系人姓名',
    contact_phone  VARCHAR(32)            DEFAULT NULL  COMMENT '手机号',
    contact_wechat VARCHAR(64)            DEFAULT NULL  COMMENT '微信号',
    contact_qq     VARCHAR(32)            DEFAULT NULL  COMMENT 'QQ号',

    -- ===== 合作信息 =====
    cooperation_status VARCHAR(32) NOT NULL  DEFAULT 'POTENTIAL'  COMMENT '合作状态: ACTIVE/SUSPENDED/ELIMINATED/POTENTIAL',
    payment_terms   VARCHAR(64)  NOT NULL  DEFAULT 'NET_30'  COMMENT '账期: CASH/NET_30/NET_60/NET_90/CREDIT',

    -- ===== 备注 =====
    notes          VARCHAR(500)           DEFAULT NULL  COMMENT '备注',

    -- ===== 索引 =====
    UNIQUE KEY uk_factory_name (factory_name),
    INDEX idx_factory_code    (factory_code),
    INDEX idx_factory_name    (factory_name),
    INDEX idx_factory_category(category),
    INDEX idx_factory_status  (cooperation_status),
    INDEX idx_factory_province(province),
    INDEX idx_factory_city    (city),
    INDEX idx_factory_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工厂/厂家信息表';
```

### 4.2 ALTER TABLE（增量变更 — 已有 factory 表时）

如果 `factory` 表已存在，通过 ALTER TABLE 补齐字段：

```sql
-- 增量 ALTER TABLE（从已有 factory 表扩展）
ALTER TABLE factory
    ADD COLUMN province            VARCHAR(64)  NOT NULL  DEFAULT ''  AFTER factory_name,
    ADD COLUMN city               VARCHAR(64)  NOT NULL  DEFAULT ''  AFTER province,
    ADD COLUMN county             VARCHAR(64)  NOT NULL  DEFAULT ''  AFTER city,
    ADD COLUMN category           VARCHAR(32)  NOT NULL  DEFAULT 'OTHER'  AFTER factory_name,
    ADD COLUMN rough_location     VARCHAR(500)          DEFAULT NULL  AFTER county,
    ADD COLUMN contact_wechat     VARCHAR(64)            DEFAULT NULL  AFTER contact_phone,
    ADD COLUMN contact_qq        VARCHAR(32)            DEFAULT NULL  AFTER contact_wechat,
    ADD COLUMN cooperation_status VARCHAR(32) NOT NULL  DEFAULT 'POTENTIAL'  AFTER contact_qq,
    ADD COLUMN payment_terms      VARCHAR(64)  NOT NULL  DEFAULT 'NET_30'  AFTER cooperation_status,
    ADD COLUMN notes             VARCHAR(500)           DEFAULT NULL  AFTER payment_terms;

-- 添加缺失索引
ALTER TABLE factory
    ADD INDEX idx_factory_category(category),
    ADD INDEX idx_factory_province(province),
    ADD INDEX idx_factory_city(city);
```

---

## §5 Java 实体更新

### 5.1 Factory.java 字段对齐

| 实体字段 | 表列 | 类型 | 说明 |
|---------|------|------|------|
| `id` | `id` | Long | 主键 |
| `createTime` | `create_time` | LocalDateTime | 创建时间 |
| `updateTime` | `update_time` | LocalDateTime | 更新时间 |
| `createBy` | `create_by` | String | 创建人 |
| `updateBy` | `update_by` | String | 更新人 |
| `isDeleted` | `is_deleted` | Boolean | 逻辑删除 |
| `factoryCode` | `factory_code` | String | 工厂编号 |
| `factoryName` | `factory_name` | String | 工厂名称 |
| `category` | `category` | FactoryCategory | 分类枚举 |
| `province` | `province` | String | 省 |
| `city` | `city` | String | 市 |
| `county` | `county` | String | 县/区 |
| `roughLocation` | `rough_location` | String | 详细地址 |
| `longitude` | `longitude` | BigDecimal | 经度 |
| `latitude` | `latitude` | BigDecimal | 纬度 |
| `contactName` | `contact_name` | String | 联系人 |
| `contactPhone` | `contact_phone` | String | 手机号 |
| `contactWechat` | `contact_wechat` | String | 微信号 |
| `contactQq` | `contact_qq` | String | QQ号 |
| `cooperationStatus` | `cooperation_status` | CooperationStatus | 合作状态 |
| `paymentTerms` | `payment_terms` | PaymentTerms | 账期 |
| `notes` | `notes` | String | 备注 |

> **字段变更说明**：`location`（原 String 省+市合并字段）已移除，替换为独立的 `province`、`city`、`county` 三列。`status`（FactoryStatus）已移除，替换为 `cooperationStatus`（CooperationStatus）。

### 5.2 新增枚举（独立领域类型，非 UI 层）

**FactoryCategory.java**
```java
public enum FactoryCategory {
    TOOLS, TEXTILE, PLASTIC, ELECTRONICS,
    FURNITURE, AUTO_PARTS, SPORTS, PET,
    MEDICAL, CRAFTS, CHEMICAL, OTHER
}
```

**CooperationStatus.java**
```java
public enum CooperationStatus {
    ACTIVE,      // 合作中
    SUSPENDED,   // 已暂停
    ELIMINATED,  // 已淘汰
    POTENTIAL     // 潜在合作
}
```

**PaymentTerms.java**
```java
public enum PaymentTerms {
    CASH,   // 现结
    NET_30, // 月结30天
    NET_60, // 月结60天
    NET_90, // 月结90天
    CREDIT  // 信用账期
}
```

---

## §6 API 接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 分页查询工厂 | GET | `/api/factories` | 支持分类/状态/省市筛选 |
| 按ID查询 | GET | `/api/factories/{id}` | 详情 |
| 按名称模糊搜索 | GET | `/api/factories/search?name=` | 补全候选 |
| 新增工厂 | POST | `/api/factories` | 含幂等校验 |
| 更新工厂 | PUT | `/api/factories/{id}` | 含版本校验 |
| 删除工厂 | DELETE | `/api/factories/{id}` | 逻辑删除（需校验无关联发注单） |

---

## §7 数据补录优先级

现有 545 条记录中，约 160 条缺少省市县和经纬度，按以下优先级补录：

| 优先级 | 条件 | 处理方式 |
|--------|------|---------|
| P0 | 已有详细地址（rough_location 非空）且省市县空 | 通过地址文本解析补全省市县 |
| P0 | 经纬度为 0 但有详细地址 | 通过地理编码 API 补全经纬度 |
| P1 | 已有省市县，经纬度为 0 | 同上 |
| P2 | 省市县和地址均空 | 人工补录 |

---

## §8 关联关系

```
factory (厂家)
    │
    ├── procurement (发注单)   ── factory_id FK
    ├── qc_record (验货记录)   ── factory_id FK（来源方）
    └── product (商品)         ── factory_id FK（生产方）
```

> 工厂删除前必须校验无关联发注单、验货记录和商品，否则拒绝删除。
