package com.company.gateway.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * 动态路由配置。
 *
 * 路由来源优先级：
 * 1. Nacos 服务发现（通过 spring.cloud.gateway.discovery.locator 启用）
 * 2. 静态路由（此处配置，适用于固定下游服务）
 *
 * 下游服务路由：
 * - java-service: 后端微服务（鉴权）
 * - python-service: Python 微服务（鉴权）
 *
 * 无 Nacos 时使用静态路由，URI 从 application.yml 读取。
 */
@Slf4j
@Configuration
public class RouteConfig {

    @Value("${gateway.route.java-service:http://java-service:8080}")
    private String javaServiceUri;

    @Value("${gateway.route.python-service:http://python-service:8000}")
    private String pythonServiceUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Java 微服务
            .route("java-service", r -> r.path("/api/v1/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .circuitBreaker(c -> c
                        .setName("javaService")
                        .setFallbackUri("forward:/fallback/java-service"))
                    .retry(retry -> retry
                        .setMethods(HttpMethod.GET)
                        .setRetries(3)
                        .setSeries(HttpStatus.Series.SERVER_ERROR)))
                .uri(javaServiceUri))
            // Python 微服务
            .route("python-service", r -> r.path("/api/v2/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .circuitBreaker(c -> c
                        .setName("pythonService")
                        .setFallbackUri("forward:/fallback/python-service"))
                    .retry(retry -> retry
                        .setMethods(HttpMethod.GET)
                        .setRetries(3)
                        .setSeries(HttpStatus.Series.SERVER_ERROR)))
                .uri(pythonServiceUri))
            // 认证服务（无鉴权，白名单）
            .route("auth", r -> r.path("/api/v1/auth/**")
                .uri(javaServiceUri))
            // Actuator（无鉴权）
            .route("actuator", r -> r.path("/actuator/**")
                .uri("forward:/actuator"))
            // 健康检查
            .route("health", r -> r.path("/health")
                .filters(f -> f.setStatus(HttpStatus.OK))
                .uri("forward:/health"))
            .build();
    }

    @Bean
    public RouteLocator fallbackRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("fallback-java", r -> r.path("/fallback/java-service")
                .filters(f -> f.setStatus(HttpStatus.BAD_GATEWAY))
                .uri("forward:/fallback"))
            .route("fallback-python", r -> r.path("/fallback/python-service")
                .filters(f -> f.setStatus(HttpStatus.BAD_GATEWAY))
                .uri("forward:/fallback"))
            .build();
    }
}
