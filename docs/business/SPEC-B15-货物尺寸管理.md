# SPEC-B15 — 货物尺寸管理

> **版本**: 1.3.0
> **更新**: 2026-05-24（v1.3.0：列顺序对齐参考表格；新增尺寸总计/包装列/备注；详情抽屉同步更新）
> **创建**: 2026-05-24
> **状态**: 规划中
> **业务步号**: 00（基础设施/基础数据）
> **前置步骤**: SPEC-B10 商品目录
> **对应后端**: `CargoSize` 聚合根 + `ProductUseCase` 扩展
> **依赖文档**: `SPEC-B10-商品目录-产品管理.md` · `SPEC-B11-用户中心与权限体系.md` · `docs/permission/PERMISSION-CODE-ALIGNMENT.md`

---

## 1. 业务定位

货物尺寸管理是商品目录的补充层——管理那些**数据不完整、尚待完善的货号**。

**三类不完整数据**：

| 来源 | 数量 | 特征 |
|------|------|------|
| `cargo_size`（item_size 未匹配） | ~319 | 只有尺寸，无商品档案 |
| `product`（item_size 已导入，无名称） | ~30 | OEM 品，只有货号，无任何数据 |
| `product`（item_size 已导入，缺字段） | ~506 | 有尺寸，缺价格/工厂/产地等 |

用户可在同一页面浏览、筛选、补全这些数据。

---

## 2. 数据审计（2026-05-24）

### 2.1 product 表现状

```
总记录数：5000
├─ 有尺寸（来自 item_size 导入）：506 条
│   ├─ 有 price：315 条
│   ├─ 无 price：191 条
│   ├─ 有 factory：202 条
│   ├─ 无 factory：304 条
│   ├─ 有 origin：212 条
│   ├─ 无 origin：294 条
│   └─ 无 warehouse（100%）：506 条
│
└─ 无 name_zh（空品）：30 条（全是 OEM）
```

### 2.2 item_size 未匹配（319 条）

```
├─ 垃圾数据：6 条（test1~4 / newitem / null）
├─ 尺寸全零（无尺寸信息）：19 条
├─ 尺寸数据正常但 DB 无对应 product：~294 条
└─ 重量异常（>100kg）：8 条
```

---

## 3. 数据模型

### 3.1 CargoSize（货物尺寸聚合根）

```java
CargoSize（聚合根）
├── id: Long                          // 主键
├── masterCode: String                // 主货号
├── subCode: String                  // 子货号（可空）
├── code: String                     // 完整货号（masterCode-subCode，unique）
├── legacyId: Long                   // 旧系统 ID
├── updateTime: LocalDateTime        // 旧系统更新时间
├── inputUser: String                // 录入人

// 尺寸字段（来自 item_size）
├── lengthCm: BigDecimal             // 长(cm)，来自 height
├── widthCm: BigDecimal              // 宽(cm)，来自 width
├── heightCm: BigDecimal             // 高(cm)，来自 depth
├── netWeightKg: BigDecimal          // 净重(kg)，来自 weight
├── packHeightCm: BigDecimal         // 外箱高(cm)
├── packWidthCm: BigDecimal          // 外箱宽(cm)
├── packDepthCm: BigDecimal          // 外箱深(cm)
├── packageWeightKg: BigDecimal      // 外箱毛重(kg)
├── unitsPerPackage: Integer          // 每箱数量

// 状态字段
├── status: CargoSizeStatus          // PENDING / PROMOTED / DISCARDED
├── productId: Long                  // 升格后 product.id
├── promotedBy: String               // 升格操作人
├── promotedAt: LocalDateTime        // 升格时间
├── remarks: String                  // 备注
├── showFlag: String                 // 0=正常，1=软删除

// 审计字段（BaseEntity）
├── createTime: LocalDateTime
├── updateTime: LocalDateTime
└── updateBy: String
```

### 3.2 CargoSizeStatus 枚举

```java
public enum CargoSizeStatus {
    PENDING,    // 待处理（默认）
    PROMOTED,   // 已升格为商品
    DISCARDED   // 已废弃
}
```

### 3.3 表结构

```sql
CREATE TABLE cargo_size (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    master_code          VARCHAR(32)  NOT NULL,
    sub_code             VARCHAR(64),
    code                 VARCHAR(96)  NOT NULL,
    legacy_id            BIGINT,
    update_time          DATETIME,
    input_user           VARCHAR(64),

    length_cm            DECIMAL(8,2),
    width_cm             DECIMAL(8,2),
    height_cm            DECIMAL(8,2),
    net_weight_kg        DECIMAL(10,4),
    pack_height_cm       DECIMAL(8,2),
    pack_width_cm        DECIMAL(8,2),
    pack_depth_cm        DECIMAL(8,2),
    package_weight_kg    DECIMAL(10,4),
    units_per_package    INT,

    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    product_id           BIGINT,
    promoted_by          VARCHAR(64),
    promoted_at          DATETIME,
    remarks              VARCHAR(512),
    show_flag            VARCHAR(10)  NOT NULL DEFAULT '0',

    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by            VARCHAR(64),

    UNIQUE KEY uk_code (code),
    INDEX idx_master_code (master_code),
    INDEX idx_status (status)
);
```

### 3.4 Product 不完整品判定规则

不作为新表，直接在 `product` 表上查询：

| 条件 | 说明 | 数量 |
|------|------|------|
| `name_zh IS NULL OR name_zh = ''` | 无中文名称 | 30 条 |
| `unit_price_rmb IS NULL OR unit_price_rmb = 0` | 无含税单价 | 191 条 |
| 无 factory 关联 | `product_factory` 无记录 | 304 条 |

---

## 4. 权限设计

### 4.1 权限定义

参考 `docs/permission/PERMISSION-CODE-ALIGNMENT.md`，新增 `cargo_size` 模块（ID 120~124）：

| ID | permission_code | permission_name_cn | action | 说明 |
|----|----------------|---------------------|--------|------|
| 120 | cargo_size:read | 查看货物尺寸 | READ | |
| 121 | cargo_size:import | 导入货物尺寸 | CREATE | 触发从 item_size 导入 |
| 122 | cargo_size:promote | 升格货物尺寸 | CREATE | 升格为商品 |
| 123 | cargo_size:discard | 废弃货物尺寸 | DELETE | 软废弃 |
| 124 | cargo_size:update | 编辑货物尺寸 | UPDATE | 编辑 cargo_size 备注等 |

**角色授权矩阵**：

| 角色 | cargo_size:read | cargo_size:import | cargo_size:promote | cargo_size:discard |
|------|:---:|:---:|:---:|:---:|
| ADMIN | ✅ | ✅ | ✅ | ✅ |
| MANAGER | ✅ | ✅ | ✅ | ✅ |
| OPERATOR | ✅ | ❌ | ❌ | ❌ |
| VIEWER | ✅ | ❌ | ❌ | ❌ |

> 注：`cargo_size:promote` 复用 `product:create`，因为本质是创建 Product 实体。页面按钮按 `cargo_size:promote` 守卫，后端接口加 `product:create` 鉴权。

### 4.2 权限加注位置

```java
// CargoSizeController.java
@GetMapping             → @PreAuthorize("hasAuthority('cargo_size:read')")
@PostMapping("/import")→ @PreAuthorize("hasAuthority('cargo_size:import')")
@PostMapping("/{id}/promote") → @PreAuthorize("hasAuthority('cargo_size:promote') or hasAuthority('product:create')")
@PostMapping("/{id}/discard")  → @PreAuthorize("hasAuthority('cargo_size:discard')")

// ProductController.java
@PutMapping("/{id}/complete") → @PreAuthorize("hasAuthority('product:update')")
```

---

## 5. 操作日志（@AuditLog）

### 5.1 日志记录规则

CargoSize 和 Product 的业务操作均通过 `@AuditLog` 注解记录：

| 操作 | 注解 | targetType | 说明 |
|------|------|------------|------|
| 升格 | `@AuditLog(action="CARGO_SIZE_PROMOTE")` | `CargoSize` | 记录 cargo_size.id + product.id + code |
| 废弃 | `@AuditLog(action="CARGO_SIZE_DISCARD")` | `CargoSize` | 记录 cargo_size.id + code |
| 补全商品 | `@AuditLog(action="PRODUCT_COMPLETE")` | `Product` | 记录 product.id + 修改字段 |

### 5.2 操作类型枚举（AuditAction）

在 `AuditAction` 枚举中追加：

```java
// CargoSize 相关
CARGO_SIZE_PROMOTE("升格货物尺寸"),
CARGO_SIZE_DISCARD("废弃货物尺寸"),
CARGO_SIZE_IMPORT("导入货物尺寸"),

// Product 补全（复用 UPDATE，变更说明中标注）
// 无需新增枚举，直接用 UPDATE，detail 中标注 "补全字段: xxx"
```

### 5.3 前端操作日志页面

操作日志页面（`AuditLogPage.vue`）自动展示所有 `CARGO_SIZE_*` 和 `PRODUCT_COMPLETE` 记录，无需额外开发。

---

## 6. API 设计

### 6.1 货物尺寸列表

```
GET /api/v1/cargo-sizes
```

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `keyword` | String | — | 模糊搜索 code / masterCode / subCode |
| `status` | String | PENDING | PENDING / PROMOTED / DISCARDED |
| `page` | Integer | 0 | |
| `pageSize` | Integer | 20 | |

### 6.2 商品不完整品列表

```
GET /api/v1/products/incomplete
```

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `type` | String | no_name | no_name / no_price / no_factory / all |
| `keyword` | String | — | 模糊搜索 masterCode / subCode / nameZh |
| `page` | Integer | 0 | |
| `pageSize` | Integer | 20 | |

响应同商品分页（ProductOverviewVO 子集）。

### 6.3 升格为商品

```
POST /api/v1/cargo-sizes/{id}/promote
```

**请求体**：

```json
{
  "nameZh": "EE106 黑色款",
  "nameEn": "EE106 Black",
  "category": "ORDINARY",
  "unit": "个",
  "unitPriceRmb": 120.00,
  "origin": "中国",
  "factoryIds": [3, 7],
  "hsCode": "9503.00",
  "remarks": "来源：item_size 升格"
}
```

**处理逻辑**：

```
1. 查 cargo_size 记录（校验 status=PENDING）
2. 创建 Product 实体：
   - masterCode / subCode / lengthCm / widthCm / heightCm / netWeightKg / pack* / packageWeightKg / unitsPerPackage（来自 cargo_size）
   - nameZh / nameEn / category / unit / unitPriceRmb / origin / hsCode / remarks（来自请求体）
3. 保存 product_factory 关联（factoryIds）
4. 更新 cargo_size：
   - status = PROMOTED
   - productId = product.id
   - promotedBy = UserContext.getUsername()
   - promotedAt = now()
5. 返回 { cargoSizeId, productId, masterCode, subCode }
```

### 6.4 编辑 product 不完整品

```
PUT /api/v1/products/{id}/complete
```

**请求体**：

```json
{
  "nameZh": "OD572 电子秤",
  "unitPriceRmb": 85.00,
  "origin": "中国",
  "factoryIds": [12],
  "hsCode": "8423.81"
}
```

### 6.5 废弃 cargo_size

```
POST /api/v1/cargo-sizes/{id}/discard
```

标记 `cargo_size.status=DISCARDED`，记录废弃人和时间。

### 6.6 触发导入

```
POST /api/v1/internal/cargo-size/import
```

从 `item_size.json` 重新导入（幂等，code 唯一约束）。

---

## 7. 前端页面设计

### 7.1 路由

```
/base/cargo-size    → CargoSizePage.vue
```

### 7.2 页面布局（对齐 ProductPage.vue 风格）

```html
<div class="page">
  <!-- 统计行 -->
  <el-row :gutter="16" class="stats-row">
    <el-col :span="6"><stat-card 货物尺寸数 PENDING></el-col>
    <el-col :span="6"><stat-card 商品待补全数></el-col>
    <el-col :span="6"><stat-card 已升格数></el-col>
    <el-col :span="6"><stat-card 已废弃数></el-col>
  </el-row>

  <!-- Tab + 筛选 -->
  <el-card class="filter-card" shadow="never">
    <el-tabs v-model="activeTab">
      <el-tab-pane :label="$t('cargoSize.tab.pending')" name="cargo" />
      <el-tab-pane :label="$t('cargoSize.tab.incomplete')" name="product" />
    </el-tabs>
    <el-form :inline="true" :model="filterForm">
      <!-- Tab A 筛选：status 下拉 + keyword -->
      <!-- Tab B 筛选：type 下拉（no_name/no_price/no_factory/all）+ keyword -->
      <el-button type="primary" @click="loadData">{{ $t('common.button.search') }}</el-button>
      <el-button @click="onReset">{{ $t('common.button.reset') }}</el-button>
      <el-button type="primary" @click="onImport" v-if="hasPermission('cargo_size:import')">
        {{ $t('cargoSize.action.import') }}
      </el-button>
    </el-form>
  </el-card>

  <!-- 表格 -->
  <el-card class="table-card" shadow="never">
    <el-table :data="tableData" v-loading="loading" stripe>
      <!-- Tab A 列：masterCode / subCode / code / 长 / 宽 / 高 / 净重 / 体积 / status / 操作 -->
      <!-- Tab B 列：masterCode / subCode / nameZh / 长 / 宽 / 高 / 净重 / 单价 / 工厂数 / 操作 -->
    </el-table>
    <el-pagination ... />
  </el-card>

  <!-- 详情抽屉（el-drawer direction="rtl" size="680px"） -->
  <!-- 升格/编辑弹窗（el-dialog width="840px" :close-on-click-modal="false"） -->
</div>
```

### 7.3 Tab A 表格列（cargo_size 待升格）

> **2026-05-24**：对齐参考表格，新增尺寸总计/包装列/备注，调整列顺序（高→宽→长）；移除支番/状态列。

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 主货号 | `masterCode` | min-width=120 | |
| 货号 | `code` | min-width=140 | el-tag 显示 |
| 高(cm) | `heightCm` | min-width=80 | 右对齐 |
| 宽(cm) | `widthCm` | min-width=80 | 右对齐 |
| 长(cm) | `lengthCm` | min-width=80 | 右对齐 |
| 尺寸总计 | 计算值 | min-width=100 | 长+宽+高，保留2位小数 |
| 净重(kg) | `netWeightKg` | min-width=90 | 右对齐 |
| 体积(m³) | 计算值 | min-width=100 | 长×宽×高/10⁶，保留6位小数 |
| 每包数量 | `unitsPerPackage` | min-width=90 | 右对齐 |
| 包装高(cm) | `packHeightCm` | min-width=100 | 右对齐 |
| 包装宽(cm) | `packWidthCm` | min-width=100 | 右对齐 |
| 包装深(cm) | `packDepthCm` | min-width=100 | 右对齐 |
| 包装尺寸总计 | 计算值 | min-width=110 | 包装高+宽+深，保留2位小数 |
| 包装重量(kg) | `packageWeightKg` | min-width=110 | 右对齐 |
| 备注 | `remarks` | min-width=140 | |
| 录入时间 | `updateTime` | min-width=160 | |
| 操作 | — | min-width=200 fixed=right | 详情 / 编辑 / 删除 |

### 7.4 Tab B 表格列（商品待补全）

> **2026-05-24**：当前版本已简化为单表格，Tab B 已移除。

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 主货号 | `masterCode` | min-width=110 | el-tag 显示 |
| 子货号 | `subCode` | min-width=100 | |
| 中文名称 | `nameZh` | min-width=150 | 可能为空，红色标注 |
| 高(cm) | `heightCm` | min-width=80 | 右对齐 |
| 宽(cm) | `widthCm` | min-width=80 | 右对齐 |
| 长(cm) | `lengthCm` | min-width=80 | 右对齐 |
| 尺寸总计 | 计算值 | min-width=100 | 长+宽+高 |
| 净重(kg) | `netWeightKg` | min-width=90 | 右对齐 |
| 含税单价 | `unitPriceRmb` | min-width=100 | 可能为空 |
| 工厂数 | `factoryCount` | min-width=80 | |
| 操作 | — | min-width=120 | 详情 / 编辑 |

### 7.5 详情抽屉（对齐 ProductPage.vue el-drawer 风格）

触发：任意 Tab 操作列点击「详情」。

```html
<el-drawer v-model="detailVisible"
  :title="$t('cargoSize.drawer.title')"
  size="680px"
  direction="rtl"
  bodyStyle="overflow-y: auto">
  <div class="drawer-content">

    <!-- Tab A 抽屉内容（cargo_size） -->
    <template v-if="activeTab === 'cargo'">
      <div class="drawer-section-title">{{ $t('cargoSize.drawer.section.basic') }}</div>
      <div class="detail-grid">
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.masterCode') }}</span><span class="detail-value">{{ currentRow.masterCode }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.subCode') }}</span><span class="detail-value">{{ currentRow.subCode || '-' }}</span></div>
        <div class="detail-item full-width"><span class="detail-label">{{ $t('cargoSize.code') }}</span><span class="detail-value">{{ currentRow.code }}</span></div>
      </div>
      <div class="drawer-section-title">{{ $t('cargoSize.drawer.section.dims') }}</div>
      <div class="detail-grid">
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.lengthCm') }}</span><span class="detail-value">{{ currentRow.lengthCm ?? '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.widthCm') }}</span><span class="detail-value">{{ currentRow.widthCm ?? '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.heightCm') }}</span><span class="detail-value">{{ currentRow.heightCm ?? '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.netWeightKg') }}</span><span class="detail-value">{{ currentRow.netWeightKg != null ? currentRow.netWeightKg + ' kg' : '-' }}</span></div>
        <div class="detail-item full-width"><span class="detail-label">{{ $t('cargoSize.volumeCbm') }}</span><span class="detail-value">{{ calcVolume(currentRow) || '-' }}</span></div>
      </div>
      <div class="drawer-section-title">{{ $t('cargoSize.drawer.section.package') }}</div>
      <div class="detail-grid">
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.packHeightCm') }}</span><span class="detail-value">{{ currentRow.packHeightCm ?? '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.packWidthCm') }}</span><span class="detail-value">{{ currentRow.packWidthCm ?? '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.packDepthCm') }}</span><span class="detail-value">{{ currentRow.packDepthCm ?? '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.packageWeightKg') }}</span><span class="detail-value">{{ currentRow.packageWeightKg ?? '-' }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.unitsPerPackage') }}</span><span class="detail-value">{{ currentRow.unitsPerPackage ?? '-' }}</span></div>
      </div>
      <div class="drawer-section-title">{{ $t('cargoSize.drawer.section.remarks') }}</div>
      <div class="detail-remarks">{{ currentRow.remarks || '-' }}</div>
    </template>

    <!-- Tab B 抽屉内容（product 不完整品） -->
    <template v-else>
      <!-- 复用 ProductPage.vue 抽屉内容模板（基本信息 + 规格信息） -->
    </template>

    <div class="drawer-footer">
      <el-button @click="detailVisible = false">{{ $t('common.button.close') }}</el-button>
      <!-- Tab A -->
      <el-button v-if="activeTab === 'cargo' && hasPermission('cargo_size:promote')" type="primary" @click="onPromote">
        {{ $t('cargoSize.action.promote') }}
      </el-button>
      <el-button v-if="activeTab === 'cargo' && hasPermission('cargo_size:discard')" type="danger" @click="onDiscard">
        {{ $t('cargoSize.action.discard') }}
      </el-button>
      <!-- Tab B -->
      <el-button v-if="activeTab === 'product' && hasPermission('product:update')" type="warning" @click="onEdit">
        {{ $t('common.button.edit') }}
      </el-button>
    </div>
  </div>
</el-drawer>
```

### 7.6 升格弹窗（el-dialog width="840px"）

对齐 `ProductPage.vue` 的 `el-dialog` 风格（`:close-on-click-modal="false"`）。

```html
<el-dialog v-model="promoteVisible"
  :title="$t('cargoSize.dialog.promoteTitle')"
  width="840px"
  :close-on-click-modal="false">
  <!-- 货号 + 尺寸信息（只读展示区） -->
  <div class="dialog-info-box">
    {{ $t('cargoSize.code') }}: {{ currentRow.code }} |
    {{ $t('cargoSize.lengthCm') }}: {{ currentRow.lengthCm }} ×
    {{ $t('cargoSize.widthCm') }}: {{ currentRow.widthCm }} ×
    {{ $t('cargoSize.heightCm') }}: {{ currentRow.heightCm }} cm /
    {{ $t('cargoSize.netWeightKg') }}: {{ currentRow.netWeightKg }} kg
  </div>
  <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
    <!-- 基本信息（el-row :gutter=16, el-col :span=8） -->
    <el-form-item :label="$t('cargoSize.dialog.nameZh')" prop="nameZh">
      <el-input v-model="form.nameZh" maxlength="255" />
    </el-form-item>
    <el-form-item :label="$t('cargoSize.dialog.nameEn')">
      <el-input v-model="form.nameEn" maxlength="255" />
    </el-form-item>
    <el-form-item :label="$t('cargoSize.dialog.category')">
      <el-select v-model="form.category">
        <el-option value="ORDINARY" :label="$t('product.category.ORDINARY')" />
        <el-option value="OEM" :label="$t('product.category.OEM')" />
        <el-option value="FACTORY_DIRECT" :label="$t('product.category.FACTORY_DIRECT')" />
      </el-select>
    </el-form-item>
    <!-- 规格信息 -->
    <el-form-item :label="$t('cargoSize.dialog.unit')">
      <el-input v-model="form.unit" maxlength="50" />
    </el-form-item>
    <el-form-item :label="$t('cargoSize.dialog.unitPriceRmb')">
      <el-input-number v-model="form.unitPriceRmb" :min="0" :precision="2" />
    </el-form-item>
    <el-form-item :label="$t('cargoSize.dialog.origin')">
      <el-input v-model="form.origin" maxlength="100" />
    </el-form-item>
    <el-form-item :label="$t('cargoSize.dialog.hsCode')">
      <el-input v-model="form.hsCode" maxlength="20" />
    </el-form-item>
    <!-- 关联工厂（el-select remote + factoryApi.getAll） -->
    <el-form-item :label="$t('cargoSize.dialog.factoryIds')">
      <el-select v-model="form.factoryIds" multiple remote placeholder="搜索工厂" ...>
        <el-option v-for="f in factoryOptions" :key="f.id" :label="f.factoryName" :value="f.id" />
      </el-select>
    </el-form-item>
    <el-form-item :label="$t('cargoSize.dialog.remarks')">
      <el-input v-model="form.remarks" type="textarea" :rows="2" maxlength="512" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="promoteVisible = false">{{ $t('common.button.cancel') }}</el-button>
    <el-button type="primary" :loading="submitting" @click="onPromoteSubmit">
      {{ $t('cargoSize.action.promote') }}
    </el-button>
  </template>
</el-dialog>
```

### 7.7 编辑弹窗（el-dialog width="840px"）

Tab B 补全商品信息弹窗，同升格弹窗布局，字段为 nameZh / unitPriceRmb / origin / factoryIds / hsCode。

---

## 8. i18n（zh.json / ja.json）

```json
{
  "menu": { "cargoSize": "货物尺寸管理" },

  "cargoSize": {
    "title": "货物尺寸管理",

    "tab": {
      "pending": "待升格尺寸（{n}）",
      "incomplete": "商品待补全（{n}）"
    },

    "stat": {
      "pending": "待升格",
      "incomplete": "商品待补全",
      "promoted": "已升格",
      "discarded": "已废弃"
    },

    "column": {
      "masterCode": "主货号",
      "subCode": "子货号",
      "code": "完整货号",
      "lengthCm": "长(cm)",
      "widthCm": "宽(cm)",
      "heightCm": "高(cm)",
      "netWeightKg": "净重(kg)",
      "volumeCbm": "体积(m\u00b3)",
      "unitPriceRmb": "含税单价",
      "factoryCount": "工厂数",
      "status": "状态",
      "updateTime": "录入时间",
      "action": "操作"
    },

    "status": {
      "PENDING": "待处理",
      "PROMOTED": "已升格",
      "DISCARDED": "已废弃"
    },

    "filter": {
      "keyword": "搜索货号",
      "keywordPlaceholder": "主货号/子货号",
      "status": "状态",
      "type": "类型",
      "noName": "无名称",
      "noPrice": "无价格",
      "noFactory": "无工厂",
      "all": "全部"
    },

    "action": {
      "detail": "详情",
      "promote": "升格",
      "discard": "废弃",
      "import": "导入尺寸",
      "edit": "编辑"
    },

    "drawer": {
      "title": "货物尺寸详情",
      "section": {
        "basic": "基本信息",
        "dims": "尺寸信息",
        "package": "外箱信息",
        "remarks": "备注"
      },
      "noFactories": "暂无关联工厂"
    },

    "dialog": {
      "promoteTitle": "升格为商品",
      "editTitle": "补全商品信息",
      "nameZh": "中文名称",
      "nameZhPlaceholder": "请输入中文名称",
      "nameEn": "英文名称",
      "category": "分类",
      "unit": "单位",
      "unitPriceRmb": "含税单价",
      "origin": "原产国",
      "hsCode": "HS编码",
      "factoryIds": "关联工厂",
      "remarks": "备注",
      "remarksPlaceholder": "来源：item_size 升格"
    },

    "confirm": {
      "promote": "确认升格此货物尺寸为正式商品？",
      "promoteSuccess": "升格成功，商品ID：{id}",
      "discard": "确认废弃此货物尺寸？",
      "discardSuccess": "废弃成功",
      "editSuccess": "保存成功"
    },

    "validation": {
      "nameZhRequired": "请输入中文名称"
    }
  }
}
```

---

## 9. 菜单配置

AppLayout.vue 基础数据菜单（对齐现有风格）：

```html
<el-sub-menu index="base-data">
  <template #title>
    <el-icon><Grid /></el-icon>
    <span>{{ $t('menu.baseData') }}</span>
  </template>
  <el-menu-item index="/base/product">{{ $t('menu.product') }}</el-menu-item>
  <el-menu-item index="/base/factory">{{ $t('menu.factory') }}</el-menu-item>
  <el-menu-item index="/base/ship">{{ $t('menu.ship') }}</el-menu-item>
  <el-menu-item index="/base/cargo-size">{{ $t('menu.cargoSize') }}</el-menu-item>  <!-- 新增 -->
</el-sub-menu>
```

Router（`router/index.ts`）：

```ts
{
  path: '/base/cargo-size',
  component: () => import('@/pages/product/CargoSizePage.vue'),
  meta: { title: 'menu.cargoSize', roles: ['ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER'] }
}
```

---

## 10. 导入策略

### 10.1 ItemSizeImportService 修改

```
for each item_size record
├── findProduct(masterCode, subCode, code)
│   ├── ✅ 找到 → UPDATE product 尺寸字段
│   └── ❌ 未找到
│       ├── UPSERT cargo_size（code 唯一约束幂等）
│       └── 标记 NOT_FOUND
```

### 10.2 升格后 item_size 重复导入处理

升格后的 cargo_size 记录 `status=PROMOTED`，重复导入时跳过（不覆盖 status）。

---

## 11. 实现计划

| 阶段 | 内容 | 文件 |
|------|------|------|
| Phase 1 | Flyway：`V__cargo_size.sql`（ID 121） + 权限 INSERT | `V__cargo_size.sql` |
| Phase 1 | CargoSize 实体 + BaseEntity 继承 | `CargoSize.java` |
| Phase 1 | CargoSizeRepository | `CargoSizeRepository.java` |
| Phase 1 | CargoSizeAssembler | `CargoSizeAssembler.java` |
| Phase 1 | CargoSizeUseCase（promote/discard/query） | `CargoSizeUseCase.java` |
| Phase 1 | CargoSizeController（REST API + @PreAuthorize + @AuditLog） | `CargoSizeController.java` |
| Phase 1 | ProductUseCase 新增 `listIncomplete()` / `complete()` | `ProductUseCase.java` |
| Phase 1 | ProductController 新增 `PUT /{id}/complete` | `ProductController.java` |
| Phase 1 | ItemSizeImportService：未匹配时 UPSERT cargo_size | `ItemSizeImportService.java` |
| Phase 1 | 触发一次 cargo_size 导入（当前 319 条） | — |
| Phase 2 | CargoSizePage.vue（Tab A + Tab B + 统计卡） | `CargoSizePage.vue` |
| Phase 2 | CargoSizeDetailDrawer.vue（详情抽屉） | `CargoSizeDetailDrawer.vue` |
| Phase 2 | CargoSizePromoteDialog.vue（升格弹窗） | `CargoSizePromoteDialog.vue` |
| Phase 2 | ProductCompleteDialog.vue（编辑弹窗） | `ProductCompleteDialog.vue` |
| Phase 2 | api/cargoSize.ts | `api/cargoSize.ts` |
| Phase 2 | 路由 + 菜单 + i18n | `router/index.ts` + `AppLayout.vue` + `zh.json/ja.json` |
| Phase 2 | 前端权限守卫 | `hasPermission('cargo_size:*')` |

---

## 12. 设计风格规范

本页面严格对齐 `ProductPage.vue` 的实现风格：

| 规范 | 来源 |
|------|------|
| 页面容器：`<div class="page">` + el-row/el-col stats-row + filter-card + table-card | ProductPage.vue |
| 表格：`el-table stripe style="width:100%" min-height="200"` + 分页 | ProductPage.vue |
| 详情抽屉：`el-drawer direction="rtl" size="680px"` + drawer-content + drawer-section-title + detail-grid | ProductPage.vue |
| 弹窗：`el-dialog :close-on-click-modal="false" width="840px"` + el-row :gutter=16 | ProductPage.vue |
| 操作列：`el-button link`（蓝色/警告色/危险色）+ 权限守卫 | ProductPage.vue |
| 表单：`:inline="true"` + label-width + el-input style width 固定值 | ProductPage.vue |
| 详情区：detail-grid + detail-item + full-width | ProductPage.vue |

---

## 13. 数据分布（导入后预期）

```
cargo_size 表（导入后）：
├─ PENDING：~319 条
│   ├─ 垃圾 test/null/newitem：6 条
│   ├─ 尺寸全零：19 条
│   └─ 正常尺寸：~294 条
├─ PROMOTED：0 条（升格后填充）
└─ DISCARDED：0 条（废弃后填充）

product 表（不完整品筛选）：
├─ no_name：无名称 ~30 条
├─ no_price：无价格 ~191 条（部分重叠）
├─ no_factory：无工厂 ~304 条（部分重叠）
└─ all：有尺寸 ~506 条
```

---

## 14. 旧方案 vs 新方案对比

| 维度 | 旧方案（auto-create） | 新方案（cargo_size + 补全） |
|------|----------------------|--------------------------|
| 未匹配处理 | 自动创建 Product | 写入 cargo_size |
| product 不完整品 | 未处理 | 统一页面编辑 |
| 用户控制 | 无 | 手动升格/废弃/补全 |
| 垃圾数据 | 大量混入 | cargo_size 可废弃标记 |
| 尺寸数据 | 只读 | cargo_size + product 均可见可编辑 |
| 权限控制 | 无 | cargo_size 模块权限 |
| 操作日志 | 无 | @AuditLog 全链路记录 |
