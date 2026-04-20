package com.manpou.allinone.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * 统一 API 响应结构。
 * 所有 Controller 必须返回此类型。
 *
 * 约定：
 * - code="ok" 表示成功，其他表示失败
 * - message 用于展示给用户
 * - data 为 null 时不序列化（节省带宽）
 *
 * @param <T> 响应数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;
    private String detail;

    public Result() {}

    public Result(String code, String message, T data, String traceId, String detail) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
        this.detail = detail;
    }

    // Lombok-free getters/setters (JDK 25 compatibility)
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    // Lombok-free builder
    public static <T> Builder<T> builder() { return new Builder<>(); }

    public static class Builder<T> {
        private String code;
        private String message;
        private T data;
        private String traceId;
        private String detail;
        public Builder<T> code(String v) { this.code = v; return this; }
        public Builder<T> message(String v) { this.message = v; return this; }
        public Builder<T> data(T v) { this.data = v; return this; }
        public Builder<T> traceId(String v) { this.traceId = v; return this; }
        public Builder<T> detail(String v) { this.detail = v; return this; }
        public Result<T> build() { return new Result<>(code, message, data, traceId, detail); }
    }

    // ===== 静态工厂方法 =====
    public static <T> Result<T> ok() { return Result.<T>builder().code("ok").message("success").build(); }
    public static <T> Result<T> ok(T data) { return Result.<T>builder().code("ok").message("success").data(data).build(); }
    public static <T> Result<T> ok(String message, T data) { return Result.<T>builder().code("ok").message(message).data(data).build(); }
    public static <T> Result<T> fail(String code, String message) { return Result.<T>builder().code(code).message(message).build(); }
    public static <T> Result<T> fail(String code, String message, String detail) { return Result.<T>builder().code(code).message(message).detail(detail).build(); }

    // ===== equals/hashCode — 仅比较 code + message =====
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Result<?> result = (Result<?>) o;
        return Objects.equals(code, result.code) && Objects.equals(message, result.message);
    }

    @Override
    public int hashCode() { return Objects.hash(code, message); }
}
