package com.company.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway 应用入口。
 *
 * 职责边界：
 * - 统一入口：所有外部请求经过此网关
 * - 路由转发：根据 Nacos 服务发现动态路由到下游微服务
 * - 鉴权：JWT RS256 验签（公钥在 classpath:keys/public.pem）
 * - 限流 + 熔断：Resilience4j per-route 配置
 * - TraceId：W3C traceparent 透传，响应头 X-Trace-Id 返回
 * - 统一响应：网关层统一 Result<T> 结构
 *
 * 禁止在网关中执行业务逻辑。
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
