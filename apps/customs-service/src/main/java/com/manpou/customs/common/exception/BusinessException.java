package com.manpou.customs.common.exception;

import lombok.Getter;

/**
 * 业务异常基类。
 * 所有业务异常应继承此类，禁止抛出 RuntimeException。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 语义化错误码，格式：{domain}.{behavior}.{detail} */
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // ===== 预定义异常工厂 =====

    public static BusinessException notFound(String resource, Object id) {
        return new BusinessException("resource.not-found",
                resource + " not found: " + id);
    }

    public static BusinessException invalidParam(String message) {
        return new BusinessException("validation.param.invalid", message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException("auth.forbidden", message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException("resource.conflict", message);
    }

    public static BusinessException internal(String message) {
        return new BusinessException("system.internal-error", message);
    }
}
