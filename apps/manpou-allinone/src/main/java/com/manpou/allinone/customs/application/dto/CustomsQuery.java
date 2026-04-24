package com.manpou.allinone.customs.application.dto;

import com.manpou.allinone.customs.domain.model.DomesticCustomsStatus;
import lombok.Data;

/**
 * 国内报关记录分页查询参数。
 */
@Data
public class CustomsQuery {

    /** 当前页（从 0 开始） */
    private Integer page = 0;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 货号关键词（模糊匹配） */
    private String keyword;

    /** 状态筛选 */
    private DomesticCustomsStatus status;

    /** 关联发注单 ID */
    private Long procurementId;

    /** 关联调配计划 ID */
    private Long logisticsPlanId;
}
