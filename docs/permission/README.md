# 权限与日志文档

> **版本**: 1.0.0
> **创建**: 2026-05-12
> **状态**: ✅ 维护中

---

## 文档索引

| 文件 | 内容 |
|------|------|
| [PERMISSION-CODE-ALIGNMENT.md](./PERMISSION-CODE-ALIGNMENT.md) | 权限代码文档对齐审计：V15 DB 67条 vs ALL_PERMISSIONS Set 63条差异分析 |
| [ALL_PERMISSIONS.md](./ALL_PERMISSIONS.md) | ALL_PERMISSIONS Set（63条）与代码文件映射 |
| [ROLE-MATRIX.md](./ROLE-MATRIX.md) | 4级角色权限矩阵（ADMIN/MANAGER/OPERATOR/VIEWER） |
| [AUDIT-LOG-SPEC.md](./AUDIT-LOG-SPEC.md) | 操作日志规格：触发规则/字段/接口/Phase 状态 |

---

## 核心约束

### 原则一：代码即文档

ALL_PERMISSIONS Set 是权限体系的唯一真实来源（Source of Truth），V15 DB seed 是数据来源。
- `allinone`: `infrastructure/security/JwtAuthenticationFilter.java` ALL_PERMISSIONS（63条）
- `user-service`: `infrastructure/security/JwtAuthenticationFilter.java` ALL_PERMISSIONS（63条，两端必须同步）
- V15 DB: `permission` 表 67 条种子数据

### 原则二：DB 优先于代码

`permission` 表是运行时权威数据源：
- 新增权限 = V15 INSERT 一条 + 两端 ALL_PERMISSIONS 加一条
- Phase 4+ warehouse/notification 完整 CRUD = V15 ALTER 添加缺失行 + ALL_PERMISSIONS 补齐
- 禁止从 ALL_PERMISSIONS 反推 DB（DB 已有 114 ID 预留空间）

### 原则三：每次变更同步两端

| 变更类型 | 操作 |
|---------|------|
| 新增权限 | V15 INSERT + allinone ALL_PERMISSIONS + user-service ALL_PERMISSIONS |
| 新增角色 | V15 INSERT role + role_permission |
| 修改角色权限 | V15 UPDATE role_permission |
| ADMIN is_editable | 只能是 0（代码硬编码禁止编辑）|

---

## Phase 状态

| Phase | 内容 | 状态 |
|-------|------|------|
| Phase 1 | DB + 登录 | ✅ |
| Phase 2 | 用户CRUD + 角色管理 | ✅ |
| Phase 3 | 权限控制（@PreAuthorize） | ✅ |
| Phase 4 | 操作日志（前端） | ⚠️ 待开发 |
| Phase 5 | 个人中心 | ⚠️ 待开发 |
| Phase 6 | 注册 + 审核 | ⚠️ 待开发 |

---

## 相关文档

- `docs/business/SPEC-B11-用户中心与权限体系.md` — 业务设计
- `docs/business/SPEC-B11-IMPLEMENT.md` — 实现设计
- `docs/pro/PRODUCTION-DEPLOY.md` — 生产部署（V15/V16）
- `docs/lessons/LESSONS-USER-SERVICE.md` — 工程教训
