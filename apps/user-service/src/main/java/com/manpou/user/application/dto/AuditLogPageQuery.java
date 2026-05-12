package com.manpou.user.application.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 操作日志分页查询参数。
 */
@Data
public class AuditLogPageQuery {

    private String userId;

    private String module;

    private String action;

    private String resourceType;

    private String resourceId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private Integer page = 0;

    private Integer size = 20;
}
