# 页面文档：登录页

> **页面路径**：`/login`
> **组件文件**：`apps/web/src/pages/auth/LoginPage.vue`
> **路由定义**：`apps/web/src/router/index.ts`
> **认证要求**：无需认证（未登录用户可见）
> **最后更新**：2026-04-28

---

## 1. 设计概述

采用 **左右分栏布局（Split Layout）**，1920×1080 全屏设计：

```
┌──────────────────────────────┬──────────────────────────────────┐
│                              │                                  │
│   左品牌面板（55%）           │   右登录面板（45%）              │
│                              │                                  │
│   深色背景 + SVG 占位图       │   白色背景                       │
│   渐变遮罩层                 │                                  │
│                              │   [语言切换]  ← 右上角           │
│   [M] MANPOU                 │                                  │
│   跨境贸易发注管理             │   ┌──────────────────────┐      │
│   覆盖发注、仓储、报关…       │   │  漫普中国管理系统    │      │
│                              │   │  MANPOU China System │      │
│   ● 发注管理 — 订单全流程追踪  │   ├──────────────────────┤      │
│   ● 智能仓储 — 实时库存同步    │   │  用户名               │      │
│   ● 报关物流 — 一体化合规通关  │   │  [________________]  │      │
│                              │   │                       │      │
│   ── MANPOU China System     │   │  密码                 │      │
│                              │   │  [________________]  │      │
│                              │   │                       │      │
│                              │   │  [     登  录     ]   │      │
│                              │   └──────────────────────┘      │
│                              │                                  │
│                              │   © 2024–2026 MANPOU China       │
└──────────────────────────────┴──────────────────────────────────┘
```

**左面板 SVG 占位图元素**：
- 深色渐变天空背景（#1A1A2E → #16213E → #0F3460）
- 网格纹理叠加
- 中心大圆形橙色光晕
- 装饰圆环（左侧/右上角）
- 顶部橙色光晕条
- 背景字母 "M" 作为 Logo 水印
- 半透明渐变遮罩层

**响应式**：≤900px 时左面板收缩为 260px 横幅，特征列表隐藏。

---

## 2. 功能说明

| 功能 | 描述 |
|------|------|
| 语言切换 | 右上角 `el-radio-button`，切换中文/日本語，实时生效并持久化到 localStorage |
| 用户名输入 | `el-input`，前缀图标 `User`，`autocomplete="username"` |
| 密码输入 | `el-input type="password"`，前缀图标 `Lock`，`show-password` |
| 表单验证 | 用户名/密码必填，`trigger: blur` |
| 登录按钮 | `type="primary"` 渐变橙色，loading 状态，回车提交 |
| 登录成功 | 读取 URL `?redirect` 参数跳转，默认 `/dashboard` |
| 登录失败 | `ElMessage.error` 提示错误信息 |

---

## 3. i18n 新增 key

| Key | 中文 | 日语 |
|-----|------|------|
| `auth.welcomeTitle` | 跨境贸易发注管理 | 跨境貿易発注管理 |
| `auth.welcomeDesc` | 覆盖发注、仓储、报关、物流、退税全链路，助您高效运营。 | 発注・倉庫・通関・物流・退税の全过程をカバーし、効率的な運営を実現。 |
| `auth.features[0]` | 发注管理 — 订单全流程追踪 | 発注管理 — 注文の全工程を追跡 |
| `auth.features[1]` | 智能仓储 — 实时库存同步 | スマート倉庫 — リアルタイム在庫同期 |
| `auth.features[2]` | 报关物流 — 一体化合规通关 | 通関物流 — 一体化した合规通関 |
| `auth.language` | 语言 | 言語 |

---

## 4. 布局规格

| 区域 | 宽度 | 背景 | 字体颜色 |
|------|------|------|---------|
| 左品牌面板 | 55%（flex: 0 0 55%） | SVG 占位图 + 渐变遮罩 | 白色 |
| 右登录面板 | 45%（flex: 0 0 45%） | #FFFFFF | 深色 |

---

## 5. 相关文件

| 文件 | 作用 |
|------|------|
| `apps/web/src/pages/auth/LoginPage.vue` | 登录页组件（重写） |
| `apps/web/src/locales/zh.json` | 中文 i18n（新增 features/welcomeTitle/welcomeDesc） |
| `apps/web/src/locales/ja.json` | 日语 i18n（新增 features/welcomeTitle/welcomeDesc） |
| `apps/web/src/locales/index.ts` | 语言切换工具函数（setLocale） |
| `apps/web/src/router/index.ts` | 路由 + 守卫 |
| `apps/web/src/stores/auth.ts` | 认证状态（Pinia） |

---

## 6. 设计决策

- **SVG 占位图**：不依赖外部图片，使用内联 SVG 生成 1920×1080 全屏背景，保证加载速度
- **遮罩层**：防止文字与背景冲突，使用 `linear-gradient` 叠加暗角和橙色光效
- **语言切换**：复用 `locales/index.ts` 中的 `setLocale()`，实时生效，无需刷新页面
- **响应式**：媒体查询 `@media (max-width: 900px)` 左面板缩小为横幅，适配平板/手机

---

*上一页：[docs/ui/README.md](../README.md) | 下一页：[02-仪表盘](./02-dashboard.md)*
