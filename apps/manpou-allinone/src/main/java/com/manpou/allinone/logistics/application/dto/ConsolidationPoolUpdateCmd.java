package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ConsolidationPoolStatus;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

@Data
public class ConsolidationPoolUpdateCmd {

    @Length(max = 64)
    private String destinationPort;

    private BigDecimal containerThresholdCbm;

    private ConsolidationPoolStatus status;
}
