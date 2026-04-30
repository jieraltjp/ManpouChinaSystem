package com.manpou.allinone.infrastructure.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manpou.allinone.domain.port.SigningKeyPort;
import com.manpou.common.security.PemParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.security.PublicKey;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 公钥管理器（只读验证模式）。
 *
 * 职责：从 user-service 拉取公钥，本地缓存，提供验签接口。
 * 不持有私钥，不签发 token。
 *
 * 详见 SPEC-B11 §1.5 JWT 跨服务验证架构（方案B）
 *
 * Lesson 68: RestTemplate + ParameterizedTypeReference + 内部类泛型反序列化
 * 可能因类型擦除失败（kid=null → 缓存失效 → 401）。
 * 改用 Map<String,Object> 手动提取字段，消除泛型歧义。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtKeyManager implements SigningKeyPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jwt.verifier.endpoint:http://127.0.0.1:18081/api/v1/auth/keys}")
    private String verifierEndpoint;

    private static final int MAX_CACHE_SIZE = 10;

    private final Map<String, CachedPublicKey> cache = new ConcurrentHashMap<>();

    /**
     * 启动时预热缓存（加载当前活跃公钥）。
     */
    @PostConstruct
    public void init() {
        refreshActiveKey();
        log.info("JwtKeyManager initialized, verifier endpoint={}", verifierEndpoint);
    }

    /**
     * 定时刷新（每 refreshIntervalSeconds 秒）。
     */
    @Scheduled(fixedRateString = "${jwt.verifier.refresh-interval-seconds:300}000")
    public void scheduledRefresh() {
        refreshActiveKey();
    }

    /**
     * 刷新当前活跃公钥（热加载）。
     */
    public void refreshActiveKey() {
        try {
            String url = verifierEndpoint + "/active/public-key";
            Map<String, Object> data = fetchKeyData(url);
            if (data != null) {
                String kid = Objects.toString(data.get("kid"), null);
                String pem = Objects.toString(data.get("publicKey"), null);
                cacheKey(kid, pem);
                log.info("Refreshed active public key: kid={}", kid);
            }
        } catch (Exception ex) {
            log.warn("Failed to refresh active public key: {}", ex.getMessage());
        }
    }

    /**
     * 根据 kid 获取公钥（先查缓存，未命中则从 user-service 拉取）。
     */
    public PublicKey getPublicKey(String kid) {
        CachedPublicKey cached = cache.get(kid);
        if (cached != null && !cached.isExpired(Duration.ofMinutes(5))) {
            return cached.publicKey;
        }

        try {
            String url = verifierEndpoint + "/" + kid + "/public-key";
            Map<String, Object> data = fetchKeyData(url);
            if (data != null) {
                String fetchedKid = Objects.toString(data.get("kid"), null);
                String pem = Objects.toString(data.get("publicKey"), null);
                cacheKey(fetchedKid, pem);
                return PemParser.parsePublicKey(pem);
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch public key for kid={}: {}", kid, ex.getMessage());
            // 缓存过期但拉取失败，降级使用缓存
            if (cached != null) {
                return cached.publicKey;
            }
        }

        throw new SecurityException("Public key not found: kid=" + kid);
    }

    /**
     * 当前 kid（从缓存获取，无则返回 null）。
     */
    public String getCurrentKid() {
        return cache.keySet().stream().findFirst().orElse(null);
    }

    @Override
    public void reloadActiveKey() {
        refreshActiveKey();
    }

    @Override
    public String getActivePublicKeyPem() {
        CachedPublicKey cached = cache.values().stream().findFirst().orElse(null);
        return cached != null ? cached.pem : null;
    }

    // ===== Map-based JSON 拉取（避免泛型类型擦除导致反序列化失败） =====

    /**
     * 从 user-service 拉取公钥数据，解析为 Map。
     * user-service 返回格式：{ "code": "ok", "data": { "kid": "...", "publicKey": "..." } }
     */
    private Map<String, Object> fetchKeyData(String url) throws java.io.IOException {
        String json = restTemplate.getForObject(url, String.class);
        if (json == null) {
            return null;
        }
        Map<String, Object> root = objectMapper.readValue(json,
                new TypeReference<Map<String, Object>>() {});
        // Result<T> 的 payload 在 JSON 中序列化为 "data"
        Object payload = root.get("data");
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload;
            return data;
        }
        return null;
    }

    // ===== 缓存管理 =====

    private void cacheKey(String kid, String pem) {
        if (kid == null || pem == null) {
            return;
        }
        if (cache.size() >= MAX_CACHE_SIZE && !cache.containsKey(kid)) {
            // LRU 淘汰：移除最旧的 entry
            String oldest = cache.entrySet().stream()
                .min(Map.Entry.comparingByValue((a, b) -> Long.compare(a.cachedAt, b.cachedAt)))
                .map(Map.Entry::getKey)
                .orElse(kid);
            cache.remove(oldest);
        }
        try {
            cache.put(kid, new CachedPublicKey(PemParser.parsePublicKey(pem), pem, System.currentTimeMillis()));
        } catch (Exception ex) {
            log.error("Failed to parse public key for kid={}: {}", kid, ex.getMessage());
        }
    }

    // ===== 内部类 =====

    private static class CachedPublicKey {
        final PublicKey publicKey;
        final String pem;
        final long cachedAt;

        CachedPublicKey(PublicKey publicKey, String pem, long cachedAt) {
            this.publicKey = publicKey;
            this.pem = pem;
            this.cachedAt = cachedAt;
        }

        boolean isExpired(Duration ttl) {
            return System.currentTimeMillis() - cachedAt > ttl.toMillis();
        }
    }
}
