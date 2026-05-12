# 页面规格 — 船只管理

> **版本**: v1.0.0
> **创建**: 2026-05-12
> **状态**: 📋 待开发（Phase 2）
> **对应业务步号**: B-12（货柜与船只管理）
> **对应后端**: `ShipController` → `POST/GET/PUT/DELETE /api/v1/ships`
> **对应 API**: `shipApi`（`api/ship.ts`，新建）
> **对应路由**: `/base/ship`
> **对应 DB**: DB-14（ship 表，V18）
> **对应 SPEC**: `SPEC-B12 §6.2`

---

## 1. 页面定位

船只管理是货柜物流的前置基础数据。用于维护船名、船号、出发港、目的港等信息，供货柜关联船只时选择。

**前置**：无
**后续**：货柜管理（`/base/container`）→ 分配船只

---

## 2. 列表页

### 2.1 筛选器

| 字段 | 控件 | 说明 |
|------|------|------|
| 船名 shipName | `el-input` | 模糊搜索 |
| 船号 shipNumber | `el-input` | 模糊搜索 |
| 目的港 arrivalPort | `el-select`（可输） | 模糊搜索 |
| 重置 | `el-button` | 清空所有筛选条件 |
| 新增船只 | `el-button type="primary"` | hasPermission('ship:create') |

### 2.2 表格

| 列 | 字段 | 宽度 | 说明 |
|----|------|------|------|
| 序号 | — | 60px | 居中 |
| 船名 | shipName | min-width=120 | — |
| 船号 | shipNumber | min-width=120 | — |
| 船公司 | carrier | min-width=100 | 可为空 |
| 出发港 | departurePort | min-width=100 | 可为空 |
| 目的港 | arrivalPort | min-width=100 | 可为空 |
| 关联货柜数 | containerCount | 100px | 右对齐，暂无则显示 0 |
| 创建时间 | createTime | min-width=160 | locale 格式 |
| 操作 | — | 120px | 编辑（hasPermission('ship:update')） |

### 2.3 状态

无状态字段，船只记录只有正常/已删除两种状态（is_deleted）。

---

## 3. 新增/编辑弹窗

### 3.1 字段

| 字段 | 控件 | 必填 | 校验 | 说明 |
|------|------|:----:|------|------|
| 船名 shipName | `el-input` | ✅ | 非空，最大64字符 | — |
| 船号 shipNumber | `el-input` | ✅ | 非空，最大32字符，唯一性校验 | — |
| 船公司 carrier | `el-input` | 否 | 最大64字符 | — |
| 出发港 departurePort | `el-input` | 否 | 最大64字符 | — |
| 目的港 arrivalPort | `el-input` | 否 | 最大64字符 | — |

### 3.2 布局

- 弹窗宽度：`500px`
- 两列布局（船名+船号 / 船公司+出发港 / 目的港）
- 底部：取消 + 确定按钮

### 3.3 唯一性校验

编辑时船号不可修改（已有关联货柜不允许改船号）。

---

## 4. 详情抽屉

| 字段 | 说明 |
|------|------|
| 船名 | — |
| 船号 | — |
| 船公司 | — |
| 出发港 | — |
| 目的港 | — |
| 创建人 | createBy |
| 创建时间 | createTime（locale 格式） |
| 更新人 | updateBy |
| 更新时间 | updateTime（locale 格式） |

底部操作：关闭 / 编辑（hasPermission('ship:update')）

---

## 5. 删除确认

**前置校验**：调用 `GET /api/v1/ships/{id}/containers` 确认无关联货柜。

| 情况 | 处理 |
|------|------|
| 有关联货柜 | 拒绝删除，提示"该船只下有 N 个货柜，请先解除关联" |
| 无关联货柜 | 弹出确认框，执行软删除 |

---

## 6. API 端点

| 方法 | 路径 | @PreAuthorize | 说明 |
|------|------|:-------------:|------|
| `GET` | `/api/v1/ships` | `ship:read` | 分页列表 |
| `GET` | `/api/v1/ships/{id}` | `ship:read` | 详情 |
| `POST` | `/api/v1/ships` | `ship:create` | 新增 |
| `PUT` | `/api/v1/ships/{id}` | `ship:update` | 编辑 |
| `DELETE` | `/api/v1/ships/{id}` | `ship:delete` | 删除（前置校验） |
| `GET` | `/api/v1/ships/{id}/containers` | `ship:read` | 查某船所有货柜 |

---

## 7. i18n key 前缀

`ship.*`

| key | 中文 | 日文 |
|-----|------|------|
| `ship.title` | 船只管理 | 船舶管理 |
| `ship.newButton` | 新增船只 | 新規船舶追加 |
| `ship.column.shipName` | 船名 | 船名 |
| `ship.column.shipNumber` | 船号 | 船番号 |
| `ship.column.carrier` | 船公司 | 船会社 |
| `ship.column.departurePort` | 出发港 | 出発港 |
| `ship.column.arrivalPort` | 目的港 | 到着港 |
| `ship.column.containerCount` | 关联货柜数 | コンテナ数 |
| `ship.column.createTime` | 创建时间 | 作成日時 |
| `ship.dialog.title.create` | 新增船只 | 新規船舶追加 |
| `ship.dialog.title.update` | 编辑船只 | 船舶編集 |
| `ship.dialog.shipName` | 船名 | 船名 |
| `ship.dialog.shipNumber` | 船号 | 船番号 |
| `ship.dialog.carrier` | 船公司 | 船会社 |
| `ship.dialog.departurePort` | 出发港 | 出発港 |
| `ship.dialog.arrivalPort` | 目的港 | 到着港 |
| `ship.dialog.deleteConfirm` | 确认删除该船只？ | 当該船舶を削除しますか？ |
| `ship.message.deleteSuccess` | 删除成功 | 削除成功 |
| `ship.message.deleteFailed.hasContainers` | 该船只下有 {n} 个货柜，请先解除关联 | 当該船舶には {n} 個のコンテナがあります。先に关联を解除してください。 |
| `ship.filter.reset` | 重置 | リセット |
| `ship.filter.search` | 查询 | 検索 |

---

## 8. 路由守卫

```typescript
// router/index.ts
{
  path: 'ship',
  name: 'Ship',
  component: () => import('@/pages/base/ShipManagementPage.vue'),
  meta: {
    titleKey: 'menu.ship',
    requiresAuth: true,
    roles: ['ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER'],
  },
}
```

菜单项：`menu.ship` = "船只管理"，位于基础数据分组（base）。
