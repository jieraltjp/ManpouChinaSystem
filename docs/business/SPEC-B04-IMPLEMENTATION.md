# SPEC-B04-IMPL — 货柜号（containerNo）实现设计

> **版本**: 1.0.0
> **创建**: 2026-04-27
> **状态**: ✅ 设计阶段
> **功能**: LogisticsPlan 新增 `containerNo` 货柜号字段，支持多货物合柜追踪
> **前置**: DB-04 · SPEC-B04-调配计划-步骤4.md

---

## 1. 字段定义

| 字段 | 类型 | 说明 |
|------|------|------|
| `containerNo` | `VARCHAR(32)` | 货柜号，船公司提供，非必填，支持模糊搜索 |

---

## 2. 业务流程

```
多条 LogisticsPlan → 填入相同 containerNo → 视为同一货柜货物
```

**使用场景：**
- 用户在"新增调配计划"弹窗中手动填写货柜号
- 同一货柜内的所有货物（多条 LogisticsPlan）填入相同货柜号
- 列表页按货柜号筛选，可快速查看同柜货物

---

## 3. 后端改动

### 3.1 Entity

```java
@Column(name = "container_no", length = 32)
private String containerNo;  // 货柜号（船公司提供）
```

### 3.2 DTO

**LogisticsPlanCreateCmd**
```java
@Length(max = 32)
private String containerNo;
```

**LogisticsPlanUpdateCmd**
```java
@Length(max = 32)
private String containerNo;
```

**LogisticsPlanPageQuery**（响应 VO）
```java
private String containerNo;
```

**LogisticsPlanQuery**（请求参数）
```java
private String containerNo;  // 支持模糊搜索
```

### 3.3 Repository

```java
Page<LogisticsPlan> findByContainerNoContainingAndDeletedIsFalse(String containerNo, Pageable pageable);
```

### 3.4 Assembler

```java
// toDto
.containerNo(entity.getContainerNo())

// copyCreate
if (cmd.getContainerNo() != null) entity.setContainerNo(cmd.getContainerNo());

// copyUpdate
if (cmd.getContainerNo() != null) entity.setContainerNo(cmd.getContainerNo());
```

### 3.5 UseCase.pageQuery

```java
if (query.getContainerNo() != null && !query.getContainerNo().isBlank()) {
    page = logisticsPlanRepository.findByContainerNoContainingAndDeletedIsFalse(
            query.getContainerNo(), pageRequest);
}
```

---

## 4. 前端改动

### 4.1 列表页表格

新增列：`货柜号`（`containerNo`），min-width=140，show-overflow-tooltip

### 4.2 新增弹窗

字段：货柜号（`el-input`），放在验货记录下方，placeholder 提示"同批次货物填入相同货柜号"

### 4.3 详情抽屉

显示字段：`containerNo`，放在计划编号下方

---

## 5. API 契约

```
GET /api/v1/logistics-plans?page=0&pageSize=20&containerNo=SBKU
```

响应同现有结构，新增 `containerNo` 字段。

```
POST /api/v1/logistics-plans
Body: { ..., "containerNo": "SBKU1234567" }
```

---

## 6. 代码实现清单

| 文件 | 改动 |
|------|------|
| `LogisticsPlan.java` | +containerNo 字段 + @Index |
| `LogisticsPlanCreateCmd.java` | +containerNo |
| `LogisticsPlanUpdateCmd.java` | +containerNo |
| `LogisticsPlanPageQuery.java` | +containerNo |
| `LogisticsPlanQuery.java` | +containerNo |
| `LogisticsPlanAssembler.java` | 映射 containerNo |
| `LogisticsPlanRepository.java` | +findByContainerNoContaining |
| `LogisticsPlanJpaRepository.java` | +findByContainerNoContaining |
| `LogisticsPlanUseCase.java` | pageQuery 支持 containerNo 筛选 |
| `@/api/logistics.ts` | LogisticsPlanVO +containerNo |
| `LogisticsPage.vue` | 表格列 + 弹窗字段 + 抽屉字段 |
| `zh.json / ja.json` | 翻译 |
