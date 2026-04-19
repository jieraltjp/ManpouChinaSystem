package com.manpou.gateway.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 网关统一响应结构。
 *
 * 约定：
 * - code="ok" 表示成功，其他表示失败
 * - 内部服务返回的 Result<T> 透传 data；非 Result 响应包装为此格式
 * - 错误响应不返回原始异常信息（防信息泄露）
 *
 * @param <T> 响应数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GatewayResult<T>(
    String code,
    String message,
    T data,
    String traceId
) {

    public static <T> GatewayResult<T> ok(T data, String traceId) {
        return new GatewayResult<>("ok", "success", data, traceId);
    }

    public static <T> GatewayResult<T> fail(String code, String message, String traceId) {
        return new GatewayResult<>(code, message, null, traceId);
    }

    @SuppressWarnings("unchecked")
    public static <T> GatewayResult<T> from(Object raw, String traceId) {
        if (raw instanceof GatewayResult<?> existing) {
            return (GatewayResult<T>) existing;
        }
        return ok((T) raw, traceId);
    }
}
