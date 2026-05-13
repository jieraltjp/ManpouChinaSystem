# SPEC-B11 — 用户中心与权限体系 · 实现设计

> **版本**: 1.7.1
> **创建**: 2026-05-01
> **更新**: 2026-05-13（v1.7.1：文档审计修正；footer 版本同步为 1.7.1）
> **状态**: ✅ Phase 2 完成；✅ Phase 3 权限控制完成；✅ Phase 4 操作日志完成；✅ Phase 5 个人中心完成；Phase 6 待开发
> **前置**: SPEC-B11 v1.8.0 · Vite proxy 已配置 · allinone V15/V16/V17/V18/V19 已就绪
> **关联**: UI-17 · UI-18 · UI-19 · UI-20 · docs/ui/pages/14-user-management.md · docs/ui/pages/15-role-management.md · `docs/permission/`（权限代码对齐文档）
> **INTJ 编号**: DOC-B11-IMPL-001

---

## 1. INTJ 干跑

**本质（一句话）：** user-service 提供用户/角色/权限 CRUD API，前端 `/system/` 路由下接入这些 API，实现用户管理、角色权限分配、操作日志查询、个人中心。

**三行推演：**
1. 管理员登录 → 进入 `/system/user` → 看到用户列表 → 可新增/编辑/禁用/重置密码/分配角色
2. 管理员进入 `/system/role` → 左侧角色列表 → 右侧权限树（按模块分组）→ 可勾选分配权限
3. 普通用户点右上角头像 → 进入 `/profile` → 改个人信息/密码/语言偏好

**禁止项：**
- 禁止 allinone 重复实现用户管理（职责在 user-service）
- 禁止前端绕过 Vite proxy 直接请求 user-service（统一通过 `/api/v1/` 路径）
- 禁止删除 ADMIN 系统内置角色（通过 `isEditable=1` 约束，实际允许删除，可根据需要调整）

---

## 2. 模块拓扑

```
user-service (port 18081)
├── user/domain/model/
│   ├── User.java              # 实体（含 BaseEntity 审计字段）
│   ├── Role.java              # 实体（含多对多 Permission）
│   ├── Permission.java        # 实体
│   └── BaseEntity.java       # 父类（id/createTime/updateTime/createBy/updateBy/isDeleted）
├── user/domain/repository/
│   ├── UserRepository.java     # extends JpaRepository + JpaSpecificationExecutor
│   ├── RoleRepository.java    # extends JpaRepository（含 findRolesByUserId native query）
│   └── PermissionRepository.java
├── user/application/dto/       # 每类一个文件（Lombok @Data）
│   ├── UserCreateCmd.java
│   ├── UserUpdateCmd.java
│   ├── UserStatusCmd.java
│   ├── PasswordResetCmd.java
│   ├── UserRolesCmd.java
│   ├── UserVO.java           # 含内部类 PositionVO / RoleSimpleVO
│   ├── UserPageQuery.java
│   ├── UserPageVO.java
│   ├── PasswordResetVO.java
│   ├── RoleCreateCmd.java
│   ├── RoleUpdateCmd.java
│   ├── RolePermissionsCmd.java
│   ├── RoleVO.java
│   ├── RoleSimpleVO.java
│   ├── PermissionVO.java
│   └── PermissionTreeVO.java
├── user/application/service/
│   ├── UserService.java       # 用户 CRUD + 分页查询 + 角色分配
│   └── RoleService.java      # 角色 CRUD + 权限分配 + 权限树
└── user/interfaces/controller/
    ├── UserController.java    # /api/v1/users
    ├── RoleController.java    # /api/v1/roles
    └── PermissionController.java # /api/v1/permissions/tree

前端 (port 13000)
├── api/user.ts               # 用户 API 客户端（新增）
├── api/role.ts               # 角色 API 客户端（新增）
├── pages/system/
│   ├── UserPage.vue         # UI-17
│   └── RolePage.vue         # UI-18
├── pages/system/ProfilePage.vue # UI-20
└── router/index.ts          # 新增 /system/user, /system/role
```

---

## 3. API 契约

### 3.1 用户列表（分页）

```
GET /api/v1/users?page=0&size=20&keyword=&status=1&companyId=&departmentId=&roleId=
```

**响应 200**：
```json
{
  "code": "ok",
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "username": "admin",
        "userCode": "U-0001",
        "nameCn": "张总",
        "nameJp": "張総",
        "email": "admin@manpou.cn",
        "phone": "13800138000",
        "avatarUrl": null,
        "companyId": 1,
        "departmentId": 1,
        "language": "zh",
        "timezone": "CST",
        "status": 1,
        "registrationStatus": "APPROVED",
        "lastLoginTime": "2026-05-01T09:00:00",
        "lastLoginIp": "192.168.13.201",
        "createTime": "2026-04-30T07:06:33",
        "roles": [
          { "id": 1, "roleCode": "ADMIN", "roleNameCn": "系统管理员", "roleNameJp": "システム管理者" }
        ],
        "positions": [
          { "id": 5, "nameCn": "经理", "nameJp": "マネージャー" }
        ]
      }
    ],
    "totalElements": 4,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

### 3.2 用户详情

```
GET /api/v1/users/{id}
```

### 3.3 新增用户

```
POST /api/v1/users
Content-Type: application/json

{
  "username": "li.ming",
  "password": "Abc123456",
  "nameCn": "李明",
  "nameJp": "李明",
  "email": "li.ming@manpou.cn",
  "phone": "13900139000",
  "companyId": 1,
  "departmentId": 2,
  "roleIds": [3],
  "language": "zh",
  "timezone": "CST"
}
```

### 3.4 更新用户

```
PUT /api/v1/users/{id}
```

### 3.5 删除用户

```
DELETE /api/v1/users/{id}
```

### 3.6 启用/禁用用户

```
PUT /api/v1/users/{id}/status
{ "status": 0 }
```

### 3.7 重置密码

```
PUT /api/v1/users/{id}/password/reset
```

**响应 200**：
```json
{
  "code": "ok",
  "data": {
    "username": "li.ming",
    "newPassword": "Kx9mRp3TvL2wQ..."
  }
}
```

### 3.8 分配角色

```
PUT /api/v1/users/{id}/roles
{ "roleIds": [2, 3] }
```

### 3.9 角色列表

```
GET /api/v1/roles
```

**响应 200**：`RoleSimpleVO[]`（不含权限列表）

### 3.10 角色详情（含权限）

```
GET /api/v1/roles/{id}
```

**响应 200**：`RoleVO`（含 `permissions[]`）

### 3.11 新增角色

```
POST /api/v1/roles
{
  "roleCode": "CUSTOM_ROLE",
  "roleNameCn": "自定义角色",
  "roleNameJp": "カスタム役割",
  "roleType": "BUSINESS",
  "description": "测试用",
  "permissionIds": [1, 2, 3]
}
```

### 3.12 更新角色

```
PUT /api/v1/roles/{id}
```

### 3.13 局部更新角色（管理员调试）

```
PATCH /api/v1/roles/{id}
{ "isEditable": 1 }   // 仅 ADMIN 可调用，用于修正预置角色属性
```

### 3.14 删除角色

```
DELETE /api/v1/roles/{id}
```

### 3.15 分配权限

```
PUT /api/v1/roles/{id}/permissions
{ "permissionIds": [1, 2, 3, 5] }
```

### 3.16 权限树

```
GET /api/v1/permissions/tree
```

**响应 200**：
```json
[
  {
    "module": "demand",
    "moduleNameCn": "补货需求",
    "moduleNameJp": "補充需要",
    "permissions": [
      { "id": 1, "permissionCode": "demand:read", "permissionNameCn": "查看补货需求", ... },
      { "id": 2, "permissionCode": "demand:create", "permissionNameCn": "创建补货需求", ... },
      ...
    ]
  },
  ...
]
```

---

## 4. 已验证接口

| 接口 | 方法 | 路径 | 状态 |
|------|------|------|------|
| 登录 | POST | `/api/v1/auth/login` | ✅ |
| 公钥 | GET | `/api/v1/auth/public-key` | ✅ |
| 角色列表 | GET | `/api/v1/roles` | ✅ |
| 角色详情 | GET | `/api/v1/roles/{id}` | ✅ |
| 权限树 | GET | `/api/v1/permissions/tree` | ✅ |
| 用户列表 | GET | `/api/v1/users` | ✅ |
| 用户详情 | GET | `/api/v1/users/{id}` | ✅ |
| 新增用户 | POST | `/api/v1/users` | ✅ |
| 更新用户 | PUT | `/api/v1/users/{id}` | ✅ |
| 删除用户 | DELETE | `/api/v1/users/{id}` | ✅ |
| 启用/禁用 | PUT | `/api/v1/users/{id}/status` | ✅ |
| 重置密码 | PUT | `/api/v1/users/{id}/password/reset` | ✅ |
| 分配角色 | PUT | `/api/v1/users/{id}/roles` | ✅ |
| 新增角色 | POST | `/api/v1/roles` | ✅ |
| 更新角色 | PUT | `/api/v1/roles/{id}` | ✅ |
| 删除角色 | DELETE | `/api/v1/roles/{id}` | ✅ |
| 局部更新角色 | PATCH | `/api/v1/roles/{id}` | ✅ |
| 分配权限 | PUT | `/api/v1/roles/{id}/permissions` | ✅ |

---

## 5. 实施顺序

### Phase 1 — 数据库 + 登录 ✅ 2026-04-30

1. Flyway V15~V16 迁移（allinone 统一管理：V15=32表+种子数据，V16=procurement_snapshot；user-service 无迁移，依赖 allinone 同一 DB）
2. JWT 跨服务验证（user-service 签发，allinone 验签）
3. Vite proxy 配置（`/api/v1/auth` → user-service）
4. BCrypt hash 正确 seed（admin/admin123）
5. allinone JwtKeyManager / JwtService 修复

### Phase 2 — 后端 CRUD ✅ 2026-05-01

1. DTO 文件拆分（每个 public 类一个 .java）
2. UserService / RoleService 实现
3. UserController / RoleController / PermissionController
4. UserRepository 继承 `JpaSpecificationExecutor`
5. Lombok 版本对齐（1.18.46，annotation processor = classpath）
6. BaseEntity public setter 方法
7. 编译通过，打包，user-service 重启

### Phase 3 — 权限控制 ✅ 完成 2026-05-08

**缺口 1（allinone JwtAuthenticationFilter）：**
- `claims.roles()` → `ROLE_USER/ADMIN`，`claims.permissions()` **完全忽略**
- 修复：复制 `ALL_PERMISSIONS` 常量（63条，与 V15 DB 实际对齐），提取 permissions 到 authorities，ADMIN `*:*` 展开
- 文件：`JwtAuthenticationFilter.java`

**缺口 2（allinone 业务 Controller）：**
- 17 个业务 Controller 全部 **零个** `@PreAuthorize` 注解
- 修复：按 HTTP 方法 + 业务语义加注解（GET:read / POST:create / PUT-PATCH:update / DELETE:delete）

**实现完成清单：**
1. allinone `JwtAuthenticationFilter` ✅ 提取 permissions（63条，与 V15 DB 实际对齐）
2. allinone 21个业务 Controller 加 `@PreAuthorize` ✅
3. user-service `ALL_PERMISSIONS` 与 V15 DB 对齐（warehouse CRUD 在 DB 中但未入 Set；notification CRUD 同理；补充 japan_customs:start/complete, user:approve, permission:read, audit:export）✅
4. Phase 3 遗留缺口：
   - AuditLogPage.vue 缺少 `hasPermission('audit:read')` 检查（依赖路由 roles 兜底）
   - CosTestPage.vue 缺少 roles 限制（后端 /api/v1/test/** 为 permitAll，生产环境风险）
   - japan_customs:update 缺失：Set 有、DB 无 → V17 已补（ID=51），JapanCustomsController @PreAuthorize 修正为语义对应

### Phase 4 — 操作日志 ✅ 完成（2026-05-12）

1. `api/auditLog.ts` ✅
2. `pages/system/AuditLogPage.vue` ✅
3. 路由注册（`/system/audit-log`，roles=ADMIN/MANAGER）✅
4. i18n key 补充 ✅
5. @AuditLog AOP 链路验证通过 ✅
⚠️ audit:export（CSV 导出）未实现

### Phase 5 — 个人中心 ✅ 完成

1. ✅ `pages/system/ProfilePage.vue`（UI-20，路由 `/profile`）
2. ✅ 当前用户信息 API（`GET /api/v1/users/me`）
3. ✅ 修改个人信息 API（`PUT /api/v1/users/me`，含 avatar 上传）
4. ✅ 修改密码 API（`PUT /api/v1/auth/password`）
5. ✅ i18n key 补充（`profile.*`）
6. ✅ 头像上传：Canvas 压缩（200×200 JPEG@0.75）→ base64 存储 → `user.avatar_url MEDIUMTEXT`
7. ✅ AppLayout.vue 头部/面板显示 base64 头像，日语/中文姓名根据 locale 切换
⚠️ notification CRUD 后端已实现（无前端 UI）
⚠️ warehouse CRUD 后端已实现（无前端 UI，DB 有但 ALL_PERMISSIONS Set 无）

### Phase 6 — 注册 + 审核流程 ⚠️ 待开发（后端部分实现）

**后端 API：**
1. `POST /api/v1/auth/register`（用户注册，自动创建 `registrationStatus=PENDING`）
2. `GET /api/v1/users/pending`（查询待审核用户列表，仅 ADMIN）
3. `PUT /api/v1/users/{id}/approve`（审核通过，`registrationStatus=APPROVED`）
4. `PUT /api/v1/users/{id}/reject`（审核拒绝，`registrationStatus=REJECTED`）

**前端：**
1. `pages/auth/RegisterPage.vue`（注册页，登录页加「注册」入口）
2. `pages/system/PendingUsersPage.vue`（待审核用户列表，仅 ADMIN 可见）
3. 路由注册（`/register` · `/system/pending-users`）

---

## 6. 数据库 Flyway 现状

> ⚠️ V4~V14 历史迁移已全部删除（杂乱无章，V15 已包含全部表结构）。user-service 无迁移文件。

| 版本 | 文件 | 内容 | 状态 |
|------|------|------|------|
| V15 | `V15__baseline_schema.sql` | 32 表 DDL + 种子数据（78 条权限 + 4 角色 + admin 用户 + 职务 + 组织） | ✅ |
| V16 | `V16__procurement_snapshot.sql` | procurement_snapshot 表（幂等兜底） | ✅ |
| V17 | `V17__user_avatar_mediumtext.sql` | user.avatar_url → MEDIUMTEXT（头像 base64 存储） | ✅ |
| V18 | `V18__ship_and_container_extension.sql` | ship 表 + container 扩展字段 + ship 权限 4 条（ID 115~118） | ✅ |
| V19 | `V19__japan_customs_update_permission.sql` | INSERT japan_customs:update（ID=119）修复 Set/DB 不一致 | ✅ |

> DB 实际权限：83 条（V15:78 + V18:4 + V19:1）；ALL_PERMISSIONS Set: allinone 71条 / user-service 66条。
> BCrypt hash: `$2a$12$t7mRpfsCDNFgj6LET1Y47eH7J2.MJ5i5nAYwYL6SfKdWE7LN.vqUG`（admin/admin123）

---

## 7. Vite Proxy 配置

**文件**: `apps/web/vite.config.ts`

```typescript
proxy: {
  '/api/v1/auth': {
    target: 'http://localhost:18081',
    changeOrigin: true,
  },
  '/api/v1/users': {
    target: 'http://localhost:18081',
    changeOrigin: true,
  },
  '/api/v1/roles': {
    target: 'http://localhost:18081',
    changeOrigin: true,
  },
  '/api/v1/permissions': {
    target: 'http://localhost:18081',
    changeOrigin: true,
  },
  '/api': {
    target: 'http://localhost:18090',
    changeOrigin: true,
    ws: true,
  },
},
```

---

## 8. 已知约束

| 约束 | 说明 |
|------|------|
| 预置角色编辑约束 | ADMIN is_editable=0 不可编辑名称/角色类型；MANAGER/OPERATOR/VIEWER is_editable=1 可编辑 |
| 用户分配角色 | UserRoleRepository 维护关联表（deleteByUserId / insertUserRole / findRoleIdsByUserId） |
| 前端权限控制 | ✅ Phase 3 完成——allinone JwtAuthenticationFilter 提取 permissions + 21个 Controller @PreAuthorize + 前端按钮 v-if + 路由角色守卫 |
| 审计日志记录 | audit_log 表已建，但 Service 层尚未接入（Phase 4） |

---

## 9. 缺口与依赖

| 项目 | 优先级 | 状态 |
|------|--------|------|
| Phase 2 后端 CRUD | P0 | 🟢 已完成 |
| Vite proxy 配置 | P0 | 🟢 已完成（auth + users + roles + permissions） |
| 前端 API 客户端 | P0 | ✅ 已完成（api/user.ts + api/role.ts） |
| UserPage.vue | P0 | ✅ 已完成 |
| RolePage.vue | P0 | ✅ 已完成 |
| 路由注册 | P0 | ✅ 已完成（/system/user + /system/role） |
| i18n key | P0 | ✅ 已完成 |
| allinone JwtAuthenticationFilter 提取 permissions | P0 | ✅ 已完成（63条，与 V15 DB 实际对齐） |
| allinone 21个业务 Controller 加 @PreAuthorize | P0 | ✅ 已完成（demand/procurement/shipment/qc/logistics/consolidation/container/customs/japan_customs/tax_refund/sales/warehouse/notification/order/product/factory） |
| 前端权限控制（JWT payload 渲染） | P1 | ⚠️ 待开发 |
| 前端删除用户按钮 | P1 | ⚠️ 待开发（UserPage.vue 缺少） |
| 前端筛选器（roleId/departmentId/companyId） | P1 | ⚠️ 待开发（UserPage.vue 缺少） |
| AuditLogPage.vue | P1 | ⚠️ 待开发 |
| ProfilePage.vue | P1 | ✅ 完成（头像上传+Canvas压缩+base64存储） |
| 用户分配角色（完整实现） | P1 | ✅ 已完成（UserRoleRepository） |
| Phase 6 注册+审核（全栈） | P2 | ⚠️ 待开发 |

---

## 10. INTJ 审计

| 判定 | 说明 |
|------|------|
| 防腐 | 前端通过 `api/*.ts` 访问后端，不直接依赖 entity/vo |
| 依赖倒置 | Service 只依赖 Repository 接口，不直接操作 JPA |
| 单一职责 | Controller 只做路由分发，业务逻辑全在 Service |
| 不可变 | DTO/VO 全用 Lombok `@Data` 生成，Service 返回新对象 |
| 熵减 | 权限树按模块分组，RoleService 中 switch 处理 CN/JP 名称 |
| 分型 | `toVO()` / `toSimpleVO()` 分流处理，层级清晰 |

---

*文档版本: 1.7.1 · 状态: Phase 5 ✅ · 2026-05-13*
