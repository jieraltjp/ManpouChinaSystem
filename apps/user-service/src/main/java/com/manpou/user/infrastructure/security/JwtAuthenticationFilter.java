package com.manpou.user.infrastructure.security;

import com.manpou.common.result.Result;
import com.manpou.user.infrastructure.security.JwtService.JwtClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JWT 认证过滤器。
 *
 * 从请求头提取 JWT Token，验证并设置 SecurityContext。
 * 详见 docs/pro/02-user-service.md §认证授权
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /** ADMIN 角色 *:* 展开后的所有具体权限（66条，与 allinone JwtAuthenticationFilter.ALL_PERMISSIONS 同步，按 SPEC-B11 v1.2.0） */
    private static final Set<String> ALL_PERMISSIONS = Set.of(
        "demand:create", "demand:read", "demand:update", "demand:delete",
        "procurement:create", "procurement:read", "procurement:update", "procurement:delete",
        "shipment:create", "shipment:read", "shipment:update", "shipment:delete",
        "qc:create", "qc:read", "qc:update", "qc:delete",
        "logistics:create", "logistics:read", "logistics:update", "logistics:delete",
        "consolidation:create", "consolidation:read", "consolidation:update", "consolidation:delete",
        "container:create", "container:read", "container:update", "container:delete",
        "customs:create", "customs:read", "customs:update", "customs:delete",
        "japan_customs:create", "japan_customs:read", "japan_customs:update", "japan_customs:delete",
        "tax_refund:create", "tax_refund:read", "tax_refund:update", "tax_refund:delete",
        "sales:create", "sales:read", "sales:update", "sales:delete",
        "factory:create", "factory:read", "factory:update", "factory:delete",
        "product:create", "product:read", "product:update", "product:delete",
        "warehouse:create", "warehouse:read", "warehouse:update", "warehouse:delete",
        "notification:create", "notification:read", "notification:update", "notification:delete",
        "user:create", "user:read", "user:update", "user:delete", "user:reset_password",
        "role:create", "role:read", "role:update", "role:delete", "role:assign",
        "audit:read"
    );

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 验证 Token
            if (jwtService.validateToken(token)) {
                JwtClaims claims = jwtService.extractClaims(token);

                // 构建认证信息（JwtClaims 是 record，使用 roles() 等访问器）
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                // 角色 → ROLE_<role>
                claims.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
                // 权限 → 直接作为 authority（支持 @PreAuthorize("hasAuthority('role:create')")）
                // ADMIN 角色的 *:* 权限需要展开为所有具体权限
                if (claims.permissions() != null) {
                    boolean isAdmin = claims.roles().contains("ADMIN");
                    for (String perm : claims.permissions()) {
                        if (isAdmin && "*:*".equals(perm)) {
                            // ADMIN + *:* → 展开为所有具体权限
                            authorities.add(new SimpleGrantedAuthority(perm)); // 保留 *:* 也加上
                            for (String specific : ALL_PERMISSIONS) {
                                authorities.add(new SimpleGrantedAuthority(specific));
                            }
                        } else {
                            authorities.add(new SimpleGrantedAuthority(perm));
                        }
                    }
                }

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        claims.userId(),  // record accessor（getUserId 由 UserContext 覆写）
                        null,
                        authorities
                    );

                // 设置上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 传递用户信息到 MDC（日志用）
                JwtContextHolder.set(claims);

                filterChain.doFilter(request, response);
            } else {
                sendUnauthorizedResponse(response, "Token expired or invalid");
            }
        } catch (Exception ex) {
            log.warn("JWT authentication failed: {}", ex.getMessage());
            sendUnauthorizedResponse(response, "Authentication failed");
        } finally {
            JwtContextHolder.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<?> result = Result.fail("auth.unauthenticated", message);
        objectMapper.writeValue(response.getWriter(), result);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 跳过登录接口
        return path.startsWith("/api/v1/auth/");
    }
}
