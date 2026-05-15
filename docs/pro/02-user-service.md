# 项目文档：user-service（用户服务）

> **文档角色**：后端开发工程师视角 → 认证与权限
> **对应角色文档**：`docs/role/04-后端开发工程师视角分析.md`

---

## 1. 服务定位

| 维度 | 说明 |
|------|------|
| 服务名 | user-service |
| 端口 | 18081 |
| 包名 | `com.manpou.user` |
| 描述 | 用户认证（JWT RS256）+ 权限管理 |
| 当前状态 | Phase 2-5 已完成（用户CRUD + 角色权限管理 + 操作日志 + 个人中心）；Phase 6 ⚠️ 后端已实现，前端待开发 |

---

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 3.2.5 | 应用框架 |
| Spring Security | 认证/鉴权 |
| Spring Data JPA | ORM |
| JJWT 0.12.5 | RS256 JWT 签名/验证 |
| H2（开发）/ MySQL 8（生产） | 数据库 |
| Flyway | 数据库迁移（生产禁用，使用 JPA `ddl-auto: none`） |

---

## 3. 项目结构

```
src/main/java/com/manpou/user/
├── UserServiceApplication.java          # 启动类
├── interfaces/
│   └── controller/
│       ├── AuthController.java          # 登录/公钥
│       ├── UserController.java         # 用户 CRUD
│       ├── RoleController.java         # 角色管理
│       ├── PermissionController.java   # 权限树
│       ├── AuditLogController.java     # 操作日志接收
│       └── AdminController.java        # 管理员（DB 迁移）
├── application/
│   ├── dto/
│   │   ├── User*.java                 # 用户相关 DTO（Create/Update/PageQuery/PageVO/VO）
│   │   ├── Role*.java                 # 角色相关 DTO（Create/Update/Patch/Permissions/VO）
│   │   ├── Permission*.java           # 权限 DTO（VO/TreeVO）
│   │   ├── AuditLog*.java             # 审计日志 DTO（Cmd/Query/VO/PageVO）
│   │   └── PasswordReset*.java        # 密码重置 DTO
│   ├── service/
│   │   ├── UserService.java          # 用户业务逻辑
│   │   ├── RoleService.java          # 角色业务逻辑
│   │   └── AuditLogService.java      # 审计日志服务
│   └── KeyManagementService.java      # 密钥轮换服务
├── domain/
│   ├── model/
│   │   ├── BaseEntity.java            # 审计基类
│   │   ├── User.java                  # 用户实体
│   │   ├── Role.java                 # 角色实体
│   │   ├── Permission.java            # 权限实体
│   │   ├── AuditLog.java             # 操作日志实体
│   │   ├── SigningKey.java           # JWT 签名密钥实体
│   │   └── SigningKeyStatus.java     # 密钥状态枚举
│   └── repository/
│       ├── UserRepository.java
│       ├── RoleRepository.java
│       ├── PermissionRepository.java
│       ├── AuditLogRepository.java
│       └── SigningKeyRepository.java
├── infrastructure/
│   ├── config/
│   │   ├── ClockConfig.java              # 统一时钟
│   │   └── JpaAuditConfig.java           # JPA 审计
│   ├── aspect/
│   │   └── IdempotencyAspect.java        # 幂等切面
│   └── security/
│       ├── JwtAuthenticationFilter.java  # JWT 过滤器
│       ├── JwtContextHolder.java         # 用户上下文
│       ├── JwtKeyManager.java            # 密钥加载/管理
│       ├── JwtService.java                # JWT 核心服务
│       └── SecurityConfig.java            # Spring Security 配置
└── common/
    ├── annotation/
    │   └── Idempotent.java                # 幂等注解
    ├── config/
    │   ├── ConfigListener.java
    │   ├── ConfigSource.java
    │   ├── ConfigSourceAutoConfiguration.java
    │   ├── ConfigSourceFactory.java
    │   ├── LocalFileConfigSource.java
    │   ├── NacosConfigSource.java
    │   └── PropertiesConfigSource.java
    ├── context/
    │   └── UserContext.java
    ├── exception/
    │   ├── BusinessException.java
    │   ├── GlobalExceptionHandler.java
    │   └── ValidationErrorCodeMapper.java
    ├── filter/
    │   └── TraceFilter.java               # 链路追踪过滤器
    ├── result/
    │   └── Result.java                    # 统一响应
    └── time/
        └── Clock.java

src/main/resources/
├── application.yml                    # 主配置（18081 端口）
├── config/local.yaml                  # 本地配置
├── db/migration/                     # 已删除（user-service 无独立迁移，依赖 allinone V15 同一 DB）
└── keys/
    ├── private.pem                   # JWT 私钥（RS256）
    └── public.pem                    # JWT 公钥
```

---

## 4. API 路由

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/v1/auth/public-key` | ❌ | 获取 RSA 公钥 |
| POST | `/api/v1/auth/login` | ❌ | 用户登录 |
| GET | `/api/v1/admin/keys` | ADMIN | 获取所有密钥（仅管理员） |
| POST | `/api/v1/admin/keys/rotate` | ADMIN | 轮换签名密钥 |
| POST | `/api/v1/admin/db/migrate-avatar` | ADMIN | 修复 user.avatar 列类型（一次性迁移） |
| GET | `/api/v1/users` | user:read | 分页查询用户列表 |
| GET | `/api/v1/users/{id}` | user:read | 获取用户详情 |
| GET | `/api/v1/users/me` | user:read | 获取当前用户信息 |
| POST | `/api/v1/users` | user:create | 新建用户 |
| PUT | `/api/v1/users/{id}` | user:update | 编辑用户 |
| DELETE | `/api/v1/users/{id}` | user:delete | 软删除用户 |
| GET | `/api/v1/roles` | role:read | 分页查询角色列表 |
| GET | `/api/v1/roles/{id}` | role:read | 获取角色详情 |
| GET | `/api/v1/permissions/tree` | permission:read | 按模块分组的权限树 |

---

## 5. 认证流程

```
客户端                    user-service
  │                           │
  ├─── GET /auth/public-key ──→ 返回 RSA 公钥
  │                           │
  ├─── POST /auth/login ──→  用密码解密/验证
  │      body: {username, encryptedPassword}    │
  │                           │
  │←─── { accessToken, tokenType } ──┘
  │                           │
  ├─── GET /xxx (Bearer token) ──→ JwtAuthenticationFilter 验证
  │                           │
  │←─── 200 OK ──或─→ 401 ──┘
```

---

## 6. 当前已知问题

| ID | 问题 | 状态 | 说明 |
|----|------|------|------|
| U-001 | AuthController 硬编码占位符登录 | ✅ 已修复 | 真实登录/JWT RS256 已实现 |
| U-002 | signing_key 表未集成轮换 | 🟡 待开发 | KeyManagementService 未实现 |
| U-003 | Refresh Token 未实现 | 🟡 待开发 | 只有 access token |

---

## 7. 数据库表

### 7.1 现有表

| 表名 | 来源 | 说明 |
|------|------|------|
| `outbox`, `saga_log` | allinone V15（共享 DB） | 基础设施表 |
| `signing_key` | user-service JPA ddl-auto | JWT 签名密钥表 |
| `user`, `role`, `permission` | allinone V15（共享 DB） | 用户体系核心表 |

### 7.2 用户体系表（✅ 已完成，依赖 allinone V15）

| 表名 | 状态 | 来源 |
|------|------|------|
| `user` | ✅ | allinone V15 |
| `role` | ✅ | allinone V15 |
| `permission` | ✅ | allinone V15 |
| `user_role`, `role_permission` | ✅ | allinone V15 |
| `company`, `department`, `position` | ✅ | allinone V15 |
| `permission` | P1 | 权限表 |

---

## 8. 行动项

- [x] ✅ **已完成**：修复 AuthController 硬编码占位符登录（2026-04-30）
- [x] ✅ **已完成**：接入真实用户表（依赖 allinone V15，user-service 无独立迁移）
- [ ] **待定**：实现 KeyManagementService（密钥自动轮换）
- [ ] **待定**：实现 Refresh Token 机制
- [ ] **持续**：所有 API 必须通过 Spring Security 认证

---

## 9. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
| `docs/pro/01-java-service-parent.md` | 父 POM 配置 |
