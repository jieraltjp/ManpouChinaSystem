package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.MappingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 需求-采购分配映射查询参数 DTO（SPEC-B11 §10.2）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandProcurementMappingQuery {

    private Long demandId;
    private Long procurementId;
    private MappingStatus status;
    @Builder.Default
    private Integer page = 1;
    @Builder.Default
    private Integer pageSize = 20;
}
