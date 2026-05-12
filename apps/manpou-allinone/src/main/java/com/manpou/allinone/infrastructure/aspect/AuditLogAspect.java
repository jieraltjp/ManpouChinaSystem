package com.manpou.allinone.infrastructure.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.infrastructure.client.AuditLogClient;
import com.manpou.allinone.infrastructure.security.JwtContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 操作日志切面。
 *
 * <p>仅记录成功操作（方法正常返回或抛出业务异常）。
 * 记录失败（连接超时、参数解析异常等）不记录。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogClient auditLogClient;
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
        String resolvedId = resolveSpEL(auditLog.resourceId(), pjp, returnValue);
        log.info("[AuditLog] triggering: module={}, action={}, resourceType={}, resourceId={}",
                auditLog.module(), auditLog.action(), auditLog.resourceType(), resolvedId);
        Map<String, Object> payload = new HashMap<>();

        // 链路追踪
        payload.put("traceId", MDC.get("traceId"));

        // 用户上下文（companyId/departmentId 需 JWT claim 扩展支持）
        payload.put("userId", JwtContextHolder.getUserId());
        payload.put("username", JwtContextHolder.getUsername());
        payload.put("operatorName", null);
        payload.put("companyId", null);
        payload.put("departmentId", null);

        // 业务信息
        payload.put("module", auditLog.module());
        payload.put("action", auditLog.action());
        payload.put("resourceType", auditLog.resourceType());

        // SpEL 动态解析（支持 #_return 引用返回值，e.g. CREATE 后取新 ID）
        payload.put("resourceId", resolveSpEL(auditLog.resourceId(), pjp, returnValue));
        payload.put("resourceCode", resolveSpEL(auditLog.resourceCode(), pjp, returnValue));

        // HTTP 请求信息
        HttpServletRequest request = currentRequest();
        if (request != null) {
            payload.put("httpMethod", request.getMethod());
            payload.put("httpUrl", request.getRequestURI());
            payload.put("ipAddress", extractClientIp(request));
            payload.put("userAgent", truncate(request.getHeader("User-Agent"), 512));
            payload.put("requestId", request.getHeader("X-Request-Id"));
        }

        // 方法参数（脱敏）
        payload.put("detail", serializeArgs(pjp));

        auditLogClient.sendAsync(payload);
    }

    // ===== SpEL 解析 =====

    /**
     * 简化 SpEL 解析：支持 "#paramName" 格式。
     * 如 "#cmd.id" → 从方法参数 "cmd" 的 "id" 字段取值。
     * 支持 "#_return" → 方法返回值（自动 unwrap Result<T> 取内层 data，
     *   用于 CREATE 场景下 resourceId="#_return" 获取新实体的 ID）。
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
     * 支持：
     * - Result&lt;T&gt; → 取 .payload（单个 ID）
     * - Result&lt;List&lt;Long&gt;&gt; → 取第一个元素（批量创建主 ID）
     * - ResponseEntity&lt;T&gt; → 取 .body
     * - Long / Integer → 直接返回
     */
    @SuppressWarnings("unchecked")
    private String extractReturnId(Object returnValue) {
        if (returnValue == null) return null;
        try {
            if (returnValue instanceof com.manpou.allinone.common.result.Result) {
                com.manpou.allinone.common.result.Result<Object> r =
                        (com.manpou.allinone.common.result.Result<Object>) returnValue;
                Object payload = r.getPayload();
                // Result<List<Long>> → 取第一个
                if (payload instanceof java.util.List<?> list && !list.isEmpty()) {
                    return String.valueOf(list.get(0));
                }
                return payload != null ? String.valueOf(payload) : null;
            }
            // ResponseEntity<T>
            if (returnValue instanceof org.springframework.http.ResponseEntity<?> re) {
                Object body = re.getBody();
                return body != null ? String.valueOf(body) : null;
            }
            return String.valueOf(returnValue);
        } catch (Exception ex) {
            return String.valueOf(returnValue);
        }
    }

    // ===== 工具方法 =====

    /**
     * 系统级异常不记录日志（参数解析、权限拦截、Bean 初始化等）。
     */
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

    /**
     * 脱敏：字段名含敏感关键词时，将值替换为 "***"。
     * 包含深度限制（maxDepth=3）和循环引用检测，防止 StackOverflow。
     *
     * <p>DAG 支持：path（当前路径）检测真循环；seen（全局已访问）防止跨分支重复处理。
     * 同一对象在不同分支出现（如 shared AddressDTO）不会被误判为 cyclic。
     */
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
        // 枚举：返回 .name()，避免暴露内部 $VALUES 字段
        if (cls.isEnum()) return ((Enum<?>) value).name();

        // 真循环：同一对象在当前递归路径中再次出现
        if (path.contains(value)) return "[cyclic]";
        // DAG 共享节点：在其他分支已处理过，直接返回占位（避免重复）
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
            // 普通 Bean：逐字段处理
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
