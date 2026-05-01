package com.manpou.allinone.common.service;

import com.manpou.allinone.common.config.CosConfig;
import com.manpou.allinone.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;

/**
 * 腾讯云 COS 对象存储服务（REST API 方式，无 SDK 依赖）。
 *
 * COS 签名算法参考：
 * https://cloud.tencent.com/document/product/436/7778
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CosService {

    private final CosConfig cosConfig;
    private final RestTemplate cosRestTemplate;

    /**
     * 上传文件到 COS，返回完整访问 URL。
     */
    public String upload(MultipartFile file) {
        String key = cosConfig.generateKey(file.getOriginalFilename());
        String host = cosConfig.getBucketHost();
        String url = "https://" + host + "/" + key;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType(file.getOriginalFilename()));
        headers.set("Host", host);

        // COS 签名（HMAC-SHA1）
        String auth = makeAuthToken("PUT", "/" + key, file.getSize(), file.getContentType());
        headers.set("Authorization", auth);
        headers.set("x-cos-security-token", ""); // V5 风格不需要 token

        HttpEntity<byte[]> request;
        try {
            request = new HttpEntity<>(file.getBytes(), headers);
        } catch (IOException e) {
            throw BusinessException.internal("文件读取失败: " + e.getMessage());
        }

        try {
            ResponseEntity<String> resp = cosRestTemplate.exchange(
                    url, HttpMethod.PUT, request, String.class);
            log.info("[COS] Uploaded, key={}, status={}", key, resp.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.error("[COS] Upload failed, key={}, status={}, body={}",
                    key, e.getStatusCode(), e.getResponseBodyAsString());
            throw BusinessException.internal("COS 上传失败: " + e.getStatusCode());
        }

        return cosConfig.getDomain() + "/" + key;
    }

    /**
     * 删除 COS 对象。
     */
    public void delete(String url) {
        String key = extractKey(url);
        if (key == null || key.isBlank()) {
            log.warn("[COS] Cannot extract key from url={}", url);
            return;
        }
        String host = cosConfig.getBucketHost();
        String fullUrl = "https://" + host + "/" + key;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Host", host);
        String auth = makeAuthToken("DELETE", "/" + key, 0, null);
        headers.set("Authorization", auth);

        try {
            ResponseEntity<String> resp = cosRestTemplate.exchange(
                    fullUrl, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            log.info("[COS] Deleted, key={}, status={}", key, resp.getStatusCode());
        } catch (HttpClientErrorException e) {
            // 404 也算成功（幂等）
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("[COS] Delete skip (not found), key={}", key);
                return;
            }
            log.error("[COS] Delete failed, key={}, status={}", key, e.getStatusCode());
            throw BusinessException.internal("COS 删除失败: " + e.getStatusCode());
        }
    }

    /**
     * 从完整 URL 中提取 COS key。
     */
    private String extractKey(String url) {
        if (url == null) return null;
        String domain = cosConfig.getDomain();
        if (domain != null && !domain.isBlank() && url.startsWith(domain)) {
            return url.substring(domain.length() + 1);
        }
        if (url.startsWith("https://" + cosConfig.getBucketHost())) {
            return url.substring(("https://" + cosConfig.getBucketHost()).length() + 1);
        }
        return url;
    }

    /**
     * 生成 COS 签名（简化版 HMAC-SHA1）。
     *
     * COS 签名格式（V5）：
     * q-sign-algorithm=sha1
     * q-ak=<SecretId>
     * q-sign-time=<signtime>
     * q-key-time=<keytime>
     * q-signature=<signature>
     */
    private String makeAuthToken(String method, String path, long contentLength, String contentType) {
        long now = Instant.now().getEpochSecond();
        long expired = now + 3600; // 1小时有效期

        String signTime = now + ";" + expired;
        String keyTime = signTime;

        // 关键字符串（Canonical Request）
        // HTTPMethod + "\n" + CanonicalURI + "\n" + CanonicalQueryString + "\n" + CanonicalHeaders + "\n" + SignedHeaders + "\n" + HashedCanonicalResource
        String httpString = method + "\n" + path + "\n\n\n" + expired + "\n";
        String stringToSign = "sha1\n" + signTime + "\n" + sha1(httpString) + "\n";
        String signature = hmacSha1(stringToSign, cosConfig.getSecretKey());

        return "q-sign-algorithm=sha1" +
                "&q-ak=" + cosConfig.getSecretId() +
                "&q-sign-time=" + signTime +
                "&q-key-time=" + keyTime +
                "&q-signature=" + signature;
    }

    private static String sha1(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA1");
            byte[] digest = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hmacSha1(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] raw = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static org.springframework.http.MediaType getMediaType(String filename) {
        if (filename == null) return MediaType.APPLICATION_OCTET_STREAM;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
