package com.manpou.allinone.customs.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JapanCustomsCreateCmd {

    private Long procurementId;
    private Long domesticCustomsId;
    private Long logisticsPlanId;
    private String subProductCode;  // 子货号/颜色（来自 Procurement，全链路追踪）

    private LocalDate arrivalDate;

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
