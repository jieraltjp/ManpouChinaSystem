# SPEC-B14: 发注单检测字段文本化

## 需求

将发注单 `requires_qc` 字段从 `Boolean`（开关）改为 `String`（文本输入），支持自由填写检测类型或备注。

**URL:** https://manpouchina.manpou.site/procurement/procurement
**字段:** `requires_qc`（需要检测）
**当前:** `el-switch`（是/否）
**目标:** `el-input`（自由文本）

---

## 变更范围

### 1. 数据库

| 文件 | 变更 |
|------|------|
| `V28__procurement_requires_qc_text.sql`（新建） | `ALTER TABLE procurement MODIFY COLUMN requires_qc VARCHAR(128) DEFAULT NULL` |

### 2. 后端 — procurement 模块

| 文件 | 变更 |
|------|------|
| `Procurement.java` | `Boolean requiresQc` → `String requiresQc` |
| `ProcurementCreateCmd.java` | `Boolean requiresQc` → `String requiresQc` |
| `ProcurementUpdateCmd.java` | `Boolean requiresQc` → `String requiresQc` |
| `ProcurementPageQuery.java` | `Boolean requiresQc` → `String requiresQc` |
| `ProcurementAssembler.java` | 4处赋值/读取（`setRequiresQc`/`getRequiresQc`），仅类型变更 |
| `DevTestDataInitializer.java` | `boolean requiresQc` → `String requiresQc`，传入值 `"true"` / `"false"` |

### 3. 后端 — order 模块

| 文件 | 变更 |
|------|------|
| `OrderProcurementSelectorDTO.java` | `Boolean requiresQc` → `String requiresQc` |
| `OrderOverviewPageVO.ProcurementVO` | `Boolean requiresQc` → `String requiresQc` |
| `OrderOverviewAssembler.toProcurementVO()` | 直接传递 String，无需逻辑变更 |

### 4. 前端 — ProcurementPage.vue

| 位置 | 变更 |
|------|------|
| 新建/编辑表单 | `el-switch` → `el-input`（`v-model="formData.requiresQc"`） |
| 详情抽屉展示 | `currentRow.requiresQc ? yes : no` → `currentRow.requiresQc \|\| '-'` |
| 货号变更填充 | `p.requiresQc` 赋值逻辑不变（已是 String） |
| 默认值 | `requiresQc: ''`（空字符串替代 `false`） |
| 编辑回填 | `requiresQc: row?.requiresQc ?? ''` |
| API 提交 | `requiresQc: formData.requiresQc \|\| undefined`（空字符串不提交） |

### 5. i18n

| 文件 | 变更 |
|------|------|
| `zh.json` | `order.drawer.requiresQc` → 显示值文本；无 `order.drawer.yes/no` 子键引用 |
| `ja.json` | 同上 |

---

## 业务逻辑影响分析

### ✅ 无影响（保持不变）

| 位置 | 原因 |
|------|------|
| `ProcurementQcPassedEventListener` | 不引用 `Procurement.requiresQc`，仅用 `qcRecord.getQcType()` |
| `ProcurementUseCase` 状态推进 | 不依赖 `requiresQc` |
| `QcRecordCompletedEvent` 广播 | 同上 |

### ⚠️ 需注意

- **旧数据兼容性**: 已有的 `BIT(1)` 数据迁移后 MySQL 会将 `1`/`0` 转为 `"1"`/`"0"` 字符串，查询行为不变
- **Assembler null 判断**: `if (cmd.getRequiresQc() != null)` 逻辑不变（String 的 `!= null` 语义相同）
- **前端 truthy 判断**: 详情页原来 `requiresQc ? yes : no` 改为 `requiresQc \|\| '-'`

---

## 数据库迁移 SQL

```sql
-- V28__procurement_requires_qc_text.sql
ALTER TABLE procurement
  MODIFY COLUMN requires_qc VARCHAR(128) DEFAULT NULL COMMENT '检测类型/备注（文本，可为空）';
```

---

## 实施顺序

1. **后端** — Entity + DTO + Assembler + DevTestDataInitializer
2. **数据库** — 新增 V28 migration
3. **前端** — ProcurementPage.vue 表单 + 详情
4. **i18n** — 清理 `order.drawer.yes/no` 子键引用（如仅用于此字段）
5. **编译验证** — `mvn compile` + `npm run type-check`
6. **启动验证** — 重启 allinone，访问页面测试新建/编辑/详情
