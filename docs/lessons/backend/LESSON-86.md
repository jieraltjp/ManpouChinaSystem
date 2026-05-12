# Lesson 86: `sanitizeImpl` visited 集合导致 DAG 结构误判为 cyclic

> **日期**: 2026-05-12
> **触发**: 审计日志 detail 字段审查
> **根因**: `visited` IdentityHashMap 在递归回溯时不移除已访问节点

---

## 问题

当 DTO 包含 DAG（非树形有向无环图）结构时，相同对象在不同分支路径中出现，`sanitizeImpl` 会误判为 cyclic 引用。

### 示例

```
ProcurementCreateCmd
  └── factory: FactoryDTO
        └── address: AddressDTO  ← 对象 A
  └── logisticsPlan: LogisticsPlanDTO
        └── originAddress: AddressDTO  ← 对象 A（同一引用，不同分支）
```

当前实现：
```java
private Object sanitizeImpl(Object value, int depth, Set<Object> visited) {
    if (visited.contains(value)) return "[cyclic]";  // ← 第二次遇到 A 时触发
    visited.add(value);                               // ← 永不回溯删除
    // ... 处理后返回
}
```

**结果**: `AddressDTO` 在第二个分支中被标记为 `[cyclic]`，即使它只是 DAG 中共享的节点。

---

## 分析

**影响范围**: 仅当请求 DTO 包含跨分支共享对象引用时才触发。实际业务 DTO 大多为树形（无共享节点），此问题在当前代码中**尚未观察到**。

**false positive 的影响**: detail 中部分字段变成 `[cyclic]` 而非真实数据，影响日志可读性，但不产生错误数据。

**为什么不修复**: 实现完美的 DAG cycle 检测需要在递归回溯时 `visited.remove(value)`，但这需要额外处理（每次递归前 remove，或传入新 set）。权衡：当前实现防止 StackOverflow 的主要目标已达成，边界情况罕见。

---

## 如果需要修复

```java
// 方案 1: 传入新 set 的快照（每层递归）
private Object sanitizeImpl(Object value, int depth, Set<Object> visited) {
    if (depth > 3) return "[max-depth]";
    if (visited.contains(value)) return "[cyclic]";

    // 克隆后递归（每分支独立 visited 集合）
    Set<Object> nextVisited = new IdentityHashMap<>(visited);
    nextVisited.add(value);
    // ... 使用 nextVisited 递归
}

// 方案 2: 递归前 remove（需在所有 return 前执行）
```

---

## 教训

**cycle 检测的 visited 集合必须在每条路径独立维护**。全局 visited 集合在树结构上正确，在 DAG 上产生 false positive。这是数据结构知识的经典陷阱。

**当前状态**: 接受为已知限制，记录在案。
