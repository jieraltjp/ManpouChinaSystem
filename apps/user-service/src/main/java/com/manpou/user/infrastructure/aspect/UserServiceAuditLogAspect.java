package com.manpou.user.infrastructure.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manpou.common.annotation.AuditLog;
import com.manpou.user.application.service.AuditLogService;
import com.manpou.user.infrastructure.security.JwtContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * user-service 操作日志切面。
 *
 * <p>仅记录成功操作（方法正常返回或抛出业务异常）。
 * 记录失败（连接超时、参数解析异常等）不记录。
 *
 * <p>与 allinone AuditLogAspect 保持一致的实现逻辑：
 * - 共享 {@link com.manpou.common.annotation.AuditLog} 注解
 * - SpEL 简化解析：#_return / #paramName
 * - 敏感字段自动脱敏
 * - JwtContextHolder 提供 username / operatorName（Lesson 87）
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class UserServiceAuditLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|token|secret|credential|key|authorization|api[_-]?key|access[_-]?token)",
            Pattern.CASE_INSENSITIVE);

    @Around("@annotation(auditLog)")
    public Object record(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        log.debug("[AuditLog] aspect AROUND START: method={}, module={}, action={}",
                methodName(pjp), auditLog.module(), auditLog.action());
        boolean isSystemEx = false;
        Object returnValue = null;
        try {
            returnValue = pjp.proceed();
            return returnValue;
        } catch (Throwable t) {
            isSystemEx = isSystemException(t);
            throw t;
        } finally {
            if (!isSystemEx) {
                try {
                    sendLog(pjp, auditLog, returnValue);
                } catch (Exception ex) {
                    log.warn("[AuditLog] aspect send failed: method={}, error={}",
                            methodName(pjp), ex.getMessage());
                }
            } else {
                log.debug("[AuditLog] skipped (system exception): method={}", methodName(pjp));
            }
        }
    }

    private void sendLog(ProceedingJoinPoint pjp, AuditLog auditLog, Object returnValue) {
        com.manpou.user.domain.model.AuditLog al = new com.manpou.user.domain.model.AuditLog();
        al.setTraceId(MDC.get("traceId"));
        al.setUserId(JwtContextHolder.getUserId());
        al.setUsername(JwtContextHolder.getUsername());
        al.setOperatorName(JwtContextHolder.getOperatorName());

        al.setModule(auditLog.module());
        al.setAction(auditLog.action());
        al.setResourceType(auditLog.resourceType());
        al.setResourceId(resolveSpEL(auditLog.resourceId(), pjp, returnValue));
        al.setResourceCode(resolveSpEL(auditLog.resourceCode(), pjp, returnValue));

        HttpServletRequest request = currentRequest();
        if (request != null) {
            al.setHttpMethod(request.getMethod());
            al.setHttpUrl(request.getRequestURI());
            al.setIpAddress(extractClientIp(request));
            al.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
            al.setRequestId(request.getHeader("X-Request-Id"));
        }

        al.setDetail(serializeArgs(pjp));
        al.setCreateTime(LocalDateTime.now());

        auditLogService.save(al);
        log.info("[AuditLog] saved: module={}, action={}, resourceId={}, username={}",
                al.getModule(), al.getAction(), al.getResourceId(), al.getUsername());
    }

    // ===== SpEL 解析 =====

    /**
     * 简化 SpEL 解析：支持 "#paramName" 和 "#_return"。
     */
    private String resolveSpEL(String expr, ProceedingJoinPoint pjp, Object returnValue) {
        if (expr == null || expr.isBlank()) {
            return null;
        }
        String paramName = expr.startsWith("#") ? expr.substring(1) : expr;
        if ("_return".equals(paramName) || "_return".equals(expr)) {
            return extractReturnId(returnValue);
        }
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            String[] names = sig.getParameterNames();
            Object[] values = pjp.getArgs();
            if (names == null || values == null || names.length != values.length) {
                return null;
            }
            for (int i = 0; i < names.length; i++) {
                if (paramName.equals(names[i])) {
                    Object v = values[i];
                    return v != null ? String.valueOf(v) : null;
                }
            }
            return null;
        } catch (Exception ex) {
            log.debug("[AuditLog] SpEL resolve failed: expr={}, error={}", expr, ex.getMessage());
            return null;
        }
    }

    /**
     * 从方法返回值中提取 ID。
     * 支持 Result&lt;T&gt; / Long / Integer。
     */
    @SuppressWarnings("unchecked")
    private String extractReturnId(Object returnValue) {
        if (returnValue == null) return null;
        try {
            if (returnValue instanceof com.manpou.common.result.Result<?> r) {
                Object payload = r.getPayload();
                if (payload instanceof Long l) return String.valueOf(l);
                if (payload instanceof Integer i) return String.valueOf(i);
                if (payload instanceof String s) return s;
                return payload != null ? String.valueOf(payload) : null;
            }
            return String.valueOf(returnValue);
        } catch (Exception ex) {
            return String.valueOf(returnValue);
        }
    }

    // ===== 工具方法 =====

    private boolean isSystemException(Throwable t) {
        String name = t.getClass().getName();
        return name.contains("IllegalArgument")
                || name.contains("BeanCreation") || name.contains("BeanInitialization")
                || name.contains("HttpMessageNotReadable") || name.contains("MissingServletRequest")
                || name.contains("MethodArgumentTypeMismatch") || name.contains("ConstraintViolation")
                || name.contains("HttpMediaType") || name.contains("NoHandlerFound");
    }

    private String methodName(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getName();
    }

    private HttpServletRequest currentRequest() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            return ((org.springframework.web.context.request.ServletRequestAttributes) attrs).getRequest();
        } catch (Exception ex) {
            return null;
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

    private String serializeArgs(ProceedingJoinPoint pjp) {
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            Object[] args = pjp.getArgs();
            String[] names = sig.getParameterNames();
            if (args == null || args.length == 0) {
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                String name = (names != null && i < names.length) ? names[i] : "arg" + i;
                map.put(name, sanitize(args[i]));
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            log.debug("[AuditLog] serialize args failed: {}", ex.getMessage());
            return null;
        }
    }

    private Object sanitize(Object value) {
        var path = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        return sanitizeImpl(value, 0, path, seen);
    }

    private Object sanitizeImpl(Object value, int depth,
                              java.util.Set<Object> path,
                              java.util.Set<Object> seen) {
        if (value == null) return null;
        if (depth > 3) return "[max-depth]";

        Class<?> cls = value.getClass();
        if (isPrimitiveOrWrapper(cls)) return value;
        if (cls == String.class) return value;
        if (cls.isArray()) return value;
        if (cls.isEnum()) return ((Enum<?>) value).name();

        if (path.contains(value)) return "[cyclic]";
        if (seen.contains(value)) return "[shared]";

        path.add(value);
        seen.add(value);

        try {
            if (value instanceof java.util.Collection<?> c) {
                var result = c.stream().limit(50).map(v -> sanitizeImpl(v, depth + 1, path, seen)).toList();
                path.remove(value);
                return result;
            }
            if (value instanceof java.util.Map<?, ?> m) {
                Map<String, Object> result = new HashMap<>();
                m.forEach((k, v) -> {
                    String key = String.valueOf(k);
                    Object sanitized = SENSITIVE_PATTERN.matcher(key).find()
                            ? "***"
                            : sanitizeImpl(v, depth + 1, path, seen);
                    result.put(key, sanitized);
                });
                path.remove(value);
                return result;
            }
            Map<String, Object> map = new HashMap<>();
            for (var f : cls.getDeclaredFields()) {
                f.setAccessible(true);
                String fieldName = f.getName();
                Object fieldValue = f.get(value);
                Object sanitized = SENSITIVE_PATTERN.matcher(fieldName).find()
                        ? "***"
                        : sanitizeImpl(fieldValue, depth + 1, path, seen);
                map.put(fieldName, sanitized);
            }
            path.remove(value);
            return map;
        } catch (Exception ex) {
            path.remove(value);
            return String.valueOf(value);
        }
    }

    private boolean isPrimitiveOrWrapper(Class<?> c) {
        return c.isPrimitive() || c == Boolean.class || c == Integer.class
                || c == Long.class || c == Double.class || c == Float.class
                || c == Short.class || c == Byte.class || c == Character.class;
    }

    private String truncate(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }
}
