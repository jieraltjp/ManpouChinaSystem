package com.manpou.gateway;

import com.alibaba.cloud.nacos.NacosConfigAutoConfiguration;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway 应用入口。
 *
 * 职责边界：
 * - 统一入口：所有外部请求经过此网关
 * - 路由转发：静态路由到下游微服务（无 Nacos 时使用）
 * - 鉴权：JWT RS256 验签（公钥在 classpath:keys/public.pem）
 * - 限流 + 熔断：Resilience4j per-route 配置
 * - TraceId：W3C traceparent 透传，响应头 X-Trace-Id 返回
 *
 * 禁止在网关中执行业务逻辑。
 */
@SpringBootApplication(
    exclude = {
        NacosConfigAutoConfiguration.class,
        NacosDiscoveryAutoConfiguration.class,
    }
)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
