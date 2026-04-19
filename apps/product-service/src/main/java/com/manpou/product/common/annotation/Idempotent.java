package com.manpou.product.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解。
 * 标注在 Controller 或 Service 方法上，
 * 自动防止接口重复提交（如网络重试导致的多扣款）。
 *
 * 使用方式：
 * 1. 客户端在请求头携带 X-Idempotency-Key: {uuid}
 * 2. 服务端检查 Redis 中是否存在此 key
 *    - 存在：直接返回缓存结果（不重复执行业务）
 *    - 不存在：执行业务逻辑，结果写入 Redis（TTL=24h）
 *
 * @see IdempotencyAspect
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等 key 的 Redis 前缀。
     * 默认：idem:{className}:{methodName}:
     */
    String keyPrefix() default "";

    /**
     * 幂等 key 的有效期。
     * 默认 24 小时。
     */
    long ttl() default 24 * 60 * 60;
}
