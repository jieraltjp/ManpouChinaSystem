package com.manpou.product.infrastructure.aspect;

import com.manpou.product.common.annotation.Idempotent;
import com.manpou.product.common.exception.BusinessException;
import com.manpou.product.common.filter.TraceFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 幂等性切面（Infrastructure 层）。
 *
 * 工作原理：
 * 1. 从 HTTP 请求头 X-Idempotency-Key 提取幂等键
 * 2. 在 Redis 中查找是否已存在该键
 *    - 已存在：说明请求已处理过，直接返回缓存结果
 *    - 不存在：执行业务方法，将结果缓存到 Redis
 * 3. 缓存有效期默认 24 小时
 *
 * 注意：
 * - 使用此注解的方法，返回值必须可序列化（JSON）
 * - 幂等键需保证全局唯一（推荐 UUID）
 * - 业务异常不缓存（防止敏感信息泄漏）
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final String KEY_PREFIX = "idem:";

    @Pointcut("@annotation(anno)")
    public void atAnnotation(Idempotent anno) {}

    @Around("atAnnotation(anno)")
    public Object checkIdempotency(ProceedingJoinPoint pjp, Idempotent anno) throws Throwable {
        String idempotencyKey = extractKey();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("[Idempotency] missing key, method={}", methodName(pjp));
            return pjp.proceed();
        }

        String redisKey = buildRedisKey(pjp, anno, idempotencyKey);
        return resolve(redisKey, idempotencyKey, pjp, anno);
    }

    // ===== 核心解析逻辑 =====

    private Object resolve(String redisKey, String idempotencyKey,
                           ProceedingJoinPoint pjp, Idempotent anno) throws Throwable {
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            return deserializeAndReturn(redisKey, cached, pjp);
        }
        return executeAndCache(redisKey, idempotencyKey, pjp, anno);
    }

    private Object deserializeAndReturn(String redisKey, String cached, ProceedingJoinPoint pjp) {
        log.info("[Idempotency] hit, key={}, method={}, traceId={}",
                redisKey, methodName(pjp), MDC.get(TraceFilter.TRACE_ID_KEY));
        try {
            return objectMapper.readValue(cached, resolveReturnType(pjp));
        } catch (Exception ex) {
            log.warn("[Idempotency] deserialize failed, key={}", redisKey, ex);
            return null;
        }
    }

    private Object executeAndCache(String redisKey, String idempotencyKey,
                                    ProceedingJoinPoint pjp, Idempotent anno) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            long costMs = (System.nanoTime() - start) / 1_000_000;
            cacheResult(redisKey, result, anno.ttl(), TimeUnit.SECONDS);
            log.info("[Idempotency] ok, key={}, method={}, cost={}ms, traceId={}",
                    idempotencyKey, methodName(pjp), costMs, MDC.get(TraceFilter.TRACE_ID_KEY));
            return result;
        } catch (BusinessException ex) {
            // 业务异常不缓存 — 防止敏感信息通过重试泄漏
            log.warn("[Idempotency] biz error, key={}, code={}, traceId={}",
                    idempotencyKey, ex.getCode(), MDC.get(TraceFilter.TRACE_ID_KEY));
            throw ex;
        } catch (Throwable t) {
            log.error("[Idempotency] error, key={}, error={}, traceId={}",
                    idempotencyKey, t.getMessage(), MDC.get(TraceFilter.TRACE_ID_KEY), t);
            throw t;
        }
    }

    // ===== 辅助方法 =====

    private void cacheResult(String redisKey, Object result, long ttl, TimeUnit unit) {
        try {
            String serialized = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(redisKey, serialized, ttl, unit);
        } catch (Exception ex) {
            log.warn("[Idempotency] cache write failed, key={}", redisKey, ex);
        }
    }

    private String extractKey() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder
                    .getRequestAttributes();
            if (attrs == null) return null;
            var request = ((org.springframework.web.context.request.ServletRequestAttributes) attrs)
                    .getRequest();
            return request.getHeader(IDEMPOTENCY_KEY_HEADER);
        } catch (Exception ex) {
            log.debug("[Idempotency] cannot extract key from request", ex);
            return null;
        }
    }

    private String buildRedisKey(ProceedingJoinPoint pjp, Idempotent anno, String idempotencyKey) {
        if (!anno.keyPrefix().isBlank()) {
            return KEY_PREFIX + anno.keyPrefix();
        }
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return KEY_PREFIX + sig.getDeclaringType().getSimpleName()
                + ":" + sig.getName() + ":" + idempotencyKey;
    }

    private String methodName(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getName();
    }

    private Class<?> resolveReturnType(ProceedingJoinPoint pjp) {
        return ((MethodSignature) pjp.getSignature()).getMethod().getReturnType();
    }
}
