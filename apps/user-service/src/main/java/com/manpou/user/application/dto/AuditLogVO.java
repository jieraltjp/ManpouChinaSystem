package com.manpou.user.application.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志视图对象。
 */
@Data
public class AuditLogVO {
    private Long id;
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
    private LocalDateTime createTime;
}
