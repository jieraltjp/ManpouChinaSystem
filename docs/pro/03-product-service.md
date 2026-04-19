# 项目文档：product-service（商品服务）

> **文档角色**：后端开发工程师视角 → 商品管理
> **对应角色文档**：`docs/role/04-后端开发工程师视角分析.md`

---

## 1. 服务定位

| 维度 | 说明 |
|------|------|
| 服务名 | product-service |
| 端口 | 18082 |
| 包名 | `com.manpou.product` |
| 描述 | 商品主数据管理（货号/品名/规格/供应商） |
| 当前状态 | 脚手架 ✅，核心功能待开发 |

---

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 3.2.5 | 应用框架 |
| Spring Data JPA | ORM |
| Spring Security | 认证/鉴权 |
| H2（开发）/ MySQL 8（生产） | 数据库 |
| Flyway | 数据库迁移 |

---

## 3. 项目结构

```
src/main/java/com/manpou/product/
├── ProductServiceApplication.java     # 启动类
├── interfaces/
│   └── controller/
│       └── ExampleController.java     # 示例 CRUD
├── application/
│   ├── dto/
│   │   ├── ExampleCreateCmd.java
│   │   └── ExampleQuery.java
│   ├── usecase/
│   │   └── ExampleUseCase.java
│   └── assembler/
│       └── ExampleAssembler.java
├── domain/
│   ├── model/
│   │   └── Example.java
│   └── repository/
│       └── ExampleRepository.java
├── infrastructure/
│   ├── config/
│   │   ├── JpaAuditConfig.java
│   │   └── SecurityConfig.java
│   └── persistence/
│       └── JpaExampleRepositoryImpl.java
└── common/
    └── ...

src/main/resources/
├── application.yml                    # 18082 端口
├── db/migration/
│   ├── V1__init_schema.sql
│   ├── V2__outbox_table.sql
│   └── V3__signing_key_table.sql
└── keys/
    ├── private.pem
    └── public.pem
```

---

## 4. 待实现功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 商品 CRUD | P0 | 增删改查商品主数据 |
| 货号管理 | P0 | 唯一货号（productCode）生成规则 |
| 分类管理 | P1 | 商品分类树 |
| 供应商管理 | P1 | 供应商主数据 |
| 规格管理 | P1 | 商品规格（颜色/尺寸/材质） |

---

## 5. 待创建数据库表

| 表名 | V | 说明 |
|------|---|------|
| `product` | V4 | 商品主表 |
| `product_category` | V5 | 商品分类 |
| `supplier` | V6 | 供应商表 |
| `product_spec` | V7 | 商品规格表 |

---

## 6. 行动项

- [ ] **本周**：理解现有脚手架结构
- [ ] **本周**：设计商品表结构（V4__product_table.sql）
- [ ] **下周二**：实现商品 CRUD API
- [ ] **持续**：商品数据作为采购单的只读参考

---

## 7. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/role/04-后端开发工程师视角分析.md` | 后端开发规范 |
| `docs/role/03-数据库工程师视角分析.md` | 数据模型设计 |
| `docs/pro/00-root-project.md` | 项目全局概览 |
