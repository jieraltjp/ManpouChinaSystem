package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.LogisticsStatus;
import com.manpou.allinone.logistics.domain.model.PlanType;
import lombok.Data;

@Data
public class LogisticsPlanQuery {

    private Integer page = 0;
    private Integer pageSize = 20;

    private String productCode;
    private PlanType planType;
    private LogisticsStatus status;
    private Long procurementId;
    private Long factoryId;
}
