package com.company.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * CORS 全局过滤器。
 *
 * 执行顺序：0（最先执行，在所有 Filter 之前）
 *
 * 配置由 application.yml 的 spring.cloud.gateway.globalcors 节点驱动。
 */
@Component
public class CorsFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        HttpHeaders headers = response.getHeaders();
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            request.getHeaders().getFirst(HttpHeaders.ORIGIN));
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            "Authorization,Content-Type,X-Requested-With,X-Trace-Id,X-Tenant-Id");
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
            "X-Trace-Id,X-User-Id");
        headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
            response.setStatusCode(org.springframework.http.HttpStatus.OK);
            return Mono.empty();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE - 1;
    }
}
