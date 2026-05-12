# Lesson 78: 权限三角审计（2026-05-08）

## 审计结果

### 前后端链路 ✅ 一致

| 前端调用 | 后端 @PreAuthorize | DB 权限 | 状态 |
|----------|-------------------|---------|------|
| `demand:create/update/delete` | ✅ | ✅ | 一致 |
| `procurement:create/update/delete` | ✅ | ✅ | 一致 |
| `shipment:create/update/delete` | ✅ | ✅ | 一致 |
| `qc:create/update/delete` | ✅ | ✅ | 一致 |
| `logistics:create/update/delete` | ✅ | ✅ | 一致 |
| `consolidation:create/update/delete` | ✅ | ✅ | 一致 |
| `container:create/update/delete` | ✅ | ✅ | 一致 |
| `customs:create/update/delete` | ✅ | ✅ | 一致 |
| `japan_customs:create/update/delete` | ✅ | ✅ | 一致 |
| `tax_refund:create/update/delete` | ✅ | ✅ | 一致 |
| `sales:create/update/delete` | ✅ | ✅ | 一致 |
| `factory:create/update/delete` | ✅ | ✅ | 一致 |
| `product:create/update/delete` | ✅ | ✅ | 一致 |
| `user:create/update/reset_password` | ✅ | ✅ | 一致 |
| `role:create/update/delete/assign` | ✅ | ✅ | 一致 |

**前端共 42 个权限，全部与后端 + DB 一致。**

### DB 多余的权限（无 @PreAuthorize 保护）

| 权限 | 说明 | 建议 |
|------|------|------|
| `customs:approve` | DB 有，后端无 @PreAuthorize | Phase 5 报关审批流程时启用 |
| `japan_customs:start` | DB 有，后端无 @PreAuthorize | Phase 5 清关状态机时启用 |
| `japan_customs:complete` | DB 有，后端无 @PreAuthorize | Phase 5 |
| `tax_refund:complete` | DB 有，后端无 @PreAuthorize | Phase 5 退税流程时启用 |
| `user:approve` | DB 有，后端无 @PreAuthorize | Phase 5 用户审核时启用 |
| `permission:read` | DB 有，后端无 @PreAuthorize | Phase 5 权限树时启用 |
| `audit:read` | DB 有，后端无 @PreAuthorize | Phase 4 操作日志时启用 |
| `audit:export` | DB 有，后端无 @PreAuthorize | Phase 4 |
| `order:read` | DB 有，后端无 @PreAuthorize | 已废弃（用 base/overview） |
| `warehouse:*` (4条) | DB+后端有，无前端页面 | 仓库管理未实现 |
| `notification:*` (4条) | DB+后端有，无前端页面 | 通知管理未实现 |

**结论**: admin 有 `*:*`，这些孤岛权限不影响安全。

### 前端不检查 `:read` 权限

前端所有页面**不调用** `hasPermission('xxx:read')`，只对 CUD 操作做权限检查。

- 原因：路由守卫按角色判断，前端不需要细粒度读权限
- 风险：无 read 权限的用户仍能进入页面查看数据
- 决策：如需细粒度读权限控制，在路由守卫加 `hasPermission('xxx:read')`

### 审计命令

```bash
# 前端用的权限
grep -r "hasPermission('" apps/web/src --include="*.vue" --include="*.ts" | grep -oP "hasPermission\('\K[^']+" | sort -u

# 后端定义的权限
grep -r "@PreAuthorize.*hasAuthority" apps/manpou-allinone/src --include="*.java" apps/user-service/src --include="*.java" | grep -oP "hasAuthority\('\K[^')]+" | sort -u

# 对比
comm -23 <(前端权限) <(后端权限)   # 前端有但后端没有
comm -13 <(前端权限) <(后端权限)   # 后端有但前端没有
```
