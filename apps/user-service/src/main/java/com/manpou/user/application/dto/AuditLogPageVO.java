package com.manpou.user.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 操作日志分页结果。
 */
@Data
public class AuditLogPageVO {
    private List<AuditLogVO> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
}
