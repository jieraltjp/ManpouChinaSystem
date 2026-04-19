# 页面文档：采购单管理

> **页面路径**：`/test`
> **组件文件**：`apps/web/src/pages/test/TestPage.vue`
> **路由定义**：`apps/web/src/router/index.ts`
> **认证要求**：需要认证（`requiresAuth: true`）
> **数据状态**：**当前为模拟数据**，待对接 procurement-service 真实 API

---

## 1. 页面预览

```
┌─────────────────────────────────────────────────────────┐
│  [折叠]                              [头像] admin [▼]  │
├──────────┬──────────────────────────────────────────────┤
│          │  采购单管理                    [新建采购单]   │
│  企业平台│  ┌─────────────────────────────────────────┐ │
│  ───────│  │ 采购单号：[___________] 状态：[全部▼]  │ │
│  仪表盘 │  │ 优先级：[全部▼]     [查询] [重置]      │ │
│  示例列表│  └─────────────────────────────────────────┘ │
│  采购单 ●│  ┌─────────────────────────────────────────┐ │
│          │  │ ☐ │ 单号          │优先级│状态  │类型│操作│ │
│          │  │ ☐ │ PO20260419001 │普通│草稿 │出口│详情│ │
│          │  │ ☐ │ PO20260418003 │紧急│待审核│内贸│详情│ │
│          │  │ ☐ │ PO20260417005 │高  │已批准│出口│详情│ │
│          │  │ ☐ │ PO20260416002 │普通│已拒绝│出口│详情│ │
│          │  │ ☐ │ PO20260415008 │普通│已取消│内贸│详情│ │
│          │  └─────────────────────────────────────────┘ │
│          │  共 5 条  [10▼] [< 1 >]                   │
│          │                                              │
│          │  ┌─ 详情抽屉（右侧滑出） ──────────────┐  │
│          │  │  采购单号    │ 状态                  │  │
│          │  │  联系人      │ 联系电话              │  │
│          │  │  发货地址（跨列）                   │  │
│          │  │  优先级      │ 类型                  │  │
│          │  │  创建时间    │                      │  │
│          │  │  备注（跨列）                      │  │
│          │  │              [关闭]  [提交审核]     │  │
│          │  └───────────────────────────────────────┘  │
└──────────┴──────────────────────────────────────────────┘
```

**截图**：`docs/ui/screenshots/04-procurement.png`

---

## 2. 截图对应

| 资源 | 路径 |
|------|------|
| 页面截图 | `docs/ui/screenshots/04-procurement.png` |

> **如何截图**：启动前端后访问 `http://localhost:3000/test`，登录后截取。

---

## 3. 功能说明

| 功能 | 描述 | 状态 |
|------|------|------|
| 筛选栏 | 采购单号（模糊）、状态、优先级 | ✅ 界面完成，筛选逻辑待实现 |
| 表格 | 分页列表，点击行打开详情 | ✅ 界面完成，数据为模拟 |
| 详情抽屉 | 右侧滑出，显示完整字段 | ✅ 界面完成 |
| 新建按钮 | 提示"新建采购单页面（待实现）" | ⚠️ 提示占位 |
| 编辑按钮 | 仅 DRAFT 状态可点击 | ⚠️ 提示占位 |
| 删除按钮 | 仅 DRAFT 状态可点击 | ⚠️ 提示占位 |
| 提交审核 | 仅 DRAFT 状态可见 | ⚠️ 提示占位 |

---

## 4. 状态与优先级

### 状态枚举

| 值 | 中文 | Tag 类型 | 允许操作 |
|-----|------|---------|---------|
| `DRAFT` | 草稿 | info | 编辑 / 删除 / 提交审核 |
| `PENDING` | 待审核 | warning | — |
| `APPROVED` | 已批准 | success | — |
| `REJECTED` | 已拒绝 | danger | — |
| `CANCELLED` | 已取消 | info | — |

### 优先级枚举

| 值 | 中文 | Tag 类型 |
|-----|------|---------|
| `URGENT` | 紧急 | danger（红色） |
| `HIGH` | 高 | warning（橙色） |
| `NORMAL` | 普通 | info（灰色） |

---

## 5. 模拟数据

```typescript
const tableData = ref<PurchaseOrder[]>([
  { id:1, orderNo:'PO20260419001', contactName:'张三', isExport:true,
    status:'DRAFT', priority:'NORMAL', createTime:'2026-04-19 10:30:00' },
  { id:2, orderNo:'PO20260418003', contactName:'李四', isExport:false,
    status:'PENDING', priority:'URGENT', createTime:'2026-04-18 14:22:00' },
  { id:3, orderNo:'PO20260417005', contactName:'王五', isExport:true,
    status:'APPROVED', priority:'HIGH', createTime:'2026-04-17 09:15:00' },
  { id:4, orderNo:'PO20260416002', contactName:'赵六', isExport:true,
    status:'REJECTED', priority:'NORMAL', createTime:'2026-04-16 16:45:00' },
  { id:5, orderNo:'PO20260415008', contactName:'孙七', isExport:false,
    status:'CANCELLED', priority:'NORMAL', createTime:'2026-04-15 11:20:00' },
])
```

---

## 6. 待实现 API

| 操作 | HTTP | 路径 | 说明 |
|------|------|------|------|
| 列表查询 | GET | `/api/v1/purchase-orders` | 分页 + 筛选 |
| 详情 | GET | `/api/v1/purchase-orders/{id}` | 单条记录 |
| 新建 | POST | `/api/v1/purchase-orders` | 创建采购单 |
| 提交审核 | POST | `/api/v1/purchase-orders/{id}/submit` | DRAFT → PENDING |
| 审批 | POST | `/api/v1/purchase-orders/{id}/approve` | PENDING → APPROVED |
| 拒绝 | POST | `/api/v1/purchase-orders/{id}/reject` | PENDING → REJECTED |

> **注意**：当前所有操作均为 `ElMessage.info/warning/success` 提示占位，需对接 procurement-service。

---

## 7. 组件结构

```
TestPage.vue
├── 页面头部
│   ├── 标题 "采购单管理"
│   └── "新建采购单" 按钮
├── 筛选栏 (el-card)
│   ├── 采购单号输入
│   ├── 状态下拉（5个选项）
│   ├── 优先级下拉（3个选项）
│   ├── 查询按钮
│   └── 重置按钮
├── 数据表格 (el-table)
│   ├── 多选列
│   ├── 单号列（monospace 字体，蓝色）
│   ├── 联系人 / 发货地址
│   ├── 优先级标签
│   ├── 状态标签
│   ├── 类型标签（出口/内贸）
│   ├── 创建时间
│   └── 操作列（详情/编辑/删除）
├── 分页
└── 详情抽屉 (el-drawer direction=rtl)
    ├── 采购单号 / 状态
    ├── 联系人 / 联系电话
    ├── 发货地址（跨2列）
    ├── 优先级 / 类型
    ├── 创建时间（跨2列）
    ├── 备注（跨2列）
    └── 底部操作（关闭 / 提交审核）
```

---

## 8. 相关文件

| 文件 | 作用 |
|------|------|
| `apps/web/src/pages/test/TestPage.vue` | 采购单管理组件 |
| `docs/pro/05-procurement-service.md` | 后端 procurement-service 文档 |
| `docs/ui/ARCHITECTURE.md` | 系统架构图（Kafka 事件流） |
| `docs/ui/pages/03-examples.md` | 上一页：示例列表 |

---

*上一页：[03-示例列表](./03-examples.md)*
