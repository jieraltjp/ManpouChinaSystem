package com.manpou.finance.infrastructure.security;

import com.manpou.finance.common.context.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT Token 服务（RS256）。
 *
 * 使用 RSA-SHA256 非对称签名：
 * - 私钥：服务端签发 Token
 * - 公钥：其他服务 / 前端验签
 *
 * 详见 docs/core/10-认证授权与权限模型.md §2.2
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
     * 签发 Access Token（RS256）。
     *
     * @param kid 密钥 ID（写入 JWT header "kid"，供验签方定位公钥）
     */
    public String generateAccessToken(String userId, String username,
                                      List<String> roles, List<String> permissions,
                                      String tenantId, String kid) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId)
            .header().add("kid", kid).and()
            .claim(CLAIM_USERNAME, username)
            .claim(CLAIM_ROLES, roles)
            .claim(CLAIM_PERMISSIONS, permissions)
            .claim(CLAIM_TENANT_ID, tenantId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
            .signWith(keyManager.getPrivateKey(), Jwts.SIG.RS256)
            .compact();
    }

    /**
     * 验证 Token 有效性（RS256 公钥验签）。
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
            claims.get(CLAIM_ROLES, List.class),
            claims.get(CLAIM_PERMISSIONS, List.class),
            claims.get(CLAIM_TENANT_ID, String.class),
            claims.getIssuedAt().toInstant().getEpochSecond(),
            claims.getExpiration().toInstant().getEpochSecond()
        );
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(keyManager.getPublicKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
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
    }
}
