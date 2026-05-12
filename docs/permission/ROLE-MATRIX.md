# 角色权限矩阵

> **版本**: 1.0.0
> **创建**: 2026-05-12
> **数据来源**: `allinone/src/main/resources/db/migration/V15__baseline_schema.sql` 第 993-1024 行（role_permission INSERT）

---

## 角色概览

| 角色 | role_code | role_type | is_editable | 描述 |
|------|-----------|-----------|-------------|------|
| 系统管理员 | ADMIN | SYSTEM | **0**（不可编辑） | 全部权限，含系统管理 |
| 运营主管 | MANAGER | BUSINESS | 1（可编辑） | 业务全部权限，不含系统写操作 |
| 普通运营 | OPERATOR | BUSINESS | 1（可编辑） | 业务 CRUD，订单只读 |
| 查看者 | VIEWER | BUSINESS | 1（可编辑） | 全部业务模块只读 |

---

## 权限矩阵

**符号说明**：
- `●` = 拥有该权限
- `○` = 不拥有该权限
- `★` = 特殊说明

### 业务模块

| 权限 | ADMIN | MANAGER | OPERATOR | VIEWER |
|------|:-----:|:-------:|:--------:|:------:|
| demand:read | ● | ● | ● | ● |
| demand:create | ● | ● | ● | ○ |
| demand:update | ● | ● | ● | ○ |
| demand:delete | ● | ● | ● | ○ |
| procurement:read | ● | ● | ● | ● |
| procurement:create | ● | ● | ● | ○ |
| procurement:update | ● | ● | ● | ○ |
| procurement:delete | ● | ● | ● | ○ |
| shipment:read | ● | ● | ● | ● |
| shipment:create | ● | ● | ● | ○ |
| shipment:update | ● | ● | ● | ○ |
| shipment:delete | ● | ● | ● | ○ |
| qc:read | ● | ● | ● | ● |
| qc:create | ● | ● | ● | ○ |
| qc:update | ● | ● | ● | ○ |
| qc:delete | ● | ● | ● | ○ |
| logistics:read | ● | ● | ● | ● |
| logistics:create | ● | ● | ● | ○ |
| logistics:update | ● | ● | ● | ○ |
| logistics:delete | ● | ● | ● | ○ |
| consolidation:read | ● | ● | ● | ● |
| consolidation:create | ● | ● | ● | ○ |
| consolidation:update | ● | ● | ● | ○ |
| consolidation:delete | ● | ● | ● | ○ |
| container:read | ● | ● | ● | ● |
| container:create | ● | ● | ● | ○ |
| container:update | ● | ● | ● | ○ |
| container:delete | ● | ● | ● | ○ |
| customs:read | ● | ● | ● | ● |
| customs:create | ● | ● | ● | ○ |
| customs:update | ● | ● | ● | ○ |
| customs:delete | ● | ● | ● | ○ |
| customs:approve | ● | ○ | ○ | ○ |
| japan_customs:read | ● | ● | ● | ● |
| japan_customs:create | ● | ● | ● | ○ |
| japan_customs:start | ● | ● | ● | ○ |
| japan_customs:complete | ● | ○ | ○ | ○ |
| japan_customs:delete | ● | ● | ● | ○ |
| tax_refund:read | ● | ● | ● | ● |
| tax_refund:create | ● | ● | ● | ○ |
| tax_refund:update | ● | ● | ● | ○ |
| tax_refund:complete | ● | ○ | ○ | ○ |
| tax_refund:delete | ● | ● | ● | ○ |
| sales:read | ● | ● | ● | ● |
| sales:create | ● | ● | ● | ○ |
| sales:update | ● | ● | ● | ○ |
| sales:delete | ● | ● | ● | ○ |
| factory:read | ● | ● | ● | ● |
| factory:create | ● | ● | ● | ○ |
| factory:update | ● | ● | ● | ○ |
| factory:delete | ● | ● | ● | ○ |
| product:read | ● | ● | ● | ● |
| product:create | ● | ● | ● | ○ |
| product:update | ● | ● | ● | ○ |
| product:delete | ● | ● | ● | ○ |
| order:read | ● | ● | ★ | ● |
| notification:delete | ● | ● | ● | ○ |

### 系统管理模块

| 权限 | ADMIN | MANAGER | OPERATOR | VIEWER |
|------|:-----:|:-------:|:--------:|:------:|
| user:read | ● | ● | ○ | ○ |
| user:create | ● | ○ | ○ | ○ |
| user:update | ● | ○ | ○ | ○ |
| user:delete | ● | ○ | ○ | ○ |
| user:approve | ● | ○ | ○ | ○ |
| user:reset_password | ● | ○ | ○ | ○ |
| role:read | ● | ● | ○ | ○ |
| role:create | ● | ○ | ○ | ○ |
| role:update | ● | ○ | ○ | ○ |
| role:assign | ● | ○ | ○ | ○ |
| role:delete | ● | ○ | ○ | ○ |
| permission:read | ● | ○ | ○ | ○ |
| audit:read | ● | ● | ○ | ○ |
| audit:export | ● | ● | ○ | ○ |

---

## V15 SQL 实现

```sql
-- ADMIN (role_id=1): 全部权限
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE is_deleted = 0;

-- MANAGER (role_id=2): 全部业务权限（不含系统管理写操作）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 2, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.permission_code NOT IN (
        'user:delete', 'user:approve', 'user:reset_password',
        'role:create', 'role:update', 'role:assign', 'role:delete',
        'permission:read'
  );

-- OPERATOR (role_id=3): 业务 CRUD + order:read
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 3, p.id FROM permission p
WHERE p.is_deleted = 0
  AND (
    p.module IN ('demand','procurement','shipment','qc','logistics',
                 'consolidation','container','customs','japan_customs',
                 'tax_refund','sales','factory','product','notification')
    OR p.permission_code IN ('order:read')
  );

-- VIEWER (role_id=4): 业务只读
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 4, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.action_ = 'READ';
```

---

## 特殊说明

### ADMIN 角色约束

- `is_editable = 0`：数据库层面标记，RoleService 应在更新/删除时检查
- 代码硬编码：禁止通过角色管理 UI 编辑 ADMIN 的名称/描述/权限

### order:read 权限

OPERATOR 拥有 `order:read`，但 `order` 模块无 CRUD，仅有 READ。
这使得普通运营可以查看订单总览（`/base/overview`）。

### customs:approve vs japan_customs:complete

- `customs:approve`：国内报关审批（ADMIN 独有）
- `japan_customs:complete`：日本清关完成（ADMIN + MANAGER）
