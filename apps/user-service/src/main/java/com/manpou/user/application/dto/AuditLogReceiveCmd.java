package com.manpou.user.application.dto;

import lombok.Data;

/**
 * allinone 通过 HTTP POST 写入操作日志的请求体。
 */
@Data
public class AuditLogReceiveCmd {
    private String traceId;
    private String userId;
    private String username;
    private String operatorName;
    private Long companyId;
    private Long departmentId;
    private String module;
    private String action;
    private String httpMethod;
    private String httpUrl;
    private String resourceType;
    private String resourceId;
    private String resourceCode;
    private String detail;
    private String ipAddress;
    private String userAgent;
    private String requestId;
}
