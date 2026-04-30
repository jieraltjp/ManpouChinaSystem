package com.manpou.allinone.replenishment.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.replenishment.domain.model.MappingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 需求-采购分配映射分页查询响应 DTO（SPEC-B11 §10.2）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemandProcurementMappingPageQuery {

    private Long id;
    private Long demandId;
    private Long procurementId;
    private Integer allocatedQuantity;
    private MappingStatus status;
    private String createBy;
    private LocalDateTime createTime;
}
