package com.manpou.allinone.replenishment.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReplenishmentDemandPageQuery {

    private Long id;
    private String demandCode;
    private DemandType demandType;
    private String productCode;
    private String subProductCode;
    private Integer quantity;
    private String destination;
    private String japanLead;
    private DemandStatus status;
    private Long linkedProcurementId;
    private String remarks;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
