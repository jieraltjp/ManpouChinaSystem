package com.manpou.allinone.common.exception;

import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.infrastructure.client.AuditLogClient;
import com.manpou.allinone.infrastructure.security.JwtContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 统一捕获所有异常并返回标准化错误响应。
 *
 * 异常分类：
 * - 业务异常（BusinessException）
 * - 参数校验异常（MethodArgumentNotValid / Bind / TypeMismatch）
 * - HTTP 路由异常（NotFound / MethodNotAllowed）
 * - 系统异常（其他所有）
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final ValidationErrorCodeMapper MAPPER = ValidationErrorCodeMapper.INSTANCE;

    private final AuditLogClient auditLogClient;

    // ===== 业务异常 =====

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("[业务异常] code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // ===== 参数校验异常 =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        String code = MAPPER.map(e.getBindingResult().getFieldErrors());
        log.warn("[参数校验失败] code={}, fields={}", code, message);
        return Result.fail(code, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        String code = MAPPER.map(e.getFieldErrors());
        log.warn("[参数绑定失败] code={}, fields={}", code, message);
        return Result.fail(code, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String requiredType = e.getRequiredType() == null ? "未知" : e.getRequiredType().getSimpleName();
        String message = String.format("参数 '%s' 类型错误，期望 %s", e.getName(), requiredType);
        log.warn("[类型不匹配] {}", message);
        return Result.fail("validation.param.type-mismatch", message);
    }

    // ===== HTTP 路由异常 =====

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        log.warn("[资源不存在] {}", e.getMessage());
        return Result.fail("resource.not-found", "API endpoint not found: " + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail("http.method-not-allowed", "HTTP method not allowed: " + e.getMethod());
    }

    // ===== 安全异常 =====

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("[权限不足] {}", e.getMessage());
        sendAccessDeniedAudit(e, request);
        return Result.fail("auth.forbidden", "Access denied: insufficient permissions");
    }

    private void sendAccessDeniedAudit(AccessDeniedException e, HttpServletRequest request) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("traceId", MDC.get("traceId"));
            payload.put("userId", JwtContextHolder.getUserId());
            payload.put("username", JwtContextHolder.getUsername());
            payload.put("module", "auth");
            payload.put("action", "AUTH_DENIED");
            payload.put("resourceType", null);
            payload.put("resourceId", null);
            payload.put("httpMethod", request.getMethod());
            payload.put("httpUrl", request.getRequestURI());
            payload.put("ipAddress", extractClientIp(request));
            payload.put("userAgent", request.getHeader("User-Agent"));
            payload.put("detail", "{\"error\": \"" + e.getClass().getSimpleName() + "\", \"message\": \"" + e.getMessage() + "\"}");
            auditLogClient.sendAsync(payload);
        } catch (Exception ex) {
            log.warn("[权限不足] 审计日志发送失败: {}", ex.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("[系统异常] type={}, message={}", e.getClass().getName(), e.getMessage(), e);
        String detail = isProduction() ? null : e.getClass().getName() + ": " + e.getMessage();
        return Result.fail("system.internal-error", "Internal server error", detail);
    }

    // ===== 私有辅助方法 =====

    private boolean isProduction() {
        String profile = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "local");
        return "prod".equals(profile);
    }
}
