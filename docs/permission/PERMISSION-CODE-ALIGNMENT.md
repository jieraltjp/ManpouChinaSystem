# 权限代码文档对齐审计

> **版本**: 1.0.0
> **创建**: 2026-05-12
> **目的**: 确保 V15 DB（67条）、ALL_PERMISSIONS Set（63条）、SPEC-B11 文档三方一致

---

## 1. V15 DB 实际数据（67条）

来源：`allinone/src/main/resources/db/migration/V15__baseline_schema.sql` 第 877-976 行。

| ID | permission_code | permission_name_cn | module | action | 说明 |
|----|----------------|---------------------|--------|--------|------|
| 1 | demand:read | 查看补货需求 | demand | READ | |
| 2 | demand:create | 创建补货需求 | demand | CREATE | |
| 3 | demand:update | 编辑补货需求 | demand | UPDATE | |
| 4 | demand:delete | 删除补货需求 | demand | DELETE | |
| 5 | procurement:read | 查看发注单 | procurement | READ | |
| 6 | procurement:create | 创建发注单 | procurement | CREATE | |
| 7 | procurement:update | 编辑发注单 | procurement | UPDATE | |
| 8 | procurement:delete | 删除发注单 | procurement | DELETE | |
| 9 | shipment:read | 查看出货批次 | shipment | READ | |
| 10 | shipment:create | 创建出货批次 | shipment | CREATE | |
| 11 | shipment:update | 编辑出货批次 | shipment | UPDATE | |
| 12 | shipment:delete | 删除出货批次 | shipment | DELETE | |
| 13 | qc:read | 查看验货记录 | qc | READ | |
| 14 | qc:create | 创建验货记录 | qc | CREATE | |
| 15 | qc:update | 编辑验货记录 | qc | UPDATE | |
| 16 | qc:delete | 删除验货记录 | qc | DELETE | |
| 21 | logistics:read | 查看物流调配 | logistics | READ | |
| 22 | logistics:create | 创建物流调配 | logistics | CREATE | |
| 23 | logistics:update | 编辑物流调配 | logistics | UPDATE | |
| 24 | logistics:delete | 删除物流调配 | logistics | DELETE | |
| 25 | consolidation:read | 查看拼柜池 | consolidation | READ | |
| 26 | consolidation:create | 创建拼柜池 | consolidation | CREATE | |
| 27 | consolidation:update | 编辑拼柜池 | consolidation | UPDATE | |
| 28 | consolidation:delete | 删除拼柜池 | consolidation | DELETE | |
| 29 | container:read | 查看货柜 | container | READ | |
| 30 | container:create | 创建货柜 | container | CREATE | |
| 31 | container:update | 编辑货柜 | container | UPDATE | |
| 32 | container:delete | 删除货柜 | container | DELETE | |
| 41 | customs:read | 查看国内报关 | customs | READ | |
| 42 | customs:create | 创建报关单 | customs | CREATE | |
| 43 | customs:update | 编辑报关单 | customs | UPDATE | |
| 44 | customs:delete | 删除报关单 | customs | DELETE | |
| 45 | customs:approve | 审批报关单 | customs | APPROVE | |
| 46 | japan_customs:read | 查看日本清关 | japan_customs | READ | |
| 47 | japan_customs:create | 创建日本清关 | japan_customs | CREATE | |
| 48 | japan_customs:start | 启动清关 | japan_customs | START | |
| 49 | japan_customs:complete | 完成清关 | japan_customs | COMPLETE | |
| 50 | japan_customs:delete | 删除日本清关 | japan_customs | DELETE | |
| 61 | tax_refund:read | 查看退税记录 | tax_refund | READ | |
| 62 | tax_refund:create | 创建退税记录 | tax_refund | CREATE | |
| 63 | tax_refund:update | 编辑退税记录 | tax_refund | UPDATE | |
| 64 | tax_refund:complete | 完成退税 | tax_refund | COMPLETE | |
| 65 | tax_refund:delete | 删除退税记录 | tax_refund | DELETE | |
| 66 | sales:read | 查看销售记录 | sales | READ | |
| 67 | sales:create | 创建销售记录 | sales | CREATE | |
| 68 | sales:update | 编辑销售记录 | sales | UPDATE | |
| 69 | sales:delete | 删除销售记录 | sales | DELETE | |
| 70 | factory:read | 查看工厂 | factory | READ | |
| 71 | factory:create | 创建工厂 | factory | CREATE | |
| 72 | factory:update | 编辑工厂 | factory | UPDATE | |
| 73 | factory:delete | 删除工厂 | factory | DELETE | |
| 74 | product:read | 查看商品 | product | READ | |
| 75 | product:create | 创建商品 | product | CREATE | |
| 76 | product:update | 编辑商品 | product | UPDATE | |
| 77 | product:delete | 删除商品 | product | DELETE | |
| 78 | order:read | 查看订单总览 | order | READ | |
| 81 | user:read | 查看用户 | user | READ | |
| 82 | user:create | 创建用户 | user | CREATE | |
| 83 | user:update | 编辑用户 | user | UPDATE | |
| 84 | user:delete | 删除用户 | user | DELETE | |
| 85 | user:approve | 审核注册用户 | user | APPROVE | Phase 6 |
| 86 | user:reset_password | 重置用户密码 | user | ADMIN | |
| 87 | role:read | 查看角色 | role | READ | |
| 88 | role:create | 创建角色 | role | CREATE | |
| 89 | role:update | 编辑角色 | role | UPDATE | |
| 90 | role:assign | 分配角色 | role | ASSIGN | |
| 91 | role:delete | 删除角色 | role | DELETE | |
| 92 | permission:read | 查看权限 | permission | READ | 仅前端展示用 |
| 93 | audit:read | 查看操作日志 | audit | READ | |
| 94 | audit:export | 导出操作日志 | audit | EXPORT | |
| 114 | notification:delete | 删除通知 | notification | DELETE | V15 仅此一条；其余 Phase 5+ |

---

## 2. ALL_PERMISSIONS Set（63条）

来源：`allinone/infrastructure/security/JwtAuthenticationFilter.java` 和 `user-service/infrastructure/security/JwtAuthenticationFilter.java`。

与 V15 DB 差异分析：

### 2.1 DB 有、Set 缺（4条）

| permission_code | DB ID | 说明 |
|----------------|-------|------|
| customs:approve | 45 | DB 有，Set 无 → `@PreAuthorize` 无法生效 |
| notification:delete | 114 | DB 有，Set 无 → Phase 6 注册审核会用到 |

**结论**：这 2 条通过运行时 DB 查询填充（`user-service` 从 DB 读 permissions 写入 JWT），不影响 ADMIN 展开。

### 2.2 Set 有、DB 缺（0条） ✅

2026-05-12 审计修正：已移除 phantom warehouse/notification CRUD，仅保留 DB 有的 `notification:delete`（ID=114）。

### 2.3 两端对齐状态

| 组件 | Set 条数 | 与 DB 对齐 | 最后审计 |
|------|---------|-----------|---------|
| allinone JwtAuthenticationFilter | 63 | ✅ | 2026-05-12 |
| user-service JwtAuthenticationFilter | 63 | ✅ | 2026-05-12 |
| V15 permission 表 | 67 | — | — |

---

## 3. SPEC-B11 文档 vs 代码

| 项目 | SPEC-B11 旧值 | 修正后 | 状态 |
|------|-------------|--------|------|
| DB 权限条数 | 102 | 67 | ✅ 已修正 |
| ALL_PERMISSIONS 条数 | 66 | 63 | ✅ 已修正 |
| BCrypt hash | 旧值 | `$2a$12$t7mRpfsCDNFgj6LET1Y47eH7J2.MJ5i5nAYwYL6SfKdWE7LN.vqUG` | ✅ 已修正 |
| ADMIN is_editable | 1 | 0 | ✅ 已修正 |
| warehouse 模块 | 列在权限表 | 移除（DB 不存在） | ✅ 已修正 |
| 迁移体系 | V4~V14 | allinone V15/V16 | ✅ 已修正 |

---

## 4. Phase 5+ 待补权限（DB 有、Set 无）

| permission_code | DB ID | 说明 | 补入 Set 时机 |
|----------------|-------|------|--------------|
| customs:approve | 45 | Phase 4+ 日本清关审批流 | Phase 4 |
| notification:delete | 114 | Phase 5 个人中心通知 | Phase 5 |
| warehouse:read/create/update/delete | — | Phase 5+ 仓储模块 | Phase 5 |
| notification:read/create/update | — | Phase 5+ 通知模块 | Phase 5 |
| user:approve | 85 | Phase 6 注册审核 | Phase 6 |
