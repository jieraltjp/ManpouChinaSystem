package com.manpou.allinone.sales.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalesRecordUpdateCmd {
    private LocalDate listingDate;
    private Integer safetyStock;
    private BigDecimal sellingPriceJpy;
    private String remarks;
}
