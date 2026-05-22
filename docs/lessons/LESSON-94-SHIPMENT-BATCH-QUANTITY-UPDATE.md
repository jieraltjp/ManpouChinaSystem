---
name: lesson-94-shipment-batch-quantity-update
description: ShipmentBatch 修改数量时状态自转移异常导致数量也回滚
type: feedback
---

# Lesson 94: 状态自转移异常导致数量字段回滚

## 现象

`/procurement/shipment-batch?procurementId=6` 页面修改数量后保存失败，页面无数据更新。

## 根因

| 层 | 文件 | 问题 |
|----|------|------|
| 前端 | `ShipmentBatchPage.vue` | 每次提交都带 `status: formData.status`（当前值），即使用户未改状态 |
| Assembler | `ShipmentBatchAssembler.java:64` | 无条件调用 `entity.updateStatus(cmd.getStatus())` |
| 领域 | `ShipmentBatchStatus.canTransitionTo()` | 禁止相同状态转移（`待验货→待验货` 报错） |
| 事务 | `ShipmentBatchUseCase.update()` | 单事务 — 状态异常导致数量变更也被回滚 |

**事务链**：`setShipmentQuantity(新值)` → `updateStatus(同状态)` → 抛异常 → **事务回滚（包括数量变更）**。

## 修复

`ShipmentBatchAssembler.java` 第 64 行，加守卫：

```java
// 修复前（错误）
if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());

// 修复后（正确）
if (cmd.getStatus() != null && cmd.getStatus() != entity.getStatus()) entity.updateStatus(cmd.getStatus());
```

## Why

前端总会传当前状态值（用户不改也要传），但领域状态机禁止自转移。在 assembler 层加守卫比改领域层更干净 —— 领域层保持严格 FSM，assembler 做幂等化。
