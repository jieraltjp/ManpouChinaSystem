# 发注管理 — 业务文档

> 业务分析 + API 契约 + 领域模型

---

## 文档索引

| 编号 | 文档 | 说明 |
|------|------|------|
| 00 | [SPEC-发注管理流程.md](./SPEC-发注管理流程.md) | 需求背景、功能范围、状态机、测试清单 |
| 01 | [API-发注管理.md](./API-发注管理.md) | REST 接口契约、请求/响应格式、错误码 |
| 02 | [DOMAIN-发注管理领域模型.md](./DOMAIN-发注管理领域模型.md) | 聚合根、值对象、枚举、仓储接口、领域服务 |

---

## 业务概览

```
发注单生命周期：
  PENDING → IN_PROGRESS → QC_PENDING → QC_PASSED → SHIPPING → CLOSED
                ↓               ↓
           SUSPENDED       REJECTED(返工)
                               ↓
                            PENDING

运输分流（QC_PASSED 之后）：
  WAREHOUSE → 自有仓二次验收 → 集货
  POOL     → 虚拟拼柜池 → 凑柜 → 装柜
  DIRECT   → 厂家直装 + 报关并行
```

---

## 实现规划

| Phase | 范围 |
|--------|------|
| Phase 3 | 发注单 CRUD + 状态机 + 商品录入 |
| Phase 4 | 验收管理 |
| Phase 5 | 三种运输模式 + 拼柜池 |
| Phase 6 | 货柜管理 + 财务结算 |
