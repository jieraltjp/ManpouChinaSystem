# 工程教训 — JWT 跨服务验签与 user-service 实施

> 项目：ManpouChinaSystem
> 覆盖范围：JWT 跨服务 Plan B 架构 / allinone 只读验签 / user-service 签发
> Lesson 编号：68–69（共 2 条，新增）

---

## Lesson 68: RestTemplate + ParameterizedTypeReference + 内部类泛型反序列化失效

### 问题

allinone 返回 401 Unauthorized，但 user-service 签发的 JWT 在 user-service 端自验签正常。

日志显示：`refreshActiveKey()` 执行时 cache 存入 `kid=null`，导致后续 `getPublicKey("7882ef00")` 缓存未命中 → HTTP 拉取失败 → SecurityException。

### 根因

**泛型类型擦除导致 `Result<PublicKeyVO>` 反序列化失效。**

`allinone` 的 `JwtKeyManager` 使用：
```java
restTemplate.exchange(url,
    HttpMethod.GET, null,
    new ParameterizedTypeReference<Result<PublicKeyVO>>() {});
```

其中 `Result<T>` 来自 `manpou-common`，`PublicKeyVO` 是 `JwtKeyManager` 的内部类。Jackson 在反序列化时遇到嵌套内部类类型推断链断裂，`vo.kid` 和 `vo.publicKey` 均为 `null`。

### 修复

改用 Map 中间层：
```java
String json = restTemplate.getForObject(url, String.class);
Map<String, Object> root = objectMapper.readValue(json,
    new TypeReference<Map<String, Object>>() {});
Object payload = root.get("data");
Map<String, Object> data = (Map<String, Object>) payload;
String kid = String.valueOf(data.get("kid"));
String pem = String.valueOf(data.get("publicKey"));
```

---

## Lesson 69: JWT RS256 验签必须在有公钥后才能 parse——allinone JwtService.parseToken 双重 parse bug

### 问题

登录获取 JWT 后，后续 allinone 的业务 API 返回 401 Unauthorized。

日志显示：`JwtService.parseToken` 抛出异常（RS256 token 在没有公钥的情况下尝试 parse 失败）。

### 根因

**`parseToken` 在没有公钥的情况下尝试解析 RS256 token——两次 `Jwts.parser()` 调用，第二次才能验签。**

错误代码：
```java
// ❌ 第一次 parseSignedClaims() 需要验签，但没有公钥 → 抛异常
String kid = Jwts.parser()
    .build()
    .parseSignedClaims(token)       // RS256 需要公钥！会抛 JwtException
    .getHeader()
    .getKeyId();

return Jwts.parser()
    .verifyWith(keyManager.getPublicKey(kid)) // 此时已无法到达
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

RS256 token 的 header 部分是 base64url 编码，可以无需签名验证直接解码。kid 就藏在 header 的 `kid` 字段中。

### 修复

直接从 base64url 解码提取 kid：
```java
private Claims parseToken(String token) {
    String kid = extractKidFromHeader(token); // 无需验签，直接解码 header
    return Jwts.parser()
        .verifyWith(keyManager.getPublicKey(kid))
        .build()
        .parseSignedClaims(token)
        .getPayload();
}

private static String extractKidFromHeader(String token) {
    String[] parts = token.split("\\.");
    String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
    @SuppressWarnings("unchecked")
    Map<String, Object> header = new ObjectMapper().readValue(headerJson, Map.class);
    return header.get("kid").toString();
}
```

### 预防

| 场景 | 规范 |
|------|------|
| RS256 token 提取 kid | 必须从 header base64url 解码，禁止先验签再提取 |
| JWT parser 调用 | 任何不提供 key 的 `parseSignedClaims()` 都会失败（RS256） |

---

## 铁律总结表（JWT 跨服务）

| # | 铁律 | 违反后果 |
|---|------|---------|
| 68 | RestTemplate 反序列化 `Result<内部类VO>` 须用 Map 中间层 | kid=null，缓存失效，401 |
| 69 | RS256 JWT 提取 kid 必须从 header base64url 解码，禁止先验签 | RS256 无公钥 parse 抛异常，401 |

