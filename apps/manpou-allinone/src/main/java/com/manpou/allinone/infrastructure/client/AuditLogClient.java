package com.manpou.allinone.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * user-service 操作日志写入客户端。
 *
 * <p>通过 HTTP POST 将业务操作日志异步发送到 user-service。
 * 失败只打 WARN 日志，不影响主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogClient {

    private final RestTemplate restTemplate;

    @Value("${app.audit-log.user-service-url:http://127.0.0.1:18081}")
    private String userServiceUrl;

    @Value("${app.audit-log.secret:}")
    private String secret;

    private static final String PATH = "/api/v1/audit-logs";
    private static final String SECRET_HEADER = "X-AuditLog-Secret";

    /**
     * 异步发送操作日志。
     * 仅发送成功操作的日志（调用方保证）。
     *
     * @param payload 包含 traceId/userId/username/module/action/httpMethod/httpUrl/
     *                resourceType/resourceId/resourceCode/detail/ipAddress/userAgent/requestId
     */
    @Async
    public void sendAsync(Map<String, Object> payload) {
        log.info("[AuditLog] sendAsync START: url={}, module={}, action={}, resourceId={}",
                userServiceUrl + PATH,
                payload.get("module"), payload.get("action"), payload.get("resourceId"));
        try {
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            if (secret != null && !secret.isBlank()) {
                headers.set(SECRET_HEADER, secret);
            }

            // 透传 traceparent（跨服务链路追踪）
            String traceparent = org.springframework.web.context.request.RequestContextHolder
                    .getRequestAttributes() != null
                    ? ((org.springframework.web.context.request.ServletRequestAttributes)
                        org.springframework.web.context.request.RequestContextHolder
                        .getRequestAttributes())
                        .getRequest().getHeader("traceparent")
                    : null;
            if (traceparent != null) {
                headers.set("traceparent", traceparent);
            }

            var entity = new org.springframework.http.HttpEntity<>(payload, headers);
            String url = userServiceUrl + PATH;
            restTemplate.postForEntity(url, entity, Void.class);

            log.debug("[AuditLog] sent: module={}, action={}, resourceId={}",
                    payload.get("module"), payload.get("action"), payload.get("resourceId"));
        } catch (Exception ex) {
            log.warn("[AuditLog] send failed: module={}, action={}, error={}",
                    payload.get("module"), payload.get("action"), ex.getMessage());
        }
    }
}
