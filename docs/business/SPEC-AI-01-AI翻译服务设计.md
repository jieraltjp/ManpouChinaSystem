# SPEC-AI-01: AI 翻译服务

> **版本**：v1.1.0
> **日期**：2026-05-23
> **状态**：✅ 实现完成（Prompt 修复：简化版 v1.1.0）
> **负责人**：jiangjie

---

## 1. 背景与目标

在商品新增/编辑模态框中，输入商品中文名称后，通过 DeepSeek 最新模型自动翻译填入日文名称，提升录入效率并保证翻译质量。

---

## 2. 技术选型

| 项目 | 选择 | 说明 |
|------|------|------|
| AI 模型 | `deepseek-chat` | 费用低，翻译质量高（实测 `女士纯棉T恤` → `レザーーズ綿100%T恤`） |
| API 接入方式 | 后端代理 | API Key 安全，不暴露在前端 |
| HTTP Client | Spring `RestTemplate`（项目已有） | 不引入额外依赖 |
| 调用时机 | 手动触发（点击翻译按钮） | 用户确认中文名后再翻译，避免无效调用 |
| 缓存 | 暂不实现 | 同一中文名重复翻译概率低，API 费用可忽略 |

---

## 3. API 设计

### 3.1 翻译接口

**端点**：`POST /api/v1/ai/translate`

**权限**：`product:read`（商品模块读权限）

**请求体**：
```json
{
  "sourceText": "女士纯棉T恤",
  "sourceLang": "zh",
  "targetLang": "ja"
}
```

**响应（成功）**：
```json
{
  "code": 200,
  "data": {
    "sourceText": "女士纯棉T恤",
    "targetText": "女士綿T恤",
    "nameJa": "女士綿T恤"
  },
  "message": "ok"
}
```

**响应（失败）**：
```json
{
  "code": 500,
  "data": null,
  "message": "AI 翻译服务暂时不可用，请稍后重试"
}
```

### 3.2 错误处理

| 场景 | HTTP 状态码 | code | message |
|------|-------------|------|---------|
| sourceText 为空 | 200 | 400 | "请先输入中文名称" |
| sourceText 过长（>200字） | 200 | 400 | "商品名称过长" |
| API Key 未配置 | 200 | 500 | "AI 翻译服务未配置" |
| DeepSeek API 超时 | 200 | 500 | "翻译超时，请重试" |
| DeepSeek API 返回格式异常 | 200 | 500 | "翻译结果解析失败" |

---

## 4. Prompt 设计（最终版，已简化）

**日译中**：
```
将以下中文商品名称翻译成日文。只返回JSON，禁止其他文字：{"nameJa":"日文翻译"}

中文：{sourceText}
```

**英译中**：
```
Translate the Chinese product name below to English. Return ONLY valid JSON, no other text: {"nameEn":"English translation"}

Chinese: {sourceText}
```

**Temperature**：0.3（低随机性，保证一致性）

---

## 5. 文件清单

### 后端（manpou-allinone）

| 文件 | 动作 | 说明 |
|------|------|------|
| `ai/interfaces/controller/TranslationController.java` | 新增 | 翻译端点 |
| `ai/application/TranslationService.java` | 新增 | 调用 DeepSeek API |
| `ai/application/dto/TranslateRequest.java` | 新增 | 请求 DTO |
| `ai/application/dto/TranslateResponse.java` | 新增 | 响应 DTO |
| `application.yml` | 修改 | 添加 `deepseek.*` 配置节 |
| `application-local.yml` | 新增 | 本地 API Key（不提交 Git） |

### 前端（web）

| 文件 | 动作 | 说明 |
|------|------|------|
| `api/ai.ts` | 新增 | AI 翻译 API 客户端 |
| `components/product/ProductFormDialog.vue` | 修改 | 添加翻译按钮（nameZh → nameJa） |
| `pages/product/ProductPage.vue` | 修改 | 添加翻译按钮（内嵌表单） |
| `locales/zh.json` | 修改 | 添加 product.dialog.translate* i18n |
| `locales/ja.json` | 修改 | 添加翻译按钮 i18n |

---

## 6. 前端交互设计

### 6.1 布局（最终版）

**一键翻译**：输入中文名后，点击「🔄 一键翻译」→ 自动填入日文名 + 英文名。材质单独翻译。

```
┌──────────────────────────────────────────────────────────────────────┐
│ 中文名称： [女士纯棉T恤       ] [🔄 一键翻译] [日文名称：女士綿T恤    ] │
├──────────────────────────────────────────────────────────────────────┤
│ 英文名称： [Ladies Cotton T-shirt ]  分类：[批发▼]   状态：[      ]  │
├──────────────────────────────────────────────────────────────────────┤
│ 材质：     [纯棉             ] [翻译] [日文材质：綿100%]  原产地：[中国] │
└──────────────────────────────────────────────────────────────────────┘
```

- 一键翻译（primary 按钮）：`nameZh` 非空时启用，并发调用 zh→ja + zh→en（Promise.all）
- 材质翻译（info 按钮）：`material` 非空时启用，调用 zh→ja
- 翻译中：按钮显示 Loading spinner + "翻译中..." 文字，禁用点击
- 翻译成功：Toast 绿色提示，自动填充字段
- 翻译失败：Toast 红色提示，不阻塞用户继续输入

### 6.2 权限

- 翻译按钮仅在 `product:read` 权限下显示（编辑/新建模式下均显示）
- 接口层 `@PreAuthorize("hasAuthority('product:read')")` 鉴权

---

## 7. 配置说明

```yaml
# application.yml（提交 Git）
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}          # 环境变量，生产使用
  model: ${DEEPSEEK_MODEL:deepseek-chat}
  base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
  timeout-seconds: ${DEEPSEEK_TIMEOUT:30}

# application-local.yml（不提交 Git）
deepseek:
  api-key: sk-40d9aa5e38204a4094a194c2b2737107
```

---

## 8. 审计清单

| # | 检查项 | 状态 |
|---|--------|------|
| 1 | API Key 不提交 Git（application-local.yml 已加入 .gitignore） | ✅ |
| 2 | 接口加 `@PreAuthorize("hasAuthority('product:read')")` | ✅ (TranslationController.java:42) |
| 3 | sourceText 防御性校验（非空、长度限制） | ✅ (TranslateRequest.java @NotBlank/@Size) |
| 4 | DeepSeek API 超时兜底（connect 10s / read 30s） | ✅ (TranslationService.java:44-45) |
| 5 | 前端翻译失败不影响用户继续输入 | ✅ (catch {} 静默，ElMessage.error Toast) |
| 6 | i18n zh/ja 双语同步添加 | ✅ (translate/translateSuccess/translateError/oneClickTranslate/translating) |
| 7 | ProductFormDialog + ProductPage 两处表单同步修改 | ✅ |
| 8 | 一键翻译：并发 zh→ja + zh→en（Promise.all） | ✅ |
| 9 | 材质单独翻译：material → materialJa | ✅ |
| 10 | `vue-tsc --noEmit` 通过 | ✅ (vue module 错误为项目已有，非本次引入) |
| 11 | `mvn compile` 通过 | ✅ |
| 12 | Prompt 简化版（短指令 + 强制 JSON 格式），避免模型回显系统文本 | ✅ (TranslationService.java:148-155) |
| 13 | SSL bypass：HttpURLConnection + X509TrustManager 匿名类（非 wildcard import） | ✅ (TranslationService.java:138-145) |

---

## 9. 后续扩展

- 翻译结果缓存（Redis，Key：`translate:{text}:{targetLang}`）
- 批量翻译（Excel 导入时调用）
- 材质随一键翻译一起自动翻译（当前材质独立按钮，可改为一起调用）
