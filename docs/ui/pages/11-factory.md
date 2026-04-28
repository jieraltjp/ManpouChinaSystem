# 页面规格 — 工厂管理

> **版本**: 1.0.0
> **创建**: 2026-04-27
> **路由**: `/base/factory`
> **组件**: `FactoryPage.vue`
> **后端聚合根**: `Factory`
> **API 前缀**: `/api/v1/factories`

---

## 1. 页面定位

工厂管理是系统的基础数据管理模块。管理所有供应商工厂的基础信息（地理位置、联系方式、合作状态），作为发注单转工厂选择的数据源。

---

## 2. 页面布局

```
┌──────────────────────────────────────────────────────────────────────────┐
│  工厂管理                                           [+ 新规录入]        │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐             │
│  │   工厂总数    │  │   合作中      │  │   潜在合作     │             │
│  │      42      │  │      28      │  │       7      │             │
│  └───────────────┘  └───────────────┘  └───────────────┘             │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  工厂名称 [___________]  合作状态 [▼ 全部]  省 [___]  市 [___]  │   │
│  │  县/区 [___]                                           [查询][重置] │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ 工厂编号 │ 工厂名称 │ 省 │ 市 │ 县/区 │ 详细地址 │ 联系人 │ ... │   │
│  │ ──────────────────────────────────────────────────────────────── │   │
│  │ F-001   │ 义乌XX厂 │浙江│金华│浦江县│ 园区路1号 │ 张三  │ ... │   │
│  │ F-002   │ 台州YY厂 │浙江│台州│ 椒江区│ 工业区   │ 李四  │ ... │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                            共 42 条  [<] 1/2 [>]       │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 统计卡

| 统计项 | 数值来源 | 说明 |
|--------|---------|------|
| 工厂总数 | `factoryStats.total` | 所有工厂数量 |
| 合作中 | `factoryStats.active` | `cooperationStatus = ACTIVE` |
| 潜在合作 | `factoryStats.potential` | `cooperationStatus = POTENTIAL` |

> 注意：统计卡仅显示 3 个，与后端 `FactoryStatsDTO` 中返回 5 个字段（total/active/potential/suspended/eliminated）不一致，Suspended 和 Eliminated 未在前端展示。

---

## 4. 筛选栏

| 筛选项 | 字段 | 控件 | 说明 |
|--------|------|------|------|
| 工厂名称 | `factoryName` | el-input | 模糊搜索 |
| 合作状态 | `cooperationStatus` | el-select | ACTIVE / SUSPENDED / ELIMINATED / POTENTIAL |
| 省 | `province` | el-input | 精确匹配 |
| 市 | `city` | el-input | 精确匹配 |
| 县/区 | `county` | el-input | 精确匹配 |

- 查询：重新加载第 1 页
- 重置：清空所有筛选条件，重新加载第 1 页

---

## 5. 表格

### 5.1 表格列

| 列 | prop | min-width | 说明 |
|----|------|----------|------|
| 工厂编号 | `factoryCode` | 140 | 唯一编码（系统生成） |
| 工厂名称 | `factoryName` | 160 | 显示不下时 tooltip |
| 省 | `province` | 100 | 省/直辖市 |
| 市 | `city` | 100 | 城市 |
| 县/区 | `county` | 100 | 区/县 |
| 详细地址 | `roughLocation` | 160 | 粗略地址，显示不下时 tooltip |
| 联系人 | `contactName` | 100 | 联系人姓名 |
| 联系电话 | `contactPhone` | 130 | 手机或座机 |
| 合作状态 | `cooperationStatus` | 110 | 彩色 tag |
| 操作 | — | 150 | 详情 / 编辑 / 删除 |

### 5.2 合作状态 Tag

| 状态值 | 标签 | Tag 类型 |
|--------|------|---------|
| `ACTIVE` | 合作中 | success (绿色) |
| `SUSPENDED` | 已暂停 | warning (橙色) |
| `ELIMINATED` | 已淘汰 | danger (红色) |
| `POTENTIAL` | 潜在合作 | info (灰色) |

### 5.3 操作列

| 按钮 | 行为 |
|------|------|
| 详情 | 打开右侧详情抽屉 |
| 编辑 | 打开编辑弹窗（预填充当前行数据） |
| 删除 | 弹出确认框，确认后调用 DELETE |

---

## 6. 新规/编辑弹窗

**宽度**：760px
**关闭方式**：点击遮罩 / ESC / 取消按钮
**表单分组**：基本信息 / 地理信息 / 联系方式 / 合作信息

### 6.1 基本信息

| 字段 | prop | 必填 | 控件 | 说明 |
|------|------|------|------|------|
| 工厂名称 | `factoryName` | ✅ | el-input | maxlength=128 |

### 6.2 地理信息

| 字段 | prop | 控件 | 说明 |
|------|------|------|------|
| 省 | `province` | el-input | maxlength=64 |
| 市 | `city` | el-input | maxlength=64 |
| 县/区 | `county` | el-input | maxlength=64 |
| 详细地址 | `roughLocation` | el-input | maxlength=500 |

### 6.3 联系方式

| 字段 | prop | 控件 | 说明 |
|------|------|------|------|
| 联系人 | `contactName` | el-input | maxlength=64 |
| 联系电话 | `contactPhone` | el-input | maxlength=32 |
| 微信号 | `contactWechat` | el-input | maxlength=64 |
| QQ号 | `contactQq` | el-input | maxlength=32 |

### 6.4 合作信息

| 字段 | prop | 控件 | 说明 |
|------|------|------|------|
| 合作状态 | `cooperationStatus` | el-select | ACTIVE / SUSPENDED / ELIMINATED / POTENTIAL |
| 备注 | `notes` | el-input (textarea) | rows=2, maxlength=500, show-word-limit |

### 6.5 验证规则

| 字段 | 规则 |
|------|------|
| `factoryName` | 必填，blur 时触发验证 |

---

## 7. 详情抽屉

**方向**：从右侧滑入（direction="rtl"）
**宽度**：600px
**分组布局**：基本信息 / 地理信息 / 联系方式 / 合作信息 / 审计信息

| 字段 | 说明 |
|------|------|
| 工厂编号 | factoryCode |
| 工厂名称 | factoryName |
| 省 / 市 / 县/区 | 三级地址 |
| 详细地址 | roughLocation（全宽） |
| 经度 / 纬度 | 有则显示，无则 "—" |
| 联系人 / 联系电话 | contactName / contactPhone |
| 微信号 / QQ号 | contactWechat / contactQq |
| 合作状态 | 带 tag |
| 创建人 | createBy |
| 创建时间 | createTime |
| 更新时间 | updateTime |
| 备注 | notes（全宽，仅有值时显示） |

**底部按钮**：关闭 / 编辑

---

## 8. API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/factories` | 分页列表（含筛选） |
| GET | `/api/v1/factories/stats` | 统计（total/active/potential/suspended/eliminated） |
| GET | `/api/v1/factories/{id}` | 详情 |
| POST | `/api/v1/factories` | 新建 |
| PUT | `/api/v1/factories/{id}` | 更新 |
| DELETE | `/api/v1/factories/{id}` | 删除 |

---

## 9. 与发注单的关联

工厂数据在以下场景被引用：
- 发注单新建时选择工厂（`/base/overview` → 转采购时选择工厂）
- 发注单内嵌工厂管理（`ProcurementPage.vue` 详情抽屉中的工厂管理）

---

## 10. 缺口分析

| 问题 | 说明 |
|------|------|
| 统计卡只有3个（前端） | 后端返回5个（total/active/potential/suspended/eliminated），前端仅展示其中3个 |
| 经纬度未在前端展示 | 经纬度可录入但表格不显示，仅在详情抽屉可见 |
| paymentTerms 未实现 | dialog 和 drawer 均有 paymentTerms 字段，但 form 表单无此控件 |
