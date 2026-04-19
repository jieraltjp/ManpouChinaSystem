package com.company.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JWT RS256 验签服务。
 *
 * 仅负责验签 + 提取 Claims，不执行业务逻辑。
 * 角色/权限校验由 SecurityContext 或 Filter 链完成。
 */
@Slf4j
@Component
public class JwtValidator {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_USERNAME = "username";

    private final JwtPublicKeyManager keyManager;

    public JwtValidator(JwtPublicKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    /**
     * 验签并提取 Claims。
     *
     * @param token Bearer token（去掉 "Bearer " 前缀）
     * @return Claims，验签失败返回 null
     */
    @SuppressWarnings("unchecked")
    public JwtClaims extractClaims(String token) {
        try {
            Claims claims = parseToken(token);
            return new JwtClaims(
                claims.getSubject(),
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_ROLES, List.class),
                claims.get(CLAIM_PERMISSIONS, List.class),
                claims.get(CLAIM_TENANT_ID, String.class)
            );
        } catch (JwtException ex) {
            log.warn("JWT verification failed: {}, traceId={}",
                ex.getMessage(), MDC.get("traceId"));
            return null;
        }
    }

    /**
     * 验签是否通过。
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(keyManager.getPublicKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
