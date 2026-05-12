---
name: LESSON-COS-URL-QUERY-PARAM
description: CosService.upload() 返回 URL 带 query param，但提取方法未同步剥离，导致 COS key 不合法
type: reference
---

# Lesson: COS URL Query Param 导致图片预览 404

## 事故现象

QC 验货记录图片，一部分能预览，一部分报 404。

**对比：**

| ID | filename | URL |
|----|----------|-----|
| 1 (正常) | `ed6ae5847dcd49ad9b088cee0d857e50.jpg` | ✅ 干净 |
| 4 (异常) | `66f3f6cd29ab4621ac0acc17e23bccb2.jpg?response-content-disposition=inline` | ❌ 含 query string |

## 根因

`commit 8b306f3` 在 `CosService.upload()` 中追加 `?response-content-disposition=inline`：

```java
// 改前
return cosConfig.getDomain() + "/" + key;

// 改后（引入 bug）
String url = cosConfig.getDomain() + "/" + key + "?response-content-disposition=inline";
return url;
```

`extractKey()` 补了 query stripping，但 `extractFilename()`（在 `QcImageController` 中是独立复制的方法）**漏改**。

导致存入 DB 的 `url` 字段含 query string，前端 `<img src="脏key">` → COS SDK 解析 key 时把 `?xxx` 当作 key 的一部分 → 对象不存在 → 404。

## 手术（已实施）

### 1. `CosService.upload()` 返回干净 key
```java
// 防腐层：返回干净 key，由调用方决定是否追加 query param
return key;
```

### 2. `QcImageController` 注入 `CosConfig`，新增 `buildDisplayUrl()`
```java
private final CosConfig cosConfig;

// 存储: filename=干净key, url=展示URL（含 query param）
// 返回客户端: url=展示URL
return Result.ok(new ImageUploadResult(buildDisplayUrl(key), image.getFilename(), file.getSize()));
```

### 3. `extractFilename()` 补 query stripping（防腐兜底）
```java
private String extractFilename(String url) {
    if (url == null) return null;
    int q = url.indexOf('?');
    if (q >= 0) url = url.substring(0, q);  // ← 新增
    int lastSlash = url.lastIndexOf('/');
    return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
}
```

## 教训

| 违反 | 说明 |
|------|------|
| 防腐层 | 同一语义修改分散在多处，必须全局搜索同步 |
| 正交性 | `upload()` 改变返回值语义（干净 key → 拼接 URL），调用方必须同步更新 |
| 溯源 | commit message 应注明"破坏性 API 变更，需同步修改调用方" |

## 预防

- 改返回值语义 → 必须 grep 全项目找所有调用点
- query string 只用于展示，存储层永远用干净 key
