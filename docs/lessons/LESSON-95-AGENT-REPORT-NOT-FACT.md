# LESSON-95 Agent 探索报告不等于代码事实

## 问题

使用 Agent 进行"探索"时，Agent 的报告可能包含**虚构内容**——代码中不存在但 Agent 凭训练记忆生成的描述。

**本次案例：** Agent 探索报告称 `manpou-allinone` 有完善的 `logback-spring.xml`，实际上该文件**完全不存在**，日志配置仅靠 `application.yml` 的 `logging:` Spring Boot 属性。

## 根因

Agent 在训练数据中见过大量 Spring Boot 项目配置模式，生成了"符合预期"的描述，而非读取实际文件。

## 正确做法

| 步骤 | 操作 | 工具 |
|------|------|------|
| 1 | 验证文件存在性 | `Glob` / `ls` |
| 2 | 读取实际内容 | `Read` / `cat` |
| 3 | 交叉验证关键声明 | `Grep` 全局搜索 |

## 验证方法

```bash
# 检查 logback 文件是否真实存在
ls apps/manpou-allinone/src/main/resources/
# 预期：无 logback-spring.xml

# 在父 POM 中搜索 logstash 依赖（跨服务验证）
grep -r "logstash" apps/java-service/pom.xml
```

## 教训

**Agent 探索报告仅作为方向性参考，不等于代码事实。** 关键配置变更（如 logback）必须直接读取文件验证后才能修改。

## 预防

探索报告中的每个"现状描述"，实施前必须用 Glob/Read/Bash 交叉验证文件存在性和内容准确性。
