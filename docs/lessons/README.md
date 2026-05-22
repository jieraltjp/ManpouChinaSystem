# 工程教训总索引

> 项目：ManpouChinaSystem
> 生成：2026-04-27
> 更新：2026-05-12（审计系统全面修复 + 教训目录分类重组 + 索引重设计）
> 来源：全面审计会话（Lombok-Decoupling-DI-Lessons.md v54 lessons 拆分 + 审计系统 Round 1-3 修复）

---

## 快速搜索

按场景查找（Ctrl+F 关键字）：

| 场景关键词 | Lesson |
|-----------|--------|
| Lombok 编译失效 / 跨模块 | [1](#1-lombok-编译失效) |
| Spring 启动失败 Bean 歧义 | [3](#3-jpa-repository-继承链-bean-歧义), [25](#25-领域层-repository-禁止加-repository) |
| @PreAuthorize 权限控制失效 | [75](#75-jwt-遗漏-permissions--controller-零注解), [83](#83-preauthorize-内部调用绕过-aop-代理) |
| @EnableAsync 不生效 / 异步变同步 | [80](#80-enableasync-缺失) |
| 审计日志查不到 / 写错 DB | [81](#81-审计日志写入错误数据库) |
| 审计日志 username/operatorName 为 null | [82](#82-usercontext-接口缺失-getusername), [87](#87-operatorname-始终-null) |
| JWT 401 / kid=null / RS256 parse 失败 | [68](#68-resttemplate-resultvo-泛型反序列化失效), [69](#69-jwt-rs256-双重-parse-失败), [79](#79-jwt-密钥来源不一致-导致-401) |
| 打包后 JAR 无法启动 | [26](#26-maven--q-静默模式掩盖打包失败), [76](#76-allinone-重启流程) |
| 前端 el-table 列错位 / 按钮截断 | [46](#46-deep-覆盖-el-table-内部-widthfixed), [55](#55-el-input-number-列宽截断), [58](#58-el-input-number-padding-计算漏扣) |
| i18n 重复 key / 硬编码 | [44](#44-对话框表格列标签须提取为-i18n-key), [53](#53-i18n-json-中-key-不得重复) |
| dist 样式修复无效 | [52](#52-dist-构建产物与源文件-commit-历史脱节) |
| DB VARCHAR 超限 / 旧列残留 | [31](#31-json-存储列须用-text), [32](#32-实体删除字段后须同步-drop-db-列) |
| Flyway 版本冲突 / 部分失败 | [45](#45-flyway-迁移版本号不得重复), [64](#64-flyway-部分失败后须-repair-再重跑) |
| nativeQuery + Pageable ORDER BY 500 | [60](#60-nativequery--pageable-order-by-陷阱) |
| COS 图片预览 404 | [77](#77-cos-url-含-query-param-导致预览-404) |
| JPQL 枚举强转 String ClassCastException | [88](#88-jpql-返回枚举字段强转为-string-导致-classcastexception) |
| Avast SSL 拦截导致 COS TLS 握手失败 | [89](#89-avast-ssl-扫描拦截导致腾讯云-cos-tls-握手失败) |
| i18n key 缺失运行时[intlify] Not found | [90](#90-i18n-key-缺失运行时-intlify-not-found) |
| user-service ddl-auto:none 新字段 500 | [91](#91-user-service-ddl-auto-none-导致新-entity-字段无法写入数据库) |
| JPA findByXxx 返回多条记录 NonUniqueResultException | [92](#92-jpa-findbymastercode-返回多条记录导致-nonuniqueresultexception-500) |
| client.ts 解包导致页面无数据 | [74](#74-clientts-resultt-解包一致性问题) |
| #_return SpEL 不生效 | [85](#85-return-对-responseentityresultlist-静默失效) |
| sanitizeImpl cyclic 误判 | [86](#86-sanitizeimpl-visited-集合误判-dag-为-cyclic) |
| 视图列与 Entity 映射不一致导致 500 | [93](#93-视图列与-entity-映射不一致导致-500) |
| 业务锚点导致 Demand 新建后不可见 | [33](#33-业务链起点-overview-入口锚点), [57](#57-业务关联变更须八层同步) |

---

## Lesson 编号 → 文件索引

| # | 标题 | 文件 |
|---|------|------|
| 1 | Lombok 编译失效的真正根因是编译顺序 | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 2 | 领域模型禁止直接引用其他模块 Entity/VO | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 3 | JPA Repository 继承链会产生多个同名 Bean | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 4 | BusinessException API 必须与调用方严格对齐 | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 5 | Test 代码必须与 Model API 同步 | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 6 | JPA Repository 方法名不一致导致编译失败 | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 7 | Windows 环境脚本必须用 Git Bash 执行 | [ops/LESSONS-OPS.md](./ops/LESSONS-OPS.md) |
| 8 | Flyway 禁用后 V4 迁移数据不会自动导入 | [database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md) |
| 9 | JWT 私钥路径必须在 classpath 和文件系统双重保险 | [ops/LESSONS-OPS.md](./ops/LESSONS-OPS.md) |
| 10 | DTO 与 Entity 混用导致模块边界模糊 | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 11 | 文档与代码同步必须持续进行，不能积累 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 12 | i18n 必须从第一天规划，不能后期打补丁 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 13 | Flyway 迁移版本号必须提前规划，禁止重编号 | [database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md) |
| 14 | 命名一致性必须在开发前锁定，禁止中途改名 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 16 | 前端 API 客户端类型必须与后端 DTO 严格对齐 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 17 | 环境差异配置必须标准化 | [ops/LESSONS-OPS.md](./ops/LESSONS-OPS.md) |
| 18 | private.pem 仅存于签发中心服务，禁止全量分发 | [ops/LESSONS-OPS.md](./ops/LESSONS-OPS.md) |
| 20 | 分页约定 page=0 vs page=1 必须在开发前锁定 | [ops/LESSONS-OPS.md](./ops/LESSONS-OPS.md) |
| 21 | BaseEntity 用 @MappedSuperclass | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 24 | 测试数据提取禁止用字符串解析 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 25 | JPA Domain Repository 禁止加 @Repository | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 26 | Maven `-q` 静默模式掩盖 repackage 失败 | [ops/LESSONS-OPS.md](./ops/LESSONS-OPS.md) |
| 27 | 依赖 scope 必须与实际运行环境匹配 | [ops/LESSONS-OPS.md](./ops/LESSONS-OPS.md) |
| 28 | 编译与启动必须分离，禁止隐蔽失败 | [ops/LESSONS-OPS.md](./ops/LESSONS-OPS.md) |
| 29 | 删除旧注解时必须同步删除所有引用 | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 30 | Test 代码必须与 Model/API 同步更新 | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 31 | JSON 存储列必须用 TEXT，不用 VARCHAR | [database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md) |
| 32 | 实体删除字段后必须同步 DROP DB 列 | [database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md) |
| 33 | 业务链起点 = Overview 入口锚点 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 34 | 前端类型定义必须与后端 VO 同步 | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 37 | 前端状态标签必须本地化，禁止直接显示枚举值 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 38 | 业务逻辑校验必须在入口处，零值/空值必须防御 | [backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md) |
| 39 | 数据库 schema 文档必须与实现同步 | [database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md) |
| 40 | TypeScript strict 编译必须通过 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 41 | 前端 API 签名变更后所有调用方必须同步 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 42 | v-for 的 index 参数未使用时必须加 `_` 前缀 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 43 | 组件 Props 必须与所有调用方对齐 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 44 | 对话框表格列标签必须提取为 i18n key | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 45 | Flyway 迁移文件版本号不得重复 | [database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md) |
| 46 | `::deep` 禁止覆盖 el-table 内部 width/fixed | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 47 | el-table 空状态 empty-block 宽度异常 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 48 | el-select 选项去重不能直接替换 ref | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 49 | el-input-number `controls-position="right"` 导致按钮遮挡文字 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 50 | API 响应必须防御性访问 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 51 | JPQL 查询字段名 = 实体字段名，非数据库列名 | [database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md) |
| 52 | dist 构建产物与源文件 commit 历史脱节 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 53 | i18n JSON 中 key 不得重复 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 54 | 多文件样式修复必须用 grep 全局扫描 | [frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md) |
| 55 | el-input-number 最小可用列宽（span≥4） | [frontend/LESSON-55-56.md](./frontend/LESSON-55-56.md) |
| 56 | 表单 divider 仅在跨语义区大区块时使用 | [frontend/LESSON-55-56.md](./frontend/LESSON-55-56.md) |
| 57 | 业务关联变更须从 SPEC → DB → 后端 → 前端八层同步 | [frontend/LESSON-57.md](./frontend/LESSON-57.md) |
| 58 | el-input-number 列宽计算须扣 padding | [frontend/LESSON-58.md](./frontend/LESSON-58.md) |
| 59 | Flyway 禁用项目新增枚举值须同步 ALTER DB | [database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md) |
| 60 | Spring Data JPA nativeQuery + Pageable ORDER BY 陷阱 | [frontend/LESSON-60.md](./frontend/LESSON-60.md) |
| 62 | BaseEntity 与 Flyway 建表必须同步 | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 63 | JPA TINYINT → Java 类型映射须用 columnDefinition | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 64 | Flyway 部分失败后须 repair 再重跑 | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 65 | Flyway 迁移必须幂等设计 | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 66 | BCrypt 密码哈希必须实际验证 | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 67 | mvn compile 和启动必须分离，不用 `-q` 静默模式 | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 68 | annotationProcessorPaths Lombok 版本必须与 classpath 一致 | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 69 | BaseEntity setter 须 public | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 70 | Repository 使用 Specification 必须显式继承 | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 71 | 同一 .java 文件多个 top-level public 类 | [user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md) |
| 72 | RestTemplate + ParameterizedTypeReference 泛型反序列化失效 | [security/LESSONS-JWT-CROSS-SERVICE.md](./security/LESSONS-JWT-CROSS-SERVICE.md) |
| 73 | JWT RS256 验签必须在有公钥后才能 parse | [security/LESSONS-JWT-CROSS-SERVICE.md](./security/LESSONS-JWT-CROSS-SERVICE.md) |
| 74 | client.ts Result\<T\> 解包一致性问题 | [frontend/LESSON-API-UNWRAP-CONSISTENCY.md](./frontend/LESSON-API-UNWRAP-CONSISTENCY.md) |
| 75 | allinone JWT 遗漏 permissions + 17个 Controller 零注解 | [security/LESSON-75.md](./security/LESSON-75.md) |
| 76 | allinone 重启流程——JAR 锁定的正确处理 | [backend/LESSON-76.md](./backend/LESSON-76.md) |
| 77 | COS URL 含 query param 导致预览 404 | [frontend/LESSON-COS-URL-QUERY-PARAM.md](./frontend/LESSON-COS-URL-QUERY-PARAM.md) |
| 78 | 权限三角审计：前端/后端/DB 完全一致 | [security/LESSON-PERMISSION-AUDIT-2026-05-08.md](./security/LESSON-PERMISSION-AUDIT-2026-05-08.md) |
| 79 | JWT 密钥来源不一致导致 allinone 401 | [security/LESSON-JWT-KEY-ENV.md](./security/LESSON-JWT-KEY-ENV.md) |
| 80 | @EnableAsync 缺失导致 @Async 方法同步执行 | [audit/LESSON-AUDIT-LOG-2026-05-12.md](./audit/LESSON-AUDIT-LOG-2026-05-12.md) |
| 81 | 审计日志写入 user_service DB 而非 manpou DB | [audit/LESSON-AUDIT-LOG-2026-05-12.md](./audit/LESSON-AUDIT-LOG-2026-05-12.md) |
| 82 | UserContext 接口缺失 getUsername() | [audit/LESSON-AUDIT-LOG-2026-05-12.md](./audit/LESSON-AUDIT-LOG-2026-05-12.md) |
| 83 | @PreAuthorize 在内部方法调用时绕过 AOP 代理 | [audit/LESSON-AUDIT-LOG-2026-05-12.md](./audit/LESSON-AUDIT-LOG-2026-05-12.md) |
| 84 | allinone 未重启导致审计日志链路诊断失效 | [audit/LESSON-AUDIT-LOG-2026-05-12.md](./audit/LESSON-AUDIT-LOG-2026-05-12.md) |
| 85 | `#_return` 对 ResponseEntity/Result\<List\> 静默失效 | [backend/LESSON-85.md](./backend/LESSON-85.md) |
| 86 | sanitizeImpl visited 集合误判 DAG 为 cyclic | [backend/LESSON-86.md](./backend/LESSON-86.md) |
| 87 | 操作日志 operatorName 始终 null | [audit/LESSON-87.md](./audit/LESSON-87.md) |
| 88 | JPQL 返回枚举字段强转为 String 导致 ClassCastException | [backend/LESSON-88.md](./backend/LESSON-88.md) |
| 89 | Avast SSL 扫描拦截导致腾讯云 COS TLS 握手失败 | [ops/LESSON-89.md](./ops/LESSON-89.md) |

> 注：Lesson 15, 19, 22-23, 35-36, 61 未分配，保持编号连续便于追溯历史。

---

## 铁律总表（按分类）

### 后端 — backend/

| # | 铁律 | 违反后果 |
|---|------|---------|
| 1 | 跨模块走 Port 接口，domain model 只用 common.enums/exception | Lombok 失效，编译顺序破坏 |
| 2 | Domain model 禁止引用其他模块 Entity | 编译耦合，业务逻辑膨胀 |
| 3 | JPA Repository 继承链 → @Qualifier 显式指定 bean | Spring 启动失败 |
| 4 | BusinessException API 添加前搜索全项目确认不冲突 | 编译失败，API 歧义 |
| 5 | Model 重构后测试必须同步 | 测试编译失败 |
| 6 | Repository 方法不依赖返回类型区分同名方法 | 编译失败 |
| 10 | Controller 返回类型必须属于自己模块 | 模块边界模糊 |
| 25 | 领域层 Repository 禁止加 @Repository | Spring Bean 歧义 |
| 29 | 删除旧注解时必须同步删除所有引用 | 编译失败 |
| 30 | Test 代码必须与 Model/API 同步更新 | 编译失败 |
| 34 | 接口变更 = 后端 VO + 前端类型 + 模板 + i18n 同步 | undefined 显示 |
| 38 | 业务逻辑校验在入口处，零值/空值必须防御 | 脏数据 |
| 76 | 代码变更后重启 allinone：先停进程→等5秒→删JAR→重打包→干跑验证 | 旧 JAR 无新代码 |
| 83 | @PreAuthorize 必须加在 Controller 层，内部方法调用绕过 AOP 代理 | 权限控制形同虚设 |
| 85 | `#_return` SpEL 引用仅支持 Result\<T\> 单值，不支持 ResponseEntity/Result\<List\> | 审计日志 resourceId 为 null |

### 运维/部署 — ops/

| # | 铁律 | 违反后果 |
|---|------|---------|
| 7 | Windows 用 Git Bash 执行 .sh 脚本 | 脚本报错 |
| 9 | 密钥资源走 classpath + 文件系统双路径 | 启动失败 |
| 17 | 开发环境配置走 .env.local/proxy，本地必须可运行 | 环境差异 |
| 18 | private.pem 仅存于签发中心服务，禁止全量分发 | 安全漏洞 |
| 20 | 分页约定 page=0 vs page=1 开发前锁定 | 前后端不对齐 |
| 26 | 打包禁止 `-q` + 确保无旧进程锁 JAR | JAR 不可用 |
| 27 | 运行时依赖不能是 test scope | 启动失败 |
| 28 | 编译与启动必须分离，错误必须可见 | 错误被掩盖 |
| 89 | 新增第三方 HTTPS 调用先 curl -v 验证 TLS 连通性 | Avast SSL 拦截导致 COS 500 |

### 数据库 — database/

| # | 铁律 | 违反后果 |
|---|------|---------|
| 8 | Flyway 禁用时数据初始化走 DevTestDataInitializer | 数据不导入 |
| 13 | Flyway 版本号提前规划，禁止重编号 | checksum 不一致 |
| 31 | JSON 存储列必须用 TEXT，不用 VARCHAR | 数据截断 |
| 32 | 实体删除字段后必须同步 DROP DB 列 | 插入失败 |
| 39 | DB schema 文档版本号 ≥ 代码版本号 | 文档失效 |
| 45 | Flyway 迁移版本号不得重复，冲突时立即修正 | 迁移执行顺序不确定 |
| 51 | JPQL 查询字段名 = 实体字段名，非数据库列名 | 查询报错 |
| 59 | Flyway 禁用项目新增枚举值须同步 ALTER DB 列类型 | Data truncated 500 错误 |
| 60 | nativeQuery=true + Pageable 排序 → Spring Data 追加实体属性名（非列名）→ Unknown column | 500 错误 |

### 前端 — frontend/

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
| 47 | el-table 空状态须防 empty-block 宽度超出 | 列宽溢出容器 |
| 48 | el-select 选项去重不能直接替换 ref——须分离原始数据 ref 和去重结果 | 选择失效 |
| 49 | el-input-number 禁止 controls-position="right" | 数字不可见 |
| 50 | API 响应必须防御性访问——`data?.content ?? []` | 运行时崩溃 |
| 52 | dist 构建产物与源文件 commit 历史脱节 | 样式修复无效 |
| 53 | i18n JSON 中 key 不得重复——后值覆盖前值 | 文案错误 |
| 54 | 多文件样式修复必须用 grep 全局扫描 | 修复不完整 |
| 55 | el-input-number 所在列 span≥4（dialog 宽 800+） | 按钮显示异常 |
| 56 | 表单 divider 仅在跨语义区大区块时使用，紧凑表单禁止加分隔线 | 视觉噪音 |
| 57 | 业务关联变更须从 SPEC → DB → 后端 → 前端八层同步 | 数据质量差 |
| 58 | el-input-number 列宽 = content - 60px(按钮) - 16px(el-col padding) | 按钮文字被遮挡 |
| 74 | client.ts Result\<T\> 解包后 API 泛型 + 页面访问必须同步 | 页面无数据 |

### 安全/JWT — security/

| # | 铁律 | 违反后果 |
|---|------|---------|
| 68 | RestTemplate 反序列化 `Result<内部类VO>` 须用 Map 中间层 | kid=null，401 |
| 69 | RS256 JWT 提取 kid 必须从 header base64url 解码，禁止先验签再提取 | RS256 无公钥 parse 抛异常，401 |
| 75 | JWT 新增 claim 必须两端同步；新增 Controller 必须加 @PreAuthorize | 权限验证形同虚设 |
| 78 | 权限三角（前端/后端/DB）必须完全一致 | 孤岛权限绕过后台 |
| 79 | JWT 密钥来源必须统一——env var > classpath > DB，禁止混用 | allinone 401 |

### user-service 专项 — user-service/

| # | 铁律 | 违反后果 |
|---|------|---------|
| 62 | BaseEntity 字段变更必须同步 Flyway ALTER 迁移 | JPA schema-validation 失败 |
| 63 | MySQL TINYINT 列对应 Java Integer/Boolean 须用 columnDefinition | 类型不匹配启动失败 |
| 64 | Flyway 部分失败后先 repair 再重跑 | 困在半失败状态无法启动 |
| 65 | Flyway INSERT 必须幂等（ON DUPLICATE KEY UPDATE） | 重跑失败或数据重复 |
| 66 | BCrypt hash 不用记忆，用时现生成并验证 | 密码错误无法登录 |
| 67 | 编译和启动分离，不用 -q 静默模式 | 错误被掩盖无法诊断 |
| 68 | annotationProcessorPaths Lombok 版本必须与 classpath 一致 | Lombok 不生成代码 |
| 69 | BaseEntity setter 须 public（跨包 Service 访问） | 字段无法赋值 |
| 70 | Repository 使用 Specification 必须显式继承 JpaSpecificationExecutor | 方法不存在 |

> Lesson 68-69 在 `user-service/` 和 `security/` 均有对应记录（跨类别同名问题）。

### 审计日志专项 — audit/

| # | 铁律 | 违反后果 |
|---|------|---------|
| 80 | `@EnableAsync` 缺失导致 `@Async` 方法同步执行 | 异步变同步，事务污染 |
| 81 | 跨服务审计日志查不到时先确认写入方数据库 | 查错库浪费时间 |
| 82 | 接口新增方法时必须检查所有实现类同步 | 接口实现缺失，运行时 NPE |
| 83 | `@PreAuthorize` 必须加在 Controller 层，内部方法调用绕过 AOP 代理 | 权限控制形同虚设 |
| 84 | 异步/sendAsync 方法变更后必须重启目标服务确认生效 | 异步逻辑形同虚设 |
| 87 | JWT 缺少 realName/companyId/departmentId claim → operatorName 始终 null | 审计日志用户信息不完整 |

---

## Lesson 锚点（快速跳转）

### 1. Lombok 编译失效 {#1-lombok-编译失效}
文件：[backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md)

### 3. JPA Repository 继承链 Bean 歧义 {#3-jpa-repository-继承链-bean-歧义}
文件：[backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md)

### 25. 领域层 Repository 禁止加 @Repository {#25-领域层-repository-禁止加-repository}
文件：[backend/LESSONS-BACKEND.md](./backend/LESSONS-BACKEND.md)

### 31. JSON 存储列须用 TEXT {#31-json-存储列须用-text}
文件：[database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md)

### 32. 实体删除字段后须同步 DROP DB 列 {#32-实体删除字段后须同步-drop-db-列}
文件：[database/LESSONS-DATABASE.md](./database/LESSONS-DATABASE.md)

### 33. 业务链起点 Overview 入口锚点 {#33-业务链起点-overview-入口锚点}
文件：[frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md)

### 44. 对话框表格列标签须提取为 i18n key {#44-对话框表格列标签须提取为-i18n-key}
文件：[frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md)

### 46. ::deep 覆盖 el-table 内部 width/fixed {#46-deep-覆盖-el-table-内部-widthfixed}
文件：[frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md)

### 52. dist 构建产物与源文件 commit 历史脱节 {#52-dist-构建产物与源文件-commit-历史脱节}
文件：[frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md)

### 53. i18n JSON 中 key 不得重复 {#53-i18n-json-中-key-不得重复}
文件：[frontend/LESSONS-FRONTEND.md](./frontend/LESSONS-FRONTEND.md)

### 55. el-input-number 列宽截断 {#55-el-input-number-列宽截断}
文件：[frontend/LESSON-55-56.md](./frontend/LESSON-55-56.md)

### 57. 业务关联变更须八层同步 {#57-业务关联变更须八层同步}
文件：[frontend/LESSON-57.md](./frontend/LESSON-57.md)

### 58. el-input-number padding 计算漏扣 {#58-el-input-number-padding-计算漏扣}
文件：[frontend/LESSON-58.md](./frontend/LESSON-58.md)

### 60. nativeQuery + Pageable ORDER BY 陷阱 {#60-nativequery--pageable-order-by-陷阱}
文件：[frontend/LESSON-60.md](./frontend/LESSON-60.md)

### 64. Flyway 部分失败后须 repair 再重跑 {#64-flyway-部分失败后须-repair-再重跑}
文件：[user-service/LESSONS-USER-SERVICE.md](./user-service/LESSONS-USER-SERVICE.md)

### 68. RestTemplate Result\<VO\> 泛型反序列化失效 {#68-resttemplate-resultvo-泛型反序列化失效}
文件：[security/LESSONS-JWT-CROSS-SERVICE.md](./security/LESSONS-JWT-CROSS-SERVICE.md)
> **同名 Lesson 68**：user-service 中指 Lombok annotationProcessorPaths 版本不一致

### 69. JWT RS256 双重 parse 失败 {#69-jwt-rs256-双重-parse-失败}
文件：[security/LESSONS-JWT-CROSS-SERVICE.md](./security/LESSONS-JWT-CROSS-SERVICE.md)
> **同名 Lesson 69**：user-service 中指 BaseEntity setter 须 public

### 74. client.ts Result\<T\> 解包一致性问题 {#74-clientts-resultt-解包一致性问题}
文件：[frontend/LESSON-API-UNWRAP-CONSISTENCY.md](./frontend/LESSON-API-UNWRAP-CONSISTENCY.md)

### 75. JWT 遗漏 permissions + Controller 零注解 {#75-jwt-遗漏-permissions--controller-零注解}
文件：[security/LESSON-75.md](./security/LESSON-75.md)

### 76. allinone 重启流程 {#76-allinone-重启流程}
文件：[backend/LESSON-76.md](./backend/LESSON-76.md)

### 77. COS URL 含 query param 导致预览 404 {#77-cos-url-含-query-param-导致预览-404}
文件：[frontend/LESSON-COS-URL-QUERY-PARAM.md](./frontend/LESSON-COS-URL-QUERY-PARAM.md)

### 80. @EnableAsync 缺失 {#80-enableasync-缺失}
文件：[audit/LESSON-AUDIT-LOG-2026-05-12.md](./audit/LESSON-AUDIT-LOG-2026-05-12.md)

### 81. 审计日志写入错误数据库 {#81-审计日志写入错误数据库}
文件：[audit/LESSON-AUDIT-LOG-2026-05-12.md](./audit/LESSON-AUDIT-LOG-2026-05-12.md)

### 82. UserContext 接口缺失 getUsername {#82-usercontext-接口缺失-getusername}
文件：[audit/LESSON-AUDIT-LOG-2026-05-12.md](./audit/LESSON-AUDIT-LOG-2026-05-12.md)

### 83. @PreAuthorize 内部调用绕过 AOP 代理 {#83-preauthorize-内部调用绕过-aop-代理}
文件：[audit/LESSON-AUDIT-LOG-2026-05-12.md](./audit/LESSON-AUDIT-LOG-2026-05-12.md)

### 85. #_return 对 ResponseEntity/Result\<List\> 静默失效 {#85-return-对-responseentityresultlist-静默失效}
文件：[backend/LESSON-85.md](./backend/LESSON-85.md)

### 86. sanitizeImpl visited 集合误判 DAG 为 cyclic {#86-sanitizeimpl-visited-集合误判-dag-为-cyclic}
文件：[backend/LESSON-86.md](./backend/LESSON-86.md)

### 87. operatorName 始终 null {#87-operatorname-始终-null}
文件：[audit/LESSON-87.md](./audit/LESSON-87.md)

### 88. JPQL 返回枚举字段强转为 String ClassCastException {#88-jpql-返回枚举字段强转为-string-classcastexception}
文件：[backend/LESSON-88.md](./backend/LESSON-88.md)

### 89. Avast SSL 扫描拦截导致腾讯云 COS TLS 握手失败 {#89-avast-ssl-扫描拦截导致腾讯云-cos-tls-握手失败}
文件：[ops/LESSON-89.md](./ops/LESSON-89.md)

---

## 目录结构

```
docs/lessons/
├── backend/        ← Java / Spring / JPA / DDD（L1-6, 10, 25, 29-30, 34, 38, 76, 85-86, 88）
├── ops/            ← 构建 / 部署 / 环境 / 运维（L7, 9, 17-18, 20, 26-28, 89）
├── database/       ← 数据库 / Flyway / Schema（L8, 13, 31-32, 39, 45, 51, 59, 60）
├── frontend/       ← 前端 Vue / TS / i18n / Element Plus（L11-12, 14, 16, 21, 24, 33-34, 37, 40-60, 74, 77）
├── security/       ← JWT / 权限 / 认证（L68-69, 72-73, 75, 78-79）
├── user-service/   ← user-service 专项（L62-71）
├── audit/          ← 审计日志系统专项（L80-84, 87）
├── README.md       ← 本文件（总索引 + 快速搜索）
├── AUDIT-2026-04-27-procurement-demand-table-mismatch.md   ← 审计记录（非教训）
└── BUSINESS-LOGIC-AUDIT-2026-05-07.md   ← 业务逻辑审计记录（非教训）
```

---

## Lesson 来源记录

| Lesson | 来源 |
|--------|------|
| 1–6, 10, 25, 29–30, 34, 38 | Lombok-Decoupling-DI-Lessons.md v54 lessons 拆分 |
| 7–9, 17–18, 20, 26–28 | 构建/部署/运维经验积累 |
| 8, 13, 31–32, 39, 45, 51, 59, 60 | 数据库迁移实战 |
| 11–12, 14, 16, 21, 24, 33–34, 37, 40–54 | 前端 Vue/TS/i18n/Element Plus 实战 |
| 55–56 | 2026-04-27 QcRecordPage el-input-number 列宽截断修复 |
| 57 | 2026-04-27 LogisticsPlan procurementId → qcRecordId 业务锚点修正 |
| 58 | 2026-04-27 LogisticsPlanPage el-input-number 按钮截断宽度计算修正 |
| 60 | 2026-04-28 `/api/v1/orders/chain` nativeQuery + Pageable ORDER BY → Unknown column |
| 62–71 | 2026-04-30 user-service SPEC-B11 Phase 1 实施 |
| 72 | 2026-04-30 allinone JwtKeyManager 跨服务 JWT 401（ParameterizedTypeReference 泛型失效）|
| 73 | 2026-04-30 allinone JwtService.parseToken RS256 双重 parse bug |
| 74 | 2026-04-30 client.ts interceptor 添加 Result\<T\> 解包后 47 处级联失效 |
| 75 | 2026-05-08 allinone JWT 遗漏 permissions + 17个 Controller 零注解 |
| 76 | 2026-05-08 allinone 重启流程（`-q` 掩盖 repackage 失败 + 进程未杀干净）|
| 77 | 2026-05-08 CosService.upload() URL 含 query param → COS 预览 404 |
| 78 | 2026-05-08 权限三角审计：前端/后端/DB 完全一致 |
| 79 | 2026-05-08 JWT 密钥 DB/classpath 来源不一致导致 allinone 401 |
| 80 | 2026-05-12 user-service `@EnableAsync` 缺失导致 `saveAsync` 同步执行 |
| 81 | 2026-05-12 审计日志写入 `user_service` DB（非 `manpou` DB）|
| 82 | 2026-05-12 `UserContext` 接口缺失 `getUsername()` |
| 83 | 2026-05-12 `@PreAuthorize` 在内部方法调用时绕过 Spring AOP 代理 |
| 84 | 2026-05-12 allinone 未重启导致 DEBUG 日志不生效 |
| 85 | 2026-05-12 `#_return` 对 ResponseEntity/Result\<List\> 静默失效 |
| 86 | 2026-05-12 sanitizeImpl visited 集合误判 DAG 结构为 cyclic |
| 87 | 2026-05-12 JWT 缺少 realName/companyId/departmentId claim → operatorName 始终 null |
| 88 | 2026-05-19 JPQL 枚举字段 (ProductCategory) 返回 Object[] 强转为 String → ClassCastException 500 |
| 89 | 2026-05-19 Avast 企业版 SSL 扫描拦截 → COS SDK PKIX 验证失败，TLS 握手无法建立 |
| 90 | 2026-05-19 `dashboard.timezone.CST/JST` i18n key 缺失 → [intlify] Not found 运行时错误 |
| 91 | 2026-05-19 user-service `ddl-auto:none` + Flyway disabled → `language`/`timezone` 列不存在 → PUT /me 500 |
| 92 | 2026-05-20 `findByMasterCodeAndDeletedIsFalse` 多条结果 → NonUniqueResultException 500 → 改为 findByMasterCodeAndSubCodeIsNullAndDeletedIsFalse |
