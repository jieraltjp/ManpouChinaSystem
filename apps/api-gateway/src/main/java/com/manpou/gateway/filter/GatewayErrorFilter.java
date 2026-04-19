package com.manpou.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.manpou.gateway.filter.TraceIdUtil.*;

/**
 * 网关全局异常处理器。
 *
 * 所有未被捕获的异常统一格式返回，禁止泄漏堆栈。
 */
@Slf4j
@Component
@Order(-1)
public class GatewayErrorFilter implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = MDC.get(TRACE_ID_KEY);
        String code;
        String message;
        HttpStatus status;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            code = mapStatusToCode(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : rse.getStatusCode().toString();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = "gateway.internal-error";
            message = "Internal server error";
            log.error("[Gateway] unhandled exception: {}, traceId={}",
                ex.getClass().getSimpleName(), traceId, ex);
        }

        response.setStatusCode(status);

        String body = """
            {"code":"%s","message":"%s","traceId":"%s"}
            """.formatted(code, message, traceId != null ? traceId : "");
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    private String mapStatusToCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> "validation.error";
            case 401 -> "auth.unauthenticated";
            case 403 -> "auth.forbidden";
            case 404 -> "resource.not-found";
            case 405 -> "http.method-not-allowed";
            case 502 -> "gateway.bad-upstream";
            case 503 -> "gateway.upstream-unavailable";
            case 504 -> "gateway.upstream-timeout";
            default -> "gateway.error";
        };
    }
}
