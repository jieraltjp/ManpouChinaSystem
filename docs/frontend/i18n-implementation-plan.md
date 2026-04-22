# 前端 i18n 实现分析报告

> **日期**: 2026-04-22
> **审计范围**: `apps/web/src/` 全部 Vue 页面
> **目标**: 实现中/日双语支持

---

## 1. 当前状态

### 1.1 i18n 依赖

| 检查项 | 状态 |
|--------|------|
| vue-i18n | ❌ **未安装** |
| 翻译文件目录 | ❌ 不存在 |
| Element Plus 国际化配置 | ❌ 未配置（默认英文内部文本） |
| 路由 meta.title 国际化 | ❌ 全部硬编码中文 |

### 1.2 硬编码文本分布

| 文件 | 硬编码中文 | 优先级 |
|------|-----------|--------|
| `src/layouts/AppLayout.vue` | 菜单项 / Logo / 退出登录 | 🔴 P0 |
| `src/pages/auth/LoginPage.vue` | 登录表单所有文本 | 🔴 P0 |
| `src/pages/dashboard/DashboardPage.vue` | 仪表盘所有文本 | 🟡 P2 |
| `src/pages/procurement/DemandPage.vue` | 补货需求所有文本 | 🔴 P0 |
| `src/pages/procurement/OrderPage.vue` | 发注单所有文本 + 状态枚举 | 🔴 P0 |
| `src/pages/procurement/InspectionPage.vue` | 验货记录所有文本 + 状态枚举 | 🔴 P0 |
| `src/pages/procurement/LogisticsPage.vue` | 调配计划所有文本 + 状态枚举 | 🔴 P0 |
| `src/pages/procurement/FactoryPage.vue` | 工厂管理所有文本 | 🔴 P0 |
| `src/pages/test/TestPage.vue` | 测试页面（可跳过） | 🟢 P3 |

---

## 2. 复杂度分析

### 2.1 三类文本

| 类型 | 示例 | 处理方式 |
|------|------|---------|
| **UI 文本** | 按钮文案、表单标签、表格列名 | 提取到 `locales/` JSON 文件 |
| **状态枚举** | `PENDING: '待确认'`, `未定`, `完了` | 状态映射对象单独管理 |
| **业务消息** | `发注单创建成功`、`用户名不能为空` | 提取到 `locales/` JSON 文件 |

### 2.2 关键风险点

**风险 1：状态字符串硬编码在 JS 逻辑中**

```typescript
// OrderPage.vue — 这些字符串直接用于比较
if (status === '完了') { ... }
if (status === '退货') { ... }
```

**影响**：状态值来自后端 API（`ShipmentStatus` 枚举的日文值），前端 JS 逻辑依赖这些字符串做判断。切换语言时 **不能** 改变状态值本身，只能改变显示文本。

**正确做法**：
```typescript
// 用 key 做判断，显示用 i18n
if (status === '完了') { return t('status.approved') }
```

**风险 2：Element Plus 组件文本**

Element Plus 默认英文。需要配置中文 locale：
```typescript
import zhCn from 'element-plus/dist/locale/zh-cn'
app.use(ElementPlus, { locale: zhCn })
```

**风险 3：日期格式**

`dayjs` 默认英文，`dayjs/locale/zh-cn` 和 `dayjs/locale/ja` 需要注册。

---

## 3. 实现方案

### 3.1 技术选型

| 方案 | 库 | 优点 | 缺点 |
|------|-----|------|------|
| **vue-i18n v9** | `vue-i18n` ^9 | 官方 Vue 3 集成、Tree-shaking、TypeScript 支持 | 需要改所有文本 |
| vue-i18n v8 | `vue-i18n` ^8 | 生态成熟 | v9 是 Vue 3 官方推荐 |

**推荐**：vue-i18n ^9

### 3.2 目录结构

```
src/
├── locales/
│   ├── index.ts          # 创建 i18n 实例
│   ├── zh.json           # 中文翻译（默认）
│   ├── ja.json           # 日文翻译
│   └── en.json           # 英文翻译（可选）
├── composables/
│   └── useStatus.ts     # 状态映射（与 i18n 解耦）
├── router/
│   └── index.ts          # meta.title 用 i18n key
├── main.ts               # 挂载 i18n + Element Plus locale
```

### 3.3 翻译文件结构

```json
// locales/zh.json
{
  "app": {
    "name": "漫普中国管理系统",
    "logout": "退出登录"
  },
  "menu": {
    "dashboard": "仪表盘",
    "procurement": "发注管理",
    "demand": "补货需求",
    "order": "发注单",
    "inspection": "验货记录",
    "logistics": "调配计划",
    "factory": "工厂管理"
  },
  "auth": {
    "login": "登录",
    "username": "用户名",
    "password": "密码",
    "usernamePlaceholder": "请输入用户名",
    "passwordPlaceholder": "请输入密码",
    "usernameRequired": "用户名不能为空",
    "passwordRequired": "密码不能为空",
    "loginFailed": "登录失败，请检查用户名和密码"
  },
  "demand": {
    "title": "补货需求",
    "newButton": "新规录入",
    "editDialog": "编辑需求",
    "stat": {
      "total": "全部需求",
      "pending": "待确认",
      "converted": "已转采购"
    },
    "columns": { "code": "需求编号", "type": "类型", ... },
    "type": { "replenishment": "补货（非新品）", "newPurchase": "新品采购" },
    "status": { "PENDING": "待确认", "CONVERTED": "已转采购", "CANCELLED": "已取消" },
    "validation": { "productCodeRequired": "主货号不能为空", "quantityRequired": "需求量不能为空" },
    "messages": { "createSuccess": "需求录入成功", "deleteSuccess": "删除成功" }
  },
  "order": {
    "title": "发注单管理",
    "newButton": "新规发注",
    "status": { "未定": "未定", "完了": "完了", "退货": "退货", ... },
    "billing": { "ZHE_LU_KAI_PIAO": "浙鲁开票", "CHAO_HUI_TUI_SHUI": "超慧退税", ... }
  },
  "inspection": { ... },
  "logistics": { ... },
  "factory": { ... }
}
```

```json
// locales/ja.json
{
  "app": {
    "name": "マンプ（中国）管理システム",
    "logout": "ログアウト"
  },
  "menu": {
    "dashboard": "ダッシュボード",
    "procurement": "発注管理",
    "demand": "補充需求",
    "order": "発注書",
    "inspection": "検品記録",
    "logistics": "配送計画",
    "factory": "工場管理"
  },
  "auth": {
    "login": "ログイン",
    "username": "ユーザー名",
    "password": "パスワード",
    "usernamePlaceholder": "ユーザー名を入力",
    "passwordPlaceholder": "パスワードを入力",
    "usernameRequired": "ユーザー名は必須です",
    "passwordRequired": "パスワードは必須です",
    "loginFailed": "ログイン失敗。ユーザー名とパスワードを確認してください"
  },
  "demand": {
    "title": "補充需求管理",
    "newButton": "新規录入",
    "stat": { "total": "全需求", "pending": "確認待ち", "converted": "発注済み" },
    "type": { "replenishment": "補充（非新品）", "newPurchase": "新品発注" },
    "status": { "PENDING": "確認待ち", "CONVERTED": "発注済み", "CANCELLED": "キャンセル済み" }
  }
}
```

### 3.4 状态映射（与 i18n 解耦）

```typescript
// composables/useStatus.ts
export function useStatus() {
  const { t } = useI18n()

  const orderStatusMap = computed(() => ({
    '未定':   t('order.status.pending'),
    '完了':   t('order.status.completed'),
    '退货':   t('order.status.returned'),
    // ... 其他状态
  }))

  return { orderStatusMap }
}

// Vue 中使用
<el-tag>{{ orderStatusMap[status] || status }}</el-tag>
```

### 3.5 main.ts 集成

```typescript
import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import ja from './locales/ja.json'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn'

const i18n = createI18n({
  legacy: false,
  locale: 'zh',
  fallbackLocale: 'zh',
  messages: { zh, ja }
})

app.use(i18n)
app.use(ElementPlus, { locale: zhCn })
```

### 3.6 路由 meta.title 国际化

```typescript
// router/index.ts
{
  path: '/dashboard',
  name: 'Dashboard',
  meta: { titleKey: 'menu.dashboard' }  // 用 key 而非硬编码
}

// AppLayout.vue 动态标题
<el-menu-item :index="item.path" :key="item.path">
  {{ $t(router.currentRoute.value.meta.titleKey) }}
</el-menu-item>
```

---

## 4. 实施计划

### 第一阶段：基础设施（P0）

| 步骤 | 任务 | 产出 |
|------|------|------|
| 1 | `npm i vue-i18n@9` | package.json 更新 |
| 2 | 创建 `src/locales/zh.json` + `ja.json` 骨架 | 翻译文件 |
| 3 | 创建 `src/locales/index.ts` | i18n 实例配置 |
| 4 | 改造 `main.ts` | i18n + Element Plus locale 挂载 |
| 5 | 路由 meta.title → titleKey | router/index.ts 更新 |

### 第二阶段：核心页面翻译（P0）

| 步骤 | 任务 | 产出 |
|------|------|------|
| 6 | `AppLayout.vue` | 所有菜单/Logo 文本 i18n 化 |
| 7 | `LoginPage.vue` | 登录表单所有文本 i18n 化 |
| 8 | `DemandPage.vue` | 补货需求所有文本 i18n 化 |
| 9 | `OrderPage.vue` | 发注单所有文本 i18n 化（含状态枚举） |
| 10 | `InspectionPage.vue` | 验货记录所有文本 i18n 化 |
| 11 | `LogisticsPage.vue` | 调配计划所有文本 i18n 化 |
| 12 | `FactoryPage.vue` | 工厂管理所有文本 i18n 化 |

### 第三阶段：细节完善（P2）

| 步骤 | 任务 |
|------|------|
| 13 | 状态映射 `useStatus.ts` composable |
| 14 | DashboardPage.vue i18n 化 |
| 15 | 日期格式本地化（dayjs zh/ja locale） |
| 16 | 语言切换按钮（顶部导航或用户菜单） |

---

## 5. 状态枚举一览（需单独管理）

### 发注单状态（`ShipmentStatus`）

| 枚举值 | 中文 | 日文 |
|--------|------|------|
| `未定` | 未定 | 未定 |
| `予定` | 予定 | 予定 |
| `OEM` | OEM | OEM |
| `発注待` | 发注待 | 発注待 |
| `永康` | 永康 | 永康 |
| `直送` | 直送 | 直送 |
| `倉庫着` | 仓库到达 | 倉庫着 |
| `検品` | 检品 | 検品 |
| `現地検品` | 现场检品 | 現地検品 |
| `エア便` | 空运 | エア便 |
| `メーカー直送` | 工厂直送 | メーカー直送 |
| `輸出` | 出口 | 輸出 |
| `国内通関` | 国内报关 | 国内通関 |
| `通関` | 报关 | 通関 |
| `日本着` | 日本到达 | 日本着 |
| `日本通関完了` | 日本报关完成 | 日本通関完了 |
| `会計` | 会计 | 会計 |
| `完了` | 完了 | 完了 |
| `退货` | 退货 | 退货 |

### 调配计划状态（`LogisticsStatus`）

| 枚举值 | 中文 | 日文 |
|--------|------|------|
| `PLANNED` | 调配中 | 計画中 |
| `BOOKED` | 已订舱 | 予約済み |
| `IN_TRANSIT` | 运输中 | 輸送中 |
| `DELIVERED` | 已送达 | 配達済み |

### 验货状态（`QcStatus`）

| 枚举值 | 中文 | 日文 |
|--------|------|------|
| `PENDING` | 待验货 | 確認待ち |
| `COMPLETED` | 已完成 | 完了 |
| `RETURN_REQUESTED` | 退货 | 退货申請中 |

### 需求状态（`DemandStatus`）

| 枚举值 | 中文 | 日文 |
|--------|------|------|
| `PENDING` | 待确认 | 確認待ち |
| `CONVERTED` | 已转采购 | 発注済み |
| `CANCELLED` | 已取消 | キャンセル済み |

### 需求类型（`DemandType`）

| 枚举值 | 中文 | 日文 |
|--------|------|------|
| `REPLENISHMENT` | 补货（非新品） | 補充（非新品） |
| `NEW_PURCHASE` | 新品采购 | 新品発注 |

---

## 6. 测试验证清单

- [ ] 切换到日文后，所有菜单、按钮、标签显示日文
- [ ] 状态枚举显示日文（如"完了"→"完了"）
- [ ] Element Plus 组件内部文本（分页、日期选择器）显示中文
- [ ] 表单验证错误消息显示中文/日文
- [ ] 成功/失败消息显示中文/日文
- [ ] 刷新页面语言状态保持

---

## 7. 后续扩展

| 方向 | 说明 |
|------|------|
| 语言检测 | 浏览器语言自动选择（navigator.language） |
| 用户偏好 | 登录后从后端读取用户语言设置 |
| 日期格式 | 中国：`YYYY-MM-DD` / 日本：`YYYY/MM/DD` |
| 数字格式 | 千分位、货币符号 |
| 后端 i18n | API 返回消息国际化（Result.messageKey） |

---

*本报告由 CODGEM v1.0 分析生成 · 2026-04-22*
