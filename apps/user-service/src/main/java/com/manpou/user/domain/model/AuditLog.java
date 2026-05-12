package com.manpou.user.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 操作日志实体（纯追加，不继承 BaseEntity）。
 *
 * <p>audit_log 表无 create_by/update_by/is_deleted 列，是只追加的审计日志，
 * 故不继承 BaseEntity，也不支持软删除。
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "user_name", length = 64)
    private String operatorName;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "module", nullable = false, length = 32)
    private String module;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "http_method", length = 8)
    private String httpMethod;

    @Column(name = "http_url", length = 256)
    private String httpUrl;

    @Column(name = "resource_type", length = 64)
    private String resourceType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Column(name = "resource_code", length = 64)
    private String resourceCode;

    @Column(name = "detail", columnDefinition = "JSON")
    private String detail;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
