# 测试策略与框架选型

> **版本**: 2.0.0
> **创建**: 2026-04-21
> **更新**: 2026-05-01
> **维护**: MANPOU 开发团队

---

## 摘要

本项目（ManpouChinaSystem）技术栈为 **Java 21 / Spring Boot 3**（后端）+ **Vue 3 / TypeScript**（前端），Phase 0 为单体架构（manpou-allinone），Phase 1 按 Kafka Topic 拆分为微服务。

**现状**：
- 后端：JUnit 5 + AssertJ + Spring Test + ArchUnit 已就绪；Testcontainers BOM 在父 pom.xml 中，但未在 manpou-allinone 子模块显式引入；前端测试完全空白

**推荐**：后端补 Testcontainers + REST Assured，前端引入 Vitest + Testing Library + Playwright。

---

## 一、当前测试基础设施

### 1.1 后端已配置依赖（来自 spring-boot-starter-test）

| 依赖 | 版本 | 状态 | 说明 |
|------|------|------|------|
| JUnit 5 | 5.10.2 (BOM) | ✅ 就绪 | 已在所有测试中使用 |
| AssertJ | 3.25.3 | ✅ 就绪 | 广泛使用 `assertThat(...).isEqualTo()` |
| Mockito | 5.11.0 | ✅ 已引入 | 通过 spring-boot-starter-test 间接引入 |
| Spring Test | 3.2.5 | ✅ 就绪 | `@SpringBootTest` + `@Transactional` |
| ArchUnit | 1.3.2 | ✅ 就绪 | 9个模块均有 `LayeredArchitectureTest` |
| Testcontainers | 1.19.7 (BOM) | ⚠️ BOM已配置 | 父 pom.xml dependencyManagement 有 BOM，需在子模块添加具体 artifact 依赖 |
| H2 Database | 2.2.224 | ✅ 就绪 | 内存测试数据库 |

### 1.2 后端现有测试文件

| 测试类 | 位置 | 覆盖范围 | 状态 |
|--------|------|----------|------|
| `ProcurementUseCaseTest` | manpou-allinone | CRUD / FSM / 业务计算 | ✅ 17个用例 |
| `FactoryUseCaseTest` | manpou-allinone | CRUD | ✅ 8个用例 |
| `QcRecordUseCaseTest` | manpou-allinone | CRUD / FSM | ✅ 10个用例 |
| `LogisticsPlanUseCaseTest` | manpou-allinone | CRUD / FSM | ✅ 12个用例 |
| `ReplenishmentDemandUseCaseTest` | manpou-allinone | CRUD | ✅ 10个用例 |
| `LayeredArchitectureTest` | 各微服务 | DDD 分层约束 | ✅ 9个模块 |
| `JwtValidatorTest` | api-gateway | JWT Claims 验证 | ✅ |

**待补充**：Container、ConsolidationPool、Customs、Finance 模块的 UseCase 测试。

### 1.3 前端现状

| 依赖 | 状态 |
|------|------|
| Vitest | ❌ 未安装 |
| @vue/test-utils | ❌ 未安装 |
| @testing-library/vue | ❌ 未安装 |
| Playwright | ❌ 未安装 |
| Jest | ❌ 未安装 |

### 1.4 现有文档

| 文件 | 内容 |
|------|------|
| `TEST-DATA.md` | Factory/Procurement/QcRecord/LogisticsPlan/Container/ConsolidationPool 测试数据集 |
| `TEST-API.md` | QcRecord/LogisticsPlan/Container/ConsolidationPool API 契约定义 + 状态机矩阵 + curl 脚本 |
| `README.md` | 文档索引 |

### 1.5 Maven Surefire 配置说明

**包含模式**（`apps/java-service/pom.xml` Line 447-449）：
```xml
<includes>
    <include>**/*Test.java</include>
    <include>**/*Tests.java</include>
</includes>
```
`**/*Test.java` 匹配所有以 `Test.java` 结尾的类（包括 `XxxUseCaseTest.java`），因此 `*UseCaseTest` 测试不会被跳过。

**CI Profile ArchUnit 排除**（`apps/java-service/pom.xml` Line 601-603）：
```xml
<excludes>
    <exclude>**/arch/*Test.java</exclude>
</excludes>
```
CI 环境跳过 ArchUnit 测试（版本兼容性本地处理）。日常开发 `mvn test` 会执行 ArchUnit。

### 1.6 Testcontainers 当前状态

| 位置 | 状态 |
|------|------|
| 父 pom.xml `dependencyManagement` | ✅ Testcontainers BOM `1.19.7` 已配置 |
| manpou-allinone/pom.xml | ❌ **未显式引入** testcontainers-mysql / testcontainers-kafka |
| 其他微服务子模块 | ❌ 未引入 |

**引入方法**（在 `manpou-allinone/pom.xml` 的 `<dependencies>` 中添加）：
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 二、框架选型分析

### 2.1 后端框架

#### JUnit 5 ✅ 推荐继续使用
无需更换。已是 Java 生态事实标准，所有现有测试均基于此。

#### AssertJ ✅ 推荐继续使用
流式断言风格，链式调用可读性高。版本 3.25.3 已满足需求。

#### Mockito ⚠️ 补充使用
**现状**：所有后端测试均使用真实 Repository + H2，无 Mock。
**建议**：保持 Integration Test 风格（真实数据库），同时在以下场景补充 Mockito：
- Kafka 消息监听器（Mock KafkaTemplate）
- Redis 缓存服务（Mock RedisTemplate）
- 跨服务调用（RestTemplate Mock）

#### Spring Test ✅ 推荐继续使用
`@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional` 自动回滚，当前配置已验证。

#### Testcontainers ⭐ **必须引入**（Phase 0 末期）

| 理由 |
|------|
| H2 无法模拟 MySQL 8 特有语法（如 `ON DELETE CASCADE`、`JSON` 类型、`FULLTEXT` 索引） |
| CI 环境需要真实 MySQL 而非 H2 |
| Phase 1 微服务化后，Kafka/Testcontainers 是唯一可靠的集成测试方案 |
| 项目已引入 Testcontainers BOM，引入成本极低 |

#### REST Assured ⭐ 推荐引入
API 契约测试。当前 `TEST-API.md` 只有文档没有实现，建议配合 Testcontainers MySQL 编写自动化契约测试。

#### ArchUnit ✅ 已正确使用
DDD 分层约束（Application/Domain/Infrastructure/Interfaces）+ 循环依赖检测，继续保持。

### 2.2 前端框架

| 框架 | 推荐指数 | 理由 |
|------|----------|------|
| **Vitest** | ⭐⭐⭐⭐⭐ | Vite 原生支持，配置简单，与项目 Vite 5.x 完美契合 |
| **Testing Library** | ⭐⭐⭐⭐ | Vue 官方推荐，侧重用户行为而非实现细节 |
| **Playwright** | ⭐⭐⭐⭐ | 跨浏览器 E2E，支持 API 拦截，与 Vitest 可共存 |
| Jest | ⭐⭐ | 生态成熟但配置复杂，与 Vite 集成不如 Vitest 顺畅 |

**推荐组合**：Vitest（单元/组件测试）+ Playwright（E2E 测试）。

不推荐 Cypress 的原因：Cypress 在 TypeScript/Vue 3 生态不如 Playwright 活跃，且 Playwright 支持 API 拦截更适合本项目的 JWT 认证场景。

---

## 三、实施路线图

### Phase 0（立即实施）

#### 后端 — 引入 Testcontainers

**1. 添加 Maven 依赖**

```xml
<!-- apps/manpou-allinone/pom.xml -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

**2. 创建 MySQL Testcontainer 基类**

```java
// src/test/java/.../IntegrationTestBase.java
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/migration/V1__init.sql");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
    }
}
```

**3. 补充缺失的 UseCase 测试**

| 模块 | 优先级 | 测试类 |
|------|--------|--------|
| Container | P1 | `ContainerUseCaseTest` |
| ConsolidationPool | P1 | `ConsolidationPoolUseCaseTest` |
| Customs | P2 | `CustomsDeclarationUseCaseTest` |
| Finance | P2 | `FinanceSettlementUseCaseTest` |

#### 前端 — 建立测试基础设施

**1. 安装依赖**

```bash
cd apps/web
npm install -D vitest @vue/test-utils jsdom
npm install -D @testing-library/vue @testing-library/jest-dom
npm install -D @playwright/test
npx playwright install chromium
```

**2. 配置 vitest.config.ts**

```typescript
// apps/web/vitest.config.ts
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: ['node_modules/', 'src/test/']
    }
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  }
})
```

**3. 配置 package.json scripts**

```json
{
  "scripts": {
    "test:unit": "vitest",
    "test:unit:run": "vitest run",
    "test:e2e": "playwright test",
    "test:e2e:ui": "playwright test --ui",
    "test:coverage": "vitest run --coverage"
  }
}
```

**4. 测试优先级**

| 优先级 | 测试目标 | 说明 |
|--------|----------|------|
| P0 | Composables（useOrderOverview, usePermission, useAuth） | 业务逻辑集中地 |
| P0 | Pinia Store（auth store） | JWT 状态管理 |
| P1 | 关键组件（订单列表、登录表单） | 用户交互核心 |
| P2 | 表格/表单通用组件 | 可复用性高 |

**5. Playwright 配置**

```typescript
// apps/web/playwright.config.ts
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:13000',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:13000',
    reuseExistingServer: !process.env.CI,
  },
})
```

---

### Phase 1（微服务化后）

#### 后端

| 场景 | 工具 | 说明 |
|------|------|------|
| MySQL | Testcontainers MySQL | 每个微服务独立容器 |
| Kafka | Testcontainers Kafka | 消息队列集成测试 |
| Redis | Testcontainers Redis | 缓存层测试 |
| 服务间调用 | `@WebMvcTest` 隔离 Controller | Mock RestTemplate |
| Contract Testing | Spring Cloud Contract | 微服务契约验证 |

#### 前端

| 场景 | 工具 | 说明 |
|------|------|------|
| E2E 覆盖扩展 | Playwright | 覆盖所有关键用户路径 |
| 视觉回归测试 | Playwright + screenshot | 检测 UI 微调导致的布局问题 |
| API Mock | MSW (Mock Service Worker) | 前后端分离并行开发 |

---

## 四、测试金字塔

```
                    ▲ E2E Tests (Playwright)
                   / \
                  /   \  Integration Tests (Testcontainers + SpringBootTest)
                 /     \
                /       \  Component Tests (Vitest + Testing Library)
               /         \
              /           \  Unit Tests (JUnit 5 / Vitest)
             /_____________\
```

| 层级 | 数量 | 执行频率 | 工具 |
|------|------|----------|------|
| E2E | 少（10-20个关键场景） | CI 每次 + 手动触发 | Playwright |
| Integration | 中（每个 UseCase 5-10个） | 每次 PR | SpringBootTest + Testcontainers |
| Component | 多（每个组件 3-10个） | 每次 save | Vitest + @vue/test-utils |
| Unit | 最多（每个类 5-20个） | 每次 save | JUnit 5 / Vitest |

---

## 五、CI/CD 集成

### 5.1 后端（Maven）

```xml
<!-- manpou-allinone/pom.xml -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*UseCaseTest.java</include>
        </includes>
        <!-- Testcontainers 在 CI 自动使用 Docker -->
        <environmentVariables>
            <DOCKER_HOST>unix:///var/run/docker.sock</DOCKER_HOST>
        </environmentVariables>
    </configuration>
</plugin>
```

### 5.2 前端（GitHub Actions 草案）

```yaml
# .github/workflows/test.yml
name: Test
on: [push, pull_request]

jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - run: npm run test:unit:run
      - uses: actions/upload-artifact@v4
        with:
          name: vitest-report
          path: coverage/

  e2e-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - run: npx playwright install --with-deps chromium
      - run: npm run test:e2e
      - uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: playwright-report
          path: playwright-report/
```

---

## 六、测试数据管理

详细数据集见 `TEST-DATA.md`。

### 数据隔离原则

- **后端**：每个测试方法使用 `@Transactional` 自动回滚，无需手动清理
- **Testcontainers**：测试结束后容器自动销毁，数据不泄漏
- **前端**：Vitest 测试使用 `vi.mock()` Mock API 层，不依赖真实后端
- **Playwright E2E**：使用 `beforeEach` 创建测试数据，`afterEach` 清理

### JWT 测试令牌

```bash
# 获取测试令牌（用于 Playwright E2E 和手动测试）
curl -s -X POST http://localhost:18090/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## 七、API 契约测试

详细契约定义见 `TEST-API.md`。

### 状态机测试矩阵

**QcRecord**：

| 当前状态 | 目标状态 | 期望结果 |
|---------|---------|---------|
| PENDING | COMPLETED | ✅ |
| PENDING | RETURN_REQUESTED | ✅ |
| COMPLETED | PENDING | ❌ 禁止 |
| RETURN_REQUESTED | COMPLETED | ✅ |

**LogisticsPlan**：

| 当前状态 | 目标状态 | 期望结果 |
|---------|---------|---------|
| PLANNED | BOOKED | ✅ |
| BOOKED | IN_TRANSIT | ✅ |
| IN_TRANSIT | DELIVERED | ✅ |
| DELIVERED | BOOKED | ❌ 禁止 |

---

## 八、覆盖率目标

| 层级 | 目标 | 说明 |
|------|------|------|
| UseCase 层 | ≥ 80% | 业务逻辑核心 |
| Repository 层 | ≥ 70% | 数据访问路径 |
| Controller 层 | ≥ 60% | HTTP 适配器 |
| 前端 Composables | ≥ 80% | 业务逻辑集中地 |
| 前端组件 | ≥ 50% | 用户交互核心 |

---

## 九、关键决策

| # | 问题 | 决策 | 理由 |
|---|------|------|------|
| 1 | Repository 用 Mock 还是真实数据库？ | **混合**：UseCase 测试用 H2 真实数据库；跨服务/外部依赖场景补充 Mockito | H2 覆盖 90% 场景，Mock 覆盖 Kafka/Redis 等基础设施 |
| 2 | 前端用 Vitest 还是 Jest？ | **Vitest** | 与 Vite 5.x 原生集成，无额外配置开销 |
| 3 | E2E 用 Cypress 还是 Playwright？ | **Playwright** | TypeScript 支持更好，API 拦截适合 JWT 测试场景 |
| 4 | Testcontainers 在 CI 如何运行？ | Docker socket 挂载 | GitHub Actions ubuntu-latest 已内置 Docker |
| 5 | 前端测试是否覆盖样式？ | **不覆盖** | 样式测试维护成本高，依赖人工 Code Review |

---

## 十、立即行动清单

### 本周

- [ ] 后端：在 `manpou-allinone/pom.xml` 中添加 Testcontainers MySQL 依赖
- [ ] 后端：创建 `IntegrationTestBase` 基类
- [ ] 后端：编写 `ContainerUseCaseTest`（参考现有 5 个测试类的模式）
- [ ] 前端：在 `apps/web` 安装 Vitest + Testing Library + Playwright
- [ ] 前端：配置 `vitest.config.ts` 和 `playwright.config.ts`
- [ ] 前端：编写 `useAuth` store 的第一个测试

### 本月

- [ ] 后端：补充 `ConsolidationPoolUseCaseTest`、`CustomsDeclarationUseCaseTest`、`FinanceSettlementUseCaseTest`
- [ ] 后端：引入 Testcontainers Kafka，编写消息集成测试
- [ ] 前端：覆盖所有 Composables 测试（useOrderOverview、usePermission）
- [ ] 前端：覆盖 auth store 测试
- [ ] 前端：编写第一个 E2E 测试（登录流程）
- [ ] 配置 GitHub Actions CI，包含后端 Maven 测试 + 前端 Vitest + Playwright
