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
| 当前状态 | 脚手架 ✅，核心功能待开发 |

---

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 3.2.5 | 应用框架 |
| Spring Security | 认证/鉴权 |
| Spring Data JPA | ORM |
| JJWT 0.12.5 | RS256 JWT 签名/验证 |
| H2（开发）/ MySQL 8（生产） | 数据库 |
| Flyway | 数据库迁移（生产启用） |

---

## 3. 项目结构

```
src/main/java/com/manpou/user/
├── UserServiceApplication.java          # 启动类
├── interfaces/
│   └── controller/
│       ├── AuthController.java         # 登录/公钥/密钥管理
│       └── ExampleController.java      # 示例 CRUD
├── application/
│   ├── dto/
│   │   ├── LoginCmd.java              # 登录请求
│   │   ├── LoginVO.java               # 登录响应
│   │   └── ...
│   ├── usecase/
│   │   └── AuthUseCase.java           # 认证业务编排
│   └── assembler/
│       └── AuthAssembler.java         # DTO ↔ Entity 转换
├── domain/
│   ├── model/
│   │   ├── User.java                  # 用户实体
│   │   └── SigningKey.java            # JWT 签名密钥实体
│   └── repository/
│       └── UserRepository.java        # JPA 仓储
├── infrastructure/
│   ├── config/
│   │   ├── JpaAuditConfig.java        # JPA 审计
│   │   ├── SecurityConfig.java       # Spring Security 配置
│   │   └── JwtAuthenticationFilter.java
│   ├── security/
│   │   ├── JwtService.java            # JWT 核心服务
│   │   └── JwtContextHolder.java      # 用户上下文
│   └── persistence/
│       └── JpaUserRepositoryImpl.java
└── common/
    ├── exception/
    │   └── GlobalExceptionHandler.java
    └── result/
        └── Result.java                # 统一响应

src/main/resources/
├── application.yml                    # 主配置（18081 端口）
├── config/local.yaml                  # 本地配置
├── db/migration/
│   ├── V1__init_schema.sql           # example 表
│   ├── V2__outbox_table.sql          # Outbox 消息表
│   └── V3__signing_key_table.sql     # JWT 签名密钥表
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
| POST | `/api/v1/admin/keys` | ADMIN | 创建新密钥 |
| GET | `/api/v1/examples` | USER | 示例列表 |
| POST | `/api/v1/examples` | USER | 创建示例 |
| PUT | `/api/v1/examples/{id}` | USER | 更新示例 |
| DELETE | `/api/v1/examples/{id}` | USER | 删除示例 |

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
| U-001 | AuthController 硬编码占位符登录 | 🔴 待修复 | 需接入真实用户数据 |
| U-002 | signing_key 表未集成轮换 | 🟡 待开发 | KeyManagementService 未实现 |
| U-003 | Refresh Token 未实现 | 🟡 待开发 | 只有 access token |

---

## 7. 数据库表

### 7.1 现有表

| 表名 | V | 说明 |
|------|---|------|
| `example` | V1 | 示例表 |
| `outbox` | V2 | Outbox 消息表 |
| `signing_key` | V3 | JWT 签名密钥表 |

### 7.2 待创建表

| 表名 | 优先级 | 说明 |
|------|--------|------|
| `user` | P0 | 用户主表 |
| `role` | P0 | 角色表 |
| `user_role` | P0 | 用户角色关联表 |
| `permission` | P1 | 权限表 |

---

## 8. 行动项

- [ ] **本周**：修复 AuthController 硬编码占位符登录
- [ ] **本周**：接入真实用户表（V4__user_table.sql）
- [ ] **下周二**：实现 KeyManagementService（密钥自动轮换）
- [ ] **下周三**：实现 Refresh Token 机制
- [ ] **持续**：所有 API 必须通过 Spring Security 认证

---

## 9. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
| `docs/pro/01-java-service-parent.md` | 父 POM 配置 |
