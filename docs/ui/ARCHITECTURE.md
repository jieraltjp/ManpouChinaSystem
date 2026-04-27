# 系统架构图

> MANPOU 企业管理系统完整架构（前端视角）

---

## 1. 整体架构

```mermaid
graph TB
    %% 前端层
    subgraph Frontend["前端层 (apps/web)"]
        WEB["Vue 3 + Vite\nlocalhost:13000"]
        ROUTER["Vue Router\n路由守卫"]
        STORE["Pinia Store\n认证状态"]
        AXIOS["Axios\nJWT 注入 + 拦截"]
    end

    %% 网关层（待实现）
    subgraph Gateway["API 网关层 (apps/api-gateway)"]
        GW["Spring Cloud Gateway\n:18080"]
        JWT_FILTER["JWT 校验过滤器"]
        RATE_LIMIT["限流过滤器"]
        TRACE["Trace ID 过滤器"]
    end

    %% 后端服务层
    subgraph Backend["后端服务层 (Phase 0 → manpou-allinone + user-service)"]
        ALLINONE["manpou-allinone\n:18090\n7领域合一"]
        USER["user-service\n:18081"]
        PRODUCT["product-service\n:18082 ⚡Phase1+"]
        PROC["procurement-service\n:18083 ⚡Phase1+"]
        WAREHOUSE["warehouse-service\n:18084 ⚡Phase1+"]
        CUSTOMS["customs-service\n:18085 ⚡Phase1+"]
        LOGISTICS["logistics-service\n:18086 ⚡Phase1+"]
        FINANCE["finance-service\n:18087 ⚡Phase1+"]
        NOTIFY["notification-service\n:18088 ⚡Phase1+"]
    end

    %% 基础设施层
    subgraph Infrastructure["基础设施 (docker/compose.yaml)"]
        REDIS["Redis\n:6379"]
        MYSQL["MySQL\n:3306"]
        KAFKA["Kafka\n:9092"]
        NACOS["Nacos\n:8848"]
        MINIO["MinIO\n:9000"]
        PROMETHEUS["Prometheus\n:9090"]
        GRAFANA["Grafana\n:3001"]
    end

    %% 数据流
    WEB -->|"GET /api/*"| ROUTER
    ROUTER -->|"JWT Token"| AXIOS
    AXIOS -->|"proxy :13000→18080"| GW

    GW -->|"路由分发"| USER
    GW -->|"路由分发"| PRODUCT
    GW -->|"路由分发"| PROC

    PROC -->|"Publish"| KAFKA
    PROC -->|"SELECT"| MYSQL

    USER -->|"SELECT"| MYSQL
    USER -->|"Cache"| REDIS

    KAFKA -->|"Consume"| WAREHOUSE
    KAFKA -->|"Consume"| CUSTOMS
    KAFKA -->|"Consume"| LOGISTICS
    KAFKA -->|"Consume"| FINANCE
    KAFKA -->|"Consume"| NOTIFY

    WAREHOUSE -->|"Store files"| MINIO
    PROC -->|"Read key"| REDIS
```

---

## 2. 前端请求流程

```mermaid
sequenceDiagram
    participant U as 用户
    participant WEB as 前端 :13000
    participant AXIOS as Axios 拦截器
    participant GW as API Gateway :18080
    participant SVC as 后端服务 :18081-18088
    participant REDIS as Redis :6379
    participant DB as MySQL :3306

    U->>WEB: 访问 /dashboard
    WEB->>AXIOS: 发起请求 /api/v1/xxx
    AXIOS->>AXIOS: 从 Pinia 读取 JWT Token
    AXIOS->>AXIOS: Authorization: Bearer <token>
    AXIOS->>WEB: 附带头部，发往 :13000
    WEB->>GW: Vite Proxy → :18080
    GW->>GW: JWT 校验过滤器
    GW->>SVC: 路由到目标服务
    SVC->>SVC: 业务逻辑
    SVC->>REDIS: 缓存查询（可选）
    REDIS-->>SVC: 缓存结果
    SVC->>DB: 数据持久化
    DB-->>SVC: 查询结果
    SVC-->>GW: JSON Response
    GW-->>AXIOS: 代理响应
    AXIOS-->>WEB: Result<T> JSON
    WEB-->>U: 页面渲染
```

---

## 3. 认证流程

```mermaid
flowchart LR
    A["访问 /dashboard"] --> B{已登录?}
    B -->|否| C["302 → /login?redirect=/dashboard"]
    B -->|是| D["显示页面"]
    C --> E["输入用户名 + 密码"]
    E --> F["POST /api/v1/auth/login"]
    F --> G["user-service 验证"]
    G --> H["签发 RS256 JWT"]
    H --> I["前端存储 Token → Pinia"]
    I --> J["跳转 /dashboard"]
    J --> D
```

---

## 4. 微服务端口映射

> **Phase 0**：所有七域合一部署在 **manpou-allinone**（18090），前端直连。
> **Phase 1+**：按 Kafka Topic 边界拆分各域为独立微服务（18083-18088），接入 API Gateway（18080）。

```mermaid
graph LR
    subgraph Ports["服务端口"]
        P90["manpou-allinone\n:18090 ✅ Phase0"]
        P81["user-service\n:18081 ✅ Phase0"]
        P83["procurement-service\n:18083 ⚡Phase1+"]
        P84["warehouse-service\n:18084 ⚡Phase1+"]
        P85["customs-service\n:18085 ⚡Phase1+"]
        P86["logistics-service\n:18086 ⚡Phase1+"]
        P87["finance-service\n:18087 ⚡Phase1+"]
        P88["notification-service\n:18088 ⚡Phase1+"]
    end

    subgraph Clients["消费方"]
        GW["API Gateway\n:18080 ⚡Phase1+"]
        WEB["前端\n:13000 ✅ Phase0"]
    end

    WEB --> P90
    WEB --> P81
    GW -.-> P83
    GW -.-> P84
    GW -.-> P85
    GW -.-> P86
    GW -.-> P87
    GW -.-> P88

    WEB -.->|"dev proxy"| P81
    WEB -.->|"dev proxy"| P83
```

---

## 5. 领域事件流（Kafka）

```mermaid
flowchart TB
    subgraph Producers["事件发布者"]
        PROC_P["procurement-service\n发注单审批通过"]
    end

    subgraph Topics["Kafka Topics"]
        T1["procurement.events\n采购单事件"]
        T2["customs.events\n报关事件"]
        T3["logistics.events\n物流事件"]
    end

    subgraph Consumers["事件消费者"]
        W["warehouse-service\n到货登记"]
        C["customs-service\n报关处理"]
        L["logistics-service\n物流处理"]
        F["finance-service\n账务处理"]
        N["notification-service\n通知推送"]
    end

    PROC_P -->|"ProcurementApproved"| T1
    PROC_P -->|"ProcurementShipped"| T1
    PROC_P -->|"CustomsCleared"| T2
    PROC_P -->|"Shipped"| T3

    T1 --> W
    T1 --> C
    T1 --> L
    T1 --> F
    T1 --> N
    T2 --> L
    T3 --> N
```

---

## 6. 前端项目内部架构

```mermaid
graph TB
    subgraph Pages["页面层 pages/"]
        LOGIN["auth/LoginPage.vue\n/login"]
        DASH["dashboard/DashboardPage.vue\n/dashboard"]
        EXAMPLES["dashboard/ExamplesPage.vue\n/examples"]
        DEMAND["procurement/DemandPage.vue\n/procurement/demand"]
        ORDER["procurement/OrderPage.vue\n/procurement/order"]
        INSPECTION["procurement/InspectionPage.vue\n/procurement/inspection"]
        LOGISTICS["procurement/LogisticsPage.vue\n/procurement/logistics"]
        FACTORY["工厂内嵌于 OrderPage.vue\n（无独立页面）"]
    end

    subgraph Layout["布局层 layouts/"]
        APP["AppLayout.vue\n侧边栏 + 顶栏"]
    end

    subgraph Stores["状态层 stores/"]
        AUTH["auth.ts\nToken + Claims"]
    end

    subgraph API["接口层 api/"]
        CLIENT["client.ts\nAxios 实例"]
        ADAPTER["adapters/auth.ts\n认证适配器"]
    end

    subgraph Router["路由 router/"]
        ROUTES["index.ts\n路由 + 守卫"]
    end

    PAGES ==> LAYOUT
    LOGIN -->|"登录成功后"| ROUTES
    ROUTES -->|"beforeEach"| AUTH
    AUTH -->|"isAuthenticated"| ROUTES
    ADAPTER --> CLIENT
    PAGES --> ADAPTER
    PAGES --> AUTH
```

---

## 7. 基础设施组件

| 组件 | 端口 | 用途 | 当前状态 |
|------|------|------|---------|
| MySQL | 3306 | 业务数据持久化 | Docker 可用 |
| Redis | 6379 | 缓存 + 会话（health 检查依赖） | Docker 可用 |
| Kafka | 9092 | 领域事件消息队列 | Docker 可用 |
| Nacos | 8848 | 配置中心 + 注册中心 | Docker 可用 |
| MinIO | 9000 | 对象存储（货物照片） | Docker 可用 |
| Prometheus | 9090 | 指标采集 | Docker 可用 |
| Grafana | 3001 | 可视化监控 | Docker 可用 |

> 本地开发不需要启动 Docker，各服务独立运行。Docker/K8s 空间已预留。

---

## 8. Element Plus 表格布局规范

> 版本: 1.0.0 · 更新: 2026-04-24
> 来源: Lesson 46 — `el-card` + `el-table` 布局防 ::deep 污染

### 8.1 正确写法

```html
<!-- ✅ 不写 table-layout（默认 auto）-->
<!-- ✅ 不写 fixed="right"（无横向滚动时禁用）-->
<!-- ✅ 列宽用 min-width，允许表格按内容扩展 -->
<el-table ...>
  <el-table-column prop="xxx" label="XXX" min-width="120" />
  <el-table-column prop="xxx" label="XXX" min-width="140" show-overflow-tooltip />
</el-table>
```

```css
/* ✅ el-card 内边距由外层控制，不覆盖 el-table 内部结构 */
.table-card :deep(.el-card__body) { padding: 16px; }

/* ❌ 禁止：覆盖 el-table 内部 width/flex 计算，破坏 header/body 对齐 */
:deep(.el-table) { width: 100% !important; }           /* ← 删除 */
:deep(.el-table__header) { width: 100% !important; }  /* ← 删除 */
```

### 8.2 规则总结

| 规则 | 说明 |
|------|------|
| 不写 `table-layout="fixed"` | 保持默认 auto，内容决定列宽 |
| 列宽用 `min-width` | 可扩展；`table-layout: fixed` 下 min-width 不生效 |
| 操作列不写 `fixed="right"` | 无横向滚动需求时，fixed 列会破坏 header/body 对齐 |
| `::deep` 不覆盖内部 width/flex | 只穿透到子组件根节点，不干预框架内部布局计算 |
| `el-card__body` padding 外层控制 | 通过 `:deep(.el-card__body)` 设置，不影响子组件 |

### 8.3 固定列的正确用法

仅在**横向滚动存在时**才使用 `fixed`：

```html
<!-- ✅ 场景：表格列数 > 15，且容器宽度不足以展示全部列时 -->
<el-table ...>
  <el-table-column ... width="80" />
  <!-- ... 很多列 ... -->
  <el-table-column :label="$t('action')" width="160" fixed="right" />
</el-table>
```

本项目所有列表页列数 ≤ 12，无横向滚动，**禁止使用 `fixed`**。

---

*相关文档：[docs/ui/README.md](README.md) · [docs/lessons/Lombok-Decoupling-DI-Lessons.md#Lesson-46]*

---

## 9. Element Plus el-input-number 布局规范

> 版本: 1.0.0 · 更新: 2026-04-27
> 来源: Lesson 58 — 列宽计算须扣 button×2 + el-col padding，内容 < 150px 时按钮截断

### 9.1 el-input-number 内部结构

```
┌──────────────────────────────────────────┐
│ [− 30px] │     input content (数字)     │ [+ 30px] │
└──────────────────────────────────────────┘
  按钮         content = 列宽 - 60px          按钮
```

### 9.2 最小可用宽度

| 元素 | 宽度 |
|------|------|
| 按钮（-/+） | 各 30px × 2 = 60px |
| `el-input__wrapper` 内边距 | 11px × 2 = 22px |
| `el-col` gutter padding | 8px × 2 = 16px |
| **最小可用 content** | **150px** |
| **最小列宽** | **content + 60px + 16px = 226px** |

### 9.3 判定速查表

| 弹窗宽度 | gutter | label | 列数 | span | 估算 content | 可用? |
|---------|--------|-------|------|------|-------------|--------|
| 640px | 16 | 100 | 3 | span 6 | ≈ 153px | ✅ |
| 680px | 10 | 86 | 3 | span 6 | ≈ 193px | ✅ |
| 820px | 16 | 100 | 3 | span 6 | ≈ 213px | ✅ |
| 820px | 16 | 100 | 3 | span 8 | ≈ 158px | ✅ 宽松 |
| 640px | 16 | 100 | 4 | span 4 | ≈ 113px | ❌ 截断 |

### 9.4 规则总结

| 规则 | 说明 |
|------|------|
| `el-input-number` 默认 controls 在两侧 | 禁止使用 `controls-position="right"`（Lesson 49） |
| 列宽 < 226px 时按钮截断 | content = 列宽 - 60px（按钮）- 16px（el-col padding） |
| 数字字段默认值为 `0` | `undefined` 显示空白；默认值 0 提供视觉锚点 |
| `is-required` 不加在数字字段 form-item | 视觉 asterisk 与表单校验不一致；由 `formRules` 统一控制 |
| span 6 适合三列布局（820px 弹窗） | content ≈ 213px，超出 150px 最小值 |

