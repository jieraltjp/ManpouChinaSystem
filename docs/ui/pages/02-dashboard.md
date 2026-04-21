# 页面文档：仪表盘

> **页面路径**：`/dashboard`
> **组件文件**：`apps/web/src/pages/dashboard/DashboardPage.vue`
> **路由定义**：`apps/web/src/router/index.ts`
> **认证要求**：需要认证（`requiresAuth: true`）
> **权限要求**：无

---

## 1. 页面预览

```
┌─────────────────────────────────────────────────────────┐
│  [折叠]                              [头像] 用户名 [▼]  │
├──────────┬──────────────────────────────────────────────┤
│          │                                              │
│  MANPOU │  仪表盘                                      │
│  ───────│  ┌──────────┬──────────┬──────────┬────────┐ │
│  仪表盘 ●│  │ 👤       │ 🔑       │ 🏢       │ ⏱     │ │
│  示例列表│  │ admin    │ admin    │ manpou   │ 14 min │ │
│  采购单  │  │ 当前用户  │ 角色      │ 租户      │剩余时间│ │
│          │  └──────────┴──────────┴──────────┴────────┘ │
│          │                                              │
│          │  JWT Claims                                  │
│          │  ┌──────────────────────────────────────┐   │
│          │  │ User ID (sub)    │ tenantId           │   │
│          │  │ Roles           │ Permissions        │   │
│          │  │ Issued At       │ Expires At         │   │
│          │  └──────────────────────────────────────┘   │
│          │                                              │
│          │  快捷入口                                    │
│          │  [示例列表]  [重新登录]                      │
│          │                                              │
└──────────┴──────────────────────────────────────────────┘
```

**截图**：`docs/ui/screenshots/02-dashboard.png`

---

## 2. 截图对应

| 资源 | 路径 |
|------|------|
| 页面截图 | `docs/ui/screenshots/02-dashboard.png` |
| 侧边栏展开 | `docs/ui/screenshots/layout-sidebar-expanded.png` |
| 侧边栏收起 | `docs/ui/screenshots/layout-sidebar-collapsed.png` |

> **如何截图**：启动前端后访问 `http://localhost:13000/dashboard`，登录后截取。

---

## 3. 功能说明

| 功能 | 描述 |
|------|------|
| 统计卡片 ×4 | 当前用户 / 角色 / 租户 / Token 剩余时间 |
| JWT Claims 表格 | `el-descriptions`，展示 sub/tenantId/roles/permissions/iat/exp |
| 角色标签 | `el-tag` 遍历 `auth.claims.roles` |
| 权限标签 | `el-tag type="info"` 遍历 `auth.claims.permissions` |
| Token 倒计时 | `dayjs.unix(exp).diff(dayjs(), 'minute')`，剩余分钟数 |
| 快捷入口 | 跳转示例列表 / 重新登录 |

---

## 4. 源代码

```vue
<!-- apps/web/src/pages/dashboard/DashboardPage.vue -->
<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409eff"><User /></el-icon>
            <div>
              <div class="stat-value">{{ auth.claims?.username || '—' }}</div>
              <div class="stat-label">当前用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <!-- 其余 3 张卡片类似 -->
    </el-row>

    <!-- JWT Claims -->
    <el-card shadow="hover" class="info-card">
      <template #header><span>JWT Claims</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="User ID (sub)">
          {{ auth.claims?.sub || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="Tenant ID">
          {{ auth.claims?.tenantId || '—' }}
        </el-descriptions-item>
        <!-- ... -->
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { User, Key, OfficeBuilding, Timer } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'

const auth = useAuthStore()
const tokenExpiry = computed(() => {
  if (!auth.claims?.exp) return '—'
  const remaining = dayjs.unix(auth.claims.exp).diff(dayjs(), 'minute')
  return remaining > 0 ? `${remaining} min` : '已过期'
})
</script>
```

---

## 5. Pinia Store 数据来源

```typescript
// apps/web/src/stores/auth.ts
auth.claims = {
  sub: "用户ID（JWT sub）",
  username: "admin",
  roles: ["admin"],
  permissions: ["example:read"],
  tenantId: "manpou",
  iat: 17113000000,   // Issued At
  exp: 17113000900,   // Expires (15min 后)
}
```

---

## 6. 相关文件

| 文件 | 作用 |
|------|------|
| `apps/web/src/pages/dashboard/DashboardPage.vue` | 仪表盘组件 |
| `apps/web/src/layouts/AppLayout.vue` | 布局（侧边栏 + 顶栏） |
| `apps/web/src/stores/auth.ts` | 认证状态 |
| `docs/ui/pages/01-login.md` | 登录页（认证起点） |

---

*上一页：[01-登录页](./01-login.md) | 下一页：[03-示例列表](./03-examples.md)*
