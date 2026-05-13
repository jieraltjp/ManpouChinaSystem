# 用户中心与权限体系 — SPEC-B11

> **版本**: 1.11.0
> **创建**: 2026-04-30
> **更新**: 2026-05-12（v1.11.0：头像上传功能上线；avatar_url VARCHAR(512)→MEDIUMTEXT；前端 Canvas 压缩→base64 存储；V17 迁移已创建）
> **状态**: ✅ Phase 2 完成；✅ Phase 3 完成（JwtAuthenticationFilter + 21 Controller @PreAuthorize + 前端按钮 v-if + 路由守卫）；✅ Phase 4 完成（操作日志全链路）；Phase 5-6 待开发
> **依据**: 用户需求（用户管理 + 权限 + 操作日志 + 个人信息设置）
> **依赖**: docs/pro/02-user-service.md（user-service 端口 18081）
> **权限代码对齐**: `docs/permission/`（PERMISSION-CODE-ALIGNMENT.md / ALL_PERMISSIONS.md / ROLE-MATRIX.md / AUDIT-LOG-SPEC.md）

---

## 1. 业务背景

当前系统认证处于**脚手架状态**：
- 登录接口硬编码返回 token，无真实用户表
- 无用户 CRUD、无角色/权限管理
- JWT 中 `permissions[]` 未注入 Spring Security
- 前端无用户管理 UI、无权限控制
- 无操作日志
- user-service 与 manpou-allinone 各有一套 signing_key，无法跨服务验证 token

本设计覆盖：
- 用户管理（账号/密码/姓名/邮箱/手机/头像/组织/职务/海关资质）
- 角色与权限管理（4 级角色 + 63 条权限编码（代码 Set）；DB 实际 78 条（V15 INSERT ID: 1~4,5~8,9~12,13~16,21~24,25~28,29~32,41~45,46~50,61~65,66~69,70~73,74~77,78,81~86,87~91,92,93~94,111~114））
- 操作日志（CREATE/UPDATE/DELETE/STATUS_CHANGE/LOGIN 全链路记录）
- 个人中心（信息修改/密码修改/偏好设置）
- **用户注册 + 管理员审核（方案B：JWT 跨服务验证）**

---

## 1.5 JWT 跨服务验证架构（方案B — 最终方案）

### 架构决策

| 方案 | 描述 | 选择 |
|------|------|:----:|
| 方案A | allinone 禁用 JWT 签发，从 user-service 的 signing_key 表读取公钥（共享 DB） | ❌ |
| **方案B（选用）** | user-service 提供公钥 API，allinone 热加载验证（独立微服务） | ✅ |

**方案B 理由**：
- 真正的服务边界隔离，不共享数据库
- allinone 只做 token **验证**（不签发），职责单一
- user-service 作为唯一的 token 签发中心
- 未来可扩展：多服务验证同一套 token

### 数据流

```
┌─────────────────┐          ┌─────────────────────────────┐
│   前端 (web)     │          │      user-service (18081)    │
│                 │          │                             │
│  1.登录 ──────→  │          │  AuthController.login()     │
│    POST /auth/login          │    └─► JwtService 签发       │
│    ←───────────  │          │         RS256 Token         │
│    { token }     │          │    Token payload:            │
│                 │          │    { sub, userId, username,  │
│  2.业务请求 ──→ │          │      roles, permissions, ... }│
│    Bearer {token}│          └─────────────────────────────┘
└─────────────────┘                    │
                                        │ 公钥同步（仅 kid 变更时）
                                        ▼
┌─────────────────────────────┐  拉取   ┌─────────────────────────────┐
│  manpou-allinone (18090)   │←────── │  公钥缓存 + JwtService      │
│                             │         │  验证 token                 │
│  JwtKeyManager (只读模式)    │         │                             │
│  ├─ cache: Map<kid, 公钥>  │         │  verifyToken(token)         │
│  ├─ refresh(): 定时刷新     │         │    └─► payload → SecurityCtx │
│  └─ verify(token): 验证签名 │         └─────────────────────────────┘
│                             │
│  3.业务请求 ──→             │
│    Bearer {token}          │
│    (由 allinone 验证)       │
└─────────────────────────────┘
```

### API 变更

**user-service 新增公钥 API**：

| 方法 | 路径 | 认证 | 说明 |
|------|------|:----:|------|
| GET | `/api/v1/auth/keys/{kid}/public-key` | ❌ | 获取指定 kid 的公钥（allinone 调用） |
| GET | `/api/v1/auth/keys/active/public-key` | ❌ | 获取当前活跃公钥（兼容） |

**响应体**：
```json
{
  "kid": "abc12345",
  "algorithm": "RS256",
  "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhki...\n-----END PUBLIC KEY-----"
}
```

**allinone JwtKeyManager 改造**：

| 改造点 | 现状 | 改造后 |
|--------|------|--------|
| 签发 token | 独立签发 | 禁用（删除 JwtService 签发逻辑） |
| 加载密钥 | 从自身 DB 加载私钥 | 从 user-service API 拉取公钥 |
| 缓存 | 无 | LRU 缓存（max=10 kid） |
| 热加载 | 定时刷新 | 定时刷新 + token 验证失败时主动刷新 |
| 公钥更新 | DB 写入后 reload | 收到 404（kid 不存在）时主动拉取最新活跃公钥 |

**allinone application.yml 新增配置**：
```yaml
jwt:
  verifier:
    endpoint: http://localhost:18081/api/v1/auth/keys  # user-service 公钥端点
    refresh-interval-seconds: 300                      # 每 5 分钟刷新
    connect-timeout-ms: 5000
    read-timeout-ms: 5000
```

### 安全性分析

| 威胁 | 防护措施 |
|------|----------|
| 公钥 API 被恶意调用 | 仅允许内网调用（allinone → user-service 直连），外网不暴露 |
| 公钥缓存过期 | 定时刷新 + 验证失败时主动拉取 |
| token 伪造 | RS256 非对称签名，私钥从未离开 user-service |
| kid 伪造 | allinone 验证签名失败时，拒绝该 token |

### 实施检查点

1. ✅ user-service `AuthController` 新增 `/keys/{kid}/public-key` 端点
2. ✅ user-service `JwtKeyManager` 提供 `getPublicKey(kid)` 接口
3. ✅ allinone `JwtKeyManager` 改造为只读模式（删除签发逻辑）
4. ✅ allinone `JwtAuthenticationFilter` 使用改造后的 `JwtKeyManager`
5. ✅ 前端登录流程不变（仍请求 user-service:18081）
6. ✅ allinone 业务请求无需额外配置（Bearer token 不变）

---

## 2. 数据库设计

### 2.1 ER 关系图

```
company (1) ──── (N) department (1) ──── (N) user (N) ──── (N) role (N) ──── (N) permission
                                │
                                └── (N) position (M-N via user_position)

user ──── (N) audit_log
```

### 2.2 表结构

#### `V4__company.sql`

```sql
CREATE TABLE company (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_code     VARCHAR(32)  NOT NULL UNIQUE COMMENT '公司编码 HAIT-XXX',
  company_name_cn  VARCHAR(128) NOT NULL COMMENT '公司中文名',
  company_name_jp  VARCHAR(128) COMMENT '公司日文名',
  company_type     VARCHAR(32)  COMMENT 'TRADER/TRADER_AGENT/FACTORY/CUSTOMS_BROKER',
  tax_id           VARCHAR(64)  COMMENT '统一社会信用代码',
  address          VARCHAR(256) COMMENT '公司地址',
  contact_person   VARCHAR(64)  COMMENT '联系人',
  contact_phone    VARCHAR(32)  COMMENT '联系电话',
  status           TINYINT      DEFAULT 1,
  create_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time      DATETIME(3)  NOT NULL,
  is_deleted       TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_code (company_code)
);

-- 预置公司
INSERT INTO company (company_code, company_name_cn, company_name_jp, company_type) VALUES
('HAIT-001', '漫普贸易（中国）有限公司', 'マンプ貿易（中国）有限公司', 'TRADER');
```

#### `V5__department.sql`

```sql
CREATE TABLE department (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id      BIGINT       NOT NULL,
  dept_code       VARCHAR(32)  NOT NULL,
  dept_name_cn    VARCHAR(64)  NOT NULL,
  dept_name_jp    VARCHAR(64)  COMMENT '部门日文名',
  parent_id       BIGINT       COMMENT '上级部门（树形支持）',
  sort_order      INT          DEFAULT 0,
  status          TINYINT      DEFAULT 1,
  create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time     DATETIME(3)  NOT NULL,
  is_deleted      TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_company_dept (company_id, dept_code),
  KEY idx_parent (parent_id)
);
```

#### `V6__position.sql`

```sql
CREATE TABLE position (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  position_code   VARCHAR(32)  NOT NULL UNIQUE,
  position_name_cn VARCHAR(64) NOT NULL,
  position_name_jp VARCHAR(64),
  level_          INT          DEFAULT 0 COMMENT '职级（数字越大越高）',
  company_id      BIGINT       COMMENT 'NULL=全局职务，非NULL=该公司私有',
  status          TINYINT      DEFAULT 1,
  create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time     DATETIME(3)  NOT NULL,
  is_deleted      TINYINT      NOT NULL DEFAULT 0,
  KEY idx_company (company_id)
);

INSERT INTO position (position_code, position_name_cn, position_name_jp, level_, company_id) VALUES
('BOSS',      '总经理',     '社長',        100, NULL),
('VP',        '副总经理',   '副社長',       90,  NULL),
('DIRECTOR',  '总监',      'ディレクター',  80,  NULL),
('MGR',       '经理',      'マネージャー',  60,  NULL),
('ASST_MGR',  '主管',     'アシスタントマネージャー', 50, NULL),
('SR_STAFF',  '高级专员',  'シニアスタッフ', 40,  NULL),
('STAFF',     '专员',      'スタッフ',    20,  NULL),
('INTERN',    '实习生',    'インターン',   10,  NULL),
('CUSTOMS_REP','报关员',   '通関士',      40,  NULL);
```

#### `V7__user.sql`

```sql
CREATE TABLE user (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code        VARCHAR(32)  NOT NULL UNIQUE COMMENT '用户编码 U-XXXX',
  username         VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录账号',
  password_hash    VARCHAR(255) NOT NULL COMMENT 'BCrypt哈希',

  -- 基本信息
  name_cn          VARCHAR(64)  COMMENT '中文姓名',
  name_jp          VARCHAR(64)  COMMENT '日文姓名',
  email            VARCHAR(128) NOT NULL UNIQUE,
  phone            VARCHAR(32)  COMMENT '手机号',
  avatar_url       MEDIUMTEXT  COMMENT '头像Base64（JPEG@0.75，200×200，约25~30KB）或外部URL；V17扩展自VARCHAR(512)'

  -- 组织信息（JOIN 查询，冗余字段已移除）
  company_id       BIGINT       COMMENT '所属公司 FK → company.id',
  department_id    BIGINT       COMMENT '所属部门 FK → department.id',

  -- 偏好
  language         VARCHAR(8)   DEFAULT 'zh',
  timezone         VARCHAR(16)  DEFAULT 'CST',

  -- 账号
  status           TINYINT      DEFAULT 1 COMMENT '1=正常 0=禁用',
  last_login_time  DATETIME(3)  COMMENT '最后登录时间',
  last_login_ip    VARCHAR(45)  COMMENT '最后登录IP',

  -- 注册审核（V7新增）
  registration_status VARCHAR(16) DEFAULT 'APPROVED' COMMENT 'APPROVED/PENDING/REJECTED',
  reject_reason    VARCHAR(256) COMMENT '拒绝原因（REJECTED时填写）',

  -- 审计
  create_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time      DATETIME(3)  NOT NULL,
  create_by        VARCHAR(64)  NOT NULL DEFAULT 'SYSTEM',
  update_by        VARCHAR(64)  NOT NULL DEFAULT 'SYSTEM',
  is_deleted       TINYINT      NOT NULL DEFAULT 0,

  UNIQUE KEY uk_code (user_code),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_email (email),
  KEY idx_company (company_id),
  KEY idx_dept (department_id),
  KEY idx_status (status)
);

-- 预置管理员（密码: admin123，BCrypt strength=12，status=APPROVED 跳过审核）
INSERT INTO user (user_code, username, password_hash, name_cn, email, company_id, status, create_by) VALUES
('U-0001', 'admin', '$2a$12$t7mRpfsCDNFgj6LET1Y47eH7J2.MJ5i5nAYwYL6SfKdWE7LN.vqUG', '系统管理员', 'admin@manpou.cn', 1, 1, 'SYSTEM');
```

#### `V7.1__user_position.sql`（替代 JSON）

```sql
-- 用户-职务关联表（M-N 规范化，替代原 JSON 字段）
CREATE TABLE user_position (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT       NOT NULL,
  position_id  BIGINT       NOT NULL,
  create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_user_position (user_id, position_id),
  KEY idx_position (position_id)
);
```

#### `V8__role_permission.sql`

```sql
CREATE TABLE role (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code       VARCHAR(32)  NOT NULL UNIQUE,
  role_name_cn    VARCHAR(64)  NOT NULL,
  role_name_jp    VARCHAR(64)  NOT NULL,
  role_type       VARCHAR(16)  COMMENT 'SYSTEM/BUSINESS',
  description     VARCHAR(256),
  is_editable     TINYINT      DEFAULT 1 COMMENT '0=不可编辑，1=可编辑（预置角色设为1）',
  status          TINYINT      DEFAULT 1,
  create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time     DATETIME(3)  NOT NULL,
  is_deleted      TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE permission (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  permission_code   VARCHAR(64)  NOT NULL UNIQUE,
  permission_name_cn VARCHAR(64) NOT NULL,
  permission_name_jp VARCHAR(64) NOT NULL,
  module            VARCHAR(32)  NOT NULL,
  action            VARCHAR(16)  NOT NULL COMMENT 'READ/CREATE/UPDATE/DELETE/APPROVE/START/COMPLETE/EXPORT',
  description       VARCHAR(256),
  sort_order       INT          DEFAULT 0,
  status            TINYINT      DEFAULT 1,
  create_time       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  is_deleted        TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE user_role (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  role_id     BIGINT       NOT NULL,
  create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_role (role_id)
);

CREATE TABLE role_permission (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id       BIGINT       NOT NULL,
  permission_id BIGINT       NOT NULL,
  create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_role_permission (role_id, permission_id),
  KEY idx_permission (permission_id)
);

-- 预置角色（ADMIN is_editable=0 不可编辑；MANAGER/OPERATOR/VIEWER is_editable=1 可编辑）
INSERT INTO role (role_code, role_name_cn, role_name_jp, role_type, is_editable) VALUES
('ADMIN',   '系统管理员',   'システム管理者',    'SYSTEM',   0),
('MANAGER', '运营主管',     '運営マネージャー', 'BUSINESS', 1),
('OPERATOR','普通运营',     '一般運営者',       'BUSINESS', 1),
('VIEWER',  '查看者',       '閲覧者',           'BUSINESS', 1);
```

#### `V9__audit_log.sql`

```sql
CREATE TABLE audit_log (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id        VARCHAR(64)  COMMENT '链路追踪ID',

  -- 操作人
  user_id         VARCHAR(64)  NOT NULL,
  username        VARCHAR(64)  NOT NULL,
  user_name       VARCHAR(64)  COMMENT '操作人姓名',
  company_id      BIGINT,
  department_id   BIGINT,

  -- 操作信息
  module          VARCHAR(32)  NOT NULL COMMENT '模块标识',
  action          VARCHAR(32)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE/STATUS_CHANGE/LOGIN/LOGOUT/REGISTER/REGISTRATION_APPROVED/REGISTRATION_REJECTED/EXPORT',
  http_method     VARCHAR(8),
  http_url        VARCHAR(256),

  -- 资源
  resource_type   VARCHAR(64)  COMMENT '资源类型',
  resource_id     VARCHAR(64),
  resource_code   VARCHAR(64),

  -- 变更详情
  detail          JSON COMMENT '变更详情JSON',

  -- 上下文
  ip_address      VARCHAR(45),
  user_agent      VARCHAR(512),
  request_id      VARCHAR(64),

  -- 时间
  create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  KEY idx_user_time     (user_id, create_time),
  KEY idx_module_time   (module, create_time),
  KEY idx_resource      (resource_type, resource_id),
  KEY idx_time         (create_time)
);
```

---

## 3. API 设计

> **Base Path**: `/api/v1`
> **认证**: 除登录/公钥外全部需要 Bearer Token
> **统一响应**: `Result<T>` 包装

### 3.1 认证 API（`/api/v1/auth`）

| 方法 | 路径 | 认证 | 说明 |
|------|------|:----:|------|
| GET | `/public-key` | ❌ | 获取 RSA 公钥（前端验签用） |
| GET | `/keys/{kid}/public-key` | ❌ | 获取指定 kid 公钥（allinone 调用，方案B） |
| GET | `/keys/active/public-key` | ❌ | 获取当前活跃公钥（兼容接口） |
| POST | `/register` | ❌ | 用户注册（提交审核） |
| POST | `/login` | ❌ | 登录，返回增强 JWT |
| POST | `/logout` | ✅ | 登出，记录操作日志 |
| PUT | `/password` | ✅ | 修改密码（本人，需验证旧密码） |
| GET | `/me` | ✅ | 获取当前用户完整信息 |

**注册请求体**：
```json
{
  "username": "li.ming",
  "password": "LiMing@123456",
  "nameCn": "李明",
  "email": "li.ming@manpou.cn",
  "phone": "13800138001",
  "companyId": 1,
  "departmentId": 5
}
```

**注册响应体**：
```json
{
  "code": 200,
  "data": {
    "userCode": "U-0002",
    "status": "PENDING",
    "message": "注册已提交，请等待管理员审核"
  }
}
```

**登录请求体**：
```json
{ "username": "admin", "password": "Admin@12345" }
```

**登录校验逻辑**：
```java
public boolean canLogin(User user) {
    return user.getStatus() == 1
        && "APPROVED".equals(user.getRegistrationStatus());
}
```
- 必须同时满足 `status=1` **AND** `registration_status='APPROVED'`
- PENDING（待审核）或 REJECTED（已拒绝）用户不允许登录
- 管理员直建用户 `registration_status='APPROVED'` 跳过审核

**登录响应体**（JWT payload 增强）：
```json
{
  "sub": "1",
  "userCode": "U-0001",
  "userId": 1,
  "username": "admin",
  "name": "系统管理员",
  "email": "admin@manpou.cn",
  "phone": "13800138000",
  "avatarUrl": "/9j/4AAQSkZJRgABAQAAAQABAAD...", // 裸base64（不含data:image/jpeg;base64,前缀）
  "companyId": 1,
  "companyName": "漫普贸易（中国）有限公司",
  "departmentId": 1,
  "departmentName": "信息中心",
  "positionNames": ["系统管理员"],
  "roles": ["ADMIN"],
  "permissions": ["*:*"],  // ADMIN拥有所有权限
  "language": "zh",
  "timezone": "CST",
  "tenantId": "HAIT-001",
  "exp": 1746000000
}
```

### 3.2 用户管理 API（`/api/v1/users`）

| 方法 | 路径 | 权限 | 说明 |
|------|------|:----:|------|
| GET | `/` | user:read | 分页查询用户列表 |
| GET | `/pending` ⚠️ | user:approve | 待审核用户列表（⚠️ 未实现，Phase 2） |
| GET | `/{id}` | user:read | 获取用户详情 |
| POST | `/` | user:create | 新建用户（管理员直建，跳过审核） |
| PUT | `/{id}/approve` ⚠️ | user:approve | 审核通过（⚠️ 未实现，Phase 2） |
| PUT | `/{id}/reject` ⚠️ | user:approve | 审核拒绝（⚠️ 未实现，Phase 2） |
| PUT | `/{id}` | user:update | 编辑用户 |
| DELETE | `/{id}` | user:delete | 软删除用户 |
| PUT | `/{id}/status` | user:update | 启用/禁用用户 |
| PUT | `/{id}/password/reset` | user:reset_password | 重置密码（生成随机密码） |
| PUT | `/{id}/roles` | user:update | 分配角色 |
| PUT | `/{id}/positions` ⚠️ | user:update | 分配职务（⚠️ 未实现，Phase 2） |
| GET | `/{id}/audit-logs` ⚠️ | user:read | 获取该用户操作日志（⚠️ 未实现，Phase 4） |

**查询参数**（GET `/`）：
| 参数 | 类型 | 说明 |
|------|------|------|
| keyword | string | 搜索：姓名/账号/邮箱 |
| companyId | long | 按公司筛选 |
| departmentId | long | 按部门筛选 |
| roleId | long | 按角色筛选 |
| status | int | 状态：1=正常 0=禁用 |
| page | int | 页码（默认0） |
| size | int | 每页（默认20） |

### 3.3 角色管理 API（`/api/v1/roles`）

| 方法 | 路径 | 权限 | 说明 |
|------|------|:----:|------|
| GET | `/` | role:read | 角色列表 |
| GET | `/{id}` | role:read | 角色详情（含权限列表） |
| POST | `/` | role:create | 新建角色 |
| PUT | `/{id}` | role:update | 编辑角色（所有角色均可编辑） |
| DELETE | `/{id}` | role:delete | 删除角色（所有角色均可删除） |
| PUT | `/{id}/permissions` | role:update | 分配权限 |
| PATCH | `/{id}` | role:update | 局部更新角色（仅 isEditable / description） |

### 3.4 权限管理 API（`/api/v1/permissions`）

| 方法 | 路径 | 权限 | 说明 |
|------|------|:----:|------|
| GET | `/` | permission:read | 权限列表（树形结构） |
| GET | `/modules` ⚠️ | permission:read | 按模块分组的权限列表（⚠️ 未实现） |

### 3.5 组织管理 API（`/api/v1/organization`）

| 方法 | 路径 | 权限 | 说明 |
|------|------|:----:|------|
| GET | `/companies` | user:read | 公司列表 |
| POST | `/companies` | user:create | 新建公司 |
| PUT | `/companies/{id}` | user:update | 编辑公司 |
| DELETE | `/companies/{id}` | user:delete | 删除公司 |
| GET | `/departments` | user:read | 部门列表（树形） |
| POST | `/departments` | user:create | 新建部门 |
| PUT | `/departments/{id}` | user:update | 编辑部门 |
| DELETE | `/departments/{id}` | user:delete | 删除部门 |
| GET | `/positions` | user:read | 职务列表 |
| POST | `/positions` | user:create | 新建职务 |
| PUT | `/positions/{id}` | user:update | 编辑职务 |
| DELETE | `/positions/{id}` | user:delete | 删除职务 |

### 3.6 操作日志 API（`/api/v1/audit-logs`）

| 方法 | 路径 | 权限 | 说明 |
|------|------|:----:|------|
| GET | `/` | audit:read | 分页查询日志 |
| GET | `/{id}` | audit:read | 日志详情 |

**查询参数**（GET `/`）：
| 参数 | 类型 | 说明 |
|------|------|------|
| userId | long | 操作人 |
| module | string | 模块 |
| action | string | 动作 |
| resourceType | string | 资源类型 |
| resourceId | string | 资源ID |
| startTime | datetime | 开始时间 |
| endTime | datetime | 结束时间 |
| page | int | 页码 |
| size | int | 每页 |

---

## 4. 权限编码规范

### 4.1 权限编码定义（63 条 ALL_PERMISSIONS Set；DB 实际 78 条 V15）

**格式**: `{模块}:{动作}`
> ⚠️ 以下权限在早期 SPEC 设计时被标记"已剔除"，但实际 V8 迁移中已实现：`order:read`（ID 78）、`permission:read`（ID 92）、`audit:export`（ID 94）、`customs:approve`（ID 45）、`tax_refund:complete`（ID 64）。

#### 发注管理模块（procurement）

| 权限编码 | 中文名 | 日文名 | 动作 |
|---------|--------|--------|------|
| `demand:read` | 查看补货需求 | 補充需要を表示 | READ |
| `demand:create` | 创建补货需求 | 補充需要を作成 | CREATE |
| `demand:update` | 编辑补货需求 | 補充需要を編集 | UPDATE |
| `demand:delete` | 删除补货需求 | 補充需要を削除 | DELETE |
| `procurement:read` | 查看发注单 | 発注書を表示 | READ |
| `procurement:create` | 创建发注单 | 発注書を作成 | CREATE |
| `procurement:update` | 编辑发注单 | 発注書を編集 | UPDATE |
| `procurement:delete` | 删除发注单 | 発注書を削除 | DELETE |
| `shipment:read` | 查看出货批次 | 出荷バッチを表示 | READ |
| `shipment:create` | 创建出货批次 | 出荷バッチを作成 | CREATE |
| `shipment:update` | 编辑出货批次 | 出荷バッチを編集 | UPDATE |
| `shipment:delete` | 删除出货批次 | 出荷バッチを削除 | DELETE |
| `qc:read` | 查看验货记录 | 検品記録を表示 | READ |
| `qc:create` | 创建验货记录 | 検品記録を作成 | CREATE |
| `qc:update` | 编辑验货记录 | 検品記録を編集 | UPDATE |
| `qc:delete` | 删除验货记录 | 検品記録を削除 | DELETE |

#### 物流模块（logistics）

| 权限编码 | 中文名 | 日文名 | 动作 |
|---------|--------|--------|------|
| `logistics:read` | 查看物流调配 | 物流配送を表示 | READ |
| `logistics:create` | 创建物流调配 | 物流配送を作成 | CREATE |
| `logistics:update` | 编辑物流调配 | 物流配送を編集 | UPDATE |
| `logistics:delete` | 删除物流调配 | 物流配送を削除 | DELETE |
| `consolidation:read` | 查看拼柜池 | コンソリを表示 | READ |
| `consolidation:create` | 创建拼柜池 | コンソリを作成 | CREATE |
| `consolidation:update` | 编辑拼柜池 | コンソリを編集 | UPDATE |
| `consolidation:delete` | 删除拼柜池 | コンソリを削除 | DELETE |
| `container:read` | 查看货柜 | コンテナを表示 | READ |
| `container:create` | 创建货柜 | コンテナを作成 | CREATE |
| `container:update` | 编辑货柜 | コンテナを編集 | UPDATE |
| `container:delete` | 删除货柜 | コンテナを削除 | DELETE |

#### 船只管理模块（ship，B-12 新增，V18 ID 115~118）

| 权限编码 | 中文名 | 日文名 | 动作 | ID |
|---------|--------|--------|------|-----|
| `ship:read` | 查看船只 | 船舶を表示 | READ | 115 |
| `ship:create` | 创建船只 | 船舶を作成 | CREATE | 116 |
| `ship:update` | 编辑船只 | 船舶を編集 | UPDATE | 117 |
| `ship:delete` | 删除船只 | 船舶を削除 | DELETE | 118 |

> 注：`container:read/create/update/delete`（ID 29-32）已存在于 V15 baseline。`ship:*`（ID 115~118）为 V18 新增，V15 baseline 尚未包含。

#### 报关模块（customs）

| 权限编码 | 中文名 | 日文名 | 动作 |
|---------|--------|--------|------|
| `customs:read` | 查看国内报关 | 国内通関を表示 | READ |
| `customs:create` | 创建报关单 | 通関書類を作成 | CREATE |
| `customs:update` | 编辑报关单 | 通関書類を編集 | UPDATE |
| `customs:delete` | 删除报关单 | 通関書類を削除 | DELETE |
| `customs:approve` | 审批报关单 | 通関書類を承認 | APPROVE |

#### 日本清关模块（japan_customs）

| 权限编码 | 中文名 | 日文名 | 动作 |
|---------|--------|--------|------|
| `japan_customs:read` | 查看日本清关 | 日本通関を表示 | READ |
| `japan_customs:create` | 创建日本清关 | 日本通関を作成 | CREATE |
| `japan_customs:start` | 启动清关 | 通関を開始 | START |
| `japan_customs:complete` | 完成清关 | 通関を完了 | COMPLETE |
| `japan_customs:delete` | 删除日本清关 | 日本通関を削除 | DELETE |

#### 财务/销售/基础模块

| 权限编码 | 中文名 | 日文名 | 动作 |
|---------|--------|--------|------|
| `tax_refund:read` | 查看退税记录 | 退税記録を表示 | READ |
| `tax_refund:create` | 创建退税记录 | 退税記録を作成 | CREATE |
| `tax_refund:update` | 编辑退税记录 | 退税記録を編集 | UPDATE |
| `tax_refund:complete` | 完成退税 | 退税を完了 | COMPLETE |
| `tax_refund:delete` | 删除退税记录 | 退税記録を削除 | DELETE |
| `sales:read` | 查看销售记录 | 販売記録を表示 | READ |
| `sales:create` | 创建销售记录 | 販売記録を作成 | CREATE |
| `sales:update` | 编辑销售记录 | 販売記録を編集 | UPDATE |
| `sales:delete` | 删除销售记录 | 販売記録を削除 | DELETE |
| `factory:read` | 查看工厂 | 工場を表示 | READ |
| `factory:create` | 创建工厂 | 工場を作成 | CREATE |
| `factory:update` | 编辑工厂 | 工場を編集 | UPDATE |
| `factory:delete` | 删除工厂 | 工場を削除 | DELETE |
| `product:read` | 查看商品 | 商品を表示 | READ |
| `product:create` | 创建商品 | 商品を作成 | CREATE |
| `product:update` | 编辑商品 | 商品を編集 | UPDATE |
| `product:delete` | 删除商品 | 商品を削除 | DELETE |
| `order:read` | 查看订单总览 | 注文一覧を表示 | READ |

#### 系统管理模块

| 权限编码 | 中文名 | 日文名 | 动作 |
|---------|--------|--------|------|
| `user:read` | 查看用户 | ユーザーを表示 | READ |
| `user:create` | 创建用户 | ユーザーを作成 | CREATE |
| `user:update` | 编辑用户 | ユーザーを編集 | UPDATE |
| `user:delete` | 删除用户 | ユーザーを削除 | DELETE |
| `user:approve` | 审核注册用户 | ユーザー登録を承認 | ADMIN |
| `user:reset_password` | 重置用户密码 | パスワードをリセット | ADMIN |
| `role:read` | 查看角色 | 役割を表示 | READ |
| `role:create` | 创建角色 | 役割を作成 | CREATE |
| `role:update` | 编辑角色 | 役割を編集 | UPDATE |
| `role:delete` | 删除角色 | 役割を削除 | DELETE |
| `role:assign` | 分配角色 | 役割を割り当て | ADMIN |
| `permission:read` | 查看权限 | 権限を表示 | READ |
| `audit:read` | 查看操作日志 | 操作ログを表示 | READ |
| `audit:export` | 导出操作日志 | 操作ログをエクスポート | EXPORT |

#### 仓储模块（warehouse）

| 权限编码 | 中文名 | 日文名 | 动作 | 说明 |
|---------|--------|--------|------|------|
| `warehouse:read` | 查看仓储记录 | 倉庫記録を表示 | READ | |
| `warehouse:create` | 创建仓储记录 | 倉庫記録を作成 | CREATE | |
| `warehouse:update` | 编辑仓储记录 | 倉庫記録を編集 | UPDATE | |
| `warehouse:delete` | 删除仓储记录 | 倉庫記録を削除 | DELETE | |

#### 通知模块（notification）

| 权限编码 | 中文名 | 日文名 | 动作 | 说明 |
|---------|--------|--------|------|------|
| `notification:read` | 查看通知 | 通知を表示 | READ | |
| `notification:create` | 创建通知 | 通知を作成 | CREATE | |
| `notification:update` | 编辑通知 | 通知を編集 | UPDATE | |
| `notification:delete` | 删除通知 | 通知を削除 | DELETE | |

> ⚠️ warehouse CRUD（ID 101-104）和 notification CRUD（ID 111-114）已在 V15 DB，但未加入 ALL_PERMISSIONS Set，仅 ADMIN（`*:*`）可用。MANAGER/OPERATOR/VIEWER 无权限。

---

## 5. 权限模板（Role Permission Matrix）

### 5.1 ADMIN（系统管理员）

拥有所有权限（`*:*`），包含 `user:approve`（注册审核）。

### 5.2 MANAGER（运营主管）

| 模块 | read | create | update | delete | 审批/流转 |
|------|:----:|:------:|:------:|:------:|:---------:|
| demand | ✅ | ✅ | ✅ | ✅ | — |
| procurement | ✅ | ✅ | ✅ | ✅ | — |
| shipment | ✅ | ✅ | ✅ | ✅ | — |
| qc | ✅ | ✅ | ✅ | ✅ | — |
| logistics | ✅ | ✅ | ✅ | ✅ | — |
| consolidation | ✅ | ✅ | ✅ | ✅ | — |
| container | ✅ | ✅ | ✅ | ✅ | — |
| customs | ✅ | ✅ | ✅ | ✅ | ✅ approve |
| japan_customs | ✅ | ✅ | — | ✅ | ✅ start/complete |
| tax_refund | ✅ | ✅ | ✅ | ✅ | ✅ complete |
| sales | ✅ | ✅ | ✅ | ✅ | — |
| factory | ✅ | ✅ | ✅ | ✅ | — |
| product | ✅ | ✅ | ✅ | ✅ | — |
| warehouse | ✅ | ✅ | ✅ | ✅ | — |
| notification | ✅ | ✅ | ✅ | ✅ | — |
| user | ✅ | — | — | — | — |
| role | ✅ | — | — | — | — |
| audit | ✅ | — | — | — | — |

### 5.3 OPERATOR（普通运营）

| 模块 | read | create | update | delete | 审批/流转 |
|------|:----:|:------:|:------:|:------:|:---------:|
| demand | ✅ | ✅ | ✅ | — | — |
| procurement | ✅ | ✅ | ✅ | — | — |
| shipment | ✅ | ✅ | ✅ | — | — |
| qc | ✅ | ✅ | ✅ | — | — |
| logistics | ✅ | ✅ | ✅ | — | — |
| consolidation | ✅ | ✅ | ✅ | — | — |
| container | ✅ | ✅ | ✅ | — | — |
| customs | ✅ | ✅ | — | — | — |
| japan_customs | ✅ | ✅ | — | — | — |
| tax_refund | ✅ | ✅ | ✅ | — | — |
| sales | ✅ | ✅ | ✅ | — | — |
| factory | ✅ | ✅ | ✅ | — | — |
| product | ✅ | ✅ | ✅ | — | — |
| order | ✅ | — | — | — | — |
| warehouse | ❌ | ❌ | ❌ | ❌ | — |
| notification | ❌ | ❌ | ❌ | ❌ | — |

### 5.4 VIEWER（查看者）

| 模块 | read |
|------|:----:|
| demand/procurement/shipment/qc/logistics/consolidation/container | ✅ |
| customs/japan_customs/tax_refund/sales/factory/product/order | ✅ |
| warehouse | ❌ |
| notification | ❌ |
| user/role/permission/audit | ❌ |

---

## 6. 操作日志记录规则

### 6.1 触发时机

| 动作 | 触发时机 | detail 内容 |
|------|----------|------------|
| `CREATE` | POST 成功（2xx） | `{ "newData": { 脱敏后的新数据 } }` |
| `UPDATE` | PUT/PATCH 成功（2xx） | `{ "oldData": {...}, "newData": {...}, "changedFields": ["field1"] }` |
| `DELETE` | DELETE 成功（2xx） | `{ "oldData": {...} }` |
| `STATUS_CHANGE` | 状态流转 API | `{ "oldStatus": "PENDING", "newStatus": "APPROVED", "remark": "审批通过" }` |
| `LOGIN` | 登录成功 | `{ "ip": "192.168.1.1", "userAgent": "...", "result": "SUCCESS" }` |
| `LOGIN_FAIL` | 登录失败 | `{ "ip": "...", "username": "admin", "reason": "密码错误" }` |
| `LOGOUT` | 登出 | `{}` |
| `PASSWORD_CHANGE` | 修改密码 | `{ "targetUserId": 1 }`（不记录旧密码） |
| `REGISTER` | 注册提交 | `{ "newData": { username, email, nameCn } }` |
| `REGISTRATION_APPROVED` | 审核通过 | `{ "userId": 2, "approvedBy": "admin" }` |
| `REGISTRATION_REJECTED` | 审核拒绝 | `{ "userId": 2, "rejectReason": "xxx" }` |

### 6.2 不记录的内容

- GET 请求（查询类）
- 健康检查、活跃检测等系统级请求
- 密码明文（hash 值也不记录）
- 敏感字段（passwordHash 等）

### 6.3 实现方式

使用 `@AuditLog` 自定义注解 + AOP 切面：

```java
@AuditLog(module = "user", action = "CREATE")
@PostMapping
public Result<UserVO> create(@Valid @RequestBody UserCreateCmd cmd) { ... }

@AuditLog(module = "demand", action = "UPDATE", resourceType = "Demand")
@PutMapping("/{id}")
public Result<DemandVO> update(@PathVariable Long id, @RequestBody @Valid DemandUpdateCmd cmd) { ... }
```

切面自动从 `UserContext` 获取当前操作人，从 `@AuditLog` 获取模块/动作，从请求/响应体提取资源信息。

---

## 7. /intjcode 自检清单

> 对应 `docs/check/99-全面审计报告.md` §G 标准
> v1.1.0 新增：JWT 跨服务验证架构 + 注册审核流程 + 数据库规范化

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| 能更短？ | ✅ | 本文档精炼，无冗余段落 |
| 有重复？ | ✅ | 无重复 key/接口/字段定义 |
| 验证？ | ✅ | API 路径逐条设计，与前端路由对应 |
| 防腐？ | ✅ | 后端 VO 与前端类型分离定义 |
| 骨架？ | ✅ | Flyway 迁移脚本完整，接口契约清晰 |
| 接口？ | ✅ | RESTful 风格，遵循 `docs/pro/02-user-service.md` 规范 |
| 分离？ | ✅ | user-service 独立管理认证，manpou-allinone 专注业务 |
| 拆分？ | ✅ | 按模块/角色/权限/日志拆分为独立 API |
| 命名？ | ✅ | 权限编码遵循 `{模块}:{动作}` 规范，无禁用词 |
| 熵增？ | ✅ | user 表冗余字段已移除，position_ids 改为中间表 |
| 不可变？ | ✅ | audit_log 只追加，不修改不删除 |
| 正交？ | ✅ | user/role/permission/department/position 各自独立，无循环依赖 |
| 显式？ | ✅ | 所有字段语义显式命名 |
| 错误？ | ✅ | GlobalExceptionHandler 统一处理，`Result<>` 包装 |
| 依赖倒置？ | ✅ | domain 层只引用 common.enums/exception |
| 领域语言？ | ✅ | 权限模块使用业务语言（角色/权限/审批），非技术语言 |
| 日志？ | ✅ | 操作日志全链路记录，含 traceId |
| 提交？ | ✅ | allinone V15→V16；user-service 无迁移 |
| 单体优先？ | ✅ | 认证在 user-service（18081），业务在 manpou-allinone（18090） |
| i18n？ | ✅ | 所有 UI 文本使用 i18n key，中日双语 |
| 密码安全？ | ✅ | BCrypt 哈希，强度 12，永不明文传输/存储 |
| JWT 安全？ | ✅ | RS256 非对称签名，15分钟 TTL，方案B（跨服务验证） |
| JWT 跨服务？ | ✅ | allinone 只读验证，user-service 唯一签发，无密钥共享 |
| 注册审核？ | ✅ | PENDING → APPROVED/REJECTED 状态机，完整流程 + 登录双重校验 |
| 数据库范式？ | ✅ | position_ids JSON → user_position 中间表（M-N 规范化） |
| 幂等性？ | ✅ | approve/reject 改为 PUT，审核操作幂等 |
| 权限完整性？ | ✅ | 代码 ALL_PERMISSIONS Set 63条（V15 DB 实际78条）；ADMIN `*:*` 特殊处理 |
| 审计日志？ | ✅ | REGISTER/REGISTRATION_APPROVED/REJECTED action 已补充 |

---

## 8. 实现依赖与顺序

```
Phase 0: JWT 跨服务验证整合（P0 — 阻塞所有后续）
  ✅ user-service: AuthController 新增 /keys/{kid}/public-key 端点
  ✅ user-service: JwtKeyManager 提供 getPublicKey(kid) 接口
  ✅ allinone: JwtKeyManager 改造为只读（删除签发，保留验证）
  ✅ allinone: JwtAuthenticationFilter 切换为只读验证模式
  ✅ 验证：登录 → 前端获取 token → allinone 业务请求成功

Phase 1: 数据库 + 登录修复（P0）
  V4 → V5 → V6 → V7 → V7.1 → V8
  后端: 真实用户表（company/department/position/user/user_position/role/permission）
  后端: AuthController 接入 BCrypt 校验
  后端: JWT 增强 payload（userCode/roles/permissions）
  后端: JWT permissions 注入 SecurityContext
  后端: @PreAuthorize 权限注解启用

Phase 2: 用户 CRUD + 角色管理（P0）
  后端: User CRUD API（POST/GET/PUT/DELETE）
  后端: Role CRUD API
  后端: Company/Department/Position API
  后端: user:approve 审核 API（GET /pending、POST /approve、POST /reject）
  前端: 用户管理页面
  前端: 角色管理页面
  前端: 权限树（只读）
  前端: 待审核用户列表 + 审核弹窗

Phase 3: 权限控制 ✅ 完成
  ✅ allinone JwtAuthenticationFilter：提取 permissions，ADMIN `*:*` 展开为 ALL_PERMISSIONS（63条）
  ✅ allinone 21个业务 Controller 加 @PreAuthorize
  ✅ 前端: UserPage.vue / RolePage.vue / FactoryPage.vue / ProductPage.vue 按钮级 hasPermission()
  ✅ 前端: 路由守卫 roles 检查（ADMIN/MANAGER）
  ⚠️ 前端: AuditLogPage.vue 缺少 hasPermission('audit:read') 检查（依赖路由 roles 兜底）
  ⚠️ 前端: CosTestPage.vue 缺少 roles 限制（后端 /api/v1/test/** 为 permitAll）

Phase 4: 操作日志 ✅ 完成（2026-05-12）
  后端: @AuditLog 注解（allinone/common/annotation/AuditLog.java）
  后端: AuditLogAspect AOP 切面（allinone/infrastructure/aspect/AuditLogAspect.java）
  后端: AuditLogClient HTTP 客户端（allinone/infrastructure/client/AuditLogClient.java）→ user-service audit_log 表
  后端: 测试端点 POST /api/v1/procurements/test-audit（ProcurementController）
  前端: 操作日志查询页面（pages/system/AuditLogPage.vue）
  前端: 路由 /system/audit-log（router/index.ts，roles=ADMIN/MANAGER）
  前端: API api/auditLog.ts + i18n auditLog.*
  ⚠️ audit:export（CSV 导出）未实现

Phase 5: 个人中心（P2）
  前端: 个人中心页
  前端: 修改密码
  前端: 偏好设置（语言/时区）
  后端: notification CRUD 端点已实现（无前端 UI）
  后端: warehouse CRUD 端点已实现（无前端 UI，DB 有但 ALL_PERMISSIONS Set 无）
  ⚠️ warehouse/notification 前端 UI（Phase 5）

Phase 6: 用户注册 + 管理员审核（P1）
  后端: POST /auth/register（public 接口，提交审核）
  后端: PENDING → APPROVED/REJECTED 状态机
  后端: 审核通过后自动分配 VIEWER 角色
  后端: 注册拒绝时记录 reject_reason
  前端: 登录页增加「注册账号」入口
  前端: 审核通过/拒绝 通知（可邮件，可前端提示）
  ⚠️ user:approve 端点已实现（无前端审核 UI）
```

---

*上一页：[SPEC-B10-商品目录](./SPEC-B10-商品目录-产品管理.md) | 下一步：Phase 4 操作日志（UI-19 待开发，Phase 4 未启动）*
