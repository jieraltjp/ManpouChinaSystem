package com.manpou.allinone.infrastructure.security;

import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.infrastructure.security.JwtService.JwtClaims;
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
 * 详见 docs/pro/19-manpou-allinone.md §认证授权
 *
 * <p>Phase 3 修复（2026-05-08）：
 * 从 JWT Claims 中同时提取 roles 和 permissions，构建完整的 Spring Security authorities。
 * roles → {@code ROLE_<role>} 格式，支持 hasRole() 检查；
 * permissions → 直接作为 authority，支持 hasAuthority('xxx:create') 检查。
 * ADMIN 角色的 *:* 权限展开为所有具体权限（63条，与 V15 DB 实际对齐）。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * ADMIN 角色 *:* 展开后的所有具体权限（108条）。
     * CRUD 87条 + page:*:access 23条。
     * warehouse/notification 未入 Set（模块未实现）。
     * V56 新增：page:*:access 路由守卫权限（ID 140~157，18个模块）。
     */
    private static final Set<String> ALL_PERMISSIONS = Set.of(
        "demand:create", "demand:read", "demand:update", "demand:delete",
        "procurement:create", "procurement:read", "procurement:update", "procurement:delete",
        "shipment:create", "shipment:read", "shipment:update", "shipment:delete",
        "qc:create", "qc:read", "qc:update", "qc:delete",
        "logistics:create", "logistics:read", "logistics:update", "logistics:delete",
        "consolidation:create", "consolidation:read", "consolidation:update", "consolidation:delete",
        "container:create", "container:read", "container:update", "container:delete",
        "ship:create", "ship:read", "ship:update", "ship:delete",
        "dispatch:create", "dispatch:read", "dispatch:update", "dispatch:delete", "dispatch:export",
        "offline_order:create", "offline_order:read", "offline_order:update", "offline_order:delete",
        "customs:create", "customs:read", "customs:update", "customs:delete", "customs:approve",
        "japan_customs:create", "japan_customs:read", "japan_customs:update", "japan_customs:delete",
        "japan_customs:start", "japan_customs:complete",
        "tax_refund:create", "tax_refund:read", "tax_refund:update", "tax_refund:complete", "tax_refund:delete",
        "sales:create", "sales:read", "sales:update", "sales:delete",
        "factory:create", "factory:read", "factory:update", "factory:delete",
        "product:create", "product:read", "product:update", "product:delete",
        "cargo_size:read", "cargo_size:create", "cargo_size:import", "cargo_size:promote", "cargo_size:discard", "cargo_size:update", "cargo_size:delete",
        "legacy_procurement:read", "legacy_procurement:create", "legacy_procurement:update", "legacy_procurement:delete",
        "order:read",
        "user:create", "user:read", "user:update", "user:delete", "user:approve", "user:reset_password",
        "role:create", "role:read", "role:update", "role:delete", "role:assign",
        "permission:read",
        "audit:read", "audit:export",
        "page:demand:access", "page:procurement:access", "page:shipment:access", "page:qc:access",
        "page:logistics:access", "page:consolidation:access", "page:container:access", "page:ship:access",
        "page:customs:access", "page:japan_customs:access", "page:tax_refund:access", "page:sales:access",
        "page:factory:access", "page:product:access", "page:order:access",
        "page:user:access", "page:role:access", "page:audit:access",
        "page:cargo_size:access", "page:dispatch:access", "page:legacy_procurement:access", "page:offline_order:access", "page:profile:access"
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

                // 角色 → ROLE_<role>（支持 hasRole() 检查）
                for (String role : claims.roles()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }

                // 权限 → 直接作为 authority（支持 hasAuthority('xxx:create') 检查）
                if (claims.permissions() != null) {
                    boolean isAdmin = claims.roles().contains("ADMIN");
                    for (String perm : claims.permissions()) {
                        if (isAdmin && "*:*".equals(perm)) {
                            // ADMIN + *:* → 展开为所有具体权限
                            authorities.add(new SimpleGrantedAuthority(perm));
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
                        claims.userId(),
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
