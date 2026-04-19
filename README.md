# ManpouChinaSystem

> 满铺中国发注管理体系

## 项目概述

满铺中国是一套跨境贸易发注管理系统，包含：

- **发注管理** - 订单启动、新品准入、基础数据库维护
- **仓储验收** - 永康待验收、货物调度、人员指派
- **报关通关** - 出口单据生成、INVOICE/PACKING LIST
- **物流调度** - 港口分流、拼柜管理
- **财务结算** - 退税管理、费用核算

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot 3 | 3.2.5 |
| 数据库 | MySQL 8 / TiDB | 8.0 |
| 消息队列 | Apache Kafka | 3.8 |
| 配置中心 | Nacos | 2.3 |
| 前端 | Vue 3 + TypeScript | 3.4 |

## 项目结构

```
ManpouChinaSystem/
├── apps/                    # 微服务
│   ├── user-service/        # 用户服务
│   ├── product-service/      # 商品服务
│   ├── procurement-service/ # 发注服务
│   ├── warehouse-service/   # 仓储服务
│   ├── customs-service/    # 报关服务
│   ├── logistics-service/  # 物流服务
│   ├── finance-service/    # 财务服务
│   └── web/               # 前端应用
├── docs/                   # 文档
└── config/                # 配置
```

## 团队成员

| 角色 | 姓名 | 职责 |
|------|------|------|
| 报关负责人 | 殷元 | 统筹通关全流程 |
| 辅料与库存协同 | 徐义超 | 辅料供应、库存管理 |
| 退税与货柜管理 | 于世荣 | 退税优化、货柜效率 |
| 方案牵头人 | 张云 | 体系升级落地 |
| 财务与系统对接 | 许文豪 | 财务凭证流对接 |
| 系统协同支持 | 陈天仪 | 业务与技术衔接 |

## 快速开始

详见 [docs/README.md](docs/README.md)

---

架构委员会 | arch@manpou.com
