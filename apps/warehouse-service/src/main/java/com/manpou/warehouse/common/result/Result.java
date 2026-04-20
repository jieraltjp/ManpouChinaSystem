package com.manpou.warehouse.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * 统一 API 响应结构。
 * 所有 Controller 必须返回此类型。
 *
 * 约定：
 * - code="ok" 表示成功，其他表示失败
 * - message 用于展示给用户
 * - payload 为 null 时不序列化（节省带宽）
 *
 * INTJ 审判：@Data 已被移除。
 * 原因：Lombok @Data 生成的 equals/hashCode 使用全部 5 个字段，
 * 若 T=Result&lt;X&gt;（自引用），equals() 会递归爆炸。
 * 修复：显式只比较 code + message，与 data 无关。
 *
 * @param <T> 响应数据类型
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 语义化状态码：ok=成功，其他=失败（如 validation.name.required） */
    private String code;

    /** 提示信息（始终返回） */
    private String message;

    /** 响应数据（成功时返回） */
    @JsonProperty("data")
    private T payload;

    /** TraceId（由 Filter 注入，始终返回） */
    private String traceId;

    /** 错误详情（仅在非生产环境返回） */
    private String detail;

    // ===== 静态工厂方法 =====

    public static <T> Result<T> ok() {
        return Result.<T>builder().code("ok").message("success").build();
    }

    public static <T> Result<T> ok(T payload) {
        return Result.<T>builder().code("ok").message("success").payload(payload).build();
    }

    public static <T> Result<T> ok(String message, T data) {
        return Result.<T>builder().code("ok").message(message).payload(payload).build();
    }

    public static <T> Result<T> fail(String code, String message) {
        return Result.<T>builder().code(code).message(message).build();
    }

    public static <T> Result<T> fail(String code, String message, String detail) {
        return Result.<T>builder().code(code).message(message).detail(detail).build();
    }

    // ===== equals/hashCode — 仅比较 code + message =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Result<?> result = (Result<?>) o;
        return Objects.equals(code, result.code) && Objects.equals(message, result.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }
}
