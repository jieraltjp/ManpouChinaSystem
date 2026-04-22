# 项目文档：manpou-allinone（8 领域合一单体）

> **视角**：INTJ 战略 — 极简先行，演进无价
> **原则**：最好的架构不是设计出来的，是业务长出来的
> **最后更新**：2026-04-22

---

## 1. 定位与职责

| 维度 | 说明 |
|------|------|
| 服务名 | manpou-allinone |
| 端口 | 18090 |
| 包前缀 | `com.manpou.allinone` |
| 定位 | **8 领域合一单体 jar**，后期按 Kafka Topic 边界逐步拆分 |
| 当前状态 | ✅ 编译通过 · 发注单 CRUD · 验货记录 · 调配计划 · 工厂管理 |

**合并领域**：

| 领域 | API 前缀 | 后期拆分目标 | 当前状态 |
|------|---------|------------|---------|
| procurement | `/api/v1/procurements` | procurement-service（Kafka topic: procurement-events） | ✅ CRUD+报价计算 |
| factory | `/api/v1/factories` | factory-service | ✅ domain层+FSM |
| qc | `/api/v1/qc-records` | qc-service | ✅ domain层+FSM |
| logistics | `/api/v1/logistics` | logistics-service（Kafka topic: logistics-events） | ✅ domain层+FSM |
| replenishment | `/api/v1/demands` | replenishment-service | ✅ CRUD+转采购 |
| product | `/api/v1/products` | product-service（Kafka topic: product-events） | 🔴 骨架 |
| customs | `/api/v1/customs` | customs-service（Kafka topic: customs-events） | 🔴 骨架 |
| finance | `/api/v1/finance` | finance-service（Kafka topic: finance-events） | 🔴 骨架 |

**保留独立**：

| 服务 | 端口 | 原因 |
|------|------|------|
| user-service | 18081 | JWT 认证体系成熟，保持独立 |

---

## 2. 演进策略

### 为什么先合后拆？

```
Phase 0（现状）
┌─────────────────────────────────────────────────┐
│ manpou-allinone (18090)                          │
│ procurement + factory + qc + logistics +          │
│ replenishment + product + customs + finance       │
│                                                  │
│ 优点：单进程调试、零服务间延迟、无版本协调          │
│ 缺点：8 领域共享 JVM，单点故障互相影响            │
└─────────────────────────────────────────────────┘

Phase 3-4（业务填充后，按 Kafka Topic 边界拆分）
┌─────────────────────────────────────────────────┐
│ product-service  ──topic: product-events──► warehouse │
│ customs-service ──topic: customs-events──► logistics │
│ finance-service ──topic: finance-events──►   ...   │
│ notification-service                              │
└─────────────────────────────────────────────────┘
```

**何时拆分？**
- 当某个领域的代码量 > 某个阈值（比如 50 个类）
- 当某个领域需要独立发布周期
- 当不同团队需要并发开发同一 jar 中的不同领域

**拆分原则（Kafka Topic 边界）**：
- 发注单创建 → `procurement.events` → 触发仓储/报关/物流
- 验收完成 → `warehouse.events` → 触发报关/物流
- 每个 Topic = 一个限界上下文

---

## 3. 包结构

```
com.manpou.allinone/
├── ManpouAllInOneApplication.java     # 启动入口
│
├── common/                           # 跨领域公共组件
│   ├── annotation/                    # @Idempotent 等
│   ├── config/                      # 配置源（Spring/Nacos）
│   ├── context/                      # 租户上下文
│   ├── exception/                    # BusinessException + GlobalExceptionHandler
│   ├── filter/                      # TraceFilter
│   ├── result/                       # Result<T>
│   └── time/                         # Clock 接口
│
├── domain/                           # 跨领域领域模型
│   ├── model/                        # SigningKey, SigningKeyStatus
│   ├── port/                        # SigningKeyPort
│   └── repository/                   # SigningKeyRepository
│
├── infrastructure/                   # 技术基础设施
│   ├── security/                    # JwtKeyManager, JwtService, JwtAuthenticationFilter
│   └── config/                      # SecurityConfig, ClockConfig, JpaAuditConfig
│
├── interfaces/                       # 对外接口（跨领域）
│   └── controller/
│       ├── AuthController.java      # /api/v1/auth/**（唯一）
│       └── KeyManagementController.java  # /api/v1/admin/keys（唯一）
│
├── procurement/                      # 发注单领域（核心业务 ✅）
│   ├── domain/
│   │   ├── model/               # Procurement.java, ShipmentStatus.java
│   │   └── repository/           # ProcurementRepository.java
│   ├── application/
│   │   └── usecase/             # ProcurementUseCase.java
│   └── interfaces/
│       └── controller/           # ProcurementController.java → /api/v1/procurements
│
├── product/                          # 商品领域
│   ├── domain/
│   │   ├── model/                  # ProductExample, ProductStatus
│   │   └── repository/             # ProductRepository
│   ├── application/
│   │   └── KeyManagementService.java # ⚠️ bean 名: productKeyManagementService
│   └── interfaces/
│       └── controller/
│           └── ProductController.java  # /api/v1/products
│
├── warehouse/                        # 仓储领域（结构同上）
├── customs/                           # 报关领域（结构同上）
├── logistics/                         # 物流领域（结构同上）
├── finance/                           # 财务领域（结构同上）
└── notification/                     # 通知领域（结构同上）
```

---

## 4. 关键设计决策

### 4.1 Bean 命名隔离

每个领域的 `KeyManagementService` 必须有唯一 Spring Bean 名称：

```java
// customs
@Service("customsKeyManagementService")
public class KeyManagementService { ... }

// finance
@Service("financeKeyManagementService")
public class KeyManagementService { ... }
```

否则 Spring 会报 `ConflictingBeanDefinitionException`。

### 4.2 跨领域共享组件

| 组件 | 位置 | 原因 |
|------|------|------|
| `SigningKey` / `SigningKeyRepository` | `domain/` | JWT 认证是全局基础设施 |
| `SigningKeyPort` | `domain/port/` | 端口隔离：Application ↔ Infrastructure |
| `AuthController` | `interfaces/` | 认证只有一份，不能每个领域重复 |
| `Clock` | `common/` | 统一时间源，测试可 mock |

### 4.3 禁止跨领域直接调用

```
product.UseCase  → warehouse.UseCase   ❌ 禁止（领域耦合）
product.UseCase  → domain/SigningKeyRepository  ✅ 允许（共享基础设施）
product.UseCase  → Kafka Topic (warehouse-events)  ✅ 正确解耦
```

---

## 5. 认证机制

**与独立服务完全一致**（RS256 非对称签名）：

| 端点 | 说明 |
|------|------|
| `POST /api/v1/auth/login` | 登录，返回 JWT |
| `GET /api/v1/auth/public-key` | 获取 RSA 公钥（前端验签用） |
| `POST /api/v1/admin/keys/rotate` | 轮换签名密钥（ADMIN 角色） |
| `GET /api/v1/admin/keys` | 列出所有密钥元数据 |

**密钥引导**：首次启动时，如果 DB 中无密钥且无 classpath 密钥，则自动生成 RSA 2048 密钥对并存入 DB。

---

## 6. 数据库

- **开发**：H2 内存（`jdbc:h2:mem:allinone`），不依赖 Docker
- **迁移**：Flyway（`db/migration/V*.sql`，当前禁用）；JPA `ddl-auto: update` 管理开发期 schema
- **表**：`product_example`, `warehouse_example`, `customs_example`, `logistics_example`, `finance_example`, `notification_example`, `signing_key`, `outbox`

---

## 7. 启动验证

```bash
cd apps/manpou-allinone
mvn spring-boot:run -DskipTests

# 冒烟测试
curl http://localhost:18090/api/v1/auth/public-key        # ✅ RSA 公钥
curl http://localhost:18090/api/v1/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'              # ✅ JWT Token

# 认证后访问各领域
TOKEN=$(curl -s .../login | jq -r .data.accessToken)
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:18090/api/v1/procurements            # ✅ 发注单
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:18090/api/v1/products                # ✅
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:18090/api/v1/warehouse              # ✅
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:18090/api/v1/customs                 # ✅
```

---

## 8. API Gateway 路由

```
客户端 → api-gateway (18080)
              ├── /api/v1/auth/**           → user-service (18081)
              └── /api/v1/{procurements,products,warehouse,customs,
                           logistics,finance,notifications}/** → manpou-allinone (18090)
```

> 注意：`/api/v1/purchase-orders/**` 是旧路径，已迁移至 `/api/v1/procurements`。

详见 [`RouteConfig.java`](file://apps/api-gateway/src/main/java/com/manpou/gateway/route/RouteConfig.java)

---

## 9. 扩展方向（按需执行）

| 方向 | 操作 | 前提 |
|------|------|------|
| 接入 MySQL | `docker-compose up mysql` + 修改 `application.yml` | Phase 2 |
| Kafka 事件驱动 | 引入 `spring-kafka`，发布/订阅领域事件 | Phase 3 |
| Redis 缓存 | 启用 `spring-boot-starter-data-redis`，配置连接 | Phase 3 |
| Nacos 配置中心 | 启用 Nacos，application.yml 迁移到 Nacos | Phase 4 |
| 独立拆分 | 从 allinone 中剪切领域包 → 新建 `xxx-service` 模块 | 业务量达到阈值后 |

---

## 10. 相关文档

| 文档 | 说明 |
|------|------|
| [`docs/check/98-项目全貌与演进路线图.md`](../check/98-项目全貌与演进路线图.md) | 演进路线（Phase 0 → 4） |
| [`docs/pro/10-api-gateway.md`](10-api-gateway.md) | API 网关配置 |
| [`docs/pro/17-服务间认证.md`](17-服务间认证.md) | JWT + RS256 机制 |
| `apps/manpou-allinone/src/main/resources/application.yml` | 所有inone 配置 |
