# Lesson 93 — el-dialog 弹窗关闭行为与居中标准化

## 规则

所有 `el-dialog` 必须同时显式设置两个属性：

```vue
<el-dialog
  v-model="visible"
  :close-on-click-modal="false"
  center
>
```

**`:close-on-click-modal="false"`**：禁止点击遮罩层关闭弹窗，用户只能通过底部按钮（Cancel/Save/Close）或右上角 X 关闭。
**`center`**：弹窗在屏幕水平和垂直方向居中显示。

**何时允许默认值**：
- 纯通知/展示类弹窗（无操作按钮）→ 必须显式 `:close-on-click-modal="false"`
- 即使用户要求"点击空白关闭"，也不应打破此规范——表单类弹窗意外关闭会导致数据丢失

## Why

1. **数据安全**：表单弹窗（新建/编辑）意外关闭 → 表单数据丢失，用户须重新填写，体验差且易产生脏数据
2. **一致性**：不同页面弹窗关闭行为不统一，用户认知成本高
3. **Element Plus 默认值**：`close-on-click-modal` 默认为 `true`，`center` 默认为 `false`，两者均需显式覆盖

## How to apply

**新建弹窗**：在 `<el-dialog>` 标签上立即添加这两个属性，不要依赖默认值。

**存量审计**：
```bash
# 查找所有 el-dialog，逐一检查
grep -rn "<el-dialog" apps/web/src/pages --include="*.vue"
```

**判断标准**：

| 弹窗类型 | close-on-click-modal | center |
|----------|----------------------|--------|
| 表单弹窗（新建/编辑） | `false` | `true` |
| 确认类弹窗（有 Confirm/Cancel） | `false` | `true` |
| 查看类弹窗（无操作按钮） | `false` | `true` |
| 纯通知/警告弹窗 | `false` | `true` |

> Element Plus 中 `center` 属性同时控制标题和内容的居中，无性能影响，强制添加。

## 实际案例（2026-05-20）

**问题**：全项目 27 个弹窗全面审计，发现 24 个缺少 `center`，部分缺少 `:close-on-click-modal="false"`。用户报告 ProcurementPage 新建弹窗点击空白处意外关闭。

**影响范围**（修改前）：
- `ProcurementPage.vue` — 3 个弹窗（主新建/工厂新建/快速创建商品）
- `DemandPage.vue` — 2 个弹窗
- `SalesRecordPage.vue` — 3 个弹窗
- `UserPage.vue`、`RolePage.vue`、`FactoryPage.vue`、`ProductPage.vue`、`ShipPage.vue` 等 13 个页面共 27 个弹窗

**修复后**：全部 27 个弹窗统一添加 `center` + `:close-on-click-modal="false"`。

## 多行写法示例

```vue
<!-- 正确写法（多行） -->
<el-dialog
  v-model="dialogVisible"
  title="标题"
  width="600px"
  :close-on-click-modal="false"
  center
>
  ...
</el-dialog>
```

```vue
<!-- 错误写法（依赖默认值） -->
<el-dialog v-model="dialogVisible" title="标题" width="600px">
  ...
</el-dialog>
```

## 相关铁律

- Lesson 54：多文件修改必须 grep 全局扫描——审计弹窗属性时同理，需确保不遗漏
- Lesson 40：vue-tsc --noEmit 必须通过——属性拼写错误会导致 TS 报错
