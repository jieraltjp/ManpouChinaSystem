# 审计记录 — /procurement/demand 表格列值不对应

> 日期：2026-04-27
> 触发：用户报告表格列和值对不上
> 范围：业务层 → 数据库 → 后端 → 前端 全链路审计

---

## 问题现象

```
http://192.168.12.198:13000/procurement/demand
表格列头（i18n 标签）：需求编号 / 类型 / 子货号 / 数量 / 目的地 / ...
表格数据（prop 绑定）：demandCode / demandType / subProductCode / quantity / destination / ...
用户感知：列和值对应不上
```

---

## 全链路审计结论

### Layer 0：数据库 Schema（V31 — `replenishment_demand`）

| 列名 | 类型 | nullable | 索引 | 状态 |
|------|------|---------|------|------|
| `id` | BIGINT PK | NO | ✅ | ✅ |
| `demand_code` | VARCHAR(32) UNIQUE | NO | ✅ UNIQUE | ✅ |
| `demand_type` | VARCHAR(32) | NO | ✅ | ✅ |
| `product_code` | VARCHAR(32) | NO | ✅ | ✅ |
| `sub_product_code` | VARCHAR(64) | YES | ✅ | ✅ |
| `quantity` | INT | YES | ⚠️ **Entity 有 @Index，SQL 未写** | ⚠️ |
| `destination` | VARCHAR(128) | YES | ⚠️ **同上** | ⚠️ |
| `status` | VARCHAR(32) | NO | ✅ | ✅ |
| `linked_procurement_id` | BIGINT | YES | — | ✅ |

### Layer 1：后端 Entity（`ReplenishmentDemand.java`）

所有 JPA 映射正确。⚠️ `quantity` 无默认值（前端已有 `min=1` 校验，防御到位）。

### Layer 2：后端 DTO（`ReplenishmentDemandPageQuery.java`）

14个字段与 Entity 1:1 对应，无遗漏。

### Layer 3：后端 Assembler

v2.0.0 直接字段映射，无 JSON 序列化。⚠️ `demandCode` 生成格式为 `DM-YYYYMMDD-NNN`，但 SPEC 文档写的是 `D-YYYYMMDD-NNN`（不一致）。

### Layer 4：后端 Controller

路径 `/api/v1/demands` 与前端 `demand.ts` 的 `'/demands'` 拼接后完整匹配。✅

### Layer 5：前端 API 类型（`demand.ts`）

14个字段与后端 `ReplenishmentDemandPageQuery` 完全对齐。✅

### Layer 6：前端表格模板（`DemandPage.vue`）

| 列头 i18n | prop | 绑定 | 状态 |
|-----------|------|------|------|
| `demand.column.demandCode` → "需求编号" | `demandCode` | `row.demandCode` | ✅ |
| `demand.column.demandType` → "类型" | `demandType` | `row.demandType` | ✅ |
| `demand.column.subProductCode` → "子货号" | `subProductCode` | `row.subProductCode` | ✅ |
| `demand.column.quantity` → "数量" | `quantity` | `row.quantity` | ✅ |
| `demand.column.destination` → "目的地" | `destination` | `row.destination` | ✅ |
| `demand.column.japanLead` → "日本担当" | `japanLead` | `row.japanLead` | ✅ |
| `demand.column.status` → "状态" | `status` | `row.status` | ✅ |
| `demand.column.createTime` → "录入时间" | `createTime` | `row.createTime` | ✅ |
| `demand.column.action` → "操作" | — | 按钮组 | ✅ |

⚠️ `productCode`（主货号）在 i18n 中有 key，但表格模板未展示。

---

## 真正根因

**dist 构建产物与源文件 commit 历史脱节：**

```
dist 构建时间：    4月22 22:54  ← 用户正在运行的版本
fix commit 时间：  4月24 18:02  ← 源文件在 2天后才修复
CSS working copy：未提交          ← variables.css 的 ::deep 修复只在 working tree
```

源文件 `DemandPage.vue` **已正确修复**：
- `table-layout="fixed"` → 已移除（commit d684024）
- `width` → 已改为 `min-width`
- `fixed="right"` → 已移除

用户看到的是 2 天前的旧 dist，包含了旧的 `table-layout="fixed"` 和 `width` 样式。

---

## 已修复项

| # | 问题 | 状态 | 修复方式 |
|---|------|------|---------|
| Bug-1 | dist 旧构建导致样式修复无效 | ⚠️ 需 `npm run build` | 见下方命令 |
| Bug-2 | `orderOverview.loadFailed` 重复 key（zh/ja） | ✅ 已修复 | 删除重复 key |
| Bug-3 | `quantity`/`destination` DB 索引缺失 | ⏸ 待确认 Flyway | 需要手动 ALTER 或重建索引 |
| Bug-4 | demandCode 格式文档 vs 实现不一致 | 📋 待决策 | SPEC `D-` vs Assembler `DM-` |
| Bug-5 | UI 文档与 i18n `productCode` 不一致 | 📋 可选 | 确认是否需要展示主货号列 |

---

## 用户必须执行的操作

```bash
# 立即：重建 dist
cd apps/web && npm run build

# 或使用 dev server（推荐开发时）
cd apps/web && npm run dev
```

---

## 新增 Lesson

| # | 主题 | 写入位置 |
|---|------|---------|
| 52 | dist 构建产物与源文件 commit 历史脱节 | `LESSONS-FRONTEND.md` |
| 53 | i18n JSON 中 key 不得重复 | `LESSONS-FRONTEND.md` |
| 54 | 多文件样式修复必须用 grep 全局扫描 | `LESSONS-FRONTEND.md` |

---

## 溯源黑匣子

```
基因: EV-046
触发: commit d684024（"移除 table-layout=fixed"）声称修复所有页面
     但 DemandPage.vue 不在修改范围内
根因: 修复者只改了部分已知页面，未用 grep 全局扫描
防止: 样式/布局类修复必须 grep 先行
     grep -r "table-layout=\"fixed\"" apps/web/src/pages/
     grep -r "fixed=\"right\"" apps/web/src/pages/
```

---

## 文件变更记录

| 文件 | 变更 |
|------|------|
| `docs/lessons/zh.json` | 删除 `orderOverview.loadFailed` 重复 key |
| `docs/lessons/ja.json` | 同上 |
| `docs/lessons/README.md` | 新建，总索引 |
| `docs/lessons/LESSONS-BACKEND.md` | 新建，后端 lesson |
| `docs/lessons/LESSONS-OPS.md` | 新建，运维 lesson |
| `docs/lessons/LESSONS-DATABASE.md` | 新建，数据库 lesson |
| `docs/lessons/LESSONS-FRONTEND.md` | 新建，前端 lesson（含 Lesson 46-54）|
| `docs/lessons/Lombok-Decoupling-DI-Lessons.md` | 替换为重定向索引 |
| `memory/MEMORY.md` | 更新，加入 lessons 索引和本会话成果 |
