package com.manpou.allinone.legacyimportlist8.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 报关查询响应 VO。
 * 根据货号(code)精准查询。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomsQueryResultVO {

    /** 货号（查询时输入的原始值） */
    private String code;

    /** 是否找到对应记录 */
    private boolean found;

    /** 税 */
    private String tax;

    /** 单价(元) */
    private Double unitCh;

    /** 汇率 */
    private Double rate;

    /** 仓库 */
    private String souko;

    /** 所在地 */
    private String location;
}
