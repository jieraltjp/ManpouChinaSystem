package com.manpou.allinone.customs.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JapanCustomsUpdateCmd {

    private LocalDate arrivalDate;

    @Size(max = 64)
    private String subProductCode;  // 子货号/颜色

    @Size(max = 128)
    private String customsBroker;

    @Size(max = 32)
    private String brokerPhone;

    @Size(max = 64)
    private String brokerContact;

    @Size(max = 64)
    private String arrivalPort;

    private BigDecimal declaredWeightKg;
    private BigDecimal declaredVolumeCbm;

    private String remarks;
}
