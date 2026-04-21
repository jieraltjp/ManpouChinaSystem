# 页面文档：登录页

> **页面路径**：`/login`
> **组件文件**：`apps/web/src/pages/auth/LoginPage.vue`
> **路由定义**：`apps/web/src/router/index.ts`
> **认证要求**：无需认证（未登录用户可见）

---

## 1. 页面预览

```
┌──────────────────────────────────────────────────┐
│                                                  │
│              渐变背景 (#667eea → #764ba2)        │
│                                                  │
│         ┌──────────────────────────┐            │
│         │  MANPOU 企业管理系统      │            │
│         │    MANPOU Enterprise      │            │
│         ├──────────────────────────┤            │
│         │  用户名                   │            │
│         │  [________________________] │            │
│         │                           │            │
│         │  密码                     │            │
│         │  [________________________] │            │
│         │                           │            │
│         │  [      登  录      ]      │  ← 主按钮  │
│         └──────────────────────────┘            │
│                                                  │
└──────────────────────────────────────────────────┘
```

**截图**：`docs/ui/screenshots/01-login.png`

---

## 2. 截图对应

| 资源 | 路径 |
|------|------|
| 页面截图 | `docs/ui/screenshots/01-login.png` |
| 设计稿（若有） | — |

> **如何截图**：启动前端后访问 `http://localhost:13000/login`，截取完整浏览器窗口。

---

## 3. 功能说明

| 功能 | 描述 |
|------|------|
| 用户名输入 | `el-input`，前缀图标 `User`，`autocomplete="username"` |
| 密码输入 | `el-input type="password"`，前缀图标 `Lock`，`show-password` |
| 表单验证 | 用户名/密码必填，`trigger: blur` |
| 登录按钮 | `type="primary"`，点击后显示 loading 状态 |
| 回车提交 | `@keyup.enter` 触发 `handleLogin` |
| 登录成功 | 读取 URL `?redirect` 参数跳转，默认 `/dashboard` |
| 登录失败 | 静默失败（console.error），按钮恢复，可重试 |

---

## 4. 源代码

```vue
<!-- apps/web/src/pages/auth/LoginPage.vue -->
<template>
  <div class="login-container">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>MANPOU 企业管理系统</h2>
          <p class="subtitle">MANPOU Enterprise</p>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top"
        @submit.prevent="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名"
            :prefix-icon="User" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"
            :prefix-icon="Lock" autocomplete="current-password" show-password
            @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading"
            @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
```

---

## 5. 样式关键

```css
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  border-radius: 12px;
}

.login-btn {
  width: 100%;
  height: 42px;
  font-size: 16px;
}
```

---

## 6. 路由守卫逻辑

```
用户访问 /login
  ↓
路由守卫检查 useAuthStore().isAuthenticated
  ↓
已登录 → 重定向到 /dashboard
未登录 → 显示登录页
  ↓
登录成功后
  ↓
读取 ?redirect 查询参数（如 ?redirect=/procurement）
  ↓
跳转到 redirect 值，无则默认 /dashboard
```

---

## 7. 相关文件

| 文件 | 作用 |
|------|------|
| `apps/web/src/pages/auth/LoginPage.vue` | 登录页组件 |
| `apps/web/src/router/index.ts` | 路由 + 守卫 |
| `apps/web/src/stores/auth.ts` | 认证状态（Pinia） |
| `apps/web/src/api/adapters/auth.ts` | 登录 API 适配器 |
| `docs/ui/ARCHITECTURE.md` | 系统架构图（认证流程） |

---

*上一页：[docs/ui/README.md](../README.md) | 下一页：[02-仪表盘](./02-dashboard.md)*
