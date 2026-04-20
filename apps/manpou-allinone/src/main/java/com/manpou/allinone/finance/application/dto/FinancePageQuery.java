package com.manpou.allinone.finance.application.dto;

import com.manpou.allinone.finance.domain.model.FinanceStatus;
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
public class FinancePageQuery {

    private Long id;
    private String name;
    private FinanceStatus status;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
