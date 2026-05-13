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
| customs:approve | ● | ● | ○ | ○ |
| japan_customs:read | ● | ● | ● | ● |
| japan_customs:create | ● | ● | ● | ○ |
| japan_customs:start | ● | ● | ○ | ○ |
| japan_customs:complete | ● | ● | ○ | ○ |
| japan_customs:delete | ● | ● | ● | ○ |
| tax_refund:read | ● | ● | ● | ● |
| tax_refund:create | ● | ● | ● | ○ |
| tax_refund:update | ● | ● | ● | ○ |
| tax_refund:complete | ● | ● | ○ | ○ |
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
| order:read | ● | ● | ● | ● |
| warehouse:read | ● | ● | ○ | ○ |
| warehouse:create | ● | ● | ○ | ○ |
| warehouse:update | ● | ● | ○ | ○ |
| warehouse:delete | ● | ● | ○ | ○ |
| notification:read | ● | ● | ○ | ○ |
| notification:create | ● | ● | ○ | ○ |
| notification:update | ● | ● | ○ | ○ |
| notification:delete | ● | ● | ○ | ○ |

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
-- ADMIN (role_id=1): 全部权限（78条，含 warehouse/notification）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE is_deleted = 0;

-- MANAGER (role_id=2): 全部业务权限（仅排除系统管理写操作）
-- 不排除：customs:approve, japan_customs:complete, tax_refund:complete
-- 包含：warehouse CRUD（ID 101-104），notification CRUD（ID 111-114）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 2, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.permission_code NOT IN (
        'user:delete', 'user:approve', 'user:reset_password',
        'role:create', 'role:update', 'role:assign', 'role:delete',
        'permission:read'
  );

-- OPERATOR (role_id=3): 核心业务 CRUD + order:read
-- 不含：warehouse, notification, user, role, permission, audit
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 3, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.module IN ('demand','procurement','shipment','qc','logistics',
                   'consolidation','container','customs','japan_customs',
                   'tax_refund','sales','factory','product','order')
  AND p.action_ IN ('READ', 'CREATE', 'UPDATE', 'DELETE');

-- VIEWER (role_id=4): 核心业务只读
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT 4, p.id FROM permission p
WHERE p.is_deleted = 0
  AND p.module IN ('demand','procurement','shipment','qc','logistics',
                   'consolidation','container','customs','japan_customs',
                   'tax_refund','sales','factory','product','order')
  AND p.action_ = 'READ';
```

---

## 特殊说明

### MANAGER 实际权限（与 SPEC-B11 §5 矩阵差异说明）

SPEC-B11 v1.9.0 前的矩阵存在错误。MANAGER 排除列表仅含 8 个系统管理写操作权限，
**不排除** customs:approve、japan_customs:complete、tax_refund:complete，
且包含 warehouse CRUD 和 notification CRUD（均为 V15 DB 实际存在）。已修正本矩阵。

### order:read 权限

OPERATOR 拥有 `order:read`（order 模块无 CRUD，仅有 READ），
使得普通运营可以查看订单总览（`/base/overview`）。

### customs:approve / japan_customs:complete / tax_refund:complete

- `customs:approve`：国内报关审批（**ADMIN + MANAGER**）
- `japan_customs:complete`：日本清关完成（**ADMIN + MANAGER**）
- `tax_refund:complete`：完成退税（**ADMIN + MANAGER**）
