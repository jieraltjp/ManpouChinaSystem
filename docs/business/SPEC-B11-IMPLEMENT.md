# SPEC-B11 — 用户中心与权限体系 · 实现设计

> **版本**: 1.1.0
> **创建**: 2026-05-01
> **更新**: 2026-05-07（v1.1.0：Phase 3 前端完成 + 审计修复 Round 1~5）
> **状态**: 🟡 Phase 3 前端完成，Phase 6（注册+审核）待开发
> **前置**: SPEC-B11 v1.1.0 · user-service Flyway V14 已完成 · Vite proxy 已配置
> **关联**: UI-17 · UI-18 · UI-19 · UI-20 · docs/ui/pages/14-user-management.md · docs/ui/pages/15-role-management.md
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

### 3.13 删除角色

```
DELETE /api/v1/roles/{id}
```

### 3.14 分配权限

```
PUT /api/v1/roles/{id}/permissions
{ "permissionIds": [1, 2, 3, 5] }
```

### 3.15 权限树

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
| 分配权限 | PUT | `/api/v1/roles/{id}/permissions` | ✅ |

---

## 5. 实施顺序

### Phase 1 — 数据库 + 登录 ✅ 2026-04-30

1. Flyway V4~V10 迁移（company/department/position/user/role/permission/audit_log）
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

### Phase 3 — 前端页面 ✅ 2026-05-07

1. `api/user.ts`（用户 API 客户端） ✅
2. `api/role.ts`（角色 API 客户端） ✅
3. Vite proxy 补充（`/api/v1/users` → user-service） ✅
4. `pages/system/UserPage.vue`（UI-17） ✅
5. `pages/system/RolePage.vue`（UI-18） ✅
6. 路由注册（`/system/user` · `/system/role`） ✅
7. i18n key 补充（zh.json · ja.json） ✅

### Phase 4 — 操作日志（前端）⚠️ 待开发

1. `api/auditLog.ts`
2. `pages/system/AuditLogPage.vue`（UI-19）
3. 路由注册（`/system/audit-log`）
4. i18n key 补充

### Phase 5 — 个人中心 ⚠️ 待开发

1. `pages/system/ProfilePage.vue`（UI-20）
2. 路由注册（`/profile`）
3. 当前用户信息 API（`/api/v1/users/me`）
4. 修改个人信息 API
5. 修改密码 API
6. i18n key 补充

---

## 6. 数据库 Flyway 现状

| 版本 | 内容 | 状态 |
|------|------|------|
| V4 | company 表 | ✅ |
| V5 | department 表 | ✅ |
| V6 | position 表 | ✅ |
| V7 | user 表（含 BaseEntity 审计列） | ✅ |
| V7_1 | user_position 表（M-N 规范化职务关联） | ✅ |
| V8 | role / permission / user_role / role_permission | ✅ |
| V9 | audit_log 表 | ✅ |
| V10 | admin 用户 seed | ✅ |
| V11 | 补充 BaseEntity 审计列（修复 schema 不一致） | ✅ |
| V12 | admin 用户数据补全 + MANAGER/OPERATOR/VIEWER 角色权限分配 | ✅ |
| V13 | 重复脚本（同 V12，可忽略） | ✅ |
| V14 | 修正预置角色 isEditable=1 | ✅ |

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
| 预置角色允许编辑 | isEditable=1，所有角色均可编辑名称/描述/权限，删除也无限制 |
| 用户分配角色 | UserRoleRepository 维护关联表（deleteByUserId / insertUserRole / findRoleIdsByUserId） |
| 前端权限控制 | Phase 3 实施（前端根据 JWT payload 渲染/隐藏功能按钮） |
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
| GET 接口 @PreAuthorize 鉴权 | P0 | ⚠️ 待开发（所有 GET 端点需加读权限） |
| 前端权限控制（JWT payload 渲染） | P1 | ⚠️ 待开发 |
| 前端删除用户按钮 | P1 | ⚠️ 待开发（UserPage.vue 缺少） |
| 前端筛选器（roleId/departmentId/companyId） | P1 | ⚠️ 待开发（UserPage.vue 缺少） |
| AuditLogPage.vue | P1 | ⚠️ 待开发 |
| ProfilePage.vue | P1 | ⚠️ 待开发 |
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

*文档版本: 1.0.0 · 状态: Phase 2 后端完成 · 2026-05-01*
