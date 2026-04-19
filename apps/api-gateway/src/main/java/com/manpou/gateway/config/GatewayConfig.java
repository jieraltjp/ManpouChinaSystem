package com.manpou.gateway.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Spring Cloud Gateway 全局配置。
 *
 * Resilience4j 限流 + 熔断（熔断配置在 application.yml，Java 仅注入限流）：
 * - 全局限流：100 req/s（无 Redis 时使用内存）
 * - 熔断：5s 窗口内 50% 失败率触发 OPEN 状态，30s 后尝试 HALF-OPEN
 * - 重试：GET 请求最多 3 次，异常情况重试
 *
 * ⚠️ 框架演进警告：Spring Cloud Gateway 在 2046 年前应迁移至 Envoy/Istio，
 * 此配置的 RouteLocator DSL 届时需完全重写。
 */
@Configuration
public class GatewayConfig {

    @Value("${gateway.ratelimit.default-rate:100}")
    private int defaultRate;

    // ==================== 限流 ====================

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(defaultRate)
            .timeoutDuration(Duration.ofMillis(500))
            .build();
        return RateLimiterRegistry.of(config);
    }

    /**
     * 按 IP 限流（可替换为 X-User-Id 限流）。
     */
    @Bean
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> Mono.justOrEmpty(
            exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "anonymous"
        );
    }

    @Bean
    public RequestRateLimiterGatewayFilterFactory rateLimiterGatewayFilterFactory(
            RedisRateLimiter limiter, KeyResolver resolver) {
        return new RequestRateLimiterGatewayFilterFactory(limiter, resolver);
    }
}
