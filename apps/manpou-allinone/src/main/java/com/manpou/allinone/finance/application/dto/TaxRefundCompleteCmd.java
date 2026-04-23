package com.manpou.allinone.finance.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TaxRefundCompleteCmd {

    @NotNull
    @Positive
    private BigDecimal actualRefundRmb;

    @NotNull
    private LocalDate refundDate;

    private String refundBank;
}
