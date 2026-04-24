package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 转采购响应 DTO（v1.6.0）。
 * 返回批量生成的 Procurement ID 列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertDemandResponse {

    private DemandStatus demandStatus;
    private List<Long> linkedProcurementIds;
}
