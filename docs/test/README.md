# 测试文档

> **版本**: 1.0.0
> **创建**: 2026-04-21
> **维护**: MANPOU 开发团队

---

## 1. 测试策略

### 1.1 分层测试

| 层级 | 工具 | 覆盖目标 |
|------|------|----------|
| 单元测试 | JUnit 5 + AssertJ | UseCase / Domain |
| 集成测试 | SpringBootTest | Controller → UseCase → Repository |
| 契约测试 | REST Assured | API 响应结构 |
| 手动测试 | Postman / curl | 端到端流程 |

### 1.2 测试配置文件

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  cloud:
    nacos:
      config:
        enabled: false
      discovery:
        enabled: false
```

### 1.3 JWT 测试令牌

```bash
# 获取测试令牌
TOKEN=$(curl -s -X POST http://localhost:18090/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# 所有 API 请求带上 Bearer Token
curl -H "Authorization: Bearer $TOKEN" http://localhost:18090/api/v1/...
```

### 1.4 测试通过标准

- 所有 `@Test` 方法必须 green
- 覆盖率：UseCase 层 ≥ 80%
- 禁止 mock Repository（用真实 H2 内存数据库）

---

## 2. 单元测试清单

### 2.1 现有测试

| 测试类 | 覆盖方法 | 状态 |
|--------|----------|------|
| `ProcurementUseCaseTest` | CRUD / FSM 状态机 / 业务计算 | ✅ |
| `FactoryUseCaseTest` | CRUD / 状态机 | ✅ |
| `ReplenishmentDemandUseCaseTest` | CRUD | ✅ |

### 2.2 待补充测试

| 测试类 | 优先级 | 状态 |
|--------|--------|------|
| `QcRecordUseCaseTest` | P0 | 🔴 |
| `LogisticsPlanUseCaseTest` | P0 | 🔴 |
| `ContainerUseCaseTest` | P1 | 🔴 |
| `ConsolidationPoolUseCaseTest` | P1 | 🔴 |

---

## 3. 测试数据

测试数据集位于同目录 `TEST-DATA.md`。

---

## 4. API 契约测试

API 契约测试位于同目录 `TEST-API.md`。

---

## 5. 业务流端到端测试

```
发注单创建 → 状态推进 → 验货记录 → 调配计划
   ↓
1. POST /api/v1/procurements     创建采购单
2. PATCH .../procurements/{id}   更新状态至 発注待
3. POST /api/v1/qc-records       创建验货记录
4. PATCH .../qc-records/{id}      状态→COMPLETED
5. POST /api/v1/logistics-plans   创建调配计划
6. PATCH .../logistics-plans/{id} 状态→BOOKED→IN_TRANSIT→DELIVERED
```
