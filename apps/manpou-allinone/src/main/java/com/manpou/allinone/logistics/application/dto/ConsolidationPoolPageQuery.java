package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ConsolidationPoolStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ConsolidationPoolPageQuery {

    private Long id;
    private String poolCode;
    private String destinationPort;
    private BigDecimal totalCbm;
    private BigDecimal totalWeightKg;
    private Integer planCount;
    private BigDecimal containerThresholdCbm;
    private ConsolidationPoolStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
