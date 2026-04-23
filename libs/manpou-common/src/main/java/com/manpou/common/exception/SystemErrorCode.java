package com.manpou.common.exception;

/**
 * 系统级错误码常量（INTJ 审计 2026-04-22）。
 * <p>
 * 对应 CODGEM P1 错误码规范：
 * E1xxx = 参数错误，E4xxx = 资源错误，E6xxx = 系统错误。
 *
 * @see GlobalExceptionHandler
 */
public final class SystemErrorCode {

    private SystemErrorCode() {}

    /** 参数类型不匹配 */
    public static final String PARAM_TYPE_MISMATCH = "E1001";

    /** API 端点不存在 */
    public static final String RESOURCE_NOT_FOUND = "E4001";

    /** HTTP 方法不支持 */
    public static final String METHOD_NOT_ALLOWED = "E4002";

    /** 内部系统错误 */
    public static final String INTERNAL_ERROR = "E6001";
}
