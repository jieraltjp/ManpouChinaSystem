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
 * - /api/v1/auth/**           → user-service       （白名单，无需 JWT 鉴权）
 * - /api/v1/purchase-orders/** → procurement-service
 * - /api/v1/products/**      → allinone (18090)
 * - /api/v1/warehouse/**      → allinone (18090)
 * - /api/v1/customs/**        → allinone (18090)
 * - /api/v1/logistics/**      → allinone (18090)
 * - /api/v1/finance/**        → allinone (18090)
 * - /api/v1/notifications/** → allinone (18090)
 *
 * product, warehouse, customs, logistics, finance, notification 六域
 * 初期合并在 manpou-allinone (18090)，后期按 Kafka Topic 边界拆分。
 */
@Slf4j
@Configuration
public class RouteConfig {

    @Value("${gateway.route.user-service:http://localhost:18081}")
    private String userServiceUri;

    @Value("${gateway.route.procurement-service:http://localhost:18083}")
    private String procurementServiceUri;

    @Value("${gateway.route.allinone-service:http://localhost:18090}")
    private String allinoneServiceUri;


    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 认证（白名单，无需 JWT 鉴权）
            .route("user-service-auth", r -> r.path("/api/v1/auth/**")
                .filters(f -> f.stripPrefix(0))
                .uri(userServiceUri))
            // 发注管理
            .route("procurement-service", r -> r.path("/api/v1/purchase-orders/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("procurementService").setFallbackUri("forward:/fallback"))
                    .retry(retry -> retry.setMethods(HttpMethod.GET).setRetries(3).setSeries(HttpStatus.Series.SERVER_ERROR)))
                .uri(procurementServiceUri))
            // 商品管理 → allinone
            .route("allinone-products", r -> r.path("/api/v1/products/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("allinoneProducts").setFallbackUri("forward:/fallback")))
                .uri(allinoneServiceUri))
            // 仓储管理 → allinone
            .route("allinone-warehouse", r -> r.path("/api/v1/warehouse/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("allinoneWarehouse").setFallbackUri("forward:/fallback")))
                .uri(allinoneServiceUri))
            // 报关管理 → allinone
            .route("allinone-customs", r -> r.path("/api/v1/customs/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("allinoneCustoms").setFallbackUri("forward:/fallback")))
                .uri(allinoneServiceUri))
            // 物流管理 → allinone
            .route("allinone-logistics", r -> r.path("/api/v1/logistics/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("allinoneLogistics").setFallbackUri("forward:/fallback")))
                .uri(allinoneServiceUri))
            // 财务管理 → allinone
            .route("allinone-finance", r -> r.path("/api/v1/finance/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("allinoneFinance").setFallbackUri("forward:/fallback")))
                .uri(allinoneServiceUri))
            // 通知服务 → allinone
            .route("allinone-notifications", r -> r.path("/api/v1/notifications/**")
                .filters(f -> f.stripPrefix(0)
                    .circuitBreaker(c -> c.setName("allinoneNotifications").setFallbackUri("forward:/fallback")))
                .uri(allinoneServiceUri))
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
