package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandType;
import lombok.Data;

@Data
public class ReplenishmentDemandQuery {

    private Integer page = 0;
    private Integer pageSize = 20;
    private DemandType demandType;
    private String productCode;
}
