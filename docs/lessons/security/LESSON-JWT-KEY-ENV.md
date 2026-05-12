# Lesson: JWT 密钥来源不一致导致 allinone 401

## 根因

user-service 签发 JWT 用 classpath `keys/private.pem`，但 `signing_key` 表存了**另一套**公钥。
allinone 从 user-service API 拉取公钥（来自 DB），与签发私钥不匹配 → 签名不匹配 → 401。

## 关键证据

```
# DB 存的公钥 MD5: c9aceec69edfa60ae48a30004e11fc3d
# classpath 公钥 MD5: 4e6cb7e600d6528a941f29dff8b66ea3
# classpath 私钥 MD5: 88925a5eeb42605c71a986d6be55f13f
# → 三者 MD5 各不同，DB 和 classpath 是两套密钥对
```

## 修复

改用环境变量加载同一套密钥：

**`application-local.yml`**（已在 .gitignore）:
```yaml
jwt:
  key:
    kid: e57043d2
    private: |
      -----BEGIN PRIVATE KEY-----
      ...（PEM 内容）...
      -----END PRIVATE KEY-----
    public: |
      -----BEGIN PUBLIC KEY-----
      ...（PEM 内容）...
      -----END PUBLIC KEY-----
```

**`JwtKeyManager`**：从 `jwt.key.private` / `jwt.key.public` 加载，classpath 仅作 fallback。

## 铁律

| # | 规则 | 违反后果 |
|---|------|---------|
| 79 | JWT 跨服务验签时，密钥必须从**同一来源**加载（环境变量 / DB 二选一），禁止 classpath + DB 混用 | 签名不匹配 401 |

## 密钥轮换流程

更新 `application-local.yml` 中的 PEM 内容 → 重启 user-service + allinone → allinone 自动热加载新公钥。

## 预防

`JwtKeyManager.init()` 启动时打印加载来源：
```
JwtKeyManager initialized, kid=e57043d2, source=env
```
