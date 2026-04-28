package com.manpou.allinone.order.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 补货需求 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplenishmentDemandVO {

    private Long id;
    private String demandCode;
    private String demandType;
    private String productCode;
    /** 子货号全码（v2.0.0，如 ad009-be） */
    private String subProductCode;
    /** 需求数量 */
    private Integer quantity;
    /** 目的地 */
    private String destination;
    private String japanLead;
    private String status;
    private LocalDateTime createTime;
}
