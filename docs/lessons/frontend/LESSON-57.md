# LESSON 57 — 业务关联变更：锚点选择决定数据质量

> **日期**: 2026-04-27
> **触发**: LogisticsPlan（调配计划）原关联 `procurementId`（采购单），改为关联 `qcRecordId`（验货记录）
> **根因**: 调配订舱需要**实际装箱尺寸**（长×宽×高）和**毛重**，这些数据只有在验货完成（步骤3）后才能确定，采购单（步骤2）仅有计划数量

---

## 业务背景

**步骤链路**：

```
步骤2: Procurement(采购单)
  → 有: 计划数量 / 估算重量 / 计划尺寸
  → 无: 实际测量数据

步骤3: QcRecord(验货记录)
  → 有: 实际装箱尺寸(boxLengthCm × boxWidthCm × boxHeightCm) / 实际毛重(grossWeight)
  → 验完货才知道 → 用于调配订舱判断 SEA/AIR 路径

步骤4: LogisticsPlan(调配计划)
  → 需要: 实际 cargo 尺寸 + 毛重 → 锚点应是步骤3，而非步骤2
```

**判断标准**：

| 问题 | 原关联(procurementId) | 新关联(qcRecordId) |
|------|----------------------|--------------------|
| 实际装箱尺寸 | ❌ 无数据，仅计划数 | ✅ 验货实测 |
| 毛重 | ❌ 无数据 | ✅ 验货实测 |
| 订舱计算 | ❌ 估算，偏差大 | ✅ 精确 |
| 业务语义 | "这笔采购的货要发" | "这批验完的货要发" |

---

## 变更路径（必须按顺序）

**正确顺序：SPEC → DB → 后端 → 前端**

```
1. SPEC 文档     → 定义 qcRecordId 关联，注明业务原因
2. DB migration → V34 ADD COLUMN qc_record_id
3. Entity       → LogisticsPlan.java 新增 qcRecordId 字段 + @Index
4. DTOs         → CreateCmd / UpdateCmd / PageQuery / Query 全部加字段
5. Assembler    → toDto/toEntity/copyCreate 全部映射 qcRecordId
6. Repository   → 新增 findByQcRecordIdAndDeletedIsFalse 方法
7. UseCase      → 校验 qcRecordId 存在且 result=PASS，auto-fill cargo 尺寸
8. Controller   → Query 参数自动绑定（无需修改）
9. 前端 API     → logistics.ts 类型加 qcRecordId
10. 前端 Vue     → 采购单下拉 → 验货记录下拉，auto-fill cargo 尺寸
11. i18n        → 新增 qcRecord/qcRecordRequired 等 key
12. Lesson      → 记录本次教训
```

**违反顺序的后果**：

| 违规场景 | 后果 |
|---------|------|
| 先改后端代码，后改 SPEC | 代码与文档不一致，新人困惑 |
| 先改前端，后改后端 | 前端 API 调用 404 或字段不匹配 |
| 后端改了，DB 没改 | 启动失败或写入报错 |
| 改完后未更新 i18n | 日语用户看到原始 key |

---

## 溯源

- **EV-057**: LogisticsPlan 锚点错误 → Lesson 57

---

## 铁律

> **业务关联变更 = SPEC 先 + DB migration + 后端 Entity/DTO/Assembler/UseCase + 前端 API/Vue/i18n 八层同步。**
