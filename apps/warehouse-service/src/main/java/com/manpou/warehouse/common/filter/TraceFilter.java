package com.manpou.warehouse.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * W3C Trace Context 过滤器。
 *
 * 功能：
 * 1. 从请求头提取 W3C traceparent，解析出 traceId
 * 2. 若无 traceparent，则生成新的 traceId
 * 3. 将 traceId 注入 MDC，供 logback 使用
 * 4. 在响应头返回 X-Trace-Id
 *
 * 格式：traceparent = 00-{trace-id}-{span-id}-{flags}
 * 示例：00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
 */
@Slf4j
@Component
@Order(1)
public class TraceFilter implements Filter {

    /** MDC 中 traceId 的 key（与 logback-spring.xml 中一致） */
    public static final String TRACE_ID_KEY = "traceId";

    /** W3C Traceparent 请求头 */
    public static final String TRACEPARENT_HEADER = "traceparent";

    /** 响应中返回的 TraceId 头 */
    public static final String TRACE_ID_RESPONSE_HEADER = "X-Trace-Id";

    /** W3C traceparent 正则：00-{32 hex}-{16 hex}-{2 hex} */
    private static final Pattern TRACEPARENT_PATTERN =
            Pattern.compile("^00-([a-f0-9]{32})-([a-f0-9]{16})-([0-9a-f]{2})$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = extractOrGenerateTraceId(httpRequest);

        try {
            // 注入 MDC
            MDC.put(TRACE_ID_KEY, traceId);

            // 记录请求（DEBUG 级别，避免性能损耗）
            if (log.isDebugEnabled()) {
                log.debug("[Trace] incoming request: method={}, uri={}, traceId={}",
                        httpRequest.getMethod(),
                        httpRequest.getRequestURI(),
                        traceId);
            }

            // 继续过滤器链
            chain.doFilter(request, response);

        } finally {
            // 必须移除 MDC，防止内存泄漏
            MDC.remove(TRACE_ID_KEY);

            // 响应头中返回 traceId（无论成功或异常）
            httpResponse.setHeader(TRACE_ID_RESPONSE_HEADER, traceId);
        }
    }

    /**
     * 从 W3C traceparent 头提取 traceId，
     * 若无则生成新的 UUID。
     */
    public String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceparent = request.getHeader(TRACEPARENT_HEADER);

        if (traceparent != null && !traceparent.isBlank()) {
            Matcher matcher = TRACEPARENT_PATTERN.matcher(traceparent.trim().toLowerCase());
            if (matcher.matches()) {
                // W3C traceparent 格式：00-{traceId}-{spanId}-{flags}
                // 提取 traceId（第一个捕获组，32字符）
                String traceId = matcher.group(1);
                log.trace("[Trace] extracted from traceparent: {}", traceId);
                return traceId;
            } else {
                log.warn("[Trace] invalid traceparent format: {}", traceparent);
            }
        }

        // 无 traceparent，生成新的 32 字符十六进制 traceId（W3C 标准）
        String generated = UUID.randomUUID().toString().replace("-", "");
        log.trace("[Trace] generated new traceId: {}", generated);
        return generated;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("[Trace] TraceFilter initialized");
    }

    @Override
    public void destroy() {
        log.info("[Trace] TraceFilter destroyed");
    }
}
