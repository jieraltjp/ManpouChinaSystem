# Lesson 89: Avast SSL 扫描拦截导致腾讯云 COS SDK TLS 握手失败

> **发现日期**: 2026-05-19
> **项目**: ManpouChinaSystem
> **教训**: 企业防火墙/杀毒软件做 SSL 解密检查时，会替换服务器证书链，导致 JVM 无法验证

---

## 问题

COS 上传接口返回 500：

```
COS 上传失败: (certificate_unknown) PKIX path building failed:
  sun.security.provider.certpath.SunCertPathBuilderException:
  unable to find valid certification path to requested target
```

关闭 Avast 10 分钟后上传恢复正常。

---

## 根因

**Avast 企业版 SSL/TLS 扫描**对出站 HTTPS 连接做中间人解密：

```
浏览器/JVM → Avast SSL Scanner → 腾讯云 COS
                  ↓
         Avast 签发拦截证书替换原证书链
```

`openssl s_client` 验证：

```
subject=*.cos.ap-tokyo.myqcloud.com
issuer=Avast Web/Mail Shield Root
Verify return code: 21 (unable to verify the first certificate)
```

JVM 的默认 trust store 不含 Avast 根证书，导致 PKIX 验证失败。

---

## 验证方法

```bash
# 1. 检查服务器 SSL 证书链
openssl s_client -connect cos.ap-tokyo.myqcloud.com:443 2>/dev/null \
  | grep -E "subject|issuer|Verify return code"

# 2. 检查 JDK 是否能连接
curl -v https://manpou-1324246219.cos.ap-tokyo.myqcloud.com/
# 若报错 "unable to find valid certification path" → Avast 拦截
```

---

## 解决方案

### 方案 A：Avast 例外名单（推荐）

在 Avast 管理控制台，将以下域名加入 **Web 防护例外**：

```
*.myqcloud.com
*.cos.*.myqcloud.com
```

优点：保持 Avast 安全能力，仅放过可信云存储域名。

### 方案 B：禁用 Avast SSL 扫描（临时）

开发调试时临时关闭 Avast Web 防护。

### 方案 C：导入 Avast 根证书到 JDK（需管理员权限）

```powershell
# 导出 Avast 根证书
Get-ChildItem Cert:\LocalMachine\Root | Where-Object { $_.Subject -like "*Avast*" }
  | Export-Certificate -FilePath C:\tmp\avast-root.crt

# 导入到 JDK trust store（需要管理员权限）
keytool -importcert -trustcacerts -alias AvastRoot \
  -file C:\tmp\avast-root.crt \
  -keystore "$env:JAVA_HOME\lib\security\cacerts" \
  -storepass changeit -noprompt
```

### 方案 D：COS SDK 配置自定义 TrustManager（仅开发）

在 `CosConfig` 中为 COS 连接配置忽略 SSL 验证的 TrustManager（**仅开发环境，生产禁用**）：

```java
// ⚠️ 仅开发环境，生产必须使用方案 A
TrustManager[] trustAll = new TrustManager[]{
    new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() { return null; }
        public void checkClientTrusted(X509Certificate[] c, String a) {}
        public void checkServerTrusted(X509Certificate[] c, String a) {}
    }
};
SSLContext sc = SSLContext.getInstance("TLS");
sc.init(null, trustAll, new java.security.SecureRandom());
// 配置到 COS ClientConfig...
```

---

## 预防

| 场景 | 检查项 |
|------|--------|
| 新增第三方 HTTP 调用 | 用 `curl -v` 先验证 TLS 连通性 |
| JDK 升级/更换 | 重新验证 SSL 连接 |
| 网络环境变更 | 确认代理/防火墙不影响 HTTPS |

## 关联 Lesson

- Lesson 77: COS URL 含 query param 导致预览 404
