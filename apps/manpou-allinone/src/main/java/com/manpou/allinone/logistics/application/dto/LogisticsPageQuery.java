package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分页查询响应 DTO。
 * 仅包含业务数据字段，不包含查询参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogisticsPageQuery {

    private Long id;
    private String name;
    private LogisticsStatus status;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
