# 项目文档：api-gateway（API 网关）

> **文档角色**：架构师 + 后端开发工程师视角
> **对应目录**：`scaffolds/api-gateway/`
> **风险等级**：🔴 高（AR-001，待实现）

---

## 1. 定位与职责

| 维度 | 说明 |
|------|------|
| 服务名 | api-gateway |
| 端口 | 8080 |
| 包名 | `com.company.gateway` |
| 定位 | 所有外部请求统一入口 |
| 当前状态 | 脚手架已完成 ✅，待集成到项目 |

**职责边界**：
- 统一入口：所有前端请求经此网关
- 路由转发：`/api/v1/**` → 后端服务，`/api/v2/**` → Python 服务
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
| Nacos | 服务发现 + 配置中心（可选，dev 环境禁用） |
| Redis | 分布式限流计数器（可选，无 Redis 时用内存限流） |

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

| 路径 | 目标服务 | 鉴权 |
|------|---------|------|
| `/api/v1/**` | java-service (:8080) | ✅ JWT |
| `/api/v2/**` | python-service (:8000) | ✅ JWT |
| `/api/v1/auth/**` | java-service | ❌ 白名单 |
| `/actuator/**` | forward:/actuator | ❌ 白名单 |
| `/health` | forward:/health | ❌ 白名单 |

### 熔断配置（per-route）

```yaml
circuitBreaker:
  fallbackUri: forward:/fallback/{service}
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
JwtValidator 使用 classpath:keys/public.pem 验签
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
scaffolds/api-gateway/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/company/gateway/
    │   │   ├── ApiGatewayApplication.java   # 启动类
    │   │   ├── route/
    │   │   │   └── RouteConfig.java        # 静态路由配置
    │   │   ├── filter/
    │   │   │   ├── TraceIdFilter.java      # TraceId 过滤器（Order=1）
    │   │   │   ├── JwtAuthFilter.java      # JWT 鉴权（Order=2）
    │   │   │   ├── RateLimitFilter.java    # 限流拦截（Order=3）
    │   │   │   ├── CorsFilter.java        # CORS
    │   │   │   ├── GatewayErrorFilter.java # 错误处理
    │   │   │   └── TraceIdUtil.java       # traceparent 工具
    │   │   ├── security/
    │   │   │   ├── JwtValidator.java      # RS256 验签
    │   │   │   ├── JwtClaims.java         # Claims 记录
    │   │   │   └── JwtPublicKeyManager.java # 公钥加载
    │   │   └── config/
    │   │       └── GatewayConfig.java
    │   └── resources/
    │       ├── application.yml
    │       └── keys/public.pem            # RS256 公钥
    └── test/
        ├── JwtValidatorTest.java
        └── TraceIdUtilTest.java
```

---

## 7. 配置项

```yaml
gateway:
  route:
    java-service: http://localhost:8080   # 下游服务地址
    python-service: http://localhost:8000
  ratelimit:
    default-rate: 100    # 每秒请求数
    default-burst: 200   # 突发容量
  jwt:
    public-key-path: classpath:keys/public.pem
  circuit-breaker:
    sliding-window-size: 10
    failure-rate-threshold: 50   # 50% 失败率触发熔断
    wait-duration-in-open-state: 30s
    permitted-calls-in-half-open-state: 3
```

---

## 8. 接入步骤

```bash
# 1. 复制脚手架到项目根目录
cp -r scaffolds/api-gateway apps/

# 2. 修改 pom.xml 中的 parent 路径
# 将 ../java-service/pom.xml 改为 ../java-service/pom.xml

# 3. 复制 RS256 公钥（必须与 user-service 的私钥配对）
cp apps/user-service/src/main/resources/keys/public.pem \
   apps/api-gateway/src/main/resources/keys/

# 4. 配置 Vite 代理（开发）
# vite.config.ts
proxy: {
  '/api': { target: 'http://localhost:8080', changeOrigin: true }
}

# 5. 构建
cd apps/api-gateway && mvn clean package -DskipTests

# 6. 运行
java -jar target/api-gateway-1.0.0-SNAPSHOT.jar
```

---

## 9. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/pro/00-root-project.md` | 项目全局（AR-001 高风险标记） |
| `docs/pro/02-user-service.md` | JWT 签发方（公钥来源） |
| `docs/pro/10-web-frontend.md` | 前端代理配置 |
| `docs/ui/ARCHITECTURE.md` | 架构图（请求流程） |
