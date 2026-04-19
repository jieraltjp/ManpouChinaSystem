package com.company.gateway.filter;

import com.company.gateway.security.JwtClaims;
import com.company.gateway.security.JwtValidator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.company.gateway.filter.TraceIdUtil.*;

/**
 * JWT RS256 鉴权过滤器。
 *
 * 执行顺序：2（在 TraceIdFilter 之后）
 *
 * 职责：
 * 1. 跳过白名单路径（/api/v1/auth/**, /health, /actuator/health）
 * 2. 从 Authorization: Bearer xxx 提取 Token
 * 3. RS256 公钥验签
 * 4. 提取 Claims，注入下游请求头：
 *    - X-User-Id: 用户 ID
 *    - X-Username: 用户名
 *    - X-Tenant-Id: 租户 ID
 *    - X-User-Roles: 角色列表（逗号分隔）
 *
 * 禁止将 Token 原文或 Claims 写入日志（防信息泄露）。
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USERNAME = "X-Username";
    private static final String X_TENANT_ID = "X-Tenant-Id";
    private static final String X_USER_ROLES = "X-User-Roles";
    private static final String X_USER_PERMISSIONS = "X-User-Permissions";

    private static final List<String> WHITE_LIST = List.of(
        "/api/v1/auth/",
        "/health",
        "/actuator/health",
        "/swagger-ui",
        "/v3/api-docs"
    );

    private final JwtValidator jwtValidator;

    public JwtAuthFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return writeUnauthorized(exchange.getResponse(), "auth.unauthenticated", "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        JwtClaims claims = jwtValidator.extractClaims(token);

        if (claims == null) {
            return writeUnauthorized(exchange.getResponse(), "auth.invalid-token", "Token verification failed");
        }

        log.debug("[Auth] user={}, tenant={}, path={}, traceId={}",
            claims.userId(), claims.tenantId(), path, MDC.get(TRACE_ID_KEY));

        ServerHttpRequest.Builder builder = request.mutate()
            .header(X_USER_ID, claims.userId())
            .header(X_USERNAME, claims.username())
            .header(X_USER_ROLES, String.join(",", claims.roles()))
            .header(X_USER_PERMISSIONS, String.join(",",
                claims.permissions() != null ? claims.permissions() : List.of()));

        // 仅在 tenantId 非空时注入，避免下游收到空串
        if (claims.tenantId() != null && !claims.tenantId().isBlank()) {
            builder.header(X_TENANT_ID, claims.tenantId());
        }

        ServerHttpRequest mutated = builder.build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> writeUnauthorized(ServerHttpResponse response, String code, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set("Content-Type", "application/json");
        String traceId = MDC.get(TRACE_ID_KEY);
        String body = """
            {"code":"%s","message":"%s","traceId":"%s"}
            """.formatted(code, message, traceId != null ? traceId : "");
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
