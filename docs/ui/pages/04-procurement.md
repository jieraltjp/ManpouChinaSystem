# 页面文档：发注单管理

> **页面路径**：`/test`
> **组件文件**：`apps/web/src/pages/test/TestPage.vue`
> **路由定义**：`apps/web/src/router/index.ts`
> **认证要求**：需要认证（`requiresAuth: true`）
> **数据状态**：✅ 已对接 manpou-allinone 真实 API（发注单 CRUD）

---

## 1. 页面预览

```
┌─────────────────────────────────────────────────────────┐
│  [折叠]                              [头像] 用户名 [▼]  │
├──────────┬──────────────────────────────────────────────┤
│          │  发注单管理                    [新规发注]      │
│ MANPOU  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│ ─────── │  │ 总数 │ │进行中│ │已完成│ │ 退货 │      │
│ 仪表盘   │  │  42  │ │  28  │ │  12  │ │   2  │      │
│ 发注单 ● │  ├──────┴───────────────────────────────┤ │
│          │  │ 商品代码：[____] 状态：[全部▼]        │ │
│          │  │ 客户公司：[____]        [查询] [重置] │ │
│          │  └─────────────────────────────────────────┘ │
│          │  ┌─────────────────────────────────────────┐ │
│          │  │商品代码│数量│估算批发价│客户公司│状态│操作│ │
│          │  │de077  │ 100│ 1,255.99│ XX会社│未定│详情│ │
│          │  │de078  │  50│   628.00│ YY商事│完了│详情│ │
│          │  └─────────────────────────────────────────┘ │
│          │  共 2 条  [20▼] [◀ 1 ▶]                     │
│          │  ┌─ 详情抽屉（右侧滑出，600px）─────────────┐  │
│          │  │  商品代码   │ 数量    │ 估算批发价(JPY)  │  │
│          │  │  人民币单价 │ 汇率    │ 票点            │  │
│          │  │  状态（Tag）│ 计费方式│ 客户公司        │  │
│          │  │  下单日     │ 厂家出货│ 计划出货日      │  │
│          │  │  商品担当   │ 日本担当│ 中国担当        │  │
│          │  │  发送目的地（跨列）                    │  │
│          │  │  创建时间   │ 更新时间                  │  │
│          │  │              [关闭]  [编辑]             │  │
│          │  └───────────────────────────────────────┘  │
└──────────┴──────────────────────────────────────────────┘
```

---

## 2. 状态枚举

> 与 `ShipmentStatus` 后端枚举完全对齐，详见 `docs/business/SPEC-发注管理流程.md §5`。

| 值 | 中文 | Tag 颜色 | 说明 |
|------|------|---------|------|
| `未定` | 未定 | info（灰） | 还未下单，仅记录需求 |
| `予定` | 予定 | info（灰） | 预计发注 |
| `OEM` | OEM | warning（橙） | OEM 定制产品路径 |
| `発注待` | 発注待 | warning（橙） | 已录入商品，等待下单 |
| `永康` | 永康 | warning（橙） | 1688下单后货物发往永康仓 |
| `直送` | 直送 | primary（蓝） | 1688下单后厂家直接发货 |
| `倉庫着` | 倉庫着 | primary（蓝） | 货物到达仓库 |
| `現地検品` | 現地検品 | primary（蓝） | 现场异地验货 |
| `検品` | 検品 | primary（蓝） | 仓库验货 |
| `エア便` | エア便 | success（绿） | 空运 |
| `メーカー直送` | メーカー直送 | success（绿） | 厂家直送 |
| `輸出` | 輸出 | success（绿） | 已出口 |
| `国内通関` | 国内通関 | success（绿） | 国内报关 |
| `通関` | 通関 | success（绿） | 日本报关 |
| `日本着` | 日本着 | success（绿） | 已到日本 |
| `日本通関完了` | 日本通関完了 | success（绿） | 日本清关完成 |
| `会計` | 会計 | warning（橙） | 财务结算 |
| `完了` | 完了 | info（灰） | **终态**，禁止任何变更 |
| `退货` | 退货 | danger（红） | 退货 |

### 状态路径（SPEC §4）

```
永康路径：未定/未定/OEM → 発注待 → 永康/直送 → 倉庫着 → 検品/現地検品
        → エア便/輸出 → 国内通関 → 通関 → 日本着 → 日本通関完了 → 会計 → 完了
OEM路径：未定/未定/OEM → 発注待 → OEM → 倉庫着 → 現地検品 → メーカー直送
        → 日本着 → 日本通関完了 → 会計 → 完了
直送路径：未定/未定/OEM → 発注待 → 直送 → 倉庫着 → 検品/現地検品 → エア便/輸出
        → 国内通関 → 通関 → 日本着 → 日本通関完了 → 会計 → 完了
```

---

## 3. 功能清单

| 功能 | 描述 | 状态 |
|------|------|------|
| 列表查询 | 分页 + 筛选（商品代码/状态/客户公司） | ✅ |
| 新建发注单 | 弹窗填写所有字段，实时计算批发价 | ✅ |
| 编辑发注单 | 详情抽屉点击编辑，完了状态禁止编辑 | ✅ |
| 状态变更 | 编辑弹窗中选择新状态（非完了行） | ✅ |
| 删除发注单 | 仅 `未定`/`発注待` 状态可删除 | ✅ |
| 详情抽屉 | 右侧滑出，显示完整字段 + 操作按钮 | ✅ |
| 分页 | 10/20/50 每页可选，总数显示 | ✅ |

---

## 4. API 集成

| 操作 | HTTP | 路径 | 说明 |
|------|------|------|------|
| 列表查询 | GET | `/api/v1/procurements` | 分页 + 筛选（后端 0-indexed page） |
| 详情 | GET | `/api/v1/procurements/{id}` | 单条记录 |
| 新建 | POST | `/api/v1/procurements` | 创建发注单 |
| 更新 | PATCH | `/api/v1/procurements/{id}` | 部分更新 |
| 删除 | DELETE | `/api/v1/procurements/{id}` | 逻辑删除 |
| 状态变更 | PATCH | `/api/v1/procurements/{id}` | body 中携带 status 字段 |

> **注意**：列表查询前端传 `page-1`（0-indexed），后端直接使用。

---

## 5. 价格计算

批发价 JPY 由前端实时计算并传给后端存储：

```
estimatedPriceJpy = (priceRmb / taxPoint × 1.02 × 1.2) × exchangeRate × 1.05
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `priceRmb` | 人民币单价 | 用户输入 |
| `taxPoint` | 票点 | 1.1 |
| `exchangeRate` | CNY→JPY 汇率 | 21.5 |
| 固定系数 | 国内流通费 1.02、利润 1.2、跨境费 1.05 | 系统内置 |

---

## 6. 组件结构

```
TestPage.vue
├── 页面头部
│   ├── 标题 "发注单管理"
│   └── "新规发注" 按钮
├── 统计行 (el-row, 4个统计卡)
│   ├── 发注单总数（橙色 Document 图标）
│   ├── 进行中（橙色 Clock 图标）
│   ├── 已完成（绿色 CircleCheck 图标）
│   └── 退货（红色 Warning 图标）
├── 筛选栏 (el-card, shadow=never)
│   ├── 商品代码输入（placeholder: 如 de077）
│   ├── 状态下拉（19个选项）
│   ├── 客户公司输入
│   ├── 查询按钮（primary）
│   └── 重置按钮
├── 数据表格 (el-table, stripe, v-loading)
│   ├── 商品代码（橙色 monospace，点击打开详情）
│   ├── 数量（右对齐）
│   ├── 估算批发价 JPY（右对齐，千分位格式化）
│   ├── 客户公司（tooltip 溢出）
│   ├── 商品担当
│   ├── 计划出货日
│   ├── 状态（Tag，颜色映射）
│   ├── 创建时间（yyyy-MM-dd HH:mm:ss 格式）
│   └── 操作（详情 / 编辑 / 删除）
├── 分页（bottom-right, background）
└── 详情抽屉 (el-drawer, rtl, 600px, el-descriptions column=2)
    ├── 商品代码 / 数量 / 人民币单价 / 汇率
    ├── 票点 / 估算批发价(JPY) / 状态 / 计费方式
    ├── 客户公司 / 下单日 / 厂家出货日 / 计划出货日
    ├── 商品担当 / 日本担当 / 中国担当
    ├── 发送目的地（跨列）
    ├── 创建时间 / 更新时间（跨列）
    └── 底部操作（关闭 / 编辑）
```

> **注意**：详情抽屉字段取自 `ProcurementPageQuery`（全部 23 个字段），前端按 UI 需要展示部分字段。

---

## 7. 字段对照

| 前端字段 | API 字段 | 说明 |
|---------|---------|------|
| `factoryId` | `factoryId` | 关联工厂 |
| `productCode` | `productCode` | 主货号 |
| `subProductCode` | `subProductCode` | 子货号/枝番（颜色） |
| `quantity` | `quantity` | 订购数量 |
| `material` | `material` | 材质 |
| `requiresQc` | `requiresQc` | 是否需要检测 |
| `priceRmb` | `priceRmb` | 人民币单价 |
| `exchangeRate` | `exchangeRate` | CNY→JPY 汇率 |
| `taxPoint` | `taxPoint` | 票点（默认 1.1） |
| `billingType` | `billingType` | 报关类型 |
| `estimatedPriceJpy` | `estimatedPriceJpy` | 估算批发价（前端计算，后端存储） |
| `customsRemarks` | `customsRemarks` | 报关备注 |
| `instructionManual` | `instructionManual` | 说明书 |
| `orderDate` | `orderDate` | 下单日（yyyy-MM-dd） |
| `factoryShipDate` | `factoryShipDate` | 厂家出货日 |
| `plannedShipDate` | `plannedShipDate` | 计划出货日 |
| `actualShipDate` | `actualShipDate` | 实际出货日 |
| `productLead` | `productLead` | 商品担当 |
| `japanLead` | `japanLead` | 日本担当 |
| `chinaLead` | `chinaLead` | 中国担当 |
| `destination` | `destination` | 发送目的地 |
| `customerCompany` | `customerCompany` | 客户公司 |
| `status` | `status` | 发注单状态 |
| `createTime` | `createTime` | 创建时间 |
| `updateTime` | `updateTime` | 更新时间 |

---

## 8. 相关文件

| 文件 | 作用 |
|------|------|
| `apps/web/src/pages/test/TestPage.vue` | 发注单管理组件 |
| `apps/web/src/api/procurement.ts` | API 类型与请求封装 |
| `apps/manpou-allinone/.../ProcurementController.java` | 后端 REST 接口 |
| `apps/manpou-allinone/.../ShipmentStatus.java` | 状态枚举 + FSM 规则 |
| `docs/business/SPEC-发注管理流程.md` | 业务规格文档 |
| `docs/business/DOMAIN-发注管理领域模型.md` | 领域模型 |

---

*上一页：[03-示例列表](./03-examples.md)*
