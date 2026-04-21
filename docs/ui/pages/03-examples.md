# 页面文档：示例列表

> **页面路径**：`/examples`
> **组件文件**：`apps/web/src/pages/dashboard/ExamplesPage.vue`
> **路由定义**：`apps/web/src/router/index.ts`
> **认证要求**：需要认证（`requiresAuth: true`）
> **权限要求**：`example:read`

---

## 1. 页面预览

```
┌─────────────────────────────────────────────────────────┐
│  [折叠]                              [头像] admin [▼]  │
├──────────┬──────────────────────────────────────────────┤
│          │  示例列表                                    │
│  MANPOU │  ┌─────────────────────────────────────────┐ │
│  ───────│  │ [新建] [🔍 搜索名称...] [🔄 刷新]      │ │
│  仪表盘 │  └─────────────────────────────────────────┘ │
│  示例列表●│  ┌─────────────────────────────────────────┐ │
│  采购单  │  │ ID │ 名称      │ 状态  │ 创建时间   │操作│ │
│          │  │ 1  │ 示例项A  │ ACTIVE│ 2026-04-10 │编辑│ │
│          │  │ 2  │ 示例项B  │ INACTV│ 2026-04-09 │编辑│ │
│          │  └─────────────────────────────────────────┘ │
│          │                            共 2 条  [<] [1] [>] │
│          │                                              │
│          │  ┌ 新建/编辑 对话框 ─────────────────────┐  │
│          │  │  名称    [__________________________] │  │
│          │  │  描述    [__________________________] │  │
│          │  │           [取消]  [确定]             │  │
│          │  └───────────────────────────────────────┘  │
└──────────┴──────────────────────────────────────────────┘
```

**截图**：`docs/ui/screenshots/03-examples.png`

---

## 2. 截图对应

| 资源 | 路径 |
|------|------|
| 页面截图 | `docs/ui/screenshots/03-examples.png` |

> **如何截图**：启动前端后访问 `http://localhost:13000/examples`，登录后截取。

---

## 3. 功能说明

| 功能 | 描述 |
|------|------|
| 列表加载 | 页面挂载时调用 `GET /examples`，分页参数 `page`/`size` |
| 搜索 | 输入名称后回车/点击刷新，按 `name` 过滤 |
| 新建 | 点击「新建」弹出 `el-dialog`，表单字段：名称（必填）、描述 |
| 编辑 | 点击「编辑」弹出对话框，填充当前行数据，`PUT /examples/{id}` |
| 删除 | 点击「删除」直接调用 `DELETE /examples/{id}`，成功后刷新 |
| 分页 | `el-pagination`，`current-page` 绑定 `page` |
| 状态标签 | `ACTIVE` → 绿色 success tag，`INACTIVE` → 灰色 info tag |

---

## 4. 源代码

```typescript
// apps/web/src/pages/dashboard/ExamplesPage.vue — 核心逻辑

interface ExampleItem {
  id: number
  name: string
  description: string
  status: string
  createTime: string
}

// 加载列表
async function loadItems() {
  const params: Record<string, unknown> = { page: page.value, size: pageSize.value }
  if (keyword.value) params.name = keyword.value
  const res = await client.get('/examples', { params })
  items.value = res.data.data?.content ?? []
  totalElements.value = res.data.data?.totalElements ?? 0
}

// 新建 / 编辑提交
async function submitForm() {
  if (editingId.value) {
    await client.put(`/examples/${editingId.value}`, form)   // 更新
  } else {
    await client.post('/examples', form)                     // 创建
  }
}

// 删除
async function deleteRow(row: ExampleItem) {
  await client.delete(`/examples/${row.id}`)
  ElMessage.success('删除成功')
  loadItems()
}
```

---

## 5. API 映射

| 操作 | HTTP | 路径 | 后端服务 |
|------|------|------|---------|
| 列表查询 | GET | `/examples?page=1&size=20&name=xx` | user-service |
| 新建 | POST | `/examples` | user-service |
| 更新 | PUT | `/examples/{id}` | user-service |
| 删除 | DELETE | `/examples/{id}` | user-service |

---

## 6. 组件结构

```
ExamplesPage.vue
├── 操作栏 (el-card + el-space)
│   ├── 新建按钮 (Plus icon)
│   ├── 搜索输入框 (Search icon)
│   └── 刷新按钮 (Refresh icon)
├── 表格 (el-table)
│   ├── ID 列
│   ├── 名称列
│   ├── 状态列 (el-tag)
│   ├── 创建时间列
│   └── 操作列 (编辑/删除 link buttons)
├── 分页 (el-pagination)
└── 新建/编辑对话框 (el-dialog)
    ├── 名称输入 (el-form-item)
    ├── 描述输入 (el-input type=textarea)
    └── 取消/确定按钮
```

---

## 7. 相关文件

| 文件 | 作用 |
|------|------|
| `apps/web/src/pages/dashboard/ExamplesPage.vue` | 示例列表组件 |
| `apps/web/src/api/client.ts` | Axios 实例 |
| `apps/web/src/stores/auth.ts` | 认证状态（权限校验） |
| `docs/ui/pages/02-dashboard.md` | 上一页：仪表盘 |
| `docs/ui/pages/04-procurement.md` | 下一页：采购单管理 |

---

*上一页：[02-仪表盘](./02-dashboard.md) | 下一页：[04-采购单管理](./04-procurement.md)*
