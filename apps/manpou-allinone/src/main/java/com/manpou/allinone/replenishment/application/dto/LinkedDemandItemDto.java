package com.manpou.allinone.replenishment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关联发注表明细 DTO（v1.6.0）。
 * 用于 CONVERTED 状态列表展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedDemandItemDto {

    private Long linkedProcurementId;
    private String subCode;
}
