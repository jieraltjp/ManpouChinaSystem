package com.manpou.allinone.order.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单总览-需求单选择器 DTO。
 * 用于 /api/v1/orders/demands 列表查询。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDemandSelectorDTO {

    private Long id;

    /** 需求单编号 */
    private String demandCode;

    /** 需求类型：REPLENISHMENT / NEW_PURCHASE */
    private String demandType;

    /** 主货号 */
    private String productCode;

    /** 子货号明细摘要（v1.6.0）："be:100久留米, bu:50名古屋" */
    private String subProductItemsSummary;

    /** 日本担当 */
    private String japanLead;

    /** 状态：PENDING / CONVERTED / CANCELLED */
    private String status;

    private LocalDateTime createTime;
}
