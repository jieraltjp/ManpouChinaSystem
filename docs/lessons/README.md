# 工程教训总索引

> 项目：ManpouChinaSystem
> 生成：2026-04-27
> 来源：全面审计会话（Lombok-Decoupling-DI-Lessons.md v54 lessons 拆分）

---

## 文档结构

| 文件 | 覆盖范围 | Lesson 编号 |
|------|---------|------------|
| [LESSONS-BACKEND.md](./LESSONS-BACKEND.md) | 后端 Java / Spring / JPA / DDD | 1–6, 10, 25, 29–34, 38 |
| [LESSONS-OPS.md](./LESSONS-OPS.md) | 构建 / 部署 / 环境 / 运维 | 7–9, 17–18, 20, 26–28 |
| [LESSONS-DATABASE.md](./LESSONS-DATABASE.md) | 数据库 / Flyway / Schema | 8, 13, 31–32, 39, 45, 51 |
| [LESSONS-FRONTEND.md](./LESSONS-FRONTEND.md) | 前端 Vue / TS / i18n / Element Plus | 11–12, 14, 16, 33–34, 37, 40–44, 46–50, 52–59 |

---

## 铁律总表（57 条）

### 后端（17 条）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 1 | 跨模块走 Port 接口，domain model 只用 common.enums/exception | Lombok 失效，编译顺序破坏 |
| 2 | Domain model 禁止引用其他模块 Entity | 编译耦合，业务逻辑膨胀在 domain |
| 3 | JPA Repository 继承链 → @Qualifier 显式指定 bean | Spring 启动失败 |
| 4 | BusinessException API 添加前搜索全项目确认不冲突 | 编译失败，API 歧义 |
| 5 | Model 重构后测试必须同步 | 测试编译失败，技术债累积 |
| 6 | Repository 方法不依赖返回类型区分同名方法 | 编译失败 |
| 10 | Controller 返回类型必须属于自己模块 | 模块边界模糊 |
| 25 | 领域层 Repository 禁止加 @Repository | Spring Bean 歧义 |
| 29 | 删除旧注解时必须同步删除所有引用，禁止只删 import | 编译失败 |
| 30 | Test 代码必须与 Model/API 同步更新 | 编译失败 |
| 31 | JSON 存储列必须用 TEXT，不用 VARCHAR | 数据截断 |
| 32 | 实体删除字段后必须同步 DROP DB 列 | 插入失败 |
| 33 | 业务链起点 = Overview 入口锚点 | Demand 新建后不可见 |
| 34 | 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步 | undefined 显示 |
| 38 | 业务逻辑校验在入口处，零值/空值必须防御 | 脏数据 |

### 运维/部署（9 条）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 7 | Windows 用 Git Bash 执行 .sh 脚本 | 脚本报错 |
| 8 | Flyway 禁用时数据初始化走 DevTestDataInitializer | 数据不导入 |
| 9 | 密钥资源走 classpath + 文件系统双路径 | 启动失败 |
| 17 | 开发环境配置走 .env.local/proxy，本地必须可运行 | 环境差异 |
| 18 | private.pem 仅存于签发中心服务，禁止全量分发 | 安全漏洞 |
| 20 | 分页约定 page=0 vs page=1 开发前锁定 | 前后端不对齐 |
| 26 | 打包禁止 `-q` + 确保无旧进程锁 JAR | JAR 不可用 |
| 27 | 运行时依赖不能是 test scope | 启动失败 |
| 28 | 编译与启动必须分离，错误必须可见 | 错误被掩盖 |

### 数据库（7 条）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 13 | Flyway 版本号提前规划，禁止重编号 | checksum 不一致 |
| 31 | JSON 存储列必须用 TEXT，不用 VARCHAR | 数据截断 |
| 32 | 实体删除字段后必须同步 DROP DB 列 | 插入失败 |
| 39 | DB schema 文档版本号 ≥ 代码版本号 | 文档失效 |
| 45 | Flyway 迁移版本号不得重复，冲突时立即修正 | 迁移执行顺序不确定 |
| 51 | JPQL 查询字段名 = 实体字段名，非数据库列名 | 查询报错 |

### 前端（21 条）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 11 | 文档和代码同 commit，持续审计不积累 | 多轮返工 |
| 12 | i18n 从第一天规划，禁止后期打补丁 | 4轮返工 |
| 14 | 命名在开发前锁定，禁止中途改名 | 大量迁移返工 |
| 16 | 前端类型从 OpenAPI schema 生成，禁止手动对齐 | 字段不匹配 |
| 21 | BaseEntity 用 @MappedSuperclass | 审计字段不一致 |
| 24 | 测试数据提取禁止用字符串解析 | 测试脆弱 |
| 33 | 业务链起点 = Overview 入口锚点 | Demand 新建后不可见 |
| 34 | 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步 | undefined 显示 |
| 37 | 前端状态标签必须本地化，禁止直接显示枚举值 | 日语用户看到乱码 |
| 40 | vue-tsc --noEmit 必须通过（strict + noUnusedLocals） | TS6133/TS2345 累积 |
| 41 | 后端 API 破坏性变更 = 前端 API 类型 + 所有调用方 + 单元测试 三处同步 | 运行时错误 |
| 42 | v-for 的 index 参数未使用加 `_` 前缀 | TS6133 |
| 43 | 组件 Props 必须与所有调用方对齐——optional 字段不加 `?` 会导致 TS2345 | 编译失败 |
| 44 | 对话框/表格列标签必须提取为 i18n key，禁止硬编码 | 日语用户无法理解 |
| 46 | `::deep` 禁止覆盖 el-table 内部 width/fixed，固定列仅限列少场景 | 表头/表体错位 |
| 47 | el-table 空状态须防 empty-block 宽度超出——table-layout=fixed 下无数据时列宽异常 | 列宽溢出容器 |
| 48 | el-select 选项去重不能直接替换 ref——须分离原始数据 ref 和去重结果 | 选择失效 |
| 49 | el-input-number 禁止 controls-position="right"——按钮遮挡文字 | 数字不可见 |
| 50 | API 响应必须防御性访问——`data?.content ?? []` 防止空指针 | 运行时崩溃 |
| 52 | dist 构建产物与源文件 commit 历史脱节——CSS working copy 未提交导致样式修复无效 | 样式修复无效 |
| 53 | i18n JSON 中 key 不得重复——后值覆盖前值（JSON 规范未定义合并行为） | 文案错误 |
| 54 | 多文件样式修复必须用 grep 全局扫描——防止"改了 A 漏了 B" | 修复不完整 |
| 55 | el-input-number 所在列 span≥4（dialog 宽 800+），按钮才不被截断 | 按钮显示异常 |
| 56 | 表单 diviser 仅在跨语义区大区块时使用，紧凑表单禁止加分隔线 | 视觉噪音 |
| 57 | 业务关联变更须从 SPEC → DB → 后端 → 前端八层同步，锚点选择决定数据质量 | 调配计划用采购单锚点导致无实际 cargo 尺寸 |
| 58 | el-input-number 列宽 = content - 60px(按钮) - 16px(el-col padding)，content < 150px 时按钮截断 | 按钮文字被遮挡 |

---

## 快速查询

| 场景 | 查哪个文件 |
|------|-----------|
| Lombok 报错 / 跨模块编译失败 | LESSONS-BACKEND.md → Lesson 1 |
| Spring 启动失败 Bean 歧义 | LESSONS-BACKEND.md → Lesson 3, 25 |
| 前端列错位 / el-table 问题 | LESSONS-FRONTEND.md → Lesson 46, 47 |
| 打包后 JAR 无法启动 | LESSONS-OPS.md → Lesson 26 |
| DB 迁移报错 / VARCHAR 超限 | LESSONS-DATABASE.md → Lesson 31, 32 |
| i18n 重复 key / 硬编码 | LESSONS-FRONTEND.md → Lesson 44, 53 |
| 接口变更后数据不对 | LESSONS-BACKEND.md → Lesson 34 |

---

## 新增 Lesson 来源

| Lesson | 来源 |
|--------|------|
| 46–50 | 2026-04-24 Element Plus 表格全面审计 |
| 51 | 2026-04-24 JPQL 字段名错误修复 |
| 52 | 2026-04-27 /procurement/demand dist 时间差审计 |
| 53 | 2026-04-27 zh.json / ja.json 重复 key 修复 |
| 54 | 2026-04-27 全局样式修复漏扫审计 |
| 55 | 2026-04-27 InspectionPage el-input-number 列宽截断修复 |
| 56 | 2026-04-27 InspectionPage dialog 无必要 divider 移除 |
| 57 | 2026-04-27 LogisticsPlan procurementId → qcRecordId 业务锚点修正 |
| 58 | 2026-04-27 LogisticsPage el-input-number 按钮截断宽度计算修正 |
