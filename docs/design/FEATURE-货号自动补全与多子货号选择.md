# FEATURE — 补货需求页主/子货号自动补全 + 多子货号选择

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: ✅ 已实现（2026-04-22）
> **影响页面**: `DemandPage.vue`（补货需求列表页）
> **对应后端**: `ProductController` / `ProductUseCase`
> **对应文档**: `SPEC-B01-补货需求-步骤1.md` · `DB-01-procurement-demand.md`

---

## 1. 现状分析

### 1.1 当前实现

| 组件 | 现状 | 问题 |
|------|------|------|
| `DemandPage.vue` 主货号输入 | `el-input` 纯文本，无提示 | 无法复用 Product 表数据，用户须记忆货号 |
| `DemandPage.vue` 子货号输入 | `el-input` 纯文本，单个值 | 无法选择多个颜色变体 |
| `ProductController` | 有 `/code/{masterCode}` 按主货号精确查询 | **无模糊搜索/自动补全接口** |
| `ProductJpaRepository` | 有 `findByMasterCodeAndIsDeletedFalse` | 无按关键词返回候选列表 |
| `ReplenishmentDemand` | `subProductCode` 为普通 String | 字段语义为"单个"，多选需扩展 |

### 1.2 数据模型对应关系

```
Product 表
├── masterCode (主货号)          ← DemandPage.productCode
├── subCode (子货号/色号)        ← DemandPage.subProductCode[]
├── nameZh (中文名称)            ← 补全显示用
├── colorName (颜色名称)          ← 子货号补全显示用

ReplenishmentDemand 表
├── productCode (主货号)         ← 单一主货号 ✅
└── subProductCode (子货号)      ← 当前为单个值，需支持多个
```

---

## 2. 设计决策

### 2.1 主货号补全（Autocomplete）

**交互**：用户在主货号输入框输入关键词 → 下拉列表显示匹配的 masterCode + nameZh → 选中之值填入输入框。

**数据来源**：`product` 表，`master_code` 列。

**候选查询**：输入 ≥ 1 字符时触发，300ms 防抖，返回最多 20 条。

**空结果处理**：显示"未找到商品「{keyword}」— 继续输入将新建"（不阻止提交）。

### 2.2 子货号多选（Multi-Select）

**交互**：选择主货号后，子货号选择器解锁 → 下拉显示该主货号下所有 `subCode`（来自 Product 表）→ 可多选标签（`el-select` multiple + filterable + tag）。

**语义扩展**：`subProductCode` 字段从单个 String 扩展为 JSON 数组存储（`["re", "wh", "bk"]`）。

**无颜色变体时**：子货号选择器降级为单行文本输入（手动录入）。

**新建品时**：若主货号在 Product 表中不存在，用户仍可填入 subProductCode（系统视为新品，后续在商品管理中补录）。

### 2.3 字段变更

| 操作 | 字段 | 变更类型 | 说明 |
|------|------|---------|------|
| 扩展 | `replenishment_demand.sub_product_code` | 列类型变更 | `VARCHAR(64)` → `VARCHAR(512)`，存储 JSON 数组如 `["re","wh"]` |
| 新增 | `product_sub_code_suggest` API | 新增 | 主货号自动补全 |
| 新增 | `product_sub_code_suggest` API | 新增 | 子货号多选候选项（按 masterCode 过滤） |
| 前端 | `DemandPage.vue` | 表单改造 | `el-input` → `el-select` remote autocomplete |

---

## 3. API 设计

### 3.1 主货号自动补全（新增）

```
GET /api/v1/products/suggest/master-codes?keyword={keyword}
Response:
{
  "code": "0",
  "data": [
    { "masterCode": "odn012", "nameZh": "折叠椅", "colorCount": 3 },
    { "masterCode": "odn015", "nameZh": "收纳箱", "colorCount": 2 }
  ]
}
```

### 3.2 子货号多选候选项（新增）

```
GET /api/v1/products/suggest/sub-codes?masterCode={masterCode}
Response:
{
  "code": "0",
  "data": [
    { "subCode": "re", "colorName": "红色" },
    { "subCode": "wh", "colorName": "白色" },
    { "subCode": "bk", "colorName": "黑色" }
  ]
}
```

> 注意：若 `masterCode` 在 Product 表中不存在，返回空数组（前端降级为文本输入）。

### 3.3 现有 API 保持不变（向后兼容）

```
GET /api/v1/products?keyword=&masterCode=&hsCode=  # 已有
GET /api/v1/products/{id}                          # 已有
GET /api/v1/products/code/{masterCode}             # 已有
```

---

## 4. 前端交互设计

### 4.1 主货号输入组件

```vue
<el-select
  v-model="formData.productCode"
  filterable
  remote
  reserve-keyword
  placeholder="输入主货号或商品名称搜索"
  :remote-method="searchMasterCode"
  :loading="masterCodeLoading"
  @change="onMasterCodeChange"
  style="width: 100%"
>
  <el-option
    v-for="item in masterCodeOptions"
    :key="item.masterCode"
    :label="item.masterCode + ' — ' + item.nameZh"
    :value="item.masterCode"
  />
</el-select>
```

### 4.2 子货号多选组件

```vue
<el-form-item :label="$t('demand.dialog.subProductCode')" prop="subProductCodes">
  <el-select
    v-model="formData.subProductCodes"
    multiple
    filterable
    allow-create
    default-first-option
    :disabled="!formData.productCode"
    :placeholder="formData.productCode ? '选择颜色变体（或手动输入）' : '请先选择主货号'"
    style="width: 100%"
  >
    <el-option
      v-for="item in subCodeOptions"
      :key="item.subCode"
      :label="(item.colorName || item.subCode) + ' (' + item.subCode + ')'"
      :value="item.subCode"
    />
  </el-select>
</el-form-item>
```

**提交时**：将 `subProductCodes: string[]` 序列化为 JSON 字符串存入 `subProductCode` 字段。

**回显时**：从 `subProductCode` 字段 JSON 反序列化为数组。

### 4.3 状态联动

| 条件 | 主货号选择器 | 子货号选择器 |
|------|------------|------------|
| 初始状态 | 可输入搜索 | 禁用（提示先选主货号） |
| 选中主货号 | 已选状态 | 解锁，显示该主货号下所有 subCode |
| 主货号在 Product 不存在 | 手动录入 | 解锁，改为普通文本输入 |

---

## 5. 数据库变更

### 5.1 `replenishment_demand` 表 ALTER

```sql
ALTER TABLE replenishment_demand
    MODIFY COLUMN sub_product_code VARCHAR(512) DEFAULT NULL
    COMMENT '子货号数组（JSON数组格式，如 ["re","wh"]）；单个时直接存字符串';

-- 旧数据兼容性：尝试解析 JSON，解析失败则视为单个子货号
```

> **迁移策略**：历史数据直接保留原值（单个子货号），前端反序列化时若非 JSON 数组格式则降级为单个字符串。

### 5.2 新增字段说明

| 列名 | 类型 | 说明 |
|------|------|------|
| `sub_product_code` | `VARCHAR(512)` | JSON 数组或单个字符串 |

---

## 6. 后端实现

### 6.1 `ProductController` 新增端点

```java
@GetMapping("/suggest/master-codes")
public Result<List<MasterCodeVO>> suggestMasterCodes(@RequestParam String keyword) {
    return Result.ok(productUseCase.suggestMasterCodes(keyword));
}

@GetMapping("/suggest/sub-codes")
public Result<List<SubCodeVO>> suggestSubCodes(@RequestParam String masterCode) {
    return Result.ok(productUseCase.suggestSubCodes(masterCode));
}
```

### 6.2 `ProductJpaRepository` 新增方法

```java
// 主货号模糊搜索（去重）
@Query("SELECT DISTINCT p.masterCode FROM Product p WHERE p.masterCode LIKE %:kw% AND p.isDeleted = false")
List<String> findDistinctMasterCodeByKeyword(@Param("kw") String keyword);

// 子货号候选项（按 masterCode 过滤）
List<Product> findByMasterCodeAndIsDeletedFalse(String masterCode);
```

---

## 7. 测试策略

| 场景 | 预期结果 |
|------|---------|
| 输入"od" → 返回包含"od"的主货号列表 | 下拉显示匹配项 |
| 选择主货号"odn012" → 子货号选择器解锁 | 显示 re/wh/bk 等候选项 |
| 不选主货号 → 子货号选择器禁用 | 禁用状态，placeholder 提示 |
| 输入 Product 表不存在的主货号 → 子货号降级为文本输入 | 正常可手动录入子货号 |
| 历史数据（单个子货号字符串）回显 | 正确解析显示 |

---

## 8. 优先级

| 优先级 | 内容 |
|--------|------|
| P1 | 主货号自动补全（el-select remote） |
| P1 | 子货号多选（el-select multiple） |
| P2 | 数据库字段扩展（VARCHAR→512，支持 JSON） |
| P2 | 回显时 JSON 降级兼容 |

---

## 9. 涉及文件清单

| 文件 | 操作 |
|------|------|
| `V6__demand_sub_product_extend.sql` | 新增：ALTER sub_product_code → VARCHAR(512) |
| `ProductController.java` | 新增 2 个 suggest 端点 |
| `ProductJpaRepository.java` | 新增 2 个查询方法 |
| `ProductUseCase.java` | 新增 suggestMasterCodes / suggestSubCodes |
| `DemandPage.vue` | 表单控件改造（el-input → el-select remote + multiple） |
| `api/demand.ts` | `CreateDemandRequest.subProductCode` 类型扩展 |
| `SPEC-B01-补货需求-步骤1.md` | 补充 API 端点说明 |
| `DB-01-procurement-demand.md` | 补充 sub_product_code 字段说明（v1.1.0） |
