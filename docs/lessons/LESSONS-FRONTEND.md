# 工程教训 — 前端（Vue / TypeScript / i18n / Element Plus）

> 项目：ManpouChinaSystem
> 覆盖范围：Vue 组件 / TypeScript / vue-i18n / Element Plus / 构建产物
> Lesson 编号：11–12, 14, 16, 21, 24, 33–34, 37, 40–44, 46–50, 52–58（共 27 条）

---

## 目录

- [Lesson 11: 文档与代码同步必须持续进行，不能积累](#lesson-11-文档与代码同步必须持续进行不能积累)
- [Lesson 12: i18n 必须从第一天规划，不能后期打补丁](#lesson-12-i18n-必须从第一天规划不能后期打补丁)
- [Lesson 14: 命名一致性必须在开发前锁定，禁止中途改名](#lesson-14-命名一致性必须在开发前锁定禁止中途改名)
- [Lesson 16: 前端 API 客户端类型必须与后端 DTO 严格对齐](#lesson-16-前端-api-客户端类型必须与后端-dto-严格对齐)
- [Lesson 21: BaseEntity 用 @MappedSuperclass，所有实体继承](#lesson-21-baseentity-用-mappedsuperclass所有实体继承)
- [Lesson 24: 测试数据提取禁止用字符串解析（grep）](#lesson-24-测试数据提取禁止用字符串解析grep)
- [Lesson 33: 锚点设计决定 Overview 可见性——Procurement 中心导致 Demand 新建后不可见](#lesson-33-锚点设计决定-overview-可见性procurement-中心导致-demand-新建后不可见)
- [Lesson 34: 前端类型定义必须与后端 VO 同步——接口变更后旧字段残留导致-undefined](#lesson-34-前端类型定义必须与后端-vo-同步接口变更后旧字段残留导致-undefined)
- [Lesson 37: 前端状态标签必须本地化，禁止直接显示枚举值](#lesson-37-前端状态标签必须本地化禁止直接显示枚举值)
- [Lesson 40: TypeScript strict 编译必须通过，TS6133/TS2345 是代码腐烂信号](#lesson-40-typescript-strict-编译必须通过ts6133ts2345-是代码腐烂信号)
- [Lesson 41: 前端 API 签名变更后所有调用方必须同步，v1.6 破坏性变更教训](#lesson-41-前端-api-签名变更后所有调用方必须同步v16-破坏性变更教训)
- [Lesson 42: Vue template `v-for` 的 `index` 参数未使用时必须加 `_` 前缀](#lesson-42-vue-template-v-for-的-index-参数未使用时必须加-前缀)
- [Lesson 43: 前端组件 Props 必须与所有调用方对齐——optional 字段不得隐式 required](#lesson-43-前端组件-props-必须与所有调用方对齐optional-字段不得隐式-required)
- [Lesson 44: 前端对话框表格列标签必须提取为 i18n key，禁止硬编码](#lesson-44-前端对话框表格列标签必须提取为-i18n-key禁止硬编码)
- [Lesson 46: `::deep` 禁止覆盖 el-table 内部 width/fixed，固定列仅限列少场景](#lesson-46-deep-禁止覆盖-el-table-内部-widthfixed固定列仅限列少场景)
- [Lesson 47: el-table 空状态 empty-block 宽度异常——table-layout=fixed 下无数据时超出实际列宽](#lesson-47-el-table-空状态-empty-block-宽度异常table-layoutfixed-下无数据时超出实际列宽)
- [Lesson 48: el-select 选项去重不能直接替换 ref——须分离原始数据 ref 和去重结果](#lesson-48-el-select-选项去重不能直接替换-ref须分离原始数据-ref-和去重结果)
- [Lesson 49: el-input-number `controls-position="right"` 导致文字被按钮遮挡](#lesson-49-el-input-number-controls-positionright-导致文字被按钮遮挡)
- [Lesson 50: API 响应必须防御性访问——`data?.content ?? []` 防止空指针](#lesson-50-api-响应必须防御性访问datausercontent--防止空指针)
- [Lesson 52: dist 构建产物与源文件 commit 历史脱节——CSS working copy 未提交导致样式修复无效](#lesson-52-dist-构建产物与源文件-commit-历史脱节css-working-copy-未提交导致样式修复无效)
- [Lesson 53: i18n JSON 中 key 不得重复——后值覆盖前值（JSON 规范未定义合并行为）](#lesson-53-i18n-json-中-key-不得重复后值覆盖前值json-规范未定义合并行为)
- [Lesson 54: 多文件样式修复必须用 grep 全局扫描——防止"改了 A 漏了 B"](#lesson-54-多文件样式修复必须用-grep-全局扫描防止改了-a-漏了-b)
- [Lesson 57: 业务关联变更须从 SPEC → DB → 后端 → 前端八层同步——LogisticsPlan 锚点从 procurementId 改为 qcRecordId](#lesson-57-业务关联变更须从-spec--db--后端--前端八层同步logisticsplan-锚点从-procurementid-改为-qcrecordid)
- [Lesson 58: el-input-number 列宽计算须扣 button×2 + el-col padding，内容 < 150px 时按钮截断](#lesson-58-el-input-number-列宽计算须扣-button×2--el-col-padding内容--150px-时按钮截断)
---

## Lesson 11: 文档与代码同步必须持续进行，不能积累

### 问题

从 git log 看到大量重复的"审计一致性修复"提交：

```
docs: fix: 审计一致性修复 — 补全缺失字段 + 更新实现状态 + 修复过期路由引用
docs: fix: 全量文档审计 — 补全缺失字段 + 更新实现状态 + 修复过期路由引用
docs: fix: 前后端 SPEC + UI 页面文档 + 前端页面三方对齐
```

历史审计轮次：Round 1 → Round 2 → Round 3 → Round 4，每轮都修上轮的残留问题。

### 解决方案

**"文档即代码"规范：**

| 场景 | 规范 |
|------|------|
| 新增 API | 文档和代码同 commit |
| 重构字段 | 文档和代码同 commit，或文档先于代码 |
| 新增枚举 | 文档和代码同 commit，SPEC 枚举表同步更新 |
| 每完成一个 UseCase | 立即更新 docs/business 对应 SPEC 状态 ✅/🔴 |

### 预防

- PR 必须包含文档更新，无文档 = 无 PR
- 定期小规模审计（每周），不要积累成大规模重构

---

## Lesson 12: i18n 必须从第一天规划，不能后期打补丁

### 问题

从 git log 看到 i18n 修复了 4 轮：

```
fix: i18n 审计 round 1 — DashboardPage 全量 i18n化
fix: i18n 审计 round 2 — zh.json 重复key + FactoryPage 全量 i18n化
fix: i18n 审计 round 3 — LogisticsPage/InspectionPage 单位符号 i18n化
fix: i18n 审计 round 4 — client.ts 错误消息 + AppLayout 语言选择器
```

### 解决方案

- **第一天**：选定 vue-i18n，配置 `zh.json` / `ja.json`，在 `vite.config.ts` 中配置 plugin
- **组件规范**：所有用户可见文本必须是 `{{ $t('key') }}`，禁止硬编码
- **CI 检查**：`vue-i18n-extract` 或 ESLint rule 检查未翻译 key
- **占位符原则**：先写英文 key 值（如 `order.status.pending`），后配翻译

### 预防

- 前端骨架生成时，vite 模板必须包含 vue-i18n 插件
- `npm run lint` 包含 i18n 检查

---

## Lesson 14: 命名一致性必须在开发前锁定，禁止中途改名

### 问题

历史命名不一致导致的返工：

| 旧命名（错误） | 新命名（正确） | 发现时机 |
|-------------|-------------|---------|
| `ShippingOrder` | `Procurement` | 开发中期 |
| `LogisticsPlanType` | `PlanType` | 开发中期 |
| `/api/v1/logistics` | `/api/v1/logistics-plans` | 第三轮审计 |

### 解决方案

**命名锁定流程：**

```
1. 写 docs/business/SPEC-*.md 时，同时定义：
   - Domain 实体名（如 Procurement 而非 ShippingOrder）
   - API 路径（如 /logistics-plans 而非 /logistics）
   - 枚举值（如 CooperationStatus 而非 FactoryStatus）

2. codegen 生成骨架后，先审命名再开发
```

### 预防

- 每个模块开发前，先过"命名评审"
- REST API 命名规范：资源用复数名词，嵌套路径不超过 2 层

---

## Lesson 16: 前端 API 客户端类型必须与后端 DTO 严格对齐

### 问题

历史前端字段与后端不一致：
```
TestPage.vue 字段（orderNo/priority 等）← 无后端对应
DemandPage.vue 的 /replenishment-demands ← 旧 endpoint
```

### 解决方案

**OpenAPI 契约优先：**

```yaml
# docs/api/SPEC-B02-procurement.yml
openapi: 3.0.0
paths:
  /api/v1/procurements:
    post:
      requestBody:
        $ref: './components/schemas/ProcurementCreate.yaml'
      responseBody:
        $ref: './components/schemas/ProcurementPageQuery.yaml'
```

- 后端实现严格按 OpenAPI schema
- 前端从 OpenAPI 生成 TypeScript 类型（`openapi-typescript-codegen`）

### 预防

- 后端 DTO 变更 → 必须更新 OpenAPI schema → 触发前端类型重新生成
- 没有 OpenAPI schema 变更 = 不允许合入

---

## Lesson 21: BaseEntity 用 @MappedSuperclass，所有实体继承

### 问题

审计字段（`createTime`、`createBy`、`updateTime`、`updateBy`、`deleted`）在各个实体中重复定义，维护成本高且容易不一致。

### 解决方案

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "create_by")
    private String createBy;

    @Column(name = "update_by")
    private String updateBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;
}
```

所有实体继承 `BaseEntity`：
```java
@Entity
public class Product extends BaseEntity { ... }
```

### 预防

- 新增实体必须继承 BaseEntity，禁止重复定义审计字段
- BaseEntity 变更 → 触发所有子类重新编译

---

## Lesson 24: 测试数据提取禁止用字符串解析（grep）

### 问题

测试从后端日志/响应中用正则提取数据，字符串格式一变测试就崩。

### 根因

```java
// ❌ 错误：用字符串解析提取测试数据
String line = outputLines.stream()
    .filter(l -> l.contains("demand_code"))
    .findFirst().get();
String code = line.split(":")[1].trim();  // 强依赖格式
```

### 解决方案

- 测试数据从测试数据库直接查询，不从日志/响应字符串提取
- 或用结构化响应（JSON），不用纯文本日志

### 预防

- 测试数据提取走结构化查询，不走字符串解析
- 日志只用于调试，不用于断言

---

## Lesson 33: 锚点设计决定 Overview 可见性——Procurement 中心导致 Demand 新建后不可见

### 问题

`/base/overview` 只展示 Procurements。用户新建 Demand 后不会出现在 Overview 中，必须先转采购。

### 设计原则

> **业务链起点 = Overview 入口锚点。禁止以中间环节作为唯一入口锚点。**

### 修复方案

```
/base/overview                          → 双 Tab（需求单 / 发注单）
/base/overview/demand/:demandId         → Demand 锚点（Step1 有数据）
/base/overview/procurement/:procurementId → Procurement 锚点
```

---

## Lesson 34: 前端类型定义必须与后端 VO 同步——接口变更后旧字段残留导致 undefined

### 问题

后端 `DemandVO` 从 `quantity/destination` 改为 `subProductItemsSummary`，前端 Step1 卡片仍引用 `overview.demand.quantity` → 显示 `undefined`。

### 根因

前端 API 类型定义与后端 VO 不同步。

### 修复

接口变更时同步更新：
1. 后端修改 VO → 同步更新前端 API 类型定义
2. 前端模板字段引用 → 对应更新
3. i18n key → 同步调整

### 教训

> **接口变更 = 后端 VO + 前端类型 + 前端模板 + i18n 四处同步。缺一不可。**

---

## Lesson 37: 前端状态标签必须本地化，禁止直接显示枚举值

### 问题

```vue
<!-- ❌ 直接显示枚举值 -->
<el-tag>{{ overview.demand.status }}</el-tag>
<!-- 日语用户看到: PENDING / CONVERTED / CANCELLED -->

<!-- ✅ 显示本地化文本 -->
<el-tag>{{ demandStatusLabel(overview.demand.status) }}</el-tag>
<!-- 日语用户看到: 確認待ち / 発注済み / キャンセル済み -->
```

### 本次修复

```typescript
function demandStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  return t(`demand.status.${status}`)
}
```

### 预防

- 前端组件中所有 `{{ someStatus }}` 文本显示必须经过 i18n 转换
- 新增页面时，状态字段必须使用 label 函数，不允许直接插值

---

## Lesson 40: TypeScript strict 编译必须通过，TS6133/TS2345 是代码腐烂信号

### 问题

```
TS6133: 'onSubCodeChange' is declared but never used        // DemandPage.vue
TS2345: Argument of type '{ currentStep: number }' is not assignable
        to parameter of type 'StepStatus[]'                   // StatusProgressBar.vue
```

### 本次修复

| 错误类型 | 修复方式 |
|---------|---------|
| TS6133 未使用变量 | 移除声明，或用 `_` 前缀 |
| TS2345 类型不匹配 | 检查组件 `defineProps` 定义与调用方是否对齐 |

```typescript
// ✅ 正确：currentStep 声明为可选
defineProps<{
  stepStatuses: StepStatus[]
  currentStep?: number  // ← optional
}>()
```

### 预防

- `vue-tsc --noEmit` 必须加入 CI
- `tsconfig.json` 开启 `strict: true`、`noUnusedLocals: true`、`noUnusedParameters: true`

---

## Lesson 41: 前端 API 签名变更后所有调用方必须同步，v1.6 破坏性变更教训

### 问题

v1.6.0 后端 `convertToProcurement` API 签名变更，前端 `OrderPage.vue` 仍用旧签名调用。

```typescript
// ❌ 旧调用
demandApi.convertToProcurement(id, factoryId as number)

// ✅ 新调用
demandApi.convertToProcurement(id, { factoryId: factoryId as number })
```

### 修复

| 旧 API（v1.5） | 新 API（v1.6） |
|---|---|
| `convertToProcurement(id, factoryId)` | `convertToProcurement(id, { factoryId })` |
| `d.subProductCodes[0]` | `d.subProductItems?.[0]?.subCode` |

### 预防

> **后端 API 破坏性变更 = 前端 API 类型 + 所有调用方 + 单元测试 三处同步。**

---

## Lesson 42: Vue template `v-for` 的 `index` 参数未使用时必须加 `_` 前缀

### 问题

```typescript
// ❌ TS6133: index is declared but never used
tableData.forEach((item, index) => {
  console.log(item)
})
```

### 解决方案

```typescript
// ✅ 正确
tableData.forEach((item, _index) => {
  console.log(item)
})
```

### 预防

- `tsconfig.json` 开启 `noUnusedParameters: true`
- IDE 配置实时提示未使用变量

---

## Lesson 43: 前端组件 Props 必须与所有调用方对齐——optional 字段不得隐式 required

### 问题

`StatusProgressBar.vue` 的 `currentStep` 声明为必需，但 3 个调用页面均未传参 → TS2345。

```typescript
// ❌ 错误：currentStep 声明为必需
defineProps<{
  stepStatuses: StepStatus[]
  currentStep: number
}>()

// ✅ 正确：非必需字段加 ?
defineProps<{
  stepStatuses: StepStatus[]
  currentStep?: number
}>()
```

### 预防

- 设计组件时，默认所有 props 均为 optional（`?`）
- 组件添加新 prop 前，grep 所有调用方确认使用场景

---

## Lesson 44: 前端对话框表格列标签必须提取为 i18n key，禁止硬编码

### 问题

`DemandPage.vue` 中"关联采购单"对话框的表格列标签硬编码：

```vue
<!-- ❌ 硬编码中文字符串 -->
<el-table-column prop="id" label="采购单号" />
```

### 解决方案

```vue
<!-- ✅ i18n 化 -->
<el-table-column prop="id" :label="t('demand.linkedDialog.column.id')" />
```

### 预防

- ESLint rule 检查硬编码文本
- 新增组件时，所有文本显示默认用 `$t()`，不提供文本默认值

---

## Lesson 46: `::deep` 禁止覆盖 el-table 内部 width/fixed，固定列仅限列少场景

### 问题

`DemandPage.vue` 表格出现**表头/表体列错位**（header 与 body 上下不对齐）。

### 根因

```css
/* ❌ 错误1：fixed="right" 在列多（9列）时破坏 header/body 同步 */
<el-table-column width="260" fixed="right">

/* ❌ 错误2：::deep 强制覆盖 el-table 内部 width 计算 */
:deep(.el-table) { width: 100% !important; }
:deep(.el-table__header) { width: 100% !important; }
```

### 本次修复

| 问题 | 修复 |
|------|------|
| 表头/表体错位 | 移除 `fixed="right"` + 移除 `::deep(.el-table) { width: 100% !important }` |
| 表格撑满无边距 | `.table-card :deep(.el-card__body) { padding: 16px; }` |

### 正确写法

```html
<!-- 固定列在列少（≤5）时可用；9列等宽场景下禁用 -->
<el-table ... min-width="160">  <!-- 不是 width -->
```

```css
/* ✅ 只控制外层容器，不动 Element Plus 内部结构 */
.table-card :deep(.el-card__body) { padding: 16px; }

/* ❌ 禁止覆盖 el-table/el-table__header 内部 width */
:deep(.el-table) { width: 100% !important; }
```

### 预防

- `::deep` 只穿透到子组件根节点，不应覆盖框架内部计算逻辑
- 固定列 `fixed` 只在列少、有横向滚动场景下使用

---

## Lesson 47: el-table 空状态 empty-block 宽度异常——table-layout=fixed 下无数据时超出实际列宽

### 问题

有数据时表格列宽正常，无数据时列宽撑破容器：

```
实际列宽总和：1360px
empty-block 宽度：1590px  ← 超出 230px
```

### 根因

`el-scrollbar__view` 被设置为 `display: inline-block`，宽度由内容撑开而非由父容器约束。

### 本次修复

| 方向 | 方案 |
|------|------|
| CSS 防御 | `table-layout="fixed" min-height="200"` — 有数据时防止塌陷 |
| CSS 覆盖 | `::deep(.el-scrollbar__view) { display: block; width: 100%; }` |
| JS 兜底 | `watch(tableData)` + `nextTick` + `ResizeObserver` 动态修正 |

```typescript
// JS 兜底
watch(tableData, () => {
  nextTick(() => {
    const emptyBlock = document.querySelector('.el-table__empty-block') as HTMLElement
    const headerTable = document.querySelector('.el-table__header') as HTMLElement
    if (emptyBlock && headerTable) {
      emptyBlock.style.width = headerTable.offsetWidth + 'px'
    }
  })
})
```

### 预防

- 新增 el-table 前，验证无数据空状态：列宽不超出容器

---

## Lesson 48: el-select 选项去重不能直接替换 ref——须分离原始数据 ref 和去重结果

### 问题

`DemandPage.vue` 的主货号下拉选中有重复选项，修复后下拉框**无法选中任何选项**。

### 根因

```typescript
// ❌ 错误：computed 去重后直接替换 ref
const masterCodeOptions = computed<MasterCodeSuggestVO[]>(() => {
  return Array.from(new Map(raw.value.map(...)).values())
})
// 问题：computed 返回新数组 → el-select v-model 绑定失效

// ❌ 同样错误
masterCodeOptions.value = Array.from(new Map(...).values())
```

### 解决方案

```typescript
// ✅ 正确：分离原始数据 ref 和去重 ref
const masterCodeRaw = ref<MasterCodeSuggestVO[]>([])
const masterCodeOptions = ref<MasterCodeSuggestVO[]>([])

masterCodeRaw.value = res.data.data?.content ?? []
masterCodeOptions.value = Array.from(
  new Map(masterCodeRaw.value.map(item => [item.masterCode, item])).values()
)
```

### 预防

- el-select / el-autocomplete 的 `options` 绑定**必须用 `ref`**，不能用 `computed`

---

## Lesson 49: el-input-number `controls-position="right"` 导致文字被按钮遮挡

### 问题

点击右侧 `+` `-` 按钮后，输入框中的数字文字被按钮遮挡。

### 解决方案

```vue
<!-- ✅ 移除 controls-position，默认按钮在左侧 -->
<el-input-number v-model="formData.quantity" />
```

### 预防

- `el-input-number` 默认 controls 在左侧，使用默认即可
- 如业务要求右侧控制，使用后必须验证按钮不遮挡数字

---

## Lesson 50: API 响应必须防御性访问——`data?.content ?? []` 防止空指针

### 问题

多个页面加载数据时报错崩溃：

```
TypeError: Cannot read properties of null (reading 'content')
```

### 根因

```typescript
// ❌ 直接访问
tableData.value = res.data.data.content  // 若 API 返回错误，res.data.data 为 null
```

### 解决方案

所有页面统一模式：
```typescript
const data = res.data.data
tableData.value = data?.content ?? []
pagination.total = data?.totalElements ?? 0
```

### 预防

- 所有 `res.data.data.content` 访问必须加 `?.` + `?? []` 防御
- API 层统一返回类型中，分页 `content` 应声明为 `T[] | undefined`

---

## Lesson 52: dist 构建产物与源文件 commit 历史脱节——CSS working copy 未提交导致样式修复无效

### 问题

用户报告 `/procurement/demand` 表格"列和值对不上"，但审计发现源文件**已正确修复**。

### 根因

```
dist 构建时间：  4月22 22:54  ← 用户正在运行的版本
fix commit：     4月24 18:02  ← 源文件在 2天后才修复
CSS working copy：未提交       ← 只在 working tree
```

### 诊断方法

```bash
# 1. 查 dist 文件时间戳
ls -la apps/web/dist/assets/demand-zvV9UDK7.js

# 2. 查该文件的最后一次修改 commit
git log --format="%ci %s" -1 -- apps/web/src/pages/procurement/DemandPage.vue
```

### 解决方案

```bash
# 重建 dist
cd apps/web && npm run build

# 或用 dev server（推荐开发时）
cd apps/web && npm run dev
```

### 预防

| 场景 | 规范 |
|------|------|
| 开发调试 | 始终用 `npm run dev`（热更新，不依赖 dist） |
| CSS 样式修复 | working copy 修改后立即 commit，禁止与代码修复分开提交 |
| 生产部署 | `npm run build` 后检查 dist 产物时间戳，确认是新构建 |

> **dist 是构建产物，不是源码。源码修复 ≠ 线上生效，必须重新构建。**

---

## Lesson 53: i18n JSON 中 key 不得重复——后值覆盖前值（JSON 规范未定义合并行为）

### 问题

`zh.json` 和 `ja.json` 中 `orderOverview.loadFailed` 出现两次：

```json
"orderOverview": {
  "loadFailed": "加载订单总览失败",
  "loadFailed": "加载失败"   // ← 第二次出现，后值覆盖前值
}
```

### 诊断方法

```bash
node -e "
const j = require('./src/locales/zh.json');
const keys = [];
function collectKeys(obj, prefix) {
  for (const k of Object.keys(obj)) {
    const full = prefix ? prefix+'.'+k : k;
    keys.push(full);
    if (typeof obj[k]==='object' && obj[k]!==null && !Array.isArray(obj[k])) {
      collectKeys(obj[k], full);
    }
  }
}
collectKeys(j, '');
const dup = keys.filter((k,i) => keys.indexOf(k)!==i);
console.log('Duplicates:', dup.length > 0 ? dup : 'None');
"
```

### 预防

- 大型 JSON i18n 文件用专用编辑器（i18n Ally / WebStorm i18n 插件）
- Git pre-commit hook 检查重复 key

---

## Lesson 54: 多文件样式修复必须用 grep 全局扫描——防止"改了 A 漏了 B"

### 问题

commit `d684024`（"移除 table-layout=fixed"）声称修复了所有页面，但 `DemandPage.vue` **不在修改范围内**。

### 根因

修复者只改了部分已知页面，没有用 grep 全局扫描所有受影响文件。

### 正确流程

```bash
# 1. 先用 grep 找到所有受影响文件
grep -rl "table-layout=\"fixed\"" apps/web/src/pages/ > /tmp/fixed-tables.txt
grep -rl "fixed=\"right\"" apps/web/src/pages/ >> /tmp/fixed-tables.txt
grep -rl ":deep(.el-table) {" apps/web/src/ >> /tmp/tables.txt

# 2. 审查文件清单，确保覆盖完整
cat /tmp/fixed-tables.txt | wc -l

# 3. commit message 必须列出所有修改的文件
```

### 预防

- 样式/布局类修复走 grep 先行流程：找文件 → 审查清单 → 批量修改 → 验证
- Code review 时 review 者也用 grep 验证修复范围完整性

---

## 铁律总结表（前端）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 11 | 文档和代码同 commit，持续审计不积累 | 多轮返工 |
| 12 | i18n 从第一天规划，禁止后期打补丁 | 4轮返工 |
| 14 | 命名在开发前锁定，禁止中途改名 | 大量迁移返工 |
| 16 | 前端类型从 OpenAPI schema 生成，禁止手动对齐 | 字段不匹配 |
| 21 | BaseEntity 用 @MappedSuperclass | 审计字段不一致 |
| 24 | 测试数据提取禁止用字符串解析 | 测试脆弱 |
| 33 | 业务链起点 = Overview 入口锚点 | Demand 新建后不可见 |
| 34 | 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步 | undefined 显示 |
| 37 | 前端状态标签必须本地化，禁止直接显示枚举值 | 日语用户看到乱码 |
| 40 | vue-tsc --noEmit 必须通过（strict + noUnusedLocals） | TS6133/TS2345 累积 |
| 41 | 后端 API 破坏性变更 = 前端 API 类型 + 所有调用方 + 单元测试 三处同步 | 运行时错误 |
| 42 | v-for 的 index 参数未使用加 `_` 前缀 | TS6133 |
| 43 | 组件 Props optional 字段不加 `?` 会导致 TS2345 | 编译失败 |
| 44 | 对话框/表格列标签必须提取为 i18n key，禁止硬编码 | 日语用户无法理解 |
| 46 | `::deep` 禁止覆盖 el-table 内部 width/fixed | 表头/表体错位 |
| 47 | el-table 空状态须防 empty-block 宽度超出 | 列宽溢出容器 |
| 48 | el-select 选项去重不能直接替换 ref | 选择失效 |
| 49 | el-input-number 禁止 controls-position="right" | 数字不可见 |
| 50 | API 响应必须防御性访问——`data?.content ?? []` | 运行时崩溃 |
| 52 | dist 构建产物与源文件 commit 历史脱节 | 样式修复无效 |
| 53 | i18n JSON 中 key 不得重复 | 文案错误 |
| 54 | 多文件样式修复必须用 grep 全局扫描 | 修复不完整 |
| 55 | el-input-number 所在列 span≥4（dialog 宽 800+），按钮才不被截断 | 按钮显示异常 |
| 56 | 表单 diviser 仅在跨语义区大区块时使用，紧凑表单禁止加分隔线 | 视觉噪音 |
| 57 | 业务关联变更须从 SPEC → DB → 后端 → 前端八层同步 | 锚点选错导致无实际 cargo 尺寸 |
| 58 | el-input-number 列宽 = content - 60px(按钮×2) - 16px(el-col padding)，content < 150px 时按钮截断 | 按钮文字被遮挡 |

---

## Lesson 55: el-input-number 最小可用列宽

### 问题

InspectionPage.vue 新规验货弹窗中，`el-input-number`（span 4）在 680px 弹窗内，加减按钮被截断：

```
内容宽度 ≈ (680 - 100 - 16×2) / 4 ≈ 112px  ← 不足以容纳按钮
```

### 判定

`el-input-number` 含左右两个按钮（各 30px），最小可用 content 宽度 ≈ **150px**。

### 公式

```
el-input-number span 最小值 ≈ 4（当 dialog width=800, gutter=10, label-width=86）
  content = (800 - 86 - 10×2) / 4 ≈ 173px ✅ 可用
  span 3 时 content ≈ 115px ❌ 太窄
```

### 正确配置

| 每行字段数 | span | dialog 宽度 | 适用控件 |
|-----------|------|------------|---------|
| 3 列 | span 6 | 800px+ | el-input-number（推荐） |
| 3 列 | span 8 | 800px | el-input-number（宽松） |
| 4 列 | span 6 | 900px | el-select / el-date-picker |
| 6 列 | span 4 | 800px+ | 仅 el-input-number（紧张） |

> **弹窗宽度 ≥ 800px，含数字输入的表单推荐 820-900px。**

---

## Lesson 56: 表单 divider 是视觉噪音

### 问题

InspectionPage.vue 验货弹窗中，两条 `el-divider` 分隔线把连续的验货字段切成孤立区块，破坏表单的整体感。

### 判定

**同一业务语义区的字段应自然分组，不需要显式分隔线。** divider 仅在跨语义区大区块（如"基本信息"vs"财务信息"）时使用。

### 手术

移除 InspectionPage.vue 新规弹窗中的两条 `el-divider`：
- ❌ 删除 `验货信息` 分隔线
- ❌ 删除 `货物信息` 分隔线

### 替代方案

用 `el-row` 自然分行即可。不同语义区之间留 `margin-bottom: 12px` 或空白行。

---

## Lesson 57: 业务关联变更须从 SPEC → DB → 后端 → 前端八层同步

### 问题

`LogisticsPlan`（调配计划）原关联 `procurementId`（采购单），但调配订舱需要**实际装箱尺寸**和**毛重**，这些数据只有在验货完成（步骤3）后才能确定，采购单（步骤2）仅有计划数量。

### 业务链路分析

| 步骤 | 实体 | 实际装箱尺寸 | 毛重 |
|------|------|------------|------|
| 步骤2 | Procurement（采购单） | ❌ 无数据 | ❌ 无数据 |
| 步骤3 | QcRecord（验货记录） | ✅ boxLengthCm × boxWidthCm × boxHeightCm | ✅ grossWeight |
| 步骤4 | LogisticsPlan（调配计划） | 锚点选步骤3才正确 | 锚点选步骤3才正确 |

### 正确变更顺序

```
SPEC 文档     → 定义 qcRecordId 关联，注明业务原因
DB migration → V34 ADD COLUMN qc_record_id
Entity       → LogisticsPlan.java 新增 qcRecordId 字段 + @Index
DTOs         → CreateCmd / UpdateCmd / PageQuery / Query 全部加字段
Assembler    → toDto/toEntity/copyCreate 全部映射 qcRecordId
Repository   → 新增 findByQcRecordIdAndDeletedIsFalse 方法
UseCase      → 校验 qcRecordId 存在且 result=PASS，auto-fill cargo 尺寸
Controller   → Query 参数自动绑定（无需修改）
前端 API     → logistics.ts 类型加 qcRecordId
前端 Vue     → 采购单下拉 → 验货记录下拉，auto-fill cargo 尺寸
i18n        → 新增 qcRecord/qcRecordRequired 等 key
Lesson       → 记录本次教训
```

### 本次变更文件清单

| 层级 | 文件 | 变更 |
|------|------|------|
| SPEC | `SPEC-B04-调配计划-步骤4.md` | v1.2.0，新增 qcRecordId 聚合根 |
| DB | `DB-04-logistics.md` | v1.2.0，新增 qc_record_id 列定义 |
| DB | `V34__logistics_plan_qc_record_id.sql` | 新建，ADD COLUMN qc_record_id |
| Entity | `LogisticsPlan.java` | 新增 qcRecordId 字段 + idx |
| DTOs | `LogisticsPlanCreateCmd.java` | 新增 qcRecordId |
| DTOs | `LogisticsPlanUpdateCmd.java` | 新增 qcRecordId |
| DTOs | `LogisticsPlanPageQuery.java` | 新增 qcRecordId + qcCode |
| DTOs | `LogisticsPlanQuery.java` | 新增 qcRecordId |
| Assembler | `LogisticsPlanAssembler.java` | 新增 qcRecordId 映射 + QcQueryPort 获取 qcCode |
| Repository | `LogisticsPlanRepository.java` | 新增 findByQcRecordIdAndDeletedIsFalse |
| JPA | `LogisticsPlanJpaRepository.java` | 新增 findByQcRecordIdAndDeletedIsFalse |
| UseCase | `LogisticsPlanUseCase.java` | 校验 qcRecord 存在且 result=PASS，auto-fill cargo |
| 前端 API | `logistics.ts` | LogisticsPlanVO + CreateRequest 新增 qcRecordId/qcCode |
| 前端 Vue | `LogisticsPage.vue` | 采购单下拉 → 验货记录下拉 |
| i18n | `zh.json` / `ja.json` | 新增 qcRecord/qcRecordRequired 等 key |

### 溯源

- **EV-057**: LogisticsPlan 锚点错误 → Lesson 57

---

## Lesson 58: el-input-number 列宽计算须扣 button×2 + el-col padding，内容 < 150px 时按钮截断

### 问题

LogisticsPage.vue 新规调配弹窗，货物长/宽/高三列，`el-input-number` 按钮文字被遮挡。显示"货物长"时部分文字不可见。

### 根因分析

**Element Plus `el-input-number` 内部结构**：

```
┌────────────────────────────────────────┐
│ [− 30px] │    input content (数字)    │ [+ 30px] │
└────────────────────────────────────────┘
  按钮         content = 列宽 - 60px        按钮
```

**漏扣项（多层 padding 消耗）**：

| 层 | padding 消耗 |
|----|------------|
| `el-input-number` 按钮 | 30px × 2 = 60px |
| `el-input__wrapper` 内边距 | 11px × 2 = 22px（文字不贴边） |
| `el-col` gutter padding | 8px × 2 = 16px |
| `el-form-item__content` | 0（无额外 padding） |

### 正确公式

```
el-input-number 最小可用 content 宽度 = 150px
最小列宽 = content + 60px（按钮×2）+ 16px（el-col padding）
         = 150 + 60 + 16
         = 226px

820px 弹窗 + label-width=100 + gutter=16 + span 6:
  content ≈ (820 - 100 - 16) / 3 ≈ 234px ✅（实际扣 padding 后 ≈ 213px，够用）

640px 弹窗 + label-width=100 + gutter=16 + span 4:
  content ≈ (640 - 100 - 16) / 4 ≈ 131px ❌（实际 ≈ 113px < 150px → 截断）
```

### 判定速查表

| 弹窗 | gutter | label | 列数 | span | 估算 content | 可用? |
|------|--------|-------|------|------|-------------|--------|
| 640px | 16 | 100 | 3 | span 6 | ≈ 153px | ✅ |
| 680px | 10 | 86 | 3 | span 6 | ≈ 193px | ✅ |
| 820px | 16 | 100 | 3 | span 6 | ≈ 213px | ✅ |
| 640px | 16 | 100 | 4 | span 4 | ≈ 113px | ❌ 截断 |

### 手术（本次）

LogisticsPage.vue：弹窗宽度 640px → 820px，货物尺寸行 span 8 → span 6。

### 溯源

- **EV-058**: LogisticsPage el-input-number 按钮截断 → Lesson 58
