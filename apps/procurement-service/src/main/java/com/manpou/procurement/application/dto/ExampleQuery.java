package com.manpou.procurement.application.dto;

import lombok.Data;

/**
 * 分页查询请求参数。
 * 仅包含分页与筛选条件，不包含业务字段。
 */
@Data
public class ExampleQuery {

    /** 当前页（从 1 开始） */
    private Integer page = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 关键词（按 name 模糊匹配） */
    private String keyword;
}
