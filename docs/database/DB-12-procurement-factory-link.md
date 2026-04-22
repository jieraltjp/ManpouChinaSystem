# DB-12 — 发注单与工厂关联

> **版本**: 1.0.0
> **创建**: 2026-04-22
> **状态**: ✅ 已实现
> **对应 UI**: `docs/ui/pages/02-procurement.md` §6 组件结构
> **对应实体**: `com.manpou.allinone.procurement.domain.model.Procurement`
> **关联设计文档**: `DB-10-factory.md`

---

## §1 现状分析

### 1.1 已具备的基础

| 层级 | 现状 |
|------|------|
| `Procurement` 实体 | 已有 `factoryId` 字段和 `idx_procurement_factory_id` 索引 |
| `ProcurementPageQuery` DTO | 已有 `factoryId`（Long）字段 |
| `ProcurementCreateCmd` | 已有 `factoryId`（Long）字段 |
| UI 新建弹窗 | 已有"选择工厂下拉" |
| UI 详情抽屉 | 已有"关联工厂"字段 |

### 1.2 缺失的部分

| 层级 | 问题 |
|------|------|
| 数据库 | `procurement` 表缺少 `factory_id` 列（Hibernate ddl-auto 未同步） |
| `ProcurementPageQuery` | 缺少 `factoryName`，列表/详情无法展示工厂名称 |
| `ProcurementAssembler.toDto` | 未查询关联工厂，factoryName 为空 |
| `ProcurementUseCase.create` | 未校验 `factoryId` 对应的工厂是否存在 |
| `ProcurementUseCase.update` | 未校验 `factoryId` 是否可修改 |
| 查询 | 不支持按工厂筛选 |

---

## §2 业务规则

### 2.1 工厂关联规则

| 场景 | 规则 |
|------|------|
| 新建发注单 | `factoryId` 为**必填**，且工厂必须存在且未被逻辑删除 |
| 编辑发注单 | `factoryId` **不允许修改**（已下单的发注不能更换工厂） |
| 删除工厂 | 系统校验：如有**未终态**（非 `完了`/`退货`）发注单关联该工厂，拒绝删除 |
| 终态发注 | `完了` / `退货` 状态的发注单，工厂信息**只读** |

### 2.2 工厂 FK 约束

```
factory (id) ←─────── (factory_id) procurement
```

- `procurement.factory_id` → `factory.id`
- 不设外键约束（MySQL 外键对历史数据迁移不友好），由应用层校验

---

## §3 数据库变更

### 3.1 `procurement` 表新增列

```sql
-- 新增 factory_id 列（可选：ALTER 前先检查是否存在）
ALTER TABLE procurement
    ADD COLUMN factory_id BIGINT DEFAULT NULL COMMENT '关联工厂ID → factory.id';

-- 新增筛选索引（支持按工厂查询）
ALTER TABLE procurement
    ADD INDEX idx_procurement_factory_id (factory_id);
```

> `idx_procurement_factory_id` 索引已在 `Procurement` 实体中声明，但 DB 尚未创建。

### 3.2 V5 Flyway 迁移脚本

```sql
-- ============================================================
-- V5__procurement_factory_link.sql
-- procurement 表新增 factory_id 关联
-- 对应: DB-11 §3
-- ============================================================

ALTER TABLE procurement
    ADD COLUMN factory_id BIGINT DEFAULT NULL COMMENT '关联工厂ID → factory.id';

ALTER TABLE procurement
    ADD INDEX idx_procurement_factory_id (factory_id);
```

---

## §4 Java 变更

### 4.1 `ProcurementPageQuery` 新增字段

```java
// 新增：关联工厂名称（只读，来自 factory 表 JOIN）
private String factoryName;   // 工厂名称
```

### 4.2 `ProcurementAssembler.toDto` 改造

**方案**：通过 `FactoryRepository` 根据 `factoryId` 查询工厂名称。

```java
public ProcurementPageQuery toDto(Procurement entity) {
    String factoryName = null;
    if (entity.getFactoryId() != null) {
        factoryName = factoryRepository.findById(entity.getFactoryId())
                .filter(f -> !Boolean.TRUE.equals(f.getIsDeleted()))
                .map(Factory::getFactoryName)
                .orElse(null);
    }
    return ProcurementPageQuery.builder()
            // ... 其他字段 ...
            .factoryId(entity.getFactoryId())
            .factoryName(factoryName)
            .build();
}
```

**注入依赖**：
```java
@Component
public class ProcurementAssembler {
    private final FactoryRepository factoryRepository;
}
```

### 4.3 `ProcurementUseCase` 工厂校验

**创建时校验**：
```java
@Transactional
public Long create(ProcurementCreateCmd cmd) {
    // 校验工厂存在
    if (cmd.getFactoryId() == null) {
        throw BusinessException.invalidParam("关联工厂不能为空");
    }
    factoryRepository.findByIdAndIsDeletedFalse(cmd.getFactoryId())
            .orElseThrow(() -> BusinessException.invalidParam("关联工厂不存在或已删除"));

    Procurement entity = assembler.toEntity(cmd);
    entity.calculateEstimatedPriceJpy();
    Procurement saved = procurementRepository.save(entity);
    return saved.getId();
}
```

**更新时禁止修改工厂**：
```java
@Transactional
public void update(Long id, ProcurementUpdateCmd cmd) {
    Procurement entity = procurementRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> BusinessException.notFound("Procurement", id));

    // factoryId 不允许修改
    if (cmd.getFactoryId() != null && !cmd.getFactoryId().equals(entity.getFactoryId())) {
        throw BusinessException.invalidParam("发注单关联工厂不允许修改");
    }

    assembler.copyToEntity(cmd, entity);
    // ...
}
```

### 4.4 删除工厂时的关联校验

在 `FactoryUseCase.delete(Long id)` 中新增：

```java
@Transactional
public void delete(Long id) {
    // 校验无未终态发注单关联
    if (procurementRepository.existsActiveByFactoryId(id)) {
        throw BusinessException.invalidParam(
            "该工厂存在未终态发注单，无法删除");
    }
    // ...
}
```

`ProcurementRepository` 新增方法：
```java
@Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Procurement p
        WHERE p.factoryId = :factoryId
          AND p.isDeleted = false
          AND p.status NOT IN ('完了', '退货')")
boolean existsActiveByFactoryId(@Param("factoryId") Long factoryId);
```

### 4.5 按工厂筛选（可选增强）

`ProcurementQuery` 新增：
```java
private Long factoryId;  // 按工厂筛选
```

`ProcurementRepository` 新增：
```java
Page<Procurement> findByFactoryIdAndIsDeletedFalse(Long factoryId, Pageable pageable);
```

`ProcurementUseCase.pageQuery` 新增分支：
```java
if (query.getFactoryId() != null) {
    page = procurementRepository.findByFactoryIdAndIsDeletedFalse(
            query.getFactoryId(), pageRequest);
}
```

---

## §5 API 变更

| 接口 | 变更 | 说明 |
|------|------|------|
| GET `/api/v1/procurements` | 响应新增 `factoryName` | 来自 factory 表 JOIN |
| GET `/api/v1/procurements/{id}` | 响应新增 `factoryName` | 来自 factory 表 JOIN |
| POST `/api/v1/procurements` | 请求校验 `factoryId` 必填且存在 | 400 异常 |
| PATCH `/api/v1/procurements/{id}` | 禁止修改 `factoryId` | 400 异常 |
| DELETE `/api/v1/factories/{id}` | 校验无未终态发注单关联 | 400 异常 |

---

## §6 实施顺序

| 步骤 | 操作 | 风险 |
|------|------|------|
| 1 | 执行 V5 迁移（ALTER TABLE） | 低：仅新增列 |
| 2 | 更新 `ProcurementAssembler`（注入 FactoryRepository） | 低：新增依赖 |
| 3 | 更新 `ProcurementUseCase.create`（校验 factoryId） | 中：现有数据 factory_id 全为 NULL，需确认兼容 |
| 4 | 更新 `ProcurementAssembler.toDto`（填充 factoryName） | 低 |
| 5 | 更新 `ProcurementUseCase.update`（禁止修改 factoryId） | 低 |
| 6 | 更新 `FactoryUseCase.delete`（关联校验） | 低 |
| 7 | 编译 + 测试 | — |

### 6.1 存量数据兼容性

现有 `procurement` 表中所有记录的 `factory_id` 均为 NULL（历史数据未关联工厂）。设计兼容：
- `factoryId` 为 `null` 时视为"未关联工厂"，前端下拉可留空
- `ProcurementAssembler.toDto` 对 `factoryId = null` 返回 `factoryName = null`
- 删除工厂时 `factoryId = null` 的发注单不触发关联校验

---

## §7 字段映射

| ProcurementPageQuery | 说明 |
|---------------------|------|
| `factoryId` (Long) | 工厂ID，已存在 |
| `factoryName` (String) | **新增**，来自 factory 表 JOIN |

| ProcurementCreateCmd | 说明 |
|---------------------|------|
| `factoryId` (Long) | **必填**，工厂ID |
