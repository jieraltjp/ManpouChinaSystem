# 权限与日志文档

> **版本**: 1.0.0
> **创建**: 2026-05-12
> **状态**: ✅ 维护中

---

## 文档索引

| 文件 | 内容 |
|------|------|
| [PERMISSION-CODE-ALIGNMENT.md](./PERMISSION-CODE-ALIGNMENT.md) | 权限代码文档对齐审计：DB 83条 vs ALL_PERMISSIONS Set 差异分析 |
| [ALL_PERMISSIONS.md](./ALL_PERMISSIONS.md) | ALL_PERMISSIONS Set 与代码文件映射 |
| [ROLE-MATRIX.md](./ROLE-MATRIX.md) | 4级角色权限矩阵（ADMIN/MANAGER/OPERATOR/VIEWER） |
| [AUDIT-LOG-SPEC.md](./AUDIT-LOG-SPEC.md) | 操作日志规格：触发规则/字段/接口/Phase 状态（v1.2.1） |
| [AUDIT-LOG-COVERAGE.md](./AUDIT-LOG-COVERAGE.md) | 审计覆盖矩阵：全部 Controller × 页面映射 + 缺口分析 |

---

## 核心约束

### 原则一：代码即文档

ALL_PERMISSIONS Set 是权限体系的唯一真实来源（Source of Truth），V15 DB seed 是数据来源。
- `allinone`: `infrastructure/security/JwtAuthenticationFilter.java` ALL_PERMISSIONS
- `user-service`: `infrastructure/security/JwtAuthenticationFilter.java` ALL_PERMISSIONS
- DB: `permission` 表（V15 基线 + V56 权限补全）

### 原则二：DB 优先于代码

`permission` 表是运行时权威数据源：
- 新增权限 = Flyway INSERT 一条 + 两端 ALL_PERMISSIONS 加一条
- 禁止从 ALL_PERMISSIONS 反推 DB

### 原则三：每次变更同步两端

| 变更类型 | 操作 |
|---------|------|
| 新增权限 | Flyway INSERT + allinone ALL_PERMISSIONS + user-service ALL_PERMISSIONS |
| 新增角色 | Flyway INSERT role + role_permission |
| ADMIN is_editable | 只能是 0（代码硬编码禁止编辑）|

---

## Phase 状态

| Phase | 内容 | 状态 | 说明 |
|-------|------|:----:|------|
| Phase 1 | DB + 登录 | ✅ | |
| Phase 2 | 用户CRUD + 角色管理 | ✅ | |
| Phase 3 | 权限控制（@PreAuthorize） | ✅ | 22 Controller + 前端按钮守卫 |
| Phase 4 | 操作日志（审计扩展） | ✅ | Phase 3 扩展：logout端点 + Role/UserController @AuditLog（v1.2.0） |
| Phase 5 | 个人中心（头像上传+Canvas压缩） | ✅ | SPEC-B11 Phase 5 完成 |
| Phase 6 | 注册 + 审核 | ⚠️ | |

---

## 相关文档

- `docs/business/SPEC-B11-用户中心与权限体系.md` — 业务设计
- `docs/business/SPEC-B11-IMPLEMENT.md` — 实现设计
- `docs/business/SPEC-B17-日志与错误追踪系统.md` — 日志系统设计（Phase 1/2/3 ✅）
- `docs/pro/PRODUCTION-DEPLOY.md` — 生产部署
- `docs/lessons/LESSONS-USER-SERVICE.md` — 工程教训
