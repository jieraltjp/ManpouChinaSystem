package com.manpou.allinone.customs.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JapanCustomsCompleteCmd {

    @NotNull
    private BigDecimal importDutyPaid;

    @NotNull
    private BigDecimal consumptionTaxPaid;

    @NotNull
    private LocalDate clearanceDate;
}
