package com.company.gateway.filter;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TraceId 工具。
 *
 * W3C Trace Context 标准：
 * - traceparent = 00-{traceId(32hex)}-{spanId(16hex)}-{flags}
 * - 示例：00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
 */
public final class TraceIdUtil {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String TRACE_ID_RESPONSE_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final Pattern TRACEPARENT_PATTERN =
        Pattern.compile("^00-([a-f0-9]{32})-([a-f0-9]{16})-([0-9a-f]{2})$");

    private TraceIdUtil() {}

    /**
     * 从 W3C traceparent 提取 traceId，无则生成新的 32 字符十六进制。
     */
    public static String extractOrGenerate(String traceparent) {
        if (traceparent != null && !traceparent.isBlank()) {
            Matcher m = TRACEPARENT_PATTERN.matcher(traceparent.trim().toLowerCase());
            if (m.matches()) {
                return m.group(1);
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void putMdc(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static void removeMdc() {
        MDC.remove(TRACE_ID_KEY);
    }
}
