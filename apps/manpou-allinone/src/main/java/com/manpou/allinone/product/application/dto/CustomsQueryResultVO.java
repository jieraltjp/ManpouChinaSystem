package com.manpou.allinone.product.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 报关查询响应 VO。
 * 每行返回一个货号的查询结果，包含：单价、税率、仓库、净重、毛重、HS编码。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomsQueryResultVO {

    /** 货号（查询时输入的原始值） */
    private String masterCode;

    /** 是否找到对应商品 */
    private boolean found;

    /** 含税单价(CNY) */
    private BigDecimal unitPriceRmb;

    /** 增值税率 */
    private BigDecimal taxRate;

    /** 仓库归属 */
    private String warehouse;

    /** 净重(kg) */
    private BigDecimal netWeightKg;

    /** 毛重(kg) */
    private BigDecimal grossWeightKg;

    /** HS编码 */
    private String hsCode;
}
