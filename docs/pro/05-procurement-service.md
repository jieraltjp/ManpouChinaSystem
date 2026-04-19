# 项目文档：procurement-service（发注管理服务）

> **文档角色**：后端开发工程师 + 产品经理 + 测试工程师视角 → 核心业务
> **对应角色文档**：
> - `docs/role/04-后端开发工程师视角分析.md`
> - `docs/role/01-产品经理视角分析.md`
> - `docs/role/06-测试工程师视角分析.md`

---

## 1. 服务定位

| 维度 | 说明 |
|------|------|
| 服务名 | procurement-service |
| 端口 | 18083 |
| 包名 | `com.manpou.procurement` |
| 描述 | 发注（采购单）全生命周期管理 — 发注→审批→到货→报关→物流→财务 |
| 当前状态 | 脚手架 ✅，核心功能待开发 |

---

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 3.2.5 | 应用框架 |
| Spring Data JPA | ORM |
| Spring Security | 认证/鉴权 |
| JJWT 0.12.5 | RS256 JWT |
| H2（开发）/ MySQL 8（生产） | 数据库 |
| Flyway | 数据库迁移 |
| SpringDoc | OpenAPI 文档 |

---

## 3. 项目结构

```
src/main/java/com/manpou/procurement/
├── ProcurementServiceApplication.java  # 启动类
├── interfaces/
│   └── controller/
│       └── ExampleController.java      # 示例 CRUD（待替换为采购单 API）
├── application/
│   ├── dto/
│   │   ├── ExampleCreateCmd.java
│   │   ├── ExamplePageQuery.java
│   │   └── ExampleVO.java
│   ├── usecase/
│   │   └── ExampleUseCase.java        # 待替换为 PurchaseOrderUseCase
│   └── assembler/
│       └── ExampleAssembler.java
├── domain/
│   ├── model/
│   │   ├── Example.java
│   │   ├── PurchaseOrder.java         # 待实现：采购单聚合根
│   │   └── OrderStatus.java          # 待实现：状态枚举
│   └── repository/
│       └── ExampleRepository.java
├── infrastructure/
│   ├── config/
│   │   ├── JpaAuditConfig.java
│   │   ├── SecurityConfig.java        # Swagger UI 已配置 permitAll
│   │   └── ClockConfig.java
│   ├── security/
│   │   ├── JwtService.java
│   │   └── JwtAuthenticationFilter.java
│   └── persistence/
│       └── JpaExampleRepositoryImpl.java
└── common/
    ├── exception/
    │   └── GlobalExceptionHandler.java
    ├── result/
    │   └── Result.java
    └── annotation/
        └── Idempotent.java            # 幂等注解

src/main/resources/
├── application.yml                    # 18083 端口
├── config/local.yaml
├── db/migration/
│   ├── V1__init_schema.sql           # example 表
│   ├── V2__outbox_table.sql          # Outbox 消息表
│   └── V3__signing_key_table.sql     # JWT 签名密钥表
└── keys/
    ├── private.pem
    └── public.pem

src/test/java/com/manpou/procurement/
└── arch/
    └── LayeredArchitectureTest.java  # ✅ ArchUnit 测试（已通过）
```

---

## 4. 采购单状态机（待实现）

```
DRAFT（草稿）
    │ 提交
    ↓
PENDING（待审核）
    │ 批准
    ↓         │ 拒绝
APPROVED ───→ REJECTED（拒绝）
    │               │
    │ 取消          │ 重新编辑
    ↓               ↓
CANCELLED       DRAFT
```

| 状态 | 说明 | 可执行动作 |
|------|------|-----------|
| DRAFT | 草稿，可编辑 | submit() |
| PENDING | 待审核 | approve() / reject() |
| APPROVED | 已批准 | ship() / cancel() |
| REJECTED | 已拒绝，可重新编辑 | submit() |
| SHIPPED | 已发货 | arrive() |
| ARRIVED | 已到货 | inspect() |
| CANCELLED | 已取消 | - |

---

## 5. 待实现 API

| 方法 | 路径 | 说明 | 优先级 |
|------|------|------|--------|
| GET | `/api/v1/purchase-orders` | 采购单列表（分页） | P0 |
| GET | `/api/v1/purchase-orders/{id}` | 采购单详情 | P0 |
| POST | `/api/v1/purchase-orders` | 新建采购单 | P0 |
| PUT | `/api/v1/purchase-orders/{id}` | 更新采购单 | P0 |
| POST | `/api/v1/purchase-orders/{id}/submit` | 提交审核 | P0 |
| POST | `/api/v1/purchase-orders/{id}/approve` | 审批通过 | P0 |
| POST | `/api/v1/purchase-orders/{id}/reject` | 审批拒绝 | P0 |
| POST | `/api/v1/purchase-orders/{id}/cancel` | 取消采购单 | P1 |
| GET | `/api/v1/purchase-orders/{id}/items` | 发注明细列表 | P0 |
| POST | `/api/v1/purchase-orders/{id}/items` | 添加发注明细 | P0 |

---

## 6. 数据库迁移（待创建）

| 版本 | 文件 | 说明 |
|------|------|------|
| V4 | `V4__purchase_order_table.sql` | 采购单主表 |
| V5 | `V5__procurement_item_table.sql` | 发注明细表 |
| V6 | `V6__arrival_record_table.sql` | 到货记录表 |
| V7 | `V7__export_document_table.sql` | 出口单据表 |

### 采购单主表核心字段

```sql
CREATE TABLE purchase_order (
    id              BIGINT       NOT NULL  AUTO_INCREMENT  PRIMARY KEY,
    order_no        VARCHAR(32)  NOT NULL  UNIQUE,       -- PO+yyyyMMdd+序号
    orderer_id      BIGINT       NOT NULL,
    contact_name    VARCHAR(64)   NOT NULL,
    contact_phone   VARCHAR(20),
    shipping_address VARCHAR(256),
    is_export       TINYINT      NOT NULL  DEFAULT 0,
    status          VARCHAR(20)   NOT NULL,               -- DRAFT/PENDING/APPROVED...
    factory_code    VARCHAR(32),
    priority        VARCHAR(20)   NOT NULL  DEFAULT 'NORMAL',
    remark          VARCHAR(512),
    -- 审计字段
    create_time     DATETIME(3)  NOT NULL,
    update_time     DATETIME(3)  NOT NULL,
    create_by       VARCHAR(64)   NOT NULL,
    update_by       VARCHAR(64)  NOT NULL,
    is_deleted      TINYINT      NOT NULL  DEFAULT 0,
    INDEX idx_order_no(order_no),
    INDEX idx_status(status)
);
```

---

## 7. 事件驱动（Kafka）

| Topic | 事件 | 发布时机 |
|-------|------|---------|
| `procurement.events` | `PurchaseOrderSubmitted` | 提交审核时 |
| `procurement.events` | `PurchaseOrderApproved` | 审批通过时 |
| `procurement.events` | `PurchaseOrderShipped` | 发货时 |

订阅方：warehouse-service、customs-service

---

## 8. 测试策略

| 测试类型 | 状态 | 说明 |
|---------|------|------|
| ArchUnit | ✅ | 分层约束 + 无循环依赖（已通过） |
| 状态机单元测试 | 🔴 待实现 | 8 个状态转换验证 |
| API 集成测试 | 🔴 待实现 | REST Assured |
| E2E | 🔴 待实现 | Playwright |

---

## 9. Swagger UI

- **地址**：http://localhost:18083/swagger-ui/index.html
- **已配置**：`/swagger-ui.html` 和 `/doc.html` 已加入 `permitAll`

---

## 10. 行动项

- [ ] **本周**：设计并评审采购单 + 发注明细表（V4/V5）
- [ ] **本周**：跑通 Swagger API（登录→获取Token→访问受保护接口）
- [ ] **下周二**：实现采购单状态机（Domain 层）
- [ ] **下周三**：实现采购单 CRUD（Controller→UseCase→Repository）
- [ ] **下周四**：添加采购单状态机单元测试
- [ ] **持续**：所有 PR 必须通过 ArchUnit + 单元测试

---

## 11. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/01-产品经理视角分析.md` | 业务流程与功能列表 |
| `docs/role/02-架构师视角分析.md` | 架构约束与 Kafka 规划 |
| `docs/role/03-数据库工程师视角分析.md` | 数据模型设计 |
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/role/06-测试工程师视角分析.md` | 测试用例（TC-PRO-001~TC-PER-004） |
| `docs/pro/00-root-project.md` | 项目全局概览 |
