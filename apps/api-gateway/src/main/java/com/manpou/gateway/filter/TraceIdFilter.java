package com.manpou.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.manpou.gateway.filter.TraceIdUtil.*;

/**
 * W3C TraceId 全局过滤器。
 *
 * 执行顺序：1（最高优先级）
 *
 * 功能：
 * 1. 从请求头提取 W3C traceparent，解析 traceId
 * 2. 无 traceparent 则生成新的 32 字符十六进制
 * 3. 注入 MDC（供日志使用）
 * 4. 将 traceId 注入下游请求头 X-Trace-Id
 * 5. 响应头返回 X-Trace-Id
 */
@Slf4j
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String traceparent = request.getHeaders().getFirst(TRACEPARENT_HEADER);
        String traceId = extractOrGenerate(traceparent);

        putMdc(traceId);
        log.debug("[Trace] request: {} {}, traceId={}",
            request.getMethod(), request.getURI().getPath(), traceId);

        ServerHttpRequest mutated = request.mutate()
            .header(TRACE_ID_HEADER, traceId)
            .build();

        return chain.filter(exchange.mutate().request(mutated).build())
            .doFinally(signal -> removeMdc())
            .then(Mono.fromRunnable(() ->
                response.getHeaders().set(TRACE_ID_RESPONSE_HEADER, traceId)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
