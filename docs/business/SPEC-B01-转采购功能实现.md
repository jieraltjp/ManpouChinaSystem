# SPEC-B01-转采购功能实现

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **更新**: 2026-04-23（补充元数据字段）
> **状态**: ✅ 已实现
> **对应 SPEC**: SPEC-B01-补货需求-步骤1.md §6 待办

---

## 1. 业务规则

### 1.1 转采购流程

需求单（ReplenishmentDemand）状态为 `PENDING` 时，点击「转采购」：

```
需求单列表 → 点「转采购」 → 发注单弹窗（需求数据预填）→ 选工厂 → 保存发注单 → 调用转换API → 需求状态变为 CONVERTED
```

**前置条件：**
- 需求状态 = `PENDING`（只有待确认可转）
- `factoryId` 必填（DB-12 要求）

**转换后：**
- 需求单 `status` → `CONVERTED`
- 需求单 `linkedProcurementId` → 新建的发注单 ID

### 1.2 误操作回退

| 场景 | 处理方式 |
|------|---------|
| 用户选错工厂 | 正常保存后可再次编辑发注单 |
| 用户想取消转采购 | 关闭弹窗即可，需求状态不变 |
| 用户已转错 | 需求单状态已变 CONVERTED，**无法直接撤销** |
| 需撤销已转采购 | 需手动：修改发注单状态为「完了/退货」→ 后端删除保护依赖此规则 |

### 1.3 业务边界

- 一个需求单只能转一次（CONVERTED 状态不可再转）
- 转采购时生成的是**发注单草稿**，工厂出货日/价格等字段需人工补充
- 转采购后，原需求单不可删除（已有 linkedProcurementId 关联）

---

## 2. UI 设计

### 2.1 按钮文案

| 当前状态 | 按钮文案 | 说明 |
|---------|---------|------|
| `PENDING` | 转采购 | 主操作按钮 |
| `CONVERTED` | 查看采购单 | 跳转/弹窗查看关联发注单 |
| `CANCELLED` | — | 不显示按钮 |

### 2.2 按钮样式

- `PENDING` → `type="primary"`（主色调）
- `CONVERTED` → `link type="default"`（次要样式）

### 2.3 操作列结构

```
[ 转采购 ]  [ 编辑 ]  [ 删除 ]
   或
[ 查看采购单 ]  [ 编辑 ]  [ 删除 ]
```

---

## 3. 技术实现

### 3.1 前端改动（OrderPage.vue）

在 `onConvert(row)` 中：
1. 设置 `convertingDemandRow.value = row`（记录当前需求）
2. 调用已有 `onDemandChange(row.id)` 预填商品信息
3. 打开发注单弹窗（`dialogMode = 'create'`）

`onSubmit()` 中，检测 `convertingDemandRow` 存在时：
1. 正常保存发注单 → 获得 `procurementId`
2. 调用 `demandApi.convertToProcurement(demandId, procurementId)`
3. 成功后清空 `convertingDemandRow`
4. 刷新需求列表

### 3.2 API 调用链

```
POST /api/v1/procurements  → 创建发注单
POST /api/v1/demands/{id}/convert?procurementId=X  → 需求状态推进
```

两个操作均在后端开启事务，任何一步失败均回滚。

### 3.3 后端状态校验（已有）

`ReplenishmentDemand.convertToProcurement()` 已在实体层校验：
- `status != PENDING` → 抛异常：`"demand.already_processed"`

---

## 4. 代码改动清单

### 前端
- [x] ✅ `OrderPage.vue`: `onConvert(row)` 实现预填+打开弹窗（转采购按钮已实现）
- [x] ✅ `DemandPage.vue`: `onViewLinked(row)` 打开发注单详情
- [x] ✅ `api/demand.ts`: `convertToProcurement` API 已实现

### 后端
- [x] `POST /api/v1/demands/{id}/convert?procurementId=` ✅ 已实现
- [x] `ReplenishmentDemand.convertToProcurement(procurementId)` ✅ 已有
- [x] `ReplenishmentDemandUseCase.convertToProcurement(demandId, procurementId)` ✅ 已有
