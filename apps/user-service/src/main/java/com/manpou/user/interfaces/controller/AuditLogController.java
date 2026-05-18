package com.manpou.user.interfaces.controller;

import com.manpou.common.result.Result;
import com.manpou.user.application.dto.AuditLogPageQuery;
import com.manpou.user.application.dto.AuditLogPageVO;
import com.manpou.user.application.dto.AuditLogReceiveCmd;
import com.manpou.user.application.dto.AuditLogVO;
import com.manpou.user.application.service.AuditLogService;
import com.manpou.user.domain.model.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志接口。
 *
 * <p>GET 接口供前端查询，POST 接口供 allinone 写入业务日志（内部调用）。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Value("${app.audit-log.secret:}")
    private String auditLogSecret;

    /**
     * 分页查询操作日志。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public Result<AuditLogPageVO> pageQuery(AuditLogPageQuery query) {
        return Result.ok(auditLogService.pageQuery(query));
    }

    /**
     * 查询操作日志详情。
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('audit:read')")
    public Result<AuditLogVO> getById(@PathVariable Long id) {
        return Result.ok(auditLogService.getById(id));
    }

    /**
     * 内部接口：接收 allinone 业务日志写入请求。
     * 通过 X-AuditLog-Secret header 校验。
     */
    @PostMapping
    public Result<Void> receiveFromAllinone(
            @RequestBody AuditLogReceiveCmd cmd,
            @RequestHeader(value = "X-AuditLog-Secret", required = false) String secret) {
        // 未配置 secret 时拒绝所有写入，防止开放漏洞
        if (auditLogSecret == null || auditLogSecret.isBlank()) {
            log.warn("[AuditLog] audit-log secret not configured, rejecting write");
            return Result.fail("auditLog.unauthorized", "AuditLog secret not configured");
        }
        if (!auditLogSecret.equals(secret)) {
            log.warn("[AuditLog] unauthorized POST attempt from {}", secret);
            return Result.fail("auditLog.unauthorized", "Unauthorized");
        }
        AuditLog al = new AuditLog();
        al.setTraceId(cmd.getTraceId());
        al.setUserId(cmd.getUserId());
        al.setUsername(cmd.getUsername());
        al.setOperatorName(cmd.getOperatorName());
        al.setCompanyId(cmd.getCompanyId());
        al.setDepartmentId(cmd.getDepartmentId());
        al.setModule(cmd.getModule());
        al.setAction(cmd.getAction());
        al.setHttpMethod(cmd.getHttpMethod());
        al.setHttpUrl(cmd.getHttpUrl());
        al.setResourceType(cmd.getResourceType());
        al.setResourceId(cmd.getResourceId());
        al.setResourceCode(cmd.getResourceCode());
        al.setDetail(cmd.getDetail());
        al.setIpAddress(cmd.getIpAddress());
        al.setUserAgent(cmd.getUserAgent());
        al.setRequestId(cmd.getRequestId());
        auditLogService.saveAsync(al);
        return Result.ok();
    }
}
