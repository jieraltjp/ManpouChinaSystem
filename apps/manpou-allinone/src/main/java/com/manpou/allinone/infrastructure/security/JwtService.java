package com.manpou.allinone.infrastructure.security;

import com.manpou.allinone.common.context.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT Token 服务（只读验证模式）。
 *
 * 仅验证 Token，不签发（签发由 user-service 负责）。
 * 从 token header 的 kid 定位公钥。
 *
 * 详见 SPEC-B11 §1.5 JWT 跨服务验证架构（方案B）
 */
@Slf4j
@Component
public class JwtService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_USERNAME = "username";

    private final JwtKeyManager keyManager;

    @Value("${jwt.access-token-ttl-seconds:900}")
    private long accessTokenTtlSeconds;

    public JwtService(JwtKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    /**
     * 验证 Token 有效性（从 token header 提取 kid，定位公钥验签）。
     * allinone 只读验证，不签发 token。
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * 提取 Token Claims。
     */
    public JwtClaims extractClaims(String token) {
        Claims claims = parseToken(token);
        return new JwtClaims(
            claims.getSubject(),
            claims.get(CLAIM_USERNAME, String.class),
            castStringList(claims.get(CLAIM_ROLES)),
            castStringList(claims.get(CLAIM_PERMISSIONS)),
            claims.get(CLAIM_TENANT_ID, String.class),
            claims.getIssuedAt().toInstant().getEpochSecond(),
            claims.getExpiration().toInstant().getEpochSecond()
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> castStringList(Object o) {
        return (List<String>) o;
    }

    private Claims parseToken(String token) {
        // RS256: header 是 base64url 编码，可直接解码提取 kid（无需验签）
        String kid = extractKidFromHeader(token);
        return Jwts.parser()
            .verifyWith(keyManager.getPublicKey(kid))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * 从 JWT token 直接解码 header，提取 kid。
     * RS256 token header 不需要签名验证即可解码（JWT 标准）。
     */
    private static String extractKidFromHeader(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 1) {
                throw new SecurityException("Invalid JWT format");
            }
            String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
            @SuppressWarnings("unchecked")
            Map<String, Object> header = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(headerJson, java.util.Map.class);
            Object kidValue = header.get("kid");
            if (kidValue == null) {
                throw new SecurityException("JWT header missing 'kid' field");
            }
            return kidValue.toString();
        } catch (Exception ex) {
            throw new SecurityException("Failed to extract kid from JWT header", ex);
        }
    }

    // ==================== JwtClaims — 领域 UserContext 实现 ====================

    /**
     * JWT Claims 值对象（Java record）。
     * 实现 {@link UserContext}，允许 Infrastructure 层存储到 ContextHolder。
     */
    public record JwtClaims(
        String userId,
        String username,
        List<String> roles,
        List<String> permissions,
        String tenantId,
        long iat,
        long exp
    ) implements UserContext {

        @Override
        public String getUserId() {
            return userId;
        }

        @Override
        public String getTenantId() {
            return tenantId;
        }

        @Override
        public String getUsername() {
            return username;
        }
    }
}
