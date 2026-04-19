package com.manpou.gateway.filter;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.manpou.gateway.filter.TraceIdUtil.*;

/**
 * 限流过滤器。
 *
 * Resilience4j 的 RequestNotPermitted 异常由 circuitBreakerFilter 统一处理。
 * 此 Filter 记录被限流的请求日志。
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
            .onErrorResume(RequestNotPermitted.class, ex -> {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                response.getHeaders().set("Content-Type", "application/json");
                response.getHeaders().set("Retry-After", "60");

                log.warn("[RateLimit] rejected: {} {}, traceId={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(),
                    MDC.get(TRACE_ID_KEY));

                String body = """
                    {"code":"gateway.rate-limit","message":"Rate limit exceeded, please retry later","traceId":"%s"}
                    """.formatted(MDC.get(TRACE_ID_KEY));
                return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
            });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
