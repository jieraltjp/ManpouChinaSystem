package com.manpou.gateway.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * 静态路由配置。
 *
 * 路由来源：application.yml 的 gateway.route.* 配置
 *
 * 路径前缀 → 目标服务：
 * - /api/v1/auth/**   → user-service       （白名单，无需 JWT 鉴权）
 * - /api/v1/products/** → product-service
 * - /api/v1/purchase-orders/** → procurement-service
 * - /api/v1/warehouse/**  → warehouse-service
 * - /api/v1/customs/**    → customs-service
 * - /api/v1/logistics/**   → logistics-service
 * - /api/v1/finance/**     → finance-service
 * - /api/v1/notifications/** → notification-service
 *
 * 所有路径 /api/v1/** 统一前缀转发，stripPrefix 保持路径原样。
 */
@Slf4j
@Configuration
public class RouteConfig {

    @Value("${gateway.route.user-service:http://localhost:18081}")
    private String userServiceUri;

    @Value("${gateway.route.product-service:http://localhost:18082}")
    private String productServiceUri;

    @Value("${gateway.route.procurement-service:http://localhost:18083}")
    private String procurementServiceUri;

    @Value("${gateway.route.warehouse-service:http://localhost:18084}")
    private String warehouseServiceUri;

    @Value("${gateway.route.customs-service:http://localhost:18085}")
    private String customsServiceUri;

    @Value("${gateway.route.logistics-service:http://localhost:18086}")
    private String logisticsServiceUri;

    @Value("${gateway.route.finance-service:http://localhost:18087}")
    private String financeServiceUri;

    @Value("${gateway.route.notification-service:http://localhost:18088}")
    private String notificationServiceUri;


    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 认证（白名单，无需 JWT 鉴权）
            .route("user-service-auth", r -> r.path("/api/v1/auth/**")
                .filters(f -> f.stripPrefix(0))
                .uri(userServiceUri))
            // 商品管理
            .route("product-service", r -> r.path("/api/v1/products/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("productService").setFallbackUri("forward:/fallback")))
                .uri(productServiceUri))
            // 发注管理
            .route("procurement-service", r -> r.path("/api/v1/purchase-orders/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("procurementService").setFallbackUri("forward:/fallback"))
                    .retry(retry -> retry.setMethods(HttpMethod.GET).setRetries(3).setSeries(HttpStatus.Series.SERVER_ERROR)))
                .uri(procurementServiceUri))
            // 仓储管理
            .route("warehouse-service", r -> r.path("/api/v1/warehouse/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("warehouseService").setFallbackUri("forward:/fallback")))
                .uri(warehouseServiceUri))
            // 报关管理
            .route("customs-service", r -> r.path("/api/v1/customs/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("customsService").setFallbackUri("forward:/fallback")))
                .uri(customsServiceUri))
            // 物流管理
            .route("logistics-service", r -> r.path("/api/v1/logistics/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("logisticsService").setFallbackUri("forward:/fallback")))
                .uri(logisticsServiceUri))
            // 财务管理
            .route("finance-service", r -> r.path("/api/v1/finance/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("financeService").setFallbackUri("forward:/fallback")))
                .uri(financeServiceUri))
            // 通知服务
            .route("notification-service", r -> r.path("/api/v1/notifications/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("notificationService").setFallbackUri("forward:/fallback")))
                .uri(notificationServiceUri))
            // 其他 /api/v1 请求默认路由到 user-service
            .route("user-service-default", r -> r.path("/api/v1/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("userService").setFallbackUri("forward:/fallback")))
                .uri(userServiceUri))
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
            .route("fallback-default", r -> r.path("/fallback")
                .filters(f -> f.setStatus(HttpStatus.BAD_GATEWAY))
                .uri("forward:/fallback"))
            .build();
    }
}
