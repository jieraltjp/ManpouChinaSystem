package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转采购响应 DTO（v2.0.0）。
 * 一条 Demand → 一条 Procurement（1:1）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertDemandResponse {

    private DemandStatus demandStatus;
    /** 生成的 Procurement ID */
    private Long linkedProcurementId;
}
