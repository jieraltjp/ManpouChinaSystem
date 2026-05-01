package com.manpou.allinone.common.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "cos")
@Getter
public class CosConfig {

    private boolean enabled = false;
    private String secretId = "";
    private String secretKey = "";
    private String bucket = "manpou-1324246219";
    private String region = "ap-tokyo";
    private String domain = "https://manpou-1324246219.cos.ap-tokyo.myqcloud.com";
    private String prefix = "qc-images/";
    private long maxFileSize = 5 * 1024 * 1024; // 5MB

    public String getBucketHost() {
        return bucket + ".cos." + region + ".myqcloud.com";
    }

    /**
     * 生成带日期路径前缀的 COS 对象 key。
     * 例如：qc-images/2026/05/01/uuid-filename.jpg
     */
    public String generateKey(String originalFilename) {
        java.time.LocalDate now = java.time.LocalDate.now();
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        String ext = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            ext = originalFilename.substring(dotIdx);
        }
        return String.format("%s%d/%02d/%02d/%s%s",
                prefix, now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                uuid, ext);
    }

    @Bean
    @ConditionalOnProperty(name = "cos.enabled", havingValue = "true")
    public RestTemplate cosRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        log.info("[COS] RestTemplate initialized, bucket={}, region={}", bucket, region);
        return new RestTemplate(factory);
    }

    /**
     * 生成 COS 临时凭证签名（用于前端直传，或后端调用）。
     * 使用 HMAC-SHA1 + Base64。
     */
    public String generateAuthSign(long expiredSeconds) {
        long currentTime = System.currentTimeMillis() / 1000;
        long expired = currentTime + expiredSeconds;

        String signTime = currentTime + ";" + expired;
        String httpString = "PUT\n/\n\n" + expired + "\n";
        String stringToSign = "sha1\n" + signTime + "\n" +
                sha1(httpString) + "\n";

        String signature = hmacSha1(stringToSign, secretKey);

        // 构造签名参数
        String keyTime = signTime;
        String qSignAlgorithm = "sha1";

        // 简单的签名（简化版，适用于后端直传）
        String sign = "q-sign-algorithm=sha1&q-ak=" + secretId +
                "&q-sign-time=" + signTime +
                "&q-key-time=" + keyTime +
                "&q-signature=" + signature;

        return sign;
    }

    private static String sha1(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA1");
            byte[] digest = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA1 error", e);
        }
    }

    private static String hmacSha1(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] raw = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA1 error", e);
        }
    }
}
