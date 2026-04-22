package com.manpou.allinone.product.application.dto;

import lombok.Data;

/**
 * 分页查询请求参数。
 * 对应 docs/business/SPEC-B10 §2.1 API 设计。
 */
@Data
public class ProductQuery {

    /** 当前页（从 1 开始） */
    private Integer page = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 主货号（精确匹配） */
    private String masterCode;

    /** 关键词（按中文名称/英文名称模糊匹配） */
    private String keyword;

    /** HS编码（精确匹配） */
    private String hsCode;
}
