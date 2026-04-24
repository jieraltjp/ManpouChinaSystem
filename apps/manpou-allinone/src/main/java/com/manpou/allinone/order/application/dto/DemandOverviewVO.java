package com.manpou.allinone.order.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Demand 锚点总览响应（/api/v1/orders/demands/{id}/overview）。
 * <p>
 * Step1 有数据，Step2-8 = NOT_STARTED。
 * <p>
 * v1.6.0：替代 {@link OrderOverviewPageVO}，保持 API 契约精确。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemandOverviewVO {

    private Long demandId;

    private OrderOverviewPageVO.DemandVO demand;

    /** 固定 [COMPLETED, NOT_STARTED, ..., NOT_STARTED] */
    private OrderOverviewPageVO.StepStatus[] stepStatuses;
}
