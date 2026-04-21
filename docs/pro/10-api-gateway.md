# 项目文档：api-gateway（API 网关）

> **文档角色**：架构师 + 后端开发工程师视角
> **对应目录**：`apps/api-gateway/`
> **风险等级**：✅ 已解决（AR-001 关闭）

---

## 1. 定位与职责

| 维度 | 说明 |
|------|------|
| 服务名 | api-gateway |
| 端口 | 18080 |
| 包名 | `com.manpou.gateway` |
| 定位 | 所有外部请求统一入口 |
| 当前状态 | 已启动验证 ✅，路由配置完成 |

**职责边界**：
- 统一入口：所有前端请求经此网关
- 路由转发：静态路由到各后端服务（端口 18081-18088）
- JWT 鉴权：RS256 公钥验签，Claims 注入下游请求头
- 限流 + 熔断：Resilience4j per-route 配置
- TraceId 透传：W3C traceparent，响应头返回 X-Trace-Id
- 统一 CORS：全局跨域配置

> **禁止**：网关执行业务逻辑。

---

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| Spring Cloud Gateway | 核心网关框架 |
| Resilience4j | 限流 + 熔断 |
| jjwt | JWT RS256 验签 |
| Nacos | 服务发现 + 配置中心（本地禁用） |
| Redis | 分布式限流计数器（可选，无 Redis 时用内存） |

---

## 3. 过滤器执行顺序

```
请求进入
  ↓
[1] TraceIdFilter    ← 最高优先级
  - 提取/生成 traceId，注入 MDC
  - 下游请求头 + 响应头注入 X-Trace-Id
  ↓
[2] JwtAuthFilter    ← 次高优先级
  - 白名单跳过（/api/v1/auth/**, /health, /actuator/health）
  - RS256 公钥验签
  - Claims 注入下游头：X-User-Id / X-Username / X-Tenant-Id / X-User-Roles
  ↓
[3] RateLimitFilter  ← 第三优先级
  - Resilience4j 限流拦截
  - 超限返回 429 + Retry-After: 60
  ↓
路由匹配 → 目标服务
```

---

## 4. 路由配置

| 路径 | 目标服务 | 端口 | 鉴权 |
|------|---------|------|------|
| `/api/v1/auth/**` | user-service | 18081 | ❌ 白名单 |
| `/api/v1/procurements/**` | manpou-allinone | 18090 | ✅ JWT，熔断+重试 |
| `/api/v1/products/**` | manpou-allinone | 18090 | ✅ JWT，熔断 |
| `/api/v1/warehouse/**` | manpou-allinone | 18090 | ✅ JWT，熔断 |
| `/api/v1/customs/**` | manpou-allinone | 18090 | ✅ JWT，熔断 |
| `/api/v1/logistics/**` | manpou-allinone | 18090 | ✅ JWT，熔断 |
| `/api/v1/finance/**` | manpou-allinone | 18090 | ✅ JWT，熔断 |
| `/api/v1/notifications/**` | manpou-allinone | 18090 | ✅ JWT，熔断 |
| `/actuator/**` | forward:/actuator | — | ❌ 白名单 |
| `/health` | forward:/health | — | ❌ 白名单 |

> **注意**：原设计中的 product-service、warehouse-service、customs-service、logistics-service、finance-service、notification-service 均已合并到 manpou-allinone (18090)，网关路由已相应更新。

### 熔断配置（per-route）

```yaml
circuitBreaker:
  fallbackUri: forward:/fallback
retry:
  methods: GET only
  retries: 3
  series: 5xx errors only
```

---

## 5. JWT 鉴权流程

```
前端请求
  Authorization: Bearer <RS256-JWT>
        ↓
JwtAuthFilter 提取 Token
        ↓
JwtValidator 使用 classpath:keys/public.pem 验签（公钥预置在 api-gateway classpath，由 user-service 同步维护）
        ↓
Claims 提取：sub / username / tenantId / roles / permissions
        ↓
注入下游请求头
  X-User-Id: <sub>
  X-Username: <username>
  X-Tenant-Id: <tenantId>
  X-User-Roles: admin,user
  X-User-Permissions: example:read
```

---

## 6. 项目结构

```
apps/api-gateway/
├── pom.xml                          # Maven 依赖（parent: java-service）
├── Dockerfile                       # 多阶段构建（JDK 21）
└── src/
    ├── main/
    │   ├── java/com/manpou/gateway/
    │   │   ├── ApiGatewayApplication.java   # 启动类（排除 Nacos 自动配置）
    │   │   ├── route/
    │   │   │   └── RouteConfig.java        # 9 条静态路由
    │   │   ├── filter/
    │   │   │   ├── TraceIdFilter.java      # TraceId（Order=1）
    │   │   │   ├── JwtAuthFilter.java      # JWT 鉴权（Order=2）
    │   │   │   ├── RateLimitFilter.java    # 限流拦截（Order=3）
    │   │   │   ├── CorsFilter.java        # CORS
    │   │   │   ├── GatewayErrorFilter.java # 错误处理
    │   │   │   └── TraceIdUtil.java       # traceparent 工具
    │   │   ├── security/
    │   │   │   ├── JwtValidator.java      # RS256 验签
    │   │   │   ├── JwtClaims.java         # Claims 记录
    │   │   │   └── JwtPublicKeyManager.java # 公钥加载（实际文件名，非 JwtKeyManager）
    │   │   └── config/
    │   │       └── GatewayConfig.java     # 限流 Bean
    │   └── resources/
    │       ├── application.yml            # 端口 18080，路由配置
    │       └── keys/public.pem            # RS256 公钥（预置，由 user-service 同步维护）
    └── test/
        ├── JwtValidatorTest.java
        └── TraceIdUtilTest.java
```

---

## 7. 启动与验证

```bash
# 构建
cd apps/api-gateway && mvn clean package -DskipTests

# 启动
java -jar target/api-gateway-1.0.0-SNAPSHOT.jar

# 验证
curl http://localhost:18080/health
# → 200 OK

# 测试路由（代理到 user-service）
curl -H "Authorization: Bearer <token>" \
     http://localhost:18080/api/v1/examples
```

> **已同步**：前端 Vite 代理已切换至 `localhost:18080`（E-01 Phase 1 完成）。

---

## 8. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/pro/00-root-project.md` | 项目全局（AR-001 风险追踪） |
| `docs/pro/02-user-service.md` | JWT 签发方（公钥来源） |
| `docs/pro/11-web-frontend.md` | 前端代理配置 |
| `docs/ui/ARCHITECTURE.md` | 架构图（请求流程） |
