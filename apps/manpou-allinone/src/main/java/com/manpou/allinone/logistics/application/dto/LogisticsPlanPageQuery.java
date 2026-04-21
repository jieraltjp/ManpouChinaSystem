package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.manpou.allinone.logistics.domain.model.PlanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LogisticsPlanPageQuery {

    private Long id;
    private String planCode;
    private Long procurementId;
    private Long factoryId;
    private String productCode;
    private String subProductCode;
    private PlanType planType;
    private LogisticsStatus status;
    private BigDecimal cargoLengthCm;
    private BigDecimal cargoWidthCm;
    private BigDecimal cargoHeightCm;
    private BigDecimal cargoWeightKg;
    private BigDecimal cargoVolumeCbm;
    private Integer quantity;
    private Boolean requiresQc;
    private Long containerId;
    private Long poolId;
    private LocalDate estimatedShipDate;
    private LocalDate actualShipDate;
    private String remarks;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
